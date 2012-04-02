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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.openbel.belframework.kam.EdgeOption;
import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.kam.KAMSession;
import org.openbel.belframework.kam.NetworkOption;
import org.openbel.belframework.kam.Utility;
import org.openbel.belframework.kam.task.AbstractSearchKamTask;
import org.openbel.belframework.kam.task.KAMTasks;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;

/**
 * {@link SearchKAMDialog} represents the UI for the Add KAM Nodes dialog.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class SearchKAMDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -8900235008972637257L;
    private static final String DIALOG_TITLE = "Add KAM Nodes";
    
    private TableRowSorter<ResultsTableModel> rowSorter;
    private KAMNetwork lastSearchedNetwork = null;
    
    // swing components
    private JTable resultsTable;
    private JComboBox networkCmb;
    private JComboBox functionCmb;
    private JButton cancelBtn;
    private JButton searchBtn;
    private JButton addBtn;
    private JTextField filterTxt;
    private JComboBox edgeCmb;
    private JLabel resultsCount;

    /**
     * Construct the {@link JDialog dialog} and initialize the UI.
     *
     * @see #initUI()
     */
    public SearchKAMDialog() {
        super(Cytoscape.getDesktop(), DIALOG_TITLE, false);
        
        initUI();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Implementation Note:
     * Clean up resources used by {@link SearchKAMDialog}.
     * </p>
     */
    @Override
    public void dispose() {
        super.dispose();

        final ResultsTableModel model = (ResultsTableModel) resultsTable
                .getModel();
        model.clear();
    }

    private void initUI() {
        // Initialize dialog with Search KAM and button panel.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createSearchKAMPanel(), BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

        // init state
        if (functionCmb.getModel().getSize() > 0) {
            functionCmb.setSelectedIndex(0);
            searchBtn.setEnabled(true);
        } else {
            searchBtn.setEnabled(false);
        }
        addBtn.setEnabled(false);

        // Dialog settings
        final Dimension dialogDim = new Dimension(400, 600);
        setMinimumSize(dialogDim);
        setSize(dialogDim);
        setPreferredSize(dialogDim);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
    }

    private JPanel createSearchKAMPanel() {
        GridBagConstraints gridBagConstraints;

        JPanel searchPanel = new JPanel();
        JLabel networkLbl = new JLabel();
        JLabel functionLbl = new JLabel();
        JPanel resultsPanel = new JPanel();
        JScrollPane resultsPane = new JScrollPane();
        functionCmb = new JComboBox();
        networkCmb = new JComboBox();
        resultsTable = new JTable();
        resultsCount = new JLabel();

        searchPanel.setLayout(new java.awt.GridBagLayout());

        networkLbl.setText("Network:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
        searchPanel.add(networkLbl, gridBagConstraints);

        final Set<CyNetwork> networkSet = Utility.getKamNetworks();
        final Iterator<CyNetwork> networkIt = networkSet.iterator();
        final List<NetworkOption> networks = new ArrayList<NetworkOption>(networkSet.size());
        NetworkOption selectedNetwork = null;

        while (networkIt.hasNext()) {
            final CyNetwork cyn = networkIt.next();

            final NetworkOption networkOpt = new NetworkOption(cyn);
            networks.add(networkOpt);

            // trap this network option if this is the active cyn
            if (Cytoscape.getCurrentNetwork() == cyn) {
                selectedNetwork = networkOpt;
            }
        }

        networkCmb.addActionListener(this);
        networkCmb.setModel(new DefaultComboBoxModel(networks
                .toArray(new NetworkOption[networks.size()])));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 10.0;
        searchPanel.add(networkCmb, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        searchPanel.add(new JSeparator(), gridBagConstraints);

        functionLbl.setText("Function Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
        searchPanel.add(functionLbl, gridBagConstraints);

        functionCmb.addActionListener(this);
        functionCmb.setModel(new DefaultComboBoxModel(Utility.getFunctions()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 10.0;
        searchPanel.add(functionCmb, gridBagConstraints);

        resultsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("KAM Node Results"));
        resultsPanel.setLayout(new java.awt.BorderLayout());

        final ResultsTableModel resultsModel = new ResultsTableModel();
        resultsTable.setModel(resultsModel);
        resultsTable.getSelectionModel().addListSelectionListener(new ResultsSelectionListener());

        rowSorter = new TableRowSorter<ResultsTableModel>(resultsModel);
        resultsTable.setRowSorter(rowSorter);
        resultsPane.setViewportView(resultsTable);
        resultsPanel.add(resultsPane, java.awt.BorderLayout.CENTER);
        
        resultsCount.setText(""); // empty until search
        JPanel countPanel = new JPanel(new BorderLayout(5, 0));
        countPanel.add(resultsCount, BorderLayout.WEST);
        resultsPanel.add(countPanel, BorderLayout.SOUTH);
        
        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);

        filterTxt = new JTextField();
        filterTxt.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                rowSorter.setRowFilter(new LabelFilter());
            }

            @Override
            public void keyPressed(KeyEvent e) {
                rowSorter.setRowFilter(new LabelFilter());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                rowSorter.setRowFilter(new LabelFilter());
            }
        });
        filterTxt.setEnabled(false);
        filterPanel.add(filterTxt, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 0));
        bottomPanel.add(countPanel, BorderLayout.WEST);
        bottomPanel.add(filterPanel, BorderLayout.SOUTH);
        
        resultsPanel.add(bottomPanel, BorderLayout.SOUTH);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 25.0;
        searchPanel.add(resultsPanel, gridBagConstraints);

        final JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setLayout(new GridBagLayout());

        final JLabel edgeLabel = new JLabel("Expand Edge Options:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
        optionsPanel.add(edgeLabel, gridBagConstraints);

        edgeCmb = new JComboBox();
        edgeCmb.setModel(new DefaultComboBoxModel(EdgeOption.values()));
        edgeCmb.getModel().setSelectedItem(EdgeOption.INTERCONNECT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
        optionsPanel.add(edgeCmb, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 1.0;
        searchPanel.add(optionsPanel, gridBagConstraints);

        // lazy selection of current network,
        // needs to happen after ResultsTableModel is set as the table model
        if (selectedNetwork != null) {
            networkCmb.setSelectedItem(selectedNetwork);
        }

        return searchPanel;
    }

    private JPanel createButtonPanel() {
        // Button panel
        JPanel bp = new JPanel();
        bp.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        cancelBtn = new JButton();
        cancelBtn.addActionListener(this);
        cancelBtn.setText("Cancel");
        cancelBtn.setPreferredSize(new java.awt.Dimension(85, 25));
        bp.add(cancelBtn);

        searchBtn = new JButton();
        searchBtn.addActionListener(this);
        searchBtn.setText("Search");
        searchBtn.setPreferredSize(new java.awt.Dimension(85, 25));
        bp.add(searchBtn);

        addBtn = new JButton();
        addBtn.addActionListener(this);
        addBtn.setText("Add");
        addBtn.setPreferredSize(new java.awt.Dimension(85, 25));
        bp.add(addBtn);

        return bp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e != null) {
            if (e.getSource() == functionCmb) {
                searchBtn.setEnabled(true);
            } else if (e.getSource() == networkCmb) {
                resultsTable.getSelectionModel().clearSelection();

                final ResultsTableModel model = (ResultsTableModel) resultsTable
                        .getModel();
                model.clear();

                model.fireTableDataChanged();

                final NetworkOption networkOption = (NetworkOption) networkCmb.getSelectedItem();
                Cytoscape.getDesktop().setFocus(
                        networkOption.getCyNetwork().getIdentifier());
            } else if (e.getSource() == cancelBtn) {
                this.dispose();
            } else if (e.getSource() == searchBtn) {
                FunctionType selfunc = (FunctionType) functionCmb.getSelectedItem();

                final NetworkOption networkOption = (NetworkOption) networkCmb
                        .getSelectedItem();

                final KAMNetwork kamNetwork = KAMSession.getInstance()
                        .getKAMNetwork(networkOption.getCyNetwork());
                this.lastSearchedNetwork = kamNetwork;
                
                final Task task = new AbstractSearchKamTask(kamNetwork, selfunc) {

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
                            resultsCount
                                    .setText(nodes.size() + " nodes found.");
                        }
                    }
                };
                Utility.executeTask(task);
            } else if (e.getSource() == addBtn) {
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
                EdgeOption eeo = (EdgeOption) edgeCmb.getSelectedItem();
                switch (eeo) {
                    case ALL_EDGES:
                        KAMTasks.addNodesAndExpand(lastSearchedNetwork, selectedNodes, EdgeDirectionType.BOTH);
                        break;
                    case DOWNSTREAM:
                        KAMTasks.addNodesAndExpand(lastSearchedNetwork, selectedNodes, EdgeDirectionType.FORWARD);
                        break;
                    case INTERCONNECT:
                        KAMTasks.addNodesAndInterconnect(lastSearchedNetwork, selectedNodes);
                        break;
                    case NONE:
                        KAMTasks.addNodes(lastSearchedNetwork, selectedNodes);
                        break;
                    case UPSTREAM:
                        KAMTasks.addNodesAndExpand(lastSearchedNetwork, selectedNodes, EdgeDirectionType.REVERSE);
                        break;
                }
            }
        }
    }

    /**
     * The {@link AbstractTableModel table model} for the
     * {@link KamNode kam node} search results.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class ResultsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -5744344001683506045L;
        private final String[] headers = new String[] { "Label" };
        private final List<KamNode> nodes;

        private ResultsTableModel() {
            this.nodes = new ArrayList<KamNode>();
        }

        private void setData(final Collection<KamNode> nodes) {
            this.nodes.clear();
            this.nodes.addAll(nodes);
            fireTableDataChanged();
        }

        private void clear() {
            nodes.clear();
        }
        
        public List<KamNode> getNodes() {
            return nodes;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return headers.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int ci) {
            return headers[ci];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return nodes.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            final KamNode node = nodes.get(ri);

            switch (ci) {
                case 0:
                    return node.getLabel();
            }

            return null;
        }
    }

    /**
     * The {@link ListSelectionListener listener} fired when a table row is
     * selected in order to flip the state of the "Add" button.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class ResultsSelectionListener implements ListSelectionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel selModel = (ListSelectionModel) e.getSource();
            addBtn.setEnabled(!selModel.isSelectionEmpty());
        }
    }

    /**
     * The {@link RowFilter row filter} that filters {@link KamNode kam nodes}
     * in the {@link ResultsTableModel search results table model} by an
     * arbitrary {@link String string}.  The {@link String search string} is
     * applied to all table rows using a {@link String#contains(CharSequence)}
     * function.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class LabelFilter extends RowFilter<ResultsTableModel, Integer> {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends ResultsTableModel, ? extends Integer> entry) {

            final ResultsTableModel rtm = entry.getModel();
            KamNode node = rtm.getNodes().get(entry.getIdentifier());
            final String lowerLabel = node.getLabel().toLowerCase();

            return lowerLabel.contains(filterTxt.getText().toLowerCase());
        }
    }
}
