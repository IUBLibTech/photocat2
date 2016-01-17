/**
 * Copyright 2015 Trustees of Indiana University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE TRUSTEES OF INDIANA UNIVERSITY ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE TRUSTEES OF INDIANA UNIVERSITY OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of the Trustees of Indiana University.
 */
package ORG.oclc.os.SRW.Lucene;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.shared.SortKeyReader;
import ORG.oclc.os.SRW.shared.SortKeyReader.SortKey;
import edu.indiana.dlib.search.indexing.FieldConfiguration;

public class LuceneHelper {

    private static Log LOG = LogFactory.getLog(LuceneHelper.class);

    private static final Pattern TERM_TOKEN_PATTERN = Pattern.compile("(\\S+)");
    
    /**
     * Escapes all special characters in the query except: * and ?
     */
    public static String escapeLuceneQueryField(String queryField) {
        return QueryParser.escape(queryField).replace("\\*", "*").replace("\\?", "?");
    }
    
    public static String wrapInQuotesIfContainsSpace(String string) {
        if (string.indexOf(' ') != -1) {
            return "\"" + escapeLuceneQueryField(string) + "\"";
        } else {
            return escapeLuceneQueryField(string);
        }
    }
    
    /**
     * Parses the sort keys and returns a lucene Sort object
     * representing the value or throws an exception if any
     * unsupported parameters are included.
     */
    public static Sort getLuceneSort(String sortKeys, FieldConfiguration fieldConfig) {
        List<SortKey> keys = SortKeyReader.parseSortKeys(sortKeys);
        ArrayList<SortField> sortFields = new ArrayList<SortField>();
        for (int i = 0; i < keys.size(); i ++) {
            String key = keys.get(i).path;
            if (key != null && key.trim().length() > 0) {
                for (String fieldName : fieldConfig.resolveFieldName(keys.get(i).path)) {
                    if (keys.get(i).missingValue.equals(SortKeyReader.MISSING_VALUE_HIGH)) {
                        // by default lucene treats missing values as low, so we only need
                        // to intercept the case where they should be high and sort such
                        // that all present values come first
                        sortFields.add(new SortField(fieldConfig.getFieldNameIsPresent(fieldName), keys.get(i).ascending));
                        LOG.info("Sorting by: " + fieldConfig.getFieldNameIsPresent(fieldName) + " (presence field) " + (keys.get(i).ascending ? " ascending" : "descending"));
                    }
                    sortFields.add(new SortField(fieldConfig.getFieldNameSort(fieldName), !keys.get(i).ascending));
                    LOG.info("Sorting by:     " + fieldConfig.getFieldNameSort(fieldName) + (keys.get(i).ascending ? " ascending " : " descending"));
                }
            }
        }
        if (sortFields.isEmpty()) {
            LOG.info("Sorting by relevance.");
            return Sort.RELEVANCE;
        } else {
            return new Sort(sortFields.toArray(new SortField[0]));
        }
    }
    
    public static String makeLuceneQuery(CQLNode node, FieldConfiguration fc, Hashtable oldResultSets) {
        StringBuffer sb=new StringBuffer();
        makeLuceneQuery(node, sb, fc, oldResultSets);
        return sb.toString();
    }
    
    /**
     * <p>
     *   Transforms a CQL query node into a lucene query string.
     * </p>
     * <p>
     *   This method is responsible for the following:
     *   <ul>
     *     <li>
     *       Resolving aliased field names in accordance
     *       with the {@link FieldConfiguration}.  (ie,
     *       mapping cql.anywhere to every field)
     *     </li>
     *     <li>
     *       Translating relations (=, any, all, exact, etc.).
     *     </li>
     *     <li>
     *       Escaping terms and index names in a way that is
     *       appropriate for the underlying Lucene index.
     *     </li>
     *     <li>
     *       Replacing references to result sets with the 
     *       query generated for those result sets when 
     *       combination logic is needed.  (note: paging
     *       though a single listed result set is handled
     *       by the superclass in an execution path that 
     *       won't result in the invocation of this method)
     *     </li>
     *   </ul>
     * </p>
     */
    private static void makeLuceneQuery(CQLNode node, StringBuffer sb, FieldConfiguration fc, Hashtable oldResultSets) {
        LOG.debug("making lucene query for node " + node.toCQL());
        if(node instanceof CQLBooleanNode) {
            LOG.debug("it's boolean");
            CQLBooleanNode cbn=(CQLBooleanNode)node;
            sb.append("(");
            makeLuceneQuery(cbn.left, sb, fc, oldResultSets);
            if(node instanceof CQLAndNode)
                sb.append(" AND ");
            else if(node instanceof CQLNotNode)
                sb.append(" NOT ");
            else if(node instanceof CQLOrNode)
                sb.append(" OR ");
            else sb.append(" UnknownBoolean("+cbn+") ");
            makeLuceneQuery(cbn.right, sb, fc, oldResultSets);
            sb.append(")");
        } else if(node instanceof CQLTermNode) {
               LOG.debug("it's a term");
            // assume the relation has no modifiers
            CQLTermNode ctn = (CQLTermNode)node;
            String term = LuceneHelper.wrapInQuotesIfContainsSpace(ctn.getTerm());

            List<String> tokens = null;
            if (ctn.getRelation().getBase().equals("any") || ctn.getRelation().getBase().equals("all")) {
                tokens = tokenizeTerm(ctn.getTerm(), TERM_TOKEN_PATTERN);
            }

            String alias = ctn.getQualifier();
            LOG.debug("index is " + alias);
            String[] baseIndices = fc.resolveFieldName(alias);
            sb.append("(");
            boolean firstIndex = true;
            for (String index : baseIndices) {
                if (firstIndex) {
                    firstIndex = false;
                } else {
                    sb.append(" OR ");
                }
                
                // special handling for 'cql.resultSetId'
                if (index.equalsIgnoreCase("cql.resultSetId")) {
                    QueryResult resultSet = (QueryResult) oldResultSets.get(term);
                    if (resultSet == null) {
                        LOG.warn("ResultSet \"" + term + "\" is unknown!");
                        //return diagnostic(SRWDiagnostic.ResultSetDoesNotExist, resultSetID, response);
                    } else if (resultSet instanceof LuceneSearchResult) {
                        sb.append(((LuceneSearchResult) resultSet).getLuceneQueryString());
                    } else {
                        LOG.warn("ResultSet \"" + term + "\" was an unexpected class: " + resultSet.getClass().getName());
                    }
                } else if (ctn.getRelation().getBase().equals("=") || ctn.getRelation().getBase().equals("scr")) {
                    // append the term to the index string
                    if(!index.equals("")) {
                        sb.append(LuceneHelper.escapeLuceneQueryField(index)).append(":");
                    }
                    sb.append(term);
                } else if (ctn.getRelation().getBase().equals("exact")) {
                    sb.append(LuceneHelper.escapeLuceneQueryField(fc.getFieldNameExact(index)));
                    sb.append(":");
                    sb.append(term);
                } else if (ctn.getRelation().getBase().equals("any")) {
                    /*
                     * Handle the "ANY" relation:
                     *    This is equivalent to a non-exact match "=" 
                     *    for any token in the term.  
                     *    "dc.title any "moon cow"" is equivalent to
                     *    "dc.title = moon OR dc.title = cow" 
                     */
                    boolean first = true;
                    if (!tokens.isEmpty()) {
                        sb.append("(");
                    }
                    for (String token : tokens) {
                        if (!first) {
                            sb.append(" OR ");
                        } else {
                            first = false;
                        }
                        sb.append(LuceneHelper.escapeLuceneQueryField(index));
                        sb.append(":");
                        sb.append(LuceneHelper.escapeLuceneQueryField(token));
                    }
                    if (!tokens.isEmpty()) {
                        sb.append(")");
                    }

                } else if (ctn.getRelation().getBase().equals("all")) {
                    /*
                     * Handle the "ALL" relation:
                     *    This is equivalent to a non-exact match "=" 
                     *    for each token in the term.  
                     *    "dc.title any "moon cow"" is equivalent to
                     *    "dc.title = moon AND dc.title = cow" 
                     */
                    boolean first = true;
                    if (!tokens.isEmpty()) {
                        sb.append("(");
                    }
                    for (String token : tokens) {
                        if (!first) {
                            sb.append(" AND ");
                        } else {
                            first = false;
                        }
                        sb.append(LuceneHelper.escapeLuceneQueryField(index));
                        sb.append(":");
                        sb.append(LuceneHelper.escapeLuceneQueryField(token));
                    }
                    if (!tokens.isEmpty()) {
                        sb.append(")");
                    }

                } else if (ctn.getRelation().getBase().equals("cql.within")) {
                    String modifiers[] = ctn.getTerm().split(" ");
                    if (modifiers.length == 2) {
                        if(!index.equals("")) {
                            sb.append(LuceneHelper.escapeLuceneQueryField(index)).append(":");
                        }
                        // append the term to the index string
                        sb.append("[" + LuceneHelper.wrapInQuotesIfContainsSpace(modifiers[0]) + " TO " + LuceneHelper.wrapInQuotesIfContainsSpace(modifiers[1]) + "]");
                    }
                } else {
                    LOG.debug("it's unrecognized");
                    sb.append("Unsupported Relation: "+ctn.getRelation().getBase());
                }
            }
            sb.append(")");
        }
        else sb.append("UnknownCQLNode("+node+")");
    }
    
    private static List<String> tokenizeTerm(String term, Pattern pattern) {
        ArrayList<String> tokens = new ArrayList<String>();
        Matcher m = pattern.matcher(term);
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }
    
}
