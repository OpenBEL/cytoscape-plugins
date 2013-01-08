/*
 * BEL Framework Webservice Plugin
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
package org.openbel.cytoscape.webservice;

import java.util.Collection;
import java.util.List;

import org.openbel.framework.ws.model.BelStatement;
import org.openbel.framework.ws.model.BelTerm;
import org.openbel.framework.ws.model.DialectHandle;
import org.openbel.framework.ws.model.Edge;
import org.openbel.framework.ws.model.EdgeDirectionType;
import org.openbel.framework.ws.model.EdgeFilter;
import org.openbel.framework.ws.model.FunctionType;
import org.openbel.framework.ws.model.Kam;
import org.openbel.framework.ws.model.KamEdge;
import org.openbel.framework.ws.model.KamHandle;
import org.openbel.framework.ws.model.KamNode;
import org.openbel.framework.ws.model.LoadKamResponse;
import org.openbel.framework.ws.model.Namespace;
import org.openbel.framework.ws.model.NamespaceDescriptor;
import org.openbel.framework.ws.model.NamespaceValue;
import org.openbel.framework.ws.model.Node;
import org.openbel.framework.ws.model.NodeFilter;
import org.openbel.framework.ws.model.SimplePath;
import org.openbel.framework.ws.model.WebAPI;

/**
 * {@link KamService} defines an API wrapper around version 1.2.3 of the
 * {@link WebAPI BEL Framework Web API}.
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public interface KamService {

    /**
     * Reloads the {@link ClientConnector} in this {@link KamService}
     */
    void reloadClientConnector();

    /**
     * Finds {@link KamNode KamNodes} by a {@link List} of
     * {@link NamespaceValue NamespaceValues} and optional {@link NodeFilter
     * node filter}.
     * 
     * @param kamHandle the {@link KamHandle kam handle} that identifies a
     * loaded {@link Kam kam}
     * @param dialectHandle the {@link DialectHandle dialect handle} that
     * identifies a loaded dialect, can be null
     * @param namespaceValues {@link List} of
     * {@link NamespaceValue NamespaceValues}, can not be empty or null
     * @param nodeFilter the optional {@link NodeFilter node filter} to further
     * restrict the results, can be null
     * @return the {@link List} of {@link KamNode kam nodes}, which will not be
     * {@code null} but may be empty
     */
    List<KamNode> findKamNodesByNamespaceValues(KamHandle kamHandle,
            DialectHandle dialectHandle, List<NamespaceValue> namespaceValues,
            NodeFilter nodeFilter);

    /**
     * Find {@link NamespaceValue}s for a given {@link Collection} of java regex
     * patterns and optional a {@link Collection} of {@link Namespace}s
     * 
     * @param patterns java regex patterns, can not be null or empty
     * @param namespaces {@link Namespace}s, can be null or empty
     * @return {@link List} of {@link NamespaceValue}s
     */
    List<NamespaceValue> findNamespaceValues(Collection<String> patterns,
            Collection<Namespace> namespaces);

    /**
     * Retrieves all {@link NamespaceDescriptor NamespaceDescriptors} recognized
     * by the BEL Framework
     * 
     * @return a {@link List} of {@link NamespaceDescriptor}s
     */
    List<NamespaceDescriptor> getAllNamespaces();

    /**
     * Retrieves the KAM catalog for the configured BELFramework.
     * 
     * @return the {@link List} of {@link Kam kams} from the KAM catalog, which
     * will not be {@code null} but might be empty
     */
    List<Kam> getCatalog();

    /**
     * Retrieves the default dialect for a KAM.
     * 
     * @param kamHandle the {@link KamHandle kam handle} that identifies a
     * loaded {@link Kam kam}
     */
    DialectHandle getDefaultDialect(KamHandle kamHandle);

    /**
     * Fires a loads request for a {@link Kam kam} on the server end of the
     * webservice and returns a {@link LoadKamResponse response}.
     * 
     * @param kam the {@link Kam kam} to load
     * @return the {@link LoadKamResponse response} for kam load request
     * @throws IllegalArgumentException Thrown if the {@code kam} parameter is
     * {@code null}
     * @see KamService#getCatalog()
     */
    LoadKamResponse loadKam(Kam kam);

    /**
     * Retrieves the supporting {@link BelTerm BEL terms} for a specific
     * {@link KamNode kam node}.
     * 
     * @param node the {@link KamNode kam node}
     * @return the {@link List} of {@link BelTerm BEL terms} that back the
     * {@link KamNode kam node}, which will not be {@code null} and must
     * contain at least one entry
     * @throws IllegalArgumentException Thrown if the {@code node} parameter is
     * {@code null}
     */
    List<BelTerm> getSupportingTerms(KamNode node);

    /**
     * Retrieves the supporting {@link BelStatement statements} for a specific
     * {@link KamEdge kam edge}.
     * 
     * @param edge the {@link KamEdge kam edge}
     * @return the {@link List} of {@link BelStatement statements} that back
     * the {@link KamEdge kam edge}, which will not be {@code null} and must
     * contain at least one entry
     * @throws IllegalArgumentException Thrown if the {@code kam} parameter is
     * {@code null}
     */
    List<BelStatement> getSupportingEvidence(KamEdge edge);

    /**
     * Finds {@link KamNode kam nodes} by {@link FunctionType BEL function} for
     * a specific loaded kam.
     * 
     * @param kamHandle the {@link KamHandle kam handle} that identifies a
     * loaded {@link Kam}
     * @param dialectHandle the {@link DialectHandle dialect handle} that
     * identifies a loaded dialect, can be null
     * @param function the {@link FunctionType BEL function} to find by
     * @return the {@link List} of {@link KamNode kam nodes}, which will not be
     * {@code null} but may be empty
     * @throws IllegalArgumentException Thrown if the {@code handle} or
     * {@code function} parameter is {@code null}
     */
    List<KamNode> findKamNodesByFunction(KamHandle kamHandle,
            DialectHandle dialectHandle, FunctionType function);

    /**
     * Finds {@link KamNode kam nodes} by a regular expression pattern and
     * optional {@link NodeFilter node filter}.
     * 
     * @param kamHandle the {@link KamHandle kam handle} that identifies a
     * loaded {@link Kam kam}
     * @param dialectHandle the {@link DialectHandle dialect handle} that
     * identifies a loaded dialect, can be null
     * @param regex the regular expression {@link String} to find by
     * @param nf the optional {@link NodeFilter node filter} to further
     * restrict the results
     * @return the {@link List} of {@link KamNode kam nodes}, which will not
     * be {@code null} but may be empty
     * @throws IllegalArgumentException Thrown if the {@code handle} or
     * {@code regex} parameter is {@code null}
     */
    List<KamNode> findKamNodesByPatterns(KamHandle kamHandle,
            DialectHandle dialectHandle, String regex, NodeFilter nf);

    /**
     * Retrieves {@link KamEdge kam edges} that are adjacent to a
     * {@link KamNode kam node} either in the outgoing, incoming, or both
     * directions. Optionally an {@link EdgeFilter edge filter} can be used to
     * further restrict the results
     * 
     * @param dialectHandle the {@link DialectHandle dialect handle} that
     * identifies a loaded dialect, can be null
     * @param node the {@link KamNode kam node} to find adjacent
     * {@link KamEdge kam edges} for
     * @param direction the {@link EdgeDirectionType edge direction}
     * @param ef the optional {@link EdgeFilter edge filter}
     * @return the {@link List} of {@link KamEdge kam edges}, which will not be
     * {@code null}, but may be empty
     * @throws IllegalArgumentException Thrown if the {@code node} or
     * {@code direction} parameter is {@code null}
     */
    List<KamEdge> getAdjacentKamEdges(DialectHandle dialectHandle,
            KamNode node, EdgeDirectionType direction, EdgeFilter ef);

    /**
     * Retrieves {@link SimplePath SimplePaths} between the given source nodes.
     * 
     * @param dialectHandle the {@link DialectHandle dialect handle} that
     * identifies a loaded dialect, can be null
     * @param sources {@link KamNode KamNodes} 2 or more nodes to search
     * between, should not be {@code null} or less then 2.
     * @param maxDepth search depth, if {@code null} backend defaults are used
     * @return the {@list List} of {@link SimplePath SimplePaths}, can be empty
     * but never {@code null}.
     */
    List<SimplePath> interconnect(DialectHandle dialectHandle,
            Collection<KamNode> sources, Integer maxDepth);

    /**
     * Resolves {@link Node nodes} ({@link FunctionType function} and
     * {@link String label}) to {@link KamNode KAM nodes} for a single
     * {@link KamHandle KAM handle}.
     * 
     * <p>
     * A {@link List} of resolved {@link KamNode KAM nodes} is returned.  The
     * cardinality of this {@link List} is equal to the {@link Node nodes}
     * input and contains {@code null}s when a {@link Node node} cannot be
     * resolved.
     * 
     * <p>
     * If a {@link DialectHandle dialect handle} is provided then
     * {@link KamNode KAM node} labels will be valid BEL term expressions.
     * 
     * @param kamHandle {@link KamHandle}; may not be {@code null}
     * @param nodes {@link List} of {@link Node}
     * @param dialectHandle {@link DialectHandle}
     * @return resolved {@link List} of {@link KamNode KAM nodes} or
     * {@code null} if {@code nodes} is {@code null}
     * @throws NullPointerException when {@code kamHandle} is {@code null}
     */
    List<KamNode> resolveNodes(KamHandle kamHandle, List<Node> nodes,
            DialectHandle dialectHandle);
    
    /**
     * Resolves {@link Edge edges} ({@link Node}, {@link RelationshipType},
     * {@link Node}) to {@link KamEdge KAM edges} for a single
     * {@link KamHandle KAM handle}.
     * 
     * <p>
     * A {@link List} of resolved {@link KamEdge KAM edges} is returned.  The
     * cardinality of this {@link List} is equal to the {@link Edge edges}
     * input and contains {@code null}s when an {@link Edge edge} cannot be
     * resolved.
     * 
     * <p>
     * If a {@link DialectHandle dialect handle} is provided then
     * {@link KamNode KAM node} labels will be valid BEL term expressions.
     * 
     * @param kamHandle {@link KamHandle}; may not be {@code null}
     * @param edges {@link List} of {@link Edge}; may not be {@code null} or
     * empty
     * @param dialectHandle {@link DialectHandle}
     * @return resolved {@link List} of {@link KamEdge KAM edges} or
     * {@code null} if {@code edges} is {@code null} 
     * @throws NullPointerException when {@code kamHandle} is {@code null}
     */
    List<KamEdge> resolveEdges(KamHandle kamHandle, List<Edge> edges,
            DialectHandle dialectHandle);
}