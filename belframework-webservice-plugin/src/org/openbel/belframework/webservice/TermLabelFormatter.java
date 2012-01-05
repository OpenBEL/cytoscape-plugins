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

/**
 * {@link TermLabelFormatter} formats {@link String BEL term labels} using
 * either short or long form BEL.  This settings is configurable from the
 * <em>BELFramework Configuration</em> option.
 *
 * <p>
 * Short-form: A protein abundance is shown as {@code p(...)}.
 * </p>
 *
 * <p>
 * Long-form: A protein abundance is shown as {@code proteinAbundance(...)}.
 * </p>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class TermLabelFormatter {

    /**
     * Formats the {@link String belExpression} by applying the formatting
     * option configured by the user.
     *
     * @param belExpression the {@link String BEL term}
     * @return the formatted {@link String BEL term}
     */
    public static String format(String belExpression) {
        if (!Configuration.getInstance().getShortBelForm()
                || belExpression == null) {
            return belExpression;
        }

        // shorten bel term's label in place
        for (final Replacement fmt : Replacement.values()) {
            belExpression = belExpression.replace(fmt.longFunction,
                    fmt.shortFunction);
        }

        return belExpression;
    }

    /**
     * Defines {@link String} replacements to convert to short-form.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private enum Replacement {
        ABUNDANCE("abundance(", "a("),
        BIOLOGICAL_PROCESS("biologicalProcess(", "bp("),
        CATALYTIC_ACTIVITY("catalyticActivity(", "cat("),
        CELL_SECRETION("cellSecretion(", "sec("),
        CELL_SURFACE_EXPRESSION("cellSurfaceExpression(", "surf("),
        CHAPERONE_ACTIVITY("chaperoneActivity(", "chap("),
        COMPLEX_ABUNDANCE("complexAbundance(", "complex("),
        COMPOSITE_ABUNDANCE("compositeAbundance(", "composite("),
        DEGRADATION("degradation(", "deg("),
        GENE_ABUNDANCE("geneAbundance(", "g("),
        GTP_BOUND_ACTIVITY("gtpBoundActivity(", "gtp("),
        KINASE_ACTIVITY("kinaseActivity(", "kin("),
        MICRORNA_ABUNDANCE("microRNAAbundance(", "m("),
        MOLECULAR_ACTIVITY("molecularActivity(", "act("),
        PATHOLOGY("pathology(", "path("),
        PEPTIDASE_ACTIVITY("peptidaseActivity(", "pep("),
        PHOSPHATASE_ACTIVITY("phosphataseActivity(", "phos("),
        PROTEIN_ABUNDANCE("proteinAbundance(", "p("),
        PROTEIN_ABUNDANCE_MOD("proteinModification(", "pmod("),
        PROTEIN_ABUNDANCE_SUB("substitution(", "sub("),
        PROTEIN_ABUNDANCE_TRUNC("truncation(", "trunc("),
        REACTION("reaction(", "rxn("),
        RIBOSYLATION_ACTIVITY("ribosylationActivity(", "ribo("),
        RNA_ABUNDANCE("rnaAbundance(", "r("),
        TRANSCRIPTIONAL_ACTIVITY("transcriptionalActivity(", "tscript("),
        TRANSLOCATION("translocation(", "tloc("),
        TRANSPORT_ACTIVITY("transportActivity(", "tport(");

        private String longFunction;
        private String shortFunction;

        private Replacement(final String longFunction,
                final String shortFunction) {
            this.longFunction = longFunction;
            this.shortFunction = shortFunction;
        }
    }
}
