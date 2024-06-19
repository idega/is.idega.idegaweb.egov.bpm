package is.idega.idegaweb.egov.bpm.cases.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CasesListSearchCriteriaBean extends CasesSearchCriteriaBean {

	private static final long serialVersionUID = 8071978111646904945L;

	private String	componentId,
					criteriasId,
					processId,
					caseListType,
					caseCodes,
					statusesToShow,
					statusesToHide,
					roles,
					customView,
					caseManagerType,
					assigned;

	private List<BPMProcessVariable> processVariables;

	private boolean usePDFDownloadColumn = true,
					allowPDFSigning = true,
					hideEmptySection,
					showCaseNumberColumn = true,
					showCreationTimeInDateColumn = true,
					onlySubscribedCases,
					clearResults = true,
					nothingFound,
					showAttachmentStatistics = false,
					showUserProfilePicture = true,
					addExportContacts = false,
					showUserCompany = false,
					showTimeSpentOnCase = false,
					showCaseSubstatus = false,
					search = false,
					convertFromPresentationToObject = true;

	private String freeVariableText;

	private Set<Integer> casesIDs;

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getCaseListType() {
		return caseListType;
	}

	public void setCaseListType(String caseListType) {
		this.caseListType = caseListType;
	}

	public boolean isUsePDFDownloadColumn() {
		return usePDFDownloadColumn;
	}

	public void setUsePDFDownloadColumn(boolean usePDFDownloadColumn) {
		this.usePDFDownloadColumn = usePDFDownloadColumn;
	}

	public boolean isAllowPDFSigning() {
		return allowPDFSigning;
	}

	public void setAllowPDFSigning(boolean allowPDFSigning) {
		this.allowPDFSigning = allowPDFSigning;
	}

	public List<BPMProcessVariable> getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariables(List<BPMProcessVariable> processVariables) {
		this.processVariables = processVariables;
	}

	public boolean isHideEmptySection() {
		return hideEmptySection;
	}

	public void setHideEmptySection(boolean hideEmptySection) {
		this.hideEmptySection = hideEmptySection;
	}

	public boolean isShowCaseNumberColumn() {
		return showCaseNumberColumn;
	}

	public void setShowCaseNumberColumn(boolean showCaseNumberColumn) {
		this.showCaseNumberColumn = showCaseNumberColumn;
	}

	public boolean isShowCreationTimeInDateColumn() {
		return showCreationTimeInDateColumn;
	}

	public void setShowCreationTimeInDateColumn(boolean showCreationTimeInDateColumn) {
		this.showCreationTimeInDateColumn = showCreationTimeInDateColumn;
	}

	public List<String> getStatusesToShowInList() {
		return statusesToShow == null ? null : StringUtil.getValuesFromString(statusesToShow, CoreConstants.COMMA);
	}

	public void setStatusesToShow(String statusesToShow) {
		this.statusesToShow = statusesToShow;
	}

	public List<String> getStatusesToHideInList() {
		return statusesToHide == null ? null : StringUtil.getValuesFromString(statusesToHide, CoreConstants.COMMA);
	}

	public void setStatusesToHide(String statusesToHide) {
		this.statusesToHide = statusesToHide;
	}

	public String getStatusesToShow() {
		return statusesToShow;
	}

	public String getStatusesToHide() {
		return statusesToHide;
	}

	public List<String> getCaseCodesInList() {
		return StringUtil.isEmpty(caseCodes) ? null : StringUtil.getValuesFromString(caseCodes, CoreConstants.COMMA);
	}

	public void setCaseCodes(String caseCodes) {
		this.caseCodes = caseCodes;
	}

	public String getCaseCodes() {
		return caseCodes;
	}

	public boolean isOnlySubscribedCases() {
		return onlySubscribedCases;
	}

	public void setOnlySubscribedCases(boolean onlySubscribedCases) {
		this.onlySubscribedCases = onlySubscribedCases;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getCriteriasId() {
		return criteriasId;
	}

	public void setCriteriasId(String criteriasId) {
		this.criteriasId = criteriasId;
	}

	public boolean isClearResults() {
		return clearResults;
	}

	public void setClearResults(boolean clearResults) {
		this.clearResults = clearResults;
	}

	public boolean isNothingFound() {
		return nothingFound;
	}

	public void setNothingFound(boolean nothingFound) {
		this.nothingFound = nothingFound;
	}

	public Set<String> getRoles() {
		return roles == null ? null : new HashSet<>(StringUtil.getValuesFromString(roles, CoreConstants.COMMA));
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	@Override
	public String toString() {
		return new StringBuilder("Case number: " + getCaseNumber()).append("\n")
			.append("Description: " + getDescription()).append("\n")
			.append("Name: " + getName()).append("\n")
			.append("Personal ID: " + getPersonalId()).append("\n")
			.append("Process ID: " + processId).append("\n")
			.append("Status ID: " + getStatusId()).append("\n")
			.append("Date range: " + getDateRange()).append("\n")
			.append("Case list type: " + caseListType).append("\n")
			.append("Contact: " + getContact()).append("\n")
			.append("Date from: " + getDateFrom()).append("\n")
			.append("Date to: " + getDateTo()).append("\n")
			.append("Statuses: " + getStatuses()).append("\n")
			.append("Case codes: " + caseCodes).append("\n")
			.append("Statuses to show: " + statusesToShow).append("\n")
			.append("Statuses to hide: " + statusesToHide).append("\n")
			.append("Roles: " + roles).append("\n")
			.append("Page: " + getPage()).append("\n")
			.append("Page size: " + getPageSize()).append("\n")
			.append("Component ID: " + componentId).append("\n")
			.append("Criterias ID: " + criteriasId).append("\n")
			.append("Process evaluation: " + getEvaluationProcess()).append("\n")
			.append("Address: " + getAddress()).append("\n")
			.append("Variables: " + getProcessVariables()).append("\n")
			.append("Free text: " + getFreeVariableText()).append("\n")
			.append("Clear results: " + clearResults).append("\n")
			.append("Search: " + search)
		.toString();
	}

	public boolean isShowAttachmentStatistics() {
		return showAttachmentStatistics;
	}

	public void setShowAttachmentStatistics(boolean showAttachmentStatistics) {
		this.showAttachmentStatistics = showAttachmentStatistics;
	}

	public boolean isShowUserProfilePicture() {
		return showUserProfilePicture;
	}

	public void setShowUserProfilePicture(boolean showUserProfilePicture) {
		this.showUserProfilePicture = showUserProfilePicture;
	}

	public boolean isAddExportContacts() {
		return addExportContacts;
	}

	public void setAddExportContacts(boolean addExportContacts) {
		this.addExportContacts = addExportContacts;
	}

	public boolean isShowUserCompany() {
		return showUserCompany;
	}

	public void setShowUserCompany(boolean showUserCompany) {
		this.showUserCompany = showUserCompany;
	}

	public boolean isSearch() {
		return search;
	}

	public void setSearch(boolean search) {
		this.search = search;
	}

	public String getCustomView() {
		return customView;
	}

	public void setCustomView(String customView) {
		this.customView = customView;
	}

	public boolean isShowTimeSpentOnCase() {
		return showTimeSpentOnCase;
	}

	public void setShowTimeSpentOnCase(boolean showTimeSpentOnCase) {
		this.showTimeSpentOnCase = showTimeSpentOnCase;
	}

	public boolean isShowCaseSubstatus() {
		return showCaseSubstatus;
	}

	public void setShowCaseSubstatus(boolean showCaseSubstatus) {
		this.showCaseSubstatus = showCaseSubstatus;
	}

	public final String getCaseManagerType() {
		return caseManagerType;
	}

	public final void setCaseManagerType(String caseManagerType) {
		this.caseManagerType = caseManagerType;
	}

	public String getFreeVariableText() {
		return freeVariableText;
	}

	public void setFreeVariableText(String freeVariableText) {
		this.freeVariableText = freeVariableText;
	}

	public Set<Integer> getCasesIDs() {
		return casesIDs;
	}

	public void setCasesIDs(Set<Integer> casesIDs) {
		this.casesIDs = casesIDs;
	}

	public String getAssigned() {
		return assigned;
	}

	public void setAssigned(String assigned) {
		this.assigned = assigned;
	}

	public boolean isConvertFromPresentationToObject() {
		return convertFromPresentationToObject;
	}

	public void setConvertFromPresentationToObject(boolean convertFromPresentationToObject) {
		this.convertFromPresentationToObject = convertFromPresentationToObject;
	}

}