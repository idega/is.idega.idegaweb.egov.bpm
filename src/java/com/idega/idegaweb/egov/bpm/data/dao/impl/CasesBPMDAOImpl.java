package com.idega.idegaweb.egov.bpm.data.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/04/12 01:53:48 $ by $Author: civilis $
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
}