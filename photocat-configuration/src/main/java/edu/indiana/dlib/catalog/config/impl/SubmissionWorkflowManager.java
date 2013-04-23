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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Manages loading and caching of FileSubmissionWorkflowConfigurations
 * from a single directory.  This implementation abstracts away all
 * the complexity of identifying and handling changes to the underlying
 * filesystem.  In other words, users can invoke the one public method
 * and be sure they're getting the latest version of the appropriate
 * configuration.  This class is thread-safe.
 */
public class SubmissionWorkflowManager {

	private File configDir;
	
	private Map<String, FileSubmissionWorkflowConfiguration> cache;
	
	private Map<String, Long> freshnessMap;
	
	private Map<String, File> idToFileMap;
	
	public SubmissionWorkflowManager(File configurationDirectory) {
		configDir = configurationDirectory;
		if (configDir == null) {
			throw new RuntimeException("Configuration directory must be specified!");
		} else if (!configDir.exists()) {
			throw new RuntimeException("Configuration directory \"" + configDir.getPath() + "\" does not exist!");
		} else if(!configDir.isDirectory()) {
			throw new RuntimeException("Configuration directory \"" + configDir.getPath() + "\" is not a directory!");
		} else if (!configDir.canRead()) {
			throw new RuntimeException("Configuration directory \"" + configDir.getPath() + "\" is not accessible!");
		}
		reloadCache();
	}
	
	/**
	 * Gets the current version of the FileSubmissionWorkflowConfiguration for
	 * the collection indicated by the given collectionId or null if that
	 * workflow isn't configured for that collection.
	 */
	public synchronized FileSubmissionWorkflowConfiguration getConfiguration(String collectionId) {
		FileSubmissionWorkflowConfiguration config = cache.get(collectionId);
		if (config != null) {
			// make sure it's current
			File file = idToFileMap.get(collectionId);
			if (file.lastModified() > freshnessMap.get(collectionId)) {
				config = loadFileIntoCache(file);
			}
			return config;
		} else {
			// maybe there's a new file
			for (File file : configDir.listFiles()) {
				if (file.getName().endsWith(".properties")) {
					if (!idToFileMap.values().contains(file)) {
						loadFileIntoCache(file);
					}
				}
			}
			return cache.get(collectionId);
		}
	}
	
	private synchronized void reloadCache() {
		cache = new HashMap<String, FileSubmissionWorkflowConfiguration>();
		freshnessMap = new HashMap<String, Long>();
		idToFileMap = new HashMap<String, File>();
		if (configDir != null && configDir.exists() && configDir.isDirectory()) {
			for (File file : configDir.listFiles()) {
				if (file.getName().endsWith(".properties")) {
					loadFileIntoCache(file);
				}
			}
		}
	}

	private synchronized FileSubmissionWorkflowConfiguration loadFileIntoCache(File propertiesFile) {
	    Logger logger = Logger.getLogger(this.getClass());
		long freshness = propertiesFile.lastModified();
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(propertiesFile));
			if (p.containsKey("collectionId")) {
				FileSubmissionWorkflowConfiguration config = new FileSubmissionWorkflowConfiguration(p);
				cache.put(p.getProperty("collectionId"), config);
				freshnessMap.put(p.getProperty("collectionId"), freshness);
				idToFileMap.put(p.getProperty("collectionId"), propertiesFile);
		        logger.debug("Loaded file submission configuration for \"" + p.getProperty("collectionId") + "\".");
				return config;
			} else {
			     logger.debug("Failed to load \"" + propertiesFile.getAbsolutePath() + "\" as a file submission configuration because it had no collectionId specified.");
				return null;
			}
		} catch (FileNotFoundException ex) {
			// do nothing, no file to cache
			return null;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
