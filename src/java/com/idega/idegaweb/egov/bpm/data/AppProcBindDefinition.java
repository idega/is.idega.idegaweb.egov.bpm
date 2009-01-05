package com.idega.idegaweb.egov.bpm.data;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.persistence.Param;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

/**
 * TODO: it would be very good if this would be jpa entity
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2009/01/05 04:35:35 $ by $Author: valdas $
 */
public class AppProcBindDefinition extends ModuleDefinition {

	private static final long serialVersionUID = -8552580976812536983L;
	@Autowired
	private CasesBPMDAO casesBPMDAO;

	public List<AppSupports> getAppSupports(Integer applicationId) {

		List<AppSupports> appSupports = getCasesBPMDAO().getResultList(
				AppSupports.getSetByProcessNameApplicationId,
				AppSupports.class,
				new Param(AppSupports.processNameProperty,
						getProcessDefinition().getName()),
				new Param(AppSupports.applicationIdProperty, applicationId));

		return appSupports;
	}

	public List<String> getRolesCanStartProcess(Integer applicationId) {

		final List<AppSupports> appSupports = getAppSupports(applicationId);
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

	public void updateRolesCanStartProcess(Integer applicationId,
			List<String> rolesKeys) {

		List<AppSupports> sups = getAppSupports(applicationId);

		if (!ListUtil.isEmpty(rolesKeys)) {

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

			String processName = getProcessDefinition().getName();

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

	@Override
	public ModuleInstance createInstance() {
		return null;
	}

	CasesBPMDAO getCasesBPMDAO() {

		if (casesBPMDAO == null)
			ELUtil.getInstance().autowire(this);

		return casesBPMDAO;
	}
}