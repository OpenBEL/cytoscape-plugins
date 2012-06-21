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

import javax.swing.JOptionPane;

import org.openbel.framework.ws.model.BelStatement;
import org.openbel.framework.ws.model.BelTerm;
import org.openbel.framework.ws.model.DialectHandle;
import org.openbel.framework.ws.model.EdgeDirectionType;
import org.openbel.framework.ws.model.EdgeFilter;
import org.openbel.framework.ws.model.FindKamNodesByNamespaceValuesRequest;
import org.openbel.framework.ws.model.FindKamNodesByNamespaceValuesResponse;
import org.openbel.framework.ws.model.FindKamNodesByPatternsRequest;
import org.openbel.framework.ws.model.FindKamNodesByPatternsResponse;
import org.openbel.framework.ws.model.FindNamespaceValuesRequest;
import org.openbel.framework.ws.model.FindNamespaceValuesResponse;
import org.openbel.framework.ws.model.FunctionType;
import org.openbel.framework.ws.model.FunctionTypeFilterCriteria;
import org.openbel.framework.ws.model.GetAdjacentKamEdgesRequest;
import org.openbel.framework.ws.model.GetAdjacentKamEdgesResponse;
import org.openbel.framework.ws.model.GetAllNamespacesRequest;
import org.openbel.framework.ws.model.GetAllNamespacesResponse;
import org.openbel.framework.ws.model.GetCatalogRequest;
import org.openbel.framework.ws.model.GetCatalogResponse;
import org.openbel.framework.ws.model.GetDefaultDialectRequest;
import org.openbel.framework.ws.model.GetDefaultDialectResponse;
import org.openbel.framework.ws.model.GetSupportingEvidenceRequest;
import org.openbel.framework.ws.model.GetSupportingEvidenceResponse;
import org.openbel.framework.ws.model.GetSupportingTermsRequest;
import org.openbel.framework.ws.model.GetSupportingTermsResponse;
import org.openbel.framework.ws.model.InterconnectRequest;
import org.openbel.framework.ws.model.InterconnectResponse;
import org.openbel.framework.ws.model.Kam;
import org.openbel.framework.ws.model.KamEdge;
import org.openbel.framework.ws.model.KamHandle;
import org.openbel.framework.ws.model.KamNode;
import org.openbel.framework.ws.model.LoadKamRequest;
import org.openbel.framework.ws.model.LoadKamResponse;
import org.openbel.framework.ws.model.Namespace;
import org.openbel.framework.ws.model.NamespaceDescriptor;
import org.openbel.framework.ws.model.NamespaceValue;
import org.openbel.framework.ws.model.NodeFilter;
import org.openbel.framework.ws.model.ObjectFactory;
import org.openbel.framework.ws.model.SimplePath;
import org.openbel.framework.ws.model.WebAPI;

import cytoscape.Cytoscape;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.logger.CyLogger;

/**
 * {@link DefaultKamService} implements an API wrapper around the {@link WebAPI
 * BEL Framework Web API}. This lightweight class reuses the same webservice
 * stub instance obtained from the {@link WebServiceClientManager cytoscape
 * webservice manager}.
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
class DefaultKamService implements KamService {

    private static final CyLogger log = CyLogger
            .getLogger(DefaultKamService.class);
    private static final ObjectFactory OBJECT_FACTORY = ObjectFactorySingleton
            .getInstance();

    protected WebAPI webAPI;
    private ClientConnector clientConnector;

    /**
     * Retrieves the webservice client from the
     * {@link WebServiceClientManager cytoscape webservice manager} and holds
     * the client stub.
     */
    DefaultKamService() {
        reloadClientConnector();
    }
    
    /**
     * {@inheritDoc}
     */
    public void reloadClientConnector() {
        clientConnector = (ClientConnector) WebServiceClientManager
                .getClient("belframework");
        if (clientConnector == null) {
            log.warn("Unable to resolve client connector");
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

        final FindNamespaceValuesRequest req = OBJECT_FACTORY
                .createFindNamespaceValuesRequest();
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

        final FindKamNodesByNamespaceValuesRequest req = OBJECT_FACTORY
                .createFindKamNodesByNamespaceValuesRequest();
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
        
        final GetAllNamespacesRequest req = OBJECT_FACTORY
                .createGetAllNamespacesRequest();
        final GetAllNamespacesResponse res = webAPI.getAllNamespaces(req);
        return res.getNamespaceDescriptors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<Kam> getCatalog() {
        checkValid();

        final GetCatalogRequest req = OBJECT_FACTORY.createGetCatalogRequest();

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

        final LoadKamRequest req = OBJECT_FACTORY.createLoadKamRequest();
        req.setKam(kam);
        return webAPI.loadKam(req);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialectHandle getDefaultDialect(final KamHandle kamHandle) {
        if (kamHandle == null) {
            throw new IllegalArgumentException("kam handle is null");
        }

        checkValid();

        final GetDefaultDialectRequest req = OBJECT_FACTORY
                .createGetDefaultDialectRequest();
        req.setKam(kamHandle);
        final GetDefaultDialectResponse res = webAPI.getDefaultDialect(req);
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
                OBJECT_FACTORY.createGetSupportingTermsRequest();
        req.setKamNode(node);

        final GetSupportingTermsResponse res = webAPI.getSupportingTerms(req);
        return res.getTerms();
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
                OBJECT_FACTORY.createGetSupportingEvidenceRequest();
        req.setKamEdge(edge);

        final GetSupportingEvidenceResponse res =
                webAPI.getSupportingEvidence(req);
        final List<BelStatement> stmts = res.getStatements();
        for (final BelStatement stmt : stmts) {
            final BelTerm subject = stmt.getSubjectTerm();

            subject.setLabel(subject.getLabel());

            final BelTerm objectTerm = stmt.getObjectTerm();
            final BelStatement objectStmt = stmt.getObjectStatement();
            if (objectTerm != null) {
                objectTerm.setLabel(objectTerm.getLabel());
            } else if (objectStmt != null) {
                final BelTerm nestedSub = objectStmt.getSubjectTerm();
                nestedSub.setLabel(nestedSub.getLabel());

                final BelTerm nestedObj = objectStmt.getObjectTerm();
                nestedObj.setLabel(nestedObj.getLabel());
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
                OBJECT_FACTORY.createFindKamNodesByPatternsRequest();
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
                OBJECT_FACTORY.createFindKamNodesByPatternsRequest();
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
                OBJECT_FACTORY.createGetAdjacentKamEdgesRequest();
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

        final InterconnectRequest req = OBJECT_FACTORY
                .createInterconnectRequest();
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
}
