package is.idega.idegaweb.egov.bpm.cases.presentation;

import com.idega.presentation.IWBaseComponent;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;

public abstract class AbstractCasesBPMAssets extends IWBaseComponent {

	private Integer caseId;

	private CasesBPMAssetsState caseState;

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}

	public CasesBPMAssetsState getCaseState() {
		return caseState;
	}

	public void setCaseState(CasesBPMAssetsState caseState) {
		this.caseState = caseState;
	}

}
