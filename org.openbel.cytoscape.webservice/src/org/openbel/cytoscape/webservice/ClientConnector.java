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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

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
    private static final String SSL_SOCKET_FACTORY_KEY =
            "com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory";
    private static final String REQUEST_TIMEOUT_KEY =
            "com.sun.xml.internal.ws.request.timeout";
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
            // created trusted SSL socket factory
            SSLSocketFactory ssl = trustedSSL();

            // setup stub and configure timeout
            HttpsURLConnection.setDefaultSSLSocketFactory(ssl);
            WebAPI stub = new WebAPIService(wsdlURL, new QName(
                    "http://belframework.org/ws/schemas", "WebAPIService"))
                    .getWebAPISoap11();

            Map<String, Object> ctx = ((BindingProvider) stub).getRequestContext();

            // set timeout
            ctx.put(REQUEST_TIMEOUT_KEY, cfg.getTimeout() * 1000);

            // set SSL socket factory
            ctx.put(SSL_SOCKET_FACTORY_KEY, ssl);

            // set endpoint location
            ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, wsdlURL.toString());

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

    /**
     * Returns an {@link SSLSocketFactory} that trusts all certificates.  This
     * allows cytoscape to communicate to TLS-enabled servers without having
     * a local certificate.
     *
     * @return {@link SSLSocketFactory}
     */
    private SSLSocketFactory trustedSSL() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {}
            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {}
        }};
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        };
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        return sc.getSocketFactory();
    }
}
