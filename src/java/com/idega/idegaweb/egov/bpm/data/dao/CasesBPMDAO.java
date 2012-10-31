package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jbpm.graph.exe.Token;

import com.idega.core.persistence.GenericDao;
import com.idega.core.user.data.User;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $ Last modified: $Date: 2009/07/06 16:55:26 $ by $Author: laddi $
 */
public interface CasesBPMDAO extends GenericDao {

	public abstract List<CaseTypesProcDefBind> getAllCaseTypes();

	public abstract CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId);

	public abstract CaseProcInstBind getCaseProcInstBindByProcessInstanceId(Long processInstanceId);

	public abstract List<CaseProcInstBind> getCasesProcInstBindsByCasesIds(List<Integer> casesIds);

	public abstract List<CaseProcInstBind> getCasesProcInstBindsByProcInstIds(List<Long> procInstIds);

	public abstract ProcessUserBind getProcessUserBind(long processInstanceId, int userId, boolean createIfNotFound);

	public abstract List<ProcessUserBind> getProcessUserBinds(int userId, Collection<Integer> casesIds);

	public abstract CaseTypesProcDefBind getCaseTypesProcDefBindByPDName(String processDefinitionName);

	public abstract void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind);

	public abstract CaseProcInstBind getCaseProcInstBindLatestByDateQN(Date date);

	public abstract CaseProcInstBind getLastCreatedCaseProcInstBind();

	public abstract List<Object[]> getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(Collection<Date> dates, Collection<Integer> identifierIDs);

	public abstract List<Token> getCaseProcInstBindSubprocessBySubprocessName(Long processInstanceId);

	public abstract List<Long> getCaseIdsByProcessDefinition(String processDefinitionName);

	public abstract List<Long> getCaseIdsByProcessDefinitionNameAndVariables(String processDefinitionName, List<BPMProcessVariable> variables);

	public abstract List<Long> getCaseIdsByCaseNumber(String caseNumber);

	public abstract List<Long> getCaseIdsByProcessUserStatus(String status);

	public abstract List<Long> getCaseIdsByCaseStatus(String[] statuses);

	public abstract List<Long> getCaseIdsByUserIds(String userId);

	public abstract List<Long> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo);

	public abstract List<Long> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds);
	public List<Integer> getCasesIdsByProcInstIds(List<Long> procInstIds);

	public List<Object[]> getCaseProcInstBindProcessInstanceByCaseIdentifier(Collection<String> identifiers);

	public abstract List<Integer> getOpenCasesIds(User user, List<String> caseCodes, List<String> caseStatuses, List<String> caseStatusesToHide,
	        Collection<Integer> groups, Collection<String> roles, boolean onlySubscribedCases, Integer caseId, List<Long> procInstIds);

	public abstract List<Integer> getClosedCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, Collection<Integer> groups,
			Collection<String> roles, boolean onlySubscribedCases, Integer caseId, List<Long> procInstIds);

	/**
	 * @param user
	 * @param caseStatuses
	 * @param caseStatusesToHide
	 * @return cases of not ended processes (end_ is null) whose user provided is handler of, or
	 *         what user is watching
	 */
	public abstract List<Integer> getMyCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, boolean onlySubscribedCases,
			Integer caseId, List<Long> procInstIds);

	public abstract List<Integer> getUserCasesIds(User user, List<String> caseStatuses, List<String> caseStatusesToHide, List<String> caseCodes,
			Collection<String> roles, boolean onlySubscribedCases, Integer caseId, List<Long> procInstIds);

	public abstract List<Integer> getPublicCasesIds(List<String> caseStatuses, List<String> caseStatusesToHide, List<String> caseCodes,
			Integer caseId, List<Long> procInstIds);

	public abstract List<Integer> getCasesIdsByStatusForAdmin(
	        List<String> caseStatuses, List<String> caseStatusesToHide);

	public List<Integer> getOpenCasesIdsForAdmin(List<String> caseCodes, List<String> caseStatusesToShow, List<String> caseStatusesToHide,
			Integer caseId, List<Long> procInstIds);

	public List<Integer> getClosedCasesIdsForAdmin(List<String> caseStatusesToShow, List<String> caseStatusesToHide, Integer caseId,
			List<Long> procInstIds);

	public List<Long> getProcessInstancesByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses, List<String> procDefNames);

	public Long getProcessInstanceIdByCaseSubject(String subject);

	public List<Integer> getCasesIdsByHandlersAndProcessDefinition(List<Integer> handlersIds, String procDefName);

	public List<Long> getProcessInstanceIdsForSubscribedCases(com.idega.user.data.User user);
	public List<Long> getProcessInstanceIdsForSubscribedCases(Integer userId);
	public List<Long> getProcessInstanceIdsForSubscribedCases(Integer userId, List<Long> procInstIds);

	public List<Long> getProcessInstanceIdsByUserAndProcessDefinition(com.idega.user.data.User user, String processDefinitionName);

	public boolean doSubscribeToCasesByProcessDefinition(com.idega.user.data.User user, String processDefinitionName);
	public boolean doSubscribeToCasesByProcessInstanceIds(com.idega.user.data.User user, List<Long> procInstIds);
	public boolean doUnSubscribeFromCasesByProcessDefinition(com.idega.user.data.User user, String processDefinitionName);
}