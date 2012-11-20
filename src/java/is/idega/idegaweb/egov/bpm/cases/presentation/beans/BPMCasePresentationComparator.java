package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;

import java.io.Serializable;
import java.lang.reflect.Method;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasePresentationComparator;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.reflect.MethodInvoker;

public class BPMCasePresentationComparator extends CasePresentationComparator {

	private Locale locale;
	private CasesListSearchCriteriaBean searchCriterias;

	private Map<String, List<VariableInstanceInfo>> variables;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	private Collator collator;

	public BPMCasePresentationComparator(Locale locale, CasesListSearchCriteriaBean searchCriterias) {
		variables = new HashMap<String, List<VariableInstanceInfo>>();

		this.locale = locale;
		this.searchCriterias = searchCriterias;

		collator = Collator.getInstance(this.locale);
	}

	@Override
	public int compare(CasePresentation case1, CasePresentation case2) {
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

	private String getSortableFieldValue(CasePresentation theCase, String sortableIdentifier) {
		if (theCase == null || StringUtil.isEmpty(sortableIdentifier)) {
			Logger.getLogger(getClass().getName()).warning("Some parameters are not provided: case presentation: " + theCase + ", method: " + sortableIdentifier);
			return CoreConstants.EMPTY;
		}

		if (isDefaultField(sortableIdentifier)) {
			try {
				return MethodInvoker.getInstance().invokeMethodWithNoParameters(theCase, sortableIdentifier).toString();
			} catch(Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error invoking " + sortableIdentifier, e);
			}
		}
		else {
			return getVariableValue(theCase, sortableIdentifier);
		}

		return CoreConstants.EMPTY;
	}

	private boolean isDefaultField(String methodName) {
		if (StringUtil.isEmpty(methodName)) {
			return false;
		}

		for (Method method: CasePresentation.class.getMethods()) {
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
	private String getVariableValue(CasePresentation theCase, String variableName) {
		try {
			List<VariableInstanceInfo> variables = getCaseVariables(theCase.getId());
			if (ListUtil.isEmpty(variables)) {
				return CoreConstants.EMPTY;
			}

			String value = null;
			for (Iterator<VariableInstanceInfo> variablesIter = variables.iterator(); (variablesIter.hasNext() && StringUtil.isEmpty(value));) {
				VariableInstanceInfo variable = variablesIter.next();

				String name = variable.getName();
				if (variableName.equals(name)) {
					Serializable tmpValue = variable.getValue();
					if (tmpValue != null) {
						value = tmpValue.toString();
					}
				}
			}

			return StringUtil.isEmpty(value) ? CoreConstants.EMPTY : value.toString();
		} catch(Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error getting variable's value: " + variableName, e);
		}
		return CoreConstants.EMPTY;
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

}