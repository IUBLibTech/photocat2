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
package edu.indiana.dlib.catalog.batch.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;

/**
 * A BatchManager implementation that stored batches as simple
 * text files on an accessible file system.  Each userId for
 * which batches have been saved is created as a subdirectory
 * of the batch root directory and within that, there's a subdirectory
 * for each collection.  Within those subdirectories each batch is
 * given the name batch-[id]-utf8.txt and is formatted such that the
 * first line of text contains the batch name (UTF-8 encoded) and
 * each subsequent line is an identifier of an item stored in the
 * batch.
 * 
 * This class is thread-safe.
 */
public class FilesystemBatchManager implements BatchManager {

    private Logger LOGGER = Logger.getLogger(FilesystemBatchManager.class);
    
    private static String FILENAME_REG_EXP = "batch\\-(\\d+)-utf8.txt";
    
    private File rootDir;
    
    private Map<String, List<Batch>> userToOpenBatchesMap;
    
    public FilesystemBatchManager(String rootFilename) {
        String photocatHome = System.getenv("PHOTOCAT_HOME");
        if (photocatHome != null && !rootFilename.startsWith("/")) {
            File homeDir = new File(photocatHome);
            rootDir = new File(homeDir, rootFilename);
        } else {
            rootDir = new File(rootFilename);
        }
        rootDir.mkdirs();
        if (!rootDir.canWrite()) {
            throw new RuntimeException("Configured directory, \"" + rootDir.getAbsolutePath() + "\" does not exist or is read-only!");
        }
        userToOpenBatchesMap = new HashMap<String, List<Batch>>();
    }

    private synchronized File getBatchFile(String userId, String collectionId, int id) { 
        File userDir = new File(rootDir, userId);
        File userColDir = new File(userDir, toPathForm(collectionId));
        if (!userColDir.exists()) {
            userColDir.mkdirs();
        }
        return new File(userColDir, "batch-" + id + "-utf8.txt"); 
    }
    
    private synchronized int getNextBatchId(String userId, String collectionId) {
        for (int i = 0; ; i ++) {
            if (!getBatchFile(userId, collectionId, i).exists()) {
                return i;
            }
        }
    }
    
    private synchronized Batch loadBatch(String userId, String collectionId, int id) throws IOException {
        File userDir = new File(rootDir, userId);
        File userColDir = new File(userDir, toPathForm(collectionId));
        File batchFile = new File(userColDir, "batch-" + id + "-utf8.txt");
        return loadBatchFile(batchFile, id, userId, collectionId);
    }
    
    private synchronized Batch loadBatchFile(File batchFile, int id, String userId, String collectionId) throws IOException {
        if (batchFile.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(batchFile), "UTF-8"));
            String name = reader.readLine();
            List<String> ids = new ArrayList<String>();
            String itemId = null;
            while ((itemId = reader.readLine()) != null) {
                ids.add(itemId);
            }
            return new DefaultBatch(id, userId, collectionId, name, ids);
        } else {
            return null;
        }
    }
    
    public synchronized Batch createNewBatch(String userId, String collectionId, String name, List<String> ids) throws IOException {
        Batch newBatch = null;
        if (ids == null) {
            newBatch = new DefaultBatch(getNextBatchId(userId, collectionId), userId, collectionId, name);
        } else {
            newBatch = new DefaultBatch(getNextBatchId(userId, collectionId), userId, collectionId, name, ids);
        }
        saveBatch(userId, collectionId, newBatch);
        return newBatch;
    }
    
    public synchronized void deleteBatch(String userId, String collectionId, int batchId) {
        closeBatch(userId, collectionId, batchId);
        File batchFile = getBatchFile(userId, collectionId, batchId);
        if (batchFile.exists()) {
            if (!batchFile.delete()) {
                throw new RuntimeException("Unable to delete file, \"" + batchFile.getAbsolutePath() + "\"!");
            }
        } else {
            throw new RuntimeException("Batch file not found! (" + batchFile.getAbsolutePath() + ")!");
        }
    }

    public synchronized List<Batch> listAllBatches(String userId, String collectionId) {
        List<Batch> batches = new ArrayList<Batch>();
        File userDir = new File(rootDir, userId);
        File userColDir = new File(userDir, toPathForm(collectionId));
        if (userColDir.exists()) {
            Pattern p = Pattern.compile(FILENAME_REG_EXP);
            for (File file : userColDir.listFiles()) {
                Matcher m = p.matcher(file.getName());
                if (m.matches()) {
                    try {
                        batches.add(loadBatchFile(file, Integer.parseInt(m.group(1)), userId, collectionId));
                    } catch (NumberFormatException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return batches;
    }

    public synchronized List<Batch> listOpenBatches(String userId, String collectionId) {
        List<Batch> allOpenBatches = userToOpenBatchesMap.get(userId);
        if (allOpenBatches == null) {
            allOpenBatches = Collections.emptyList();
            return allOpenBatches;
        } else {
            List<Batch> openBatches = new ArrayList<Batch>();
            for (Batch batch : allOpenBatches) {
                if (batch.getCollectionId().equals(collectionId)) {
                    openBatches.add(batch);
                }
            }
            return openBatches;
        }
    }
    
    public synchronized boolean isBatchOpen(String userId, String collectionId, int batchId) {
        List<Batch> allOpenBatches = userToOpenBatchesMap.get(userId);
        if (allOpenBatches == null) {
            return false;
        } else {
            for (Batch openBatch : allOpenBatches) {
                if (openBatch.getId() == batchId && openBatch.getCollectionId().equals(collectionId) && openBatch.getUserId().equals(userId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public Batch fetchBatch(String userId, String collectionId, int batchId) {
        try {
            Batch batch = loadBatch(userId, collectionId, batchId);
            return batch;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public synchronized Batch openBatch(String userId, String collectionId, int batchId) {
        List<Batch> openBatches = userToOpenBatchesMap.get(userId);
        if (openBatches == null) {
            openBatches = new ArrayList<Batch>();
            userToOpenBatchesMap.put(userId, openBatches);
        }
        for (Batch batch : openBatches) {
            if (batch.getId() == batchId && batch.getCollectionId().equals(collectionId)) {
                // already open
                LOGGER.info("User " + batch.getUserId() + " reopened batch " + batch.getId() + " for collection " + batch.getCollectionId() + ".");
                return batch;
            }
        }
        try {
            Batch batch = loadBatch(userId, collectionId, batchId);
            openBatches.add(batch);
            LOGGER.info("User " + batch.getUserId() + " opened batch " + batch.getId() + " for collection " + batch.getCollectionId() + ".");
            return batch;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void closeBatch(String userId, String collectionId, int batchId) {
        List<Batch> openBatches = userToOpenBatchesMap.get(userId);
        if (openBatches == null) {
            return;
        }
        for (int i = 0; i < openBatches.size(); i ++) {
            Batch openBatch = openBatches.get(i);
            if (openBatch.getId() == batchId && openBatch.getCollectionId().equals(collectionId)) {
                openBatches.remove(i);
                LOGGER.info("User " + openBatch.getUserId() + " closed batch " + openBatch.getId() + " for collection " + openBatch.getCollectionId() + ".");
                return;
            }
        }
    }
    
    public synchronized void saveBatch(String userId, String collectionId, Batch batch) throws IOException {
        PrintStream ps = new PrintStream(getBatchFile(userId, collectionId, batch.getId()), "UTF-8");
        ps.println(batch.getName().replace("\n", ""));
        for (String id : batch.listItemIds()) {
            ps.println(id);
        }
        ps.close();
    }

    /**
     * A method to turn a collection identifier into a valid 
     * name for a filesystem directory.
     */
    public static String toPathForm(String collId) {
        return collId;
    }
    
}
