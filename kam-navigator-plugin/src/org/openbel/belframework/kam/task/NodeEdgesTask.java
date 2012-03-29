/*
 * KAM Navigator Plugin
 *
 * URLs: http://openbel.org/
 * Copyright (C) 2012, Selventa
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openbel.belframework.kam.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.view.CyNetworkView;

/**
 * Package-protected {@link Task task} to add {@link KamNode kam nodes} and
 * {@link KamEdge adjacent kam edges} to a {@link CyNetwork cytoscape network}.
 *
 * <p>
 * This {@link Task task} should be called by
 * {@link KAMTasks#expandNodes(KAMNetwork, List, EdgeDirectionType)}.
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
class NodeEdgesTask extends AddNodeTask {
    private static final String TITLE = "Expanding Edges";
    private final KAMService kamService;
    private final List<KamNode> kamNodes;
    private final EdgeDirectionType direction;
    private final Set<String> kamNodeIds;

    NodeEdgesTask(final KAMNetwork kamNetwork, final List<KamNode> kamNodes,
            final EdgeDirectionType direction) {
        super(kamNetwork, kamNodes);
        this.kamService = KAMServiceFactory.getInstance().getKAMService();
        this.kamNodes = kamNodes;
        this.direction = direction;
        this.kamNodeIds = new HashSet<String>(kamNodes.size());
        for (final KamNode kamNode : kamNodes) {
            this.kamNodeIds.add(kamNode.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     *
     * Adds {@link KamNode kam nodes}, expands to adjacent
     * {@link KamEdge kam edges}, adds them all to the
     * {@link CyNetwork cytoscape network}, and re-renders the view.
     */
    @Override
    public void run() {
        // run AddNodes (super) task
        super.addNodes();

        setStatus();

        for (final KamNode selectedNode : kamNodes) {
            if (halt) {
                // stop if halted
                break;
            }
            
            final List<KamEdge> edges = kamService.getAdjacentKamEdges(
                    kamNetwork.getDialectHandle(), selectedNode, direction,
                    null);

            for (final KamEdge edge : edges) {
                kamNetwork.addEdge(edge);
            }
        }
        
        if (halt) {
            return;
        }

        final CyNetwork cyn = kamNetwork.getCyNetwork();

        cyn.unselectAllNodes();
        cyn.setSelectedNodeState(cynodes, true);

        CyLayoutAlgorithm dcl = CyLayouts.getLayout("degree-circle");
        dcl.setSelectedOnly(true);
        dcl.doLayout();

        final CyNetworkView view = Cytoscape.getNetworkView(cyn.getIdentifier());
        view.redrawGraph(true, true);
        Cytoscape.getDesktop().setFocus(cyn.getIdentifier());

        m.setPercentCompleted(100);
    }

    private void setStatus() {
        final String edgeType;
        switch (direction) {
            case FORWARD:
                edgeType = "downstream";
                break;
            case REVERSE:
                edgeType = "upstream";
                break;
            default:
                edgeType = "downstream and upstream";
                break;
        }
        m.setStatus("Expanding " + edgeType + " edges for " + kamNodes.size()
                + " selected nodes.");
    }
}
