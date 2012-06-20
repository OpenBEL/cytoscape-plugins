package org.openbel.cytoscape.navigator.dialog.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openbel.cytoscape.navigator.EdgeOption;
import org.openbel.cytoscape.navigator.KAMOption;
import org.openbel.cytoscape.navigator.KamIdentifier;
import org.openbel.cytoscape.navigator.Utility;
import org.openbel.cytoscape.navigator.dialog.model.SearchKamModel;
import org.openbel.cytoscape.navigator.dialog.view.SearchKamView;
import org.openbel.cytoscape.navigator.task.AbstractSearchKamTask;
import org.openbel.cytoscape.navigator.task.KAMTasks;
import org.openbel.cytoscape.webservice.Configuration;
import org.openbel.cytoscape.webservice.KAMService;
import org.openbel.cytoscape.webservice.KAMServiceFactory;
import org.openbel.framework.ws.model.EdgeDirectionType;
import org.openbel.framework.ws.model.FunctionType;
import org.openbel.framework.ws.model.Kam;
import org.openbel.framework.ws.model.KamNode;
import org.openbel.swing.mvc.AbstractController;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;

public class SearchKamController extends AbstractController {

    private CyNetwork lastSearchedNetwork = null;
    private KamIdentifier lastSearchedKamId = null;
    private final KAMService kamService;
    
    public SearchKamController() {
        // TODO models
        addModel(new SearchKamModel());
        addView(new SearchKamView(this));
        
        this.kamService = KAMServiceFactory.getInstance().getKAMService();
    }

    public void add(EdgeOption eeo) {
        ResultsTableModel rtm = (ResultsTableModel) resultsTable.getModel();
        final List<KamNode> nodes = rtm.getNodes();

        // hold the selected KAM nodes we're interested in
        final List<KamNode> selectedNodes = new ArrayList<KamNode>();

        // determine selected rows from the filtered view
        int[] viewIndices = resultsTable.getSelectedRows();
        for (int viewIndex : viewIndices) {
            int modelIndex = resultsTable.convertRowIndexToModel(viewIndex);

            KamNode selectedNode = nodes.get(modelIndex);
            selectedNodes.add(selectedNode);
        }

        // run add task, hook in edges based on selected option
        switch (eeo) {
        case ALL_EDGES:
            KAMTasks.addNodesAndExpand(lastSearchedNetwork, lastSearchedKamId,
                    selectedNodes, EdgeDirectionType.BOTH);
            break;
        case DOWNSTREAM:
            KAMTasks.addNodesAndExpand(lastSearchedNetwork, lastSearchedKamId,
                    selectedNodes, EdgeDirectionType.FORWARD);
            break;
        case INTERCONNECT:
            KAMTasks.addNodesAndInterconnect(lastSearchedNetwork,
                    lastSearchedKamId, selectedNodes);
            break;
        case NONE:
            KAMTasks.addNodes(lastSearchedNetwork, lastSearchedKamId,
                    selectedNodes);
            break;
        case UPSTREAM:
            KAMTasks.addNodesAndExpand(lastSearchedNetwork, lastSearchedKamId,
                    selectedNodes, EdgeDirectionType.REVERSE);
            break;
        }
    }

    public void changeKam() {
        resultsTable.getSelectionModel().clearSelection();

        ResultsTableModel model = (ResultsTableModel) resultsTable.getModel();
        model.clear();
        model.fireTableDataChanged();
    }
    
    public void dispose() {
        final ResultsTableModel model = (ResultsTableModel) resultsTable
                .getModel();
        model.clear();
    }
    
    public void clearNodes() {
        // FIXME clear nodes in model
    }
    
    public List<KamNode> getNodes() {
        // FIXME get nodes from model
    }
    
    public void setNodes(Collection<KamNode> nodes) {
        // FIXME update model
    }
    
    public List<KAMOption> getKamOptions() {
        List<Kam> kamCatalog = kamService.getCatalog();
        List<KAMOption> kamOptions = new ArrayList<KAMOption>(kamCatalog.size());
        for (Kam kam : kamCatalog) {
            kamOptions.add(new KAMOption(kam));
        }
        Collections.sort(kamOptions);
        return kamOptions;
    }

    public void search(FunctionType selffunc) {

        this.lastSearchedNetwork = Cytoscape.getCurrentNetwork();
        KAMOption kamOpt = (KAMOption) kamCmb.getSelectedItem();
        this.lastSearchedKamId = new KamIdentifier(kamOpt.getKam(),
                Configuration.getInstance().getWSDLURL());

        final Task task = new AbstractSearchKamTask(lastSearchedKamId, selfunc) {

            @Override
            protected void updateUI(Collection<KamNode> nodes) {
                ResultsTableModel rtm = (ResultsTableModel) resultsTable
                        .getModel();
                rtm.setData(nodes);
                int nodeCount = nodes.size();
                filterTxt.setEnabled(nodeCount > 0);
                if (nodeCount == 1) {
                    resultsCount.setText(nodes.size() + " node found.");
                } else {
                    resultsCount.setText(nodes.size() + " nodes found.");
                }
            }
        };
        Utility.executeTask(task);
    }
}
