package com.idega.idegaweb.egov.bpm.presentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.idega.block.process.presentation.beans.CaseListPropertiesBean;
import com.idega.block.process.presentation.beans.CasesListCustomizer;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

public abstract class DefaultCasesListCustomizer extends DefaultSpringBean implements CasesListCustomizer {

	@Autowired
	@Lazy
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	protected CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return casesBPMDAO;
	}

	protected void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	protected VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}

		return variablesQuerier;
	}

	protected void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	protected String getLocalizedHeader(IWResourceBundle iwrb, String key) {
		return iwrb.getLocalizedString(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX.concat(key), key);
	}

	protected void setEmptyValues(Map<String, Map<String, String>> labels, Map<String, List<String>> missingLabels) {
		for (String caseId: missingLabels.keySet()) {
			List<String> varNames = missingLabels.get(caseId);
			if (ListUtil.isEmpty(varNames)) {
				continue;
			}

			Map<String, String> caseLabels = labels.get(caseId);
			for (String varName: varNames) {
				String realValue = caseLabels.get(varName);
				caseLabels.put(varName, StringUtil.isEmpty(realValue) || varName.equals(realValue) ? CoreConstants.MINUS : realValue);
			}
		}
	}

	@Override
	public List<String> getHeaders(List<String> headersKeys) {
		if (ListUtil.isEmpty(headersKeys)) {
			return null;
		}

		List<String> headers = new ArrayList<>();
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		for (String key: headersKeys) {
			headers.add(getLocalizedHeader(iwrb, key));
		}
		return headers;
	}

	@Override
	public Map<String, String> getHeadersAndVariables(CaseListPropertiesBean properties, List<String> headersKeys) {
		if (ListUtil.isEmpty(headersKeys)) {
			return null;
		}

		Map<String, String> headers = new LinkedHashMap<>(headersKeys.size());
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		for (String key: headersKeys) {
			headers.put(key, getLocalizedHeader(iwrb, key));
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

		if (resolver != null) {
			return resolver;
		}

		if (validResolver != null && validResolver) {
			return ELUtil.getInstance().getBean(key);
		}

		return null;
	}

	protected AdvancedProperty getLabel(VariableInstanceInfo variable, Map<Long, Integer> procInstIdsAndCasesIds) {
		String name = variable.getName();

		MultipleSelectionVariablesResolver resolver = getResolver(name);
		if (resolver != null) {
			resolver.setProcInstIdsAndCasesIds(procInstIdsAndCasesIds);
			return new AdvancedProperty(name, resolver.isValueUsedForCaseList() ?
					resolver.getPresentation(variable.getValue().toString()) :
					resolver.getKeyPresentation((Long) variable.getProcessInstanceId(), variable.getValue().toString())
			);
		}

		return new AdvancedProperty(name, variable.getValue().toString());
	}

	@Override
	public Map<String, Map<String, String>> getLabelsForHeaders(List<String> casesIds, List<String> headersKeys) {
		if (ListUtil.isEmpty(casesIds) || ListUtil.isEmpty(headersKeys)) {
			getLogger().warning("There are no cases IDs or/and headers provided");
			return null;
		}

		List<Integer> tmp = new ArrayList<>();
		for (String caseId: casesIds) {
			if (StringHandler.isNumeric(caseId)) {
				tmp.add(Integer.valueOf(caseId));
			}
		}
		Map<Long, Integer> procInstIdsAndCasesIds = getCasesBPMDAO().getProcessInstancesAndCasesIdsByCasesIds(tmp);
		if (MapUtil.isEmpty(procInstIdsAndCasesIds)) {
			return null;
		}

		Map<String, Map<String, String>> labels = new LinkedHashMap<>();

		Map<Long, List<VariableInstanceInfo>> vars = getVariablesQuerier().getGroupedVariables(getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(procInstIdsAndCasesIds.keySet(), headersKeys));
		if (MapUtil.isEmpty(vars)) {
			getLogger().warning("There are no values for cases " + casesIds + " and variables " + headersKeys);

		} else {
			//	Resolving labels
			Map<String, Long> mappings = new HashMap<>();
			for (Long procId: vars.keySet()) {
				List<VariableInstanceInfo> procVars = vars.get(procId);
				if (ListUtil.isEmpty(procVars)) {
					continue;
				}

				Map<String, String> caseLabels = null;
				for (VariableInstanceInfo info: procVars) {
					Integer caseIdTmp = procInstIdsAndCasesIds.get(procId);
					if (caseIdTmp == null) {
						continue;
					}

					String caseId = caseIdTmp.toString();
					if (StringUtil.isEmpty(caseId)) {
						continue;
					}

					mappings.put(caseId, procId);

					caseLabels = labels.get(caseId);
					if (caseLabels == null) {
						caseLabels = new HashMap<>();
						labels.put(caseId, caseLabels);
					}
					if (caseLabels.containsKey(info.getName())) {
						continue;
					}

					Serializable value = info.getValue();
					if (value == null) {
						continue;
					}

					AdvancedProperty label = getLabel(info, procInstIdsAndCasesIds);
					caseLabels.put(label.getId(), label.getValue());
				}
			}
		}

		//	Checking if everything was resolved
		Map<String, List<String>> missingLabels = new HashMap<>();
		for (String caseId: labels.keySet()) {
			for (String headerKey: headersKeys) {
				Map<String, String> caseLabels = labels.get(caseId);
				if (caseLabels.containsKey(headerKey) && !StringUtil.isEmpty(caseLabels.get(headerKey))) {
					continue;
				}

				List<String> varNames = missingLabels.get(caseId);
				if (varNames == null) {
					varNames = new ArrayList<>();
					missingLabels.put(caseId, varNames);
				}
				varNames.add(headerKey);
			}
		}
		if (MapUtil.isEmpty(labels) && MapUtil.isEmpty(missingLabels) && !ListUtil.isEmpty(headersKeys)) {
			Map<String, String> noValue = new HashMap<>();
			for (String headerKey: headersKeys) {
				noValue.put(headerKey, headerKey);
			}
			for (String caseId: casesIds) {
				labels.put(caseId, MapUtil.deepCopy(noValue));
				missingLabels.put(caseId, new ArrayList<>(headersKeys));
			}
		}
		if (!MapUtil.isEmpty(missingLabels)) {
			doResolveMissingLabels(labels, missingLabels);
		}

		setEmptyValues(labels, missingLabels);

		return labels;
	}

	/**
	 * Resolves missing values
	 *
	 * @param labels: case ID -> variable name: value
	 * @param missingLabels: case ID -> variable names
	 */
	protected void doResolveMissingLabels(Map<String, Map<String, String>> labels, Map<String, List<String>> missingLabels) {
		if (MapUtil.isEmpty(labels) || MapUtil.isEmpty(missingLabels)) {
			return;
		}

		for (String caseId: missingLabels.keySet()) {
			List<String> varNames = missingLabels.get(caseId);
			if (ListUtil.isEmpty(varNames)) {
				continue;
			}

			Map<String, String> caseLabels = labels.get(caseId);
			for (String varName: varNames) {
				if (!caseLabels.containsKey(varName)) {
					caseLabels.put(varName, CoreConstants.MINUS);
				}
			}
		}
	}

	@Override
	public Map<String, String> getLocalizedStatuses(List<String> casesIds, Locale locale) {
		return null;
	}

	@Override
	public Map<String, Map<String, String>> getCustomColumnsForSearchResult(List<String> casesIds, Locale locale) {
		return null;
	}
}