package edu.indiana.dlib.catalog.dataimport;

import java.io.File;

import edu.indiana.dlib.catalog.dataimport.filemaker.FilemakerXML;
import edu.indiana.dlib.catalog.dataimport.spreadsheet.SpreadsheetRecords;

//creates an object based on given file type and return the object
public class ParseFactory {
	
	public Records getRecords(File file, String originalName) {
		Records records = null;
	      try {
	        	
	        	if (originalName.endsWith(".xml")) {
	        		records =  new FilemakerXML(file);	
	        	}else if (originalName.endsWith("xls") || originalName.endsWith("xlsx")) {
	        		
	        		records = new SpreadsheetRecords(file);
	        	}
	        	
	        	
	        }catch (Exception ex) {
	            // not a parsable spreadsheet or filemaker file
	            records = null;
	        }
	      return records;
		
	}

}
