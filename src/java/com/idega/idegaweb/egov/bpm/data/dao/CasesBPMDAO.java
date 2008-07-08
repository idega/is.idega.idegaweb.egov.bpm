package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jbpm.graph.exe.Token;

import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2008/07/08 09:13:13 $ by $Author: valdas $
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
	
	public abstract List<Integer> getCaseIdsByProcessDefinitionIdsAndName(List<Long> processDefinitionIds, String processDefinitionName);
	
	public abstract List<Integer> getCaseIdsByCaseNumber(String caseNumber);
	
	public abstract List<Integer> getCaseIdsByProcessUserStatus(String status);
	
	public abstract List<Integer> getCaseIdsByCaseStatus(String[] statuses);
	
	public abstract List<Integer> getCaseIdsByUserIds(String userId);
	
	public abstract List<Integer> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo);
	
	public abstract List<Integer> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds);
}