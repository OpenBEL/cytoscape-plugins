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
import static com.selventa.belframework.ws.client.ObjectFactory.createGetSupportingEvidenceRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createGetSupportingTermsRequest;
import static com.selventa.belframework.ws.client.ObjectFactory.createLoadKamRequest;

import java.util.List;

import javax.swing.JOptionPane;

import com.selventa.belframework.ws.client.BelStatement;
import com.selventa.belframework.ws.client.BelTerm;
import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.EdgeFilter;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsRequest;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsResponse;
import com.selventa.belframework.ws.client.FunctionType;
import com.selventa.belframework.ws.client.FunctionTypeFilterCriteria;
import com.selventa.belframework.ws.client.GetAdjacentKamEdgesRequest;
import com.selventa.belframework.ws.client.GetAdjacentKamEdgesResponse;
import com.selventa.belframework.ws.client.GetCatalogRequest;
import com.selventa.belframework.ws.client.GetCatalogResponse;
import com.selventa.belframework.ws.client.GetSupportingEvidenceRequest;
import com.selventa.belframework.ws.client.GetSupportingEvidenceResponse;
import com.selventa.belframework.ws.client.GetSupportingTermsRequest;
import com.selventa.belframework.ws.client.GetSupportingTermsResponse;
import com.selventa.belframework.ws.client.KAMLoadStatus;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamHandle;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.LoadKamRequest;
import com.selventa.belframework.ws.client.LoadKamResponse;
import com.selventa.belframework.ws.client.NodeFilter;
import com.selventa.belframework.ws.client.WebAPI;

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
    protected WebAPI ws;
    private static final int SLEEP_TIME_MS = 1000; 
    private ClientConnector wsClient;

    /**
     * Retrieves the webservice client from the
     * {@link WebServiceClientManager cytoscape webservice manager} and holds
     * the client stub.
     */
    DefaultKAMService() {
        wsClient = (ClientConnector) WebServiceClientManager
                .getClient("belframework");
        if (wsClient == null) {
            return;
        }

        ws = wsClient.getClientStub();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<Kam> getCatalog() {
        checkValid();

        final GetCatalogRequest req = createGetCatalogRequest();

        final GetCatalogResponse res = ws.getCatalog(req);
        return res.getKams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KamHandle loadKam(Kam kam) {
        if (kam == null || kam.getName() == null) {
            throw new IllegalArgumentException("kam parameter is invalid");
        }

        checkValid();

        final LoadKamRequest req = createLoadKamRequest();
        req.setKam(kam);

        LoadKamResponse res = ws.loadKam(req);
        while (res.getLoadStatus() == KAMLoadStatus.IN_PROCESS) {
            // sleep and then retry
            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                // no-op
            }

            res = ws.loadKam(req);
        }

        if (res.getLoadStatus() == KAMLoadStatus.FAILED) {
            return null;
        }

        return res.getHandle();
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

        final GetSupportingTermsResponse res = ws.getSupportingTerms(req);

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
                ws.getSupportingEvidence(req);
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
            final FunctionType function) {
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
        req.getPatterns().add(".*");

        final NodeFilter nf = new NodeFilter();
        final FunctionTypeFilterCriteria ftfc =
                new FunctionTypeFilterCriteria();
        ftfc.setIsInclude(true);
        ftfc.getValueSet().add(function);
        nf.getFunctionTypeCriteria().add(ftfc);
        req.setFilter(nf);

        final FindKamNodesByPatternsResponse res = ws
                .findKamNodesByPatterns(req);
        return res.getKamNodes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<KamNode> findKamNodesByPatterns(final KamHandle handle,
            final String regex, final NodeFilter nf) {
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

        req.getPatterns().add(regex);
        if (nf != null) {
            req.setFilter(nf);
        }

        final FindKamNodesByPatternsResponse res = ws
                .findKamNodesByPatterns(req);
        return res.getKamNodes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<KamEdge> getAdjacentKamEdges(final KamNode node,
            final EdgeDirectionType direction, final EdgeFilter ef) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }

        if (direction == null) {
            throw new IllegalArgumentException("direction is null");
        }

        checkValid();

        final GetAdjacentKamEdgesRequest req =
                createGetAdjacentKamEdgesRequest();
        req.setKamNode(node);
        req.setDirection(direction);

        if (ef != null) {
            req.setFilter(ef);
        }

        final GetAdjacentKamEdgesResponse res = ws.getAdjacentKamEdges(req);
        return res.getKamEdges();
    }

    /**
     * Checks for a valid connection and errors out if not.
     *
     * @throws RuntimeException Thrown to fail the existing request
     */
    protected void checkValid() {
        if (!wsClient.isValid() || ws == null) {
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "Error connecting to the BELFramework Web Services.\n" +
                            "Please check the BELFramework Web Services Configuration.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
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
