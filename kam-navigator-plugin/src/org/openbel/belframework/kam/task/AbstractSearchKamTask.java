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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openbel.belframework.kam.KAMSession;
import org.openbel.belframework.kam.KamIdentifier;
import org.openbel.belframework.kam.Utility;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.DialectHandle;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.FunctionTypeFilterCriteria;
import com.selventa.belframework.ws.client.KamHandle;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.Namespace;
import com.selventa.belframework.ws.client.NamespaceValue;
import com.selventa.belframework.ws.client.NodeFilter;

import cytoscape.logger.CyLogger;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

/**
 * Abstract {@link Task cytoscape task} to handle searching for {@link KamNode
 * kam nodes} using the Web API.
 * 
 * Any needed UI updates will have to be implemented by subclasses
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
public abstract class AbstractSearchKamTask implements Task {
    
    private static final CyLogger log = CyLogger.getLogger(AbstractSearchKamTask.class);

    private final KamIdentifier kamId;
    private final FunctionType function;
    private final Namespace namespace;
    private final Collection<String> identifiers;
    private final KAMService kamService;
    private final boolean functionOnly;

    private TaskMonitor monitor;

    // marked as volatile in case halt is called by multiple threads
    private volatile boolean halt = false;

    public AbstractSearchKamTask(KamIdentifier kamId,
            FunctionType function) {
        this(kamId, function, null, null);
    }

    public AbstractSearchKamTask(KamIdentifier kamId,
            FunctionType function, Namespace namespace,
            List<String> identifiers) {
        if (namespace != null && Utility.isEmpty(identifiers)) {
            throw new IllegalArgumentException(
                    "Can't search namespace without identifiers");
        }
        if (namespace == null && !Utility.isEmpty(identifiers)) {
            throw new IllegalArgumentException(
                    "Can't search identifiers without namespace");
        }

        this.kamId = kamId;
        this.function = function;
        this.namespace = namespace;
        this.identifiers = identifiers;

        this.kamService = KAMServiceFactory.getInstance().getKAMService();
        if (function != null && namespace == null) {
            functionOnly = true;
        } else {
            functionOnly = false;
        }
    }

    /**
     * Update any elements of the UI
     * 
     * @param nodes
     *            nodes found from search
     */
    protected abstract void updateUI(Collection<KamNode> nodes);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return "Searching KAM Nodes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTaskMonitor(TaskMonitor monitor)
            throws IllegalThreadStateException {
        this.monitor = monitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void halt() {
        this.halt = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        monitor.setStatus("Searching for KAM Nodes");

        monitor.setPercentCompleted(0);
        List<KamNode> nodes = searchKAMNodes();
        
        // TODO update UI should still be called if the halt command is issued
        // to perform clean up, etc
        if (!halt && nodes != null) {
            // sort nodes by label
            Collections.sort(nodes, new Comparator<KamNode>() {
                @Override
                public int compare(KamNode o1, KamNode o2) {
                    if (o1 == null ^ o2 == null) {
                        return (o1 == null) ? -1 : 1;
                    }
                    if (o1 == null && o2 == null) {
                        return 0;
                    }

                    return o1.getLabel().compareTo(o2.getLabel());
                }
            });
            
            updateUI(nodes);
        }
        monitor.setPercentCompleted(100);
    }

    private List<KamNode> searchKAMNodes() {
        ExecutorService e = Executors.newSingleThreadExecutor();
        Future<List<KamNode>> future = e.submit(buildCallable());

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
            log.warn("Error searching kam nodes", ex);
            return null;
        } catch (ExecutionException ex) {
            log.warn("Error searching kam nodes", ex);
            return null;
        }
    }

    private Callable<List<KamNode>> buildCallable() {
        KamHandle kamHandle = KAMSession.getInstance().getKamHandle(kamId);
        DialectHandle dialectHandle = KAMSession.getInstance()
                .getDialectHandle(kamId);

        if (functionOnly) {
            return new SingleFunctionSearch(kamHandle, dialectHandle, function,
                    kamService);
        }

        NodeFilter nodeFilter = null;
        if (function != null) {
            nodeFilter = buildFunctionFilter(function);
        }

        List<Namespace> namespaces = null;
        if (namespace != null) {
            namespaces = new ArrayList<Namespace>();
            namespaces.add(namespace);
        }

        List<String> patterns = null;
        if (!Utility.isEmpty(identifiers)) {
            boolean rightOnlyWildcard = true;
            // TODO implement logic for when to use right side only wildcards
            patterns = buildRegexPatterns(identifiers, rightOnlyWildcard);
        }

        return new NamespaceSearch(kamHandle, dialectHandle, nodeFilter,
                namespaces, patterns, kamService);
    }

    private static NodeFilter buildFunctionFilter(FunctionType function) {
        final NodeFilter nf = new NodeFilter();
        final FunctionTypeFilterCriteria ftfc = new FunctionTypeFilterCriteria();
        ftfc.setIsInclude(true);
        ftfc.getValueSet().add(function);
        nf.getFunctionTypeCriteria().add(ftfc);
        return nf;
    }

    private static List<String> buildRegexPatterns(
            Collection<String> identifiers, boolean rightOnlyWildcard) {
        final String wildCard = ".*";

        List<String> patterns = new ArrayList<String>();
        for (final String identifier : identifiers) {
            String pattern = identifier + wildCard;

            if (!rightOnlyWildcard) {
                pattern = wildCard + identifier;
            }

            patterns.add(pattern);
        }
        return patterns;
    }

    private static class SingleFunctionSearch implements
            Callable<List<KamNode>> {

        private final KamHandle kamHandle;
        private final DialectHandle dialectHandle;
        private final FunctionType function;
        private final KAMService kamService;

        public SingleFunctionSearch(KamHandle kamHandle,
                DialectHandle dialectHandle, FunctionType function,
                KAMService kamService) {
            if (kamHandle == null || dialectHandle == null || function == null
                    || kamService == null) {
                throw new IllegalArgumentException("Null parameter");
            }

            this.kamHandle = kamHandle;
            this.dialectHandle = dialectHandle;
            this.function = function;
            this.kamService = kamService;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<KamNode> call() throws Exception {
            // find kam nodes by function
            return kamService.findKamNodesByFunction(kamHandle, dialectHandle,
                    function);
        }
    }

    // FIXME we don't need to use patterns, can construct namespace value
    // directly, at least for existing searches
    private class NamespaceSearch implements Callable<List<KamNode>> {

        private final KamHandle kamHandle;
        private final DialectHandle dialectHandle;
        private final NodeFilter nodeFilter;
        private final Collection<Namespace> namespaces;
        private final Collection<String> patterns;
        private final KAMService kamService;

        public NamespaceSearch(KamHandle kamHandle,
                DialectHandle dialectHandle, NodeFilter nodeFilter,
                Collection<Namespace> namespaces, Collection<String> patterns,
                KAMService kamService) {
            this.kamHandle = kamHandle;
            this.dialectHandle = dialectHandle;
            this.nodeFilter = nodeFilter;
            this.namespaces = namespaces;
            this.patterns = patterns;
            this.kamService = kamService;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<KamNode> call() throws Exception {
            List<NamespaceValue> namespaceValues = kamService
                    .findNamespaceValues(patterns, namespaces);
            if (halt) {
                return null;
            }

            if (Utility.isEmpty(namespaceValues)) {
                // nothing found, different from null being returned
                return new ArrayList<KamNode>();
            }

            return kamService.findKamNodesByNamespaceValues(kamHandle,
                    dialectHandle, namespaceValues, nodeFilter);
        }
    }
}
