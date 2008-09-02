package com.idega.idegaweb.egov.bpm.data;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.persistence.Param;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.expression.ELUtil;


/**
 * TODO: it would be very good if this would be jpa entity
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/02 12:55:32 $ by $Author: civilis $
 */
public class AppProcBindDefinition extends ModuleDefinition {

	private static final long serialVersionUID = 5796559212060985520L;

	private List<AppSupports> appSupports;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	public List<AppSupports> getAppSupports() {
		
		if(appSupports == null) {
			
			if(casesBPMDAO == null)
				ELUtil.getInstance().autowire(this);
			
			appSupports = getCasesBPMDAO().getResultListByInlineQuery(
					"select asup from "+AppSupports.class.getName()+" asup where asup."+AppSupports.appProcBindDefinitionIdProperty+" = :"+AppSupports.appProcBindDefinitionIdProperty+" ",
					AppSupports.class, new Param(AppSupports.appProcBindDefinitionIdProperty, getId()));
		}
		
		return appSupports;
	}
	
	public List<AppSupports> getAppSupports(Integer applicationId) {
		
		if(appSupports == null || true) {
			
			appSupports = getCasesBPMDAO().getResultListByInlineQuery(
					"select asup from "+AppSupports.class.getName()+" asup where asup."+AppSupports.appProcBindDefinitionIdProperty+" = :"+AppSupports.appProcBindDefinitionIdProperty+
					" and asup."+AppSupports.applicationIdProperty+" = :"+AppSupports.applicationIdProperty+" and asup."+AppSupports.roleKeyProperty+" is not null",
					AppSupports.class, 
					new Param(AppSupports.appProcBindDefinitionIdProperty, getId()),
					new Param(AppSupports.applicationIdProperty, applicationId)
			);
		}
		
		return appSupports;
	}
	
	public List<String> getRolesCanStartProcess(Integer applicationId) {
		
		final List<AppSupports> sups = getAppSupports(applicationId);
		final ArrayList<String> rolesKeys;
		
		if(sups != null && !sups.isEmpty()) {
			
			rolesKeys = new ArrayList<String>(sups.size());
			
			for (AppSupports sup : sups) {
				rolesKeys.add(sup.getRoleKey());
			}
		} else
			rolesKeys = null;
		
		return rolesKeys;
	}
	
	public void updateRolesCanStartProcess(Integer applicationId, List<String> rolesKeys) {
		
		List<AppSupports> sups = getAppSupports(applicationId);
		
		if(rolesKeys != null && !rolesKeys.isEmpty()) {

			ArrayList<String> rolesToAdd = new ArrayList<String>(rolesKeys);
			
//			update
			for (AppSupports appSupports : sups) {
				
				if(!rolesKeys.contains(appSupports.getRoleKey())) {
					getCasesBPMDAO().mergeRemove(appSupports);
				} else
//					removing from roles to add list, as we already got this
					rolesToAdd.remove(appSupports.getRoleKey());
			}
			
			for (String roleToAdd : rolesToAdd) {
				
				AppSupports sup = new AppSupports();
				sup.setApplicationId(applicationId);
				sup.setAppProcBindDefinitionId(getId());
				sup.setRoleKey(roleToAdd);
				
				getCasesBPMDAO().persist(sup);
			}
			
		} else {
			
//			remove if exists
			
			if(sups != null)
				for (AppSupports appSupports : sups) {
					
					getCasesBPMDAO().remove(appSupports);
				}
		}
	}

	public ModuleInstance createInstance() {
		return null;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		
		if(casesBPMDAO == null)
			ELUtil.getInstance().autowire(this);
		
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}