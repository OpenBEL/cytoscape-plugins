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

import java.util.List;
import java.util.Set;

import org.openbel.cytoscape.navigator.KamIdentifier;
import org.openbel.cytoscape.navigator.Utility;

import org.openbel.framework.ws.model.EdgeDirectionType;
import org.openbel.framework.ws.model.KamEdge;
import org.openbel.framework.ws.model.KamNode;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.task.Task;

/**
 * {@link KamTasks} defines a wrapper to call supported, long-running
 * {@link Task cytoscape tasks}.
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
// TODO update javadocs
public class KamTasks {

    /**
     * Call the {@link AddNodesTask}.
     * 
     * @see AddNodesTask#run()
     * @param kamNetwork
     *            the {@link KAMNetwork kam network} to add to
     * @param kamNodes
     *            the {@link KamNode kam nodes} to add
     */
    public static void addNodes(CyNetwork cyNetwork, KamIdentifier kamId,
            List<KamNode> kamNodes) {
        Utility.executeTask(new AddNodesTask(cyNetwork, kamId, kamNodes));
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
    public static void addEdges(CyNetwork cyNetwork, KamIdentifier kamId,
            List<KamEdge> kamEdges) {
        Utility.executeTask(new AddEdgesTask(cyNetwork, kamId, kamEdges));
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
    public static void addNodesAndExpand(CyNetwork cyNetwork,
            KamIdentifier kamId, List<KamNode> kamNodes,
            EdgeDirectionType direction) {
        Utility.executeTask(new AddNodesEdgesTask(cyNetwork, kamId, kamNodes,
                direction));
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
    public static void addNodesAndInterconnect(CyNetwork cyNetwork,
            KamIdentifier kamId, List<KamNode> kamNodes) {
        Utility.executeTask(new AddNodesInterconnectTask(cyNetwork, kamId,
                kamNodes));
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
    public static void expandNodes(CyNetwork cyNetwork, KamIdentifier kamId,
            Set<CyNode> cynodes, EdgeDirectionType direction) {
        Utility.executeTask(new ExpandNodesTask(cyNetwork, kamId, cynodes,
                direction));
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
    public static void interconnectNodes(CyNetwork cyNetwork,
            KamIdentifier kamId, Set<CyNode> cynodes) {
        Utility.executeTask(new InterconnectNodesTask(cyNetwork, kamId, cynodes));
    }

    private KamTasks() {
        // prevent instantiation
    }
}
