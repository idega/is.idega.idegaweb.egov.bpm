package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jbpm.graph.exe.Token;

import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/04/21 05:09:05 $ by $Author: civilis $
 */
public interface CasesBPMDAO extends GenericDao {

	public abstract List<CaseTypesProcDefBind> getAllCaseTypes();
	
	public abstract CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId);
	
	public abstract ProcessUserBind getProcessUserBind(long processInstanceId, int userId, boolean createIfNotFound);
	
	public abstract List<ProcessUserBind> getProcessUserBinds(int userId, Collection<Integer> casesIds);
	
	public abstract CaseTypesProcDefBind getCaseTypesProcDefBindByPDName(String processDefinitionName);
	
	public abstract void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind);
	
	public abstract CaseProcInstBind getCaseProcInstBindLatestByDateQN(Date date);
	
	public abstract List<Object[]> getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(Collection<Date> dates, Collection<Integer> identifierIDs);
	
	public abstract List<Token> getCaseProcInstBindSubprocessBySubprocessName(Long processInstanceId);
}