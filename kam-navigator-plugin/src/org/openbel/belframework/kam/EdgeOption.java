package org.openbel.belframework.kam;

/**
 * {@link EdgeOption} enum for the different types of expansion rules.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public enum EdgeOption {
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