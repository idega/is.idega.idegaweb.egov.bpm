package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2009/04/22 13:17:06 $ by $Author: arunas $
 */
@Service("casesStatusMapperHandler")
@Scope("singleton")
public class CasesStatusMapperHandler {
	
	private static final String CASE_STATUS_OPEN_KEY = "UBEH";
	private static final String CASE_STATUS_INACTIVE_KEY = "TYST";
	private static final String CASE_STATUS_GRANTED_KEY = "BVJD";
	private static final String CASE_STATUS_DENIED_KEY = "AVSL";
	private static final String CASE_STATUS_REVIEW_KEY = "OMPR";
	private static final String CASE_STATUS_GROUPED_KEY = "GROU";
	private static final String CASE_STATUS_PRELIMINARY_KEY = "PREL";
	private static final String CASE_STATUS_READY_KEY = "KLAR";
	private static final String CASE_STATUS_MOVED_KEY = "FLYT";
	private static final String CASE_STATUS_IN_PROGRESS = "PEND";
	private static final String CASE_STATUS_PLACED_KEY = "PLAC";
	private static final String CASE_STATUS_WAIT = "WAIT";
	private static final String CASE_STATUS_IN_PROCESS = "INPR";
	private static final String CASE_STATUS_DELETED = "DELE";
	private static final String STATUS_EXP = "string_";

	
	public String getStatusVariableNameFromStatusCode(String statusCode){
		
		String caseStatusVariableName = CoreConstants.EMPTY;
	
		if (CASE_STATUS_DENIED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusDenied;
		else if (CASE_STATUS_GRANTED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusGranted;
		else if (CASE_STATUS_INACTIVE_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusInactive;
		else if (CASE_STATUS_MOVED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusMoved;
		else if (CASE_STATUS_OPEN_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusReceived;
		else if (CASE_STATUS_IN_PROGRESS.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusInProgress;
		else if (CASE_STATUS_PRELIMINARY_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusPreliminary;
		else if (CASE_STATUS_READY_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusReady;
		else if (CASE_STATUS_REVIEW_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusReview;
		else if (CASE_STATUS_GROUPED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusGrouped;
		else if (CASE_STATUS_PLACED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusPlaced;
		else if (CASE_STATUS_WAIT.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusWait;
		else if (CASE_STATUS_IN_PROCESS.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusInProcess;
		else if (CASE_STATUS_DELETED.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusDeleted;

		
		return caseStatusVariableName.equals(CoreConstants.EMPTY) ? new StringBuilder().append(STATUS_EXP).append(statusCode).toString() : caseStatusVariableName;
	     
	 }
	
	public String getStatusCodeByMappedName (String statusMappedName) {
		
		String statusKey = CoreConstants.EMPTY;
		
		if (CasesBPMProcessConstants.CASE_STATUS_GRANTED_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_GRANTED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_DENIED_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_DENIED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_INACTIVE_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_INACTIVE_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_MOVED_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_MOVED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_OPENED_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_OPEN_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_INPROGRESS_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_IN_PROGRESS;
		else if (CasesBPMProcessConstants.CASE_STATUS_PRELIMINARY_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_PRELIMINARY_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_READY_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_READY_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_REVIEW_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_REVIEW_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_PLACED_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_PLACED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_WAIT_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_WAIT;
		else if (CasesBPMProcessConstants.CASE_STATUS_INPROCESS_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_IN_PROCESS;
		else if (CasesBPMProcessConstants.CASE_STATUS_DELETED_MAPNAME.equals(statusMappedName))
			statusKey = CASE_STATUS_DELETED;
		
		return statusKey;
	}
	
	
}
