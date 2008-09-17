package is.idega.idegaweb.egov.bpm.cases;

import com.idega.jbpm.artifacts.ProcessArtifactsProvider;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/09/17 13:14:19 $ by $Author: civilis $
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
	public static final String caseDescription = ProcessArtifactsProvider.CASE_DESCRIPTION;
	
	public static final String caseIdentifierNumberParam = "caseIdentifierNumber";
	public static final String userIdActionVariableName = "userId";
	public static final String caseCategoryIdActionVariableName = "caseCategoryId";
	public static final String caseTypeActionVariableName = "caseType";
	
	public static final String caseStatusReceivedVariableName = "string_caseStatusReceived";
	public static final String caseStatusInProgressVariableName = "string_caseStatusInProgress";
	public static final String caseStatusClosedVariableName = "string_caseStatusClosed";
}