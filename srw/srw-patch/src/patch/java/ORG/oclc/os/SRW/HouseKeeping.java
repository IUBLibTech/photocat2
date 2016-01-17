/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * HouseKeeping.java
 *
 * Created on November 2, 2004, 2:19 PM
 */

package ORG.oclc.os.SRW;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.commons.logging.Log;

/**
 *
 * @author  levan
 */
public class HouseKeeping extends java.util.TimerTask {
    Log  log;
    Hashtable oldResultSets, timers;
    
    public HouseKeeping(final Hashtable timers, final Hashtable oldResultSets,
      final Log log) {
        this.timers=timers;
        this.oldResultSets=oldResultSets;
        this.log=log;
    }
    
    public void run() {
        Enumeration enumer, enum2;
        Hashtable sortedResults;
        long   now=System.currentTimeMillis(), when;
        QueryResult result, sortedResult;
        String key, resultSetName;
        for(enumer=timers.keys(); enumer.hasMoreElements();) {
            resultSetName=(String)enumer.nextElement();
            when=((Long)timers.get(resultSetName)).longValue();
            if(when<now) {
                log.info("removing resultSet "+resultSetName);
                result=(QueryResult)oldResultSets.remove(resultSetName);
                timers.remove(resultSetName);
                try {
                sortedResults=result.getSortedResults();
                enum2=sortedResults.keys();
                while(enum2.hasMoreElements()) {
                    key=(String)enum2.nextElement();
                    sortedResult=(QueryResult)sortedResults.remove(key);
                    sortedResult.close();
                }
                result.close();
                } catch (Throwable t) {
                    log.error("Exception thrown while cleaning up resultSet \"" + resultSetName + "\".", t);
                }
            }
        }
    }
}
