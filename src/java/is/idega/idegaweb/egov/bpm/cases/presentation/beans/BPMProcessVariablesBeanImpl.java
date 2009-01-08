package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.model.SelectItem;

import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.variableinstance.DateInstance;
import org.jbpm.context.exe.variableinstance.DoubleInstance;
import org.jbpm.context.exe.variableinstance.HibernateLongInstance;
import org.jbpm.context.exe.variableinstance.HibernateStringInstance;
import org.jbpm.context.exe.variableinstance.LongInstance;
import org.jbpm.context.exe.variableinstance.NullInstance;
import org.jbpm.context.exe.variableinstance.StringInstance;
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
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
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

	public List<AdvancedProperty> getAvailableVariables(List<VariableInstance> variables, Locale locale, boolean isAdmin, boolean useRealValue) {
		IWResourceBundle iwrb = getBundle().getResourceBundle(locale);
		return getAvailableVariables(variables, iwrb, locale, isAdmin, useRealValue);
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariables(List<VariableInstance> variables, IWResourceBundle iwrb, Locale locale, boolean isAdmin,
			boolean useRealValue) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		String at = "@";
		String name = null;
		String type = null;
		String localizedName = null;
		List<String> addedVariables = new ArrayList<String>();
		List<AdvancedProperty> availableVariables = new ArrayList<AdvancedProperty>();
		for (VariableInstance variable: variables) {
			if (!(variable instanceof NullInstance)) {
				name = variable.getName();
				
				if (!addedVariables.contains(name)) {
					type = getVariableValueType(variable);
					
					if (!StringUtil.isEmpty(type)) {
						localizedName = getVariableLocalizedName(name, iwrb, isAdmin);
						if (!StringUtil.isEmpty(localizedName)) {
							String realValue = null;
							if (useRealValue) {
								realValue = getVariableRealValue(variable, locale);
							}
							
							if (!useRealValue || !StringUtil.isEmpty(realValue)) {
								availableVariables.add(new AdvancedProperty(useRealValue ? realValue : new StringBuilder(name).append(at).append(type).toString(),
																										localizedName));
								addedVariables.add(name);
							}
						}
					}
				}
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
		if (!ListUtil.isEmpty(processVariables)) {
			return processVariables;
		}
		
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
		
		List<VariableInstance> variables = null;
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
		String localizedName = iwrb.getLocalizedString(new StringBuilder("bpm_variable.").append(name).toString(), isAdmin ? name : null);
		
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
	
	private String getVariableValueType(VariableInstance variable) {
		if (variable instanceof DateInstance) {
			return BPMProcessVariable.DATE_TYPES.get(0);
		}
		if (variable instanceof DoubleInstance) {
			return BPMProcessVariable.DOUBLE_TYPES.get(0);
		}
		if (variable instanceof LongInstance || variable instanceof HibernateLongInstance) {
			return BPMProcessVariable.LONG_TYPES.get(0);
		}
		if (variable instanceof StringInstance || variable instanceof HibernateStringInstance) {
			return BPMProcessVariable.STRING_TYPES.get(0);
		}
		
		return null;	//	We do not support other types
	}
	
	private String getVariableRealValue(VariableInstance variable, Locale locale) {
		if (variable instanceof DateInstance) {
			IWTimestamp date = new IWTimestamp((Date) variable.getValue());
			return date.getLocaleDate(locale, DateFormat.SHORT);
		}
		if (variable instanceof DoubleInstance) {
			return variable.getValue().toString();	//	TODO: is this OK?
		}
		if (variable instanceof LongInstance || variable instanceof HibernateLongInstance) {
			return variable.getValue().toString();
		}
		if (variable instanceof StringInstance || variable instanceof HibernateStringInstance) {
			return variable.getValue().toString();
		}
		
		return null;	//	We do not support other types
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
	
}
