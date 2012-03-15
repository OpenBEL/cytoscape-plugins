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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.openbel.belframework.kam.task.KAMTasks;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;

/**
 * {@link KnowledgeNeighborhoodDialog} represents the UI for the Knowledge
 * Neighborhood dialog.
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
public class KnowledgeNeighborhoodDialog extends JDialog implements
        ActionListener, SelectEventListener {
    private static final long serialVersionUID = -736918933072782546L;
    private static final String DIALOG_TITLE = "Knowledge Neighborhood";
    private final KAMService kamService;

    // swing components
    private JButton addButton;
    private JButton cancelButton;
    private JLabel resultsLabel;
    private JTable resultsTable;
    private JLabel selectionLabel;
    private JScrollPane tableScrollPane;

    /**
     * Construct the {@link JDialog dialog} and initialize the UI.
     * 
     * @see #initUI()
     */
    public KnowledgeNeighborhoodDialog() {
        super(Cytoscape.getDesktop(), DIALOG_TITLE, false);
        this.kamService = KAMServiceFactory.getInstance().getKAMService();

        initUI();

        // register listener
        // TODO: what if the KAM network is loaded after?
        // what if the current network is not the KAM network?
        Cytoscape.getCurrentNetwork().addSelectEventListener(this);

        loadNeighborhood();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Implementation Note: Clean up resources used by
     * {@link KnowledgeNeighborhoodDialog}.
     * </p>
     */
    @Override
    public void dispose() {
        super.dispose();

        Cytoscape.getCurrentNetwork().removeSelectEventListener(this);
    }

    private void initUI() {
        initComponents();
        
        // additional stuff (kept seprate for future UI work)
        resultsLabel.setText("");
        selectionLabel.setText("");
        cancelButton.addActionListener(this);
        addButton.addActionListener(this);
        addButton.setEnabled(false);
        resultsTable.setModel(new EdgeTableModel());
        resultsTable.getSelectionModel().addListSelectionListener(new ResultsSelectionListener());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e == null) {
            return;
        }

        if (e.getSource().equals(cancelButton)) {
            this.dispose();
        } else if (e.getSource().equals(addButton)) {
            EdgeTableModel model = (EdgeTableModel) resultsTable.getModel();
            List<KamEdge> edges = model.getEdges();

            List<KamEdge> selectedEdges = new ArrayList<KamEdge>();

            // determine selected rows from the filtered view
            int[] viewIndices = resultsTable.getSelectedRows();
            for (int viewIndex : viewIndices) {
                int modelIndex = resultsTable.convertRowIndexToModel(viewIndex);

                KamEdge selectedEdge = edges.get(modelIndex);
                if (selectedEdge != null) {
                    selectedEdges.add(selectedEdge);
                }
            }

            // FIXME change how we get the network
            KAMNetwork kamNetwork = KAMSession.getInstance().getKAMNetwork(
                    Cytoscape.getCurrentNetwork());

            KAMTasks.addEdges(kamNetwork, selectedEdges);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSelectEvent(SelectEvent e) {
        if (e == null) {
            return;
        }

        if (e.getTargetType() == SelectEvent.SINGLE_NODE
                || e.getTargetType() == SelectEvent.NODE_SET) {
            loadNeighborhood();
        }
    }

    private void loadNeighborhood() {
        // TODO change the way we load selected
        @SuppressWarnings("unchecked")
        final Set<CyNode> selected = Cytoscape.getCurrentNetwork()
                .getSelectedNodes();
        if (selected.isEmpty()) {
            // if empty no point in resolve edges
            final EdgeTableModel model = (EdgeTableModel) this.resultsTable
                    .getModel();
            model.clear();
            resultsLabel.setText("Found 0 items");
            return;
        }

        final CyNetwork network = Cytoscape.getCurrentNetwork();
        final KAMNetwork kamNetwork = KAMSession.getInstance().getKAMNetwork(
                network);

        final Collection<KamNode> kamNodes = new HashSet<KamNode>();
        for (final CyNode cynode : selected) {
            kamNodes.add(kamNetwork.getKAMNode(cynode));
        }

        List<KamEdge> edges = new ArrayList<KamEdge>();
        for (KamNode kamNode : kamNodes) {
            edges.addAll(kamService.getAdjacentKamEdges(kamNode,
                    EdgeDirectionType.BOTH, null));
        }

        final EdgeTableModel model = (EdgeTableModel) this.resultsTable
                .getModel();
        model.clear();

        for (KamEdge edge : edges) {
            model.addEdge(edge);
        }
        resultsLabel.setText("Found " + model.getRowCount() + " items");
    }
    
    private class ResultsSelectionListener implements ListSelectionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel selModel = (ListSelectionModel) e.getSource();
            addButton.setEnabled(!selModel.isSelectionEmpty());
            int selectionNumber = resultsTable.getSelectedRows().length;
            selectionLabel.setText(selectionNumber + " items selected");
        }
    }

    private class EdgeTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 56833762228520599L;
        private final List<KamEdge> edges = new ArrayList<KamEdge>();

        private EdgeTableModel() {
            super(new Object[][] {

            }, new String[] { "Source", "Relationship", "Target" });
        }

        Class[] types = new Class[] { String.class, String.class, String.class };
        boolean[] canEdit = new boolean[] { false, false, false };

        public Class getColumnClass(int columnIndex) {
            return types[columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }

        public void addEdge(KamEdge edge) {
            super.addRow(new String[] { edge.getSource().getLabel(),
                    edge.getRelationship().toString(),
                    edge.getTarget().getLabel() });
            edges.add(edge);
        }

        public void clear() {
            dataVector.clear();
            edges.clear();
        }

        public List<KamEdge> getEdges() {
            return edges;
        }
    }

    // taken from netbeans
    private void initComponents() {

        resultsLabel = new JLabel();
        selectionLabel = new JLabel();
        cancelButton = new JButton();
        addButton = new JButton();
        tableScrollPane = new JScrollPane();
        resultsTable = new JTable();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        resultsLabel.setText("Found n items");

        selectionLabel.setText("n items selected");

        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Close this window");
        /*
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        */

        addButton.setText("Add");
        addButton.setToolTipText("Add selected edges to graph");

        // resultsTable.setModel(new EdgeTableModel());
        tableScrollPane.setViewportView(resultsTable);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(tableScrollPane, GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(cancelButton)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addComponent(addButton)))
                        .addGap(20, 20, 20))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(resultsLabel)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(selectionLabel)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(resultsLabel)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(tableScrollPane, GroupLayout.PREFERRED_SIZE, 208, GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(addButton)
                            .addComponent(cancelButton)))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(selectionLabel)))
                .addContainerGap())
        );

        pack();
    }

}
