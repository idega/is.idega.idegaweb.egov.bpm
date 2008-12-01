package com.idega.idegaweb.egov.bpm.data.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.jbpm.graph.exe.Token;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.22 $
 *
 * Last modified: $Date: 2008/12/01 02:24:04 $ by $Author: valdas $
 */
@Scope("singleton")
@Repository("casesBPMDAO")
@Transactional(readOnly=true)
public class CasesBPMDAOImpl extends GenericDaoImpl implements CasesBPMDAO {

	public List<CaseTypesProcDefBind> getAllCaseTypes() {
		
		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> casesProcesses = getEntityManager().createNamedQuery(CaseTypesProcDefBind.CASES_PROCESSES_GET_ALL)
		.getResultList();
		
		return casesProcesses;
	}
	
	public CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId) {
		
		@SuppressWarnings("unchecked")
		List<CaseProcInstBind> l = getEntityManager().createNamedQuery(CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME)
		.setParameter(CaseProcInstBind.caseIdParam, caseId).getResultList();
		
		if(l.isEmpty())
			return null;
		
		return l.iterator().next();
	}
	
	@Transactional(readOnly = false)
	public ProcessUserBind getProcessUserBind(long processInstanceId, int userId, boolean createIfNotFound) {
		
		@SuppressWarnings("unchecked")
		List<ProcessUserBind> u = getEntityManager().createNamedQuery(ProcessUserBind.byUserIdNPID)
		.setParameter(ProcessUserBind.pidParam, processInstanceId)
		.setParameter(ProcessUserBind.userIdParam, userId)
		.getResultList();
		
		if(u.isEmpty() && createIfNotFound) {

			CaseProcInstBind bind = find(CaseProcInstBind.class, processInstanceId);
			
			if(bind != null) {
			
				ProcessUserBind cu = new ProcessUserBind();
				cu.setCaseProcessBind(bind);
				cu.setUserId(userId);
				persist(cu);
				return cu;
				
			} else
				throw new IllegalStateException("Case not bound to process instance");
			
		} else if(!u.isEmpty()) {
			
			return u.iterator().next();
			
		} else
			return null;
	}
	
	public List<ProcessUserBind> getProcessUserBinds(int userId, Collection<Integer> casesIds) {
		
		if(casesIds.isEmpty())
			return new ArrayList<ProcessUserBind>(0);
		
		@SuppressWarnings("unchecked")
		List<ProcessUserBind> u = getEntityManager().createNamedQuery(ProcessUserBind.byUserIdAndCaseId)
		.setParameter(ProcessUserBind.userIdParam, userId)
		.setParameter(ProcessUserBind.casesIdsParam, casesIds)
		.getResultList();
		
		return u;
	}
	
	public CaseTypesProcDefBind getCaseTypesProcDefBindByPDName(String pdName) {
		
		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> u = getEntityManager().createNamedQuery(CaseTypesProcDefBind.CASES_PROCESSES_GET_BY_PDNAME)
		.setParameter(CaseTypesProcDefBind.procDefNamePropName, pdName)
		.getResultList();
		
		if(!u.isEmpty())
			return u.iterator().next();
		
		return null;
	}
	
	@Transactional(readOnly = false)
	public void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind) {

		getEntityManager().merge(bind);
	}
	
	public CaseProcInstBind getCaseProcInstBindLatestByDateQN(Date date) {
		
		CaseProcInstBind b = null;
		
		if(date != null) {
		
			@SuppressWarnings("unchecked")
			List<CaseProcInstBind> u = getEntityManager().createNamedQuery(CaseProcInstBind.getLatestByDateQN)
			.setParameter(CaseProcInstBind.dateCreatedProp, date)
			.getResultList();
			
			if(!u.isEmpty())
				b = u.iterator().next();
		}
		
		return b;
	}
	
	public List<Object[]> getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(Collection<Date> dates, Collection<Integer> identifierIDs) {
		
		List<Object[]> cps = null;
		
		if(dates != null && !dates.isEmpty() && identifierIDs != null && !identifierIDs.isEmpty()) {
		
			@SuppressWarnings("unchecked")
			List<Object[]> u = getEntityManager().createNamedQuery(CaseProcInstBind.getByDateCreatedAndCaseIdentifierId)
			.setParameter(CaseProcInstBind.dateCreatedProp, dates)
			.setParameter(CaseProcInstBind.caseIdentierIDProp, identifierIDs)
			.getResultList();
			
			cps = u;
		} else
			cps = new ArrayList<Object[]>(0);
		
		return cps;
	}
	
	public List<Token> getCaseProcInstBindSubprocessBySubprocessName(Long processInstanceId) {
		
		if(processInstanceId != null) {
		
			@SuppressWarnings("unchecked")
			List<Token> u = getEntityManager().createNamedQuery(CaseProcInstBind.getSubprocessTokensByPI)
			.setParameter(CaseProcInstBind.procInstIdProp, processInstanceId)
			.getResultList();
			
			return u;
			
		} else
			return new ArrayList<Token>(0);
	}

	public List<Long> getCaseIdsByProcessDefinitionIdsAndNameAndVariables(List<Long> processDefinitionIds, String processDefinitionName,
			List<AdvancedProperty> variables) {
		if (ListUtil.isEmpty(processDefinitionIds) || StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}
		
		if (ListUtil.isEmpty(variables)) {
			return getResultList(CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndName, Long.class, new Param(CaseProcInstBind.processDefinitionIdsProp,
					processDefinitionIds), new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName));
		}
		
		HashSet<String> variablesNames = new HashSet<String>(variables.size());
		HashSet<String> variablesValues = new HashSet<String>(variables.size());
		for (AdvancedProperty variable: variables) {
			variablesNames.add(variable.getId());
			variablesValues.add(variable.getValue());
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndVariables, Long.class,
				new Param(CaseProcInstBind.processDefinitionIdsProp, processDefinitionIds),
				new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName),
				new Param(CaseProcInstBind.variablesNamesProp, variablesNames),
				new Param(CaseProcInstBind.variablesValuesProp, variablesValues)
		);
	}

	public List<Long> getCaseIdsByCaseNumber(String caseNumber) {
		if (caseNumber == null || CoreConstants.EMPTY.equals(caseNumber)) {
			return new ArrayList<Long>(0);
		}
		
		if (!caseNumber.startsWith(CoreConstants.PERCENT)) {
			caseNumber = CoreConstants.PERCENT + caseNumber;
		}
		if (!caseNumber.endsWith(CoreConstants.PERCENT)) {
			caseNumber = caseNumber + CoreConstants.PERCENT;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByCaseNumber, Long.class, new Param(CaseProcInstBind.caseNumberProp, caseNumber));
	}

	public List<Long> getCaseIdsByProcessUserStatus(String status) {
		if (status == null || CoreConstants.EMPTY.equals(status)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessUserStatus, Long.class, new Param(ProcessUserBind.statusProp, status));
	}

	public List<Long> getCaseIdsByCaseStatus(String[] statuses) {
		if (statuses == null || statuses.length == 0) {
			return null;
		}
		
		HashSet<String> statusesInSet = new HashSet<String>(statuses.length);
		for (int i = 0; i < statuses.length; i++) {
			statusesInSet.add(statuses[i]);
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByCaseStatus, Long.class, new Param(CaseProcInstBind.caseStatusesProp, statusesInSet));
	}

	public List<Long> getCaseIdsByUserIds(String userId) {
		if (StringUtil.isEmpty(userId)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByUserIds, Long.class, new Param(ProcessUserBind.userIdParam, userId));
	}

	public List<Long> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo) {
		if (dateFrom == null || dateTo == null) {
			return null;
		}

		return getResultList(CaseProcInstBind.getCaseIdsByDateRange, Long.class, new Param(CaseProcInstBind.caseStartDateProp, dateFrom.getTimestamp().toString()),
				new Param(CaseProcInstBind.caseEndDateProp, dateTo.getTimestamp().toString()));
	}

	public List<Long> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIds, Long.class, new Param(CaseProcInstBind.processInstanceIdsProp, processInstanceIds));
	}

	public List<String> getVariablesByProcessDefinition(String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getVariablesByProcessDefinitionName, String.class,
				new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName)
		);
	}
	
}