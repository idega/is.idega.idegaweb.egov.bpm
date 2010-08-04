package is.idega.idegaweb.egov.bpm.cases.search.impl;

import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.util.ListUtil;
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
			casesByProcessDefinition = getConvertedFromLongs(getCasesByProcessDefinition(processDefinitionId, getProcessVariables()));
		}
		
		if (ListUtil.isEmpty(casesByProcessDefinition)) {
			getLogger().log(Level.INFO, "No cases found by process definition id: " + processDefinitionId);
		} else {
			getLogger().log(Level.INFO, "Found cases by process definition (" + processDefinitionId + "): " + casesByProcessDefinition);
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
		
		try {
			final ProcessDefinition processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
			return getCasesBPMDAO().getCaseIdsByProcessDefinitionIdsAndNameAndVariables(processDefinitionIds, processDefinition.getName(), variables);
			
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = "+
					processDefinitionId, e);
		}
		
		return null;
	}

	@Override
	protected String getFilterKey() {
		return getProcessId();
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