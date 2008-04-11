package com.idega.idegaweb.egov.caseform;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.idega.data.IDOHome;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/11 12:53:32 $ by $Author: arunas $
 */
public interface GeneralXformCaseHome extends IDOHome{
    
    public GeneralXformCase create() throws CreateException;
    
    public GeneralXformCase findByPrimaryKey(Object pk) throws FinderException;
    

}
