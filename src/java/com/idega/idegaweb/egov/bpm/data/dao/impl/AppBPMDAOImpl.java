package com.idega.idegaweb.egov.bpm.data.dao.impl;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.idegaweb.egov.bpm.data.dao.AppBPMDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/04/12 01:53:48 $ by $Author: civilis $
 */
@Scope("singleton")
@Repository("appBPMDAO")
@Transactional(readOnly=true)
public class AppBPMDAOImpl extends GenericDaoImpl implements AppBPMDAO {
	
}