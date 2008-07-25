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
 * @version $Revision: 1.19 $
 *
 * Last modified: $Date: 2008/07/25 13:10:38 $ by $Author: valdas $
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

	public List<Integer> getCaseIdsByProcessDefinitionIdsAndName(List<Long> processDefinitionIds, String processDefinitionName) {
		if (ListUtil.isEmpty(processDefinitionIds) || StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndName, Integer.class, new Param(CaseProcInstBind.processDefinitionIdsProp, 
				processDefinitionIds), new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName));
	}

	public List<Integer> getCaseIdsByCaseNumber(String caseNumber) {
		if (caseNumber == null || CoreConstants.EMPTY.equals(caseNumber)) {
			return new ArrayList<Integer>(0);
		}
		
		if (!caseNumber.startsWith(CoreConstants.PERCENT)) {
			caseNumber = CoreConstants.PERCENT + caseNumber;
		}
		if (!caseNumber.endsWith(CoreConstants.PERCENT)) {
			caseNumber = caseNumber + CoreConstants.PERCENT;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByCaseNumber, Integer.class, new Param(CaseProcInstBind.caseNumberProp, caseNumber));
	}

	public List<Integer> getCaseIdsByProcessUserStatus(String status) {
		if (status == null || CoreConstants.EMPTY.equals(status)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessUserStatus, Integer.class, new Param(ProcessUserBind.statusProp, status));
	}

	public List<Integer> getCaseIdsByCaseStatus(String[] statuses) {
		if (statuses == null || statuses.length == 0) {
			return null;
		}
		
		HashSet<String> statusesInSet = new HashSet<String>(statuses.length);
		for (int i = 0; i < statuses.length; i++) {
			statusesInSet.add(statuses[i]);
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByCaseStatus, Integer.class, new Param(CaseProcInstBind.caseStatusesProp, statusesInSet));
	}

	public List<Integer> getCaseIdsByUserIds(String userId) {
		if (StringUtil.isEmpty(userId)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByUserIds, Integer.class, new Param(ProcessUserBind.userIdParam, userId));
	}

	public List<Integer> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo) {
		if (dateFrom == null || dateTo == null) {
			return null;
		}

		return getResultList(CaseProcInstBind.getCaseIdsByDateRange, Integer.class, new Param(CaseProcInstBind.caseStartDateProp, dateFrom.getTimestamp().toString()),
				new Param(CaseProcInstBind.caseEndDateProp, dateTo.getTimestamp().toString()));
	}

	public List<Integer> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIds, Integer.class, new Param(CaseProcInstBind.processInstanceIdsProp, processInstanceIds));
	}
	
}