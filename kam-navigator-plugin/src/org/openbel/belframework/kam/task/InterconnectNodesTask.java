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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.SimplePath;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.logger.CyLogger;
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
    
    private static final CyLogger log = CyLogger.getLogger(InterconnectNodesTask.class);
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
        final Collection<KamNode> kamNodes = kamNetwork.getKAMNodes(cynodes);
        final List<SimplePath> paths = interconnect(kamNodes);
        if (paths == null) {
            return null;
        }

        final List<KamEdge> edges = new ArrayList<KamEdge>();
        for (final SimplePath path : paths) {
            if (halt) {
                break;
            }

            edges.addAll(path.getEdges());
        }
        return edges;
    }

    // TODO this method of interrupting the executor is taken directly
    // from the node search, can we push it up somewhere?
    private List<SimplePath> interconnect(final Collection<KamNode> kamNodes) {
        ExecutorService e = Executors.newSingleThreadExecutor();
        Future<List<SimplePath>> future = e
                .submit(new Callable<List<SimplePath>>() {
                    @Override
                    public List<SimplePath> call() throws Exception {
                        return kamService.interconnect(
                                kamNetwork.getDialectHandle(), kamNodes,
                                INTERCONNECT_DEPTH);
                    }
                });

        while (!(future.isDone() || future.isCancelled()) && !e.isShutdown()) {
            try {
                if (halt) {
                    // this should not block
                    // but be aware that if the thread in the executor is
                    // blocked it will continue to live on
                    e.shutdownNow();

                    future.cancel(true);
                }
                // sleep thread to enable interrupt
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                halt = true;
            }
        }

        if (future.isCancelled()) {
            return null;
        }
        try {
            return future.get();
        } catch (InterruptedException ex) {
            log.warn("Error interconnecting nodes", ex);
            return null;
        } catch (ExecutionException ex) {
            log.warn("Error interconnecting nodes", ex);
            return null;
        }
    }
}
