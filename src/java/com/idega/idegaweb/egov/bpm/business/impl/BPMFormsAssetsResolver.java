package com.idega.idegaweb.egov.bpm.business.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.jbpm.context.exe.variableinstance.StringInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.business.FormAssetsResolver;
import com.idega.block.form.data.XFormSubmission;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.persistence.Param;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.ListUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMFormsAssetsResolver extends DefaultSpringBean implements FormAssetsResolver {

	@Autowired
	private BPMDAO bpmDAO;

	@Override
	public List<XFormSubmission> getFilteredOutForms(IWContext iwc, List<XFormSubmission> submissions, List<String> procDefNames) {
		getLogger().warning("Not implemented!");
		return null;
	}

	@Override
	public User getOwner(XFormSubmission submission) {
		getLogger().warning("Not implemented!");
		return null;
	}

	@Override
	public List<String> getNamesOfAvailableProcesses(User user, List<String> processes) {
		if (user == null) {
			return null;
		}

		try {
			Set<String> roles = getApplication().getAccessController().getAllRolesForUser(user);
			if (ListUtil.isEmpty(roles)) {
				return null;
			}

			List<Param> params = new ArrayList<Param>();
			params.add(new Param("roles", roles));
			String query = "select distinct pd.name from " + ProcessDefinition.class.getName() + " pd, " + ProcessInstance.class.getName() + " pi, " + StringInstance.class.getName() +
					" v where v.value in (:roles) ";
			if (!ListUtil.isEmpty(processes)) {
				query += " and pd.name in (:names) ";
				params.add(new Param("names", processes));
			}
			query += " and pd.id = pi.processDefinition.id and v.processInstance.id = pi.id";

			List<String> names = bpmDAO.getResultListByInlineQuery(
					query,
					String.class,
					ArrayUtil.convertListToArray(params)
			);
			return names;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error resolving which processes are available for " + user + ", ID: " + user.getId(), e);
		}

		return null;
	}

}
