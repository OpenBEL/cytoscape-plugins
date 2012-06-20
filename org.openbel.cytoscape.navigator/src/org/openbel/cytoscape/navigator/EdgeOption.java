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

/**
 * {@link EdgeOption} enum for the different types of expansion rules.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public enum EdgeOption {
    /**
     * Expand downstream and upstream of each node being added.  The nodes
     * and all adjacent edges will be added to the network.
     */
    ALL_EDGES ("All Edges"),
    /**
     * Expand downstream of each node being added.  The nodes and all
     * downstream edges will be added to the network.
     */
    DOWNSTREAM ("Downstream Edges"),
    /**
     * Expand upstream of each node being added.  The nodes and all
     * upstream edges will be added to the network.
     */
    UPSTREAM ("Upstream Edges"),
    /**
     * Expand the selected nodes with only edges that interconnect between
     * them.  The selected nodes and any interconnected edges are added to
     * the network.
     */
    INTERCONNECT ("Interconnect Nodes"),
    /**
     * Do not expand the selected nodes to include edges.  Only the
     * selected nodes are added to the network.
     */
    NONE ("None");

    private final String label;

    private EdgeOption(final String label) {
        this.label = label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return label;
    }
}