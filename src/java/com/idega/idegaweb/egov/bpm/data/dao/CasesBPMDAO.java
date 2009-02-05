package com.idega.idegaweb.egov.bpm.data.dao;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMProcessVariable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.Token;

import com.idega.core.persistence.GenericDao;
import com.idega.core.user.data.User;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $
 *
 * Last modified: $Date: 2009/02/05 12:33:20 $ by $Author: donatas $
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
	
	public abstract List<Long> getCaseIdsByProcessDefinitionIdsAndNameAndVariables(List<Long> processDefinitionIds, String processDefinitionName,
			List<BPMProcessVariable> variables);
	
	public abstract List<Long> getCaseIdsByCaseNumber(String caseNumber);
	
	public abstract List<Long> getCaseIdsByProcessUserStatus(String status);
	
	public abstract List<Long> getCaseIdsByCaseStatus(String[] statuses);
	
	public abstract List<Long> getCaseIdsByUserIds(String userId);
	
	public abstract List<Long> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo);
	
	public abstract List<Long> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds);
	
	public abstract List<VariableInstance> getVariablesByProcessDefinition(String processDefinitionName);
	
	public abstract List<VariableInstance> getVariablesByProcessInstanceId(Long processInstanceId);

	public List<Object[]> getCaseProcInstBindProcessInstanceByCaseIdentifier(Collection<String> identifiers);
	
	public abstract List<Integer> getOpenCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, Collection groups,
			Collection<String> roles);
	
	public abstract List<Integer> getClosedCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, Collection groups,
			Collection<String> roles);
	
	public abstract List<Integer> getMyCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, Collection<String> roles);
	
	public abstract List<Integer> getUserCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, List<String> caseCodes, Collection<String> roles);
	
	public abstract List<Integer> getCasesIdsByStatusForAdmin(List<String> caseStatuses, List<String> caseStatusesToHide);
	
	public List<Integer> getOpenCasesIdsForAdmin(List<String> caseStatusesToShow, List<String> caseStatusesToHide);
	
	public List<Integer> getClosedCasesIdsForAdmin(List<String> caseStatusesToShow, List<String> caseStatusesToHide);
}