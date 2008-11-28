package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.model.SelectItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Scope("request")
@Service(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER)
public class BPMProcessVariablesBeanImpl implements BPMProcessVariablesBean {

	private static final long serialVersionUID = 5469235199056822371L;

	private static final Logger LOGGER = Logger.getLogger(BPMProcessVariablesBeanImpl.class.getName());
	
	private Long processDefinitionId;
	
	private List<SelectItem> processVariables;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;

	public List<SelectItem> getProcessVariables() {
		if (!ListUtil.isEmpty(processVariables)) {
			return processVariables;
		}
		
		if (processDefinitionId == null) {
			return null;
		}
		
		ProcessDefinitionW procDef = null;
		try {
			procDef = getBpmFactory().getProcessManager(processDefinitionId).getProcessDefinition(processDefinitionId);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting process definition by id: " + processDefinitionId, e);
		}
		if (procDef == null) {
			return null;
		}
		
		List<String> variables = null;
		try {
			variables = getCasesBPMDAO().getVariablesByProcessDefinition(procDef.getProcessDefinition().getName());
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting variables for process: " + processDefinitionId, e);
		}
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		
		String localizedName = null;
		String key = "bpm_variable.";
		boolean isAdmin = iwc.isSuperAdmin();
		List<AdvancedProperty> availableVariables = new ArrayList<AdvancedProperty>();
		for (String variableName: variables) {
			localizedName = iwrb.getLocalizedString(new StringBuilder(key).append(variableName).toString(), isAdmin ? variableName : null);
			if (!StringUtil.isEmpty(localizedName)) {
				availableVariables.add(new AdvancedProperty(variableName, localizedName));
			}
		}
		if (ListUtil.isEmpty(availableVariables)) {
			return null;
		}
		
		Collections.sort(availableVariables, new AdvancedPropertyComparator(iwc.getCurrentLocale()));
		availableVariables.add(0, new AdvancedProperty(String.valueOf(-1), iwrb.getLocalizedString("cases_search.select_variable", "Select variable")));
		
		processVariables = new ArrayList<SelectItem>();
		for (AdvancedProperty variable: availableVariables) {
			processVariables.add(new SelectItem(variable.getId(), variable.getValue()));
		}
		
		return processVariables;
	}
	
	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public boolean isDisplayVariables() {
		return !isDisplayNoVariablesText();
	}

	public boolean isDisplayNoVariablesText() {
		return ListUtil.isEmpty(getProcessVariables());
	}
	
}
