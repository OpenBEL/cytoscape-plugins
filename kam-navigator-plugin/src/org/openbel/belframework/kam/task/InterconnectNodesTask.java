package org.openbel.belframework.kam.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.SimplePath;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.task.Task;

/**
 * Package-protected {@link Task task} to add {@link KamEdge kam edges} from
 * interconnect nodes to a {@link CyNetwork cytoscape network}.
 * 
 * <p>
 * This {@link Task task} should be called by
 * {@link KAMTasks#interconnectNodes(KAMNetwork, Set)}.
 * </p>
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
final class InterconnectNodesTask extends AddEdgesTask {
    // interconnect should always have a max depth of 1, otherwise it is a 
    // pathfind
    private static final int INTERCONNECT_DEPTH = 1;

    private final Set<CyNode> cynodes;
    private final KAMService kamService;

    InterconnectNodesTask(KAMNetwork kamNetwork, Set<CyNode> cynodes) {
        super(kamNetwork, null);
        this.cynodes = cynodes;
        this.kamService = KAMServiceFactory.getInstance().getKAMService();

        if (cynodes == null || cynodes.size() < 2) {
            throw new IllegalArgumentException(
                    "Can't interconnect less then two nodes");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<KamEdge> getEdgesToAdd() {
        final Collection<KamNode> kamNodes = new HashSet<KamNode>();
        for (final CyNode cynode : cynodes) {
            kamNodes.add(kamNetwork.getKAMNode(cynode));
        }

        // TODO do we want to split up interconnect into multiple calls so
        // if we are interconnecting a large number of nodes the user will
        // be able to cancel without waiting for back end to return?
        // or alternatively, put this in a thread?
        final List<SimplePath> paths = kamService.interconnect(
                kamNetwork.getDialectHandle(), kamNodes,
                INTERCONNECT_DEPTH);
        final List<KamEdge> edges = new ArrayList<KamEdge>();
        for (final SimplePath path : paths) {
            if (halt) {
                break;
            }

            edges.addAll(path.getEdges());
        }
        return edges;
    }

}
