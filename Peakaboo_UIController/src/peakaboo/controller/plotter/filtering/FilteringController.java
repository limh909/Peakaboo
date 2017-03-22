package peakaboo.controller.plotter.filtering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eventful.Eventful;
import peakaboo.controller.plotter.IPlotController;
import peakaboo.filter.controller.IFilteringController;
import peakaboo.filter.model.AbstractFilter;
import peakaboo.filter.model.FilterSet;
import peakaboo.filter.model.FilteringModel;
import scitypes.Spectrum;


public class FilteringController extends Eventful implements IFilteringController
{

	IPlotController	plot;
	FilteringModel	filteringModel;
	
	public FilteringController(IPlotController plotController)
	{
		this.plot = plotController;
		filteringModel = new FilteringModel();
	}

	@Override
	public FilteringModel getFilteringMode()
	{
		return filteringModel;
	}

	@Override
	public void clearFilters()
	{
		filteringModel.filters.clearFilters();
		plot.history().setUndoPoint("Clear Filters");
		filteredDataInvalidated();
	}

	@Override
	public List<String> getAvailableFiltersByName()
	{
		List<String> filterNames = new ArrayList<String>();

		for (AbstractFilter filter : filteringModel.filters.getAvailableFilters())
		{
			filterNames.add(filter.getFilterName());
		}

		Collections.sort(filterNames);

		return filterNames;
	}

	@Override
	public List<AbstractFilter> getAvailableFilters()
	{
		return filteringModel.filters.getAvailableFilters();
	}

	@Override
	public void addFilter(String name)
	{

		for (AbstractFilter f : filteringModel.filters.getAvailableFilters())
		{
			if (f.getFilterName().equals(name))
			{

				try
				{
					// this will call filterschanged, so we don't need to
					// manually update the listeners
					AbstractFilter filter = f.getClass().newInstance();
					filter.initialize();
					addFilter(filter);
					break;
				}
				catch (InstantiationException e)
				{
					e.printStackTrace();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}

			}
		}

	}

	@Override
	public void addFilter(AbstractFilter f)
	{
		filteringModel.filters.addFilter(f);
		plot.history().setUndoPoint("Add Filter");
		filteredDataInvalidated();
	}

	@Override
	public void removeFilter(int index)
	{
		filteringModel.filters.removeFilter(index);
		plot.history().setUndoPoint("Remove Filter");
		filteredDataInvalidated();
	}

	@Override
	public boolean filterSetContains(AbstractFilter f)
	{
		return filteringModel.filters.contains(f);
	}

	@Override
	public int getFilterCount()
	{
		return filteringModel.filters.size();
	}

	@Override
	public void setFilterEnabled(int index, boolean enabled)
	{
		filteringModel.filters.setFilterEnabled(index, enabled);
		plot.history().setUndoPoint("Enable Filter");
		filteredDataInvalidated();
	}

	@Override
	public boolean getFilterEnabled(int index)
	{
		return filteringModel.filters.getFilterEnabled(index);
	}

	@Override
	public void moveFilterUp(int index)
	{
		filteringModel.filters.moveFilterUp(index);
		plot.history().setUndoPoint("Move Filter Up");
		filteredDataInvalidated();
	}

	@Override
	public void moveFilterDown(int index)
	{
		filteringModel.filters.moveFilterDown(index);
		plot.history().setUndoPoint("Move Filter Down");
		filteredDataInvalidated();
	}

	@Override
	public AbstractFilter getActiveFilter(int index)
	{
		return filteringModel.filters.getFilter(index);
	}

	@Override
	public int filterIndex(AbstractFilter f)
	{
		return filteringModel.filters.indexOf(f);
	}


	@Override
	public void calculateFilteredData(Spectrum data)
	{
		filteringModel.filteredPlot = filteringModel.filters.filterData(data, true);
		updateListeners();
	}

	@Override
	public void filteredDataInvalidated()
	{
		// Clear cached values, since they now have to be recalculated
		filteringModel.filteredPlot = null;
		plot.data().invalidateFilteredData();

		plot.fitting().fittingDataInvalidated();
		updateListeners();

	}

	@Override
	public FilterSet getActiveFilters()
	{
		return filteringModel.filters;
	}

	@Override
	public Spectrum getFilteredPlot()
	{
		return filteringModel.filteredPlot;
	}
	
}
