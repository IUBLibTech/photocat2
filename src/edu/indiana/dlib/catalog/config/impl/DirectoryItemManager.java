/*
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
package edu.indiana.dlib.catalog.config.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.OptimisticLockingException;
import edu.indiana.dlib.catalog.config.RepositoryException;
import edu.indiana.dlib.catalog.index.ItemMetadataLuceneIndex;

/**
 * A dummy ItemManager implementation that stores and manages
 * items in simple directory structure.  There is one directory
 * for each item (based on it's id).  Within that there's an
 * item-metadata.xml file representing the metadata record and
 * a directory called "views" that contains files treated as
 * views.
 */
public class DirectoryItemManager implements ItemManager {

    private ItemMetadataLuceneIndex index;
    
    private File rootDirectory;
    
    public DirectoryItemManager(String itemRepositoryDirName, String indexDirName) {
        String photocatHome = System.getenv("PHOTOCAT_HOME");
        if (photocatHome != null && !itemRepositoryDirName.startsWith("/")) {
            File homeDir = new File(photocatHome);
            rootDirectory = new File(homeDir, itemRepositoryDirName);
        } else {
            rootDirectory = new File(itemRepositoryDirName);
        }
        try {
            if (photocatHome != null && !indexDirName.startsWith("/")) {
                File homeDir = new File(photocatHome);
                index = new ItemMetadataLuceneIndex(new File(homeDir, indexDirName));
            } else {
                index = new ItemMetadataLuceneIndex(new File(indexDirName));
            }
            index.clearIndex();
            rootDirectory.mkdirs();
            for (File itemDir : rootDirectory.listFiles()) {
                if (itemDir.isDirectory()) {
                    index.indexItem(fetchItem(getItemId(itemDir)));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Item fetchItem(String id) throws RepositoryException {
        File itemDir = getItemDirectory(id);
        if (!itemDir.exists()) {
            return null;
        }
        try {
            File itemMetadataFile = new File(itemDir, "item-metadata.xml");
            ItemMetadata im = new FileItemMetadata(itemMetadataFile);
            if (!im.getId().equals(id)) {
                throw new IllegalStateException("id mismatch in file " + itemMetadataFile.getAbsolutePath());
            }
            
            List<DataView> views = new ArrayList<DataView>();
            for (File file : itemDir.listFiles()) {
                if (file.getName().endsWith(".jpg")) {
                    views.add(new DefaultDataView(file.toURI().toURL(), "image/jpeg", file.getName(), false));
                }
            }
            return new DefaultItem(im, views, null, null);
        } catch (Exception ex) {
            throw new RepositoryException(ex);
        }
    }
    
    private File getItemDirectory(String id) {
        return new File(rootDirectory, id);
    }
    
    private String getItemId(File dir) {
        return dir.getName();
    }

    public void saveItemMetadata(Item item, UserInfo user) throws OptimisticLockingException, RepositoryException {
        try {
            FileOutputStream fos = new FileOutputStream(new File(getItemDirectory(item.getId()), "item-metadata.xml"));
            item.getMetadata().writeOutXML(fos);
            fos.close();
            index.indexItem(item);
        } catch (Throwable t) {
            throw new RepositoryException(t);
        }
    }

    public String createNewItem(CollectionConfiguration config, UserInfo user) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    public boolean removeItem(Item item, CollectionConfiguration config, UserInfo user) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
