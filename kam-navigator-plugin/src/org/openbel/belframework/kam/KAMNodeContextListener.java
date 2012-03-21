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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.SimplePath;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
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
    private static final int DEFAULT_INTERCONNECT_DEPTH = 2;

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
        Set<CyNode> selected = new HashSet<CyNode>(view.getSelectedNodes());
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
        
        menu.add(kamNodeItem);
    }

    /**
     * A menu {@link AbstractAction action} that drives the expansion of the
     * active {@link KamNode kam node}.
     *
     * @see #actionPerformed(ActionEvent)
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static final class ExpandAction extends EdgeAddAction {
        private static final long serialVersionUID = -8467637028387407708L;
        private final EdgeDirectionType direction;
        private final KAMService kamService;
        private final Set<CyNode> cynodes;

        private ExpandAction(final EdgeDirectionType direction,
                final Set<CyNode> cynodes, final CyNetworkView view) {
            super("Expand " + getLabel(direction), view);
            this.direction = direction;
            this.kamService = KAMServiceFactory.getInstance().getKAMService();
            this.cynodes = cynodes;
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
        Collection<KamEdge> getEdgesToAdd(KAMNetwork kamNetwork) {
            final Collection<KamNode> kamNodes = new HashSet<KamNode>();
            for (final CyNode cynode : cynodes) {
                kamNodes.add(kamNetwork.getKAMNode(cynode));
            }

            List<KamEdge> edges = new ArrayList<KamEdge>();
            for (KamNode kamNode : kamNodes) {
                edges.addAll(kamService.getAdjacentKamEdges(
                        kamNetwork.getDialectHandle(), kamNode, direction, 
                        null));
            }
            return edges;
        }
    }

    /**
     * A menu {@link AbstractAction action} that drives the interconnect of the
     * selected {@link KamNode kam nodes}.
     * 
     * @see #actionPerformed(ActionEvent)
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static final class InterconnectAction extends EdgeAddAction {
        private static final long serialVersionUID = 8540857606052921412L;
        private final KAMService kamService;
        private final Set<CyNode> cynodes;

        private InterconnectAction(final Set<CyNode> cynodes, 
                final CyNetworkView view) {
            super(INTERCONNECT_LABEL, view);
            this.kamService = KAMServiceFactory.getInstance().getKAMService();
            this.cynodes = cynodes;
            
            if (cynodes == null || cynodes.size() < 2) {
                throw new IllegalArgumentException("Can't interconnect less then two nodes");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        Collection<KamEdge> getEdgesToAdd(KAMNetwork kamNetwork) {
            final Collection<KamNode> kamNodes = new HashSet<KamNode>();
            for (final CyNode cynode : cynodes) {
                kamNodes.add(kamNetwork.getKAMNode(cynode));
            }

            final List<SimplePath> paths = kamService.interconnect(
                    kamNetwork.getDialectHandle(), kamNodes, 
                    DEFAULT_INTERCONNECT_DEPTH);
            final List<KamEdge> edges = new ArrayList<KamEdge>();
            for (final SimplePath path : paths) {
                edges.addAll(path.getEdges());
            }
            return edges;
        }
    }

    /**
     * Abstract action for edge addition
     * 
     * @see #actionPerformed(ActionEvent)
     * @author James McMahon &lt;jmcmahon@selventa.com&gt;
     */
    private static abstract class EdgeAddAction extends AbstractAction {
        // TODO review how serial version ids work on abstract classes
        // is this needed?
        private static final long serialVersionUID = 8091837614372980023L;
        protected final CyNetworkView view;

        protected EdgeAddAction(String label, CyNetworkView view) {
            super(label);
            this.view = view;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final void actionPerformed(ActionEvent e) {
            // FIXME If a cytoscape session is restored the KAMNetwork
            // will not exist. We will have to reconnect to the KAM.

            final CyNetwork network = view.getNetwork();
            final KAMNetwork kamNetwork = KAMSession.getInstance()
                    .getKAMNetwork(network);
            final Collection<KamEdge> edges = getEdgesToAdd(kamNetwork);

            final Set<CyNode> nn = new HashSet<CyNode>();
            for (final KamEdge edge : edges) {
                CyEdge cye = kamNetwork.addEdge(edge);

                nn.add((CyNode) cye.getSource());
                nn.add((CyNode) cye.getTarget());
            }

            network.unselectAllNodes();
            network.setSelectedNodeState(nn, true);

            CyLayoutAlgorithm dcl = CyLayouts.getLayout("degree-circle");
            dcl.setSelectedOnly(true);
            dcl.doLayout(view);

            view.redrawGraph(true, true);
        }

        /**
         * Generate all edges to be added to to the network
         * 
         * @param kamNetwork
         *            for node translation
         * @return {@link Collection} of {@link KamEdge KamEdges} to add, can be
         *         empty but not null
         */
        abstract Collection<KamEdge> getEdgesToAdd(KAMNetwork kamNetwork);
    }
}
