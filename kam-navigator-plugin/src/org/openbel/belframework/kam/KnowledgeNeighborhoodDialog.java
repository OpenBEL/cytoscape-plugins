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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
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
    private JLabel edgeLabel;
    private JComboBox edgeRelationshipCombo;
    private JLabel edgeRelationshipLabel;
    private JRadioButton expandBothButton;
    private ButtonGroup expandButtonGroup;
    private JRadioButton expandDownstreamButton;
    private JLabel expandLabel;
    private JRadioButton expandUpstreamButton;
    private JPanel filterPanel;
    private JLabel resultsLabel;
    private JTable resultsTable;
    private JLabel selectionLabel;
    private JSeparator separator;
    private JComboBox sourceFunctionCombo;
    private JLabel sourceFunctionLabel;
    private JLabel sourceLabel;
    private JTextField sourceLabelField;
    private JLabel sourceLabelLabel;
    private JScrollPane tableScrollPane;
    private JComboBox targetFunctionCombo;
    private JLabel targetFunctionLabel;
    private JLabel targetLabel;
    private JTextField targetLabelField;
    private JLabel targetLabelLabel;

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
        
        // additional stuff (kept seperate for future UI work)
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
        // TODO put this a thread so it doesn't slow down other UI actions
        
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
            edges.addAll(kamService.getAdjacentKamEdges(
                    kamNetwork.getDialectHandle(), kamNode, 
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

        @SuppressWarnings("rawtypes")
        Class[] types = new Class[] { String.class, String.class, String.class };
        boolean[] canEdit = new boolean[] { false, false, false };

        @SuppressWarnings({ "rawtypes", "unchecked" })
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
            fireTableDataChanged();
        }

        public void clear() {
            dataVector.clear();
            edges.clear();
            fireTableDataChanged();
        }

        public List<KamEdge> getEdges() {
            return edges;
        }
    }

    // taken from netbeans
    // this method was taken auto generated code, apologies if it sucks
    private void initComponents() {

        expandButtonGroup = new ButtonGroup();
        resultsLabel = new JLabel();
        selectionLabel = new JLabel();
        cancelButton = new JButton();
        addButton = new JButton();
        tableScrollPane = new JScrollPane();
        resultsTable = new JTable();
        filterPanel = new JPanel();
        expandLabel = new JLabel();
        expandBothButton = new JRadioButton();
        expandUpstreamButton = new JRadioButton();
        expandDownstreamButton = new JRadioButton();
        edgeLabel = new JLabel();
        edgeRelationshipCombo = new JComboBox();
        edgeRelationshipLabel = new JLabel();
        sourceLabel = new JLabel();
        targetLabel = new JLabel();
        targetFunctionLabel = new JLabel();
        targetFunctionCombo = new JComboBox();
        targetLabelLabel = new JLabel();
        targetLabelField = new JTextField();
        sourceFunctionLabel = new JLabel();
        sourceLabelLabel = new JLabel();
        sourceFunctionCombo = new JComboBox();
        sourceLabelField = new JTextField();
        separator = new JSeparator();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        resultsLabel.setText("Found n Items");

        selectionLabel.setText("n items selected");

        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Close this window");

        addButton.setText("Add");
        addButton.setToolTipText("Add selected edges to graph");

        tableScrollPane.setViewportView(resultsTable);

        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter"));

        expandLabel.setFont(expandLabel.getFont().deriveFont(expandLabel.getFont().getStyle() | Font.BOLD));
        expandLabel.setText("Expand:");

        expandButtonGroup.add(expandBothButton);
        expandBothButton.setText("Both");

        expandButtonGroup.add(expandUpstreamButton);
        expandUpstreamButton.setText("Upstream");

        expandButtonGroup.add(expandDownstreamButton);
        expandDownstreamButton.setText("Downstream");

        edgeLabel.setFont(edgeLabel.getFont().deriveFont(edgeLabel.getFont().getStyle() | Font.BOLD));
        edgeLabel.setText("Edge:");

        edgeRelationshipCombo.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        edgeRelationshipLabel.setLabelFor(edgeRelationshipCombo);
        edgeRelationshipLabel.setText("Relationship");

        sourceLabel.setFont(sourceLabel.getFont().deriveFont(sourceLabel.getFont().getStyle() | Font.BOLD));
        sourceLabel.setText("Source:");

        targetLabel.setFont(targetLabel.getFont().deriveFont(targetLabel.getFont().getStyle() | Font.BOLD));
        targetLabel.setText("Target:");

        targetFunctionLabel.setLabelFor(targetFunctionCombo);
        targetFunctionLabel.setText("Function");

        targetFunctionCombo.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        targetLabelLabel.setLabelFor(targetLabelField);
        targetLabelLabel.setText("Label");

        sourceFunctionLabel.setLabelFor(sourceFunctionCombo);
        sourceFunctionLabel.setText("Function");

        sourceLabelLabel.setLabelFor(sourceLabelField);
        sourceLabelLabel.setText("Label");

        sourceFunctionCombo.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        GroupLayout filterPanelLayout = new GroupLayout(filterPanel);
        filterPanel.setLayout(filterPanelLayout);
        filterPanelLayout.setHorizontalGroup(
            filterPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(filterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(filterPanelLayout.createSequentialGroup()
                        .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(expandLabel)
                            .addGroup(filterPanelLayout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(expandUpstreamButton)
                                    .addComponent(expandBothButton)
                                    .addComponent(expandDownstreamButton)))
                            .addGroup(filterPanelLayout.createSequentialGroup()
                                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                                    .addGroup(filterPanelLayout.createSequentialGroup()
                                        .addGap(39, 39, 39)
                                        .addComponent(sourceLabelLabel))
                                    .addGroup(filterPanelLayout.createSequentialGroup()
                                        .addGap(20, 20, 20)
                                        .addComponent(sourceFunctionLabel)))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(sourceFunctionCombo, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(sourceLabelField, GroupLayout.PREFERRED_SIZE, 194, GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(ComponentPlacement.RELATED, 58, Short.MAX_VALUE)
                        .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(filterPanelLayout.createSequentialGroup()
                                .addGroup(filterPanelLayout.createParallelGroup(Alignment.TRAILING)
                                    .addComponent(targetLabelLabel)
                                    .addComponent(targetFunctionLabel)
                                    .addComponent(edgeRelationshipLabel))
                                .addGap(18, 18, 18)
                                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING, false)
                                    .addComponent(targetFunctionCombo, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(targetLabelField, GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                                    .addComponent(edgeRelationshipCombo, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(filterPanelLayout.createSequentialGroup()
                                .addGap(29, 29, 29)
                                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(targetLabel)
                                    .addComponent(edgeLabel)))))
                    .addGroup(filterPanelLayout.createSequentialGroup()
                        .addComponent(sourceLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        filterPanelLayout.setVerticalGroup(
            filterPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(filterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(expandLabel)
                    .addComponent(edgeLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(expandBothButton)
                    .addComponent(edgeRelationshipLabel)
                    .addComponent(edgeRelationshipCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(expandUpstreamButton)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(expandDownstreamButton)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(sourceLabel)
                    .addComponent(targetLabel))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(targetFunctionLabel)
                        .addComponent(targetFunctionCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(sourceFunctionLabel)
                        .addComponent(sourceFunctionCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(filterPanelLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(filterPanelLayout.createSequentialGroup()
                        .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(targetLabelField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(targetLabelLabel))
                        .addContainerGap())
                    .addGroup(Alignment.TRAILING, filterPanelLayout.createSequentialGroup()
                        .addGroup(filterPanelLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(sourceLabelField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(sourceLabelLabel))
                        .addGap(35, 35, 35))))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(filterPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tableScrollPane, GroupLayout.DEFAULT_SIZE, 691, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(selectionLabel)
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cancelButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(addButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(resultsLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(separator)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filterPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addComponent(separator, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(resultsLabel)
                .addGap(9, 9, 9)
                .addComponent(tableScrollPane, GroupLayout.PREFERRED_SIZE, 219, GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(selectionLabel))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(addButton)
                            .addComponent(cancelButton))))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

}
