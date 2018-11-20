package peakaboo.curvefit.peak.search.searcher;

import java.util.ArrayList;
import java.util.List;

import cyclops.ISpectrum;
import cyclops.ReadOnlySpectrum;
import cyclops.Spectrum;
import cyclops.SpectrumCalculations;
import net.sciencestudio.autodialog.model.Value;
import peakaboo.filter.model.Filter;
import peakaboo.filter.plugins.noise.WeightedAverageNoiseFilter;

public class TopHatPeakSearcher implements PeakSearcher {

	float[] coefs;
	int halfW, fullV;
	
	public TopHatPeakSearcher() {
		halfW = 5;
		fullV = 10;
		coefs = new float[1+halfW+fullV];
		float wValue = 1f / (halfW*2f+1f);
		float vValue = -1f / (fullV*2f);
		
		coefs[0] = wValue;
		for (int i = 1; i <= halfW; i++) {
			coefs[i] = wValue;
		}
		for (int i = 1+halfW; i < 1+halfW+fullV; i++) {
			coefs[i] = vValue;
		}
	}
	
	@Override
	public List<Integer> search(ReadOnlySpectrum data) {
		
		Spectrum tophat = tophat(data);
		Spectrum d1 = SpectrumCalculations.derivative(tophat);
		float max = data.max();
		
		List<Integer> peaks = new ArrayList<>();
		boolean lastPositive = d1.get(0) > 0;
		for (int i = 1; i < data.size(); i++) {
			boolean thisPositive = d1.get(i) > 0 && tophat.get(i) > 0;
			
			//crossing the boundary from pos to neg means a peak top
			if (lastPositive && !thisPositive && data.get(i) > max*0.002) {
				int peak = i;
				while (true) {
					if (data.get(peak+1) > data.get(peak)) {
						peak = peak+1;
					} else if (data.get(peak-1) > data.get(peak)) {
						peak = peak-1;
					} else {
						break;
					}
				}
				peaks.add(peak);
			}
			
			lastPositive = thisPositive;
			
		}
		
		peaks.sort((a, b) -> {
			return Float.compare(tophat.get(b), tophat.get(a));
		});
		return peaks;
		
	}

	public Spectrum tophat(ReadOnlySpectrum data) {
		

		Filter filter = new WeightedAverageNoiseFilter();
		filter.initialize();
		Value<Integer> width = (Value<Integer>) filter.getParameters().get(0);
		width.setValue(new Integer(10));
		
		//aggressive smoothing to get just the most significant peaks
		ReadOnlySpectrum smoothed = new ISpectrum(data);
		
		for (int i = 0; i < 3; i++) {
			smoothed = filter.filter(smoothed, false);
		}
		
		
		Spectrum tophat = new ISpectrum(smoothed.size());
		
		int range = halfW + fullV;
		for (int i = 0; i < smoothed.size(); i++) {
			float value = 0f;
			for (int j = -range; j <= +range; j++) {
				int index = i+j;
				index = Math.max(index, 0);
				index = Math.min(index, smoothed.size()-1);
				value += smoothed.get(index) * coefs[Math.abs(j)];
			}
			tophat.set(i, value);
		}
		
		

		
		return new ISpectrum(tophat);
	}
	
}
