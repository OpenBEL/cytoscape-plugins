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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.openbel.belframework.kam.EdgeOption;
import org.openbel.belframework.kam.KAMNetwork;
import org.openbel.belframework.kam.KAMSession;
import org.openbel.belframework.kam.NetworkOption;
import org.openbel.belframework.kam.Utility;
import org.openbel.belframework.kam.task.AbstractSearchKamTask;
import org.openbel.belframework.kam.task.KAMTasks;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.Namespace;
import com.selventa.belframework.ws.client.NamespaceDescriptor;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.logger.CyLogger;
import cytoscape.task.Task;
import cytoscape.util.CyFileFilter;
import cytoscape.util.FileUtil;

/**
 * {@link SearchKAMListDialog} represents the UI for the Add KAM Nodes from list
 * dialog.
 * 
 * @author James McMahon &lt;jmcmahon@selventa.com&gt;
 */
// FIXME disable search button until KAMNetwork is selected
public final class SearchKAMListDialog extends JDialog {
    private static final long serialVersionUID = -2555610304142946995L;

    private static final CyLogger log = CyLogger.getLogger(SearchKAMListDialog.class);
    public static final String TITLE = "Add KAM Nodes From List";
    private static final String ALL_SELECTION = "ALL";

    private final KAMService kamService;
    private final List<String> identifiers = new ArrayList<String>();
    private final CyFileFilter csvAndTxtFilter;
    
    private KAMNetwork lastSearchedNetwork = null;

    // swing fields
    private JButton addButton;
    private JButton browseButton;
    private JLabel browseResultsLabel;
    private JButton cancelButton;
    private JComboBox edgeComboBox;
    private JLabel edgeLabel;
    private JTextField fileTextField;
    private JComboBox functionComboBox;
    private JLabel functionLabel;
    private JComboBox namespaceComboBox;
    private JLabel namespaceLabel;
    private JComboBox networkComboBox;
    private JLabel networkLabel;
    private JLabel resultsFoundLabel;
    private JPanel resultsPanel;
    private JTable resultsTable;
    private JButton searchButton;
    private JScrollPane tableScrollPane;

    public SearchKAMListDialog() {
        super(Cytoscape.getDesktop(), TITLE, false);

        this.kamService = KAMServiceFactory.getInstance().getKAMService();
        initUI();
        
        // FIXME CSV and TXT files are still selectable
        csvAndTxtFilter = new CyFileFilter(new String[] { "csv", "txt" },
                "CSV and TXT files");
    }

    private void initUI() {
        initComponents();

        // additional stuff (kept separate for future UI work)
        // adjust position to default, keeps dialog from appearing offscreen
        setLocationRelativeTo(null);
        setResizable(false);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchButtonActionPerformed(e);
            }
        });
        
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addButtonActionPerformed(e);
            }
        });

        // resultsPanel.setVisible(false);

        // network options
        final Set<CyNetwork> networkSet = KAMSession.getInstance()
                .getKamBackedNetworks();
        final List<NetworkOption> networks = new ArrayList<NetworkOption>(
                networkSet.size());
        for (Iterator<CyNetwork> it = networkSet.iterator(); it.hasNext();) {
            CyNetwork cyn = it.next();

            NetworkOption networkOpt = new NetworkOption(cyn);
            networks.add(networkOpt);

            // trap this network option if this is the active cyn
            if (Cytoscape.getCurrentNetwork() == cyn) {
                networkComboBox.setSelectedItem(networkOpt);
            }
        }
        networkComboBox.setModel(new DefaultComboBoxModel(networks
                .toArray(new NetworkOption[networks.size()])));

        // namespace options
        namespaceComboBox.setModel(new DefaultComboBoxModel(
                new Vector<NamespaceOption>(getNamespaceOptions())));

        // file text field
        fileTextField.setText("");
        fileTextField.setEditable(false);

        // model for results table
        ResultsTableModel model = new ResultsTableModel();
        resultsTable.setModel(model);
        resultsTable.setRowSorter(new TableRowSorter<ResultsTableModel>(model));
        // disable selection as selection currently has no effect on whats added
        resultsTable.setCellSelectionEnabled(false);

        // edge model
        edgeComboBox.setModel(new DefaultComboBoxModel(EdgeOption.values()));
        edgeComboBox.getModel().setSelectedItem(EdgeOption.INTERCONNECT);

        // function model
        FunctionType[] functions = Utility.getFunctions();
        String[] funcStrings = new String[functions.length + 1];
        funcStrings[0] = ALL_SELECTION;
        for (int i = 0; i < functions.length; i++) {
            funcStrings[i + 1] = functions[i].name();
        }
        functionComboBox.setModel(new DefaultComboBoxModel(funcStrings));
        functionComboBox.getModel().setSelectedItem(ALL_SELECTION);

        // results label
        browseResultsLabel.setText("No file loaded");
        resultsFoundLabel.setText("No search performed");
        
        // disable buttons
        searchButton.setEnabled(false);
        addButton.setEnabled(false);
    }

    private void addButtonActionPerformed(ActionEvent e) {
        ResultsTableModel rtm = (ResultsTableModel) resultsTable.getModel();
        final List<KamNode> nodes = rtm.getNodes();

        // run add task, hook in edges based on selected option
        EdgeOption eeo = (EdgeOption) edgeComboBox.getSelectedItem();
        switch (eeo) {
        case ALL_EDGES:
            KAMTasks.addNodesAndExpand(lastSearchedNetwork, nodes,
                    EdgeDirectionType.BOTH);
            break;
        case DOWNSTREAM:
            KAMTasks.addNodesAndExpand(lastSearchedNetwork, nodes,
                    EdgeDirectionType.FORWARD);
            break;
        case INTERCONNECT:
            KAMTasks.addNodesAndInterconnect(lastSearchedNetwork, nodes);
            break;
        case NONE:
            KAMTasks.addNodes(lastSearchedNetwork, nodes);
            break;
        case UPSTREAM:
            KAMTasks.addNodesAndExpand(lastSearchedNetwork, nodes,
                    EdgeDirectionType.REVERSE);
            break;
        }
    }
    
    private void browseButtonActionPerformed(ActionEvent e) {
        
        File file = FileUtil.getFile("Select file with identifiers",
                FileUtil.LOAD, new CyFileFilter[] { csvAndTxtFilter });
        
        // clear previous identifiers whenever selecting a new file
        this.identifiers.clear();
        
        fileTextField.setText(file.getName());

        List<String> fileIdentifiers = null;
        try {
            fileIdentifiers = readIdentifiersFromFile(file);
        } catch (IOException ex) {
            log.warn("Error reading identifiers from file", ex);
        }

        if (!Utility.isEmpty(fileIdentifiers)) {
            this.identifiers.addAll(fileIdentifiers);
        }

        searchButton.setEnabled(!Utility.isEmpty(identifiers));
        
        int results = 0;
        if (identifiers != null) {
            results = identifiers.size();
        }
        browseResultsLabel.setText(results + " identifiers in file");
    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        this.dispose();
    }

    protected void searchButtonActionPerformed(ActionEvent e) {
        final ResultsTableModel model = (ResultsTableModel) resultsTable
                .getModel();
        model.clear();

        if (Utility.isEmpty(identifiers)) {
            return;
        }

        final Namespace namespace = ((NamespaceOption) namespaceComboBox
                .getSelectedItem()).getDescriptor().getNamespace();
        final NetworkOption networkOption = (NetworkOption) networkComboBox
                .getSelectedItem();
        final KAMNetwork kamNetwork = KAMSession.getInstance().getKAMNetwork(
                networkOption.getCyNetwork());
        this.lastSearchedNetwork = kamNetwork;

        FunctionType functionType = null;
        if (!functionComboBox.getSelectedItem().equals(ALL_SELECTION)) {
            functionType = FunctionType.valueOf((String) functionComboBox
                    .getSelectedItem());
        }
        
        final Task task = new AbstractSearchKamTask(kamNetwork, functionType,
                namespace, identifiers) {

            @Override
            protected void updateUI(Collection<KamNode> nodes) {
                model.setData(nodes);
                addButton.setEnabled(!Utility.isEmpty(nodes));
            
                resultsFoundLabel.setText(nodes.size() + " found");
                resultsFoundLabel.setVisible(true);
            }
        };
        Utility.executeTask(task);
    }

    private List<NamespaceOption> getNamespaceOptions() {
        List<NamespaceOption> options = new ArrayList<NamespaceOption>();
        for (NamespaceDescriptor desc : kamService.getAllNamespaces()) {
            options.add(new NamespaceOption(desc));
        }
        Collections.sort(options);
        return options;
    }

    // taken from netbeans
    // this method was taken from auto generated code, apologies if it sucks
    private void initComponents() {

        namespaceComboBox = new JComboBox();
        namespaceLabel = new JLabel();
        fileTextField = new JTextField();
        browseButton = new JButton();
        resultsPanel = new JPanel();
        addButton = new JButton();
        cancelButton = new JButton();
        tableScrollPane = new JScrollPane();
        resultsTable = new JTable();
        resultsFoundLabel = new JLabel();
        searchButton = new JButton();
        edgeComboBox = new JComboBox();
        edgeLabel = new JLabel();
        networkLabel = new JLabel();
        networkComboBox = new JComboBox();
        browseResultsLabel = new JLabel();
        functionComboBox = new JComboBox();
        functionLabel = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        namespaceLabel.setLabelFor(namespaceComboBox);
        namespaceLabel.setText("Namespace:");

        fileTextField.setText("jTextField1");

        browseButton.setText("Browse");
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        addButton.setText("Add");

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        tableScrollPane.setViewportView(resultsTable);

        resultsFoundLabel.setText("n found");

        searchButton.setText("Search");

        edgeLabel.setLabelFor(edgeComboBox);
        edgeLabel.setText("Expand Edges:");

        GroupLayout resultsPanelLayout = new GroupLayout(resultsPanel);
        resultsPanel.setLayout(resultsPanelLayout);
        resultsPanelLayout.setHorizontalGroup(
            resultsPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(resultsPanelLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(tableScrollPane, GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                    .addGroup(resultsPanelLayout.createSequentialGroup()
                        .addComponent(edgeLabel)
                        .addGap(18, 18, 18)
                        .addComponent(edgeComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(Alignment.TRAILING, resultsPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(cancelButton)
                        .addGap(18, 18, 18)
                        .addComponent(searchButton)
                        .addGap(18, 18, 18)
                        .addComponent(addButton))
                    .addGroup(resultsPanelLayout.createSequentialGroup()
                        .addComponent(resultsFoundLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        resultsPanelLayout.setVerticalGroup(
            resultsPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(tableScrollPane, GroupLayout.PREFERRED_SIZE, 316, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(resultsFoundLabel)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(resultsPanelLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(edgeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(edgeLabel))
                .addGap(18, 18, 18)
                .addGroup(resultsPanelLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(addButton)
                    .addComponent(cancelButton)
                    .addComponent(searchButton))
                .addContainerGap())
        );

        networkLabel.setLabelFor(namespaceComboBox);
        networkLabel.setText("Network:");

        networkComboBox.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        browseResultsLabel.setText("n identifiers in file");

        functionLabel.setLabelFor(functionComboBox);
        functionLabel.setText("Function Type:");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(resultsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(fileTextField)
                        .addGap(18, 18, 18)
                        .addComponent(browseButton))
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(browseResultsLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(namespaceLabel)
                            .addComponent(networkLabel)
                            .addComponent(functionLabel))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(namespaceComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(networkComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(functionComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(networkComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(networkLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(functionComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(functionLabel))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(namespaceLabel)
                    .addComponent(namespaceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(fileTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(browseResultsLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(resultsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private static List<String> readIdentifiersFromFile(final File file)
            throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        List<String> identifiers = new ArrayList<String>();
        // this is null so it can be determined by the first iteration
        Boolean csv = null;
        try {
            for (String line = null; (line = reader.readLine()) != null;) {
                if (isBlank(line)) {
                    // ignore blank lines
                    continue;
                }

                if (csv == null) {
                    csv = (line.indexOf(",") != -1);
                }

                if (csv) {
                    // if csv take first column as identifier
                    String[] split = line.split(",");
                    String firstColumn = split[0];
                    if (!isBlank(firstColumn)) { // don't add blank identifiers
                        identifiers.add(firstColumn);
                    }
                } else {
                    // else just take entire line as identifier
                    identifiers.add(line);
                }
            }
        } finally {
            Utility.closeSilently(reader);
        }
        return identifiers;
    }

    /**
     * Returns true if string is null, empty, or all whitespace
     */
    private static boolean isBlank(final String string) {
        if (string == null || string.isEmpty()) {
            return true;
        }

        for (char c : string.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    private static final class NamespaceOption implements
            Comparable<NamespaceOption> {
        private final NamespaceDescriptor descriptor;

        public NamespaceOption(NamespaceDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public NamespaceDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public String toString() {
            return descriptor.getName();
        }

        @Override
        public int compareTo(NamespaceOption o) {
            if (o == null) {
                return 1;
            }
            return this.toString().compareTo(o.toString());
        }
    }
    

    // FIXME this is taken straight from search KAM Dialog
    // extract to common location?
    /**
     * The {@link AbstractTableModel table model} for the
     * {@link KamNode kam node} search results.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static final class ResultsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -5744344001683506045L;
        private final String[] headers = new String[] { "Label" };
        private final List<KamNode> nodes;

        public ResultsTableModel() {
            this.nodes = new ArrayList<KamNode>();
        }

        public void setData(final Collection<KamNode> nodes) {
            this.nodes.clear();
            this.nodes.addAll(nodes);
            fireTableDataChanged();
        }

        public void clear() {
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
}
