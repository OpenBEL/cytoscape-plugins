package org.openbel.belframework.webservice;

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
