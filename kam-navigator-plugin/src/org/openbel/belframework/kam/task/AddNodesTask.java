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
package org.openbel.belframework.kam.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbel.belframework.kam.KAMNetwork;

import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;

/**
 * Package-protected {@link Task task} to add {@link KamNode kam nodes} to a
 * {@link CyNetwork cytoscape network}.
 *
 * <p>
 * This {@link Task task} should be called by
 * {@link KAMTasks#addNodes(KAMNetwork, List)}.
 * </p>
 *
 * <p>
 * The {@link #addNodes()} function is protected to allow subclassing tasks to
 * add nodes.
 *
 * @see AddNodesEdgesTask#run()
 * @see AddNodesInterconnectTask#run()
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
class AddNodesTask implements Task {
    private static final String TITLE = "Adding Nodes";
    protected TaskMonitor m;
    protected boolean halt = false;
    protected final KAMNetwork kamNetwork;
    private final List<KamNode> kamNodes;

    AddNodesTask(final KAMNetwork kamNetwork, final List<KamNode> kamNodes) {
        this.kamNodes = kamNodes;
        this.kamNetwork = kamNetwork;
    }

    /**
     * {@inheritDoc}
     *
     * Adds {@link KamNode kam nodes} to the
     * {@link CyNetwork cytoscape network} and re-renders the view.
     */
    @Override
    public void run() {
        Set<CyNode> cynodes = addNodes();

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

        Cytoscape.setCurrentNetwork(cyn.getIdentifier());
        Cytoscape.setCurrentNetworkView(cyn.getIdentifier());

        m.setPercentCompleted(100);
    }

    /**
     * Adds {@link KamNode kam nodes} to the {@link KAMNetwork kam network}.
     * 
     * @return added nodes
     */
    protected Set<CyNode> addNodes() {
        m.setStatus("Adding " + kamNodes.size() + " selected nodes.");
        int percentage = 0;
        m.setPercentCompleted(percentage);

        // Add the KAM nodes and keep track
        Set<CyNode> cynodes = new HashSet<CyNode>();
        // use current percentage to keep track of values less then 1
        double currentPercentage = 0.0;
        double nodePercent = (1.0 / kamNodes.size()) * 100;
        for (final KamNode node : kamNodes) {
            if (halt) {
                // stop if halted
                break;
            }
            
            CyNode cyn = kamNetwork.addNode(node);
            cynodes.add(cyn);
            currentPercentage += nodePercent;
            if (currentPercentage >= 1.0) {
                // only add to the percent complete if can be rounded to 1 or greater
                int round = (int) Math.round(currentPercentage);
                percentage += round;
                m.setPercentCompleted(percentage);
                // set to remainder to even out percentages
                currentPercentage = currentPercentage - round;
            }
        }
        
        return cynodes;
    }

    @Override
    public void setTaskMonitor(TaskMonitor m)
            throws IllegalThreadStateException {
        this.m = m;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void halt() {
        halt = true;
    }
}
