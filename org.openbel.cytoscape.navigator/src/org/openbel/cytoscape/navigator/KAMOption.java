package org.openbel.cytoscape.navigator;

import org.openbel.framework.ws.model.Kam;

public final class KAMOption implements Comparable<KAMOption> {
    private final Kam kam;
    
    public KAMOption(Kam kam) {
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
    public int compareTo(KAMOption o) {
        if (o == null) {
            return 1;
        }
        return this.toString().compareTo(o.toString());
    }

}
