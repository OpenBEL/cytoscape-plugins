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
package org.openbel.belframework.kam;

import org.openbel.framework.ws.model.Kam;

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
