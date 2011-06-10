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
package edu.indiana.dlib.catalog.asynchronous;

/**
 * An AsynchronousOperation essentially some program that is 
 * run outside of the normal flow of execution.  Any process
 * that could take more than the few seconds that a user can 
 * be expected to wait for a response should be an asynchronous
 * operation.
 * 
 * These operations are relatively dumb, but should be written
 * to be thread-safe.
 */
public interface Operation extends Runnable {
    
    /**
     * Gets the description of this operation for human consumption.
     */
    public String getDescription();
    
    /**
     * The method that performs the work of this operation.
     * @extends Runnable
     */
    public void run();
    
    /**
     * Aborts the current operation, such that the "run()" method
     * will return ASAP.  This method will only be called in 
     * critical situations and therefore underlying implementations
     * should err on the side of quickly returning rather than
     * cleaning up.
     */
    public void abort();
    
    /**
     * Gets an estimate of the percent of this operation that has been
     * completed.  If implementations don't wish to hazard a guess, a
     * returned value of less than zero will be interpreted as unknown.
     * @return a value 0-1 that indicates the percent complete, or a negative
     * value if no estimate is provided. 
     */
    public double getEstimatedPercentCompleted();
    
    /**
     * A method that returns true if this process will not complete without
     * user interaction.
     * @return true if this operation requires user interaction before completing
     */
    public boolean requiresUserInteraction();

    /**
     * A method to get the current dialog (or interaction) with the process.
     * @return a Dialog object containing information to convey to the user
     * about this operation.
     */
    public Dialog getInteractionDialog();
    
    /**
     * A method to receive the response from the user regarding a dialog
     * initiated by this operation.  
     */
    public void respondToInteractionDialog(Dialog dialog, String response);

}
