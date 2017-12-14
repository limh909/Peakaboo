package peakaboo.filter.plugins.advanced;

import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.Filter;
import scitypes.ReadOnlySpectrum;

public class Identity extends AbstractSimpleFilter
{

	@Override
	public boolean canFilterSubset()
	{
		return true;
	}

	@Override
	public String pluginVersion() {
		return "1.0";
	}

	@Override
	protected ReadOnlySpectrum filterApplyTo(ReadOnlySpectrum data)
	{
		return data;
	}


	@Override
	public String getFilterDescription()
	{
		return "This filter is the identity function -- it does no processing to the data";
	}


	@Override
	public String getFilterName()
	{
		return "None";
	}


	@Override
	public Filter.FilterType getFilterType()
	{
		return Filter.FilterType.ADVANCED;
	}


	@Override
	public void initialize()
	{

	}


	@Override
	public boolean pluginEnabled()
	{
		return false;
	}


	@Override
	public boolean validateParameters()
	{
		return true;
	}

}
