package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CasesBPMBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/06 11:49:26 $ by $Author: civilis $
 */
public interface CasesBPMDAO extends GenericDao {

	public abstract List<CasesBPMBind> getAllCasesJbpmBinds();
	
	public abstract List<Object[]> getCasesProcessDefinitions();
}