package org.openbel.cytoscape.webservice.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * {@link X509TrustManager} that loads certificates from another trust store.
 *
 * <p>
 * An {@link SSLContext} can then be created with this {@link X509TrustManager}
 * to validate against your custom certificates.
 */
class CustomTrustManager implements X509TrustManager {

    private final X509TrustManager _tmgr;

    /**
     * Construct and load certificates from {@code is}.
     *
     * @param is {@link InputStream}; may not be {@code null}
     * @throws IOException when an IO error occurs loading certificates
     * @throws CertificateException when a certificate error occurs on load
     * @throws NoSuchAlgorithmException when the requested algorithm is not
     * available on load or init of the trust store
     * @throws KeyStoreException if the {@link TrustManagerFactory} fails to
     * use init the {@link KeyStore}
     * @throws NullPointerException when {@code is} is {@code null}
     */
    CustomTrustManager(InputStream is) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        if (is == null) throw new NullPointerException();
        _tmgr = _setup(is);
    }

    /**
     * Construct and load certificates from {@link File trustStore}.
     *
     * @param trustStore {@link InputStream}; may not be {@code null}
     * @throws IOException when an IO error occurs loading certificates
     * @throws CertificateException when a certificate error occurs on load
     * @throws NoSuchAlgorithmException when the requested algorithm is not
     * available on load or init of the trust store
     * @throws KeyStoreException if the {@link TrustManagerFactory} fails to
     * use init the {@link KeyStore}
     * @throws NullPointerException when {@code trustStore} is {@code null}
     */
    CustomTrustManager(File trustStore) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException {
        if (trustStore == null) throw new NullPointerException();
        _tmgr = _setup(new FileInputStream(trustStore));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
            String authType) throws java.security.cert.CertificateException {
        _tmgr.checkClientTrusted(chain, authType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
            String authType) throws java.security.cert.CertificateException {
        _tmgr.checkServerTrusted(chain, authType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return _tmgr.getAcceptedIssuers();
    }

    private X509TrustManager _setup(InputStream is) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            ts.load(is, null);
        } finally {
            try {
                is.close();
            } catch (Exception e) {}
        }

        // initialize a new TMF with the ts we just loaded
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        // acquire X509 trust manager from factory
        TrustManager tms[] = tmf.getTrustManagers();
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager) {
                return (X509TrustManager) tms[i];
            }
        }
        throw new RuntimeException("No X509TrustMgr in TrustManagerFactory");
    }
}
