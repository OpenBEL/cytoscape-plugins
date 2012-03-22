package org.openbel.belframework.webservice;

import java.util.EventListener;

/**
 * Interface for classes that want to listen for configuration changes
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
public interface ConfigurationListener extends EventListener {

    /**
     * Called when configuration is updated or changed
     */
    void configurationChange();
}
