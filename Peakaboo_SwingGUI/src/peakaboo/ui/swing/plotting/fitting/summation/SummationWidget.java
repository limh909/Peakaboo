package peakaboo.ui.swing.plotting.fitting.summation;



import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;

import fava.*;
import fava.signatures.FunctionMap;
import static fava.Fn.*;

import peakaboo.controller.plotter.FittingController;
import peakaboo.datatypes.DataTypeFactory;
import peakaboo.datatypes.peaktable.TransitionSeries;
import peakaboo.datatypes.peaktable.TransitionSeriesMode;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.ClearPanel;
import swidget.widgets.ImageButton;
import swidget.widgets.ImageButton.Layout;



class SummationWidget extends ClearPanel
{

	private FittingController	controller;
	private List<TSSelector>	selectors;
	private ImageButton			addButton;


	public SummationWidget(FittingController controller)
	{
		this.controller = controller;

		setLayout(new GridBagLayout());

		selectors = DataTypeFactory.<TSSelector> list();

		addButton = new ImageButton(StockIcon.EDIT_ADD, "Add", Layout.IMAGE, IconSize.BUTTON);
		addButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				addTSSelector();
			}
		});

		resetSelectors();
	}
	
	
	public void resetSelectors()
	{

		selectors.clear();

		selectors.add(new TSSelector(controller, this));
		selectors.add(new TSSelector(controller, this));

		refreshGUI();

	}
	
	
	public TransitionSeries getTransitionSeries()
	{

		//get a list of all TransitionSeries to be summed
		List<TransitionSeries> tss = filter(map(selectors, new FunctionMap<TSSelector, TransitionSeries>() {

			public TransitionSeries f(TSSelector element)
			{
				return element.getTransitionSeries();
			}
		}), Functions.<TransitionSeries>notNull());
		
		return TransitionSeries.summation(tss);

	}
	
	
	
	
	


	protected void removeTSSelector(TSSelector tssel)
	{
		selectors.remove(tssel);
		if (selectors.size() < 2) addTSSelector();
		refreshGUI();
	}


	protected void addTSSelector()
	{
		selectors.add(new TSSelector(controller, this));
		refreshGUI();
	}





	private void refreshGUI()
	{

		removeAll();

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = 0.0;

		for (TSSelector tss : selectors)
		{
			c.gridy += 1;

			c.gridx = 0;
			c.weightx = 1.0;
			add(tss, c);

			c.gridx = 1;
			c.weightx = 0.0;
			add(removeButtonWidget(tss), c);


		}

		c.gridy++;
		c.gridx = 1;
		add(addButton, c);

		c.gridy++;
		c.gridx = 0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		add(new ClearPanel(), c);

		revalidate();

		TSSelectorUpdated();


	}


	private ImageButton removeButtonWidget(final TSSelector tss)
	{
		ImageButton remove = new ImageButton(StockIcon.EDIT_DELETE, "Remove", Layout.IMAGE, IconSize.BUTTON);

		remove.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				removeTSSelector(tss);
			}
		});

		return remove;
	}


	protected void TSSelectorUpdated()
	{
		controller.clearProposedTransitionSeries();
		TransitionSeries ts = getTransitionSeries();
		if (ts == null) return;
		controller.addProposedTransitionSeries(ts);
	}



}



class TSSelector extends ClearPanel
{

	JComboBox	tsCombo;


	public TSSelector(FittingController controller, final SummationWidget owner)
	{

		setLayout(new BorderLayout());



		tsCombo = new JComboBox(
				filter(controller.getFittedTransitionSeries(), new FunctionMap<TransitionSeries, Boolean>() {

					public Boolean f(TransitionSeries element)
					{
						return element.mode == TransitionSeriesMode.PRIMARY;
					}
				}).toArray()
				);

		tsCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				owner.TSSelectorUpdated();
			}
		});

		add(tsCombo, BorderLayout.CENTER);


	}


	public TransitionSeries getTransitionSeries()
	{
		return (TransitionSeries) tsCombo.getSelectedItem();
	}

}
