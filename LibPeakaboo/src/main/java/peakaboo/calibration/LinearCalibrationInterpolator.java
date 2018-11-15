package peakaboo.calibration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import peakaboo.curvefit.peak.table.Element;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.curvefit.peak.transition.TransitionSeriesType;

public class LinearCalibrationInterpolator implements CalibrationProcessor {

	@Override
	public void process(CalibrationReference reference, CalibrationProfile profile) {
		interpolate(profile, TransitionSeriesType.K);
		interpolate(profile, TransitionSeriesType.L);
		interpolate(profile, TransitionSeriesType.M);
	}
	
	private void interpolate(CalibrationProfile profile, TransitionSeriesType tst) {
		List<TransitionSeries> knowns = profile.calibrations
				.keySet()
				.stream()
				.filter(ts -> ts.type == tst)
				.sorted((a, b) -> Integer.compare(a.element.ordinal(), b.element.ordinal()))
				.collect(Collectors.toList());
		if (knowns.size() < 2) { return; }
		
		TransitionSeries previous = null;
		for (TransitionSeries known : knowns) {
			if (previous == null) {
				previous = known;
				continue;
			}
						
			//all missing entries between previous and known
			for (int i = previous.element.ordinal()+1; i < known.element.ordinal(); i++) {
				TransitionSeries inter = new TransitionSeries(Element.values()[i], tst);				
				profile.calibrations.put(inter, interpolate(profile.calibrations, inter, previous, known));
				profile.interpolated.add(inter);
			}
			
			previous = known;
		}
		
		
	}
	
	private float interpolate(Map<TransitionSeries, Float> calibrations, TransitionSeries current, TransitionSeries previous, TransitionSeries next) {
				
		float pv = calibrations.get(previous);
		float nv = calibrations.get(next);
		
		float po = previous.element.ordinal();
		float no = next.element.ordinal();
		float co = current.element.ordinal();
		
		float percent = (co-po) / (no-po);
		float value = pv + ((nv - pv) * percent);
		
		return value;
		
		
	}

	@Override
	public void process(CalibrationReference reference, Map<TransitionSeries, Float> calibrations) {
		//Not Used
	}
	
	
}