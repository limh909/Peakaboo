package peakaboo.display.calibration;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cyclops.Bounds;
import cyclops.Coord;
import cyclops.ISpectrum;
import cyclops.Spectrum;
import cyclops.visualization.Surface;
import cyclops.visualization.drawing.DrawingRequest;
import cyclops.visualization.drawing.ViewTransform;
import cyclops.visualization.drawing.painters.axis.AxisPainter;
import cyclops.visualization.drawing.painters.axis.LineAxisPainter;
import cyclops.visualization.drawing.painters.axis.TitleAxisPainter;
import cyclops.visualization.drawing.plot.PlotDrawing;
import cyclops.visualization.drawing.plot.painters.PlotPainter;
import cyclops.visualization.drawing.plot.painters.PlotPainter.TraceType;
import cyclops.visualization.drawing.plot.painters.axis.GridlinePainter;
import cyclops.visualization.drawing.plot.painters.axis.TickMarkAxisPainter;
import cyclops.visualization.drawing.plot.painters.axis.TickMarkAxisPainter.TickFormatter;
import cyclops.visualization.drawing.plot.painters.plot.AreaPainter;
import cyclops.visualization.palette.PaletteColour;
import peakaboo.calibration.Concentrations;
import peakaboo.curvefit.peak.table.Element;

public class ConcentrationPlot {

	private Concentrations comp;
	
	private PlotDrawing plotDrawing;
	private Spectrum calibratedData;
	private Spectrum uncalibratedData;
	private DrawingRequest dr = new DrawingRequest();
	private List<PlotPainter> plotPainters;
	private List<AxisPainter> axisPainters;
		
	public ConcentrationPlot(Concentrations conc) {
		this.comp = conc;
		calculateData();
		configure();
	}
	
	protected void configure() {
	
		dr.dataHeight = 1;
		dr.dataWidth = calibratedData.size();
		dr.drawToVectorSurface = false;
		dr.maxYIntensity = Math.max(uncalibratedData.max(), calibratedData.max());
		dr.unitSize = 1f;
		dr.viewTransform = ViewTransform.LINEAR;
		
		plotPainters = new ArrayList<>();
		plotPainters.add(new GridlinePainter(new Bounds<Float>(0f, dr.maxYIntensity*100f)));

		plotPainters.add(new AreaPainter(calibratedData, 
				new PaletteColour(0xff00897B), 
				new PaletteColour(0xff00796B), 
				new PaletteColour(0xff004D40)
			).withTraceType(TraceType.BAR));
		
		plotPainters.add(new AreaPainter(uncalibratedData, 
				new PaletteColour(0xff89000e), 
				new PaletteColour(0xff79000e), 
				new PaletteColour(0xff4d000d)
			).withTraceType(TraceType.BAR));
		

		List<Element> elements = comp.elementsByConcentration();

		axisPainters = new ArrayList<>();
		
		axisPainters.add(new TitleAxisPainter(TitleAxisPainter.SCALE_TEXT, "Concentrations vs. " + comp.getProfile().getReference().getAnchor().getElement(), null, null, "Elements - Calibrated With " + comp.getProfile().getName()));
		NumberFormat format = new DecimalFormat("0.0");
		Function<Integer, String> sensitivityFormatter = i -> format.format(  ((float)i/10000f)  ) + "%";
		axisPainters.add(new TickMarkAxisPainter(
				new TickFormatter(0f, dr.maxYIntensity, sensitivityFormatter), 
				new TickFormatter(-0.5f, calibratedData.size()-1-0.5f+0.999f, i -> elements.get(i).name()), 
				null, 
				new TickFormatter(0f, dr.maxYIntensity, sensitivityFormatter),
				false, 
				false));
		axisPainters.add(new LineAxisPainter(true, true, false, true));
	}

	public PlotDrawing draw(Surface context, Coord<Integer> size) {

		context.setSource(new PaletteColour(0xffffffff));
		context.rectAt(0, 0, size.x, size.y);
		context.fill();
		
		dr.imageWidth = size.x;
		dr.imageHeight = size.y;
		plotDrawing = new PlotDrawing(context, dr, plotPainters, axisPainters);	
		plotDrawing.draw();
		
		return plotDrawing;
	}
	
	private void calculateData() {	
		
		List<Element> es = comp.elementsByConcentration();
		calibratedData = new ISpectrum(es.size());
		uncalibratedData = new ISpectrum(es.size());
		for (Element e : es) {
			if (comp.isCalibrated(e)) {
				calibratedData.add(comp.getPercent(e));
				uncalibratedData.add(0);
			} else {
				calibratedData.add(0);
				uncalibratedData.add(comp.getPercent(e));
			}
			
		}
		
	}

	
}
