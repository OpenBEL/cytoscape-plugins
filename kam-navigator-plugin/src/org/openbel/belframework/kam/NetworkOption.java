package org.openbel.belframework.kam;

import com.selventa.belframework.ws.client.Kam;

import cytoscape.CyNetwork;

/**
 * {@link NetworkOption} represents the combo-box option for currently loaded
 * {@link Kam kam}-backed {@link CyNetwork cytoscape networks}.
 * 
 * This is a simple container class to get the network title in the toString
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public final class NetworkOption {
    private final CyNetwork cyn;

    public NetworkOption(final CyNetwork cyn) {
        this.cyn = cyn;
    }

    /**
     * @return the {@link CyNetwork} represented by this option
     */
    public CyNetwork getCyNetwork() {
        return cyn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return cyn.getTitle();
    }
}
