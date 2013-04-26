/*
 * KAM Navigator Plugin
 *
 * URLs: http://openbel.org/
 * Copyright (C) 2012, Selventa
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openbel.cytoscape.navigator;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.openbel.cytoscape.navigator.dialog.AssociateToKamDialog;
import org.openbel.cytoscape.navigator.dialog.SearchKamDialog;
import org.openbel.cytoscape.navigator.dialog.SearchKamListDialog;
import org.openbel.cytoscape.webservice.dialog.SettingsDialog;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeVersion;
import cytoscape.data.CyAttributes;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualStyle;

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
public class KamNavigatorPlugin extends CytoscapePlugin {
    public static final String KAM_PLUGIN_SUBMENU = "KAM Navigator";
    public static final String KAM_NODE_ID_ATTR = "kam_node_id";
    public static final String KAM_NODE_FUNCTION_ATTR = "function";
    public static final String KAM_EDGE_ID_ATTR = "kam_edge_id";
    public static final String KAM_NAME_ATTR = "kam_name";
    public static final String KAM_COMPILE_DATE_ATTR = "kam_compile";
    public static final String WSDL_URL_ATTR = "kam_url";
    public static final String KAM_MAPPED_ATTR = "kam_mapped";

    private static final CyLogger log = CyLogger.getLogger(KamNavigatorPlugin.class);
    private static final String KAM_NAVIGATOR_VERSION = "0.9";
    private static final String KAM_STYLE = "KAM Visualization";
    private static final String KAM_STYLE_FILE = "/org/openbel/cytoscape/navigator/style.props";

    private final JMenuItem searchItem;
    private final JMenuItem searchListItem;
    private final JMenuItem associateItem;

    /**
     * Default no-arg plugin construtor to initialize this plugin.
     */
    public KamNavigatorPlugin() {
        CyAttributes nattr = Cytoscape.getNodeAttributes();
        CyAttributes eattr = Cytoscape.getEdgeAttributes();

        nattr.setUserEditable(KAM_NODE_ID_ATTR, false);
        nattr.setUserVisible(KAM_NODE_ID_ATTR, false);
        nattr.setUserEditable(KAM_NODE_FUNCTION_ATTR, false);
        nattr.setUserVisible(KAM_NODE_FUNCTION_ATTR, true);
        nattr.setUserEditable(KAM_NAME_ATTR, false);
        nattr.setUserVisible(KAM_NAME_ATTR, true);
        nattr.setUserEditable(KAM_COMPILE_DATE_ATTR, false);
        nattr.setUserVisible(KAM_COMPILE_DATE_ATTR, false);
        nattr.setUserEditable(WSDL_URL_ATTR, false);
        nattr.setUserVisible(WSDL_URL_ATTR, false);
        nattr.setUserEditable(KAM_MAPPED_ATTR, false);
        nattr.setUserVisible(KAM_MAPPED_ATTR, true);

        eattr.setUserEditable(KAM_EDGE_ID_ATTR, false);
        eattr.setUserVisible(KAM_EDGE_ID_ATTR, false);
        eattr.setUserEditable(KAM_NAME_ATTR, false);
        eattr.setUserVisible(KAM_NAME_ATTR, true);
        eattr.setUserEditable(KAM_COMPILE_DATE_ATTR, false);
        eattr.setUserVisible(KAM_COMPILE_DATE_ATTR, false);
        eattr.setUserEditable(WSDL_URL_ATTR, false);
        eattr.setUserVisible(WSDL_URL_ATTR, false);
        eattr.setUserEditable(KAM_MAPPED_ATTR, false);
        eattr.setUserVisible(KAM_MAPPED_ATTR, true);


        // hook up propery change listeners
        final KamNodeContextListener nctx = new KamNodeContextListener();
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_CREATED, nctx);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_DESTROYED, nctx);

        // register property change listener for this instance
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_CREATED, this);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                Cytoscape.NETWORK_CREATED, this);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                Cytoscape.NETWORK_DESTROYED, this);

        // build menu
        final JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus()
                .getOperationsMenu();
        JMenu kiMenu = getKamPluginMenu();

        // add to "KAM Navigator" menu if it doesn't exist
        if (kiMenu == null) {
            kiMenu = new JMenu(KAM_PLUGIN_SUBMENU);
            pluginMenu.add(kiMenu);
        }

        searchItem = kiMenu.add(new SearchKAMDialogAction());
        searchListItem = kiMenu.add(new SearchKAMListDialogAction());
        associateItem = kiMenu.add(new AssociateToKamDialogAction());
        kiMenu.addSeparator();
        kiMenu.add(new SettingsDialogAction());

        // add "Send Feedback" action to submenu
        JMenuItem feedbackItem = kiMenu.add(new FeedbackMailToAction());
        // disable if default mail client is not setup
        feedbackItem.setEnabled(Desktop.isDesktopSupported() ? Desktop
                .getDesktop().isSupported(Desktop.Action.MAIL) : false);

        // set the proper menu state
        updateMenuState(false);

        // load the default style or styles
        loadKAMStyle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);

        if (CytoscapeDesktop.NETWORK_VIEW_CREATED.equals(e.getPropertyName())) {
            CyNetwork cyn = ((CyNetworkView) e.getNewValue()).getNetwork();
            // TODO can this been done with a single instance?
            // TODO do we need to remove this on network destruction?
            cyn.addSelectEventListener(new NetworkDetailsListener());
        } else if (Cytoscape.NETWORK_CREATED.equals(e.getPropertyName()) ||
                Cytoscape.NETWORK_DESTROYED.equals(e.getPropertyName())) {
            updateMenuState(Cytoscape.NETWORK_DESTROYED.equals(e
                    .getPropertyName()));
        }
    }

    private void updateMenuState(boolean networkDestroyed) {
        boolean hasNetworks = !Cytoscape.getNetworkSet().isEmpty();

        // workaround for networks not being destroyed until AFTER event
        if (networkDestroyed && Cytoscape.getNetworkSet().size() == 1) {
            hasNetworks = false;
        }

        CyNetwork current = Cytoscape.getCurrentNetwork();

        // disable / enable items that require networks
        searchItem.setEnabled(hasNetworks);
        searchListItem.setEnabled(hasNetworks);
        associateItem.setEnabled(hasNetworks && current != null);
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
     * Load the {@link VisualStyle visual style} into the vizmapper.
     */
    private void loadKAMStyle() {
        final VisualMappingManager vismanager = Cytoscape.getVisualMappingManager();

        final CalculatorCatalog ccat = vismanager.getCalculatorCatalog();
        VisualStyle visualStyle = ccat.getVisualStyle(KAM_STYLE);
        if (visualStyle == null) {
            loadKAMStyleFromFile();
            visualStyle = ccat.getVisualStyle(KAM_STYLE);
        }
    }

    // TODO better exception handling
    private void loadKAMStyleFromFile() {
        // XXX is there a way to do this statically? getClass requires the
        // current class instance
        InputStream in = this.getClass().getResourceAsStream(KAM_STYLE_FILE);
        File f = null;
        try {
            f = File.createTempFile("viz", null);
            writeInputStreamIntoFile(in, f);
        } catch (IOException e) {
            log.warn("Error loading style", e);
            return;
        } finally {
            Utility.closeSilently(in);
        }

        if (!f.exists() || !f.canRead()) {
            return;
        }

        // load style
        Cytoscape.firePropertyChange(Cytoscape.VIZMAP_LOADED, null, f.getAbsolutePath());
    }

    private static void writeInputStreamIntoFile(InputStream in, File f)
            throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);

            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = bis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } finally {
            Utility.closeSilently(bis);
            Utility.closeSilently(fos);
        }
    }

    /**
     * Simple feedback mail action
     *
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class FeedbackMailToAction extends CytoscapeAction {
        private static final long serialVersionUID = -2109588518850444632L;

        public FeedbackMailToAction() {
            super("Send Feedback");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            String supportEmail = "support@belframework.org";
            String subject = "KAM%20Navigator%20Feedback";
            String body = "Autogenerated information: ["
                    + "KAM Navigator Version:" + KAM_NAVIGATOR_VERSION
                    + ", Cytoscape Version:" + CytoscapeVersion.version
                    + ", OS Name:" + System.getProperty("os.name")
                    + ", OS Version:" + System.getProperty("os.version")
                    + ", Java Version:" + System.getProperty("java.version")
                    + "]";
            body = urlEncode(body);

            String uriString = "mailto:" + supportEmail + "?subject=" + subject
                    + "&body=" + body;
            URI uri = null;
            try {
                uri = new URI(uriString);
            } catch (URISyntaxException e) {
                log.error("Error generating support e-mail", e);
                return;
            }

            try {
                Desktop.getDesktop().mail(uri);
            } catch (IOException e) {
                log.error("Error generating support e-mail", e);
            }
        }

        /**
         * Url encode a string.
         *
         * Taken from http://stackoverflow.com/a/4605816/20774
         */
        private static String urlEncode(String input) {
            StringBuilder resultStr = new StringBuilder();
            for (char ch : input.toCharArray()) {
                if (isUnsafe(ch)) {
                    resultStr.append('%');
                    resultStr.append(toHex(ch / 16));
                    resultStr.append(toHex(ch % 16));
                } else {
                    resultStr.append(ch);
                }
            }
            return resultStr.toString();
        }

        private static char toHex(int ch) {
            return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
        }

        private static boolean isUnsafe(char ch) {
            if (ch > 128 || ch < 0) {
                return true;
            }
            return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
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
            SearchKamDialog kcdialog = new SearchKamDialog();
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
            super(SearchKamListDialog.TITLE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            SearchKamListDialog dialog = new SearchKamListDialog();
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

    private static final class AssociateToKamDialogAction extends
            CytoscapeAction {

        private static final long serialVersionUID = 7620147624359237108L;

        public AssociateToKamDialogAction() {
            super(AssociateToKamDialog.TITLE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            AssociateToKamDialog dialog = new AssociateToKamDialog();
            dialog.setVisible(true);
        }
    }
}
