/*
 * KAM Navigator Plugin
 *
 * URLs: http://openbel.org/
 * Copyright (C) 2012, Selventa
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openbel.belframework.kam.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.openbel.belframework.kam.KAMNavigatorPlugin;
import org.openbel.belframework.kam.KAMSession;
import org.openbel.belframework.kam.Utility;
import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.DialectHandle;
import com.selventa.belframework.ws.client.KAMLoadStatus;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamHandle;
import com.selventa.belframework.ws.client.LoadKamResponse;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

/**
 * {@link LoadKAMDialog} represents the UI for the Load KAM dialog.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class LoadKAMDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -6182867704453220885L;
    private static final String DIALOG_TITLE = "Load KAM from Catalog";
    private final KAMService kamService;
    private final SimpleDateFormat dateFormat;
    private JComboBox selectKAMCmb;
    private JLabel kamName;
    private JTextArea kamDesc;
    private JLabel kamLastCompiled;
    private JButton cancelBtn;
    private JButton selectBtn;

    /**
     * Construct the {@link JDialog dialog} and initialize the UI.
     *
     * @see #initUI()
     */
    public LoadKAMDialog() {
        super(Cytoscape.getDesktop(), DIALOG_TITLE, false);
        this.kamService = KAMServiceFactory.getInstance().getKAMService();
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa");

        initUI();
    }

    private void initUI() {
        // Initialize dialog with Select KAM and button panel.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createSelectKAMPanel(), BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

        // init state
        if (selectKAMCmb.getModel().getSize() > 0) {
            selectKAMCmb.setSelectedIndex(0);
        }
        
        // Dialog settings
        final Dimension dialogDim = new Dimension(400, 300);
        setMinimumSize(dialogDim);
        setSize(dialogDim);
        setPreferredSize(dialogDim);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
    }

    private JPanel createSelectKAMPanel() {
        GridBagConstraints gbc = new GridBagConstraints();

        JPanel selectKAMPanel = new JPanel(new BorderLayout());

        final JPanel fp = new JPanel();
        JLabel kamLbl = new JLabel();
        this.selectKAMCmb = new JComboBox();
        JPanel dp = new JPanel();
        JLabel kamNameLbl = new JLabel();
        JLabel kamDescLbl = new JLabel();
        JLabel lcomLbl = new JLabel();
        this.kamName = new JLabel();
        this.kamLastCompiled = new JLabel();
        JScrollPane descPane = new JScrollPane();
        this.kamDesc = new JTextArea();
        this.kamDesc.setEnabled(false);
        this.kamDesc.setDisabledTextColor(Color.black);
        this.kamDesc.setBorder(BorderFactory.createEmptyBorder());

        // Field Panel
        fp.setLayout(new GridBagLayout());
        kamLbl.setText("KAM:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        fp.add(kamLbl, gbc);
        selectKAMCmb.setModel(loadKAMModel());
        selectKAMCmb.addActionListener(this);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 0.9;
        gbc.weighty = 1.0;
        fp.add(selectKAMCmb, gbc);
        dp.setBorder(BorderFactory.createTitledBorder("KAM Details"));
        dp.setLayout(new GridBagLayout());
        kamNameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        kamNameLbl.setText("Name:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 10.0;
        gbc.insets = new Insets(0, 3, 0, 0);
        dp.add(kamNameLbl, gbc);
        kamDescLbl.setText("Description:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 80.0;
        gbc.insets = new Insets(0, 3, 0, 0);
        dp.add(kamDescLbl, gbc);
        lcomLbl.setText("Last Compiled:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 10.0;
        gbc.insets = new Insets(0, 3, 0, 0);
        dp.add(lcomLbl, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 5.0;
        gbc.insets = new Insets(0, 0, 0, 3);
        dp.add(kamName, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 5.0;
        gbc.insets = new Insets(0, 0, 0, 3);
        dp.add(kamLastCompiled, gbc);
        kamDesc.setColumns(20);
        kamDesc.setEditable(false);
        kamDesc.setLineWrap(true);
        kamDesc.setRows(5);
        descPane.setViewportView(kamDesc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 5.0;
        gbc.insets = new Insets(0, 0, 5, 0);
        dp.add(descPane, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weighty = 30.0;
        fp.add(dp, gbc);
        selectKAMPanel.add(fp, BorderLayout.CENTER);

        return selectKAMPanel;
    }

    private JPanel createButtonPanel() {
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        cancelBtn = new JButton();
        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(this);
        buttonPanel.add(cancelBtn);

        selectBtn = new JButton();
        selectBtn.setText("Load");
        selectBtn.addActionListener(this);
        buttonPanel.add(selectBtn);
        return buttonPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e != null) {
            if (e.getSource() == cancelBtn) {
                this.dispose();
            } else if (e.getSource() == selectBtn) {
                final KAMListModel kamModel = (KAMListModel) selectKAMCmb
                        .getModel();
                final Kam selectedKAM = kamModel.getSelectedKAM();

                final LoadKAMTask loadKAMTask = new LoadKAMTask(selectedKAM);
                Utility.executeTask(loadKAMTask);
            } else if (e.getSource() == selectKAMCmb) {
                final KAMListModel kamModel = (KAMListModel) selectKAMCmb
                        .getModel();
                final Kam selectedKAM = kamModel.getSelectedKAM();

                this.kamName.setText(selectedKAM.getName());
                this.kamDesc.setText(selectedKAM.getDescription());

                final Date compileTime = selectedKAM.getLastCompiled()
                        .toGregorianCalendar().getTime();
                this.kamLastCompiled.setText(dateFormat.format(compileTime));
            }
        }
    }

    /**
     * Fetches {@link Kam kams} from the KAM catalog and creates a new
     * {@link KAMListModel kam list model} to display to the user.
     *
     * @return the {@link KAMListModel kam list mode} to show to the user
     */
    private KAMListModel loadKAMModel() {
        final List<Kam> results = kamService.getCatalog();
        List<Kam> kams = new ArrayList<Kam>(results.size());
        for (final Kam result : results) {
            kams.add(result);
        }

        return new KAMListModel(kams);
    }

    /**
     * The {@link Task cytoscape task} to load a selected {@link Kam kam}.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class LoadKAMTask implements Task {
        private final Kam kam;
        private TaskMonitor m;
        private final int SLEEP_TIME_MS = 1000;
        // marked as volatile in case halt is called by multiple threads
        private volatile boolean halt = false;

        private LoadKAMTask(final Kam kam) {
            this.kam = kam;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTitle() {
            return "Loading KAM";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setTaskMonitor(TaskMonitor m)
                throws IllegalThreadStateException {
            this.m = m;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void halt() {
            halt = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            m.setStatus("Loading \"" + kam.getName() + "\" KAM.");

            m.setPercentCompleted(0);
            loadKAM();
            m.setPercentCompleted(100);
        }

        /**
         * Calls out to Web API using {@link KAMServices} and loads a specific
         * {@link Kam kam}.
         *
         * @see KAMServices#loadKam(Kam)
         */
        private void loadKAM() {
            
            LoadKamResponse res = kamService.loadKam(kam);
            while (!halt && res.getLoadStatus() == KAMLoadStatus.IN_PROCESS) {
                // sleep and then retry
                try {
                    Thread.sleep(SLEEP_TIME_MS);
                } catch (InterruptedException e) {
                    halt = true;
                }

                res = kamService.loadKam(kam);
            }
            KamHandle kamHandle = null;
            if (res.getLoadStatus() == KAMLoadStatus.COMPLETE) {
                kamHandle = res.getHandle();
            } else if (res.getLoadStatus() == KAMLoadStatus.FAILED) {
                // dispose of kam select dialog
                // otherwise user can't click on error dialogue
                // TODO fix this, should just close progress bar but leave
                // select dialog open for additional selections
                LoadKAMDialog.this.dispose();
                
                JOptionPane.showMessageDialog(getContentPane(),
                        "Error loading \"" + kam.getName() + "\" KAM.\n",
                        "Kam Load Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // else still in progress and was canceled

            if (kamHandle != null) {
                // load default dialect handle
                DialectHandle dialectHandle = kamService
                        .getDefaultDialect(kamHandle);
                KAMSession session = KAMSession.getInstance();
                
                // set unique network name
                Set<String> existingNames = new HashSet<String>();
                for (KAMNetwork network : session.getKAMNetworks()) {
                    existingNames.add(network.getName());
                }
                String networkName = kam.getName();
                int i = 2;
                while (existingNames.contains(networkName)) {
                    networkName = kam.getName() + " " + i;
                    i++;
                }
                
                // Create KAM Network for this selected KAM.
                final KAMNetwork kamNetwork = new KAMNetwork(networkName, 
                        kamHandle, dialectHandle);
    
                // Store session data for KAM and CyNetwork.
                session.getKAMNetworks().add(kamNetwork);
                
                Cytoscape.firePropertyChange(
                        KAMNavigatorPlugin.KAM_NETWORK_CREATED_EVENT, null,
                        kamNetwork);
            }

            // dispose of kam select dialog
            LoadKAMDialog.this.dispose();
        }
    }

    /**
     * The {@link AbstractListModel list model} for showing {@link Kam kams} in
     * the KAM catalog.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class KAMListModel extends AbstractListModel implements ComboBoxModel {
        private static final long serialVersionUID = 8038870641055715208L;
        private List<Kam> kams;
        private Kam selectedKAM;

        public KAMListModel(final List<Kam> kams) {
            if (kams == null) {
                throw new IllegalArgumentException("kams cannot be null");
            }

            Collections.sort(kams, new Comparator<Kam>() {
                @Override
                public int compare(Kam o1, Kam o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            this.kams = kams;
        }

        @Override
        public int getSize() {
            return kams.size();
        }

        @Override
        public Object getElementAt(int i) {
            return kams.get(i).getName();
        }

        public Kam getSelectedKAM() {
            return selectedKAM;
        }

        @Override
        public Object getSelectedItem() {
            if (selectedKAM == null) {
                return null;
            }

            return selectedKAM.getName();
        }

        @Override
        public void setSelectedItem(Object item) {
            final String kamName = (String) item;
            for (final Kam kam : kams) {
                if (kam.getName().equals(kamName)) {
                    selectedKAM = kam;
                    break;
                }
            }
        }
    }
}
