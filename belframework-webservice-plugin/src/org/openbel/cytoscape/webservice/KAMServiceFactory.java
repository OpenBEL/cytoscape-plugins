/*
 * BEL Framework Webservice Plugin
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
package org.openbel.cytoscape.webservice;

/**
 * {@link KAMServiceFactory} creates the {@link KAMService service interface} to
 * the BEL Framework Web API.
 *
 * <p>
 * This class is a singleton to provide a single instance of
 * {@link KAMService}.
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMServiceFactory {
	private static KAMServiceFactory instance;
	private KAMService kamService;

	/**
	 * Retrieve the singleton instance of {@link KAMServiceFactory}.
	 *
	 * @return the singleton instance
	 */
	public static synchronized KAMServiceFactory getInstance() {
		if (instance == null) {
			instance = new KAMServiceFactory();
		}

		return instance;
	}

	/**
	 * Retrieve the single instance {@link KAMService}.
	 *
	 * @return the {@link KAMService kam service}
	 */
	public KAMService getKAMService() {
		return kamService;
	}

	/**
	 * Private constructor for singleton.
	 */
	private KAMServiceFactory() {
		this.kamService = new DefaultKAMService();
	}
}
