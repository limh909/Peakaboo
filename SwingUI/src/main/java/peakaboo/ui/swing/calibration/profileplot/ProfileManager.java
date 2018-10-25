package peakaboo.ui.swing.calibration.profileplot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cyclops.util.Mutable;
import eventful.EventfulListener;
import eventful.EventfulTypeListener;
import net.sciencestudio.autodialog.model.Parameter;
import net.sciencestudio.autodialog.model.style.editors.TextBoxStyle;
import net.sciencestudio.autodialog.view.swing.SwingAutoPanel;
import peakaboo.calibration.CalibrationProfile;
import peakaboo.calibration.CalibrationReference;
import peakaboo.common.PeakabooLog;
import peakaboo.controller.plotter.PlotController;
import peakaboo.controller.plotter.PlotController.UpdateType;
import peakaboo.controller.plotter.calibration.CalibrationController;
import peakaboo.controller.plotter.fitting.FittingController;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.curvefit.peak.transition.TransitionSeriesType;
import peakaboo.ui.swing.calibration.picker.ReferencePicker;
import peakaboo.ui.swing.plotting.PlotPanel;
import swidget.dialogues.fileio.SimpleFileExtension;
import swidget.dialogues.fileio.SwidgetFilePanels;
import swidget.icons.StockIcon;
import swidget.widgets.Spacing;
import swidget.widgets.buttons.ImageButton;
import swidget.widgets.buttons.ToolbarImageButton;
import swidget.widgets.layerpanel.LayerDialog;
import swidget.widgets.layerpanel.ModalLayer;
import swidget.widgets.layerpanel.ToastLayer;
import swidget.widgets.layerpanel.LayerDialog.MessageType;
import swidget.widgets.layout.ButtonBox;
import swidget.widgets.layout.HeaderBox;
import swidget.widgets.layout.HeaderTabBuilder;

public class ProfileManager extends JPanel {

	private PlotController controller;
	private PlotPanel parent;
	
	private HeaderBox header;
	protected JPanel body;
	protected List<ProfilePlot> profileplots = new ArrayList<>();

	
	private JPanel namePanel;
	private JTextField nameField;
	private boolean nameShown = false;

	
	private ImageButton create, open, clear, save;
	
	public ProfileManager(PlotPanel parent, PlotController controller, Runnable onClose) {
		super();
		this.controller = controller;
		this.parent = parent;
		
		
		EventfulTypeListener<String> listener = t -> {
			CalibrationProfile profile = controller.calibration().getCalibrationProfile();
			File file = controller.calibration().getCalibrationProfileFile();
			if (profile == null) {
				return;
			}
			for (ProfilePlot plot : profileplots) {
				plot.setCalibrationProfile(profile, file);
			}
			
			//show the name field if a ref is set, because it means we're creating
			if (controller.calibration().hasCalibrationReference()) {
				showNamePanel();				
			} else {
				hideNamePanel();
			}
			
			save.setEnabled(controller.calibration().hasCalibrationReference());
			
		};
		controller.addListener(listener);
		
		
		ImageButton cancel = new ImageButton(StockIcon.WINDOW_CLOSE).withTooltip("Close").withBordered(false).withAction(() -> {
			controller.removeListener(listener);
			onClose.run();
		});
		
		ButtonBox box = new ButtonBox(Spacing.tiny, false);
		box.setOpaque(false);
		
		create = new ImageButton(StockIcon.DOCUMENT_NEW).withTooltip("Create Z-Calibration").withBordered(false).withAction(() -> {
			actionLoadCalibrationReference();
		});
		box.addLeft(create);
		
		open = new ImageButton(StockIcon.DOCUMENT_OPEN).withTooltip("Load Z-Calibration").withBordered(false).withAction(() -> {
			controller.calibration().clearCalibrationReference();
			actionLoadCalibrationProfile();
		});
		box.addLeft(open);
		
		clear = new ImageButton(StockIcon.EDIT_CLEAR).withTooltip("Clear Z-Calibration").withBordered(false).withAction(() -> {
			controller.calibration().setCalibrationProfile(new CalibrationProfile(), null);
			
		});
		box.addLeft(clear);
		
		save = new ImageButton(StockIcon.DOCUMENT_SAVE_AS).withTooltip("Save New Z-Calibration").withBordered(false).withAction(() -> {
			actionSaveCalibrationProfile();
		});
		box.addLeft(save);
		
		
		makeNamePanel();
		

		
		init(controller.calibration().getCalibrationProfile(), controller.calibration().getCalibrationProfileFile(), box, cancel);
		
	}
	

	protected void init(CalibrationProfile profile, File source, JComponent left, JComponent right) {
		
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(700, 350));
		
		body = new JPanel(new BorderLayout());
		this.add(body, BorderLayout.CENTER);
		
		//plot views
		HeaderTabBuilder tabBuilder = new HeaderTabBuilder();
		
		ProfilePlot kplot = new ProfilePlot(profile, source, TransitionSeriesType.K);
		ProfilePlot lplot = new ProfilePlot(profile, source, TransitionSeriesType.L);
		ProfilePlot mplot = new ProfilePlot(profile, source, TransitionSeriesType.M);
		profileplots.add(kplot);
		profileplots.add(lplot);
		profileplots.add(mplot);

		
		
		tabBuilder.addTab("K Series", kplot);
		tabBuilder.addTab("L Series", lplot);
		tabBuilder.addTab("M Series", mplot);
		
		body.add(tabBuilder.getBody(), BorderLayout.CENTER);
				
		header = new HeaderBox(left, tabBuilder.getTabStrip(), right);
		this.add(header, BorderLayout.NORTH);
	}

	
	
	public void actionSaveCalibrationProfile() {
		
		//generate profile
		CalibrationProfile profile = controller.calibration().generateCalibrationProfile();
		if (profile == null) {
			LayerDialog layer = new LayerDialog("Failed to Generate Profile", "Peakaboo could not generate a calibration profile", MessageType.ERROR);
			layer.showIn(parent);
			return;
		}

		//apply name field to new profile
		String name = nameField.getText();		
		if (name == null || name.trim().length() == 0) {
			ToastLayer toast = new ToastLayer(parent, "Z-Calibration Profile must have a name");
			parent.pushLayer(toast);
			return;
		}
		profile.setName(name);
		
		
		//save the profile
		saveCalibrationProfile(profile);
		
	
	}
	
	private void makeNamePanel() {
		namePanel = new JPanel(new BorderLayout(Spacing.small, Spacing.small));
		namePanel.setBorder(Spacing.bMedium());
		nameField = new JTextField();
		namePanel.add(nameField, BorderLayout.CENTER);
		namePanel.add(new JLabel("Name"), BorderLayout.WEST);

	}
	
	private void showNamePanel() {
		if (nameShown) { return; }
		nameShown = true;
		body.add(namePanel, BorderLayout.NORTH);
		if (controller.calibration().hasCalibrationReference()) {
			nameField.setText(controller.calibration().getCalibrationReference().getName());
		}
	}
	
	private void hideNamePanel() {
		if (!nameShown) { return; }
		nameShown = false;
		body.remove(namePanel);
	}


	private void saveCalibrationProfile(CalibrationProfile profile) {
		String yaml = CalibrationProfile.save(profile);
		
		SimpleFileExtension ext = new SimpleFileExtension("Peakaboo Calibration Profile", "pbcp");
		SwidgetFilePanels.saveFile(parent, "Save Calibration Profile", parent.saveFilesFolder, ext, file -> {
			if (!file.isPresent()) { return; }
			File f = file.get();
			FileWriter writer;
			try {
				writer = new FileWriter(f);
				writer.write(yaml);
				writer.close();

				actionLoadCalibrationProfileFromFile(f);
				
			} catch (IOException e) {
				PeakabooLog.get().log(Level.SEVERE, "Failed to save calibration file", e);
			}

		});
	}
	
	
	public void actionLoadCalibrationProfile() {
		SwidgetFilePanels.openFile(this, "Select Calibration Profile", null, new SimpleFileExtension("Peakaboo Calibration Profile", "pbcp"), result -> {
			if (!result.isPresent()) {
				return;
			}
			
			actionLoadCalibrationProfileFromFile(result.get());
		});
	}
	
	public void actionLoadCalibrationProfileFromFile(File file) {
		try {
			CalibrationProfile profile = CalibrationProfile.load(new String(Files.readAllBytes(file.toPath())));
			controller.calibration().setCalibrationProfile(profile, file);
			
		} catch (IOException e1) {
			PeakabooLog.get().log(Level.SEVERE, "Could not load calibration profile", e1);
		}
	}
		
	
	public void actionLoadCalibrationReference() {
		
		ReferencePicker picker = new ReferencePicker(parent);
		ModalLayer layer = new ModalLayer(parent, picker);
		
		picker.setOnOK(ref -> {
			controller.calibration().loadCalibrationReference(ref);
			parent.removeLayer(layer);
		});
		
		picker.setOnCancel(() -> {
			parent.removeLayer(layer);
		});
		
		parent.pushLayer(layer);
		
	}
	
	
}