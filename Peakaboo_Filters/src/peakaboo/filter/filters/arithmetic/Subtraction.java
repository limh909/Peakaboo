package peakaboo.filter.filters.arithmetic;


import peakaboo.filter.AbstractFilter;
import peakaboo.filter.Parameter;
import peakaboo.filter.Parameter.ValueType;
import scidraw.drawing.plot.painters.PlotPainter;
import scitypes.Spectrum;
import scitypes.SpectrumCalculations;


public class Subtraction extends AbstractFilter
{

	private static final int AMOUNT = 0;
	
	public Subtraction()
	{
		super();
	}
	
	@Override
	public void initialize()
	{
		parameters.put(AMOUNT, new Parameter(ValueType.REAL, "Amount to Subtract", 1.0));
	}
	
	@Override
	protected Spectrum filterApplyTo(Spectrum data, boolean cache)
	{
		return SpectrumCalculations.subtractFromList(data, getParameter(AMOUNT).realValue());
	}


	@Override
	public String getFilterDescription()
	{
		// TODO Auto-generated method stub
		return "The" + getFilterName() + " filter subtracts a constant value to all points on a spectrum.";
	}


	@Override
	public String getFilterName()
	{
		// TODO Auto-generated method stub
		return "Subtract";
	}


	@Override
	public FilterType getFilterType()
	{
		// TODO Auto-generated method stub
		return FilterType.ARITHMETIC;
	}


	@Override
	public PlotPainter getPainter()
	{
		return null;
	}


	@Override
	public boolean validateParameters()
	{
		return true;
	}

	@Override
	public boolean showFilter()
	{
		return true;
	}
	
	
	@Override
	public boolean canFilterSubset()
	{
		return true;
	}
	
}
