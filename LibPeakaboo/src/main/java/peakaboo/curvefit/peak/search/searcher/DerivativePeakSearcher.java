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

public class DerivativePeakSearcher implements PeakSearcher {

	@Override
	public List<Integer> search(ReadOnlySpectrum rawdata) {
		
		Filter filter = new WeightedAverageNoiseFilter();
		filter.initialize();
		Value<Integer> width = (Value<Integer>) filter.getParameters().get(0);
		width.setValue(new Integer(8));
		
		//aggressive smoothing to get just the most significant peaks
		ReadOnlySpectrum smoothed = new ISpectrum(rawdata);
		
		for (int i = 0; i < 3; i++) {
			smoothed = filter.filter(smoothed, false);
		}
		ReadOnlySpectrum data = smoothed;
				
		//first and second derivatives.
		Spectrum d1 = SpectrumCalculations.derivative(data);
		Spectrum d2 = SpectrumCalculations.derivative(d1);
		float dataMax = data.max();
		float d2Max = SpectrumCalculations.abs(d2).max();
		
		//Any detected peak for which the second derivative doesn't 
		//exceed 0.1% of the max-abs of the second derivative spectrum
		//will be discarted
		float d2threshold = SpectrumCalculations.abs(d2).max() * 0.001f;
		
				
		//First derivative crossing from positive to negative indicates a peak top
		List<Integer> channels = new ArrayList<>();
		int nonnegative = 0;
		int negative = 0;
		boolean gap = false;
		int bestChannel = 0;
		float bestValue = 0;
		
		
		for (int i = 0; i < data.size(); i++) {
			float value = d2.get(i);
			
			if (value >= 0) {
				
				
				//If we had been tracking a possible peak, and it looks good
				if (gap && negative >= 10) {
					float score = score(bestChannel, data, dataMax, d2, d2Max);
					if (score > 0.001) {
						channels.add(bestChannel);
						gap = false;
					}
					
				}
				
				nonnegative++;
				//Clear any negative-related values
				negative = 0;
				bestChannel = 0;
				bestValue = 0;
				
				//If we've gone 10 channels without a negative value, mark as having had a 
				//good sized gap between peaks
				if (nonnegative >= 5) {
					gap = true;
				}
				
			} else  {
				negative++;
				nonnegative = 0;

				//Start tracking the best point incase this turns out to be good
				if (d2.get(i) < bestValue) {
					bestValue = d2.get(i);
					bestChannel = i;
				}
				
			}
			
		}
		
		//System.exit(0);
		

		channels.sort((a, b) -> {
			return Float.compare(
					score(b, data, dataMax, d2, d2Max),
					score(a, data, dataMax, d2, d2Max)
				);
		});
		
		return channels;
		
	}
	
	private float scorePeak(int channel, ReadOnlySpectrum data, float dataMax, ReadOnlySpectrum d2, float d2Max) {
		float dataPercent = data.get(channel) / dataMax; 
		float d2Percent = (-d2.get(channel)) / d2Max;
		return (dataPercent + d2Percent) / 2f;
	}
	
	private float score(int channel, ReadOnlySpectrum data, float dataMax, ReadOnlySpectrum d2, float d2Max) {
		int range = 1;
		while (range < 20) {
			if (d2.get(channel - range) < d2.get(channel - range + 1)) break;
			if (d2.get(channel + range) < d2.get(channel + range - 1)) break;
			range++;
		}
		float bpre = data.get(channel - range);
		float bpost = data.get(channel + range);
		float bdelta = (bpost - bpre) / (float)(range * 2);
				
		//generate a peak with linear background removed
		Spectrum peak = new ISpectrum(data.subSpectrum(channel - range, channel + range));
		for (int i = 0; i < range*2+1; i++) {
			peak.set(i, peak.get(i) - bpre - bdelta*i);
		}
				
		Spectrum d2sub = new ISpectrum(d2.subSpectrum(channel - range, channel + range));
		
		return scorePeak(range, peak, dataMax, d2sub, d2Max);
		
	}
	
	
}
