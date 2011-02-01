package is.idega.idegaweb.egov.bpm.cases.search.impl;

import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.bpm.BPMConstants;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.user.data.User;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessesFilter extends DefaultCasesListSearchFilter {

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
			List<Long> ids = getCasesByProcessDefinition(processDefinitionId, getProcessVariables());
			casesByProcessDefinition = getConvertedFromLongs(ids);
		}
		
		if (ListUtil.isEmpty(casesByProcessDefinition)) {
			getLogger().info("No cases found by process definition id: " + processDefinitionId + " and variables: " + getProcessVariables());
		} else {
			getLogger().info("Found cases by process definition (" + processDefinitionId + ") and variables (" + getProcessVariables() + "): " +
					casesByProcessDefinition);
		}
			
		return casesByProcessDefinition;
	}
	
	@Override
	protected String getInfo() {
		return "Looking for cases by process definition: " + getProcessId();
	}
	
	private List<Long> getCasesByProcessDefinition(String processDefinitionId, List<BPMProcessVariable> variables) {
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
		
		BPMProcessVariable handlerVariable = null;
		for (BPMProcessVariable variable: variables) {
			if (BPMConstants.BPM_PROCESS_HANDLER_VARIABLE.equals(variable.getName())) {
				handlerVariable = variable;
			}
		}
		
		if (handlerVariable != null) {
			variables.remove(handlerVariable);
			
			String handlerId = getHandlerId(handlerVariable.getValue());
			if (handlerId == null) {
				getLogger().warning("Handler ID can not be resolved by search key: " + handlerVariable.getValue());
				return null;
			}
			BPMProcessVariable handlerVar = new BPMProcessVariable(CaseHandlerAssignmentHandler.handlerUserIdVarName, handlerId, VariableInstanceType.LONG.getTypeKeys().get(0));
			variables.add(handlerVar);
		}
		
		try {
			final ProcessDefinition processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
			return getCasesBPMDAO().getCaseIdsByProcessDefinitionIdsAndNameAndVariables(processDefinitionIds, processDefinition.getName(), variables);
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = "+
					processDefinitionId + ", variables: " + variables, e);
		}
		
		return null;
	}
	
	private String getHandlerId(String searchKey) {
		if (StringUtil.isEmpty(searchKey)) {
			return null;
		}
		
		if (StringHandler.isNaturalNumber(searchKey)) {
			User handler = null;
			try {
				handler = getUserBusiness().getUser(searchKey);
			} catch (Exception e) {
				getLogger().warning("User was not found by personal ID: " + searchKey);
			}
			if (handler != null) {
				return handler.getId();
			}
		} else {
			getLogger().info("'" + searchKey + "' is not treated as a personal ID");
		}
			
		Collection<User> users = null;
		try {
			users = getUserBusiness().getUsersByNameOrEmailOrPhone(searchKey);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting users by search key: " + searchKey, e);
		}
		
		return ListUtil.isEmpty(users) ? null : users.iterator().next().getId();
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
}