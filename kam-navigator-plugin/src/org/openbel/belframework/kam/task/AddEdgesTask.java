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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbel.belframework.kam.KamIdentifier;
import org.openbel.belframework.kam.NetworkUtility;
import org.openbel.belframework.kam.Utility;

import com.selventa.belframework.ws.client.KamEdge;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;

/**
 * Package-protected {@link Task task} to add {@link KamEdge kam edges} to a
 * {@link CyNetwork cytoscape network}.
 * 
 * <p>
 * This {@link Task task} should be called by
 * {@link KAMTasks#addEdges(KAMNetwork, List)}.
 * </p>
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
class AddEdgesTask implements Task {
    private static final String TITLE = "Adding Edges";
    private TaskMonitor monitor;
    protected boolean halt = false;
    protected final CyNetwork cyNetwork;
    protected final KamIdentifier kamId;
    protected final Collection<KamEdge> kamEdges;

    AddEdgesTask(CyNetwork cyNetwork, KamIdentifier kamId, Collection<KamEdge> kamEdges) {
        this.cyNetwork = cyNetwork;
        this.kamId = kamId;
        this.kamEdges = kamEdges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Collection<KamEdge> edgesToAdd = getEdgesToAdd();
        if (halt || Utility.isEmpty(edgesToAdd)) {
            return;
        }
        
        monitor.setStatus("Adding " + edgesToAdd.size() + " edges");
        int percentage = 0;
        // TODO percentages should take into account previous operations
        // used by getEdgesToAdd
        monitor.setPercentCompleted(percentage);

        Set<CyEdge> addedEdges = new HashSet<CyEdge>();
        // use current percentage to keep track of values less then 1
        double currentPercentage = 0.0;
        double nodePercent = (1.0 / edgesToAdd.size()) * 100;
        for (final KamEdge edge : edgesToAdd) {
            if (halt) {
                // stop if halted
                break;
            }

            CyEdge cyEdge = NetworkUtility.addEdge(cyNetwork, kamId, edge);
            addedEdges.add(cyEdge);
            
            // TODO move percentage code (used by this and add nodes)
            // to some shared location
            currentPercentage += nodePercent;
            if (currentPercentage >= 1.0) {
                // only add to the percent complete if can be rounded to 1 or
                // greater
                int round = (int) Math.round(currentPercentage);
                percentage += round;
                monitor.setPercentCompleted(percentage);
                // set to remainder to even out percentages
                currentPercentage = currentPercentage - round;
            }
        }

        if (halt) {
            return;
        }

        cyNetwork.unselectAllEdges();
        cyNetwork.setSelectedEdgeState(addedEdges, true);
        // do we want to keep track of added nodes and select those as well?

        // TODO push this default layout up somewhere
        CyLayoutAlgorithm dcl = CyLayouts.getLayout("degree-circle");
        dcl.setSelectedOnly(true);
        dcl.doLayout();

        final CyNetworkView view = Cytoscape
                .getNetworkView(cyNetwork.getIdentifier());
        view.redrawGraph(true, true);

        Cytoscape.setCurrentNetwork(cyNetwork.getIdentifier());
        Cytoscape.setCurrentNetworkView(cyNetwork.getIdentifier());

        monitor.setPercentCompleted(100);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void halt() {
        halt = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTaskMonitor(TaskMonitor monitor)
            throws IllegalThreadStateException {
        this.monitor = monitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return TITLE;
    }
    
    /**
     * Get or retrieve edges to be added to the network.
     * 
     * Override this to implement different methods of retrieving edges
     * 
     * @return {@link Collection} of {@link KamEdge KamEdges}
     */
    protected Collection<KamEdge> getEdgesToAdd() {
        return kamEdges;
    }

}
