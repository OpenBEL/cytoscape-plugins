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

import static com.selventa.belframework.ws.client.EdgeDirectionType.BOTH;
import static com.selventa.belframework.ws.client.EdgeDirectionType.FORWARD;
import static com.selventa.belframework.ws.client.EdgeDirectionType.REVERSE;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_FUNCTION_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_ID_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_LABEL_ATTR;
import giny.view.NodeView;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.openbel.belframework.kam.dialog.KnowledgeNeighborhoodDialog;
import org.openbel.belframework.kam.task.KAMTasks;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import ding.view.NodeContextMenuListener;

/**
 * {@link KAMNodeContextListener} contributes actions to the context-sensitive
 * menu when clicking on a {@link CyNode cytoscape node}.  Specifically the
 * user is allowed to:
 * <ul>
 * <li>Expand to {@link KamNode kam nodes} downstream of current node.</li>
 * <li>Expand to {@link KamNode kam nodes} upstream of current node.</li>
 * <li>Expand in both directions from the {@link KamNode kam nodes}.</li>
 * </ul>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMNodeContextListener implements PropertyChangeListener,
        NodeContextMenuListener {
    private static final CyAttributes nodeAtt = Cytoscape.getNodeAttributes();
    private static final String INTERCONNECT_LABEL = "Interconnect";

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e != null) {
            if (CytoscapeDesktop.NETWORK_VIEW_CREATED.equals(e
                    .getPropertyName())) {
                CyNetworkView view = (CyNetworkView) e.getNewValue();
                view.addNodeContextMenuListener(this);
            } else if (CytoscapeDesktop.NETWORK_VIEW_DESTROYED.equals(e
                    .getPropertyName())) {
                CyNetworkView view = (CyNetworkView) e.getNewValue();
                view.removeNodeContextMenuListener(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNodeContextMenuItems(NodeView nv, JPopupMenu menu) {
        if (!(nv.getGraphView() instanceof CyNetworkView)) {
            // this shouldn't happen, but if it does return gracefully
            return;
        }
        
        CyNetworkView view = (CyNetworkView) nv.getGraphView();
        // documentation doesn't specify that selected nodes are CyNodes 
        //  but they should be 
        @SuppressWarnings("unchecked")
        Set<CyNode> selected = view.getNetwork().getSelectedNodes();
        for (CyNode cynode : selected) {
            // check to see if node is KAM backed
            String cyid = cynode.getIdentifier();
            final String id = nodeAtt.getStringAttribute(cyid, KAM_NODE_ID_ATTR);
            final String func = nodeAtt.getStringAttribute(cyid, KAM_NODE_FUNCTION_ATTR);
            final String lbl = nodeAtt.getStringAttribute(cyid, KAM_NODE_LABEL_ATTR);
            if (id == null || func == null || lbl == null) {
                // return if cynode does not reference kam node
                return;
            }
        }

        if (menu == null) {
            menu = new JPopupMenu();
        }
        
        // construct node menu and add to context popup
        final JMenu kamNodeItem = new JMenu("KAM Node");
        final JMenuItem downstream = new JMenuItem(new ExpandAction(FORWARD,
                selected, view));
        kamNodeItem.add(downstream);
        final JMenuItem upstream = new JMenuItem(new ExpandAction(REVERSE,
                selected, view));
        kamNodeItem.add(upstream);
        final JMenuItem both = new JMenuItem(new ExpandAction(BOTH, selected,
                view));
        kamNodeItem.add(both);
        
        if (selected.size() > 1) {
            // interconnect added only if more then one nodes are selected
            final JMenuItem interconnect = new JMenuItem(new InterconnectAction(
                    selected, view));
            kamNodeItem.add(interconnect);
        } else {
            // create placeholder disabled item for interconnect to keep menu 
            // size consistent
            final JMenuItem placeholder = new JMenuItem(INTERCONNECT_LABEL);
            placeholder.setEnabled(false);
            kamNodeItem.add(placeholder);
        }
        
        // add "Knowledge Neighborhood" action to context menu
        kamNodeItem.add(new KnowledgeNeighborhoodDialogAction());
        
        menu.add(kamNodeItem);
    }

    /**
     * The {@link CytoscapeAction action} to trigger the Knowledge Neighborhood
     * dialog.
     * 
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class KnowledgeNeighborhoodDialogAction extends
            CytoscapeAction {
        private static final long serialVersionUID = 2243171495622023060L;

        public KnowledgeNeighborhoodDialogAction() {
            super("Knowledge Neighborhood");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            KnowledgeNeighborhoodDialog dialog = new KnowledgeNeighborhoodDialog();
            dialog.setVisible(true);
        }
    }

    /**
     * A menu {@link AbstractAction action} that drives the expansion of the
     * active {@link KamNode kam node}.
     *
     * @see #actionPerformed(ActionEvent)
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static final class ExpandAction extends AbstractAction  {
        private static final long serialVersionUID = -8467637028387407708L;
        
        private final EdgeDirectionType direction;
        private final Set<CyNode> cynodes;
        private final CyNetworkView view;

        private ExpandAction(final EdgeDirectionType direction,
                final Set<CyNode> cynodes, final CyNetworkView view) {
            super("Expand " + getLabel(direction));
            this.direction = direction;
            this.cynodes = cynodes;
            this.view = view;
        }

        private static String getLabel(final EdgeDirectionType direction) {
            switch (direction) {
                case FORWARD:
                    return "Downstream";
                case REVERSE:
                    return "Upstream";
                case BOTH:
                    return "Downstream & Upstream";
                default:
                    throw new UnsupportedOperationException("Unsupported direction");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // FIXME If a cytoscape session is restored the KAMNetwork
            // will not exist. We will have to reconnect to the KAM.
            final CyNetwork network = view.getNetwork();
            final KAMNetwork kamNetwork = KAMSession.getInstance()
                    .getKAMNetwork(network);
            
            KAMTasks.expandNodes(kamNetwork, cynodes, direction);
        }
    }

    /**
     * A menu {@link AbstractAction action} that drives the interconnect of the
     * selected {@link KamNode kam nodes}.
     * 
     * @see #actionPerformed(ActionEvent)
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class InterconnectAction extends AbstractAction {
        private static final long serialVersionUID = 8540857606052921412L;
        
        private final Set<CyNode> cynodes;
        private final CyNetworkView view;

        private InterconnectAction(final Set<CyNode> cynodes, 
                final CyNetworkView view) {
            super(INTERCONNECT_LABEL);
            this.cynodes = cynodes;
            this.view = view;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // FIXME If a cytoscape session is restored the KAMNetwork
            // will not exist. We will have to reconnect to the KAM.
            final CyNetwork network = view.getNetwork();
            final KAMNetwork kamNetwork = KAMSession.getInstance()
                    .getKAMNetwork(network);
            
            KAMTasks.interconnectNodes(kamNetwork, cynodes);
        }
    }
}
