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

import static cytoscape.data.SelectEvent.EDGE_SET;
import static cytoscape.data.SelectEvent.NODE_SET;
import static cytoscape.data.SelectEvent.SINGLE_EDGE;
import static cytoscape.data.SelectEvent.SINGLE_NODE;

import java.util.Iterator;
import java.util.Set;

import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;

/**
 * The {@link SelectEventListener listener} that triggers the
 * {@link DetailsView detail view} to re-render when a
 * {@link CyNode cytoscape node} or {@link CyEdge cytoscape edge} is clicked.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class NetworkDetailsListener implements SelectEventListener {
    private final KAMNetwork kamNetwork;

    public NetworkDetailsListener(final KAMNetwork kamNetwork) {
        this.kamNetwork = kamNetwork;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onSelectEvent(SelectEvent e) {
        // handle select only when state is active
        if (e.getEventType() && e.getTarget() != null) {
            switch(e.getTargetType()) {
                case SINGLE_NODE:
                    final CyNode cynode = (CyNode) e.getTarget();
                    DetailsView.getInstance().showNodeDetails(kamNetwork, cynode);
                case NODE_SET:
                    Set<CyNode> nodesSelected = (Set<CyNode>) e.getTarget();
                    Iterator<CyNode> nit = nodesSelected.iterator();
                    if (nit.hasNext()) {
                        DetailsView.getInstance().showNodeDetails(kamNetwork,
                                nit.next());
                    }
                    break;
                case SINGLE_EDGE:
                    final CyEdge cyedge = (CyEdge) e.getTarget();
                    DetailsView.getInstance().showEdgeDetails(kamNetwork, cyedge);
                case EDGE_SET:
                    Set<CyEdge> edgesSelected = (Set<CyEdge>) e.getTarget();
                    Iterator<CyEdge> eit = edgesSelected.iterator();
                    if (eit.hasNext()) {
                        DetailsView.getInstance().showEdgeDetails(kamNetwork,
                                eit.next());
                    }
                    break;
            }
        }
    }
}
