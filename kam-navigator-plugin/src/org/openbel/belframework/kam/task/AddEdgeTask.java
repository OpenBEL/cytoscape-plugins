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
final class AddEdgeTask implements Task {
    private static final String TITLE = "Adding Edges";
    protected TaskMonitor m;
    protected boolean halt = false;
    protected final KAMNetwork kamNetwork;
    private final List<KamEdge> kamEdges;

    AddEdgeTask(KAMNetwork kamNetwork, List<KamEdge> kamEdges) {
        this.kamEdges = kamEdges;
        this.kamNetwork = kamNetwork;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        m.setStatus("Adding " + kamEdges.size() + " edges");
        int percentage = 0;
        m.setPercentCompleted(percentage);

        Set<CyEdge> addedEdges = new HashSet<CyEdge>();
        // use current percentage to keep track of values less then 1
        double currentPercentage = 0.0;
        double nodePercent = (1.0 / kamEdges.size()) * 100;
        for (final KamEdge edge : kamEdges) {
            if (halt) {
                // stop if halted
                break;
            }

            CyEdge cyEdge = kamNetwork.addEdge(edge);
            addedEdges.add(cyEdge);
            // TODO move percentage code (used by this and add nodes)
            // to some shared location
            currentPercentage += nodePercent;
            if (currentPercentage >= 1.0) {
                // only add to the percent complete if can be rounded to 1 or
                // greater
                int round = (int) Math.round(currentPercentage);
                percentage += round;
                m.setPercentCompleted(percentage);
                // set to remainder to even out percentages
                currentPercentage = currentPercentage - round;
            }
        }

        if (halt) {
            return;
        }

        final CyNetwork cyn = kamNetwork.getCyNetwork();

        cyn.unselectAllEdges();
        cyn.setSelectedEdgeState(addedEdges, true);

        // TODO push this default layout up somewhere
        CyLayoutAlgorithm dcl = CyLayouts.getLayout("degree-circle");
        dcl.setSelectedOnly(true);
        dcl.doLayout();

        final CyNetworkView view = Cytoscape
                .getNetworkView(cyn.getIdentifier());
        view.redrawGraph(true, true);

        Cytoscape.setCurrentNetwork(cyn.getIdentifier());
        Cytoscape.setCurrentNetworkView(cyn.getIdentifier());

        m.setPercentCompleted(100);
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
    public void setTaskMonitor(TaskMonitor m)
            throws IllegalThreadStateException {
        this.m = m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return TITLE;
    }

}
