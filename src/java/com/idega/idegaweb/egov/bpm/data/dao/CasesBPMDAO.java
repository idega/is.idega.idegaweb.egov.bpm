package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/02/15 12:37:22 $ by $Author: civilis $
 */
public interface CasesBPMDAO extends GenericDao {

	public abstract List<CaseTypesProcDefBind> getAllCaseTypesProcDefBinds();
	
	public abstract List<Object[]> getCaseTypesProcessDefinitions();
	
	public abstract CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId);
}