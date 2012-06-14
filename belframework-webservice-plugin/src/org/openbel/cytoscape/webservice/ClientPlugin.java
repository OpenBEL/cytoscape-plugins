/*
 * BEL Framework Webservice Plugin
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
package org.openbel.cytoscape.webservice;

import java.io.IOException;

import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.plugin.CytoscapePlugin;

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
     * Default no-arg plugin construtor to initialize this plugin.
     */
    public ClientPlugin() {
        onCytoscapeStart();
        WebServiceClientManager.registerClient(ClientConnector.getInstance());
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
}
