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

import static com.selventa.belframework.ws.client.ObjectFactory.createFindKamNodesByPatternsRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetAdjacentKamEdgesRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetCatalogRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetDialectRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetSupportingEvidenceRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetSupportingTermsRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createInterconnectRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createLoadKamRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetAllNamespacesRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createFindNamespaceValuesRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createFindKamNodesByNamespaceValuesRequest;

import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import com.selventa.belframework.ws.client.BelStatement;
import com.selventa.belframework.ws.client.BelTerm;
import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.EdgeFilter;
import com.selventa.belframework.ws.client.FindKamNodesByNamespaceValuesRequest;
import com.selventa.belframework.ws.client.FindKamNodesByNamespaceValuesResponse;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsRequest;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsResponse;
import com.selventa.belframework.ws.client.FindNamespaceValuesRequest;
import com.selventa.belframework.ws.client.FindNamespaceValuesResponse;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.FunctionTypeFilterCriteria;
import com.selventa.belframework.ws.client.GetAdjacentKamEdgesRequest;
import com.selventa.belframework.ws.client.GetAdjacentKamEdgesResponse;
import com.selventa.belframework.ws.client.GetAllNamespacesRequest;
import com.selventa.belframework.ws.client.GetAllNamespacesResponse;
import com.selventa.belframework.ws.client.GetCatalogRequest;
import com.selventa.belframework.ws.client.GetCatalogResponse;
import com.selventa.belframework.ws.client.GetDialectRequest;
import com.selventa.belframework.ws.client.GetDialectResponse;
import com.selventa.belframework.ws.client.GetSupportingEvidenceRequest;
import com.selventa.belframework.ws.client.GetSupportingEvidenceResponse;
import com.selventa.belframework.ws.client.GetSupportingTermsRequest;
import com.selventa.belframework.ws.client.GetSupportingTermsResponse;
import com.selventa.belframework.ws.client.InterconnectRequest;
import com.selventa.belframework.ws.client.InterconnectResponse;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamHandle;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.LoadKamRequest;
import com.selventa.belframework.ws.client.LoadKamResponse;
import com.selventa.belframework.ws.client.Namespace;
import com.selventa.belframework.ws.client.NamespaceDescriptor;
import com.selventa.belframework.ws.client.NamespaceValue;
import com.selventa.belframework.ws.client.NodeFilter;
import com.selventa.belframework.ws.client.SimplePath;
import com.selventa.belframework.ws.client.WebAPI;
import com.selventa.belframework.ws.client.DialectHandle;

import cytoscape.Cytoscape;
import cytoscape.data.webservice.WebServiceClientManager;

/**
 * {@link DefaultKAMService} implements an API wrapper around version
 * 1.2.3 of the {@link WebAPI BEL Framework Web API}.  This lightweight class
 * reuses the same webservice stub instance obtained from the
 * {@link WebServiceClientManager cytoscape webservice manager}.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
class DefaultKAMService implements KAMService {
    protected WebAPI webAPI;
    private ClientConnector clientConnector;

    /**
     * Retrieves the webservice client from the
     * {@link WebServiceClientManager cytoscape webservice manager} and holds
     * the client stub.
     */
    DefaultKAMService() {
        reloadClientConnector();
    }
    
    /**
     * {@inheritDoc}
     */
    public void reloadClientConnector() {
        clientConnector = (ClientConnector) WebServiceClientManager
                .getClient("belframework");
        if (clientConnector == null) {
            return;
        }

        webAPI = clientConnector.getClientStub();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamespaceValue> findNamespaceValues(
            final Collection<String> patterns,
            final Collection<Namespace> namespaces) {
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("patterns parameter is invalid");
        }
        // namespaces can be null or empty

        checkValid();

        final FindNamespaceValuesRequest req = createFindNamespaceValuesRequest();
        req.getPatterns().addAll(patterns);
        if (namespaces != null) {
            req.getNamespaces().addAll(namespaces);
        }

        final FindNamespaceValuesResponse res = webAPI.findNamespaceValues(req);
        return res.getNamespaceValues();
    }

    /**
     * {@inheritDoc}
     */
    public List<KamNode> findKamNodesByNamespaceValues(
            final KamHandle kamHandle, final DialectHandle dialectHandle,
            final List<NamespaceValue> namespaceValues,
            final NodeFilter nodeFilter) {
        if (kamHandle == null) {
            throw new IllegalArgumentException("handle is null");
        }

        if (namespaceValues == null || namespaceValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "namespaceValues parameter is invalid");
        }
        // dialectHandle and nodeFilter can be null

        checkValid();

        final FindKamNodesByNamespaceValuesRequest req = createFindKamNodesByNamespaceValuesRequest();
        req.setHandle(kamHandle);
        req.getNamespaceValues().addAll(namespaceValues);

        if (dialectHandle != null) {
            req.setDialect(dialectHandle);
        }
        if (nodeFilter != null) {
            req.setFilter(nodeFilter);
        }

        final FindKamNodesByNamespaceValuesResponse res = webAPI
                .findKamNodesByNamespaceValues(req);
        return res.getKamNodes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamespaceDescriptor> getAllNamespaces() {
        checkValid();
        
        final GetAllNamespacesRequest req = createGetAllNamespacesRequest();
        final GetAllNamespacesResponse res = webAPI.getAllNamespaces(req);
        return res.getNamespaceDescriptors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<Kam> getCatalog() {
        checkValid();

        final GetCatalogRequest req = createGetCatalogRequest();

        final GetCatalogResponse res = webAPI.getCatalog(req);
        return res.getKams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadKamResponse loadKam(Kam kam) {
        if (kam == null || kam.getName() == null) {
            throw new IllegalArgumentException("kam parameter is invalid");
        }

        checkValid();

        final LoadKamRequest req = createLoadKamRequest();
        req.setKam(kam);
        return webAPI.loadKam(req);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DialectHandle getDialect() {
        // no parameters yet
        
        checkValid();
        final GetDialectRequest req = createGetDialectRequest();
        // nothing in the request
        final GetDialectResponse res = webAPI.getDialect(req);
        return res.getDialect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<BelTerm> getSupportingTerms(final KamNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node parameter is null");
        }

        checkValid();

        final GetSupportingTermsRequest req =
                createGetSupportingTermsRequest();
        req.setKamNode(node);

        final GetSupportingTermsResponse res = webAPI.getSupportingTerms(req);

        final List<BelTerm> terms = res.getTerms();
        for (final BelTerm term : terms) {
            formatLabel(term);
        }

        return terms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<BelStatement> getSupportingEvidence(final KamEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("edge parameter is null");
        }

        checkValid();

        final GetSupportingEvidenceRequest req =
                createGetSupportingEvidenceRequest();
        req.setKamEdge(edge);

        final GetSupportingEvidenceResponse res =
                webAPI.getSupportingEvidence(req);
        final List<BelStatement> stmts = res.getStatements();
        for (final BelStatement stmt : stmts) {
            final BelTerm subject = stmt.getSubjectTerm();

            String label = TermLabelFormatter.format(subject.getLabel());
            subject.setLabel(label);

            final BelTerm objectTerm = stmt.getObjectTerm();
            final BelStatement objectStmt = stmt.getObjectStatement();
            if (objectTerm != null) {
                label = TermLabelFormatter.format(objectTerm.getLabel());
                objectTerm.setLabel(label);
            } else if (objectStmt != null) {
                final BelTerm nestedSub = objectStmt.getSubjectTerm();
                label = TermLabelFormatter.format(nestedSub.getLabel());
                nestedSub.setLabel(label);

                final BelTerm nestedObj = objectStmt.getObjectTerm();
                label = TermLabelFormatter.format(nestedObj.getLabel());
                nestedObj.setLabel(label);
            }
        }

        return stmts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<KamNode> findKamNodesByFunction(final KamHandle handle, 
	        final DialectHandle dialectHandle, final FunctionType function) {
        if (handle == null) {
            throw new IllegalArgumentException("handle is null");
        }

        if (function == null) {
            throw new IllegalArgumentException("function is null");
        }

        checkValid();

        final FindKamNodesByPatternsRequest req =
                createFindKamNodesByPatternsRequest();
        req.setHandle(handle);
        if (dialectHandle != null) {
            req.setDialect(dialectHandle);
        }
        req.getPatterns().add(".*");

        final NodeFilter nf = new NodeFilter();
        final FunctionTypeFilterCriteria ftfc =
                new FunctionTypeFilterCriteria();
        ftfc.setIsInclude(true);
        ftfc.getValueSet().add(function);
        nf.getFunctionTypeCriteria().add(ftfc);
        req.setFilter(nf);

        final FindKamNodesByPatternsResponse res = webAPI
                .findKamNodesByPatterns(req);
        return res.getKamNodes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<KamNode> findKamNodesByPatterns(final KamHandle handle,
	        final DialectHandle dialectHandle, final String regex, 
	        final NodeFilter nf) {
        if (handle == null) {
            throw new IllegalArgumentException("handle is null");
        }

        if (regex == null) {
            throw new IllegalArgumentException("regex is null");
        }

        checkValid();

        final FindKamNodesByPatternsRequest req =
                createFindKamNodesByPatternsRequest();
        req.setHandle(handle);
        if (dialectHandle != null) {
            req.setDialect(dialectHandle);
        }

        req.getPatterns().add(regex);
        if (nf != null) {
            req.setFilter(nf);
        }

        final FindKamNodesByPatternsResponse res = webAPI
                .findKamNodesByPatterns(req);
        return res.getKamNodes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<KamEdge> getAdjacentKamEdges(final DialectHandle dialectHandle, 
	        final KamNode node, final EdgeDirectionType direction, 
	        final EdgeFilter ef) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }

        if (direction == null) {
            throw new IllegalArgumentException("direction is null");
        }

        checkValid();

        final GetAdjacentKamEdgesRequest req =
                createGetAdjacentKamEdgesRequest();
        if (dialectHandle != null) {
            req.setDialect(dialectHandle);
        }
        req.setKamNode(node);
        req.setDirection(direction);

        if (ef != null) {
            req.setFilter(ef);
        }

        final GetAdjacentKamEdgesResponse res = webAPI.getAdjacentKamEdges(req);
        return res.getKamEdges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SimplePath> interconnect(final DialectHandle dialectHandle, 
            final Collection<KamNode> sources,
            final Integer maxDepth) {
        if (sources == null) {
            throw new IllegalArgumentException("sources is null");
        }
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("sources is empty");
        }
        // maxDepth is nullable, so no null check

        checkValid();

        final InterconnectRequest req = createInterconnectRequest();
        if (dialectHandle != null) {
            req.setDialect(dialectHandle);
        }
        req.getSources().addAll(sources);
        req.setMaxDepth(maxDepth);

        final InterconnectResponse res = webAPI.interconnect(req);
        return res.getPaths();
    }

    /**
     * Checks for a valid connection and errors out if not.
     *
     * @throws RuntimeException Thrown to fail the existing request
     */
    protected void checkValid() {
        if (webAPI == null || !clientConnector.isValid()) {
            // attempt to reconfigure to see if WSDL is now up
            clientConnector.reconfigure();
        }
        
        // if reconfigure fails
        if (webAPI == null || !clientConnector.isValid()) {
            // FIXME move this message dialog out of the Kam service, UI
            // has no place here
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "Error connecting to the BELFramework Web Services.\n" +
                            "Please check the BELFramework Web Services Configuration.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            // FIXME make this in a checked(?) exception so that layers using
            // the kam service can create their own UI errors
            throw new RuntimeException("Connection error.");
        }
    }

    /**
     * Calls to the {@link TermLabelFormatter label formatter} to format the
     * {@link String BEL term label} based on the user's configuration.
     *
     * @param term
     */
    private void formatLabel(final BelTerm term) {
        String label = TermLabelFormatter.format(term.getLabel());
        term.setLabel(label);
    }
}
