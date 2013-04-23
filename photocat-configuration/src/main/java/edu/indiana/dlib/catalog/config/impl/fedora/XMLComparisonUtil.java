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
package edu.indiana.dlib.catalog.config.impl.fedora;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * TODO: memory usage and processing time could be dramatically reduced
 * if instead of cloning nodes and cutting out items to ignore we simply
 * created a serializer that could skip those nodes in serialization
 * obviating the need for cloning.
 *
 */
public class XMLComparisonUtil {
    
    private static Logger LOGGER = Logger.getLogger(XMLComparisonUtil.class);

    /**
     * Computes an MD5 hash for a standard serialization of a given
     * Document.
     */
    public static String computeDocumentHash(Node node) throws TransformerException {
        DOMSource source = new DOMSource(node);
        HashOutputStream hos = new HashOutputStream();
        StreamResult sResult = new StreamResult(hos);
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer t = tFactory.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.transform(source, sResult);
        return hos.getMD5Hash();
    }
    
    public static String computeHash(byte[] bytes) {
        HashOutputStream hos = new HashOutputStream();
        hos.write(bytes);
        return hos.getMD5Hash();
    }
    
    public static class HashOutputStream extends OutputStream {

        private MessageDigest digest;
        
        public HashOutputStream() {
            try {
                this.digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                // can't happen because MD5 is supported by all JVMs
                assert false;
            }            
        }
        
        public String getMD5Hash() {
            byte[] inn = this.digest.digest();
            byte ch = 0x00;
            int i = 0;
            String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
            StringBuffer out = new StringBuffer(inn.length * 2);
            while (i < inn.length) {
                ch = (byte) (inn[i] & 0xF0);
                ch = (byte) (ch >>> 4);
                ch = (byte) (ch & 0x0F);
                out.append(pseudo[ (int) ch]);
                ch = (byte) (inn[i] & 0x0F);
                out.append(pseudo[ (int) ch]);
                i++;
            }
            return new String(out);
        }
        
        public void write(int b) throws IOException {
            this.digest.update(new byte[] { (byte) b });
        }
        
        public void write(byte[] b, int off, int len) throws IOException {
            this.digest.update(b, off, len);
        }
        
        public void write(byte[] b) {
            this.digest.update(b);
        }
        
    }
    
}
