package peakaboo.display.plot.painters;


import java.awt.Color;
import java.util.List;

import cyclops.visualization.drawing.painters.PainterData;
import cyclops.visualization.drawing.plot.PlotDrawing;
import cyclops.visualization.drawing.plot.painters.PlotPainter;
import cyclops.visualization.drawing.plot.painters.plot.PlotPalette;
import peakaboo.curvefit.curve.fitting.FittingResult;
import peakaboo.curvefit.curve.fitting.FittingResultSet;


/**
 * 
 * A {@link PlotPainter} for {@link PlotDrawing}s which draws the data for each {@link FittingResult} in a {@link FittingResultSet}
 * 
 * @author Nathaniel Sherry, 2009
 * 
 */

public class FittingPainter extends PlotPainter
{

	private List<FittingResult>	data;
	private PlotPalette palette;



	/**
	 * 
	 * Create a new FittingPainter
	 * 
	 * @param data the data to draw on the plot
	 * @param stroke the {@link Color} to stroke the data with
	 * @param fill the {@link Color} to fill the data with
	 */
	public FittingPainter(FittingResultSet data, PlotPalette palette)
	{
		this(data.getFits(), palette);
	}
	
	
	/**
	 * 
	 * Create a new FittingPainter
	 * 
	 * @param data the data to draw on the plot
	 * @param stroke the {@link Color} to stroke the data with
	 * @param fill the {@link Color} to fill the data with
	 */
	public FittingPainter(List<FittingResult> data, PlotPalette palette) {
		this.data = data;
		this.palette = palette;
	}



	@Override
	public void drawElement(PainterData p)
	{
		
		for (FittingResult fitResult : data) {

			traceData(p, fitResult.getFit());

			p.context.setSource(palette.fitFill);
			p.context.fillPreserve();

			p.context.setSource(palette.fitStroke);
			p.context.stroke();
		}
		

	}

}
