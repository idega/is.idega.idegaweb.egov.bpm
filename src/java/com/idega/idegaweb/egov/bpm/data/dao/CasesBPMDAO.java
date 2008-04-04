package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.Collection;
import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/04/04 21:48:26 $ by $Author: civilis $
 */
public interface CasesBPMDAO extends GenericDao {

	public abstract List<CaseTypesProcDefBind> getAllCaseTypesProcDefBinds();
	
	public abstract List<Object[]> getCaseTypesProcessDefinitions();
	
	public abstract CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId);
	
	public abstract ProcessUserBind getProcessUserBind(long processInstanceId, int userId, boolean createIfNotFound);
	
	public abstract List<ProcessUserBind> getProcessUserBinds(int userId, Collection<Integer> casesIds);
	
	public abstract CaseTypesProcDefBind getCaseTypesProcDefBind(long processDefinitionId);
	
	public abstract void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind);
}