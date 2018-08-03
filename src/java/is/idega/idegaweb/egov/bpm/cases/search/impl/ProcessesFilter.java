package is.idega.idegaweb.egov.bpm.cases.search.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessesFilter extends DefaultCasesListSearchFilter {

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	@Autowired
	private CasesBPMDAO casesDAO;

	@Autowired
	private BPMFactory bpmFactory;

	private String procDefName;

	@Override
	public List<Integer> getSearchResults(List<Integer> casesIds) {
		String processDefinitionId = getProcessId();
		List<Integer> casesByProcessDefinition = null;
		if (CasesConstants.GENERAL_CASES_TYPE.equals(processDefinitionId)) {
			//	Getting ONLY none "BPM" cases
			try {
				casesByProcessDefinition = getCasesBusiness().getFilteredProcesslessCasesIds(casesIds, CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()));
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting non BPM cases", e);
			}
		} else {
			//	Getting "BPM" cases
			casesByProcessDefinition = getCasesByProcessDefinition(processDefinitionId, getProcessVariables());
		}

		if (ListUtil.isEmpty(casesByProcessDefinition)) {
			getLogger().warning("No cases found by process definition (ID: " + processDefinitionId + ", name: " + getProcessDefinitionName() +
					") and variables (" + getProcessVariables() + ")");
		} else {
			getLogger().info("Found " + casesByProcessDefinition.size() + " case(s) by process definition (ID: " + processDefinitionId + ", name: " +
					getProcessDefinitionName() + ") and variables (" + getProcessVariables() + "): " + (casesByProcessDefinition.size() >= 1000 ?
					casesByProcessDefinition.subList(0, 999) + " ..." : casesByProcessDefinition));
		}

		return casesByProcessDefinition;
	}

	@Override
	protected String getInfo() {
		return "Looking for cases by process definition. ID: " + getProcessId() + ", name: " + getProcessDefinitionName() + " and BPM variables: " +
				getProcessVariables();
	}

	private String getProcessDefinitionName() {
		return procDefName;
	}

	@SuppressWarnings("unchecked")
	private List<Integer> getCasesByProcessDefinition(String processDefinitionId, List<BPMProcessVariable> vars) {
		if (StringUtil.isEmpty(processDefinitionId)) {
			return null;
		}

		String procDefName = null;
		if (StringHandler.isNumeric(processDefinitionId)) {
			ProcessDefinition processDefinition = null;
			try {
				Long procDefId = Long.valueOf(processDefinitionId);
				processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting process definition by ID: " + processDefinitionId, e);
			}
			procDefName = processDefinition == null ? null : processDefinition.getName();
		} else {
			//Remove all vars, because variables for BPM2 are stored in Camunda, not in the local DB
			if (!ListUtil.isEmpty(vars)) {
				vars.clear();
			}

			ProcessDefinitionW procDefW = bpmFactory.getProcessManager((Serializable) processDefinitionId).getProcessDefinition((Serializable) processDefinitionId);
			if (procDefW == null) {
				return null;
			}

			procDefName = procDefW.getProcessDefinitionName();
			if (ListUtil.isEmpty(vars)) {
				Map<String, BPMDAO> bpmDAOs = WebApplicationContextUtils.getWebApplicationContext(getApplication().getServletContext()).getBeansOfType(BPMDAO.class);
				if (MapUtil.isEmpty(bpmDAOs)) {
					return null;
				}

				List<String> procInstancesUUIDs = new ArrayList<>();
				List<Long> procInstancesIds = new ArrayList<>();
				for (BPMDAO bpmDAO: bpmDAOs.values()) {
					List<?> ids = bpmDAO.getProcessInstanceIds(processDefinitionId, procDefName);
					if (!ListUtil.isEmpty(ids)) {
						if (ids.get(0) instanceof Long) {
							procInstancesIds.addAll((List<Long>) ids);
						} else if (ids.get(0) instanceof String) {
							procInstancesUUIDs.addAll((List<String>) ids);
						}
					}
				}

				Set<Integer> allCasesIds = new HashSet<>();
				if (!ListUtil.isEmpty(procInstancesUUIDs)) {
					List<Integer> casesIds = getCasesBPMDAO().findCasesIdsByUUIDs(procInstancesUUIDs);
					if (!ListUtil.isEmpty(casesIds)) {
						allCasesIds.addAll(casesIds);
					}
				}
				if (!ListUtil.isEmpty(procInstancesIds)) {
					List<Integer> casesIds = getCasesBPMDAO().getCasesIdsByProcInstIds(procInstancesIds);
					if (!ListUtil.isEmpty(casesIds)) {
						allCasesIds.addAll(casesIds);
					}
				}

				if (ListUtil.isEmpty(allCasesIds)) {
					return null;
				}

				return new ArrayList<>(allCasesIds);
			}
		}

		if (StringUtil.isEmpty(procDefName)) {
			return null;
		}
		this.procDefName = procDefName;

		getLogger().info(getInfo());

		List<BPMProcessVariable> variables = new ArrayList<BPMProcessVariable>(vars);

		Object tmp = null;
		BPMProcessVariable handlerVariable = null;
		List<BPMProcessVariable> varsToRemove = new ArrayList<BPMProcessVariable>();
		Map<String, List<Serializable>> multValues = new HashMap<String, List<Serializable>>();
		for (BPMProcessVariable variable: variables) {
			String name = variable.getName();

			//	Checking if variable is holding handler ID
			if (CaseHandlerAssignmentHandler.handlerUserIdVarName.equals(name) || CaseHandlerAssignmentHandler.performerUserIdVarName.equals(name)) {
				handlerVariable = variable;
			} else if ((tmp = variable.getRealValue()) instanceof Collection) {	//	Checking if value is multi-type
				Collection<?> newMultipleValues = (Collection<?>) tmp;
				List<Serializable> existingMultipleValues = multValues.get(name);
				if (existingMultipleValues == null) {
					existingMultipleValues = new ArrayList<Serializable>();
					multValues.put(name, existingMultipleValues);
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
			if (ListUtil.isEmpty(casesIdsByMultipleValues)) {
				return null;
			}

			getLogger().info("Found cases by multiple variables: " + multValues + " and process definition: " + procDefName + ": " + casesIdsByMultipleValues);
		}

		if (handlerVariable != null) {
			List<Integer> casesIdsByHandlers = getCaseIdsByHandlers(handlerVariable.getName(), handlerVariable.getValue(), procDefName);
			if (ListUtil.isEmpty(casesIdsByHandlers)) {
				return null;
			}

			getLogger().info("Found cases by handler: " + handlerVariable + " and process definition: " + procDefName + ": " + casesIdsByHandlers);

			if (casesIdsByMultipleValues == null) {
				//	Just handler variable was specified
				casesIdsByMultipleValues = new ArrayList<Integer>(casesIdsByHandlers);
			} else {
				//	Keeping condition AND
				casesIdsByMultipleValues = getNarrowedResults(casesIdsByMultipleValues, casesIdsByHandlers);
				if (ListUtil.isEmpty(casesIdsByMultipleValues)) {
					return null;
				}
			}

			variables.remove(handlerVariable);
		}

		if (!ListUtil.isEmpty(varsToRemove)) {
			variables.removeAll(varsToRemove);
		}

		List<Integer> casesIds = null;
		try {
			List<Integer> casesIdsByOtherVars = getConvertedFromNumbers(getCasesBPMDAO().getCaseIdsByProcessDefinitionNameAndVariables(procDefName, variables));
			casesIds = ListUtil.isEmpty(casesIdsByMultipleValues) ? casesIdsByOtherVars : getNarrowedResults(casesIdsByMultipleValues, casesIdsByOtherVars);
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = " +
					processDefinitionId + ", variables: " + variables, e);
		}

		return casesIds;
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

	private List<Long> getProcInstIdsFromVars(Collection<VariableInstanceInfo> vars, List<Long> ids) {
		if (ListUtil.isEmpty(vars)) {
			return null;
		}

		ids = ids == null ? new ArrayList<Long>() : ids;
		for (VariableInstanceInfo var: vars) {
			Long procInstId = var.getProcessInstanceId();
			if (procInstId == null) {
				continue;
			}

			if (!ids.contains(procInstId)) {
				ids.add(procInstId);
			}
		}
		return ids;
	}

	private List<Integer> getCaseIdsByHandlers(String varName, String handlers, String procDefName) {
		if (StringUtil.isEmpty(handlers)) {
			return null;
		}

		String[] ids = handlers.split(CoreConstants.SEMICOLON);
		if (ArrayUtil.isEmpty(ids)) {
			return null;
		}

		List<Integer> usersIds = new ArrayList<Integer>();
		for (String id: ids) {
			Integer handlerId = null;
			try {
				handlerId = Integer.valueOf(id);
			} catch (NumberFormatException e) {
				Collection<User> users = getUserBusiness().getUsersByNameOrEmailOrPhone(id);
				if (!ListUtil.isEmpty(users)) {
					for (User user: users) {
						usersIds.add(Integer.valueOf(user.getId()));
					}
				}
				handlerId = null;
			}

			if (handlerId != null) {
				usersIds.add(handlerId);
			}
		}

		return getCasesDAO().getCasesIdsByHandlersAndProcessDefinition(usersIds, procDefName);
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
		if (variablesQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variablesQuerier;
	}

	CasesBPMDAO getCasesDAO() {
		if (casesDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return casesDAO;
	}
}