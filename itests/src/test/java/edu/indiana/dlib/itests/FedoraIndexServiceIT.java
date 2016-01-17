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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FedoraIndexServiceIT extends AbstractIT {

    @Test
    public void testStatus() throws IOException {
        HttpGet get = new HttpGet(BASE_URL + "fedora-index-service/index?operation=status");
        HttpResponse response = CLIENT.execute(get);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        Status s = new Status(response.getEntity().getContent());
        Assert.assertEquals(indexUpdateCalls, s.getUpdates());
        Assert.assertEquals(0, s.getAdds());
        Assert.assertEquals(0, s.getRemoves());
        Assert.assertEquals(indexUpdateCalls + 1, s.getRestApiCalls());
        Assert.assertEquals(0, s.getMessageCount());
    }

    /**
     * A simple class that crudely parses the XML response from the
     * index status request to the fedora-index-service.  The current
     * implementation just searches the text for
     * &lt;tag-name&gt;value&lt;/tag-name&gt; patterns.
     */
    private static class Status {
        private String rawXML;

        public Status(InputStream xml) throws IOException {
            rawXML = IOUtils.toString(xml);
        }

        public int getUpdates() {
            return getIntField("updates");
        }

        public int getAdds() {
            return getIntField("adds");
        }

        public int getRemoves() {
            return getIntField("removes");
        }

        public int getRestApiCalls() {
            return getIntField("rest-api-calls");
        }

        public int getMessageCount() {
            return getIntField("jms-messages-received");
        }

        private int getIntField(String name) {
            String value = getField(name);
            if (name == null) {
                return -1;
            } else {
                return Integer.parseInt(value);
            }
        }

        private String getField(String name) {
            Pattern p = Pattern.compile("^.*\\Q<" + name + ">\\E(.*)\\Q</" + name + ">\\E.*$", Pattern.DOTALL);
            Matcher m = p.matcher(rawXML);
            if (m.matches()) {
                return m.group(1);
            } else {
                System.out.println("No match within \"" + rawXML + "\"!");
                return null;
            }

        }
    }
}
