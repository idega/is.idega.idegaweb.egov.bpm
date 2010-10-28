package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.CaseBMPBean;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.5 $ Last modified: $Date: 2009/06/23 10:22:01 $ by $Author: valdas $
 */
@Service("casesStatusMapperHandler")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CasesStatusMapperHandler {
	
	private static final String CASE_STATUS_OPEN_KEY = CaseBMPBean.CASE_STATUS_OPEN_KEY;
	private static final String CASE_STATUS_CREATED_KEY = CaseBMPBean.CASE_STATUS_CREATED_KEY;
	private static final String CASE_STATUS_INACTIVE_KEY = CaseBMPBean.CASE_STATUS_INACTIVE_KEY;
	private static final String CASE_STATUS_GRANTED_KEY = CaseBMPBean.CASE_STATUS_GRANTED_KEY;
	private static final String CASE_STATUS_DENIED_KEY = CaseBMPBean.CASE_STATUS_DENIED_KEY;
	private static final String CASE_STATUS_REVIEW_KEY = CaseBMPBean.CASE_STATUS_REVIEW_KEY;
	private static final String CASE_STATUS_GROUPED_KEY = CaseBMPBean.CASE_STATUS_GROUPED_KEY;
	private static final String CASE_STATUS_PRELIMINARY_KEY = CaseBMPBean.CASE_STATUS_PRELIMINARY_KEY;
	private static final String CASE_STATUS_READY_KEY = CaseBMPBean.CASE_STATUS_READY_KEY;
	private static final String CASE_STATUS_MOVED_KEY = CaseBMPBean.CASE_STATUS_MOVED_KEY;
	private static final String CASE_STATUS_IN_PROGRESS = CaseBMPBean.CASE_STATUS_PENDING_KEY;
	private static final String CASE_STATUS_PLACED_KEY = CaseBMPBean.CASE_STATUS_PLACED_KEY;
	private static final String CASE_STATUS_WAIT_KEY = CaseBMPBean.CASE_STATUS_WAITING_KEY;
	private static final String CASE_STATUS_IN_PROCESS_KEY = CaseBMPBean.CASE_STATUS_IN_PROCESS_KEY;
	private static final String CASE_STATUS_DELETED_KEY = CaseBMPBean.CASE_STATUS_DELETED_KEY;
	private static final String CASE_STATUS_FINISHED_KEY = CaseBMPBean.CASE_STATUS_FINISHED_KEY;
	private static final String CASE_STATUS_CLOSED_KEY = CaseBMPBean.CASE_STATUS_CLOSED;
	
	private static final String STATUS_EXP = "string_";
	
	public String getStatusVariableNameFromStatusCode(String statusCode) {
		
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
		else if (CASE_STATUS_WAIT_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusWait;
		else if (CASE_STATUS_IN_PROCESS_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusInProcess;
		else if (CASE_STATUS_DELETED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusDeleted;
		else if (CASE_STATUS_CREATED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusCreated;
		else if (CASE_STATUS_FINISHED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusFinished;
		else if (CASE_STATUS_CLOSED_KEY.equals(statusCode))
			caseStatusVariableName = CasesBPMProcessConstants.caseStatusFinished;
		
		return caseStatusVariableName.equals(CoreConstants.EMPTY) ? new StringBuilder()
		        .append(STATUS_EXP).append(statusCode).toString()
		        : caseStatusVariableName;
		
	}
	
	public String getStatusCodeByMappedName(String statusMappedName) {
		
		String statusKey = CoreConstants.EMPTY;
		
		if (CasesBPMProcessConstants.CASE_STATUS_GRANTED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_GRANTED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_DENIED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_DENIED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_INACTIVE_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_INACTIVE_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_MOVED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_MOVED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_OPENED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_OPEN_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_INPROGRESS_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_IN_PROGRESS;
		else if (CasesBPMProcessConstants.CASE_STATUS_PRELIMINARY_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_PRELIMINARY_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_READY_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_READY_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_REVIEW_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_REVIEW_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_PLACED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_PLACED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_WAIT_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_WAIT_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_INPROCESS_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_IN_PROCESS_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_DELETED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_DELETED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_CREATED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_CREATED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_FINISHED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_FINISHED_KEY;
		else if (CasesBPMProcessConstants.CASE_STATUS_CLOSED_MAPNAME
		        .equals(statusMappedName))
			statusKey = CASE_STATUS_CLOSED_KEY;
		
		return statusKey;
	}
	
}
