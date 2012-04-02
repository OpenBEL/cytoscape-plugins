/*
 * KAM Navigator Plugin
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
package org.openbel.belframework.kam;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.openbel.belframework.kam.dialog.LoadKAMDialog;
import org.openbel.belframework.kam.dialog.SearchKAMDialog;
import org.openbel.belframework.kam.dialog.SearchKAMListDialog;
import org.openbel.belframework.webservice.SettingsDialog;

import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;

/**
 * The {@link CytoscapePlugin cytoscape plugin} class for the KAM Navigator
 * plugin. This plugin provides:
 * <ul>
 * <li>Load a KAM with Plugins -> KAM Navigator -> Load KAM</li>
 * <li>Add KAM nodes to network with
 * Plugins -> KAM Navigator -> Add KAM Nodes</li>
 * <li>Expand existing network nodes with context-sensitive node actions</li>
 * </ul>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMNavigatorPlugin extends CytoscapePlugin {
    public static final String KAM_PLUGIN_SUBMENU = "KAM Navigator";
    public static final String KAM_NODE_ID_ATTR = "KAM_NODE_ID";
    public static final String KAM_NODE_FUNCTION_ATTR = "KAM_NODE_FUNCTION";
    public static final String KAM_NODE_LABEL_ATTR = "KAM_NODE_LABEL";
    public static final String KAM_EDGE_ID_ATTR = "KAM_EDGE_ID";
    public static final String KAM_NETWORK_CREATED_EVENT = "KAM_NETWORK_CREATED_EVENT";

    /**
     * Default no-arg plugin construtor to initialize this plugin.
     */
    public KAMNavigatorPlugin() {
        // add KAM_NODE_ID as a system node attribute
        Cytoscape.getNodeAttributes().setUserEditable(KAM_NODE_ID_ATTR, false);
        Cytoscape.getNodeAttributes().setUserVisible(KAM_NODE_ID_ATTR, false);

        // add KAM_NODE_FUNCTION as a system node attribute
        Cytoscape.getNodeAttributes().setUserEditable(KAM_NODE_FUNCTION_ATTR, false);
        Cytoscape.getNodeAttributes().setUserVisible(KAM_NODE_FUNCTION_ATTR, true);

        // add KAM_EDGE_ID as a system edge attribute
        Cytoscape.getEdgeAttributes().setUserEditable(KAM_EDGE_ID_ATTR, false);
        Cytoscape.getEdgeAttributes().setUserVisible(KAM_EDGE_ID_ATTR, false);

        // hook up propery change listeners
        final KAMNodeContextListener nctx = new KAMNodeContextListener();
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_CREATED, nctx);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_DESTROYED, nctx);

        // register property change listener for this instance
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                KAM_NETWORK_CREATED_EVENT, this);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_DESTROYED, this);

        // build menu
        final JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus()
                .getOperationsMenu();
        JMenu kiMenu = getKamPluginMenu();

        // add to "KAM Navigator" menu if it doesn't exist
        if (kiMenu == null) {
            kiMenu = new JMenu(KAM_PLUGIN_SUBMENU);
            pluginMenu.add(kiMenu);
        }

        // add "Select KAM" action to submenu
        kiMenu.add(new SelectKAMDialogAction());

        // add "Add Kam Nodes" action to submenu
        kiMenu.add(new SearchKAMDialogAction());

        // add "Add Kam List" action to submenu
        kiMenu.add(new SearchKAMListDialogAction());

        // add separtor before bel configuration entry
        kiMenu.addSeparator();

        // add to "KAM Navigator" menu if KAM Plugin is available
        kiMenu.add(new SettingsDialogAction());
        
        updateMenuState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);

        if (CytoscapeDesktop.NETWORK_VIEW_DESTROYED.equals(e.getPropertyName())) {
            if (e.getNewValue() instanceof CyNetworkView) {
                // remove kam network from session
                CyNetworkView view = (CyNetworkView) e.getNewValue();
                KAMSession session = KAMSession.getInstance();
                KAMNetwork network = session.getKAMNetwork(view.getNetwork());
                session.getKAMNetworks().remove(network);
            }
            updateMenuState();
        } else if (KAM_NETWORK_CREATED_EVENT.equals(e.getPropertyName())) {
            updateMenuState();
        }
    }

    private static void updateMenuState() {
        JMenu kiMenu = getKamPluginMenu();
        JMenuItem addNodesItem = kiMenu.getItem(1);
        JMenuItem addListItem = kiMenu.getItem(2);

        boolean hasKamNetworks = !Utility.isEmpty(KAMSession.getInstance()
                .getKAMNetworks());
        // disable / enable items that require kam networks
        addNodesItem.setEnabled(hasKamNetworks);
        addListItem.setEnabled(hasKamNetworks);
    }

    private static JMenu getKamPluginMenu() {
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
        return kiMenu;
    }

    /**
     * The {@link CytoscapeAction action} to trigger the Load KAM dialog.
     * 
     * @see LoadKAMDialog
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static final class SelectKAMDialogAction extends CytoscapeAction {
        private static final long serialVersionUID = 2243171495622023060L;

        public SelectKAMDialogAction() {
            super("Load KAM");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            LoadKAMDialog kcdialog = new LoadKAMDialog();
            kcdialog.setVisible(true);
        }
    }

    /**
     * The {@link CytoscapeAction action} to trigger the Add KAM Nodes dialog.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static final class SearchKAMDialogAction extends CytoscapeAction {
        private static final long serialVersionUID = 2243171495622023060L;

        public SearchKAMDialogAction() {
            super("Add KAM Nodes");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            SearchKAMDialog kcdialog = new SearchKAMDialog();
            kcdialog.setVisible(true);
        }
    }

    /**
     * The {@link CytoscapeAction action} to trigger the Add KAM List dialog.
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class SearchKAMListDialogAction extends CytoscapeAction {
        private static final long serialVersionUID = -5051721582642478695L;

        public SearchKAMListDialogAction() {
            super(SearchKAMListDialog.TITLE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            SearchKAMListDialog dialog = new SearchKAMListDialog();
            dialog.setVisible(true);
        }
    }

    /**
     * Defines a {@link CytoscapeAction cytoscape action} to launch the
     * <em>BELFramework Configuration</em> dialog.  This allows the cytoscape
     * user to configure their access to the BELFramework Web API.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static final class SettingsDialogAction extends CytoscapeAction {
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
