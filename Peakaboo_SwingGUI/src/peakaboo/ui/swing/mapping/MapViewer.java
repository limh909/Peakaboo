package peakaboo.ui.swing.mapping;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import peakaboo.controller.mapper.MapController;
import peakaboo.controller.mapper.SingleMapModel;
import peakaboo.datatypes.eventful.PeakabooSimpleListener;
import peakaboo.ui.swing.PeakabooMapperSwing;
import scitypes.Coord;
import swidget.widgets.Spacing;



public class MapViewer extends JPanel
{

	private JPanel			canvas;

	protected MapController	controller;

	private JLabel			warnOnTooSmallDataset;
	private JLabel			mapMouseMonitor;

	private SingleMapModel	viewModel;


	public MapViewer(SingleMapModel viewModel, MapController controller, PeakabooMapperSwing owner)
	{

		this.controller = controller;
		this.viewModel = viewModel;

		addComponentListener(new ComponentListener() {

			public void componentShown(ComponentEvent e)
			{
			}


			public void componentResized(ComponentEvent e)
			{
				setNeedsRedraw();
				repaint();
			}


			public void componentMoved(ComponentEvent e)
			{
			}


			public void componentHidden(ComponentEvent e)
			{
			}
		});

		controller.addListener(new PeakabooSimpleListener() {

			public void change()
			{
				setNeedsRedraw();
				repaint();
			}
		});

		owner.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				MapViewer.this.viewModel.discard();
			}
		});
		
		init(owner);

	}


	private void init(PeakabooMapperSwing owner)
	{

		setLayout(new BorderLayout());
		add(createMapView(), BorderLayout.CENTER);

		canvas.addMouseMotionListener(new MouseMotionListener() {

			public void mouseMoved(MouseEvent e)
			{
				showValueAtCoord(controller.getMapCoordinateAtPoint(e.getX(), e.getY()));
			}


			public void mouseDragged(MouseEvent e)
			{
				// Nothing to do here

			}
		});

		controller.addListener(new PeakabooSimpleListener() {

			public void change()
			{

				if (controller.getDataHeight() * controller.getDataWidth() == controller.getMapSize())
				{
					warnOnTooSmallDataset.setVisible(false);
				}
				else
				{
					warnOnTooSmallDataset.setVisible(true);
				}

				if (controller.getActiveTabModel() == viewModel) fullRedraw();
			}
		});

		add(new SidePanel(controller, owner), BorderLayout.WEST);

		controller.updateListeners();

	}


	public SingleMapModel getMapViewModel()
	{
		return viewModel;
	}


	private JPanel createMapView()
	{

		JPanel pane = new JPanel();

		pane.setLayout(new BorderLayout());

		pane.add(createCanvasPanel(), BorderLayout.CENTER);

		return pane;

	}


	private JPanel createCanvasPanel()
	{
		canvas = new JPanel(true) {

			@Override
			public void paint(Graphics g)
			{
				MapViewer.this.paintCanvasEvent(g);
			}
		};

		JPanel canvasContainer = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		canvasContainer.add(canvas, c);

		warnOnTooSmallDataset = new JLabel("Warning: Map dimensions are smaller than data set ("
				+ controller.getMapSize() + ")");
		warnOnTooSmallDataset.setBorder(Spacing.bSmall());
		warnOnTooSmallDataset.setBackground(new Color(0.64f, 0.0f, 0.0f));
		warnOnTooSmallDataset.setForeground(new Color(1.0f, 1.0f, 1.0f));
		warnOnTooSmallDataset.setOpaque(true);
		warnOnTooSmallDataset.setHorizontalAlignment(SwingConstants.CENTER);
		if (controller.getDataHeight() * controller.getDataWidth() == controller.getMapSize())
		{
			warnOnTooSmallDataset.setVisible(false);
		}
		else
		{
			warnOnTooSmallDataset.setVisible(true);
		}

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		canvasContainer.add(warnOnTooSmallDataset, c);

		mapMouseMonitor = new JLabel("");
		mapMouseMonitor.setBorder(Spacing.bSmall());
		mapMouseMonitor.setHorizontalAlignment(JLabel.CENTER);
		mapMouseMonitor.setFont(mapMouseMonitor.getFont().deriveFont(Font.PLAIN));
		showValueAtCoord(null);

		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		canvasContainer.add(mapMouseMonitor, c);

		return canvasContainer;
	}


	public void fullRedraw()
	{

		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			public void run()
			{

				MapViewer.this.validate();
				repaint();

			}
		});

	}


	public void setNeedsRedraw()
	{
		controller.setNeedsRedraw();
	}


	/*
	 * METHODS FOR HANDLING CANVAS EVENTS
	 */

	public void paintCanvasEvent(Graphics g)
	{

		controller.setImageWidth(canvas.getWidth());
		controller.setImageHeight(canvas.getHeight());

		g.setColor(new Color(1.0f, 1.0f, 1.0f));
		g.fillRect(0, 0, (int) controller.getImageWidth(), (int) controller.getImageHeight());

		controller.draw(g);

		/*
		 * controller.setLinearSampleStart(new Coord<Integer>(1,1)); controller.setLinearSampleStop(new
		 * Coord<Integer>(10,10));
		 * 
		 * Coord<Integer> startCoord = controller.getPointForMapCoordinate(controller.getLinearSampleStart());
		 * Coord<Integer> stopCoord = controller.getPointForMapCoordinate(controller.getLinearSampleStop());
		 * 
		 * if (startCoord != null && stopCoord != null) {
		 * 
		 * g.drawArc(startCoord.x, startCoord.y, 10, 10, 0, 360); g.drawArc(stopCoord.x, stopCoord.y, 10, 10, 0, 360);
		 * 
		 * }
		 */

	}


	public void showValueAtCoord(Coord<Integer> mapCoord)
	{
		if (mapCoord == null)
		{
			mapMouseMonitor.setText("X: -, Y: -, Value: -");
			return;
		}

		// GridPerspective<Double> mapGrid = new GridPerspective<Double>(controller.getDataWidth(),
		// controller.getDataHeight(), null);
		// Double value = mapGrid.get(controller.getSummedVisibleMaps(), mapCoord.x, mapCoord.y);

		if (controller.isValidPoint(mapCoord))
		{
			mapMouseMonitor.setText("X: " + (mapCoord.x + 1) + ", Y: " + (mapCoord.y + 1) + ", Value: "
					+ controller.getIntensityMeasurementAtPoint(mapCoord));
		}
		else
		{
			mapMouseMonitor.setText("X: -, Y: -, Value: -");
		}

	}

}
