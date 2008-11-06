package is.idega.idegaweb.egov.bpm.cases;

import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/11/06 09:27:25 $ by $Author: arunas $
 */
public class CasesStatusVariables {
	
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
		
		return result;
	     
	 }
	
	
}
