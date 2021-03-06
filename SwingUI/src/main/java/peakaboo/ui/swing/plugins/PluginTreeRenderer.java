package peakaboo.ui.swing.plugins;

import java.awt.Component;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sciencestudio.bolt.plugin.core.BoltPlugin;
import net.sciencestudio.bolt.plugin.core.BoltPluginManager;
import net.sciencestudio.bolt.plugin.core.BoltPluginPrototype;
import peakaboo.calibration.CalibrationReference;
import peakaboo.datasink.plugin.JavaDataSinkPlugin;
import peakaboo.datasource.plugin.JavaDataSourcePlugin;
import peakaboo.filter.plugins.JavaFilterPlugin;
import swidget.icons.IconFactory;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.Spacing;

public class PluginTreeRenderer extends DefaultTreeCellRenderer {
	
	private boolean init = false;
	
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel,
            boolean expanded,
            boolean leaf, int row,
            boolean hasFocus) {
    	
    	super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    	
    	if (init == false) {
    		init = true;
    		setBorder(Spacing.bSmall());
    	}
    	
    	DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    	Object object = node.getUserObject();
    	if (object instanceof BoltPluginManager) {
    		BoltPluginManager<?> manager = (BoltPluginManager<?>) object;
        	setText(manager.getInterfaceName());
        	setIcon(StockIcon.PLACE_FOLDER.toImageIcon(IconSize.BUTTON));
    	} else if (object instanceof BoltPluginPrototype) {
        	BoltPluginPrototype<? extends BoltPlugin> plugin = (BoltPluginPrototype<? extends BoltPlugin>)object;
        	setText(plugin.getName());
        	setIcon(getIcon(plugin));	
    	}
    	

    	return this;
    	
    }
    
	private ImageIcon getIcon(BoltPluginPrototype<? extends BoltPlugin> plugin) {
		Class<? extends BoltPlugin> pluginBaseClass = plugin.getPluginClass();
		
		if (pluginBaseClass == JavaDataSourcePlugin.class) {
			return StockIcon.DOCUMENT_IMPORT.toImageIcon(IconSize.BUTTON);
		}
		
		if (pluginBaseClass == JavaDataSinkPlugin.class) {
			return StockIcon.DOCUMENT_EXPORT.toImageIcon(IconSize.BUTTON);
		}
		
		if (pluginBaseClass == JavaFilterPlugin.class) {
			return StockIcon.MISC_EXECUTABLE.toImageIcon(IconSize.BUTTON);
		}
		
		if (pluginBaseClass == CalibrationReference.class) {
			return IconFactory.getImageIcon("calibration", IconSize.BUTTON);
		}
		
		return null;
	}

}
