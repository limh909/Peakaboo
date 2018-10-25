package peakaboo.calibration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cyclops.ReadOnlySpectrum;
import cyclops.SpectrumCalculations;
import net.sciencestudio.bolt.plugin.core.BoltPluginSet;
import peakaboo.calibration.processor.CalibrationNormalizer;
import peakaboo.calibration.processor.CalibrationProcessor;
import peakaboo.calibration.processor.CalibrationSmoother;
import peakaboo.calibration.processor.LinearCalibrationInterpolator;
import peakaboo.common.YamlSerializer;
import peakaboo.curvefit.curve.fitting.FittingResult;
import peakaboo.curvefit.curve.fitting.FittingResultSet;
import peakaboo.curvefit.peak.table.Element;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.curvefit.peak.transition.TransitionSeriesType;

/*
 * NOTE: Calibration does not use PeakTable TransitionSeries, 
 * it uses blank ones. This is so that the PeakTable does not 
 * limit which transition series it can represent. 
 */
public class CalibrationProfile {

	private CalibrationReference reference;
	private Map<TransitionSeries, Float> calibrations;
	private String name = "";
	
	/**
	 * Create an empty CalibrationProfile
	 */
	public CalibrationProfile() {
		this.reference = CalibrationReference.empty();
		calibrations = new LinkedHashMap<>();
		name = "Empty Z-Calibration Profile";
	}
	
	public CalibrationProfile(CalibrationReference reference, FittingResultSet sample) {
		this.reference = reference;
		calibrations = new LinkedHashMap<>();
				
		if (!sample.getParameters().getCalibration().isZero()) {
		
			//Build profile
			for (FittingResult fit : sample) {
				
				TransitionSeries ts = fit.getTransitionSeries();
				int channel = sample.getParameters().getCalibration().channelFromEnergy(ts.getStrongestTransition().energyValue);
				
				//we look up the transitionseries, but ultimately use a blank one.
				//we have to use all blanks, otherwise equality/contains cheks will fail
				ts = new TransitionSeries(ts.element, ts.type);
				if (! reference.contains(ts)) { continue; }
				
				//TODO: Is this the right way to measure sample intensity
				
				float sampleIntensity = fit.getFit().get(channel);
				float referenceValue = reference.getConcentration(ts);
				float calibration = (sampleIntensity / referenceValue) * 1000f;
				
				//don't add if element is being completely suppressed, this only seems 
				//to happen when the fitting solver algorithm incorrectly completely hides it 
				//we'll interpolate it later
				if (calibration < 1f) { continue; }
				if (Float.isInfinite(calibration) || Float.isNaN(calibration)) { continue; }
				calibrations.put(ts, calibration);
			}
			
		}
		
		//interpolate missing elements
		CalibrationProcessor interpolator = new LinearCalibrationInterpolator();
		interpolator.process(reference, calibrations);
		
		//smooth calibrations
		CalibrationProcessor smoothing = new CalibrationSmoother();
		smoothing.process(reference, calibrations);

		//normalize against anchor
		CalibrationProcessor normalizer = new CalibrationNormalizer();
		normalizer.process(reference, calibrations);
		
		
	}
	

	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<TransitionSeries, Float> getCalibrations() {
		return new HashMap<>(calibrations);
	}

	public boolean contains(TransitionSeries ts) {
		ts = new TransitionSeries(ts.element, ts.type);
		return calibrations.keySet().contains(ts);
	}
	
	public float getCalibration(TransitionSeries ts) {
		ts = new TransitionSeries(ts.element, ts.type);
		return calibrations.get(ts);
	}
	
	public CalibrationReference getReference() {
		return reference;
	}
	
	public float calibratedSum(FittingResult fittingResult) {
		TransitionSeries ts = fittingResult.getTransitionSeries();
		float rawfit = fittingResult.getFit().sum();
		return calibrate(rawfit, ts);
	}
	
	public float calibrate(float value, TransitionSeries ts) {
		ts = new TransitionSeries(ts.element, ts.type);
		if (calibrations.keySet().contains(ts)) {
			float calibration = calibrations.get(ts);
			return value / calibration;
		} else {
			return value;
		}
	}
	
	public float calibrate(FittingResult result) {
		return calibrate(result.getFit().sum(), result.getTransitionSeries());
	}
	
	public ReadOnlySpectrum calibrateMap(ReadOnlySpectrum data, TransitionSeries ts) {
		if (!contains(ts)) {
			return data;
		}
		float calibration = getCalibration(ts);
		return SpectrumCalculations.divideBy(data, calibration);
	}
	
	public boolean isEmpty() {
		return calibrations.size() == 0;
	}
	
	/**
	 * returns a sorted list of TransitionSeries in this profile 
	 */
	public List<TransitionSeries> getTransitionSeries(TransitionSeriesType tst) {
		List<TransitionSeries> tss = calibrations
				.keySet()
				.stream()
				.filter(ts -> ts.type == tst)
				.sorted((a, b) -> Integer.compare(a.element.ordinal(), b.element.ordinal()))
				.collect(Collectors.toList());
		return tss;
	}
	
	
	public static String save(CalibrationProfile profile) {
		SerializedCalibrationProfile serialized = new SerializedCalibrationProfile();
		serialized.referenceUUID = profile.reference.getUuid();
		serialized.referenceName = profile.reference.getName();
		serialized.name = profile.name;
		for (TransitionSeries ts : profile.calibrations.keySet()) {
			serialized.calibrations.put(ts.toIdentifierString(), profile.calibrations.get(ts));
		}
		return YamlSerializer.serialize(serialized);
	}
	
	
	public static CalibrationProfile load(Path path) throws IOException {
		return load(new String(Files.readAllBytes(path)));
	}
	
	public static CalibrationProfile load(String yaml) {
		CalibrationProfile profile = new CalibrationProfile();
		SerializedCalibrationProfile serialized = YamlSerializer.deserialize(yaml);
		for (String tsidentifier : serialized.calibrations.keySet()) {
			TransitionSeries ts = TransitionSeries.get(tsidentifier);
			profile.calibrations.put(ts, serialized.calibrations.get(tsidentifier));
		}
		
		BoltPluginSet<CalibrationReference> plugins = CalibrationPluginManager.SYSTEM.getPlugins();
		profile.reference = plugins.getByUUID(serialized.referenceUUID).create();
		if (profile.reference == null) {
			throw new RuntimeException("Cannot find Calibration Reference '" + serialized.referenceName + "' (" + serialized.referenceUUID + ")");
		}
		
		profile.name = serialized.name;
		if (profile.name == null) {
			profile.name = profile.reference.getName();
		}
		
		return profile;
	}

	public static void main(String[] args) throws IOException {
		
		CalibrationPluginManager.init(new File("/home/nathaniel/Desktop/PBCP/"));
		CalibrationProfile p = CalibrationProfile.load(new File("/home/nathaniel/Desktop/nist610sigray-15.pbcp").toPath());
		
		for (TransitionSeriesType tst : TransitionSeriesType.values()) {
			System.out.println(tst);
			for (Element e : Element.values()) {
				TransitionSeries ts = new TransitionSeries(e, tst);
				if (!p.contains(ts)) { continue; }
				System.out.println(e.atomicNumber() + ", " + p.getCalibration(ts));
			}
		}
		
	}

	
	
	
}


class SerializedCalibrationProfile {
	public String referenceUUID = null;
	public String referenceName = null;
	public String name = null;
	public Map<String, Float> calibrations = new LinkedHashMap<>();
}
 