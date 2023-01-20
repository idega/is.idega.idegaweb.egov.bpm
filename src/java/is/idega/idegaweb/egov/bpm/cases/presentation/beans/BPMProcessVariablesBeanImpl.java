package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.idega.bpm.BPMConstants;
import com.idega.bpm.model.VariableInstance;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.LocaleUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardBean;

@Scope("request")
@Service(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER)
public class BPMProcessVariablesBeanImpl extends DefaultSpringBean implements BPMProcessVariablesBean {

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

	@Override
	public List<AdvancedProperty> getAvailableVariables(Collection<VariableInstance> variables, Locale locale, boolean isAdmin, boolean useRealValue) {
		IWResourceBundle iwrb = getBundle().getResourceBundle(locale);
		Collection<IWResourceBundle> resources = LocaleUtil.getEnabledResources(getApplication(), locale, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		resources = ListUtil.isEmpty(resources) ? new ArrayList<>() : new ArrayList<>(resources);
		resources.add(iwrb);
		return getAvailableVariables(variables, resources, locale, isAdmin, useRealValue);
	}

	@Override
	public List<AdvancedProperty> getAllAvailableVariables(
			Collection<IWResourceBundle> bundles,
			Collection<com.idega.bpm.model.VariableInstance> variables,
			Locale locale,
			boolean isAdmin,
			boolean useRealValue
	) {
		return getAvailableVariables(variables, bundles, locale, isAdmin, useRealValue);
	}

	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariables(
			Collection<VariableInstance> variables,
			Collection<IWResourceBundle> bundles,
			Locale locale,
			boolean isAdmin,
			boolean useRealValue
	) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}

		isAdmin = isAdmin ? isAdmin : getSettings().getBoolean("bpm_variables_all", false);

		String at = "@";
		String name = null;
		String type = null;
		String localizedName = null;
		List<String> addedVariables = new ArrayList<>();
		List<AdvancedProperty> availableVariables = new ArrayList<>();
		for (VariableInstance variable: variables) {
			name = variable.getName();
			if (StringUtil.isEmpty(name) || addedVariables.contains(name)) {
				continue;
			}

			VariableInstanceType varType = variable.getTypeOfVariable();
			type = varType == null ? null : varType.getTypeKeys().get(0);
			if (StringUtil.isEmpty(type)) {
				continue;
			}

			Object value = name.equals(CaseBoardBean.PROJECT_NATURE) ? variable.getVariableValue() : null;
			for (IWResourceBundle iwrb: bundles) {
				localizedName = getVariableLocalizedName(name, value, iwrb, isAdmin);
				if (!StringUtil.isEmpty(localizedName)) {
					break;
				}
			}

			if (StringUtil.isEmpty(localizedName)) {
				continue;
			}
			if (!isAdmin && localizedName.equals(name)) {
				continue;
			}

			String realValue = null;
			if (useRealValue) {
				realValue = getVariableRealValue(variable, locale);
			}

			if (!useRealValue || !StringUtil.isEmpty(realValue)) {
				AdvancedProperty var = new AdvancedProperty(
						useRealValue ? realValue : new StringBuilder(name).append(at).append(type).toString(),
						localizedName,
						name
				);
				Serializable piId = variable.getProcessInstanceId();
				if (piId instanceof Long) {
					var.setExternalId((Long) piId);
				}
				availableVariables.add(var);
				addedVariables.add(name);
			}
		}
		if (ListUtil.isEmpty(availableVariables)) {
			return null;
		}

		Collections.sort(availableVariables, new AdvancedPropertyComparator(locale));
		return availableVariables;
	}

	@Override
	@Transactional(readOnly=true)
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

		Collection<VariableInstance> variables = null;
		try {
			String procDefName = procDef.getProcessDefinitionName();
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
		List<AdvancedProperty> availableVariables = getAvailableVariables(variables, Arrays.asList(iwrb), iwc.getCurrentLocale(), isAdmin, false);
		if (ListUtil.isEmpty(availableVariables)) {
			LOGGER.info("No variables found for process: " + procDef.getProcessDefinitionName());
			processVariables = new ArrayList<>();
			return null;
		}
		availableVariables.add(0, new AdvancedProperty(String.valueOf(-1), iwrb.getLocalizedString("cases_search.select_variable", "Select variable")));

		processVariables = new ArrayList<>();
		for (AdvancedProperty variable: availableVariables) {
			processVariables.add(new SelectItem(variable.getId(), variable.getValue()));
		}

		return processVariables;
	}

	@Override
	public String getVariableLocalizedName(String name, Locale locale) {
		IWBundle bundle = getBundle();
		Collection<IWResourceBundle> iwResourceBundles = LocaleUtil.getEnabledResources(getApplication(), locale, bundle.getBundleIdentifier());
		iwResourceBundles = ListUtil.isEmpty(iwResourceBundles) ? new ArrayList<>() : new ArrayList<>(iwResourceBundles);
		iwResourceBundles.add(bundle.getResourceBundle(locale));
		for (IWResourceBundle iwrb: iwResourceBundles) {
			String locName = getVariableLocalizedName(name, null, iwrb, false);
			if (!StringUtil.isEmpty(locName)) {
				return locName;
			}
		}
		return null;
	}

	@Override
	public String getVariableLocalizedName(Collection<IWResourceBundle> bundles, String name, Locale locale) {
		bundles = ListUtil.isEmpty(bundles) ? Arrays.asList(getBundle().getResourceBundle(locale)) : bundles;
		for (IWResourceBundle iwrb: bundles) {
			String localizedName = getVariableLocalizedName(name, null, iwrb, false);
			if (!StringUtil.isEmpty(localizedName)) {
				return localizedName;
			}
		}
		return null;
	}

	private String getVariableLocalizedName(String name, Object value, IWResourceBundle iwrb, boolean isAdmin) {
		List<String> prefixes = Arrays.asList(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX, BPMConstants.BPMN_VARIABLE_PREFIX, BPMConstants.GROUP_LOC_NAME_PREFIX);
		for (String prefix: prefixes) {
			String localizedName = getVariableLocalizedName(prefix, name, value, iwrb, isAdmin);
			if (!StringUtil.isEmpty(localizedName)) {
				return localizedName;
			}
		}
		return null;
	}

	private String getVariableLocalizedName(String prefix, String name, Object value, IWResourceBundle iwrb, boolean isAdmin) {
		String localizedName = iwrb.getLocalizedString(
				new StringBuilder(StringUtil.isEmpty(prefix) ? CoreConstants.EMPTY : prefix).append(value == null ? name : value.toString()).toString(),
				isAdmin ? name : null
		);

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

	private String getVariableRealValue(VariableInstance variable, Locale locale) {
		Serializable value = variable.getVariableValue();
		if (value == null)
			return null;	//	Invalid value

		MultipleSelectionVariablesResolver resolver = getResolver(variable.getName());
		if (resolver != null)
			return resolver.getPresentation(variable);

		BPMProcessVariable bpmVariable = new BPMProcessVariable(variable.getName(), value.toString(), variable.getTypeOfVariable().getTypeKeys().get(0));
		Serializable realValue = bpmVariable.getRealValue();
		if (realValue == null)
			return null;

		if (realValue instanceof String)
			return (String) realValue;

		if (realValue instanceof Date) {
			IWTimestamp date = new IWTimestamp((Date) value);
			String dateValue = null;
			try {
				dateValue = date.getLocaleDateAndTime(locale, DateFormat.SHORT, DateFormat.SHORT);
			} catch (Exception e) {}
			if (dateValue == null)
				return date.getLocaleDate(locale, DateFormat.SHORT);
			return dateValue;
		}

		return realValue.toString();
	}

	private MultipleSelectionVariablesResolver getResolver(String variableName) {
		MultipleSelectionVariablesResolver resolver = null;
		try {
			resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + variableName);
		} catch (Exception e) {}
		return resolver;
	}

	@Override
	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	@Override
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

	@Override
	public boolean isDisplayVariables() {
		return !isDisplayNoVariablesText();
	}

	@Override
	public boolean isDisplayNoVariablesText() {
		return ListUtil.isEmpty(getProcessVariables());
	}

	private IWBundle getBundle() {
		return IWMainApplication.getDefaultIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
	}

	@Override
	public String getDeleteImagePath() {
		return getBundle().getVirtualPathWithFileNameString("images/delete.png");
	}

	@Override
	public String getLoadingMessage() {
		try {
			return getBundle().getLocalizedString("loading", "Loading...");
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting localized string", e);
		}
		return "Loading...";
	}

	@Override
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