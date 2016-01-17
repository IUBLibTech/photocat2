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
package edu.indiana.dlib.catalog.config.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that encapsulates the configuration for collections with
 * some sort of record creation and file-submission workflow.
 */
public class FileSubmissionWorkflowConfiguration {
    
    private String collectionId;
    
	private File dropboxDir;
	
	private File ingestDir;
	
	private URL idGenerationUrl;
	
	private String idPattern;
	
	private File idNumberFile;
	
	private String idPrefix;
	
	private NumberFormat idNumberFormat;
	
	public FileSubmissionWorkflowConfiguration(Properties p) throws MalformedURLException {
	    collectionId = p.getProperty("collectionId");
	    
		dropboxDir = new File(p.getProperty("dropboxDir"));
		ingestDir = new File(p.getProperty("ingestDir"));
		
		if (p.containsKey("idGenerationUrl")) {
		    idGenerationUrl = new URL(p.getProperty("idGenerationUrl"));
		}
		
		idPattern = p.getProperty("idPattern");

		if (p.containsKey("idNumberFile")) {
		    idNumberFile = new File(p.getProperty("idNumberFile"));
		}
		idPrefix = p.getProperty("idPrefix");
		if (p.containsKey("idDigits")) { 
		    idNumberFormat = new DecimalFormat(p.getProperty("idDigits"));
		}
	}
	
	public File getDropboxDir() {
	    return dropboxDir;
	}
	
	public File getIngestDir() {
	    return ingestDir;
	}
	
	/**
	 * Parses the id pattern (if available) to determine if it has any
	 * required argument names.
	 * @return a collection of strings (possibly empty, but never null)
	 * representing the required argument names.
	 */
	public Collection<String> getRequiredArguments() {
	    if (idPattern != null) {
	        Matcher m = Pattern.compile("^[^\\{]*\\{([^\\}]*)\\}[^\\{]*$").matcher(idPattern);
	        if (m.matches()) {
	            String[] namePatternPair = m.group(1).split(",");
	            return Collections.singleton(namePatternPair[0]);
	        }
	        return Collections.emptyList();
	    } else {
	        return Collections.emptyList();
	    }
	}
	
	/**
	 * Gets the next ID from the configured source.
     */
    public String getId(Map<String, String> args) throws IOException {
        if (idGenerationUrl != null) {
            // Case 1: The id comes from an external service that allows
            //         it to be parsed from the response of a simple HTTP
            //         request.
            URLConnection conn = idGenerationUrl.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            InputStream is = conn.getInputStream();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String id = reader.readLine();
                return "http://purl.dlib.indiana.edu/iudl/" + collectionId + "/" + id;
            } finally {
                is.close();
            }
        } else if (idPattern != null) {
            // Case 2: The id comes from a pattern which may have a substitution 
            //         parameter that was provided.  Typically this is where the
            //         user provides the numeric portion of the id.
            Matcher m = Pattern.compile("^[^\\{]*(\\{([^\\}]*)\\})[^\\{]*$").matcher(idPattern);
            if (m.matches()) {
                String[] namePatternPair = m.group(2).split(",");
                NumberFormat format = new DecimalFormat(namePatternPair[1]);
                return idPattern.replace(m.group(1), format.format(Integer.parseInt(args.get(namePatternPair[0]))));
            }
            throw new IllegalStateException("Invalid id pattern specified in configuration: \"" + idPattern + "\"");
        } else if (idNumberFile != null && idNumberFormat != null && idPrefix != null) {
            // Case 3: The id is derived from a number that is stored on disk
            //         and incremented each time it is read.
            int nextNumber = -1;
            InputStream is = new FileInputStream(idNumberFile);
            String result = null;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                nextNumber = Integer.parseInt(reader.readLine());
                result = idPrefix.trim().concat(idNumberFormat.format(nextNumber));
                reader.close();
            } finally {
                is.close();
            }
            OutputStream os = new FileOutputStream(idNumberFile);
            try {
                PrintWriter writer = new PrintWriter(os);
                writer.println(++nextNumber);
                writer.close();
            } finally {
                os.close();
            }
            return result;
        } else {
            throw new IllegalStateException("Configuration must contain either \"idGenerationUrl\" or \"idPattern\" or (\"idNumberFile\", \"idDigits\" and \"idPrefix\")!");
        }
    }
	
	
}
