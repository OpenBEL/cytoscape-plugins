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
package org.openbel.cytoscape.navigator.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbel.cytoscape.navigator.KamIdentifier;
import org.openbel.cytoscape.navigator.KamSession;
import org.openbel.cytoscape.navigator.NetworkUtility;
import org.openbel.cytoscape.webservice.KamService;
import org.openbel.cytoscape.webservice.KamServiceFactory;
import org.openbel.framework.ws.model.EdgeDirectionType;
import org.openbel.framework.ws.model.KamEdge;
import org.openbel.framework.ws.model.KamNode;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.view.CyNetworkView;

/**
 * Package-protected {@link Task task} to interconnect
 * {@link KamNode kam nodes} and add to a {@link CyNetwork cytoscape network}.
 *
 * <p>
 * This {@link Task task} should be called by
 * {@link KamTasks#addNodesAndInterconnect(KAMNetwork, List)}.
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
// TODO this needs to be redone to use interconnect backend
final class AddNodesInterconnectTask extends AddNodesTask {
    private static final String TITLE = "Interconnecting Nodes";
    private final KamService kamService;
    private final List<KamNode> kamNodes;
    private final Set<String> kamNodeIds;

    AddNodesInterconnectTask(CyNetwork cyNetwork, KamIdentifier kamId, List<KamNode> kamNodes) {
        super(cyNetwork, kamId, kamNodes);
        this.kamService = KamServiceFactory.getInstance().getKAMService();
        this.kamNodes = kamNodes;
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
     * Adds {@link KamNode kam nodes}, interconnects them adding
     * {@link KamEdge kam edges}, adds them all to the
     * {@link CyNetwork cytoscape network}, and re-renders the view.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        // run AddNodes (super) task
        Set<CyNode> cynodes = super.addNodes();

        m.setStatus("Interconnecting network for " + kamNodes.size() + " selected nodes.");

        // Add existing KAM nodes to selected
        List<CyNode> nodes = cyNetwork.nodesList();
        kamNodes.addAll(NetworkUtility.getKAMNodes(nodes));

        // If more than one node selected, link up shared edges
        int numNodes = kamNodes.size();
        if (numNodes > 1) {
            for (final KamNode selectedNode : kamNodes) {
                if (halt) {
                    // stop if halted
                    break;
                }

                // TODO this should use the backend interconnect method
                final List<KamEdge> edges = kamService
                        .getAdjacentKamEdges(
                                KamSession.getInstance().getDialectHandle(kamId),
                                selectedNode, EdgeDirectionType.BOTH, null);

                for (final KamEdge edge : edges) {
                    final KamNode esrc = (KamNode) edge.getSource();
                    final KamNode etgt = (KamNode) edge.getTarget();

                    // filter out adjacent edges not between selected nodes
                    if (kamNodeIds.contains(esrc.getId())
                            && kamNodeIds.contains(etgt.getId())) {
                        NetworkUtility.addEdge(cyNetwork, kamId, edge);
                    }
                }
            }
        }

        if (halt) {
            return;
        }

        cyNetwork.unselectAllNodes();
        cyNetwork.setSelectedNodeState(cynodes, true);

        CyLayoutAlgorithm dcl = CyLayouts.getLayout("degree-circle");
        dcl.setSelectedOnly(true);
        dcl.doLayout();

        final CyNetworkView view = Cytoscape.getNetworkView(cyNetwork.getIdentifier());
        view.redrawGraph(true, true);

        Cytoscape.setCurrentNetwork(cyNetwork.getIdentifier());
        Cytoscape.setCurrentNetworkView(cyNetwork.getIdentifier());

        m.setPercentCompleted(100);
    }
}
