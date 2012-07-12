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

import java.util.List;

import org.openbel.cytoscape.webservice.Configuration;
import org.openbel.cytoscape.webservice.KamService;
import org.openbel.cytoscape.webservice.KamServiceFactory;

import org.openbel.framework.ws.model.DialectHandle;
import org.openbel.framework.ws.model.KAMLoadStatus;
import org.openbel.framework.ws.model.Kam;
import org.openbel.framework.ws.model.KamHandle;
import org.openbel.framework.ws.model.LoadKamResponse;

/**
 * Class to handle loading of Kams
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
public class KamLoader {

    private final KamService kamService;
    private final int SLEEP_TIME_MS = 1000;
    // marked as volatile in case halt is called by multiple threads
    private volatile boolean halt = false;

    public KamLoader() {
        this.kamService = KamServiceFactory.getInstance().getKAMService();
    }

    public void halt() {
        this.halt = true;
    }

    public KamHandle load(KamIdentifier kamId) throws KAMLoadException {
        if (!Configuration.getInstance().getWSDLURL()
                .equals(kamId.getWsdlUrl())) {
            throw new KAMLoadException("Currently configured around WSDL "
                    + Configuration.getInstance().getWSDLURL()
                    + " does not match KAM WSDL " + kamId.getWsdlUrl());
        }

        List<Kam> kamCatalog = kamService.getCatalog();
        Kam kam = null;
        for (Kam k : kamCatalog) {
            if (k.getName().equals(kamId.getName())
                    && k.getLastCompiled().toGregorianCalendar()
                            .getTimeInMillis() == kamId.getCompiledTime()) {
                kam = k;
                break;
            }
        }
        if (kam == null) {
            throw new KAMLoadException("Couldn't find KAM " + kamId.getName()
                    + " in KAM catalog");
        }
        return load(kam);
    }

    public KamHandle load(Kam kam) throws KAMLoadException {
        KamHandle kamHandle = loadKAMHandle(kam);
        storeKamHandle(kam, kamHandle);
        return kamHandle;
    }

    /**
     * Calls out to Web API using {@link KAMServices} and loads a specific
     * {@link Kam kam}.
     * 
     * @throws KAMLoadException
     * 
     * @see KAMServices#loadKam(Kam)
     */
    private KamHandle loadKAMHandle(Kam kam) throws KAMLoadException {

        LoadKamResponse res = kamService.loadKam(kam);
        while (!halt && res.getLoadStatus() == KAMLoadStatus.IN_PROCESS) {
            // sleep and then retry
            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                halt = true;
            }

            res = kamService.loadKam(kam);
        }

        if (res.getLoadStatus() == KAMLoadStatus.COMPLETE) {
            return res.getHandle();
        } else if (res.getLoadStatus() == KAMLoadStatus.FAILED) {
            throw new KAMLoadException("KAM Load Failed");
        }
        // else still in progress and was canceled
        return null;
    }

    private void storeKamHandle(Kam kam, KamHandle kamHandle) {
        if (kamHandle == null) {
            return;
        }
        // load default dialect handle
        DialectHandle dialectHandle = kamService.getDefaultDialect(kamHandle);
        KamSession.getInstance().addKam(kam, kamHandle, dialectHandle);
    }

    public static class KAMLoadException extends Exception {
        private static final long serialVersionUID = 3131380766706167752L;

        public KAMLoadException(String message) {
            super(message);
        }

        public KAMLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
