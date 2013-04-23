package ORG.oclc.os.SRW;

import gov.loc.www.zing.srw.ExtraDataType;

public interface ExtendedQueryResult {
    
    public ExtraDataType getExtraDataForRecord(int index);

}
