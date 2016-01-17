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
package edu.indiana.dlib.catalog.asynchronous;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

/**
 * A ServletContextListener that is responsible for managing
 * the execution of all asynchronous Operations.  While exposed
 * by the individual UserOperationManager objects, to allow
 * operations to run after session end this class is required.
 */
public class OperationManagerContextListener implements ServletContextListener {

    private Logger LOGGER = Logger.getLogger(OperationManagerContextListener.class);
    
    /**
     * This queue is only added to by the method queueOperation() and 
     * only has items removed by the Worker.  If it is empty, it can 
     * be assured that there is no Thread Working and a new one should
     * be created for new requests.
     */
    private LinkedList<UserOperation> operationQueue;
    
    /**
     * Null whenever the queue is empty, set to the working thread
     * whenever the queue is not empty.  Synchronized access to the 
     * operationQueue can ensure this.
     */
    private Thread worker;

    /**
     * Gets the one OperationManagerContextListener for this application 
     * from the ServletContext.
     */
    public static OperationManagerContextListener getOperationManager(ServletContext sc) {
        return (OperationManagerContextListener) sc.getAttribute(OperationManagerContextListener.class.getName());
    }
    
    public OperationManagerContextListener() {
        operationQueue = new LinkedList<UserOperation>();
    }

    /**
     * This method should resume any paused operations, but 
     * currently does not.
     */
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute(getClass().getName(), this);
    }
    
    /**
     * This method should pauses and serialize any currently running
     * operations, but right now it simply aborts them.
     */
    public void contextDestroyed(ServletContextEvent sce) {
        synchronized (operationQueue) {
            if (!operationQueue.isEmpty()) {
                for (UserOperation userOp : operationQueue) {
                    userOp.operation.abort();
                }
            }
        }
        try {
            worker.join();
        } catch (NullPointerException ex) {
            // the process ended and the thread was released 
            // between when we released the lock on "operationQueue"
            // and right now.  This is very unlikely and using a 
            // second lock could prevent it, but it's simpler 
            // to just acknowledge the possibility here than
            // complicate this method, especially since this 
            // entire implementation will be overwritten once we
            // make Operations persistent across application runs.
        } catch (InterruptedException ex) {
            // Ok... perhaps the operation is really stalled
            throw new RuntimeException(ex);
        }
    }

    /**
     * Adds the given operation to the queue of operations to be 
     * performed by this UserOperationManager.  The current
     * implementation will start working on the operation immediately
     * unless there are other Operations in the queue before it.
     */
    public void queueOperation(Operation operation, String userId) {
        synchronized(operationQueue) {
            if (operationQueue.isEmpty()) {
                operationQueue.add(new UserOperation(userId, operation));
                worker = new Thread(new Worker());
                worker.start();
            } else {
                operationQueue.add(new UserOperation(userId, operation));
            }
        }
    }
    
    /**
     * Lists operations that either haven't run yet or are
     * currently running for the given userId.
     */
    public List<Operation> listIncompleteOperations(String userId) {
        List<Operation> operations = new ArrayList<Operation>();
        synchronized (operationQueue) {
            for (UserOperation userOp : operationQueue) {
                if (userOp.userId.equals(userId)) {
                    operations.add(userOp.operation);
                }
            }
            return operations;
        }
    }
    
    /**
     * A thread that processes the operationQueue until it is empty, then dies. 
     */
    public class Worker implements Runnable {

        public void run() {
            Operation op = null;
            do {
                // add the just
                synchronized (operationQueue) {
                    if (operationQueue.isEmpty()) {
                        LOGGER.debug("Worker completed the job and was released.");
                        return;
                    } else {
                        op = operationQueue.peek().operation;
                    }
                }
                LOGGER.debug("Worker began processing " + op.getDescription() + ".");
                long start = System.currentTimeMillis();
                try {
                    op.run();
                } catch (Throwable t) {
                    LOGGER.error("Operation threw an exception and was terminated!", t);
                }
                long end = System.currentTimeMillis();
                LOGGER.debug("Worker finished processing " + op.getDescription() + " in " + ((end - start) / 1000) + " seconds.");
                
                // remove the just-completed operation
                synchronized (operationQueue) {
                    operationQueue.poll();
                }
            } while (true);
        }
    }
    
    /**
     * A data structure containing a user id and an Operation.
     */
    private static class UserOperation {
        
        public Operation operation;
        
        public String userId;
        
        public UserOperation(String userId, Operation op) {
            this.userId = userId;
            operation = op;
        }
        
    }

}
