package peakaboo.ui.swing.calibration.composition;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JPanel;

import peakaboo.calibration.Composition;
import peakaboo.common.PeakabooLog;
import peakaboo.curvefit.peak.table.Element;
import peakaboo.curvefit.peak.transition.ITransitionSeries;
import stratus.controls.ButtonLinker;
import swidget.dialogues.fileio.SimpleFileExtension;
import swidget.dialogues.fileio.SwidgetFilePanels;
import swidget.icons.StockIcon;
import swidget.widgets.buttons.ImageButton;
import swidget.widgets.buttons.ImageButtonSize;
import swidget.widgets.layerpanel.HeaderLayer;
import swidget.widgets.layerpanel.LayerPanel;
import swidget.widgets.layerpanel.ToastLayer;
import swidget.widgets.layout.HeaderTabBuilder;

public class CompositionView extends HeaderLayer {

	private Composition conc;
	private LayerPanel parent;
	
	public CompositionView(Composition conc, LayerPanel parent) {
		super(parent);
		this.conc = conc;
		this.parent = parent;
		
		getContentRoot().setPreferredSize(new Dimension(700, 350));
		
		
		ImageButton save = new ImageButton(StockIcon.DOCUMENT_SAVE_AS)
				.withButtonSize(ImageButtonSize.LARGE)
				.withTooltip("Save composition as text")
				.withAction(this::saveData);
		
		ImageButton copy = new ImageButton(StockIcon.EDIT_COPY)
				.withButtonSize(ImageButtonSize.LARGE)
				.withTooltip("Copy composition to clipboard")
				.withAction(this::copyData);
		
		ButtonLinker linker = new ButtonLinker(save, copy);
		
		
		HeaderTabBuilder tabs = new HeaderTabBuilder();
		tabs.addTab("Chart", buildChart());
		tabs.addTab("Table", buildTable());
		
		setBody(tabs.getBody());
		getHeader().setLeft(linker);
		getHeader().setCentre(tabs.getTabStrip());
		
	}

	private JPanel buildChart() {
		JPanel chart = new CompositionPlotPanel(conc);
		return chart;
	}
	
	private JPanel buildTable( ) {
		JPanel table = new CompositionTablePanel(conc);
		return table;
	}


	private String textData() {
		List<Element> es = conc.elementsByConcentration();
		StringBuilder sb = new StringBuilder();
		for (Element e : es) {
			sb.append(e.name());
			sb.append(": ");
			sb.append(conc.getPercent(e));
			
			
			ITransitionSeries ts = conc.getCompositionSource(e);
			if (ts == null) {
				sb.append(" (No Source!)");
			} else if (!conc.isCalibrated(e)) {
				sb.append(" (Not Calibrated!)");
			} else {
				/*
				 * We don't include this in the text output to avoid clutter and make the text
				 * easier to use in other documents. Missing calibration still requires a
				 * notice, though.
				 */
			}
			
			
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private void copyData() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection sel = new StringSelection(textData());
		clipboard.setContents(sel, null);
		
		ToastLayer toast = new ToastLayer(parent, "Data copied to clipboard");
		parent.pushLayer(toast);
		
	}
	
	private void saveData() {
		
		//TODO: starting folder
		SwidgetFilePanels.saveFile(parent, "Save Composition Data", null, new SimpleFileExtension("Text File", "txt"), result -> {
			if (!result.isPresent()) {
				return;
			}
			
			try {
				File f = result.get();
				FileWriter writer = new FileWriter(f);
				writer.write(textData());
				writer.close();
			} catch (IOException e) {
				PeakabooLog.get().log(Level.SEVERE, "Failed to save composition data", e);
			}
			
		});
		
	}
	
	
	
}
