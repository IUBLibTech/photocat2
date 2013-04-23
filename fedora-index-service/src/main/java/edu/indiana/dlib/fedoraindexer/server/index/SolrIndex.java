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
package edu.indiana.dlib.fedoraindexer.server.index;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.SolrServerException;

import edu.indiana.dlib.fedora.client.FedoraClient;
import edu.indiana.dlib.fedora.client.FedoraException;
import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient;
import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient.DLPStatus;
import edu.indiana.dlib.fedora.client.iudl.PURLLogic;
import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.Index;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;
import edu.indiana.dlib.fedoraindexer.server.IndexOperationException;

public class SolrIndex extends AbstractIndex{
	    
    private XMLReader xmlReader;
    
    private Transformer solrXmlTransformer;
    
    private DLPFedoraClient fc;
	
	/** parent class of remote and embedded server*/
	private SolrServer solr;
	
	/** solr base Url */
	private URL solrURL;
	
	/** solr update Url */
	private URL solrUpdateURL;
	
	/**solr username */
	private String username;
	
	/**solr password */
	private String password;
	
	/**index name */
	private String index;
	
	private static final String SOLR_OK_RESPONSE_EXCERPT = "<int name=\"status\">0</int>";
	
	private static final String DEFAULT_COMMIT = "yes";
	
	/**user-specified exception is checked exception, which must be included the try-catch clause with
	 * a catch clause for that exception or,
	 * include checked exceptions in the method header with throws */
	public SolrIndex (Properties config, DLPFedoraClient fc) throws IndexInitializationException{
		super(config);	
		this.fc = fc;
		this.index = config.getProperty("name");
		
		try {
			this.xmlReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
		} catch (SAXException e) {
			throw new IndexInitializationException("Unsuccessful to create SAX XMLreader:", e);
		}
		
		//create the transformation template from the xslt
		String xsltFileName = config.getProperty("xmlToSolrXSLT");
		LOGGER.info("xmlToSolrXSLT:" + xsltFileName);
		if (xsltFileName == null){
			//throw like a unchecked exception because it is included in the method header
			throw new IndexInitializationException("xmlToSolrXSLT is null in configuration");
		}
	
		FileInputStream fis = null;
		//TransformerFactory factory = TransformerFactory.newInstance();
		TransformerFactory factory = new org.apache.xalan.xsltc.trax.TransformerFactoryImpl();
		
		try {
			fis = new FileInputStream(xsltFileName);
			Source source = new StreamSource(fis);
			Templates temps = factory.newTemplates(source);
			this.solrXmlTransformer = temps.newTransformer();
		} catch(FileNotFoundException e){
			//caught a checked exception and re-throw it, IndexInitializationException should
			//be included in the method header as it is not in the catch clause
			throw new IndexInitializationException("XSLT not found:", e);
		} catch(TransformerConfigurationException e){
			throw new IndexInitializationException("Error to create transformation from the XSLT:", e);
		}
		
		//start solr http server
        String strSolrURL = config.getProperty("solrURL");
        if (strSolrURL == null) {
            throw new IndexInitializationException("Required property, solrURL, was not specified!");
        }
        try {
        	this.solrURL = new URL(strSolrURL);
        	this.solr = startHttpServer(strSolrURL);
        	//System.out.print("solr BaseURL:" + ((CommonsHttpSolrServer) solr).getBaseURL());
        	username = config.getProperty("username");
        	password = config.getProperty("password");
            if (username != null && password != null) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                        username, password);
                HttpClient hc = ((CommonsHttpSolrServer) solr).getHttpClient();
                hc.getParams().setAuthenticationPreemptive(true);
                hc.getState().setCredentials(AuthScope.ANY, credentials);
            }
        }catch (MalformedURLException e){
        	throw new IndexInitializationException("Malformed solr URL:", e);
        }catch (IOException ioe){
        	throw new IndexInitializationException("Unable to read username and password:", ioe);
        }
        
        //parse out solr update url
        String strUpdateURL = config.getProperty("solrUpdateURL");
        //System.out.print("solrupdateurl: " + strUpdateURL);
        if (strUpdateURL == null) {
            throw new IndexInitializationException("Required property, solrUpdateURL, was not specified!");
        }
        try{
        	this.solrUpdateURL = new URL(strUpdateURL);
        }catch (MalformedURLException e){
        	throw new IndexInitializationException("Malformed solr update URL:", e);
        }
  
	}


	public void indexObject(Operation op,FedoraObjectAdministrativeMetadata objectAdminInfo)
			throws IndexOperationException{
		String pid = objectAdminInfo.getPid();
		String format = objectAdminInfo.getContentModel().toString();
		String coll_id = objectAdminInfo.getCollectionId();
		
		String status_str = "init status";
		//get DLP record status
		try {
			if (fc.getDLPStatus(pid) == null){
				status_str = "ignored";
			}
			else {
				status_str = fc.getDLPStatus(pid).getString();
			}
		} catch (FedoraException e) {
			if (e.toString().contains("Unknown Status")){
				LOGGER.warn("Skip indexing pid: " + pid + " because its record status is unknown!");
			}
			else {
				throw new IndexOperationException("DLPFedoraClient has error:", e);
			}
		} catch (IOException e) {
			throw new IndexOperationException("IOException with getting record status:", e);
		}
        
		if (op.equals(Operation.REMOVE)){
			this.deleteByPid(pid);
			LOGGER.info("Successfully remove pid: " + pid + " from index");
		}
		else{
			//check record status
			if (("cataloged".equalsIgnoreCase(status_str)) || ("minimal".equalsIgnoreCase(status_str)) || ("ignored".equalsIgnoreCase(status_str)) || "".equalsIgnoreCase(status_str)){
				LOGGER.info("record status for pid: " + pid + " is: " + status_str);
				//System.out.print("start add/update operation with pid: " + pid);
				ByteArrayOutputStream foxmlOS = new ByteArrayOutputStream();
	            Reader xmlReader = null;
	            ByteArrayOutputStream solrXmlOS = new ByteArrayOutputStream();
	            ByteArrayOutputStream responseOS = new ByteArrayOutputStream();
				            
				try {
					//get mets metadata by pid
				    fc.pipeDatastream(pid, "METADATA", null, foxmlOS);

				    //System.out.print("foxmlos size: " + foxmlOS.size());
					xmlReader = new StringReader(foxmlOS.toString("UTF-8"));
					
					//transform the mets file
	                SAXSource source = new SAXSource(this.xmlReader, new InputSource(xmlReader));
	                StreamResult streamResult = new StreamResult(solrXmlOS);
	                this.solrXmlTransformer.setParameter("pid", pid);
	                this.solrXmlTransformer.setParameter("format", format.replaceAll("^\\[info:fedora/cmodel:|\\]$", ""));
	                this.solrXmlTransformer.setParameter("coll_id", coll_id);
	                this.solrXmlTransformer.transform(source, streamResult);
	                solrXmlOS.close();
	                
	                String solrXML = solrXmlOS.toString("UTF-8");
	                //System.out.print("solrxmlos string: " + solrXML);
	                //index solrXML with post
	                this.postData(new StringReader(solrXML), new OutputStreamWriter(responseOS));
	                //System.out.print("responseos string: " + responseOS.toString("UTF-8"));
					
	                //check index status
	                if (isIndexSuccessful(responseOS.toString("UTF-8"), SOLR_OK_RESPONSE_EXCERPT)){
	                    //commit update
	                    //System.out.print("auto_commit: " + DEFAULT_COMMIT);
	                    if ("yes".equals(DEFAULT_COMMIT)){
	                    	LOGGER.info("Solr commits adding/updating pid: " + pid + " to " + this.index);
	                    	solr.commit();
	                    }
	                    else {
	                    	throw new IndexOperationException("Solr can not commit add/update operation because DEFAULT_COMMIT is not set to \"yes\"");
	                    }
	                }
	                else {
	                	LOGGER.warn("Solr returns error: " + responseOS.toString("UTF-8"));
	                	throw new IndexOperationException ("Solr index is unsuccessful to add/update:" + pid);
	                }
	                
				}catch (IOException e) {
					throw new IndexOperationException("Writing fedora metadata to outputstream is unsuccessful:", e);
				}catch (TransformerException te){
					throw new IndexOperationException("Tranforming meta to solr xml is unsuccessful:", te);
				}catch (SolrServerException e){
					throw new IndexOperationException("Unsuccessful to commit solr add/update:", e);
				}
				//System.out.print("complete add/update operation with pid" + pid);
			}
			else {
				LOGGER.info("Skip indexing pid: " + pid + " because its record status is invalid");
			}
		}
	}


	public void open() throws IndexOperationException {
		// do nothing
		
	}


	public void close() throws IndexOperationException {
		// do nothing
		
	}


	public void optimize() throws IndexOperationException {
		try {
			solr.optimize();
		}catch (SolrServerException e){
			throw new IndexOperationException ("Cannot optimize the solr index:", e);
		}catch (IOException ioe){
			throw new IndexOperationException ("Cannot open the solr index:", ioe);
		}
	}

	public SolrServer startHttpServer(String url) throws MalformedURLException {
		CommonsHttpSolrServer solr = new CommonsHttpSolrServer(url);
		solr.setRequestWriter(new BinaryRequestWriter());
		return solr;
	}
	
	public void deleteByPid(String pid) throws IndexOperationException {
		try{
			/**Pid is the id of solr index document
			 */
			solr.deleteById(pid);
			solr.commit();
		}catch(SolrServerException e){
			throw new IndexOperationException ("cannot delete id: " + pid + "from solr index ", e);
		}catch(IOException ioe){
			throw new IndexOperationException ("cannot open the solr index:", ioe);
		}
	}
	
    public static void writeStreamToStream(InputStream is, OutputStream os) throws IOException {
        ReadableByteChannel inputChannel = Channels.newChannel(is);  
        WritableByteChannel outputChannel = Channels.newChannel(os);  
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);  
        while (inputChannel.read(buffer) != -1) {  
            buffer.flip();  
            outputChannel.write(buffer);  
            buffer.compact();  
        }  
        buffer.flip();  
        while (buffer.hasRemaining()) {  
            outputChannel.write(buffer);  
        }  
       inputChannel.close();  
       outputChannel.close();
    }
    
    /**
     * Reads data from the data reader and posts it to solr,
     * writes to the response to output
     */
    public void postData(Reader data, Writer output) throws IndexOperationException{

      HttpURLConnection urlc = null;      
      try {
        urlc = (HttpURLConnection) solrUpdateURL.openConnection();
        try {
          urlc.setRequestMethod("POST");
        } catch (ProtocolException e) {
          throw new IndexOperationException("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
        }
        urlc.setDoOutput(true);
        urlc.setDoInput(true);
        urlc.setUseCaches(false);
        urlc.setAllowUserInteraction(false);
        urlc.setRequestProperty("Content-type", "text/xml; charset=" + "UTF-8");
        
        OutputStream out = urlc.getOutputStream();
        
        try {
          Writer writer = new OutputStreamWriter(out, "UTF-8");
          pipe(data, writer);
          writer.close();
        } catch (IOException e) {
          throw new IndexOperationException("IOException while posting data", e);
        } finally {
          if(out!=null) out.close();
        }
        
        InputStream in = urlc.getInputStream();
        try {
          Reader reader = new InputStreamReader(in);
          pipe(reader, output);
          reader.close();
        } catch (IOException e) {
          throw new IndexOperationException("IOException while reading response", e);
        } finally {
          if(in!=null) in.close();
        }
        
      } catch (IOException e) {
    	  throw new IndexOperationException ("Unsuccessful to post data to solr:", e);
      } finally {
        if(urlc!=null) urlc.disconnect();
      }
    }

    /**
     * Pipes everything from the reader to the writer via a buffer
     */
    private static void pipe(Reader reader, Writer writer) throws IOException {
      char[] buf = new char[1024];
      int read = 0;
      while ( (read = reader.read(buf) ) >= 0) {
        writer.write(buf, 0, read);
      }
      writer.flush();
    }
    
    private boolean isIndexSuccessful(String response, String pattern){
    	return response.contains(pattern);
    }
    
}
