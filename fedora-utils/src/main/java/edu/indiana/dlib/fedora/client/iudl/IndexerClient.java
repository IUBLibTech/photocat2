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
package edu.indiana.dlib.fedora.client.iudl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * A class that allows easy access to the REST interface for
 * the fedora-index-service.  Essentially this class can be
 * used to trigger the indexing (reindexing) of a given object.
 */
public class IndexerClient {

    /**
     * An underlying HttpClient that handles the REST calls.  This
     * client is initialized at construction time.
     */
    private HttpClient client;
    
    private String baseUrl;
    
    public IndexerClient(String host, String contextName, int port) {
        this(null, null, host, contextName, port);
    }
    
    public IndexerClient(String username, String password, String host, String contextName, int port) {
    
        this.baseUrl = "http://" + host + ":" + port + "/" + contextName + "/";
    
        // Create an HTTP client for future REST calls
        this.client = new HttpClient();
        if (username != null) {
            this.client.getParams().setAuthenticationPreemptive(true);
            Credentials credentials = new UsernamePasswordCredentials(username, password);
            this.client.getState().setCredentials(new AuthScope(host, AuthScope.ANY_PORT), credentials);
        }
    }
    
    /**
     * Sends a request to reindex the item identified by the given pid, as if it were
     * modified at this given date.
     * @param pid the pid of the object to reindex
     * @param modificationDate the modification (in the fedora format) the item was last
     * changed.
     * @throws IOException if an error occurs while accessing the remote resource
     * @throws HttpException if an error occurs while making the HTTP reqeust
     */
    public void indexItem(String pid, String modificationDate) throws HttpException, IOException {
        String url = this.baseUrl + "index?operation=UPDATE&pid=" + pid + "&lastModificationDate=" + URLEncoder.encode(modificationDate, "UTF-8");
        System.out.println("url: " + url);
        GetMethod get = new GetMethod(url);
        int statusCode = this.client.executeMethod(get);
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
        }
    }
    
    public void removeItem(String pid, String modificationDate) throws HttpException, IOException {
        String url = this.baseUrl + "index?operation=REMOVE&pid=" + pid + "&lastModificationDate=" + URLEncoder.encode(modificationDate, "UTF-8");
        System.out.println("url: " + url);
        GetMethod get = new GetMethod(url);
        int statusCode = this.client.executeMethod(get);
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
        }
    }
    
    
    
    public void optimizeIndex() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Gets the timestamp that the fedora-index-service is reporting as the
     * last time a message (from JMS) was received.
     */
    public String getLastMessageDateString() throws HttpException, IOException {
        String url = this.baseUrl + "index?operation=status";
        System.out.println("url: " + url);
        GetMethod get = new GetMethod(url);
        int statusCode = this.client.executeMethod(get);
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()));
            Pattern p = Pattern.compile(".*\\Q<jms-last-message-date>\\E(.*)\\Q</jms-last-message-date>\\E.*");
            String line = reader.readLine();
            while (line != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    return m.group(1);
                }
                line = reader.readLine();
            }
        }
        throw new RuntimeException("Unable to parse date from response!");
    }

}
