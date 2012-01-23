package is.idega.idegaweb.egov.bpm.cases;

/**
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $ Last modified: $Date: 2009/06/23 10:22:01 $ by $Author: valdas $
 */
public class CasesBPMProcessConstants {
	
	private CasesBPMProcessConstants() {
	}
	
	public static final String caseIdVariableName = "string_caseId";
	public static final String caseTypeNameVariableName = "string_caseTypeName";
	public static final String caseCategoryNameVariableName = "string_caseCategoryName";
	public static final String caseCreatedDateVariableName = "string_caseCreatedDateString";
	public static final String caseAllocateToVariableName = "string_allocateTo";
	public static final String casePerformerIdVariableName = "string_performerId";
	public static final String caseStatusVariableName = "string_caseStatus";
	public static final String caseOwnerFirstNameVariableName = "string_ownerFirstName";
	public static final String caseOwnerLastNameVariableName = "string_ownerLastName";
	public static final String caseIdentifierNumberParam = "caseIdentifierNumber";
	public static final String caseIdentifier = "caseIdentifier";
	public static final String userIdActionVariableName = "userId";
	public static final String caseCategoryIdActionVariableName = "caseCategoryId";
	public static final String caseTypeActionVariableName = "caseType";
	public static final String caseCreationDateParam = "caseCreationDate";
	
	// case status variable names
	public static final String caseStatusClosedVariableName = "string_caseStatusClosed";
	public static final String caseStatusDenied = "string_caseStatusDenied";
	public static final String caseStatusGranted = "string_caseStatusGranted";
	public static final String caseStatusInactive = "string_caseStatusInactive";
	public static final String caseStatusMoved = "string_caseStatusMoved";
	public static final String caseStatusReceived = "string_caseStatusReceived";
	public static final String caseStatusInProgress = "string_caseStatusInProgress";
	public static final String caseStatusPreliminary = "string_caseStatusPreliminary";
	public static final String caseStatusReady = "string_caseStatusReady";
	public static final String caseStatusReview = "string_caseStatusReview";
	public static final String caseStatusGrouped = "string_caseStatusGrouped";
	public static final String caseStatusPlaced = "string_caseStatusPlaced";
	public static final String caseStatusWait = "string_caseStatusWait";
	public static final String caseStatusInProcess = "string_caseStatusInProcess";
	public static final String caseStatusDeleted = "string_caseStatusDeleted";
	public static final String caseStatusCreated = "string_caseStatusCreated";
	public static final String caseStatusFinished = "string_caseStatusFinished";
	public static final String caseStatusClosed = "string_caseStatusClosed";
	
	// mapped case status names
	public static final String CASE_STATUS_GRANTED_MAPNAME = "caseStatusGranted";
	public static final String CASE_STATUS_DENIED_MAPNAME = "caseStatusDenied";
	public static final String CASE_STATUS_INACTIVE_MAPNAME = "caseStatusInactive";
	public static final String CASE_STATUS_MOVED_MAPNAME = "caseStatusMoved";
	public static final String CASE_STATUS_OPENED_MAPNAME = "caseStatusOpened";
	public static final String CASE_STATUS_INPROGRESS_MAPNAME = "caseStatusInProgress";
	public static final String CASE_STATUS_PRELIMINARY_MAPNAME = "caseStatusPreliminary";
	public static final String CASE_STATUS_READY_MAPNAME = "caseStatusReady";
	public static final String CASE_STATUS_REVIEW_MAPNAME = "caseStatusReview";
	public static final String CASE_STATUS_PLACED_MAPNAME = "caseStatusPlaced";
	public static final String CASE_STATUS_WAIT_MAPNAME = "caseStatusWait";
	public static final String CASE_STATUS_INPROCESS_MAPNAME = "caseStatusInProcess";
	public static final String CASE_STATUS_DELETED_MAPNAME = "caseStatusDeleted";
	public static final String CASE_STATUS_CREATED_MAPNAME = "caseStatusCreated";
	public static final String CASE_STATUS_FINISHED_MAPNAME = "caseStatusFinished";
	public static final String CASE_STATUS_CLOSED_MAPNAME = "caseStatusClosed";
	
}