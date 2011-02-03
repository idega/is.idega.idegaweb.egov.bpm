package is.idega.idegaweb.egov.bpm.cases.search.impl;

import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessesFilter extends DefaultCasesListSearchFilter {

	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	@Override
	public List<Integer> getSearchResults(List<Integer> casesIds) {
		String processDefinitionId = getProcessId();
		List<Integer> casesByProcessDefinition = null;
		if (CasesConstants.GENERAL_CASES_TYPE.equals(processDefinitionId)) {
			//	Getting ONLY none "BPM" cases
			try {
				casesByProcessDefinition = getCasesBusiness().getFilteredProcesslessCasesIds(casesIds, 
						CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()));
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting non BPM cases", e);
			}
		} else {
			//	Getting "BPM" cases
			casesByProcessDefinition = getCasesByProcessDefinition(processDefinitionId, getProcessVariables());
		}
		
		if (ListUtil.isEmpty(casesByProcessDefinition)) {
			getLogger().info("No cases found by process definition id: " + processDefinitionId + " and variables: " + getProcessVariables());
		} else {
			getLogger().info("Found cases by process definition (" + processDefinitionId + ") and variables (" + getProcessVariables() + "): " + casesByProcessDefinition);
		}
			
		return casesByProcessDefinition;
	}
	
	@Override
	protected String getInfo() {
		return "Looking for cases by process definition: " + getProcessId();
	}
	
	private List<Integer> getCasesByProcessDefinition(String processDefinitionId, List<BPMProcessVariable> variables) {
		if (StringUtil.isEmpty(processDefinitionId))
			return null;
		
		final Long procDefId;
		try {
			procDefId = Long.valueOf(processDefinitionId);
		} catch (NumberFormatException e) {
			getLogger().log(Level.SEVERE, "Process definition id provided ("+processDefinitionId+") was incorrect", e);
			return null;
		}
		
		List<Long> processDefinitionIds = new ArrayList<Long>(1);
		processDefinitionIds.add(procDefId);
		
		ProcessDefinition processDefinition = null;
		try {
			processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting process definition by ID: " + processDefinitionId, e);
		}
		String procDefName = processDefinition == null ? null : processDefinition.getName();
		
		Object tmp = null;
		BPMProcessVariable handlerVariable = null;
		List<BPMProcessVariable> varsToRemove = new ArrayList<BPMProcessVariable>();
		Map<String, List<Serializable>> multValues = new HashMap<String, List<Serializable>>();
		for (BPMProcessVariable variable: variables) {
			if (CaseHandlerAssignmentHandler.handlerUserIdVarName.equals(variable.getName())) {	//	Checking if variable is holding handler ID
				handlerVariable = variable;
				
			} else if ((tmp = variable.getRealValue()) instanceof Collection) {	//	Checking if value is multi-type
				Collection<?> newMultipleValues = (Collection<?>) tmp;
				List<Serializable> existingMultipleValues = multValues.get(variable.getName());
				if (existingMultipleValues == null) {
					existingMultipleValues = new ArrayList<Serializable>();
					multValues.put(variable.getName(), existingMultipleValues);
				}
				for (Object value: newMultipleValues) {
					if (value instanceof Serializable && !existingMultipleValues.contains(value)) {
						existingMultipleValues.add((Serializable) value);
						varsToRemove.add(variable);
					}
				}
			}
		}
		
		List<Integer> casesIdsByMultipleValues = null;
		if (!multValues.isEmpty()) {
			casesIdsByMultipleValues = getConvertedFromNumbers(getCasesIdsByMultipleVariableValues(procDefName, multValues));
			if (ListUtil.isEmpty(casesIdsByMultipleValues))
				return null;
		}
		
		if (handlerVariable != null) {
			List<Integer> casesIdsByHandlers = getCaseIdsByHandlers(handlerVariable.getName(), handlerVariable.getValue(), procDefName);
			if (ListUtil.isEmpty(casesIdsByHandlers))
				return null;
			
			casesIdsByMultipleValues = casesIdsByMultipleValues == null ?
					new ArrayList<Integer>(casesIdsByHandlers) :
					getNarrowedResults(casesIdsByMultipleValues, casesIdsByHandlers);
			
			variables.remove(handlerVariable);
		}
		
		if (!ListUtil.isEmpty(varsToRemove))
			variables.removeAll(varsToRemove);
		
		try {
			List<Integer> casesIdsByOtherVars = getConvertedFromNumbers(getCasesBPMDAO().getCaseIdsByProcessDefinitionIdsAndNameAndVariables(processDefinitionIds, procDefName,
					variables));
			return ListUtil.isEmpty(casesIdsByMultipleValues) ? casesIdsByOtherVars : getNarrowedResults(casesIdsByMultipleValues, casesIdsByOtherVars);
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = "+
					processDefinitionId + ", variables: " + variables, e);
		}
		
		return null;
	}
	
	private List<Long> getCasesIdsByMultipleVariableValues(String processDefinitionName, Map<String, List<Serializable>> multipleValues) {
		List<String> procDefNames = Arrays.asList(processDefinitionName);
		
		List<Long> procInstIds = new ArrayList<Long>();
		for (String variableName: multipleValues.keySet()) {
			Collection<VariableInstanceInfo> vars = getVariablesQuerier().getProcessVariablesByNameAndValue(variableName, multipleValues.get(variableName), procDefNames);
			procInstIds = getProcInstIdsFromVars(vars, procInstIds);
		}
		
		return getCasesBPMDAO().getCaseIdsByProcessInstanceIds(procInstIds);
	}
	
	private List<Long> getProcInstIdsFromVars(Collection<VariableInstanceInfo> vars) {
		return getProcInstIdsFromVars(vars, null);
	}
		
	private List<Long> getProcInstIdsFromVars(Collection<VariableInstanceInfo> vars, List<Long> ids) {
		if (ListUtil.isEmpty(vars))
			return null;
		
		ids = ids == null ? new ArrayList<Long>() : ids;
		for (VariableInstanceInfo var: vars) {
			Long procInstId = var.getProcessInstanceId();
			if (procInstId == null)
				continue;
			
			if (!ids.contains(procInstId))
				ids.add(procInstId);
		}
		return ids;
	}
	
	private List<Integer> getCaseIdsByHandlers(String varName, String handlersIds, String procDefName) {
		if (StringUtil.isEmpty(handlersIds))
			return null;
		
		String[] ids = handlersIds.split(CoreConstants.SEMICOLON);
		if (ArrayUtil.isEmpty(ids))
			return null;
		
		List<Serializable> usersIds = new ArrayList<Serializable>();
		for (String id: ids) {
			usersIds.add(Long.valueOf(id));
		}
		
		Collection<VariableInstanceInfo> vars = getVariablesQuerier().getProcessVariablesByNameAndValue(varName, usersIds, Arrays.asList(procDefName));
		if (ListUtil.isEmpty(vars))
			return null;
		
		List<Long> procInstIds = getProcInstIdsFromVars(vars);
		if (ListUtil.isEmpty(procInstIds))
			return null;
		
		return getConvertedFromNumbers(getCasesBPMDAO().getCaseIdsByProcessInstanceIds(procInstIds));
	}

	@Override
	protected String getFilterKey() {
		return new StringBuilder().append(getProcessId()).append(getProcessVariables()).toString();
	}

	@Override
	protected boolean isFilterKeyDefined() {
		String processDefinitionId = getProcessId();
		if (StringUtil.isEmpty(processDefinitionId)) {
			getLogger().info("Process is not defined, not filtering by it!");
			return false;
		}
		return true;
	}
	
	VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null)
			ELUtil.getInstance().autowire(this);
		return variablesQuerier;
	}
}