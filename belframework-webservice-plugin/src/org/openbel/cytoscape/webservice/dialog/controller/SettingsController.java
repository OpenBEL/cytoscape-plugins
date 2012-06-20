package org.openbel.cytoscape.webservice.dialog.controller;

import java.io.IOException;

import javax.swing.JOptionPane;

import org.openbel.cytoscape.webservice.ClientConnector;
import org.openbel.cytoscape.webservice.Configuration;
import org.openbel.cytoscape.webservice.dialog.view.SettingsView;
import org.openbel.swing.mvc.AbstractController;

import cytoscape.Cytoscape;
import cytoscape.logger.CyLogger;

public class SettingsController extends AbstractController {

    private static final CyLogger log = CyLogger
            .getLogger(SettingsController.class);

    public SettingsController() {
        // no model for now
        addView(new SettingsView(this));
    }

    public void save(String wsdlUrl, Integer timeout) {
        final Configuration cfg = Configuration.getInstance();
        cfg.setWSDLURL(wsdlUrl);
        cfg.setTimeout(timeout);

        // write configuration to file
        try {
            cfg.saveState();
        } catch (IOException ex) {
            String msg = "Error writing to configuration file";
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(), msg,
                    "IO Error", JOptionPane.ERROR_MESSAGE);
            log.error(msg, ex);
        }

        // reload connector
        ClientConnector client = ClientConnector.getInstance();
        client.reconfigure();
        if (!client.isValid()) {
            JOptionPane
                    .showMessageDialog(
                            Cytoscape.getDesktop(),
                            "Error connecting to the BEL Framework Web Services.\n"
                                    + "Please check the BEL Framework Web Services Configuration.",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
