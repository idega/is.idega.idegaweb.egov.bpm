package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.model.SelectItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

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

	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	public List<AdvancedProperty> getAvailableVariables(Collection<VariableInstanceInfo> variables, Locale locale, boolean isAdmin, boolean useRealValue) {
		IWResourceBundle iwrb = getBundle().getResourceBundle(locale);
		return getAvailableVariables(variables, iwrb, locale, isAdmin, useRealValue);
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariables(Collection<VariableInstanceInfo> variables, IWResourceBundle iwrb, Locale locale, boolean isAdmin,
			boolean useRealValue) {
		
		if (ListUtil.isEmpty(variables))
			return null;
		
		String at = "@";
		String name = null;
		String type = null;
		String localizedName = null;
		List<String> addedVariables = new ArrayList<String>();
		List<AdvancedProperty> availableVariables = new ArrayList<AdvancedProperty>();
		for (VariableInstanceInfo variable: variables) {
			name = variable.getName();		
			if (StringUtil.isEmpty(name) || addedVariables.contains(name)) {
				continue;
			}
			
			VariableInstanceType varType = variable.getType();
			type = varType == null ? null : varType.getTypeKeys().get(0);
			if (StringUtil.isEmpty(type)) {
				continue;
			}
			
			localizedName = getVariableLocalizedName(name, iwrb, isAdmin);
			if (StringUtil.isEmpty(localizedName))
				continue;
			if (!isAdmin && localizedName.equals(name))
				continue;
			
			String realValue = null;
			if (useRealValue) {
				realValue = getVariableRealValue(variable, locale);
			}
			if (!useRealValue || !StringUtil.isEmpty(realValue)) {
				availableVariables.add(new AdvancedProperty(useRealValue ? realValue : new StringBuilder(name).append(at).append(type).toString(),localizedName));
				addedVariables.add(name);
			}
		}
		if (ListUtil.isEmpty(availableVariables)) {
			return null;
		}
		
		Collections.sort(availableVariables, new AdvancedPropertyComparator(locale));
		return availableVariables;
	}
	
	@Transactional(readOnly=true)
	public List<SelectItem> getProcessVariables() {
		if (processVariables != null) {
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
		
		Collection<VariableInstanceInfo> variables = null;
		try {
			String procDefName = procDef.getProcessDefinition().getName();
			variables = getVariablesQuerier().getVariablesByProcessDefinition(procDefName);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting variables for process: " + processDefinitionId, e);
		}
		if (ListUtil.isEmpty(variables)) {
			processVariables = Collections.emptyList();
			return processVariables;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		IWResourceBundle iwrb = getBundle().getResourceBundle(iwc);
		boolean isAdmin = iwc.isSuperAdmin();
		List<AdvancedProperty> availableVariables = getAvailableVariables(variables, iwrb, iwc.getCurrentLocale(), isAdmin, false);
		if (ListUtil.isEmpty(availableVariables)) {
			LOGGER.info("No variables found for process: " + procDef.getProcessDefinition().getName());
			processVariables = new ArrayList<SelectItem>();
			return null;
		}
		availableVariables.add(0, new AdvancedProperty(String.valueOf(-1), iwrb.getLocalizedString("cases_search.select_variable", "Select variable")));
		
		processVariables = new ArrayList<SelectItem>();
		for (AdvancedProperty variable: availableVariables) {
			processVariables.add(new SelectItem(variable.getId(), variable.getValue()));
		}
		
		return processVariables;
	}
	
	private String getVariableLocalizedName(String name, IWResourceBundle iwrb, boolean isAdmin) {
		String localizedName = iwrb.getLocalizedString(new StringBuilder(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX).append(name).toString(), isAdmin ? name : null);
		
		if (StringUtil.isEmpty(localizedName)) {
			return isAdmin ? name : null;	//	No translation found
		}
		
		if ("null".equals(localizedName) || localizedName.startsWith("null")) {
			return isAdmin ? name : null;	//	Checks because localizer bugs
		}
		
		if (localizedName.equals(name)) {
			return isAdmin ? localizedName : null;	//	Administrator can see not localized variables
		}
		
		return localizedName;
	}
	
	private String getVariableRealValue(VariableInstanceInfo variable, Locale locale) {
		Serializable value = variable.getValue();
		if (value == null) {
			return null;	//	Invalid value
		}
		
		BPMProcessVariable bpmVariable = new BPMProcessVariable(variable.getName(), value.toString(), variable.getType().getTypeKeys().get(0));
		Serializable realValue = bpmVariable.getRealValue();
		if (realValue == null) {
			return null;
		}
		
		if (realValue instanceof String) {
			return (String) realValue;
		}
		if (realValue instanceof Date) {
			IWTimestamp date = new IWTimestamp((Date) value);
			return date.getLocaleDate(locale, DateFormat.SHORT);
		}
		
		if (CaseHandlerAssignmentHandler.handlerUserIdVarName.equals(variable.getName()) || CaseHandlerAssignmentHandler.performerUserIdVarName.equals(variable.getName())
				|| variable.getName().startsWith(VariableInstanceType.OBJ_LIST.getPrefix()) || variable.getName().startsWith(VariableInstanceType.LIST.getPrefix())
				|| variable.getName().equals("string_harbourNr")) {
			return getResolver(variable.getName()).getPresentation(variable);
		}
		
		return realValue.toString();
	}
	
	private MultipleSelectionVariablesResolver getResolver(String variableName) {
		MultipleSelectionVariablesResolver resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + variableName);
		return resolver;
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

	private IWBundle getBundle() {
		return IWMainApplication.getDefaultIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
	}
	
	public String getDeleteImagePath() {
		return getBundle().getVirtualPathWithFileNameString("images/delete.png");
	}

	public String getLoadingMessage() {
		try {
			return getBundle().getLocalizedString("loading", "Loading...");
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting localized string", e);
		}
		return "Loading...";
	}

	public String getAddVariableImage() {
		return getBundle().getVirtualPathWithFileNameString("images/add.png");
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

}