package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.CaseState;

@Scope("request")
@Service(CaseStateConfigBean.NAME)
public class CaseStateConfigBean {
	
	public static final String NAME = "caseStateConfig";
	
	private List<List<String>> processDefinitions;
	private List<CaseState> caseStates;
	
	public List<List<String>> getProcessDefinitions() {
		return processDefinitions;
	}
	public void setProcessDefinitions(List<List<String>> processDefinitions) {
		this.processDefinitions = processDefinitions;
	}
	public List<CaseState> getCaseStates() {
		return caseStates;
	}
	public void setCaseStates(List<CaseState> caseStates) {
		this.caseStates = caseStates;
	}
}
