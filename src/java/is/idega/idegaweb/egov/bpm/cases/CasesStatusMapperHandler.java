package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2009/04/21 08:31:20 $ by $Author: arunas $
 */
@Service("casesStatusMapperHandler")
@Scope("singleton")
public class CasesStatusMapperHandler {
	
	private static final String CASE_STATUS_OPEN_KEY = "UBEH";
	private static final String CASE_STATUS_INACTIVE_KEY = "TYST";
	private static final String CASE_STATUS_GRANTED_KEY = "BVJD";
	private static final String CASE_STATUS_DENIED_KEY = "AVSL";
	private static final String CASE_STATUS_REVIEW_KEY = "OMPR";
	private static final String CASE_STATUS_GROU = "GROU";
	private static final String CASE_STATUS_PRELIMINARY_KEY = "PREL";
	private static final String CASE_STATUS_READY_KEY = "KLAR";
	private static final String CASE_STATUS_MOVED_KEY = "FLYT";
	private static final String CASE_STATUS_IN_PROGRESS = "PEND";
	private static final String CASE_STATUS_PLACED_KEY = "PLAC";
	private static final String CASE_STATUS_WAIT = "WAIT";
	private static final String CASE_STATUS_IN_PROCESS = "INPR";
	private static final String CASE_STATUS_DELETED = "DELE";
	private static final String STATUS_EXP = "string_";

	
	public static String evaluateStatusVariableName(String status){
		
		String result = CoreConstants.EMPTY;
	
		if (CASE_STATUS_DENIED_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusDenied;
		else if (CASE_STATUS_GRANTED_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusGranted;
		else if (CASE_STATUS_INACTIVE_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusInactive;
		else if (CASE_STATUS_MOVED_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusMoved;
		else if (CASE_STATUS_OPEN_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusReceived;
		else if (CASE_STATUS_IN_PROGRESS.equals(status))
			result = CasesBPMProcessConstants.caseStatusInProgress;
		else if (CASE_STATUS_PRELIMINARY_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusPreliminary;
		else if (CASE_STATUS_READY_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusReady;
		else if (CASE_STATUS_REVIEW_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusReview;
		else if (CASE_STATUS_GROU.equals(status))
			result = CasesBPMProcessConstants.caseStatusGrou;
		else if (CASE_STATUS_PLACED_KEY.equals(status))
			result = CasesBPMProcessConstants.caseStatusPlaced;
		else if (CASE_STATUS_WAIT.equals(status))
			result = CasesBPMProcessConstants.caseStatusWait;
		else if (CASE_STATUS_IN_PROCESS.equals(status))
			result = CasesBPMProcessConstants.caseStatusInProcess;
		else if (CASE_STATUS_DELETED.equals(status))
			result = CasesBPMProcessConstants.caseStatusDeleted;

		
		return result.equals(CoreConstants.EMPTY) ? new StringBuilder().append(STATUS_EXP).append(status).toString() : result;
	     
	 }
	
	public String getStatusCodeByMappedName (String statusMappedName) {
		String statusKey = CoreConstants.EMPTY;
			
		if ("caseStatusGranted".equals(statusMappedName))
			statusKey = CASE_STATUS_GRANTED_KEY;
		else if ("caseStatusDenied".equals(statusMappedName))
			statusKey = CASE_STATUS_DENIED_KEY;
		else if ("caseStatusInactive".equals(statusMappedName))
			statusKey = CASE_STATUS_INACTIVE_KEY;
		else if ("caseStatusMoved".equals(statusMappedName))
			statusKey = CASE_STATUS_MOVED_KEY;
		else if ("caseStatusOpened".equals(statusMappedName))
			statusKey = CASE_STATUS_OPEN_KEY;
		else if ("caseStatusInProgress".equals(statusMappedName))
			statusKey = CASE_STATUS_IN_PROGRESS;
		else if ("caseStatusPreliminary".equals(statusMappedName))
			statusKey = CASE_STATUS_PRELIMINARY_KEY;
		else if ("caseStatusReady".equals(statusMappedName))
			statusKey = CASE_STATUS_READY_KEY;
		else if ("caseStatusReview".equals(statusMappedName))
			statusKey = CASE_STATUS_REVIEW_KEY;
		else if ("caseStatusPlaced".equals(statusMappedName))
			statusKey = CASE_STATUS_PLACED_KEY;
		else if ("caseStatusWait".equals(statusMappedName))
			statusKey = CASE_STATUS_WAIT;
		else if ("caseStatusInProcess".equals(statusMappedName))
			statusKey = CASE_STATUS_IN_PROCESS;
		else if ("caseStatusDeleted".equals(statusMappedName))
			statusKey = CASE_STATUS_DELETED;
		
		return statusKey;
	}
	
	
}
