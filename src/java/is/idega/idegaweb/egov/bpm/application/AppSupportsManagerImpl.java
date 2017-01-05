package is.idega.idegaweb.egov.bpm.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.idegaweb.egov.bpm.data.AppSupports;
import com.idega.jbpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.util.ListUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/29 13:39:10 $ by $Author: civilis $
 */
@Service
@Scope("prototype")
public class AppSupportsManagerImpl implements AppSupportsManager {
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	private String processName;
	private Integer applicationId;
	
	List<AppSupports> getAppSupports() {
		
		Integer applicationId = getApplicationId();
		
		List<AppSupports> appSupports = getCasesBPMDAO().getResultList(
		    AppSupports.getSetByApplicationId, AppSupports.class,
		    new Param(AppSupports.applicationIdProperty, applicationId));
		
		return appSupports;
	}
	
	@Transactional(readOnly = true)
	public List<String> getRolesCanStartProcess() {
		
		final List<AppSupports> appSupports = getAppSupports();
		final ArrayList<String> rolesKeys;
		
		if (!appSupports.isEmpty()) {
			
			rolesKeys = new ArrayList<String>(appSupports.size());
			
			for (AppSupports appSupportz : appSupports) {
				
				rolesKeys.add(appSupportz.getRoleKey());
			}
			
		} else {
			rolesKeys = null;
		}
		
		return rolesKeys;
	}
	
	@Transactional
	public void updateRolesCanStartProcess(List<String> rolesKeys) {
		
		List<AppSupports> sups = getAppSupports();
		
		if (!ListUtil.isEmpty(rolesKeys)) {
			
			Integer applicationId = getApplicationId();
			
			JBPMConstants.bpmLogger
			        .finer("Updating roles, that can start process for application="
			                + applicationId
			                + ". Current roles={"
			                + sups
			                + "}, gonna update to={" + rolesKeys + "}");
			
			ArrayList<String> rolesToAdd = new ArrayList<String>(rolesKeys);
			
			if (sups != null) {
				
				// remove roles, that shouldn't exist anymore
				for (AppSupports appSupports : sups) {
					
					if (!rolesKeys.contains(appSupports.getRoleKey())) {
						getCasesBPMDAO().mergeRemove(appSupports);
					} else
						// removing from roles to add list, as we already got
						// this
						rolesToAdd.remove(appSupports.getRoleKey());
				}
			}
			
			String processName = getProcessName();
			
			for (String roleToAdd : rolesToAdd) {
				
				AppSupports sup = new AppSupports();
				sup.setApplicationId(applicationId);
				sup.setProcessName(processName);
				sup.setRoleKey(roleToAdd);
				
				getCasesBPMDAO().persist(sup);
			}
			
		} else {
			
			// remove if exists
			if (sups != null)
				for (AppSupports appSupports : sups) {
					
					getCasesBPMDAO().remove(appSupports);
				}
		}
	}
	
	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}
	
	public void setProcessName(String processName) {
		this.processName = processName;
	}
	
	public void setApplicationId(Integer applicationId) {
		this.applicationId = applicationId;
	}
	
	String getProcessName() {
		return processName;
	}
	
	Integer getApplicationId() {
		return applicationId;
	}
}