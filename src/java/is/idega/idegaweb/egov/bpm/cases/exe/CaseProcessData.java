package is.idega.idegaweb.egov.bpm.cases.exe;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;

public class CaseProcessData implements Serializable {

	private static final long serialVersionUID = -3552796850515446853L;

	private Timestamp caseCreated;

	private CaseProcInstBind bind;

	private Map<String, Object> caseData;

	public Timestamp getCaseCreated() {
		return caseCreated;
	}

	public void setCaseCreated(Timestamp caseCreated) {
		this.caseCreated = caseCreated;
	}

	public CaseProcInstBind getBind() {
		return bind;
	}

	public void setBind(CaseProcInstBind bind) {
		this.bind = bind;
	}

	public Map<String, Object> getCaseData() {
		return caseData;
	}

	public void setCaseData(Map<String, Object> caseData) {
		this.caseData = caseData;
	}

}