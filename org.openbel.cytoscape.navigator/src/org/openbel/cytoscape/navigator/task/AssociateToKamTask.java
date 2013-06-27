package org.openbel.cytoscape.navigator.task;

import static cytoscape.Cytoscape.getVisualMappingManager;
import static cytoscape.Cytoscape.setCurrentNetworkView;
import static cytoscape.data.Semantics.INTERACTION;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.openbel.cytoscape.navigator.KamNavigatorPlugin.KAM_NODE_FUNCTION_ATTR;
import static org.openbel.cytoscape.navigator.NetworkUtility.disassociate;
import static org.openbel.cytoscape.navigator.NetworkUtility.updateEdge;
import static org.openbel.cytoscape.navigator.NetworkUtility.updateNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.openbel.cytoscape.navigator.KamIdentifier;
import org.openbel.cytoscape.navigator.KamLoader;
import org.openbel.cytoscape.navigator.KamLoader.KAMLoadException;
import org.openbel.cytoscape.navigator.KamSession;
import org.openbel.cytoscape.webservice.KamService;
import org.openbel.cytoscape.webservice.KamServiceFactory;
import org.openbel.framework.ws.model.DialectHandle;
import org.openbel.framework.ws.model.Edge;
import org.openbel.framework.ws.model.FunctionType;
import org.openbel.framework.ws.model.KamEdge;
import org.openbel.framework.ws.model.KamHandle;
import org.openbel.framework.ws.model.KamNode;
import org.openbel.framework.ws.model.Node;
import org.openbel.framework.ws.model.RelationshipType;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.VisualStyle;

class AssociateToKamTask implements Task {

    private static final String TITLE = "Associating %s to %s";
    private static final String STYLE = "KAM Association";
    private final CyNetworkView view;
    private final CyNetwork network;
    private final KamIdentifier kamId;
    private TaskMonitor m;
    private Thread me;

    AssociateToKamTask(CyNetworkView networkView, KamIdentifier kamId) {
        if (networkView == null)
            throw new NullPointerException("networkView is null");
        if (kamId == null)
            throw new NullPointerException("kamId is null");
        this.view = networkView;
        this.network = networkView.getNetwork();
        this.kamId = kamId;
    }

    @Override
    public String getTitle() {
        return format(TITLE, network.getTitle(), kamId.getName());
    }

    @Override
    public void run() {
        me = currentThread();

        m.setPercentCompleted(0);
        m.setStatus("Loading KAM: " + kamId.getName());
        KamLoader kamLoader = new KamLoader();
        KamHandle handle;
        try {
            handle = kamLoader.load(kamId);
        } catch (KAMLoadException e) {
            JOptionPane.showMessageDialog(null,
                    "Error loading \"" + kamId.getName()
                            + "\" KAM.\n", "Kam Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        m.setPercentCompleted(20);

        KamService svc = KamServiceFactory.getInstance().getKAMService();
        DialectHandle dialect = KamSession.getInstance().getDialectHandle(kamId);
        CyAttributes nodeattr = Cytoscape.getNodeAttributes();
        CyAttributes edgeattr = Cytoscape.getEdgeAttributes();

        m.setStatus("Resolving nodes and edges to KAM: " + kamId.getName());

        // resolve nodes
        int nodeCount = network.getNodeCount();
        int[] nodes = network.getNodeIndicesArray();
        List<Node> wsNodes = new ArrayList<Node>(nodeCount);
        List<CyNode> cyNodes = new ArrayList<>(nodes.length);
        for (int idx : nodes) {
            CyNode node = (CyNode) network.getNode(idx);
            String nodeId = node.getIdentifier();
            String f = nodeattr.getStringAttribute(nodeId,
                    KAM_NODE_FUNCTION_ATTR);
            // disassociate if function not set
            if (f == null) {
                disassociate(node);
                continue;
            }

            // disassociate if function not valid
            FunctionType fx;
            try {
                fx = FunctionType.fromValue(f);
            } catch (IllegalArgumentException e) {
                disassociate(node);
                continue;
            }

            // track queryable cytoscape node
            cyNodes.add(node);

            // build up WebAPI Node objects for query
            Node wsNode = new Node();
            wsNode.setFunction(fx);
            wsNode.setLabel(nodeId);
            wsNodes.add(wsNode);
        }

        // if nodes did not resolve; return
        List<KamNode> resolvedNodes = svc.resolveNodes(handle, wsNodes, dialect);
        if (resolvedNodes == null || resolvedNodes.isEmpty()) {
            return;
        }

        m.setPercentCompleted(60);

        // hard assertion; resolveNodes web operation should be congruent
        if (cyNodes.size() != resolvedNodes.size())
            throw new AssertionError("len(cyNodes) != len(resolvedNodes)");

        // update non-null nodes; index to ease resolve edges
        int nc = resolvedNodes.size();
        Map<String, Node> cyNodeResolveMap = new HashMap<String, Node>(nc);
        for (int i = 0; i < nc; i++) {
            CyNode node = cyNodes.get(i);
            if (node == null) continue;

            KamNode resolved = resolvedNodes.get(i);
            if (resolved == null) {
                disassociate(node);
                continue;
            }

            updateNode(node, kamId, resolved);
            cyNodeResolveMap.put(node.getIdentifier(), resolved);
        }

        // resolve edges
        int edgeCount = network.getEdgeCount();
        int[] edges = network.getEdgeIndicesArray();
        List<Edge> wsEdges = new ArrayList<Edge>(edgeCount);
        List<CyEdge> cyEdges = new ArrayList<CyEdge>(edgeCount);
        for (int idx : edges) {
            CyEdge edge = (CyEdge) network.getEdge(idx);
            String relationship = edgeattr.getStringAttribute(
                    edge.getIdentifier(), INTERACTION);

            RelationshipType rel;
            try {
                rel = RelationshipType.fromValue(relationship);
            } catch (IllegalArgumentException e) {
                // relationship is unknown; disassociate
                disassociate(edge);
                continue;
            }
            CyNode source = (CyNode) edge.getSource();
            CyNode target = (CyNode) edge.getTarget();
            Node sourceNode = cyNodeResolveMap.get(source.getIdentifier());
            Node targetNode = cyNodeResolveMap.get(target.getIdentifier());

            if (rel != null && sourceNode != null && targetNode != null) {
                Edge wsEdge = new Edge();
                wsEdge.setRelationship(rel);
                wsEdge.setSource(sourceNode);
                wsEdge.setTarget(targetNode);
                wsEdges.add(wsEdge);
                cyEdges.add(edge);
            } else {
                disassociate(edge);
            }
        }

        if (!wsEdges.isEmpty()) {
            List<KamEdge> resolvedEdges = svc.resolveEdges(handle, wsEdges, dialect);

            Iterator<KamEdge> eit = resolvedEdges.iterator();
            for (CyEdge edge : cyEdges) {
                KamEdge resolved = eit.next();
                if (resolved != null) {
                    updateEdge(edge, kamId, resolved);
                } else {
                    disassociate(edge);
                }
            }
        }

        CalculatorCatalog cat = getVisualMappingManager()
                .getCalculatorCatalog();
        VisualStyle vstl = cat.getVisualStyle(STYLE);

        if (vstl != null) {
            view.setVisualStyle(vstl.getName());
            getVisualMappingManager().setVisualStyle(vstl);
            view.redrawGraph(true, true);
            setCurrentNetworkView(view.getIdentifier());
        }

        m.setPercentCompleted(100);
    }

    @Override
    public void halt() {
        // assuming this happens on a separate thread
        me.interrupt();
    }

    @Override
    public void setTaskMonitor(TaskMonitor m)
            throws IllegalThreadStateException {
        this.m = m;
    }
}
