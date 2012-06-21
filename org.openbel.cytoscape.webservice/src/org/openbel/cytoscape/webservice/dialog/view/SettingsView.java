package org.openbel.cytoscape.webservice.dialog.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import org.openbel.cytoscape.webservice.ClientConnector;
import org.openbel.cytoscape.webservice.Configuration;
import org.openbel.cytoscape.webservice.dialog.controller.SettingsController;

import cytoscape.Cytoscape;

public class SettingsView extends JDialog implements ActionListener,
        PropertyChangeListener {

    private static final long serialVersionUID = 5906507645846028923L;

    private static final String TITLE = "BELFramework Configuration";
    private static final Configuration cfg = Configuration.getInstance();

    private final SettingsController controller;

    private JTextField wsdlURLTxt;
    private JSpinner timeoutSpn;
    private JButton cancelBtn;
    private JButton saveBtn;

    /**
     * Constructs the dialog and initializes the UI.
     */
    public SettingsView(SettingsController controller) {
        super(Cytoscape.getDesktop(), TITLE, true);

        this.controller = controller;

        // set up dialog components
        initUI();

        // set configuration values
        wsdlURLTxt.setText(cfg.getWSDLURL());
        timeoutSpn.setValue(cfg.getTimeout());

        // set up dialog
        setTitle(TITLE);
        final Dimension dialogDim = new Dimension(600, 300);
        setMinimumSize(dialogDim);
        setSize(dialogDim);
        setPreferredSize(dialogDim);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        
        this.setVisible(true);
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
            this.controller.save(wsdlURLTxt.getText(),
                    (Integer) timeoutSpn.getValue());
            this.dispose();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
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

}
