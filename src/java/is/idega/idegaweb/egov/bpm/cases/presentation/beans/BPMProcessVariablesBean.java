package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.idegaweb.IWResourceBundle;

public interface BPMProcessVariablesBean extends Serializable {

	public static final String SPRING_BEAN_IDENTIFIER = "bpmProcessVariablesBean";

	public abstract List<SelectItem> getProcessVariables();

	public abstract Long getProcessDefinitionId();

	public abstract void setProcessDefinitionId(Long processDefinitionId);

	public boolean isDisplayVariables();

	public boolean isDisplayNoVariablesText();

	public String getDeleteImagePath();

	public String getLoadingMessage();

	public String getAddVariableImage();

	public List<AdvancedProperty> getAvailableVariables(Collection<com.idega.bpm.model.VariableInstance> variables, Locale locale, boolean isAdmin, boolean useRealValue);

	public List<AdvancedProperty> getAllAvailableVariables(
			Collection<IWResourceBundle> bundles,
			Collection<com.idega.bpm.model.VariableInstance> variables,
			Locale locale,
			boolean isAdmin,
			boolean useRealValue
	);

	public String getVariableLocalizedName(String name, Locale locale);

	public String getVariableLocalizedName(Collection<IWResourceBundle> bundles, String name, Locale locale);

}