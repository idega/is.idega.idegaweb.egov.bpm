package is.idega.idegaweb.egov.bpm.cases.business;

public class CasesListSearchCriteriaBean {
	
	private String caseNumber;
	private String description;
	private String name;
	private String personalId;
	private String processId;
	private String statusId;
	private String dateRange;
	private String caseListType;
	
	public String getCaseNumber() {
		return caseNumber;
	}
	public void setCaseNumber(String caseNumber) {
		this.caseNumber = caseNumber;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPersonalId() {
		return personalId;
	}
	public void setPersonalId(String personalId) {
		this.personalId = personalId;
	}
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	public String getStatusId() {
		return statusId;
	}
	public void setStatusId(String statusId) {
		this.statusId = statusId;
	}
	public String getDateRange() {
		return dateRange;
	}
	public void setDateRange(String dateRange) {
		this.dateRange = dateRange;
	}
	public String getCaseListType() {
		return caseListType;
	}
	public void setCaseListType(String caseListType) {
		this.caseListType = caseListType;
	}
	
}
