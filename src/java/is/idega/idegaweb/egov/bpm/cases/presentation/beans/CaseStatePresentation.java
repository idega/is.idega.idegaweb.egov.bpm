package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;

import com.idega.idegaweb.egov.bpm.data.CaseState;
import com.idega.idegaweb.egov.bpm.data.CaseStateInstance;

public class CaseStatePresentation implements Serializable{

	private static final long serialVersionUID = -1201284587530069691L;
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
