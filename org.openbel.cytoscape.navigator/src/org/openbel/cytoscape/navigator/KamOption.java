package org.openbel.cytoscape.navigator;

import org.openbel.framework.ws.model.Kam;

public final class KamOption implements Comparable<KamOption> {
    private final Kam kam;
    
    public KamOption(Kam kam) {
        this.kam = kam;
    }
    
    public Kam getKam() {
        return kam;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return kam.getName();
    }
    
    @Override
    public int compareTo(KamOption o) {
        if (o == null) {
            return 1;
        }
        return this.toString().compareTo(o.toString());
    }

}
