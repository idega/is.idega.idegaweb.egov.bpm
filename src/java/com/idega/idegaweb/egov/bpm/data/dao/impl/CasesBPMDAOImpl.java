package com.idega.idegaweb.egov.bpm.data.dao.impl;

import java.util.List;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.CaseUser;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/02/25 16:16:26 $ by $Author: civilis $
 */
public class CasesBPMDAOImpl extends GenericDaoImpl implements CasesBPMDAO {

	public List<CaseTypesProcDefBind> getAllCaseTypesProcDefBinds() {

		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> binds = getEntityManager().createNamedQuery(CaseTypesProcDefBind.CASES_PROCESSES_GET_ALL_QUERY_NAME).getResultList();

		return binds;
	}
	
	public List<Object[]> getCaseTypesProcessDefinitions() {
		
		@SuppressWarnings("unchecked")
		List<Object[]> casesProcesses = getEntityManager().createNamedQuery(CaseTypesProcDefBind.CASES_PROCESSES_DEFINITIONS_QUERY_NAME)
		.getResultList();
		
		return casesProcesses;
	}
	
	public CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId) {
		
		return (CaseProcInstBind)getEntityManager().createNamedQuery(CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME)
		.setParameter(CaseProcInstBind.caseIdParam, caseId)
		.getSingleResult();
	}
	
	public CaseUser getCaseUser(long processInstanceId, int userId, boolean createIfNotFound) {
		
		System.out.println("_________________x");
		
		@SuppressWarnings("unchecked")
		List<CaseUser> u = getEntityManager().createNamedQuery(CaseUser.byUserIdNPID)
		.setParameter(CaseUser.pidParam, processInstanceId)
		.setParameter(CaseUser.userIdParam, userId)
		.getResultList();
		
		if(u.isEmpty())
		
		System.out.println("f_________________x");
		
		if(u.isEmpty() && createIfNotFound) {
			
			CaseUser cu = new CaseUser();
			cu.setProcessInstanceId(processInstanceId);
			cu.setUserId(userId);
			persist(cu);
			return cu;
			
		} else if(!u.isEmpty()) {
			
			return u.iterator().next();
			
		} else
			return null;
	}
}