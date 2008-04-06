package com.idega.idegaweb.egov.bpm.data.dao.impl;

import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.idegaweb.egov.bpm.data.AppProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.AppBPMDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/04/06 17:53:12 $ by $Author: civilis $
 */
@Scope("singleton")
@Repository("appBPMDAO")
@Transactional(readOnly=true)
public class AppBPMDAOImpl extends GenericDaoImpl implements AppBPMDAO {
	
	public ProcessDefinition getProcessDefinitionByAppId(int appId) {
		
		@SuppressWarnings("unchecked")
		List<ProcessDefinition> u = getEntityManager().createNamedQuery(AppProcDefBind.findProcessDefByAppId)
		.setParameter(AppProcDefBind.applicationIdProp, appId)
		.getResultList();
		
		if(!u.isEmpty())
			return u.iterator().next();
		
		return null;
	}
}