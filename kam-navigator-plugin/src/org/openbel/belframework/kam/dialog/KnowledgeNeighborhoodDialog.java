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
package org.openbel.belframework.kam.dialog;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import javax.swing.RowFilter;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.kam.KAMSession;
import org.openbel.belframework.kam.task.KAMTasks;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.RelationshipType;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;

/**
 * {@link KnowledgeNeighborhoodDialog} represents the UI for the Knowledge
 * Neighborhood dialog.
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
public class KnowledgeNeighborhoodDialog extends JDialog implements
        ActionListener, PropertyChangeListener, SelectEventListener {
    private static final long serialVersionUID = -736918933072782546L;
    private static final String DIALOG_TITLE = "Knowledge Neighborhood";
    private static final String ALL_SELECTION = "All";
    
    private final KAMService kamService;
    // used to keep track of currently selected nodes in kam form
    private final Set<String> selectedKamNodeIds = new HashSet<String>();
    // networks that this instance is registered as a listener on
    private final Set<CyNetwork> subjectNetworks = new HashSet<CyNetwork>();
    // network that nodes were last selected on
    private CyNetwork currentNetwork;
    
    // Executor for loading the knowledge neighborhood
    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean loading = false;
    private volatile boolean haltLoading = false;

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

        
        // register property change listener for this instace
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_CREATED, this);
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_DESTROYED, this);
        
        // register listener
        Cytoscape.getCurrentNetwork().addSelectEventListener(this);
        subjectNetworks.add(Cytoscape.getCurrentNetwork());

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

        // deregister this listener for all associated objects
        Cytoscape.getPropertyChangeSupport().removePropertyChangeListener(this);
        for (CyNetwork network : subjectNetworks) {
            if (network != null) {
                network.removeSelectEventListener(this);
            }
        }
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
            // cancel button
            this.dispose();
        } else if (e.getSource().equals(addButton)) {
            // add button
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

            KAMNetwork kamNetwork = KAMSession.getInstance().getKAMNetwork(
                    currentNetwork);

            KAMTasks.addEdges(kamNetwork, selectedEdges);
        } else if (e.getSource().equals(expandBothButton)
                || e.getSource().equals(expandUpstreamButton)
                || e.getSource().equals(expandDownstreamButton)
                || e.getSource().equals(sourceFunctionCombo)
                || e.getSource().equals(targetFunctionCombo)
                || e.getSource().equals(edgeRelationshipCombo)) {
            sort();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e == null) {
            return;
        }
        
        if (CytoscapeDesktop.NETWORK_VIEW_CREATED.equals(e
                .getPropertyName())) {
            CyNetworkView view = (CyNetworkView) e.getNewValue();
            view.getNetwork().addSelectEventListener(this);
            subjectNetworks.add(view.getNetwork());
        } else if (CytoscapeDesktop.NETWORK_VIEW_DESTROYED.equals(e
                .getPropertyName())) {
            CyNetworkView view = (CyNetworkView) e.getNewValue();
            view.getNetwork().removeSelectEventListener(this);
            subjectNetworks.remove(view.getNetwork());
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
    
    private void initUI() {
        initComponents();

        // additional stuff (kept separate for future UI work)
        // adjust position to default, keeps dialog from appearing offscreen
        setLocationRelativeTo(null); 
        
        resultsLabel.setText("");
        selectionLabel.setText("");
        cancelButton.addActionListener(this);
        addButton.addActionListener(this);
        addButton.setEnabled(false);
        resultsTable.setModel(new EdgeTableModel());
        resultsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {

                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        ListSelectionModel selModel = (ListSelectionModel) e
                                .getSource();
                        addButton.setEnabled(!selModel.isSelectionEmpty());
                        int selectionNumber = resultsTable.getSelectedRows().length;
                        selectionLabel.setText(selectionNumber
                                + " edges selected");
                    }
                });

        // register the filters with the sorter
        Collection<RowFilter<EdgeTableModel, Integer>> filters = new HashSet<RowFilter<EdgeTableModel, Integer>>();
        filters.add(new SourceFunctionFilter());
        filters.add(new TargetFunctionFilter());
        filters.add(new RelationshipFilter());
        filters.add(new SourceLabelFilter());
        filters.add(new TargetLabelFilter());
        filters.add(new DirectionFilter());
        RowFilter<EdgeTableModel, Integer> andFilter = RowFilter
                .andFilter(filters);
        // sorter has alphabetical column sort on by default
        TableRowSorter<EdgeTableModel> rowSorter = new TableRowSorter<EdgeTableModel>(
                (EdgeTableModel) resultsTable.getModel());
        rowSorter.setRowFilter(andFilter);
        resultsTable.setRowSorter(rowSorter);

        // filter options
        expandBothButton.addActionListener(this);
        expandUpstreamButton.addActionListener(this);
        expandDownstreamButton.addActionListener(this);

        sourceFunctionCombo.setModel(new SourceFunctionComboBoxModel());
        targetFunctionCombo.setModel(new TargetFunctionComboBoxModel());
        edgeRelationshipCombo.setModel(new RelationshipComboBoxModel());

        sourceFunctionCombo.addActionListener(this);
        targetFunctionCombo.addActionListener(this);
        edgeRelationshipCombo.addActionListener(this);

        // key listener for target / source labels
        KeyListener keyListener = new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                sort();
            }

            // TODO do we need sort on key released or pressed?
            @Override
            public void keyReleased(KeyEvent e) {
                sort();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                sort();
            }
        };
        sourceLabelField.addKeyListener(keyListener);
        targetLabelField.addKeyListener(keyListener);
        
        // set filters to default state
        sourceFunctionCombo.setSelectedItem(ALL_SELECTION);
        targetFunctionCombo.setSelectedItem(ALL_SELECTION);
        edgeRelationshipCombo.setSelectedItem(ALL_SELECTION);
        expandBothButton.setSelected(true);
        sourceLabelField.setText(null);
        targetLabelField.setText(null);
    }

    /**
     * Refreshes the table sort and filters
     */
    @SuppressWarnings("unchecked")
    private synchronized void sort() {
        ((TableRowSorter<EdgeTableModel>) resultsTable.getRowSorter()).sort();
        // number of found reflects the number of rows post filter
        resultsLabel.setText("Found " + resultsTable.getRowCount() + " edges");
    }

    /**
     * Load (or reload) the edges around the selected nodes, update UI to match
     */
    private void loadNeighborhood() {
        if (loading) {
            // halt previous load
            haltLoading = true;
            while (loading) {
                // wait until previous task finishes
            }
            // reset halt for new load
            haltLoading = false;
        }

        // clear previously selected
        selectedKamNodeIds.clear();
        final EdgeTableModel model = (EdgeTableModel) this.resultsTable
                .getModel();
        model.clear();
        
        // register current network (will be used for add edges command)
        currentNetwork = Cytoscape.getCurrentNetwork();
        @SuppressWarnings("unchecked")
        final Set<CyNode> selected = currentNetwork.getSelectedNodes();
        
        if (selected.isEmpty()) {
            // if empty no point in resolve edges
            resultsLabel.setText("Found 0 edges");

            // clear filters combo boxes
            ((DefaultComboBoxModel) sourceFunctionCombo.getModel())
                    .removeAllElements();
            ((DefaultComboBoxModel) targetFunctionCombo.getModel())
                    .removeAllElements();
            ((DefaultComboBoxModel) edgeRelationshipCombo.getModel())
                    .removeAllElements();

            return;
        }

        final KAMNetwork kamNetwork = KAMSession.getInstance().getKAMNetwork(
                currentNetwork);

        final Collection<KamNode> kamNodes = new HashSet<KamNode>();
        for (final CyNode cynode : selected) {
            KamNode kamNode = kamNetwork.getKAMNode(cynode);
            kamNodes.add(kamNode);
            // update selected kamNode ids
            selectedKamNodeIds.add(kamNode.getId());
        }
        
        // put this a thread so it doesn't lock the UI
        loadExecutor.execute(new Runnable() {
            
            @Override
            public void run() {
                // start loading
                loading = true;

                try {
                    List<KamEdge> edges = new ArrayList<KamEdge>();
                    for (KamNode kamNode : kamNodes) {
                        if (haltLoading) {
                            break;
                        }

                        edges.addAll(kamService.getAdjacentKamEdges(
                                kamNetwork.getDialectHandle(), kamNode,
                                EdgeDirectionType.BOTH, null));
                    }

                    if (!haltLoading) {
                        model.addEdges(edges);
                        // update filters combo boxes
                        ((SourceFunctionComboBoxModel) sourceFunctionCombo
                                .getModel()).updateEdges(edges);
                        ((TargetFunctionComboBoxModel) targetFunctionCombo
                                .getModel()).updateEdges(edges);
                        ((RelationshipComboBoxModel) edgeRelationshipCombo
                                .getModel()).updateEdges(edges);
                        // resort filters after update
                        sort();
                    }
                } finally {
                    // finished loading
                    loading = false;
                }
            }
        });
        
    }

    /**
     * Simple extension of {@link DefaultTableModel} to keep track of added
     * edges
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class EdgeTableModel extends DefaultTableModel {
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

        public synchronized void addEdges(Collection<KamEdge> edges) {
            for (KamEdge edge : edges) {
                if (edge != null) {
                    addEdge(edge);
                }
            }
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

        private void addEdge(KamEdge edge) {
            super.addRow(new String[] { edge.getSource().getLabel(),
                    edge.getRelationship().toString(),
                    edge.getTarget().getLabel() });
            edges.add(edge);
        }
    }

    /**
     * Model for the source function combo box filter
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class SourceFunctionComboBoxModel extends
            UpdatableComboBoxModel {
        private static final long serialVersionUID = 847486496638770057L;

        @Override
        protected String getName(final KamEdge e) {
            if (e.getSource() == null || e.getSource().getFunction() == null) {
                return null;
            }
            return e.getSource().getFunction().name();
        }

    }

    /**
     * Model for the target function combo box filter
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class TargetFunctionComboBoxModel extends
            UpdatableComboBoxModel {
        private static final long serialVersionUID = -6749141138659929487L;

        @Override
        protected String getName(final KamEdge e) {
            if (e.getTarget() == null || e.getTarget().getFunction() == null) {
                return null;
            }
            return e.getTarget().getFunction().name();
        }

    }

    /**
     * Model for the edge relationship combo box filter
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class RelationshipComboBoxModel extends
            UpdatableComboBoxModel {
        private static final long serialVersionUID = 4774181753730742386L;

        @Override
        protected String getName(final KamEdge e) {
            if (e.getRelationship() == null) {
                return null;
            }
            return e.getRelationship().name();
        }
    }

    /**
     * Extension of {@link DefaultComboBoxModel} that adds the ablity to update
     * its contents based on a collection of {@link KamEdge}s
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private abstract static class UpdatableComboBoxModel extends
            DefaultComboBoxModel {
        private static final long serialVersionUID = -8049723723613055311L;

        public synchronized void updateEdges(final Collection<KamEdge> edges) {
            String previousSelection = (String) getSelectedItem();
            removeAllElements();

            // filter duplicates out by using a set
            Set<String> names = new HashSet<String>();
            for (KamEdge e : edges) {
                String name = getName(e);
                if (name != null) {
                    names.add(name);
                }
            }

            List<String> sortedNames = new ArrayList<String>(names);
            Collections.sort(sortedNames);
            // add all selection option at beginning
            sortedNames.add(0, ALL_SELECTION);

            for (String name : sortedNames) {
                addElement(name);

                // restore previous selections
                if (name.equals(previousSelection)) {
                    setSelectedItem(name);
                }
            }

            if (previousSelection == null) {
                // work around for addElement making a selection
                setSelectedItem(ALL_SELECTION);
            }

            fireContentsChanged(this, 0, names.size());
        }

        protected abstract String getName(KamEdge e);
    }

    private class SourceFunctionFilter extends
            RowFilter<EdgeTableModel, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends EdgeTableModel, ? extends Integer> entry) {
            String selected = (String) sourceFunctionCombo.getSelectedItem();
            if (selected == null || ALL_SELECTION.equals(selected)) {
                return true;
            }

            KamEdge edge = entry.getModel().getEdges()
                    .get(entry.getIdentifier());
            FunctionType function = FunctionType.valueOf(selected);

            return function.equals(edge.getSource().getFunction());
        }
    }

    private class TargetFunctionFilter extends
            RowFilter<EdgeTableModel, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends EdgeTableModel, ? extends Integer> entry) {
            String selected = (String) targetFunctionCombo.getSelectedItem();
            if (selected == null || ALL_SELECTION.equals(selected)) {
                return true;
            }

            KamEdge edge = entry.getModel().getEdges()
                    .get(entry.getIdentifier());
            FunctionType function = FunctionType.valueOf(selected);

            return function.equals(edge.getTarget().getFunction());
        }
    }

    private class RelationshipFilter extends RowFilter<EdgeTableModel, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends EdgeTableModel, ? extends Integer> entry) {
            String selected = (String) edgeRelationshipCombo.getSelectedItem();
            if (selected == null || ALL_SELECTION.equals(selected)) {
                return true;
            }

            KamEdge edge = entry.getModel().getEdges()
                    .get(entry.getIdentifier());
            RelationshipType relationship = RelationshipType.valueOf(selected);

            return relationship.equals(edge.getRelationship());
        }
    }
    
    private class DirectionFilter extends RowFilter<EdgeTableModel, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends EdgeTableModel, ? extends Integer> entry) {
            EdgeDirectionType direction = EdgeDirectionType.BOTH;
            if (expandUpstreamButton.isSelected()) {
                direction = EdgeDirectionType.REVERSE;
            } else if (expandDownstreamButton.isSelected()) {
                direction = EdgeDirectionType.FORWARD;
            }
            if (EdgeDirectionType.BOTH.equals(direction)) {
                return true;
            }

            KamEdge edge = entry.getModel().getEdges()
                    .get(entry.getIdentifier());
            KamNode node = null;
            if (EdgeDirectionType.FORWARD.equals(direction)) {
                node = (KamNode) edge.getSource();
            } else {
                node = (KamNode) edge.getTarget();
            }

            return selectedKamNodeIds.contains(node.getId());
        }
    }

    private class SourceLabelFilter extends RowFilter<EdgeTableModel, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends EdgeTableModel, ? extends Integer> entry) {
            String filterText = sourceLabelField.getText();
            if (filterText == null || filterText.isEmpty()) {
                return true;
            }

            KamEdge edge = entry.getModel().getEdges()
                    .get(entry.getIdentifier());

            return edge.getSource().getLabel().toLowerCase()
                    .contains(filterText.toLowerCase());
        }
    }

    private class TargetLabelFilter extends RowFilter<EdgeTableModel, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends EdgeTableModel, ? extends Integer> entry) {
            String filterText = targetLabelField.getText();
            if (filterText == null || filterText.isEmpty()) {
                return true;
            }

            KamEdge edge = entry.getModel().getEdges()
                    .get(entry.getIdentifier());

            return edge.getTarget().getLabel().toLowerCase()
                    .contains(filterText.toLowerCase());
        }
    }

    // taken from netbeans
    // this method was taken from auto generated code, apologies if it sucks
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

        resultsLabel.setText("Found n edges");

        selectionLabel.setText("n edges selected");

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
