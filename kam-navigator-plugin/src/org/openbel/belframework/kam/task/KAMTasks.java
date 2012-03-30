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

import java.util.List;
import java.util.Set;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.kam.Utility;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNode;
import cytoscape.task.Task;

/**
 * {@link KAMTasks} defines a wrapper to call supported, long-running
 * {@link Task cytoscape tasks}.
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMTasks {

    /**
     * Call the {@link AddNodesTask}.
     * 
     * @see AddNodesTask#run()
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param kamNodes
     *            the {@link KamNode kam nodes} to add
     */
    public static void addNodes(final KAMNetwork kamNetwork,
            final List<KamNode> kamNodes) {
        final AddNodesTask task = new AddNodesTask(kamNetwork, kamNodes);
        Utility.executeTask(task);
    }

    /**
     * Call the {@link AddEdgesTask}.
     * 
     * @see AddEdgesTask#run()
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param kamNodes
     *            the {@link KamEdge kam edges} to add
     */
    public static void addEdges(final KAMNetwork kamNetwork,
            final List<KamEdge> kamEdges) {
        final AddEdgesTask task = new AddEdgesTask(kamNetwork, kamEdges);
        Utility.executeTask(task);
    }

    /**
     * Call the {@link AddNodesEdgesTask}.
     * 
     * @see AddNodesEdgesTask#run()
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param kamNodes
     *            the {@link KamNode kam nodes} to expand
     */
    public static void addNodesAndExpand(final KAMNetwork kamNetwork,
            final List<KamNode> kamNodes, final EdgeDirectionType direction) {
        final AddNodesEdgesTask task = new AddNodesEdgesTask(kamNetwork,
                kamNodes, direction);
        Utility.executeTask(task);
    }

    /**
     * Call the {@link AddNodesInterconnectTask}.
     * 
     * @see AddNodesInterconnectTask#run()
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param kamNodes
     *            the {@link KamNode kam nodes} to interconnect
     */
    public static void addNodesAndInterconnect(final KAMNetwork kamNetwork,
            final List<KamNode> kamNodes) {
        final AddNodesInterconnectTask task = new AddNodesInterconnectTask(
                kamNetwork, kamNodes);
        Utility.executeTask(task);
    }

    /**
     * Call the {@link ExpandNodesTask}.
     * 
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param cynodes
     *            the {@link CyNode CyNodes} to expand
     * @param direction
     *            the {@link EdgeDirectionType} to expand in
     */
    public static void expandNodes(final KAMNetwork kamNetwork,
            final Set<CyNode> cynodes, final EdgeDirectionType direction) {
        final ExpandNodesTask task = new ExpandNodesTask(kamNetwork, cynodes,
                direction);
        Utility.executeTask(task);
    }

    /**
     * Call the {@link InterconnectNodesTask}.
     * 
     * 
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param cynodes
     *            the {@link CyNode CyNodes} to interconnect, must be 2 or more
     */
    public static void interconnectNodes(final KAMNetwork kamNetwork,
            final Set<CyNode> cynodes) {
        final InterconnectNodesTask task = new InterconnectNodesTask(
                kamNetwork, cynodes);
        Utility.executeTask(task);
    }

    private KAMTasks() {
        // prevent instantiation
    }
}
