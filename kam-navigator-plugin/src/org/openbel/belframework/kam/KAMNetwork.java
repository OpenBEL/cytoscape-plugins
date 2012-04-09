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

import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_EDGE_ID_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_FUNCTION_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_ID_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_LABEL_ATTR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.selventa.belframework.ws.client.DialectHandle;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamHandle;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.logger.CyLogger;
import cytoscape.view.CyNetworkView;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualStyle;

/**
 * {@link KAMNetwork} is a lightweight object that links a loaded
 * {@link Kam kam} to a {@link CyNetwork cytoscape network}.
 *
 * <p>
 * All {@link KamNode kam nodes} and {@link KamEdge kam edges} should be added
 * to the {@link CyNetwork cytoscape network} by calling
 * {@link #addNode(KamNode)} and {@link #addEdge(KamEdge)} in order to attach
 * the ID as a hidden attribute.
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMNetwork {
    
    private static final CyLogger log = CyLogger.getLogger(KAMNetwork.class);
    private static final CyAttributes nodeAtt = Cytoscape.getNodeAttributes();
    private static final CyAttributes edgeAtt = Cytoscape.getEdgeAttributes();
    private static final String NETWORK_SUFFIX = " (KAM)";
    private static final String KAM_STYLE = "KAM Visualization";
    private final CyNetwork cyn;
    private final KamHandle kamHandle;
    private final DialectHandle dialectHandle;
    private final String name;

    /**
     * Create the {@link KAMNetwork} with the {@link String kam name} and
     * {@link String kam handle}.
     *
     * @param kamName the {@link String kam name}
     * @param kamHandle the {@link String kam handle} that identifies the
     * loaded {@link Kam kam} in the Web API
     */
    public KAMNetwork(final String kamName, final KamHandle kamHandle, 
            final DialectHandle dialectHandle) {
        this.kamHandle = kamHandle;
        this.dialectHandle = dialectHandle;
        this.name = kamName;
        this.cyn = Cytoscape.createNetwork(kamName + NETWORK_SUFFIX, true);
        this.cyn.addSelectEventListener(new NetworkDetailsListener(this));

        loadNetworkStyle();
    }

    /**
     * Retrieve the {@link CyNetwork cytoscape network} created to support the
     * {@link Kam kam}.
     *
     * @return the {@link CyNetwork cytoscape network}
     */
    public CyNetwork getCyNetwork() {
        return cyn;
    }

    /**
     * Retrieve the {@link KamHandle kam handle} of the loaded {@link Kam kam}
     * which identifies it on the Web API.
     *
     * @return the {@link KamHandle kam handle}
     */
    public KamHandle getKAMHandle() {
        return kamHandle;
    }
    
    /**
     * Retrieve the {@link DialectHandle} associated with the loaded {@link Kam}
     * which identifies the dialect on the Web API. If no handle is present,
     * returns <code>null</code>
     * 
     * @return the {@link DialectHandle}, can be null
     */
    public DialectHandle getDialectHandle() {
        return dialectHandle;
    }

    /**
     * Retrieve all {@link CyNode cytoscape nodes} in the
     * {@link CyNetwork cytoscape network} as {@link KamNode kam nodes}.
     *
     * @return the {@link List} of {@link KamNode kam nodes} for all
     * {@link CyNode cytoscape nodes} in the
     * {@link CyNetwork cytoscape network}, which will not be {@code null} but
     * might be empty
     */
    @SuppressWarnings("unchecked")
    public Set<KamNode> getKAMNodes() {
        return getKAMNodes(cyn.nodesList());
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
    public Set<KamNode> getKAMNodes(final Collection<CyNode> cynodes) {
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
     * Retrieve the {@link KamNode kam node} for a specific
     * {@link CyNode cytoscape node}.
     *
     * @param cynode the {@link CyNode cytoscape node}
     * @return the {@link KamNode kam node} equivalent of the specific
     * {@link CyNode cytoscape node}, null if the cynode is not kam backed
     */
    public KamNode getKAMNode(final CyNode cynode) {
        final String id = cynode.getIdentifier();
        final String kamId = nodeAtt.getStringAttribute(id, KAM_NODE_ID_ATTR);
        final String func = nodeAtt.getStringAttribute(id, KAM_NODE_FUNCTION_ATTR);
        final String lbl = nodeAtt.getStringAttribute(id, KAM_NODE_LABEL_ATTR);
        // check to see if the cynode is kam backed
        if (kamId == null || func == null || lbl == null) {
            return null;
        }
        
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
    @SuppressWarnings("unchecked")
    public List<KamEdge> getKAMEdges() {
        final List<CyEdge> cyedges = cyn.edgesList();
        final List<KamEdge> kamEdges = new ArrayList<KamEdge>(cyedges.size());
        for (final CyEdge cyedge : cyedges) {
            kamEdges.add(getKAMEdge(cyedge));
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
    public KamEdge getKAMEdge(final CyEdge cyedge) {
        final KamEdge kamEdge = new KamEdge();

        String kamEdgeId = edgeAtt.getStringAttribute(cyedge.getIdentifier(),
                KAM_EDGE_ID_ATTR);
        kamEdge.setId(kamEdgeId);
        return kamEdge;
    }

    /**
     * @return name of this KAM networks, sans postfix
     */
    public String getName() {
        return name;
    }

    /**
     * Adds a {@link KamNode kam node} to the
     * {@link CyNetwork cytoscape network} and returns the equivalent
     * {@link CyNode cytoscape node}.
     *
     * @param node the {@link KamNode kam node} to add
     * @return the {@link CyNode cytoscape node} created for the specific
     * {@link KamNode kam node}
     */
    public CyNode addNode(final KamNode node) {
        // create cytoscape node and attach KAM node id as hidden attribute
        CyNode cynode = Cytoscape.getCyNode(node.getLabel(), true);
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NODE_ID_ATTR,
                node.getId());
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NODE_FUNCTION_ATTR,
                node.getFunction().name());
        nodeAtt.setAttribute(cynode.getIdentifier(), KAM_NODE_LABEL_ATTR,
                node.getLabel());

        cyn.addNode(cynode);
        return cynode;
    }

    /**
     * Adds a {@link KamEdge kam edge} to the
     * {@link CyNetwork cytoscape network} and returns the equivalent
     * {@link CyEdge cytoscape edge}.
     *
     * @param edge the {@link KamEdge kam edge} to add
     * @return the {@link CyEdge cytoscape edge} created for the specific
     * {@link KamEdge kam edge}
     */
    public CyEdge addEdge(final KamEdge edge) {
        // link up the source node
        final KamNode srckn = (KamNode) edge.getSource();

        CyNode cynsource = findCyNode(srckn);
        if (cynsource == null) {
            cynsource = addNode(srckn);
        }

        // link up the target node
        final KamNode tgtkn = (KamNode) edge.getTarget();

        CyNode cyntarget = findCyNode(tgtkn);
        if (cyntarget == null) {
            cyntarget = addNode(tgtkn);
        }

        // create cytoscape edge and attach KAM edge id as hidden attribute
        CyEdge cye = Cytoscape.getCyEdge(cynsource, cyntarget,
                Semantics.INTERACTION, edge.getRelationship().toString(),
                true, true);
        edgeAtt.setAttribute(cye.getIdentifier(), KAM_EDGE_ID_ATTR,
                edge.getId());

        cyn.addEdge(cye);

        return cye;
    }

    @SuppressWarnings("unchecked")
    private CyNode findCyNode(final KamNode kamNode) {
        final List<CyNode> nodes = cyn.nodesList();
        for (final CyNode node : nodes) {
            final String kamNodeId = nodeAtt.getStringAttribute(
                    node.getIdentifier(), 
                    KAM_NODE_ID_ATTR);
            // if the kamNodeId is null, the node isn't a kam node
            if (kamNodeId != null && kamNodeId.equals(kamNode.getId())) {
                return node;
            }
        }

        return null;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private CyEdge findCyEdge(final KamEdge kamEdge) {
        final List<CyEdge> edges = cyn.edgesList();
        for (final CyEdge edge : edges) {
            final String kamEdgeId = edgeAtt.getStringAttribute(
                    edge.getIdentifier(),
                    KAM_EDGE_ID_ATTR);
            if (kamEdgeId.equals(kamEdge.getId())) {
                return edge;
            }
        }

        return null;
    }

    /**
     * Load the {@link VisualStyle visual style} to apply to new
     * {@link CyNetworkView cytoscape network views}.
     */
    private void loadNetworkStyle() {
        final VisualMappingManager vismanager = Cytoscape.getVisualMappingManager();

        final CalculatorCatalog ccat = vismanager.getCalculatorCatalog();
        VisualStyle visualStyle = ccat.getVisualStyle(KAM_STYLE);
        if (visualStyle == null) {
            loadKAMStyle();
            visualStyle = ccat.getVisualStyle(KAM_STYLE);
        }
        
        if (visualStyle == null) {
            return;
        }

        final CyNetworkView view = Cytoscape.getNetworkView(cyn.getIdentifier());
        view.setVisualStyle(visualStyle.getName());

        vismanager.setVisualStyle(visualStyle);
        view.redrawGraph(true, true);
    }
    
    // TODO better exception handling
    private void loadKAMStyle() {
        String name = "/org/openbel/belframework/kam/style.props";
        InputStream in = getClass().getResourceAsStream(name);
        File f = null;
        try {
            f = File.createTempFile("viz", null);
            writeInputStreamIntoFile(in, f);
        } catch (IOException e) {
            log.warn("Error loading style", e);
            return;
        } finally {
            Utility.closeSilently(in);
        }
        
        if (!f.exists() || !f.canRead()) {
            return;
        }
        
        // load style
        Cytoscape.firePropertyChange(Cytoscape.VIZMAP_LOADED, null, f.getAbsolutePath());
    }
    
    private static void writeInputStreamIntoFile(InputStream in, File f)
            throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = bis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } finally {
            Utility.closeSilently(bis);
            Utility.closeSilently(fos);
        }
    }
}
