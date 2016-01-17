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
package edu.indiana.dlib.catalog.fields.click.control.uifield;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.click.control.TextField;

public class RobustDateTextField extends TextField {

    public RobustDateTextField(String string) {
        super(string);
    }

    public boolean getValidate() {
        return true;
    }
    
    public void validate() {
        String entered = getValue();
        if (value == null || value.trim().length() == 0) {
            return;
        } else {
            // split the value
        	Matcher yearOnlyMatcher = Pattern.compile("\\d\\d\\d\\d").matcher(entered);
        	Matcher yearMonthMatcher = Pattern.compile("\\d\\d\\d\\d-\\d\\d").matcher(entered);
        	Matcher yearMonthDayMatcher = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d").matcher(entered);
            Matcher yearRangeMatcher = Pattern.compile("(\\d\\d\\d\\d)\\Q-\\E(\\d\\d\\d\\d)").matcher(entered);
            Matcher bceMatcher = Pattern.compile("\\d+ BCE").matcher(entered);
            Matcher ceMatcher = Pattern.compile("\\d+ CE").matcher(entered);
            if (!yearOnlyMatcher.matches() && !yearRangeMatcher.matches() && !yearMonthMatcher.matches() && !yearMonthDayMatcher.matches() && !bceMatcher.matches() && !ceMatcher.matches()) {
                setError(getMessage("invalid-date-spec", entered));
                return;
            }
        }        
    }
    
    public static boolean isValueValid(String entered) {
        if (entered == null || entered.trim().length() == 0) {
            return true;
        } else {
            // split the value
            Matcher yearOnlyMatcher = Pattern.compile("\\d\\d\\d\\d").matcher(entered);
            Matcher yearMonthMatcher = Pattern.compile("\\d\\d\\d\\d-\\d\\d").matcher(entered);
            Matcher yearMonthDayMatcher = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d").matcher(entered);
            Matcher yearRangeMatcher = Pattern.compile("(\\d\\d\\d\\d)\\Q-\\E(\\d\\d\\d\\d)").matcher(entered);
            Matcher bceMatcher = Pattern.compile("\\d+ BCE").matcher(entered);
            Matcher ceMatcher = Pattern.compile("\\d+ CE").matcher(entered);
            if (!yearOnlyMatcher.matches() && !yearRangeMatcher.matches() && !yearMonthMatcher.matches() && !yearMonthDayMatcher.matches() && !bceMatcher.matches() && !ceMatcher.matches()) {
                return false;
            } else {
                return true;
            }
        }        
    }
    
    public static String getYear(String value) {
    	for (String patternStr : new String[] {"(\\d\\d\\d\\d)", "(\\d\\d\\d\\d)-\\d\\d", "(\\d\\d\\d\\d)-\\d\\d-\\d\\d"}) {
    		Matcher m = Pattern.compile(patternStr).matcher(value);
    		if (m.matches()) {
    			return m.group(1);
    		}
    	}
    	return null;
    }
	
    public static String getMonth(String value) {
    	for (String patternStr : new String[] {"\\d\\d\\d\\d-(\\d\\d)", "\\d\\d\\d\\d-(\\d\\d)-\\d\\d"}) {
    		Matcher m = Pattern.compile(patternStr).matcher(value);
    		if (m.matches()) {
    			return String.valueOf(Integer.parseInt(m.group(1)));
    		}
    	}
    	return null;
    }
    
    public static String getDay(String value) {
    	for (String patternStr : new String[] {"\\d\\d\\d\\d-\\d\\d-(\\d\\d)"}) {
    		Matcher m = Pattern.compile(patternStr).matcher(value);
    		if (m.matches()) {
    			return String.valueOf(Integer.parseInt(m.group(1)));
    		}
    	}
    	return null;
    }
}
