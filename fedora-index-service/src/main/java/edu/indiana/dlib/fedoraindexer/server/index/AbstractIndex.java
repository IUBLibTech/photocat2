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
package edu.indiana.dlib.fedoraindexer.server.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.Index;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;

/**
 * <p>
 *   An incomplete base {@link Index} implementation that allows for 
 *   easy subclassing to create custom {@link Index} objects.
 * </p>
 * <p>
 *   The configuration of this class is provided at construction time by
 *   means of a Properties file.  The follow properties are recognized by
 *   default, though subclasses may add to this list.
 *   <ul>
 *     <li>name - required</li>
 *     <li>
 *       indexImmediately - an optional value that can be set to
 *       "true" or "false" to indicate whether this index should 
 *       be updated immediately or whether an asynchronous update
 *       is tolerable.
 *     </li>
 *     <li>
 *       maximumUpdateProcrastination - an optional value that can
 *       be set to the highest number of milliseconds that is still
 *       considered an acceptable delay before the index is updated.
 *     </li>
 *     <li>
 *       maxRetries - an optional value that can be set to the number
 *       of times to attempt to index an object before giving up due
 *       to exceptions being thrown.  The default value is zero, 
 *       indicating that no subsequent attempts will be made after a
 *       failure.
 *     </li>
 *     <li>
 *       PID.filterExpression, FULL_ITEM_ID.filterExpression, TITLE.filterExpression,
 *       COLLECTION_ID.filterExpression, CONTENT_MODEL.filterExpression, 
 *       CREATION_DATE.filterExpression and LAST_MODIFICATION_DATE.filterExpression: 
 *       (optional) java regular expressions representing required values for the
 *       basic administrative field (listed in the prefix) in order for objects to
 *       be included in this index.  If no filterExpressions are supplied, no
 *       filtering will occur and all objects will be included.  Otherwise, each 
 *       expression will be evaluated against an incoming object and used to further
 *       restrict the objects that may be included in this index. 
 *     </li>
 *   </ul>
 * </p>
 */
public abstract class AbstractIndex implements Index {

    public static Logger LOGGER = Logger.getLogger(Index.class);
    
    private String name;

    protected String indexFilename;
    
    private Map<FedoraObjectAdministrativeMetadata.Field, Pattern> fieldToPatternMap;
    
    public AbstractIndex(Properties config) throws IndexInitializationException {
        // get name
        this.name = config.getProperty("name");
        
        // set up the filter patterns
        this.fieldToPatternMap = new HashMap<FedoraObjectAdministrativeMetadata.Field, Pattern>();
        for (FedoraObjectAdministrativeMetadata.Field field : FedoraObjectAdministrativeMetadata.Field.values()) {
            String patternStr = config.getProperty(field + ".filterExpression");
            try {
                if (patternStr != null) {
                    this.fieldToPatternMap.put(field, Pattern.compile(patternStr));
                }
            } catch (PatternSyntaxException ex) {
                LOGGER.warn("Invalid regular expression specified \"" + patternStr + "\": see the Java documentation for java.util.regex.Pattern.");
            }
        }
    }
    
    /**
     * Returns the name given as the "name" property in the
     * configuration properties file that was provided to the
     * constructor.
     */
    public String getIndexName() {
        return this.name;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     *   The current implementation attempts to match the values of
     *   any provided filterExpressions against the corresponding
     *   field values.  If any mismatches are identified, this method
     *   returns null.
     * </p> 
     */
    public boolean shouldIndexObject(FedoraObjectAdministrativeMetadata objectAdminInfo) {
        for (FedoraObjectAdministrativeMetadata.Field field : this.fieldToPatternMap.keySet()) {
            Pattern p = this.fieldToPatternMap.get(field);
            if (p != null) {
                int matchCount = 0;
                for (String fieldValue : objectAdminInfo.getFieldValues(field)) {
                    if (p.matcher(fieldValue).matches()) {
                        matchCount ++;
                        LOGGER.debug(fieldValue + " matches " + p.toString());
                    } else {
                        LOGGER.debug(fieldValue + " does not match " + p.toString());
                    }
                }
                if (matchCount == 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
