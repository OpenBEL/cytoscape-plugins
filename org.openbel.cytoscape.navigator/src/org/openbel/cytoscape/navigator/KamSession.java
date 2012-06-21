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
package org.openbel.cytoscape.navigator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openbel.cytoscape.webservice.Configuration;

import org.openbel.framework.ws.model.DialectHandle;
import org.openbel.framework.ws.model.Kam;
import org.openbel.framework.ws.model.KamHandle;

import cytoscape.CyNetwork;
import cytoscape.CyNode;

/**
 * {@link KamSession} tracks the {@link Set set} of the loaded
 * {@link KAMNetwork kam networks}.
 * 
 * <p>
 * This object is a singleton.
 * </p>
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
public class KamSession {
    private static KamSession instance;
    private Map<KamIdentifier, KamHandle> kamHandles = new HashMap<KamIdentifier, KamHandle>();
    private Map<KamIdentifier, DialectHandle> dialectHandles = new HashMap<KamIdentifier, DialectHandle>();
    // right there should only be one kam associated with any given network
    private Map<CyNetwork, KamIdentifier> networkKamIds = new HashMap<CyNetwork, KamIdentifier>();

    public static synchronized KamSession getInstance() {
        if (instance == null) {
            instance = new KamSession();
        }

        return instance;
    }

    public synchronized void addKam(Kam kam, KamHandle kamHandle,
            DialectHandle dialectHandle) {
        KamIdentifier kamId = new KamIdentifier(kam, Configuration
                .getInstance().getWSDLURL());
        // TODO do we want to check for existing kam and throw an error if there
        // is?
        kamHandles.put(kamId, kamHandle);
        if (dialectHandle != null) {
            dialectHandles.put(kamId, dialectHandle);
        }
    }

    public synchronized KamHandle getKamHandle(KamIdentifier kamIdentifier) {
        return kamHandles.get(kamIdentifier);
    }

    public synchronized DialectHandle getDialectHandle(
            KamIdentifier kamIdentifier) {
        return dialectHandles.get(kamIdentifier);
    }

    @SuppressWarnings("unchecked")
    public synchronized KamIdentifier getKamIdentifier(CyNetwork network) {
        KamIdentifier kamId = networkKamIds.get(network);
        if (kamId == null) {
            Map<KamIdentifier, Set<CyNode>> map = NetworkUtility
                    .getKamNodeIds(network.nodesList());
            if (map.keySet().size() < 1) {
                // no kam nodes
                return null;
            } else if (map.keySet().size() > 1) {
                // there is more then 1 kam associated with this network
                throw new IllegalStateException(
                        "More then 1 kam associated with elements in network");
            } else {
                // one kam id
                kamId = map.keySet().iterator().next();
                associateNetworkWithKam(network, kamId);
            }
        }
        return kamId;
    }

    public synchronized void associateNetworkWithKam(CyNetwork network,
            KamIdentifier kamId) {
        KamIdentifier previousId = networkKamIds.get(network);
        if (previousId != null && !previousId.equals(kamId)) {
            throw new IllegalArgumentException(
                    "Network is already associated with KAM");
        }
        networkKamIds.put(network, kamId);
    }

    private KamSession() {
        // singleton. use get instance
    }
}
