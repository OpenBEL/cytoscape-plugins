package org.openbel.belframework.kam.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.task.Task;

/**
 * Package-protected {@link Task task} to add {@link KamEdge kam edges} from a
 * node expansion to a {@link CyNetwork cytoscape network}.
 * 
 * <p>
 * This {@link Task task} should be called by
 * {@link KAMTasks#expandNodes(KAMNetwork, Set, EdgeDirectionType)}.
 * </p>
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
final class ExpandNodesTask extends AddEdgesTask {

    private final EdgeDirectionType direction;
    private final Set<CyNode> cynodes;
    private final KAMService kamService;

    ExpandNodesTask(KAMNetwork kamNetwork, Set<CyNode> cynodes,
            EdgeDirectionType direction) {
        super(kamNetwork, null);
        this.cynodes = cynodes;
        this.direction = direction;
        this.kamService = KAMServiceFactory.getInstance().getKAMService();
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

        List<KamEdge> edges = new ArrayList<KamEdge>();
        for (KamNode kamNode : kamNodes) {
            if (halt) {
                break;
            }

            edges.addAll(kamService.getAdjacentKamEdges(
                    kamNetwork.getDialectHandle(), kamNode, direction, null));
        }
        return edges;
    }

}
