/*
 * KAM Navigator Plugin
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
package org.openbel.cytoscape.navigator;

import org.openbel.framework.ws.model.Kam;

/**
 * Simple wrapper class to make Kams sortable 
 * 
 * Normally this would an inner class of a dialog, but this was extracted due
 * to multiple dialogs making use of it.
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
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
