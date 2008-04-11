package com.idega.idegaweb.egov.caseform;

import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.data.GeneralCaseBMPBean;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/11 12:53:32 $ by $Author: arunas $
 */
public class GeneralXformCaseBMPBean extends GeneralCaseBMPBean implements GeneralXformCase, GeneralCase{

    private final static String CASE_CODE_KEY_DESC = "Xform cases";
    private static final String ENTITY_NAME = "XfORMS_CASE";
    private static final String COLUMN_XFORM_ID = "Xform_id";
    private static final String COLUMN_XFORM_LOCATION = "Location";

    @Override
    public void initializeAttributes() {
	super.initializeAttributes();
	addAttribute(getNameColumnXfID(), "xform_id", long.class);
	addAttribute(getNameColumnXfLocation(), "location", String.class);

    }

    public String getCaseCodeDescription() {
	return "XFORM";
    }
    
    public String getCaseCodeKey() {
	return CASE_CODE_KEY_DESC;
    }
    
//	get names
    public String getEntityName() {
	return ENTITY_NAME;
    }

    public String getNameColumnXfID() {
	return COLUMN_XFORM_ID;
    }

    public String getNameColumnXfLocation() {
	return COLUMN_XFORM_LOCATION;
    }
//	get colunms values 
    public long getXformId(){
	return getIntegerColumnValue(COLUMN_XFORM_ID);
    }
    
    public String getXfromLocation() {
	return getStringColumnValue(getNameColumnXfLocation());
    }

//	set columns
    public void setXformId(String xformId) {
	setColumn(getNameColumnXfID(), xformId);
    }
    public void setXfromLocation(String location) {
	setColumn(getNameColumnXfLocation(), location);
    }
    
}
