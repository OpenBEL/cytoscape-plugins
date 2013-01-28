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

import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_COMPILE_DATE_ATTR;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_EDGE_ID_ATTR;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_MAPPED_ATTR;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_NAME_ATTR;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_NODE_FUNCTION_ATTR;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_NODE_ID_ATTR;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.WSDL_URL_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openbel.framework.ws.model.FunctionType;
import org.openbel.framework.ws.model.KamEdge;
import org.openbel.framework.ws.model.KamNode;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;

/**
 * Static utility methods for adding and reading a Cytoscape Network with a
 * Kam context.
 *
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
// TODO Finish Javadocs on all methods
public class NetworkUtility {

    private static final CyAttributes nodeAtt = Cytoscape.getNodeAttributes();
    private static final CyAttributes edgeAtt = Cytoscape.getEdgeAttributes();

    /**
     * Adds a {@link KamNode kam node} to the {@link CyNetwork cytoscape
     * network} and returns the equivalent {@link CyNode cytoscape node}.
     *
     * @param node the {@link KamNode kam node} to add
     * @return the {@link CyNode cytoscape node} created for the specific
     *         {@link KamNode kam node}
     */
    public static CyNode addNode(CyNetwork cyn, KamIdentifier kamId,
            KamNode kamNode) {
        // create cytoscape node and attach KAM attributes
        CyNode cynode = Cytoscape.getCyNode(kamNode.getLabel(), true);
        String id = cynode.getIdentifier();

        nodeAtt.setAttribute(id, KAM_NODE_ID_ATTR, kamNode.getId());
        nodeAtt.setAttribute(id, KAM_NODE_FUNCTION_ATTR, kamNode.getFunction()
                .getDisplayValue());
        nodeAtt.setAttribute(id, KAM_NAME_ATTR, kamId.getName());
        nodeAtt.setAttribute(id, KAM_COMPILE_DATE_ATTR,
                Long.toString(kamId.getCompiledTime()));
        nodeAtt.setAttribute(id, WSDL_URL_ATTR, kamId.getWsdlUrl());
        nodeAtt.setAttribute(id, KAM_MAPPED_ATTR, "yes");

        cyn.addNode(cynode);
        return cynode;
    }

    /**
     * Updates {@link CyAttributes node attributes} for a {@link CyNode node}
     * using a {@link KamIdentifier KAM id} and {@link KamNode KAM node}.
     *
     * @param cynode {@link CyNode}; may not be {@code null}
     * @param kamId {@link KamIdentifier}; may not be {@code null}
     * @param node {@link KamNode}; may not be {@code null}
     * @throws NullPointerException when {@code cynode}, {@code kamId}, or
     * {@code node} is {@code null}
     */
    public static void updateNode(CyNode cynode, KamIdentifier kamId,
            KamNode node) {
        if (cynode == null) throw new NullPointerException();
        if (kamId == null) throw new NullPointerException();
        if (node == null) throw new NullPointerException();

        String id = cynode.getIdentifier();
        nodeAtt.setAttribute(id, KAM_NODE_ID_ATTR, node.getId());
        nodeAtt.setAttribute(id, KAM_NODE_FUNCTION_ATTR, node.getFunction()
                .getDisplayValue());
        nodeAtt.setAttribute(id, KAM_NAME_ATTR, kamId.getName());
        nodeAtt.setAttribute(id, KAM_COMPILE_DATE_ATTR,
                Long.toString(kamId.getCompiledTime()));
        nodeAtt.setAttribute(id, WSDL_URL_ATTR, kamId.getWsdlUrl());
        nodeAtt.setAttribute(id, KAM_MAPPED_ATTR, "yes");
    }

    /**
     * Adds a {@link KamEdge kam edge} to the {@link CyNetwork cytoscape
     * network} and returns the equivalent {@link CyEdge cytoscape edge}.
     *
     * @param edge the {@link KamEdge kam edge} to add
     * @return the {@link CyEdge cytoscape edge} created for the specific
     *         {@link KamEdge kam edge}
     */
    public static CyEdge addEdge(CyNetwork cyn, KamIdentifier kamId,
            KamEdge edge) {
        // link up the source node
        final KamNode srckn = (KamNode) edge.getSource();

        CyNode cynsource = findCyNode(cyn, srckn);
        if (cynsource == null) {
            cynsource = addNode(cyn, kamId, srckn);
        }

        // link up the target node
        final KamNode tgtkn = (KamNode) edge.getTarget();

        CyNode cyntarget = findCyNode(cyn, tgtkn);
        if (cyntarget == null) {
            cyntarget = addNode(cyn, kamId, tgtkn);
        }

        // create cytoscape edge and attach KAM edge id as hidden attribute
        CyEdge cye = Cytoscape.getCyEdge(cynsource, cyntarget,
                Semantics.INTERACTION,
                edge.getRelationship().getDisplayValue(), true, true);
        String id = cye.getIdentifier();
        edgeAtt.setAttribute(id, KAM_EDGE_ID_ATTR, edge.getId());
        edgeAtt.setAttribute(id, KAM_NAME_ATTR, kamId.getName());
        String compileTime = Long.toString(kamId.getCompiledTime());
        edgeAtt.setAttribute(id, KAM_COMPILE_DATE_ATTR, compileTime);
        edgeAtt.setAttribute(id, WSDL_URL_ATTR, kamId.getWsdlUrl());
        edgeAtt.setAttribute(id, KAM_MAPPED_ATTR, "yes");
        cyn.addEdge(cye);
        return cye;
    }

    /**
     * Updates {@link CyAttributes edges attributes} for a {@link CyEdge edge}
     * using a {@link KamIdentifier KAM id} and {@link KamEdge KAM edge}.
     *
     * @param cyedge {@link CyEdge}; may not be {@code null}
     * @param kamId {@link KamIdentifier}; may not be {@code null}
     * @param edge {@link KamEdge}; may not be {@code null}
     * @throws NullPointerException when {@code cyedge}, {@code kamId}, or
     * {@code edge} is {@code null}
     */
    public static void updateEdge(CyEdge cyedge, KamIdentifier kamId,
            KamEdge edge) {
        if (cyedge == null) throw new NullPointerException();
        if (edge == null) throw new NullPointerException();
        if (kamId == null) throw new NullPointerException();

        String id = cyedge.getIdentifier();
        edgeAtt.setAttribute(id, Semantics.INTERACTION, edge.getRelationship()
                .getDisplayValue());
        edgeAtt.setAttribute(id, KAM_EDGE_ID_ATTR, edge.getId());
        edgeAtt.setAttribute(id, KAM_NAME_ATTR, kamId.getName());
        String compileTime = Long.toString(kamId.getCompiledTime());
        edgeAtt.setAttribute(id, KAM_COMPILE_DATE_ATTR, compileTime);
        edgeAtt.setAttribute(id, WSDL_URL_ATTR, kamId.getWsdlUrl());
        edgeAtt.setAttribute(id, KAM_MAPPED_ATTR, "yes");
    }

    public static void disassociate(CyNode cynode) {
        if (cynode == null) throw new NullPointerException();

        String id = cynode.getIdentifier();
        safeDeleteAttribute(nodeAtt, id, KAM_NODE_ID_ATTR);
        safeDeleteAttribute(nodeAtt, id, KAM_NAME_ATTR);
        safeDeleteAttribute(nodeAtt, id, KAM_COMPILE_DATE_ATTR);
        safeDeleteAttribute(nodeAtt, id, WSDL_URL_ATTR);
        nodeAtt.setAttribute(id, KAM_MAPPED_ATTR, "no");
    }

    public static void disassociate(CyEdge cyedge) {
        if (cyedge == null) throw new NullPointerException();

        String id = cyedge.getIdentifier();
        safeDeleteAttribute(edgeAtt, id, KAM_EDGE_ID_ATTR);
        safeDeleteAttribute(edgeAtt, id, KAM_NAME_ATTR);
        safeDeleteAttribute(edgeAtt, id, KAM_COMPILE_DATE_ATTR);
        safeDeleteAttribute(edgeAtt, id, WSDL_URL_ATTR);
        edgeAtt.setAttribute(id, KAM_MAPPED_ATTR, "no");
    }

    /**
     * Retrieve all the {@link KamNode kam nodes} for a {@link Collection} of
     * {@link CyNode cytoscape nodes}.
     *
     * @param cynodes {@link Collection} of {@link CyNode cytoscape nodes}
     * @return {@link List} of {@link KamNode kam nodes}, which will not be
     * {@code null} but might be empty
     */
    public static Set<KamNode> getKAMNodes(Collection<CyNode> cynodes) {
        final Set<KamNode> kamNodes = new HashSet<KamNode>(cynodes.size());
        for (final CyNode cynode : cynodes) {
            KamNode kamNode = getKAMNode(cynode);
            if (kamNode != null) {
                kamNodes.add(kamNode);
            }
        }

        return kamNodes;
    }

    /**
     * Retrieve the {@link KamNode kam node} for a specific {@link CyNode
     * cytoscape node}.
     *
     * @param cynode the {@link CyNode cytoscape node}
     * @return the {@link KamNode kam node} equivalent of the specific
     * {@link CyNode cytoscape node}, null if the cynode is not kam backed
     */
    public static KamNode getKAMNode(CyNode cynode) {
        // check to see if the cynode is kam backed
        if (!isKamBacked(cynode)) {
            return null;
        }

        final String id = cynode.getIdentifier();
        final String kamId = nodeAtt.getStringAttribute(id, KAM_NODE_ID_ATTR);
        final String func = nodeAtt.getStringAttribute(id,
                KAM_NODE_FUNCTION_ATTR);

        final KamNode kamNode = new KamNode();
        kamNode.setId(kamId);
        final FunctionType ftype = FunctionType.fromValue(func);
        kamNode.setFunction(ftype);
        kamNode.setLabel(cynode.getIdentifier());
        return kamNode;
    }

    /**
     * Retrieve all {@link CyEdge cytoscape edges} in the {@link CyNetwork
     * cytoscape network} as {@link KamEdge kam edges}.
     *
     * @return the {@link List} of {@link KamEdge kam edges} for all
     * {@link CyEdge cytoscape edges} in the
     * {@link CyNetwork cytoscape network}, which will not be {@code null} but
     * might be empty
     */
    public List<KamEdge> getKAMEdges(Collection<CyEdge> cyedges) {
        final List<KamEdge> kamEdges = new ArrayList<KamEdge>(cyedges.size());
        for (final CyEdge cyedge : cyedges) {
            KamEdge kamEdge = getKAMEdge(cyedge);
            if (kamEdge != null) {
                kamEdges.add(kamEdge);
            }
        }

        return kamEdges;
    }

    /**
     * Retrieve the {@link KamEdge kam edge} for a specific {@link CyEdge
     * cytoscape edge}.
     *
     * @param cyedge the {@link CyEdge cytoscape edge}
     * @return the {@link KamEdge kam edge} equivalent of the specific
     * {@link CyEdge cytoscape edge}
     */
    public static KamEdge getKAMEdge(CyEdge cyedge) {
        if (!isKamBacked(cyedge)) {
            return null;
        }

        final KamEdge kamEdge = new KamEdge();

        String kamEdgeId = edgeAtt.getStringAttribute(cyedge.getIdentifier(),
                KAM_EDGE_ID_ATTR);
        kamEdge.setId(kamEdgeId);
        return kamEdge;
    }

    public static boolean isKamBacked(CyNode cynode) {
        final String kamId = nodeAtt.getStringAttribute(cynode.getIdentifier(),
                KAM_NODE_ID_ATTR);
        return kamId != null;
    }

    public static boolean isKamBacked(CyEdge cyedge) {
        String kamEdgeId = edgeAtt.getStringAttribute(cyedge.getIdentifier(),
                KAM_EDGE_ID_ATTR);
        return kamEdgeId != null;
    }

    public static Map<KamIdentifier, Set<CyNode>> getKamNodeIds(
            Collection<CyNode> cynodes) {
        Map<KamIdentifier, Set<CyNode>> ret = new HashMap<KamIdentifier, Set<CyNode>>();

        for (CyNode n : cynodes) {
            if (!isKamBacked(n)) {
                continue;
            }

            KamIdentifier id = new KamIdentifier(n);

            Set<CyNode> ns = ret.get(id);
            if (ns == null) {
                ns = new HashSet<CyNode>();
                ret.put(id, ns);
            }
            ns.add(n);
        }

        return ret;
    }

    public static Map<KamIdentifier, Set<CyEdge>> getKamEdgeIds(
            Collection<CyEdge> cyedges) {
        Map<KamIdentifier, Set<CyEdge>> ret = new HashMap<KamIdentifier, Set<CyEdge>>();

        for (CyEdge e : cyedges) {
            if (!isKamBacked(e)) {
                continue;
            }

            KamIdentifier sId = new KamIdentifier((CyNode) e.getSource());
            KamIdentifier tId = new KamIdentifier((CyNode) e.getTarget());
            if (!sId.equals(tId)) {
                throw new IllegalArgumentException(
                        "Edge nodes can not exist in two different kams");
            }

            Set<CyEdge> es = ret.get(sId);
            if (es == null) {
                es = new HashSet<CyEdge>();
            }
            es.add(e);
        }

        return ret;
    }

    /**
     * Deletes an attribute for the object identified by {@link String id} from
     * the {@link CyAttributes attributes collection}.  The delete is only
     * attempted if the {@link String attr} exists for {@link String id}.
     *
     * <p>
     * Returns {@code true} if the {@link String attr} existed and was deleted
     * for {@link String id}, {@code false} if {@link String attr} did not
     * exist for {@link String id}.
     *
     * @param attrs {@link CyAttributes}; may not be {@code null}
     * @param id {@link String}; may not be {@code null}
     * @param attr {@link String}; may not be {@code null}
     * @return {@code true} if the {@link String attr} existed and was deleted
     * for {@link String id}, {@code false} if {@link String attr} did not
     * exist for {@link String id}
     */
    public static boolean safeDeleteAttribute(CyAttributes attrs, String id,
            String attr) {
        if (attrs == null) throw new NullPointerException();
        if (id == null) throw new NullPointerException();
        if (attr == null) throw new NullPointerException();

        if (attrs.hasAttribute(id, attr))
            return attrs.deleteAttribute(id, attr);
        return false;
    }

    // convience method
    // XXX this could be slow performing
    // might be better to associate a kamid with a network in the session
    public static KamIdentifier getKamNodeId(Collection<CyNode> cynodes) {
        Map<KamIdentifier, Set<CyNode>> map = getKamNodeIds(cynodes);
        if (map.keySet().size() != 1) {
            return null;
        }
        return map.keySet().iterator().next();
    }

    private static CyNode findCyNode(CyNetwork cyn, KamNode kamNode) {
        @SuppressWarnings("unchecked")
        final List<CyNode> nodes = cyn.nodesList();
        for (final CyNode node : nodes) {
            final String kamNodeId = nodeAtt.getStringAttribute(
                    node.getIdentifier(), KAM_NODE_ID_ATTR);
            // if the kamNodeId is null, the node isn't a kam node
            if (kamNodeId != null && kamNodeId.equals(kamNode.getId())) {
                return node;
            }
        }

        return null;
    }
}
