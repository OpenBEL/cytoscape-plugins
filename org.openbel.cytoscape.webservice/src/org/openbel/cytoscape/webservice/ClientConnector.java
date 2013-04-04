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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.openbel.cytoscape.webservice.ssl.SSL;
import org.openbel.framework.ws.model.WebAPI;
import org.openbel.framework.ws.model.WebAPIService;

import cytoscape.data.webservice.CyWebServiceEvent;
import cytoscape.data.webservice.CyWebServiceException;
import cytoscape.data.webservice.WebServiceClientImpl;
import cytoscape.data.webservice.WebServiceClientManager;

/**
 * {@link ClientConnector} defines the BELFramework webservice to register with
 * the {@link WebServiceClientManager cytoscape webservice manager}.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class ClientConnector extends WebServiceClientImpl<WebAPI> {
    private static final long serialVersionUID = -6685554742203767122L;
    private static final String CLIENT = "belframework";
    private static final String DISPLAY_NAME = "BEL Framework Web Services Connection";
    private static final String REQUEST_TIMEOUT_KEY =
            "com.sun.xml.internal.ws.request.timeout";
    private static final String SSL_KEY =
            "com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory";
    private static final Configuration cfg = Configuration.getInstance();
    private static ClientConnector instance;
    private boolean valid = false;

    public static synchronized ClientConnector getInstance() {
        if (instance == null) {
            instance = new ClientConnector();
        }

        return instance;
    }

    /**
     * Private constructor for singleton instance.
     */
    private ClientConnector() {
        super(CLIENT, DISPLAY_NAME);
        configure();
    }

    /**
     * Reconfigures and reports an error if the connection is unsuccessful.
     *
     * @see ClientConnector#configure()
     */
    public synchronized void reconfigure() {
        configure();

        // reload client connector in kam service after reconfigure
        KamServiceFactory.getInstance().getKAMService().reloadClientConnector();
    }

    /**
     * Configures webservice settings and validates connection.
     */
    private void configure() {
        URL wsdlURL;
        try {
            wsdlURL = new URL(cfg.getWSDLURL());
        } catch (MalformedURLException e) {
            valid = false;
            return;
        }

        try {
            // Setup stub with local WSDL to provide chicken and egg problem
            // with SSL.  If the access is over HTTPS the request context will
            // not have the custom SSL context initially.
            URL local = ClientConnector.class.getResource("belframework.wsdl");
            WebAPI stub = new WebAPIService(local, new QName(
                    "http://belframework.org/ws/schemas", "WebAPIService"))
                    .getWebAPISoap11();

            // request context to configure
            Map<String, Object> ctx = ((BindingProvider) stub).getRequestContext();

            // set timeout
            ctx.put(REQUEST_TIMEOUT_KEY, cfg.getTimeout() * 1000);

            // set endpoint location
            ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, wsdlURL.toString());

            // set custom SSL context
            SSLContext sslContext = SSL.getContext();
            ctx.put(SSL_KEY, sslContext.getSocketFactory());

            // set web api stub for plugin use
            setClientStub(stub);
            valid = true;
        } catch (Throwable e) {
            valid = false;
        }
    }

    /**
     * Returns {@code true} if the webservice connection is valid,
     * {@code false} if not.
     *
     * @return {@code true} for valid connection, {@code false} for not valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * {@inheritDoc}
     *
     * Current unsupported.  If called an
     * {@link UnsupportedOperationException exception} is thrown.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void executeService(CyWebServiceEvent wse)
            throws CyWebServiceException {
        throw new UnsupportedOperationException("executeService not supported.");
    }
}
