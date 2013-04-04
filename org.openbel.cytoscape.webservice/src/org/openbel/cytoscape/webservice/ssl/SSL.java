package org.openbel.cytoscape.webservice.ssl;

import java.net.URL;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * SSL provides static access to a new {@link SSLContext} backed by a custom
 * trust store.  This trust store is all of Java's trusted certificate
 * authorities plus Go Daddy (gd_intermediate and gd_cross_intermediate).
 */
public class SSL {

    private static final String TRUST_STORE = "cacerts";
    private static final String SSL_PROTOCOL = "SSL";
    private static SSLContext _ssl;

    /**
     * Static construction of the custom trust store and SSLContext.
     */
    static {
        try {
            URL url = SSL.class.getResource(TRUST_STORE);
            TrustManager mgr = new CustomTrustManager(url.openStream());
            _ssl = SSLContext.getInstance(SSL_PROTOCOL);
            _ssl.init(null, new TrustManager[] {mgr}, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the custom {@link SSLContext}.
     *
     * @return {@link SSLContext}
     */
    public static SSLContext getContext() {
        return _ssl;
    }
}
