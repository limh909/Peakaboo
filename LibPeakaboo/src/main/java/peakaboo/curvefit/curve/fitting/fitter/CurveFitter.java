package peakaboo.curvefit.curve.fitting.fitter;

import cyclops.ReadOnlySpectrum;
import cyclops.Spectrum;
import peakaboo.curvefit.curve.fitting.Curve;
import peakaboo.curvefit.curve.fitting.FittingResult;
import peakaboo.curvefit.curve.fitting.solver.GreedyFittingSolver;

/**
 * Defines a method by which a single {@link Curve} is fitted against a
 * {@link Spectrum} to determine how much the curve should be scaled by to match
 * the data. This is used with the {@link GreedyFittingSolver}, which allows each
 * curve in order to fit against as much of the remaining signal as it can. 
 * implementations of this interface provide different algorithms to determine
 * how much can be fit.
 * 
 * @author NAS
 *
 */
public interface CurveFitter {

	FittingResult fit(ReadOnlySpectrum data, Curve curve);
	
	String name(); 
		
}
