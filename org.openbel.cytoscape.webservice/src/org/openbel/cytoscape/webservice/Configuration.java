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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import cytoscape.CytoscapeInit;

/**
 * {@link Configuration} handles configuring access to the Web API.
 * Configuration is read/saved to cytoscape property files.
 *
 * @see Configuration#restoreState()
 * @see Configuration#saveState()
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class Configuration {
    private static final String COMMENTS =
            "Stores configuration for the BELFramework Web Service cytoscape plugin.";
    private static final String WSDL_KEY = "WSDL_URL";
    private static final String TIMEOUT_KEY = "TIMEOUT";
    private static final String DEFAULT_WSDL_URL =
            "http://localhost:8080/openbel-ws/belframework.wsdl";
    private static final int DEFAULT_TIMEOUT = 120;
    private static Configuration instance;
    private String wsdlURL;
    private Integer timeout;

    /**
     * Gets the singleton {@link Configuration} instance.  If the singleton
     * instance is null it will be initialized with default configuration
     * settings.
     *
     * @return the singleton {@link Configuration} instance
     */
    public synchronized static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration(DEFAULT_WSDL_URL, DEFAULT_TIMEOUT);
        }

        return instance;
    }

    /**
     * Resets the configuration settings back to defaults.
     */
    protected synchronized static void resetToDefaults() {
        if (instance != null) {
            instance.wsdlURL = DEFAULT_WSDL_URL;
            instance.timeout = DEFAULT_TIMEOUT;
        }
    }

    /**
     * Private constructor.
     *
     * @param wsdlURL the wsdl url
     * @param timeout the timeout value
     */
    private Configuration(final String wsdlURL, final Integer timeout) {
        this.wsdlURL = wsdlURL;
        this.timeout = timeout;
    }

    public String getWSDLURL() {
        return wsdlURL;
    }

    public void setWSDLURL(final String wsdlURL) {
        if (wsdlURL != null) {
            this.wsdlURL = wsdlURL;
        } else {
            this.wsdlURL = DEFAULT_WSDL_URL;
        }
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(final Integer timeout) {
        if (timeout != null) {
            this.timeout = timeout;
        } else {
            this.timeout = DEFAULT_TIMEOUT;
        }
    }

    /**
     * Saves the configuration state of the webservice client plugin to the
     * plugin properties file {@code belframework-webservice.props}.
     *
     * <p>
     * This plugin properties file is held in the .cytoscape folder located in
     * the user's home folder.
     * </p>
     *
     * @throws IOException Thrown if an IO exception occurred while writing the
     * configuration settings to the properties file
     */
    public void saveState() throws IOException {
        final File cfg = CytoscapeInit
                .getConfigFile("belframework-webservice.props");

        final Properties cfgprops = new Properties();
        cfgprops.put(WSDL_KEY, wsdlURL);
        cfgprops.put(TIMEOUT_KEY, timeout.toString());
        cfgprops.store(new FileWriter(cfg), COMMENTS);
    }

    /**
     * Restores the configuration state of the webservice client plugin from
     * the plugin properties file {@code belframework-webservice.props}.
     *
     * <p>
     * This plugin properties file is held in the .cytoscape folder located in
     * the user's home folder.
     * </p>
     *
     *
     * @throws IOException Thrown if an IO exception occurred while reading the
     * configuration settings to the properties file
     */
    public void restoreState() throws IOException {
        // creates empty file immediately
        final File cfg = CytoscapeInit
                .getConfigFile("belframework-webservice.props");

        if (cfg.exists() && cfg.canRead()) {
            final Properties cfgprops = new Properties();
            cfgprops.load(new FileReader(cfg));

            wsdlURL = cfgprops.getProperty(WSDL_KEY);
            if (wsdlURL == null) {
                wsdlURL = DEFAULT_WSDL_URL;
            }

            String timeoutProperty = cfgprops.getProperty(TIMEOUT_KEY);
            if (timeoutProperty != null) {
                String remainder = timeoutProperty.replaceFirst("\\d+", "");

                if (remainder.isEmpty()) {
                    // the timeout property can be parsed as an integer
                    timeout = Integer.parseInt(timeoutProperty);
                } else {
                    timeout = DEFAULT_TIMEOUT;
                }
            } else {
                timeout = DEFAULT_TIMEOUT;
            }
        } else {
            Configuration.resetToDefaults();
        }
    }
}
