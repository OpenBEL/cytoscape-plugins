/*
 * BEL Framework Webservice Plugin
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
package org.openbel.belframework.webservice;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import cytoscape.Cytoscape;

/**
 * {@link SettingsDialog} defines the <em>BELFramework Configuration</em>
 * dialog that sets the BELFramework Web API configuration.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class SettingsDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 6873725058633597932L;
    private static final String TITLE = "BELFramework Configuration";
    private static final Configuration cfg = Configuration.getInstance();
    private JTextField wsdlURLTxt;
    private JSpinner timeoutSpn;
    private JCheckBox shortFormChk;
    private JButton cancelBtn;
    private JButton saveBtn;

    /**
     * Constructs the dialog and initializes the UI.
     */
    public SettingsDialog() {
        super(Cytoscape.getDesktop(), TITLE, true);

        // set up dialog components
        initUI();

        // set configuration values
        wsdlURLTxt.setText(cfg.getWSDLURL());
        timeoutSpn.setValue(cfg.getTimeout());
        shortFormChk.setSelected(cfg.getShortBelForm());

        // set up dialog
        setTitle(TITLE);
        final Dimension dialogDim = new Dimension(600, 300);
        setMinimumSize(dialogDim);
        setSize(dialogDim);
        setPreferredSize(dialogDim);
        setLocationRelativeTo(null);
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
    }

    /**
     * Initialize UI components of the dialog.
     */
    private void initUI() {
        GridBagConstraints gridBagConstraints;

        JPanel sp = new JPanel();

        wsdlURLTxt = new JTextField();
        sp.setLayout(new java.awt.GridBagLayout());
        JLabel wsdlURLLbl = new JLabel("WSDL URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        sp.add(wsdlURLLbl, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 5.0;
        gridBagConstraints.weighty = 0.1;
        sp.add(wsdlURLTxt, gridBagConstraints);

        JLabel timeoutLbl = new JLabel("Timeout:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        sp.add(timeoutLbl, gridBagConstraints);
        timeoutSpn = new JSpinner();
        timeoutSpn.setModel(new SpinnerNumberModel(120, 5, 1800, 1));
        timeoutSpn.setPreferredSize(new java.awt.Dimension(90, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        sp.add(timeoutSpn, gridBagConstraints);

        JLabel shortFormLbl = new JLabel("Use short-form in BEL terms:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 5.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        sp.add(shortFormLbl, gridBagConstraints);

        shortFormChk = new JCheckBox();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 5.0;
        gridBagConstraints.weighty = 5.0;
        sp.add(shortFormChk, gridBagConstraints);

        getContentPane().add(sp, java.awt.BorderLayout.CENTER);

        JPanel bp = new JPanel();
        bp.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(this);
        bp.add(cancelBtn);
        saveBtn = new JButton("Save");
        saveBtn.addActionListener(this);
        bp.add(saveBtn);
        getContentPane().add(bp, java.awt.BorderLayout.SOUTH);
    }

    /**
     * {@inheritDoc}
     *
     * Handles the saving of webservice configuration by calling
     * {@link ClientConnector#reconfigure()}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancelBtn) {
            this.dispose();
        } else if (e.getSource() == saveBtn) {
            final Configuration cfg = Configuration.getInstance();
            cfg.setWSDLURL(wsdlURLTxt.getText());
            cfg.setTimeout((Integer) timeoutSpn.getValue());
            cfg.setShortBelForm(shortFormChk.isSelected());

            try {
                cfg.saveState();
                ClientConnector.getInstance().reconfigure();
                this.dispose();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
