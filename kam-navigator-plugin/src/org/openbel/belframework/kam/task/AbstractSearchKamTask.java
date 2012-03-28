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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.KamNode;

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

    private final KAMNetwork kamNetwork;
    private final FunctionType function;
    private final KAMService kamService;

    private TaskMonitor monitor;

    // marked as volatile in case halt is called by multiple threads
    private volatile boolean halt = false;

    public AbstractSearchKamTask(final KAMNetwork kamNetwork,
            final FunctionType function) {
        this.kamNetwork = kamNetwork;
        this.function = function;

        this.kamService = KAMServiceFactory.getInstance().getKAMService();
    }

    /**
     * Update any elements of the UI
     * 
     * @param nodes nodes found from search
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
        monitor.setStatus("Searching for " + function + " functions.");

        monitor.setPercentCompleted(0);
        List<KamNode> nodes = searchKAMNodes();
        if (!halt && nodes != null) {
            updateUI(nodes);
        }
        monitor.setPercentCompleted(100);
    }

    private List<KamNode> searchKAMNodes() {
        ExecutorService e = Executors.newSingleThreadExecutor();
        Future<List<KamNode>> future = e.submit(new Callable<List<KamNode>>() {

            @Override
            public List<KamNode> call() {
                // find kam nodes by function
                return kamService.findKamNodesByFunction(
                        kamNetwork.getKAMHandle(),
                        kamNetwork.getDialectHandle(), function);
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
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return null;
        } catch (ExecutionException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return null;
        }
    }
}
