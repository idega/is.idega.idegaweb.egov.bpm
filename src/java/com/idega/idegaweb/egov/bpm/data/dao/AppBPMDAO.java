package com.idega.idegaweb.egov.bpm.data.dao;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.core.persistence.GenericDao;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/04/06 17:53:12 $ by $Author: civilis $
 */
public interface AppBPMDAO extends GenericDao {

	public abstract ProcessDefinition getProcessDefinitionByAppId(int appId);
}