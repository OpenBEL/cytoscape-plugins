/*
 * BEL Framework Webservice Plugin
 *
 * URLs: http://openbel.org/
 * Copyright (C) 2012, Selventa
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openbel.belframework.webservice;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.JMenu;

import cytoscape.Cytoscape;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;

/**
 * The {@link CytoscapePlugin cytoscape plugin} class for the BELFramework
 * webservice client plugin.  This exposes the webservice stub through the
 * {@link WebServiceClientManager cytoscape webservice manager}.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class ClientPlugin extends CytoscapePlugin {
    public static final String KAM_PLUGIN_SUBMENU = "KAM Navigator";

    /**
     * Default no-arg plugin construtor to initialize this plugin.  This plugin
     * contributes the <em>BELFramework Configuration</em> item to the
     * <em>Plugins</em> menu.
     */
    public ClientPlugin() {
        onCytoscapeStart();

        WebServiceClientManager.registerClient(ClientConnector.getInstance());

        final JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus()
                .getOperationsMenu();

        JMenu kiMenu = null;
        for (final Component menu : pluginMenu.getMenuComponents()) {
            if (menu == null) {
                continue;
            }

            if (menu instanceof JMenu
                    && KAM_PLUGIN_SUBMENU.equals(((JMenu) menu).getText())) {
                kiMenu = (JMenu) menu;
                break;
            }
        }

        if (kiMenu == null) {
            kiMenu = new JMenu(KAM_PLUGIN_SUBMENU);
            pluginMenu.add(kiMenu);
        }

        // add to "KAM Navigator" menu if KAM Plugin is available
        final SettingsDialogAction settingsAction = new SettingsDialogAction();
        kiMenu.add(settingsAction);
    }

    /**
     * Configures the webservice plugin from properties file.
     * <p>
     * Note: This method is called when cytoscape starts up.
     * </p>
     */
    public void onCytoscapeStart() {
        try {
            Configuration.getInstance().restoreState();
        } catch (IOException e) {
            // bad, so reset to defaults
            Configuration.resetToDefaults();
        }
    }

    /**
     * Saves webservice plugin configuration to a properties file.
     * <p>
     * Note: This method is called when cytoscape shuts down.
     * </p>
     */
    @Override
    public void onCytoscapeExit() {
        try {
            Configuration.getInstance().saveState();
        } catch (IOException e) {
            // bad, but what can I do?
            e.printStackTrace();
        }
    }

    /**
     * Defines a {@link CytoscapeAction cytoscape action} to launch the
     * <em>BELFramework Configuration</em> dialog.  This allows the cytoscape
     * user to configure their access to the BELFramework Web API.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class SettingsDialogAction extends CytoscapeAction {
        private static final long serialVersionUID = 5424095704897475438L;

        public SettingsDialogAction() {
            super("BELFramework Configuration");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            SettingsDialog settingsDialog = new SettingsDialog();
            settingsDialog.setVisible(true);
        }
    }
}
