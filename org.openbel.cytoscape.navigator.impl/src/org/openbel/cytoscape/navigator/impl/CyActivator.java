package org.openbel.cytoscape.navigator.impl;

import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.model.CyNetworkManager;
import org.openbel.cytoscape.navigator.impl.ExampleTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.cytoscape.service.util.AbstractCyActivator;
import java.util.Properties;

public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {

		CyNetworkManager cyNetworkManagerServiceRef = getService(bc,CyNetworkManager.class);
		CyNetworkNaming cyNetworkNamingServiceRef = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc,CyNetworkFactory.class);
		
		ExampleTaskFactory tf = new ExampleTaskFactory(cyNetworkManagerServiceRef,cyNetworkNamingServiceRef,cyNetworkFactoryServiceRef);
		Properties props = new Properties();
		props.setProperty("preferredMenu","Apps");
		props.setProperty("menuGravity","11.0");
		props.setProperty("title","Sample 5");
		registerService(bc,tf,TaskFactory.class, props);
	}
}

