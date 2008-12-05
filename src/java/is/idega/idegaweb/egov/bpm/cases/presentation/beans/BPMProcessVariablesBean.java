package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.SelectItem;

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
	
}
