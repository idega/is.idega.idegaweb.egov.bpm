package com.idega.idegaweb.egov.bpm.cases.data;

import java.util.List;

import com.idega.core.persistence.impl.GenericDaoImpl;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/05 19:32:16 $ by $Author: civilis $
 */
public class CasesBPMDAOImpl extends GenericDaoImpl implements CasesBPMDAO {

	public List<CasesBPMBind> getAllCasesJbpmBinds() {

		@SuppressWarnings("unchecked")
		List<CasesBPMBind> binds = getEntityManager().createNamedQuery(CasesBPMBind.CASES_PROCESSES_GET_ALL_QUERY_NAME).getResultList();

		return binds;
	}
	
	public List<Object[]> getCasesProcessDefinitions() {
		
		@SuppressWarnings("unchecked")
		List<Object[]> casesProcesses = getEntityManager().createNamedQuery(CasesBPMBind.CASES_PROCESSES_DEFINITIONS_QUERY_NAME)
		.getResultList();
		
		return casesProcesses;
	}
}