package org.openbel.cytoscape.navigator.dialog;

import static org.openbel.cytoscape.navigator.task.KamTasks.resolve;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openbel.cytoscape.navigator.KamIdentifier;
import org.openbel.cytoscape.webservice.Configuration;
import org.openbel.cytoscape.webservice.KamService;
import org.openbel.cytoscape.webservice.KamServiceFactory;
import org.openbel.framework.ws.model.Kam;

import cytoscape.Cytoscape;
import cytoscape.view.CyNetworkView;

public class AssociateToKamDialog extends JDialog implements ActionListener,
        ListSelectionListener {

    public static final String TITLE = "Associate to KAM";
    private static final long serialVersionUID = -4306286466293255277L;
    private final Map<String, Kam> kams;
    private final JList kamList;
    private final JButton cancel;
    private final JButton associate;
    private final CyNetworkView current;

    public AssociateToKamDialog() {
        super(Cytoscape.getDesktop(), TITLE, true);

        current = Cytoscape.getCurrentNetworkView();

        getContentPane().setLayout(new BorderLayout());

        // KAM list; center
        kams = new HashMap<String, Kam>();
        KamService svc = KamServiceFactory.getInstance().getKAMService();
        List<Kam> catalog = svc.getCatalog();
        String[] names = new String[catalog.size()];
        int i = 0;
        for (Kam k : catalog) {
            String name = k.getName();
            kams.put(name, k);
            names[i++] = name;
        }
        kamList = new JList(names);
        kamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kamList.getSelectionModel().addListSelectionListener(this);
        getContentPane().add(new JScrollPane(kamList), BorderLayout.CENTER);

        // Buttons; south
        JPanel buttons = new JPanel();
        buttons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        cancel = new JButton();
        cancel.addActionListener(this);
        cancel.setPreferredSize(new java.awt.Dimension(85, 25));
        cancel.setText("Cancel");
        buttons.add(cancel);
        associate = new JButton();
        associate.addActionListener(this);
        associate.setEnabled(false);
        associate.setPreferredSize(new java.awt.Dimension(105, 25));
        associate.setText("Associate");
        buttons.add(associate);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // Dialog settings
        final Dimension dialogDim = new Dimension(400, 600);
        setMinimumSize(dialogDim);
        setSize(dialogDim);
        setPreferredSize(dialogDim);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == cancel)
            this.dispose();
        else if (ev.getSource() == associate) {
            if (!kamList.isSelectionEmpty()) {
                String name = (String) kamList.getSelectedValue();
                Kam kam = kams.get(name);
                String wsdlUrl = Configuration.getInstance().getWSDLURL();
                KamIdentifier kamId = new KamIdentifier(kam, wsdlUrl);
                resolve(current, kamId);
                dispose();
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent ev) {
        Object selected = kamList.getSelectedValue();
        associate.setEnabled(selected != null);
    }
}
