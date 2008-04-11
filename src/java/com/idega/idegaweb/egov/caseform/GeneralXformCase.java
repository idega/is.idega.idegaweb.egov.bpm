package com.idega.idegaweb.egov.caseform;

import com.idega.block.process.data.Case;
import com.idega.data.IDOEntity;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/11 12:53:32 $ by $Author: arunas $
 */
public interface GeneralXformCase extends IDOEntity, Case {
    
    public long getXformId();
    
    public String getXfromLocation();
    
    public void setXformId(String xformId);
    
    public void setXfromLocation(String location);
    
}
