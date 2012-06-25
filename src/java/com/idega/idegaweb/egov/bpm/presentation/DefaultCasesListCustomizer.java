package com.idega.idegaweb.egov.bpm.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.presentation.beans.CasesListCustomizer;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

public abstract class DefaultCasesListCustomizer extends DefaultSpringBean implements CasesListCustomizer {

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	protected CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO == null)
			ELUtil.getInstance().autowire(this);

		return casesBPMDAO;
	}

	protected void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	protected VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null)
			ELUtil.getInstance().autowire(this);

		return variablesQuerier;
	}

	protected void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	protected String getLocalizedHeader(IWResourceBundle iwrb, String key) {
		return iwrb.getLocalizedString(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX.concat(key), key);
	}

	@Override
	public List<String> getHeaders(List<String> headersKeys) {
		if (ListUtil.isEmpty(headersKeys))
			return null;

		List<String> headers = new ArrayList<String>();
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		for (String key: headersKeys) {
			headers.add(getLocalizedHeader(iwrb, key));
		}
		return headers;
	}

	private MultipleSelectionVariablesResolver getResolver(String name) {
		Map<String, Boolean> resolvers = getCache("multipleSelectionVariablesResolverCache");
		String key = MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + name;
		Boolean validResolver = resolvers.get(key);
		MultipleSelectionVariablesResolver resolver = null;
		if (validResolver == null) {
			try {
				resolver = ELUtil.getInstance().getBean(key);
			} catch (Exception e) {}
			validResolver = resolver != null;
			resolvers.put(key, validResolver);
		}

		if (resolver != null)
			return resolver;

		if (validResolver != null && validResolver)
			return ELUtil.getInstance().getBean(key);

		return null;
	}

	protected AdvancedProperty getLabel(VariableInstanceInfo variable) {
		String name = variable.getName();

		MultipleSelectionVariablesResolver resolver = getResolver(name);
		if (resolver != null)
			return new AdvancedProperty(name, resolver.getPresentation(variable.getValue().toString()));

		return new AdvancedProperty(name, variable.getValue().toString());
	}

	@Override
	public Map<String, Map<String, String>> getLabelsForHeaders(List<String> casesIds, List<String> headersKeys) {
		if (ListUtil.isEmpty(casesIds) || ListUtil.isEmpty(headersKeys))
			return null;

		List<Integer> ids = new ArrayList<Integer>(casesIds.size());
		for (String caseId: casesIds) {
			ids.add(Integer.valueOf(caseId));
		}
		List<CaseProcInstBind> binds = getCasesBPMDAO().getCasesProcInstBindsByCasesIds(ids);
		if (ListUtil.isEmpty(binds))
			return null;
		Map<Long, String> procIds = new HashMap<Long, String>();
		for (CaseProcInstBind bind: binds) {
			procIds.put(bind.getProcInstId(), String.valueOf(bind.getCaseId()));
		}

		Map<Long, List<VariableInstanceInfo>> vars = getVariablesQuerier().getGroupedVariables(
				getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(headersKeys, procIds.keySet(), false, false, false));
		if (MapUtil.isEmpty(vars))
			return null;

		Map<String, Map<String, String>> labels = new HashMap<String, Map<String,String>>();
		for (Long procId: vars.keySet()) {
			String caseId = procIds.get(procId);
			if (StringUtil.isEmpty(caseId))
				continue;

			Map<String, String> caseLabels = labels.get(caseId);
			if (caseLabels == null) {
				caseLabels = new HashMap<String, String>();
				labels.put(caseId, caseLabels);
			}

			List<VariableInstanceInfo> procVars = vars.get(procId);
			if (ListUtil.isEmpty(procVars))
				continue;

			for (VariableInstanceInfo info: procVars) {
				Serializable value = info.getValue();
				if (value == null)
					continue;

				AdvancedProperty label = getLabel(info);
				caseLabels.put(label.getId(), label.getValue());
			}
		}

		return labels;
	}

}