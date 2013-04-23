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
package edu.indiana.dlib.catalog.search.structured;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.click.Context;


public abstract class AbstractSearchConstraint implements SearchConstraint {

    //private Map<String, String> messages;
    
    /**
     * Return the localized message for the given key or null if not found.
     * The resource message returned will use the Locale obtained from the
     * Context.
     * <p/>
     * This method will attempt to lookup the localized message in the
     * parent's messages, which resolves to the Page's resource bundle.
     * <p/>
     * If the message was not found, this method will attempt to look up the
     * value in the <tt>/click-control.properties</tt> message properties file,
     * through the method {@link #getMessages()}.
     * <p/>
     * If still not found, this method will return null.
     *
     * @param name the name of the message resource
     * @return the named localized message for the control, or null if not found
     */
    /*
    public String getMessage(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        if (getMessages().containsKey(name)) {
            return getMessages().get(name);
        } else {
            return null;
        }
    }*/

    /**
     * Return the formatted message for the given resource name and message
     * format arguments or null if no message was found. The resource
     * message returned will use the Locale obtained from the Context.
     * <p/>
     * {@link #getMessage(java.lang.String)} is invoked to retrieve the message
     * for the specified name.
     *
     * @param name resource name of the message
     * @param args the message arguments to format
     * @return the named localized message for the control or null if no message
     * was found
     */
    /*
    public String getMessage(String name, Object... args) {
        String value = getMessage(name);
        if (value == null) {
            return null;
        }
        return MessageFormat.format(value, args);
    } */
    
    /**
     * Return a Map of localized messages for the control. The messages returned
     * will use the Locale obtained from the Context.
     *
     * @return a Map of localized messages for the control
     * @throws IllegalStateException if the context for the control has not be set
     */
    /*
    public Map<String, String> getMessages() {
        if (messages == null) {
            messages = getContext().createMessagesMap(getClass(), null);
        }
        return messages;
    }
    */
    
    /**
     * @see org.apache.click.Control#getContext()
     *
     * @return the Page request Context
     */
    /*
    public Context getContext() {
        return Context.getThreadLocalContext();
    }
    */
    
}
