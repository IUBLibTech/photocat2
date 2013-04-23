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

import java.io.IOException;
import java.io.InputStream;

import edu.indiana.dlib.catalog.config.FileSubmissionStatus;
import edu.indiana.dlib.catalog.config.Item;

/**
 * A class that encapsulates and interprets the configuration
 * for file submission/management.  There are several types
 * of file submission cases possible.
 * 
 * Submission of a file to an item.  (the item has no Aspect objects)
 * 
 * Submission of a replacement file for an item's aspect. (the item has one 
 * or more Aspect objects with fully processed files)
 * 
 * Removal of an aspect.
 */
public interface FileSubmitter {

    /**
	 * Returns true if the configuration is sufficient to allow
	 * submission of a single file.
	 */
	public boolean isFileSubmissionAvailable(Item item);
	
	public void submitFile(InputStream fileIs, Item item) throws IOException;
	   
    public boolean isFileReplacementAvailable(Item item, String aspectId);
    
    public void replaceFile(InputStream fileIs, Item item, String aspectId) throws IOException;
    
    public boolean isFileRemovalAvailable(Item item, String aspectId);
    
    public void removeFile(Item item, String aspectId) throws IOException;
	
    public FileSubmissionStatus getFileSubmissionStatus(Item item);
    
    public FileSubmissionStatus getFileSubmissionStatus(Item item, String aspectId);

}
