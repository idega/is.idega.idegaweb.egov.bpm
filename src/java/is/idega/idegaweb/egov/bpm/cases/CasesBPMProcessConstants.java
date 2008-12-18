package is.idega.idegaweb.egov.bpm.cases;

import com.idega.jbpm.artifacts.ProcessArtifactsProvider;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 * Last modified: $Date: 2008/12/18 09:54:42 $ by $Author: arunas $
 *
 */
public class CasesBPMProcessConstants {
	
	private CasesBPMProcessConstants () {}
	
	public static final String caseIdVariableName = "string_caseId";
	public static final String caseTypeNameVariableName = "string_caseTypeName";
	public static final String caseCategoryNameVariableName = "string_caseCategoryName";
	public static final String caseCreatedDateVariableName = "string_caseCreatedDateString";
	public static final String caseAllocateToVariableName = "string_allocateTo";
	public static final String casePerformerIdVariableName = "string_performerId";
	public static final String caseStatusVariableName = "string_caseStatus";
	public static final String caseOwnerFirstNameVariableName = "string_ownerFirstName";
	public static final String caseOwnerLastNameVariableName = "string_ownerLastName";
	public static final String caseIdentifier = ProcessArtifactsProvider.CASE_IDENTIFIER;
	
	public static final String caseIdentifierNumberParam = "caseIdentifierNumber";
	public static final String userIdActionVariableName = "userId";
	public static final String caseCategoryIdActionVariableName = "caseCategoryId";
	public static final String caseTypeActionVariableName = "caseType";

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
	public static final String caseStatusGrou = "string_caseStatusGrou";
	public static final String caseStatusPlaced = "string_caseStatusPlaced";
	public static final String caseStatusWait = "string_caseStatusWait";
	public static final String caseStatusInProcess = "string_caseStatusInProcess";
	
	
}