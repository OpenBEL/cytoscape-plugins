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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import org.openbel.belframework.kam.task.KAMTasks;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.BelTerm;
import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

/**
 * {@link SearchKAMDialog} represents the UI for the Add KAM Nodes dialog.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class SearchKAMDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -8900235008972637257L;
    private static final String DIALOG_TITLE = "Add KAM Nodes";
    private final KAMService kamService;
    private JTable resultsTable;
    private JComboBox networkCmb;
    private JComboBox functionCmb;
    private JButton cancelBtn;
    private JButton searchBtn;
    private JButton addBtn;
    private TableRowSorter<ResultsTableModel> rowSorter;
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
        this.kamService = KAMServiceFactory.getInstance().getKAMService();

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

        final Set<CyNetwork> networkSet = Cytoscape.getNetworkSet();
        final Iterator<CyNetwork> networkIt = networkSet.iterator();
        final List<NetworkOption> networks = new ArrayList<NetworkOption>(networkSet.size());
        NetworkOption selectedNetwork = null;
        final KAMSession session = KAMSession.getInstance();

        while (networkIt.hasNext()) {
            final CyNetwork cyn = networkIt.next();

            // only add cytoscape network if it's KAM-backed
            if (session.getKAMNetwork(cyn) != null) {
                final NetworkOption networkOpt = new NetworkOption(cyn);
                networks.add(networkOpt);

                // trap this network option if this is the active cyn
                if (Cytoscape.getCurrentNetwork() == cyn) {
                    selectedNetwork = networkOpt;
                }
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
                Cytoscape.getDesktop().setFocus(networkOption.cyn.getIdentifier());
            } else if (e.getSource() == cancelBtn) {
                this.dispose();
            } else if (e.getSource() == searchBtn) {
                FunctionType selfunc = (FunctionType) functionCmb.getSelectedItem();

                final NetworkOption networkOption = (NetworkOption) networkCmb
                        .getSelectedItem();

                final SearchKAMNodesTask task = new SearchKAMNodesTask(networkOption.cyn, selfunc);
                Utility.executeTask(task);
            } else if (e.getSource() == addBtn) {
                ResultsTableModel rtm = (ResultsTableModel) resultsTable.getModel();
                final List<KamNode> nodes = rtm.nodes;

                // hold the selected KAM nodes we're interested in
                final List<KamNode> selectedNodes = new ArrayList<KamNode>();

                // track selected KAM node ids to efficiently filter edges
                final Set<String> selectedNodeIds = new HashSet<String>();

                // determine selected rows from the filtered view
                int[] viewIndices = resultsTable.getSelectedRows();
                for (int viewIndex : viewIndices) {
                    int modelIndex = resultsTable.convertRowIndexToModel(viewIndex);

                    KamNode selectedNode = nodes.get(modelIndex);
                    selectedNodes.add(selectedNode);
                    selectedNodeIds.add(selectedNode.getId());
                }

                final NetworkOption networkOption = (NetworkOption) networkCmb
                        .getSelectedItem();
                final KAMNetwork kamNetwork = KAMSession.getInstance()
                        .getKAMNetwork(networkOption.cyn);

                // run add task, hook in edges based on selected option
                EdgeOption eeo = (EdgeOption) edgeCmb.getSelectedItem();
                switch (eeo) {
                    case ALL_EDGES:
                        KAMTasks.expandNodes(kamNetwork, selectedNodes, EdgeDirectionType.BOTH);
                        break;
                    case DOWNSTREAM:
                        KAMTasks.expandNodes(kamNetwork, selectedNodes, EdgeDirectionType.FORWARD);
                        break;
                    case INTERCONNECT:
                        KAMTasks.interconnect(kamNetwork, selectedNodes);
                        break;
                    case NONE:
                        KAMTasks.addNodes(kamNetwork, selectedNodes);
                        break;
                    case UPSTREAM:
                        KAMTasks.expandNodes(kamNetwork, selectedNodes, EdgeDirectionType.REVERSE);
                        break;
                }
            }
        }
    }

    /**
     * The {@link Task cytoscape task} to handle searching for
     * {@link KamNode kam nodes} using the Web API.
     *
     * @see KAMServices#findKamNodesByFunction(
     * com.selventa.belframework.ws.client.KamHandle, FunctionType)
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class SearchKAMNodesTask implements Task {
        private final CyNetwork cyn;
        private final FunctionType function;
        private TaskMonitor m;
        // marked as volatile in case halt is called by multiple threads
        private volatile boolean halt = false;
        // keeps track of if threaded task is finished
        private volatile boolean finished = false;

        private SearchKAMNodesTask(final CyNetwork cyn, final FunctionType function) {
            this.cyn = cyn;
            this.function = function;
        }

        @Override
        public String getTitle() {
            return "Searching KAM Nodes";
        }

        @Override
        public void setTaskMonitor(TaskMonitor m)
                throws IllegalThreadStateException {
            this.m = m;
        }

        @Override
        public void halt() {
            halt = true;
        }

        @Override
        public void run() {
            m.setStatus("Searching for " + function + " functions.");

            m.setPercentCompleted(0);
            searchKAMNodes();
            m.setPercentCompleted(100);
        }

        private void searchKAMNodes() {
            ExecutorService e = Executors.newSingleThreadExecutor();
            e.execute(new Runnable() {
                
                @Override
                public void run() {
                    final FunctionType selfunc = (FunctionType) functionCmb.getSelectedItem();
        
                    // get KAM Network for selected CyNetwork
                    final KAMNetwork kamNetwork = KAMSession.getInstance().getKAMNetwork(cyn);
        
                    // find kam nodes by function
                    final List<KamNode> nodes = kamService.findKamNodesByFunction(kamNetwork.getKAMHandle(), selfunc);
                    
                    // build out term map
                    // XXX most of the time searching for nodes is spent here,
                    // might be worth reviewing
                    final Map<KamNode, BelTerm> termMap = new HashMap<KamNode, BelTerm>();
                    for (final KamNode node : nodes) {
                        if (halt) {
                            break;
                        }
                        
                        final List<BelTerm> terms = kamService.getSupportingTerms(node);
                        final BelTerm firstTerm = terms.get(0);
                        termMap.put(node, firstTerm);
                    }
                    
                    ResultsTableModel rtm = (ResultsTableModel) resultsTable.getModel();
                    if (halt) {
                        // don't update ui if search is halted
                        return;
                    }
                    
                    rtm.setData(nodes, termMap);
                    int nodeCount = nodes.size();
                    filterTxt.setEnabled(nodeCount > 0);
                    if (nodeCount == 1) {
                        resultsCount.setText(nodes.size() + " node found.");
                    } else {
                        resultsCount.setText(nodes.size() + " nodes found.");
                    }
                    finished = true;
                }
            });
            
            while (!finished && !e.isShutdown()) {
                try {
                    if (halt) {
                        // this should not block
                        // but be aware that if the thread in the executor is
                        // blocked it will continue to live on
                        e.shutdownNow();
                    }
                    // sleep thread to enable interrupt
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    halt = true;
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
        private final Map<KamNode, BelTerm> termMap;

        private ResultsTableModel() {
            this.nodes = new ArrayList<KamNode>();
            this.termMap = new HashMap<KamNode, BelTerm>();
        }

        private void setData(final List<KamNode> nodes, 
                final Map<KamNode, BelTerm> termMap) {
            this.termMap.clear();
            this.termMap.putAll(termMap);

            this.nodes.clear();
            this.nodes.addAll(nodes);
            fireTableDataChanged();
        }

        private void clear() {
            nodes.clear();
            termMap.clear();
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
                    return termMap.get(node).getLabel();
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
            KamNode node = rtm.nodes.get(entry.getIdentifier());
            final BelTerm term = rtm.termMap.get(node);
            final String lowerLabel = term.getLabel().toLowerCase();

            return lowerLabel.contains(filterTxt.getText().toLowerCase());
        }
    }

    /**
     * {@link NetworkOption} represents the combo-box option for currently
     * loaded {@link Kam kam}-backed {@link CyNetwork cytoscape networks}.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private final class NetworkOption {
        private final CyNetwork cyn;

        private NetworkOption(final CyNetwork cyn) {
            this.cyn = cyn;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return cyn.getTitle();
        }
    }

    /**
     * {@link EdgeOption} enum for the different types of expansion rules.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private enum EdgeOption {
        /**
         * Expand downstream and upstream of each node being added.  The nodes
         * and all adjacent edges will be added to the network.
         */
        ALL_EDGES ("All Edges"),
        /**
         * Expand downstream of each node being added.  The nodes and all
         * downstream edges will be added to the network.
         */
        DOWNSTREAM ("Downstream Edges"),
        /**
         * Expand upstream of each node being added.  The nodes and all
         * upstream edges will be added to the network.
         */
        UPSTREAM ("Upstream Edges"),
        /**
         * Expand the selected nodes with only edges that interconnect between
         * them.  The selected nodes and any interconnected edges are added to
         * the network.
         */
        INTERCONNECT ("Interconnect Nodes"),
        /**
         * Do not expand the selected nodes to include edges.  Only the
         * selected nodes are added to the network.
         */
        NONE ("None");

        private final String label;

        private EdgeOption(final String label) {
            this.label = label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return label;
        }
    }
}
