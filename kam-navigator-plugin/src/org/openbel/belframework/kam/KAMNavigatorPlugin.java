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

import javax.swing.JMenu;

import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
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
    public static final String KAM_EDGE_ID_ATTR = "KAM_EDGE_ID";

    /**
     * Default no-arg plugin construtor to initialize this plugin.
     */
    public KAMNavigatorPlugin() {
        // add KAM_NODE_ID as a system node attribute
        Cytoscape.getNodeAttributes().setUserEditable(KAM_NODE_ID_ATTR, false);
        Cytoscape.getNodeAttributes().setUserVisible(KAM_NODE_ID_ATTR, false);

        // add KAM_EDGE_ID as a system edge attribute
        Cytoscape.getEdgeAttributes().setUserEditable(KAM_EDGE_ID_ATTR, false);
        Cytoscape.getEdgeAttributes().setUserVisible(KAM_EDGE_ID_ATTR, false);

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

        // add to "KAM Navigator" menu if it doesn't exist
        if (kiMenu == null) {
            kiMenu = new JMenu(KAM_PLUGIN_SUBMENU);
            pluginMenu.add(kiMenu);
        }

        // add "Select KAM" action to submenu
        kiMenu.add(new SelectKAMDialogAction());

        // add "Search KAM" action to submenu
        kiMenu.add(new SearchKAMDialogAction());

        // hook up propery change listeners
        final KAMNodeContextListener nctx = new KAMNodeContextListener();
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_CREATED, nctx);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_DESTROYED, nctx);
    }

    /**
     * The {@link CytoscapeAction action} to trigger the Load KAM dialog.
     *
     * @see LoadKAMDialog
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    public class SelectKAMDialogAction extends CytoscapeAction {
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
    public class SearchKAMDialogAction extends CytoscapeAction {
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
}
