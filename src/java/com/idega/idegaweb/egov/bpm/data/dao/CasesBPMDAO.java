package com.idega.idegaweb.egov.bpm.data.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/12 14:37:23 $ by $Author: civilis $
 */
public interface CasesBPMDAO extends GenericDao {

	public abstract List<CaseTypesProcDefBind> getAllCaseTypesProcDefBinds();
	
	public abstract List<Object[]> getCaseTypesProcessDefinitions();
}