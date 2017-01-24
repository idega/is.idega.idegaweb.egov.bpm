package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import com.idega.idegaweb.egov.bpm.data.CaseState;
import com.idega.idegaweb.egov.bpm.data.CaseStateInstance;

public class CaseStatePresentation {

	private CaseStateInstance stateInstance;
	private CaseState stateDefinition;

	public CaseStateInstance getStateInstance() {
		return stateInstance;
	}
	public void setStateInstance(CaseStateInstance stateInstance) {
		this.stateInstance = stateInstance;
	}
	public CaseState getStateDefinition() {
		return stateDefinition;
	}
	public void setStateDefinition(CaseState stateDefinition) {
		this.stateDefinition = stateDefinition;
	}
}
