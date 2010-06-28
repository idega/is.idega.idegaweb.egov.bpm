package is.idega.idegaweb.egov.bpm.cases.exe;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.data.IDOLookup;
import com.idega.data.MetaData;
import com.idega.data.MetaDataBMPBean;
import com.idega.data.MetaDataHome;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public abstract class DefaultIdentifierGenerator {

	private static final Logger LOGGER = Logger.getLogger(DefaultIdentifierGenerator.class.getName());
	
	private static final String IDENTIFIER_META_DATA = "CASE_IDENTIFIER_IS_TAKEN_META_DATA";
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	/**
	 * It is strongly recommended to implement this method as synchronized
	 * 
	 * @return
	 */
	public abstract Object[] generateNewCaseIdentifier();
	
	/**
	 * It is strongly recommended to implement this method as synchronized
	 * 
	 * @return
	 */
	protected abstract Object[] generateNewCaseIdentifier(String usedIdentifier);
	
	protected synchronized boolean canUseIdentifier(String identifier) {
		//	0.	Checking if identifier is not empty
		if (StringUtil.isEmpty(identifier)) {
			LOGGER.warning("Identifier is empty or null!");
			return false;
		}
		
		//	1.	Will check record in meta data table for the given identifier
		try {
			if (isStoredInMetaData(identifier)) {
				return false;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occured while checking identifier ('" + identifier + "') in DB table: " + MetaDataBMPBean.TABLE_NAME, e);
		}
		
		//	2.	Checking if already exists variable with such identifier
		try {
			if (isStoredInVariables(identifier)) {
				return false;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occured while checking identifier ('" + identifier + "') in BPM variables table", e);
		}
		
		//	Identifier can be used, marking it as "taken" in meta data table
		try {
			storeIdentifier(identifier);
		} catch (Exception e) {
			String errorMessage = "Error saving identifier: '" + identifier + "'";
			LOGGER.log(Level.SEVERE, errorMessage, e);
			CoreUtil.sendExceptionNotification(errorMessage, e);
			return false;
		}
		
		return true;
	}
	
	private boolean isStoredInMetaData(String identifier) throws Exception {
		MetaDataHome metaDataHome = (MetaDataHome) IDOLookup.getHome(MetaData.class);
		try {
			metaDataHome.findByMetaDataNameAndValueAndType(IDENTIFIER_META_DATA, identifier, String.class.getName());
			return true;
		}
		catch (FinderException e) {
			return false;
		}
	}
	
	private boolean isStoredInVariables(String identifier) throws Exception {
		try {
			return getVariablesQuerier().isVariableStored(CasesBPMProcessConstants.caseIdentifier, identifier);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error occurred while selecting " + VariableInstanceInfo.class + " objects by variable name: " +
					CasesBPMProcessConstants.caseIdentifier + " and value: " + identifier, e);
		}
		
		return false;
	}
	
	private void storeIdentifier(String identifier) throws Exception {
		MetaDataHome metaDataHome = (MetaDataHome) IDOLookup.getHome(MetaData.class);
		MetaData metaData = metaDataHome.create();
		metaData.setValue(identifier);
		metaData.setName(IDENTIFIER_META_DATA);
		metaData.setType(String.class.getName());
		metaData.store();
	}

	public CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}
}