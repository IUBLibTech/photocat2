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
package edu.indiana.dlib.search.srw.terminology;

import java.util.Iterator;

import edu.indiana.dlib.search.srw.SRUScanResponseParser.ScanTerm;

public class Test {

    /**
     * A test program that illustrates the functionality 
     * of all listed ThesaurusAdapter implementations.
     * 
     * This program requires at least 3 arguments:
     * a term name to look up followed by an SRU base URL 
     * indicating the server on which to look up the term
     * followed by a list of full class names that implement
     * ThesaurusAdapter against which to perform the
     * searches.
     * 
     * Example arguments:
     *   "goats" 
     *   "http://tspilot.oclc.org/lctgm/" 
     *   "edu.indiana.dlib.search.srw.terminology.OCLCResearchTSSRWAdapter" 
     *   "http://localhost:8080/SRW/search/tgm1" 
     *   "edu.indiana.dlib.search.srw.terminology.ZThesSRWAdapter"
     */
    public static void main(String args[]) throws Exception {
        if (args.length < 3 || args.length % 2 != 1) {
            System.out.println("Usage: ZThesSRWAdapter [term name] ([sru server] [class name])... ");
            return;
        } else {
            for (int i = 1; i < args.length; i += 2) {
                long start = System.currentTimeMillis();
                ThesaurusAdapter adapter = (ThesaurusAdapter) Class.forName(args[i + 1]).newInstance();
                adapter.setProperty("sruBaseUrl", args[i]);
                String term = args[0];
                System.out.println("Searching with " + adapter.getClass().getSimpleName());
                if (adapter.exists(term)) {
                    System.out.println("Term, \"" + term + "\" exists in thesaurus.");
                    System.out.println();
                    
                    System.out.println("  Broader Terms:");
                    Iterator<String> btIt = adapter.getBroaderTermNames(term);
                    while (btIt.hasNext()) {
                        System.out.println("    " + btIt.next());
                    }
                    
                    System.out.println();
                    System.out.println("  Narrower Terms:");
                    Iterator<String> ntIt = adapter.getNarrowerTermNames(term);
                    while (ntIt.hasNext()) {
                        System.out.println("    " + ntIt.next());
                    }
                    
                    System.out.println();
                    System.out.println("  Related Terms:");
                    Iterator<String> rtIt = adapter.getRelatedTermNames(term);
                    while (rtIt.hasNext()) {
                        System.out.println("    " + rtIt.next());
                    }
                    System.out.println();
                    System.out.println("  Use For Terms:");
                    Iterator<String> ufIt = adapter.getUseForTermNames(term);
                    while (ufIt.hasNext()) {
                        System.out.println("    " + ufIt.next());
                    }
                    
                    System.out.println();
                    System.out.println("  Nearby Terms:");
                    for (ScanTerm scanTerm : adapter.getNearbyTerms(term, 5, 5)) {
                        if (scanTerm.equals(term)) {
                            System.out.println("    [" + scanTerm.getValue() + "]");
                        } else {
                            System.out.println("    " + scanTerm.getValue());
                        }
                    }
                } else {
                    System.out.println("Term, \"" + term + "\" does not exist in thesaurus.");
                    System.out.println();
                    System.out.println("  Nearby Terms:");
                    for (ScanTerm scanTerm : adapter.getNearbyTerms(term, 5, 5)) {
                        System.out.println("    " + scanTerm.getValue());
                    }
                }
                long end = System.currentTimeMillis();
                System.out.println("Processing time: " + (end - start) + "ms.");
            }
        }
    }
    
}
