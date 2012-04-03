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
import java.util.Collection;
import java.util.List;

import com.selventa.belframework.ws.client.FunctionType;

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
     * Executes the {@link Task task} using the cytoscape {@link TaskManager
     * task manager}.
     * 
     * @param task
     *            the {@link Task task} to execute, which cannot be null
     * @return true value indicates that task completed successfully. false
     *         value indicates that task was halted by user or task encountered
     *         an error.
     * @throws IllegalArgumentException
     *             Thrown if {@code task} is {@code null}
     */
    public static boolean executeTask(final Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task cannot be null");
        }

        JTaskConfig taskcfg = new JTaskConfig();
        taskcfg.setOwner(Cytoscape.getDesktop());
        taskcfg.displayCancelButton(true);
        taskcfg.displayStatus(true);
        taskcfg.setAutoDispose(true);
        return TaskManager.executeTask(task, taskcfg);
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

    /**
     * Check to see if the collection is null or empty
     * 
     * @param collection
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }
    
    private Utility() {
        // prevent instantiation
    }
}
