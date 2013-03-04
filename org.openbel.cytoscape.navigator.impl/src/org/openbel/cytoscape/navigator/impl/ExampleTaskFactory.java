package org.openbel.cytoscape.navigator.impl;

import org.openbel.cytoscape.navigator.api.MyNetworkTask;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ExampleTaskFactory extends AbstractTaskFactory {
	private final CyNetworkManager netMgr;
	private final CyNetworkFactory cnf;
	private final CyNetworkNaming namingUtil; 
	
	public ExampleTaskFactory(final CyNetworkManager netMgr, 
			final CyNetworkNaming namingUtil, final CyNetworkFactory cnf){
		this.netMgr = netMgr;
		this.namingUtil = namingUtil;
		this.cnf = cnf;
	}
	
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new MyNetworkTask(netMgr, namingUtil, cnf));
	}
}

