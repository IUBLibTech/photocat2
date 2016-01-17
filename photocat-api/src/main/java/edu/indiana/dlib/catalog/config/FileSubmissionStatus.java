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
package edu.indiana.dlib.catalog.config;

import java.util.Date;

public class FileSubmissionStatus {

	public enum Status {
		/** The status when an additional data file may be submitted. */
		PENDING_SUBMISSION,
		
        /** The status when a data file has been uploaded but not yet processed. */
        PENDING_PROCESSING,
        
        /** The status a data file was uploaded but rejected for being invalid. */
        FILE_VALIDATION_ERROR,
        
        /** The status when a data file has been processed but not yet ingested. */
        PENDING_INGEST,
        
        /**
         * The status a data file has been ingested and no more data files are expected. 
         */
        INGESTED,
        
        /** 
         * The status of an image that is not current in the processing/ingest
         *  workflow.
         */
        SUBMISSION_NOT_CONFIGURED;
	}
	
	private Status status;
	
	private Date lastActionDate;
	
	public FileSubmissionStatus(Status status, Date lastActionDate) {
		this.status = status;
		this.lastActionDate = lastActionDate;
	}
	
	public Status getStatusCode() {
		return status;
	}
	
	public Date getLastActionDate() {
		return lastActionDate;
	}
}
