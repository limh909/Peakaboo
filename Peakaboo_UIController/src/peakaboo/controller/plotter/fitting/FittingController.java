package peakaboo.controller.plotter.fitting;

import static fava.Fn.filter;

import java.util.Comparator;
import java.util.List;

import peakaboo.controller.plotter.PlotController;
import peakaboo.controller.plotter.PlotController.UpdateType;
import peakaboo.controller.plotter.settings.SettingsController;
import peakaboo.curvefit.automation.TSOrdering;
import peakaboo.curvefit.fitting.EscapePeakType;
import peakaboo.curvefit.fitting.FittingSet;
import peakaboo.curvefit.peaktable.PeakTable;
import peakaboo.curvefit.peaktable.TransitionSeries;
import peakaboo.curvefit.peaktable.TransitionSeriesType;
import peakaboo.curvefit.results.FittingResult;
import peakaboo.curvefit.results.FittingResultSet;
import scitypes.Spectrum;
import scitypes.SpectrumCalculations;
import eventful.Eventful;
import eventful.EventfulType;
import fava.Fn;
import fava.Functions;
import fava.datatypes.Pair;
import fava.functionable.FList;
import fava.signatures.FnFold;
import fava.signatures.FnMap;


public class FittingController extends EventfulType<Boolean> implements IFittingController
{

	FittingModel fittingModel;
	PlotController plot;
	
	public FittingController(PlotController plotController)
	{
		this.plot = plotController;
		fittingModel = new FittingModel();
	}
	
	public FittingModel getFittingModel()
	{
		return fittingModel;
	}
	
	private void setUndoPoint(String change)
	{
		plot.undoController.setUndoPoint(change);
	}
	
	
	
	public void addTransitionSeries(TransitionSeries e)
	{
		if (e == null) return;
		fittingModel.selections.addTransitionSeries(e);
		setUndoPoint("Add Fitting");
		fittingDataInvalidated();
	}

	public void addAllTransitionSeries(List<TransitionSeries> tss)
	{
		for (TransitionSeries ts : tss)
		{
			fittingModel.selections.addTransitionSeries(ts);
		}
		setUndoPoint("Add Fittings");
		fittingDataInvalidated();
	}

	public void clearTransitionSeries()
	{
		
		fittingModel.selections.clear();
		setUndoPoint("Clear Fittings");
		fittingDataInvalidated();
	}

	public void removeTransitionSeries(TransitionSeries e)
	{
		
		fittingModel.selections.remove(e);
		setUndoPoint("Remove Fitting");
		fittingDataInvalidated();
	}

	public List<TransitionSeries> getFittedTransitionSeries()
	{
		return fittingModel.selections.getFittedTransitionSeries();
	}

	public List<TransitionSeries> getUnfittedTransitionSeries(final TransitionSeriesType tst)
	{

		final List<TransitionSeries> fitted = getFittedTransitionSeries();


		return filter(PeakTable.getAllTransitionSeries(), new FnMap<TransitionSeries, Boolean>() {


			public Boolean f(TransitionSeries ts)
			{
				return (!fitted.contains(ts)) && tst.equals(ts.type);
			}
		});

	}

	public void setTransitionSeriesVisibility(TransitionSeries e, boolean show)
	{
		fittingModel.selections.setTransitionSeriesVisibility(e, show);
		setUndoPoint("Fitting Visiblitiy");
		fittingDataInvalidated();
	}

	public boolean getTransitionSeriesVisibility(TransitionSeries e)
	{
		return e.visible;
	}

	public List<TransitionSeries> getVisibleTransitionSeries()
	{

		return filter(getFittedTransitionSeries(), new FnMap<TransitionSeries, Boolean>() {


			public Boolean f(TransitionSeries ts)
			{
				return ts.visible;
			}
		});

	}

	public float getTransitionSeriesIntensity(TransitionSeries ts)
	{
		plot.regenerateCahcedData();

		if (fittingModel.selectionResults == null) return 0.0f;

		for (FittingResult result : fittingModel.selectionResults.fits)
		{
			if (result.transitionSeries == ts) {
				float max = SpectrumCalculations.max(result.fit);
				if (Float.isNaN(max)) max = 0f;
				return max;
			}
		}
		return 0.0f;

	}

	public void moveTransitionSeriesUp(TransitionSeries e)
	{
		fittingModel.selections.moveTransitionSeriesUp(e);
		setUndoPoint("Move Fitting Up");
		fittingDataInvalidated();
	}

	public void moveTransitionSeriesUp(List<TransitionSeries> tss)
	{
		fittingModel.selections.moveTransitionSeriesUp(tss);
		setUndoPoint("Move Fitting Up");
		fittingDataInvalidated();
	}
		
	public void moveTransitionSeriesDown(TransitionSeries e)
	{
		fittingModel.selections.moveTransitionSeriesDown(e);
		setUndoPoint("Move Fitting Down");
		fittingDataInvalidated();
	}

	public void moveTransitionSeriesDown(List<TransitionSeries> tss)
	{
		fittingModel.selections.moveTransitionSeriesDown(tss);
		setUndoPoint("Move Fitting Down");
		fittingDataInvalidated();
	}

	public void fittingDataInvalidated()
	{
		// Clear cached values, since they now have to be recalculated
		fittingModel.selectionResults = null;

		// this will call update listener for us
		fittingProposalsInvalidated();

	}

	
	
	public void addProposedTransitionSeries(TransitionSeries e)
	{
		fittingModel.proposals.addTransitionSeries(e);
		fittingProposalsInvalidated();
	}

	public void removeProposedTransitionSeries(TransitionSeries e)
	{
		fittingModel.proposals.remove(e);
		fittingProposalsInvalidated();
	}

	public void clearProposedTransitionSeries()
	{
		fittingModel.proposals.clear();
		fittingProposalsInvalidated();
	}

	public List<TransitionSeries> getProposedTransitionSeries()
	{
		return fittingModel.proposals.getFittedTransitionSeries();
	}

	public void commitProposedTransitionSeries()
	{
		addAllTransitionSeries(fittingModel.proposals.getFittedTransitionSeries());
		fittingModel.proposals.clear();
		fittingDataInvalidated();
	}

	public void fittingProposalsInvalidated()
	{
		// Clear cached values, since they now have to be recalculated
		fittingModel.proposalResults = null;
		updateListeners(false);
	}

	public void setEscapeType(EscapePeakType type)
	{
		fittingModel.selections.setEscapeType(type);
		fittingModel.proposals.setEscapeType(type);
		
		fittingDataInvalidated();
		
		setUndoPoint("Escape Peaks");
		updateListeners(false);
	}
		
	public EscapePeakType getEscapeType()
	{
		return plot.settingsController.getEscapePeakType();
	}
		
	public void optimizeTransitionSeriesOrdering()
	{
		
		
		
		//all visible TSs
		final FList<TransitionSeries> tss = Fn.map(getVisibleTransitionSeries(), Functions.<TransitionSeries>id());
		
		//all invisible TSs
		FList<TransitionSeries> invisibles = Fn.filter(getFittedTransitionSeries(), new FnMap<TransitionSeries, Boolean>() {

			public Boolean f(TransitionSeries element)
			{
				return ! tss.include(element);
			}
		});
		
		
		//find all the TSs which overlap with other TSs
		final FList<TransitionSeries> overlappers = tss.filter(new FnMap<TransitionSeries, Boolean>() {

			public Boolean f(final TransitionSeries ts)
			{
				
				return TSOrdering.getTSsOverlappingTS(ts, tss, plot.settingsController.getEnergyPerChannel(), plot.dataController.getDataWidth(), plot.settingsController.getEscapePeakType()).size() != 0;			
				
			}
		});
		
		
		//then get all the TSs which don't overlap
		FList<TransitionSeries> nonOverlappers = tss.filter(new FnMap<TransitionSeries, Boolean>() {

			public Boolean f(TransitionSeries element)
			{
				return ! overlappers.include(element);
			}
		});
	
		

		//score each of the overlappers w/o competition
		FList<Pair<TransitionSeries, Float>> scoredOverlappers = overlappers.map(new FnMap<TransitionSeries, Pair<TransitionSeries, Float>>() {

			public Pair<TransitionSeries, Float> f(TransitionSeries ts)
			{
				return new Pair<TransitionSeries, Float>(ts, TSOrdering.fScoreTransitionSeries(plot.settingsController.getEscapePeakType(), plot.settingsController.getEnergyPerChannel(), plot.filteringController.getFilteredPlot()).f(ts));
			}
		});
		
		//sort all the overlappig visible elements according to how strongly they would fit on their own (ie no competition)
		Fn.sortBy(scoredOverlappers, new Comparator<Float>() {
			
			public int compare(Float f1, Float f2)
			{
				return (f2.compareTo(f1));
				
			}
		}, Functions.<TransitionSeries, Float>second());
		
		
		
		//find the optimal ordering of the visible overlapping TSs based on how they fit with competition
		FList<TransitionSeries> bestfit = optimizeTSOrderingHelper(scoredOverlappers.map(Functions.<TransitionSeries, Float>first()), new FList<TransitionSeries>());
		

		
		//FList<TransitionSeries> bestfit = TSOrdering.optimizeTSOrdering(getEnergyPerChannel(), tss, filteringController.getFilteredPlot());

		//re-add all of the overlappers
		bestfit.addAll(nonOverlappers);
		
		//re-add all of the invisible TSs
		bestfit.addAll(invisibles);
		
		
		//set the TS selection for the model to be the ordering we have just calculated
		clearTransitionSeries();
		addAllTransitionSeries(bestfit);
		setUndoPoint("Fitting Ordering");
		updateListeners(false);
		
	}
		
	public List<TransitionSeries> proposeTransitionSeriesFromChannel(final int channel, TransitionSeries currentTS)
	{
		
		if (! plot.dataController.hasDataSet() ) return null;
		
		return TSOrdering.proposeTransitionSeriesFromChannel(
				plot.settingsController.getEscapePeakType(),
				plot.settingsController.getEnergyPerChannel(),
				plot.filteringController.getFilteredPlot(),
				fittingModel.selections,
				fittingModel.proposals,
				channel,
				currentTS	
		);
	}

	public boolean canMap()
	{
		return ! (getFittedTransitionSeries().size() == 0 || plot.dataController.datasetScanCount() == 0);
	}

	// =============================================
	// Helper Functions for IFittingController
	// =============================================

	private FList<TransitionSeries> optimizeTSOrderingHelper(FList<TransitionSeries> unfitted, FList<TransitionSeries> fitted)
	{
		
		//assumption: unfitted will be in sorted order based on how well each TS fits independently
		if (unfitted.size() == 0) return fitted;
		
		int n = 4;
		
		FList<TransitionSeries> topn = unfitted.take(n);
		unfitted.removeAll(topn);
		FList<List<TransitionSeries>> perms = Fn.permutations(topn);
				
		//function to score an ordering of Transition Series
		final FnMap<List<TransitionSeries>, Float> scoreTSs = new FnMap<List<TransitionSeries>, Float>() {

			public Float f(List<TransitionSeries> tss)
			{
				
				final FnMap<TransitionSeries, Float> scoreTS = TSOrdering.fScoreTransitionSeries(plot.settingsController.getEscapePeakType(), plot.settingsController.getEnergyPerChannel(), plot.filteringController.getFilteredPlot());
				
				Float score = 0f;
				for (TransitionSeries ts : tss)
				{
					score = scoreTS.f(ts);
				}
				return score;
				

			}
		};
	
		
		//find the best fitting for the currently selected fittings
		FList<TransitionSeries> bestfit = new FList<TransitionSeries>(perms.fold(new FnFold<List<TransitionSeries>, List<TransitionSeries>>() {
			
			public List<TransitionSeries> f(List<TransitionSeries> l1, List<TransitionSeries> l2)
			{
				Float s1, s2; //scores
				s1 = scoreTSs.f(l1);
				s2 = scoreTSs.f(l2);				
				
				if (s1 < s2) return l1;
				return l2;
				
			}
		}));

		
		//add the best half of the fitted elements to the fititngs list
		//and the rest back into the start of the unfitted elements list
		fitted.addAll(bestfit.take(n/2));
		bestfit.removeAll(fitted);
		unfitted.addAll(0, bestfit);
		
		
		//recurse
		return optimizeTSOrderingHelper(unfitted, fitted);

				
	}

	public void setFittingParameters(float energyPerChannel)
	{

		int scanSize = 0;
		
		plot.dr.unitSize = energyPerChannel;
		fittingModel.selections.setDataParameters(scanSize, energyPerChannel, plot.settingsController.getEscapePeakType());
		fittingModel.proposals.setDataParameters(scanSize, energyPerChannel, plot.settingsController.getEscapePeakType());

		setUndoPoint("Calibration");
		plot.filteringController.filteredDataInvalidated();
	}

	
	
	public void calculateProposalFittings()
	{
		fittingModel.proposalResults = fittingModel.proposals.calculateFittings(fittingModel.selectionResults.residual);
	}

	public void calculateSelectionFittings(Spectrum data)
	{
		fittingModel.selectionResults = fittingModel.selections.calculateFittings(data);
	}

	public boolean hasProposalFitting()
	{
		return fittingModel.proposalResults != null;
	}

	public boolean hasSelectionFitting()
	{
		return fittingModel.selectionResults != null;
	}

	public FittingSet getFittingSelections()
	{
		return fittingModel.selections;
	}

	public FittingResultSet getFittingProposalResults()
	{
		return fittingModel.proposalResults;
	}

	public FittingResultSet getFittingSelectionResults()
	{
		return fittingModel.selectionResults;
	}
	
	
}
