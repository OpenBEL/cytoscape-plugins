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
package org.openbel.belframework.kam;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.selventa.belframework.ws.client.FunctionType;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

/**
 * Misc static utility methods.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class Utility {
    private static FunctionType[] functionArray;

    static {
        // initialize the supported functions, remove UNKNOWN and LIST
        final FunctionType[] funcs = FunctionType.values();
        final List<FunctionType> flist = new ArrayList<FunctionType>(funcs.length);
        for (final FunctionType f : funcs) {
            if (f != FunctionType.UNKNOWN && f != FunctionType.LIST) {
                flist.add(f);
            }
        }

        functionArray = flist.toArray(new FunctionType[flist.size()]);
    }

    /**
     * Executes the {@link Task task} using the cytoscape
     * {@link TaskManager task manager}.
     *
     * @param task the {@link Task task} to execute, which cannot be null
     * @throws IllegalArgumentException Thrown if {@code task} is {@code null}
     */
    public static void executeTask(final Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task cannot be null");
        }

        JTaskConfig taskcfg = new JTaskConfig();
        taskcfg.setOwner(Cytoscape.getDesktop());
        taskcfg.displayCloseButton(true);
        taskcfg.displayCancelButton(true);
        taskcfg.displayStatus(true);
        taskcfg.setAutoDispose(true);
        TaskManager.executeTask(task, taskcfg);
    }

    /**
     * Retrieve the array of {@link FunctionType functions} supported by the
     * BEL Framework.
     *
     * @return the supported {@link FunctionType functions}
     */
    public static FunctionType[] getFunctions() {
        return functionArray;
    }
    
    /**
     * Retrieve a set of all kam backed networks
     * 
     * @return kam backed networks
     */
    // TODO this method would be unneeded if the KAMSession was updated when a 
    // network was closed
    public static Set<CyNetwork> getKamNetworks() {
        KAMSession session = KAMSession.getInstance();
        Set<CyNetwork> allNetworks = Cytoscape.getNetworkSet();
        Set<CyNetwork> kamNetworks = new HashSet<CyNetwork>();

        for (Iterator<CyNetwork> it = allNetworks.iterator(); it.hasNext();) {
            CyNetwork cyn = it.next();
            // only add cytoscape network if it's KAM-backed
            if (session.getKAMNetwork(cyn) != null) {
                kamNetworks.add(cyn);
            }
        }

        return kamNetworks;
    }
    
    /**
     * Closes a {@link Closeable} silently
     * 
     * @param closable
     */
    public static void closeSilently(final Closeable closable) {
        if (closable == null) {
            return;
        }
        
        try {
            closable.close();
        } catch (IOException e) {
            // silently
        }
    }
    
    private Utility() {
        // prevent instantiation
    }
}
