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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openbel.belframework.webservice.Configuration;

import com.selventa.belframework.ws.client.DialectHandle;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamHandle;

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
    private Map<KamIdentifier, KamHandle> kamHandles = new HashMap<KamIdentifier, KamHandle>();
    private Map<KamIdentifier, DialectHandle> dialectHandles = new HashMap<KamIdentifier, DialectHandle>();

    public static synchronized KAMSession getInstance() {
        if (instance == null) {
            instance = new KAMSession();
        }

        return instance;
    }

    public synchronized void addKam(Kam kam, KamHandle kamHandle, DialectHandle dialectHandle) {
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

    public synchronized DialectHandle getDialectHandle(KamIdentifier kamIdentifier) {
        return dialectHandles.get(kamIdentifier);
    }

    private KAMSession() {
        // singleton. use get instance
    }
}
