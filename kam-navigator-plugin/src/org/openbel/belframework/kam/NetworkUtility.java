package org.openbel.belframework.kam;

import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_COMPILE_DATE_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_EDGE_ID_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NAME_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_FUNCTION_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_ID_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_LABEL_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.WSDL_URL_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;

// FIXME update all javadocs
public class NetworkUtility {

    private static final CyAttributes nodeAtt = Cytoscape.getNodeAttributes();
    private static final CyAttributes edgeAtt = Cytoscape.getEdgeAttributes();

    /**
     * Adds a {@link KamNode kam node} to the {@link CyNetwork cytoscape
     * network} and returns the equivalent {@link CyNode cytoscape node}.
     * 
     * @param node
     *            the {@link KamNode kam node} to add
     * @return the {@link CyNode cytoscape node} created for the specific
     *         {@link KamNode kam node}
     */
    public static CyNode addNode(CyNetwork cyn, KamIdentifier kamId,
            KamNode kamNode) {
        // create cytoscape node and attach KAM attributes
        CyNode cynode = Cytoscape.getCyNode(kamNode.getLabel(), true);
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NODE_ID_ATTR,
                kamNode.getId());
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NODE_FUNCTION_ATTR,
                kamNode.getFunction().name());
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NODE_LABEL_ATTR,
                kamNode.getLabel());
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NAME_ATTR,
                kamId.getName());
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_COMPILE_DATE_ATTR,
                Long.toString(kamId.getCompiledTime()));
        nodeAtt.setAttribute(cynode.getIdentifier(), WSDL_URL_ATTR,
                kamId.getWsdlUrl());

        cyn.addNode(cynode);
        return cynode;
    }

    /**
     * Adds a {@link KamEdge kam edge} to the {@link CyNetwork cytoscape
     * network} and returns the equivalent {@link CyEdge cytoscape edge}.
     * 
     * @param edge
     *            the {@link KamEdge kam edge} to add
     * @return the {@link CyEdge cytoscape edge} created for the specific
     *         {@link KamEdge kam edge}
     */
    public static CyEdge addEdge(CyNetwork cyn, KamIdentifier kamId, KamEdge edge) {
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
                Semantics.INTERACTION, edge.getRelationship().toString(), true,
                true);
        edgeAtt.setAttribute(cye.getIdentifier(), KAM_EDGE_ID_ATTR,
                edge.getId());

        cyn.addEdge(cye);

        return cye;
    }

    /**
     * Retrieve all the {@link KamNode kam nodes} for a {@link Collection} of
     * {@link CyNode cytoscape nodes}.
     * 
     * @param cynodes
     *            {@link Collection} of {@link CyNode cytoscape nodes}
     * @return {@link List} of {@link KamNode kam nodes}, which will not be
     *         {@code null} but might be empty
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
     * @param cynode
     *            the {@link CyNode cytoscape node}
     * @return the {@link KamNode kam node} equivalent of the specific
     *         {@link CyNode cytoscape node}, null if the cynode is not kam
     *         backed
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
        final String lbl = nodeAtt.getStringAttribute(id, KAM_NODE_LABEL_ATTR);

        final KamNode kamNode = new KamNode();
        kamNode.setId(kamId);
        final FunctionType ftype = FunctionType.valueOf(func);
        kamNode.setFunction(ftype);
        kamNode.setLabel(lbl);
        return kamNode;
    }

    /**
     * Retrieve all {@link CyEdge cytoscape edges} in the
     * {@link CyNetwork cytoscape network} as {@link KamEdge kam edges}.
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
     * Retrieve the {@link KamEdge kam edge} for a specific
     * {@link CyEdge cytoscape edge}.
     *
     * @param cyedge the {@link CyEdge cytoscape edge}
     * @return the {@link KamEdge kam edge} equivalent of the specific
     * {@link CyEdge cytoscape edge}
     */
    public static KamEdge getKAMEdge(CyEdge cyedge) {
        if (!isKamBacked(cyedge)) {
            // FIXME add checks for this new behavior
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
