package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.block.process.presentation.beans.CaseComparator;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.reflect.MethodInvoker;

import is.idega.idegaweb.egov.bpm.business.CaseListVariableCache;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;

public class BPMCaseComparator extends CaseComparator {

	private Locale locale;
	private CasesListSearchCriteriaBean searchCriterias;

	private Map<String, List<VariableInstanceInfo>> variables;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	@Autowired
	private CaseListVariableCache casesListVariableCache;

	private Collator collator;

	public BPMCaseComparator(Locale locale, CasesListSearchCriteriaBean searchCriterias) {
		variables = new HashMap<String, List<VariableInstanceInfo>>();

		this.locale = locale;
		this.searchCriterias = searchCriterias;

		collator = Collator.getInstance(this.locale);
	}

	@Override
	public int compare(Integer case1, Integer case2) {
		StringBuilder compareValue1 = new StringBuilder();
		StringBuilder compareValue2 = new StringBuilder();

		boolean datesUsed = false;
		String getCreatedSortingOption = "getCreated";
		for (AdvancedProperty sortingOption: searchCriterias.getSortingOptions()) {
			String sortingOptionValue = sortingOption.getId();
			datesUsed = getCreatedSortingOption.equals(sortingOptionValue);

			compareValue1.append(getSortableFieldValue(case1, sortingOptionValue));
			compareValue2.append(getSortableFieldValue(case2, sortingOptionValue));
		}

		int result = (StringUtil.isEmpty(compareValue1.toString()) && StringUtil.isEmpty(compareValue2.toString())) ? 0 :
			collator.compare(compareValue1.toString(), compareValue2.toString());
		return datesUsed ? -1 * result : result;
	}

	private String getSortableFieldValue(Integer theCase, String sortableIdentifier) {
		if (theCase == null || StringUtil.isEmpty(sortableIdentifier)) {
			Logger.getLogger(getClass().getName()).warning("Some parameters are not provided: case id: " + theCase + ", method: " + sortableIdentifier);
			return CoreConstants.EMPTY;
		}

		if (isDefaultField(sortableIdentifier) || ("getCaseStatusLocalized".equals(sortableIdentifier))) {
			Case case_ = getCaseFromCache(theCase);
			if (case_ == null) {
				try {
					case_ = getCaseBusiness().getCase(theCase);
				} catch (RemoteException | FinderException e) {
				}
			}
			if ("getCaseStatusLocalized".equals(sortableIdentifier)){
				return StringUtil.isEmpty(case_.getStatus()) ? CoreConstants.EMPTY : getStatusFromCache(case_.getStatus());
			}
			if (case_ != null) {
				try {
					return MethodInvoker.getInstance().invokeMethodWithNoParameters(case_, sortableIdentifier).toString();
				} catch(Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error invoking " + sortableIdentifier, e);
				}
			}
		}
		else {
			return getVariableValue(theCase, sortableIdentifier);
		}

		return CoreConstants.EMPTY;
	}

	private String getStatusFromCache(String status) {
		Object ch =  getCasesListVariableCache().getCache(CasesEngineImp.STATUS_MAP_CACHE);
		if (ch != null && ch instanceof Map<?, ?>) {
			Map<String, String> cache = (Map<String, String>)ch;
			return StringUtil.isEmpty(cache.get(status)) ? CoreConstants.EMPTY : cache.get(status);
		}
		return null;
	}

	private Case getCaseFromCache(Integer theCase) {
		Object ch =  getCasesListVariableCache().getCache(CasesEngineImp.CASE_DATA_CACHE_NAME);
		if (ch != null && ch instanceof Map<?, ?>) {
			Map<Integer, Case> cache = (Map<Integer, Case>)ch;
			return cache.get(theCase);
		}
		return null;
	}

	private VariableInstanceInfo getVariableFromCache(Integer theCase, String variable) {
		Object ch =  getCasesListVariableCache().getCache(CasesEngineImp.CASE_PROC_ID_BIND);
		if (ch != null && ch instanceof Map<?, ?>) {
			Map<Integer, Long> bindCache = (Map<Integer, Long>)ch;
			Long procInstId = bindCache.get(theCase);
			Object ch2 =  getCasesListVariableCache().getCache(CasesEngineImp.VARIABLE_INFO_CACHE_NAME);
			if (ch2 != null && ch2 instanceof Map<?, ?>) {
				Map<?, ?> procVarCache = (Map<?, ?>)ch2;
				Object ch3 = procVarCache.get(procInstId);
				if (ch3 != null && ch3 instanceof Map<?, ?>) {
					Map<String, VariableInstanceInfo> varVarInfo = (Map<String, VariableInstanceInfo>)ch3;
					return varVarInfo.get(variable);
				}
			}
		}
		return null;
	}

	private boolean isDefaultField(String methodName) {
		if (StringUtil.isEmpty(methodName)) {
			return false;
		}

		for (Method method: Case.class.getMethods()) {
			if (methodName.equals(method.getName())) {
				return true;
			}
		}
		return false;
	}

	private List<VariableInstanceInfo> getCaseVariables(String caseId) {
		List<VariableInstanceInfo> variables = this.variables.get(caseId);
		if (variables == null) {
			CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByCaseId(Integer.valueOf(caseId));
			Long piId = cpi == null ? null : cpi.getProcInstId();
			Collection<VariableInstanceInfo> tmpVariables = piId == null ? null : getVariablesQuerier().getFullVariablesByProcessInstanceId(piId);

			if (tmpVariables == null) {
				variables = new ArrayList<VariableInstanceInfo>(0);
			} else {
				variables = new ArrayList<VariableInstanceInfo>(tmpVariables);
			}
			this.variables.put(caseId, variables);
		}
		return variables;
	}

	@Transactional(readOnly=true)
	private String getVariableValue(Integer theCase, String variableName) {
		try {
			String value = null;
			VariableInstanceInfo var = getVariableFromCache(theCase, variableName);
			if (var != null){
				MultipleSelectionVariablesResolver resolver = getResolver(variableName);
				if (resolver != null){
					value = resolver.getPresentation(var);
				} else {
					Serializable tmpValue = var.getValue();
					if (tmpValue != null) {
						value = tmpValue.toString();
					}
				}
			} else {
				List<VariableInstanceInfo> variables = getCaseVariables(theCase.toString());
				if (ListUtil.isEmpty(variables)) {
					return CoreConstants.EMPTY;
				}

				for (Iterator<VariableInstanceInfo> variablesIter = variables.iterator(); (variablesIter.hasNext() && StringUtil.isEmpty(value));) {
					VariableInstanceInfo variable = variablesIter.next();

					String name = variable.getName();
					if (variableName.equals(name)) {
						MultipleSelectionVariablesResolver resolver = getResolver(variableName);
						if (resolver != null){
							value = resolver.getPresentation(variable);
						} else {
							Serializable tmpValue = variable.getValue();
							if (tmpValue != null) {
								value = tmpValue.toString();
							}
						}
					}
				}
			}
			return StringUtil.isEmpty(value) ? CoreConstants.EMPTY : value.toString();
		} catch(Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error getting variable's value: " + variableName, e);
		}
		return CoreConstants.EMPTY;
	}

	private MultipleSelectionVariablesResolver getResolver(String variableName) {
		MultipleSelectionVariablesResolver resolver = null;
		try {
			resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + variableName);
		} catch (Exception e) {}
		return resolver;
	}

	private CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMDAO;
	}

	public void setCaseBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	private CaseBusiness getCaseBusiness() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CaseListVariableCache getCasesListVariableCache() {
		if (casesListVariableCache == null) ELUtil.getInstance().autowire(this);
		return casesListVariableCache;
	}

	public void setCasesListVariableCache(CaseListVariableCache casesListVariableCache) {
		this.casesListVariableCache = casesListVariableCache;
	}

}