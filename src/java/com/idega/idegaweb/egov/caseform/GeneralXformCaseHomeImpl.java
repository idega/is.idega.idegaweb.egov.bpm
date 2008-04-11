package com.idega.idegaweb.egov.caseform;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.idega.data.IDOFactory;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/11 12:53:32 $ by $Author: arunas $
 */
public class GeneralXformCaseHomeImpl extends IDOFactory implements GeneralXformCaseHome{

    public Class getEntityInterfaceClass() {
	return GeneralXformCase.class;
    }
    
    public GeneralXformCase create() throws CreateException {
	return (GeneralXformCase) super.createIDO();
    }

    public GeneralXformCase findByPrimaryKey(Object pk) throws FinderException {
	return (GeneralXformCase) super.findByPrimaryKeyIDO(pk);
    }
    

}
