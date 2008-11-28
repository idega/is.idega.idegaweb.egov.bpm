package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jbpm.graph.exe.Token;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.15 $
 *
 * Last modified: $Date: 2008/11/28 10:34:24 $ by $Author: valdas $
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
			List<AdvancedProperty> variables);
	
	public abstract List<Long> getCaseIdsByCaseNumber(String caseNumber);
	
	public abstract List<Long> getCaseIdsByProcessUserStatus(String status);
	
	public abstract List<Long> getCaseIdsByCaseStatus(String[] statuses);
	
	public abstract List<Long> getCaseIdsByUserIds(String userId);
	
	public abstract List<Long> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo);
	
	public abstract List<Long> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds);
	
	public abstract List<String> getVariablesByProcessDefinition(String processDefinitionName);
}