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
package edu.indiana.dlib.catalog.search.structured.constraints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.structured.AbstractSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;

public abstract class SerializableSearchConstraint extends AbstractSearchConstraint {

    private Logger LOGGER = Logger.getLogger(SerializableSearchConstraint.class);
            
    private static String delimiter = ":";
    
    public static String encodeParts(String[] values) {
        StringBuffer sb = new StringBuffer();
        for (String value : values) {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            String encodedText = new String(Base64.encodeBase64(value.getBytes()));
            if (encodedText.indexOf(delimiter) != -1) {
                throw new RuntimeException("Invalid delimiter: \"" + delimiter + "\"");
            }
            sb.append(encodedText);
            //sb.append(value.replace(delimiter, ""));
            //sb.append(value.replaceAll(new String(delimiter + ""), delimiter + delimiter));
        }
        return sb.toString();
    }
    
    public static List<String> decodeParts(String serializedVersion) {
        List<String> results = new ArrayList<String>();
        for (String encodedPart : serializedVersion.split(delimiter)) {
            results.add(new String(Base64.decodeBase64(encodedPart.getBytes())));
        }
        return results;
        //return Arrays.asList(serializedVersion.split(delimiter));
    }
    
    public static String getDelimiter() {
        return delimiter;
    }

    public static String serialize(SearchConstraint searchConstraint, SearchManager sm) {
        if (searchConstraint instanceof CollectionSearchConstraint) {
            CollectionSearchConstraint csc = (CollectionSearchConstraint) searchConstraint;
            return encodeParts(new String[] { "c", csc.getCollectionId(), csc.getCollectionName() });
        } else if (searchConstraint instanceof UserQuerySearchConstraint) {
            UserQuerySearchConstraint uqsc = (UserQuerySearchConstraint) searchConstraint;
            return encodeParts(new String[] { "uq", uqsc.getUserQuery() });
        } else if (searchConstraint instanceof FieldPartValueSearchConstraint) {
            FieldPartValueSearchConstraint sc = (FieldPartValueSearchConstraint) searchConstraint;
            return encodeParts(new String[] {"fpvl", sc.getFieldType(), sc.getPartName(), sc.getValue(), sc.getDisplayLabel() });
        } else if (searchConstraint instanceof OrSearchConstraintGroup) {
            OrSearchConstraintGroup oscg = (OrSearchConstraintGroup) searchConstraint;
            ArrayList<String> parts = new ArrayList<String>();
            parts.add("or");
            parts.add(oscg.getDisplay() == null ? "" : oscg.getDisplay());
            parts.add(oscg.isImplicit() ? "t" : "f");
            for (SearchConstraint sub : oscg.getOredConstraints()) {
                parts.add(serialize(sub, sm));
            }
            return encodeParts(parts.toArray(new String[0]));
        } else if (searchConstraint instanceof AndSearchConstraintGroup) {
            AndSearchConstraintGroup ascg = (AndSearchConstraintGroup) searchConstraint;
            ArrayList<String> parts = new ArrayList<String>();
            parts.add("and");
            parts.add(ascg.getDisplay() == null ? "" : ascg.getDisplay());
            parts.add(ascg.isImplicit() ? "t" : "f");
            for (SearchConstraint sub : ascg.getAndedConstraints()) {
                parts.add(serialize(sub, sm));
            }
            return encodeParts(parts.toArray(new String[0]));
        } else if (searchConstraint instanceof QueryClauseSearchConstraint) {
            QueryClauseSearchConstraint c = (QueryClauseSearchConstraint) searchConstraint;
            return encodeParts(new String[] { "q", c.getDisplay(), c.getQueryClause() });
        } else {
            throw new IllegalArgumentException("Unable to serialize!");
        }
    }
    
    public static SerializableSearchConstraint deserialize(String serializedVersion) {
        List<String> parts = decodeParts(serializedVersion);
        String type = parts.get(0);
        //System.out.println(listToCSV(parts));
        if (type.equals("c")) {
            return new CollectionSearchConstraint(parts.get(1), parts.get(2));
        } else if (type.equals("uq")) {
            return new UserQuerySearchConstraint(parts.get(1));
        } else if (type.equals("fpvl")) {
            return new FieldPartValueSearchConstraint(parts.get(1), parts.get(2), parts.get(3), parts.get(4));
        } else if (type.equals("or")) {
            List<SerializableSearchConstraint> constraints = new ArrayList<SerializableSearchConstraint>();
            String display = parts.get(1);
            boolean implicit = parts.get(2).equals("t");
            for (int i = 3; i < parts.size(); i ++) {
                constraints.add(deserialize(parts.get(i)));
            }
            return new OrSearchConstraintGroup(display, constraints, implicit);
        } else if (type.equals("and")) {
            List<SerializableSearchConstraint> constraints = new ArrayList<SerializableSearchConstraint>();
            String display = parts.get(1);
            boolean implicit = parts.get(2).equals("t");
            for (int i = 3; i < parts.size(); i ++) {
                constraints.add(deserialize(parts.get(i)));
            }
            return new AndSearchConstraintGroup(display, constraints, implicit);
        } else if (type.equals("q")) {
            return new QueryClauseSearchConstraint(parts.get(1), parts.get(2));
        } else {
            throw new IllegalArgumentException("Unknown type (" + type + ")");
        }
    }
    
    public static String listToCSV(List<String> list) {
        StringBuffer sb = new StringBuffer();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(s);
        }
        return sb.toString();
    }
    
    /**
     * Returns true if the two strings are equal or both null.
     */
    protected static boolean equal(String one, String two) {
        if (one == null && two == null) {
            return true;
        } else if (one == null) {
            return false;
        } else {
            return one.equals(two);
        }
    }
    
    /**
     * Determines if a particular ItemMetadata matches the given SerializableSearchConstriant.
     */
    public static boolean doesRecordMatch(SerializableSearchConstraint c, ItemMetadata im) {
        if (c instanceof CollectionSearchConstraint) {
            return im.getCollectionId().equals(((CollectionSearchConstraint) c).getCollectionId());
        } else if (c instanceof FieldPartValueSearchConstraint) {
            FieldPartValueSearchConstraint fpvsc = (FieldPartValueSearchConstraint) c;
            FieldData data = im.getFieldData(fpvsc.getFieldType());
            return (data != null && data.getPartValues(fpvsc.getPartName()).contains(fpvsc.getValue()));
        } else if (c instanceof AndSearchConstraintGroup) {
            AndSearchConstraintGroup and = (AndSearchConstraintGroup) c;
            for (SerializableSearchConstraint child : and.getAndedConstraints()) {
                if (!doesRecordMatch(child, im)) {
                    return false;
                }
            }
            return true;
        } else if (c instanceof OrSearchConstraintGroup) {
            OrSearchConstraintGroup or = (OrSearchConstraintGroup) c;
            for (SerializableSearchConstraint child : or.getOredConstraints()) {
                if (doesRecordMatch(child, im)) {
                    return true;
                }
            }
            return false;
        } else {
            throw new IllegalArgumentException("Unsupported search constraint type: " + c.getClass().getName());
        }
    }
}
