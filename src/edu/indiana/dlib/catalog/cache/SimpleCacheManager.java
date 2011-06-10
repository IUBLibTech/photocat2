/**
 * Copyright 2011, Trustees of Indiana University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 *   Neither the name of Indiana University nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.indiana.dlib.catalog.cache;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.DocumentHelper;

public class SimpleCacheManager implements CacheManager {

    private Logger LOGGER = Logger.getLogger(SimpleCacheManager.class);
    
    /**
     * Constructs a cache location on disk.  If the PHOTOCAT_HOME
     * environment variable is set and the indicated baseCacheDirectory
     * does not begin with a "/" character, the cache directory will
     * be relative to the PHOTOCAT_HOME directory.
     */
    public SimpleCacheManager(String baseCacheDirectory) {
        File schemaCacheDir = null;
        String photocatHome = System.getenv("PHOTOCAT_HOME");
        if (photocatHome != null && !baseCacheDirectory.startsWith("/")) {
            File homeDir = new File(photocatHome);
            schemaCacheDir = new File(homeDir, baseCacheDirectory);
        } else {
            schemaCacheDir = new File(new File(baseCacheDirectory), "schema-cache");
        }
        try {
            schemaCacheDir.mkdirs();
            DocumentHelper.getInstance().setCacheDirectory(schemaCacheDir);
            DocumentHelper.getInstance().clearCache();
            LOGGER.info("Schema cache initialized at, \"" + schemaCacheDir + "\".");
        } catch (IOException ex) {
            LOGGER.error("Error setting DocumentHelper schema cache!", ex);
        }
    }
    
}
