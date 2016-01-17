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
package edu.indiana.dlib.itests;

import static java.lang.Integer.MAX_VALUE;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;

import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient;
import edu.indiana.dlib.fedora.client.iudl.IndexerClient;

public abstract class AbstractIT {

    protected static final int CARGO_PORT = Integer.parseInt(System.getProperty("cargo.port"));

    protected static final HttpClient CLIENT = createClient();

    private static HttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                .setMaxConnTotal(MAX_VALUE).build();
    }

    protected final static String BASE_URL = "http://localhost:" + CARGO_PORT + "/";

    /**
     * The FIXURES_DIR is a directory containing foxml files that should be ingested
     * before the integration tests are run.  These foxml files should have been 
     * exported using the "context=archive" parameter so they are fully self-contained
     * and are expected to have a name that equals their pid with the ":" replaced by an
     * underscore.
     */
    protected final static File FIXTURES_DIR = new File(System.getProperty("fixtures.path"));

    /**
     * A dirty trick to ensure that the setUp() method only ingests items once.
     */
    private static boolean setUp = false;
   
    protected static int indexUpdateCalls = 0;
    
    /**
     * A method called before each class that extends this base class to pre-populate fedora
     * and the indexes with data from the fixtures directory.
     * @throws Exception
     */
    @BeforeClass
    public synchronized static void setUp() throws Exception {
        if (!setUp) {
            DLPFedoraClient fc = new DLPFedoraClient("fedoraAdmin", "integrationTest", "localhost", "fedora", CARGO_PORT, false);
            for (File foxmlFile : FIXTURES_DIR.listFiles()) {
                fc.importObject(foxmlFile);
            }

            // Index them in pid order.
            IndexerClient ic = new IndexerClient("fedoraAdmin", "integrationTest", "localhost", "fedora-index-service", CARGO_PORT);
            for (String filename : FIXTURES_DIR.list()) {
                String pid = filename.replace("_", ":");
                ic.indexItem(pid, fc.getObjectProfile(pid, null).getLastModDate());
                indexUpdateCalls ++;
            }
            setUp = true;
        }
    }

    protected void assertStatusCode(String url, int code) throws ClientProtocolException, IOException {
        assertStatusCode(url, code, false);
    }
    
    protected void assertStatusCode(String url, int code, boolean echoOutput) throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(url);
        HttpResponse response = CLIENT.execute(get);
        if (echoOutput || (code != response.getStatusLine().getStatusCode())) {
            System.out.println("(content at " + url + ") " + IOUtils.toString(response.getEntity().getContent()));
        }
        Assert.assertEquals("Bad response code (" + response.getStatusLine() + ") from " + url + ".", code, response.getStatusLine().getStatusCode());
    }
    
   
    
}
