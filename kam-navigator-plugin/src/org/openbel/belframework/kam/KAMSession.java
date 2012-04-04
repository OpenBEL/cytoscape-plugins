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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cytoscape.CyNetwork;

/**
 * {@link KAMSession} tracks the {@link Set set} of the loaded
 * {@link KAMNetwork kam networks}.
 *
 * <p>
 * This object is a singleton.
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMSession {
    private static KAMSession instance;
    private Set<KAMNetwork> kamNetworks;

    public static synchronized KAMSession getInstance() {
        if (instance == null) {
            instance = new KAMSession();
        }

        return instance;
    }

    private KAMSession() {
        kamNetworks = new HashSet<KAMNetwork>();
    }

    public Set<KAMNetwork> getKAMNetworks() {
        return kamNetworks;
    }

    /**
     * Convenience method to retrieve {@link CyNetwork CyNetworks} in session
     * 
     * @return a sorted {@link List} of {@link CyNetwork CyNetworks} backing the
     *         {@link KAMNetwork KAMNetworks} in the session. Can be empty but
     *         not null.
     */
    public List<CyNetwork> getKamBackedNetworks() {
        List<CyNetwork> networks = new ArrayList<CyNetwork>();
        for (KAMNetwork kn : kamNetworks) {
            networks.add(kn.getCyNetwork());
        }
        Collections.sort(networks, new Comparator<CyNetwork>() {
            @Override
            public int compare(CyNetwork o1, CyNetwork o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
        return networks;
    }

    public KAMNetwork getKAMNetwork(final CyNetwork cyn) {
        for (final KAMNetwork kamNetwork : kamNetworks) {
            if (kamNetwork.getCyNetwork() == cyn) {
                return kamNetwork;
            }
        }

        return null;
    }
}
