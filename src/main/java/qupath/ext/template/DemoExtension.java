package qupath.ext.template;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.extensions.QuPathExtension;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import qupath.lib.gui.prefs.PathPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a demo to provide a template for creating a new QuPath extension in Java.
 * <p>
 * <b>Important!</b> For your extension to work in QuPath, you need to make sure the name & package
 * of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 * </pre>
 */
public class DemoExtension implements QuPathExtension{

    // Defining the variables
    private final String EXTENSION_NAME = "My Java extension";
    private final String EXTENSION_DESCRIPTION = "This is just a demo to show how Java extensions work";
    private final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.4.0");

	private static Property<Integer> numThreadsProperty = PathPrefs.createPersistentPreference(
			"demo.num.threads", 1).asObject();

	private static final Logger logger = LoggerFactory.getLogger(DemoExtension.class);


	private boolean isInstalled = false;
    @Override
    public void installExtension(QuPathGUI qupath) {
		if (isInstalled) {
			logger.debug("{} is already installed", getName());
			return;
		}
		isInstalled = true;
		addMenuItem(qupath);
    }

	@Override
	public String getName() {
		return EXTENSION_NAME;
	}
	@Override
	public String getDescription() {
		return EXTENSION_DESCRIPTION;
	}
	
	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}

	public static Property<Integer> numThreadsProperty() {
		return numThreadsProperty;
	}

	private void addMenuItem(QuPathGUI qupath) {
		var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
		MenuItem menuItem = new MenuItem("My Java menu item");
		// Set the action for the menu item
        menuItem.setOnAction(e -> {
            Dialogs.showMessageDialog(EXTENSION_NAME, "Hello! This is my Java extension.");
        });
		menu.getItems().add(menuItem);
	}
}


