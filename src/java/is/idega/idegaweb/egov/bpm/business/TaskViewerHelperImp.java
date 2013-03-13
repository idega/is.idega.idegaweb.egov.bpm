package is.idega.idegaweb.egov.bpm.business;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.cases.presentation.CaseViewer;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.presentation.UserCases;
import com.idega.builder.business.BuilderLogic;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.presentation.IWContext;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

@Service
@Scope("singleton")
@Transactional
public class TaskViewerHelperImp implements TaskViewerHelper {

	private static final Logger LOGGER = Logger.getLogger(TaskViewerHelperImp.class.getName());
	
	public static final String PROCESS_INSTANCE_ID_PARAMETER = "processInstanceId";
	public static final String TASK_NAME_PARAMETER = "taskName";
	public static final String CASE_ID_PARAMETER = "caseId";
	public static final String BACK_PAGE_PARAMETER = "backPage";
	public static final String TASK_VIEWER_PAGE_REQUESTED_PARAMETER = "taskViewerPageRequested";
	
	@Autowired
	private BuilderLogicWrapper builderLogicWrapper;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	public String getLinkToTheTaskRedirector(IWContext iwc, String basePage, String caseId, Long processInstanceId, String backPage, String taskName) {
		if (iwc == null || StringUtil.isEmpty(basePage) || StringUtil.isEmpty(taskName)) {
			return null;
		}
		
		URIUtil uriUtil = null;
		if (getBpmFactory().getProcessInstanceW(processInstanceId).hasEnded()) {
			uriUtil = new URIUtil(BuilderLogic.getInstance().getFullPageUrlByPageType(iwc, "bpm_assets_view", true));
			uriUtil.setParameter("piId", String.valueOf(processInstanceId));
			return iwc.getIWMainApplication().getTranslatedURIWithContext(uriUtil.getUri());
		}
		
		uriUtil = new URIUtil(basePage);
		uriUtil.setParameter(TASK_VIEWER_PAGE_REQUESTED_PARAMETER, Boolean.TRUE.toString());
		uriUtil.setParameter(CASE_ID_PARAMETER, caseId);
		uriUtil.setParameter(PROCESS_INSTANCE_ID_PARAMETER, String.valueOf(processInstanceId));
		uriUtil.setParameter(TASK_NAME_PARAMETER, taskName);
		if (!StringUtil.isEmpty(backPage)) {
			uriUtil.setParameter(BACK_PAGE_PARAMETER, backPage);
		}

		return iwc.getIWMainApplication().getTranslatedURIWithContext(uriUtil.getUri());
	}
	
	public String getLinkToTheTask(IWContext iwc, String caseId, String taskInstanceId, String backPage) {
		URIUtil uriUtil = new URIUtil(getPageUriForTaskViewer(iwc));
		uriUtil.setParameter(CasesProcessor.PARAMETER_ACTION, String.valueOf(UserCases.ACTION_CASE_MANAGER_VIEW));
		uriUtil.setParameter(CaseViewer.PARAMETER_CASE_PK, caseId);
		uriUtil.setParameter(CasesBPMAssetsState.TASK_INSTANCE_ID_PARAMETER, taskInstanceId);
		if (!StringUtil.isEmpty(backPage)) {
			uriUtil.setParameter(CasesBPMAssetsState.CASES_ASSETS_SPECIAL_BACK_PAGE_PARAMETER, backPage.toString());
		}
		
		return IWMainApplication.getDefaultIWMainApplication().getTranslatedURIWithContext(uriUtil.getUri());
	}
	
	public String getPageUriForTaskViewer(IWContext iwc) {
		String uri = getBuilderLogicWrapper().getBuilderService(iwc).getFullPageUrlByPageType(iwc, BPMUser.defaultAssetsViewPageType, true);
		return StringUtil.isEmpty(uri) ? iwc.getRequestURI() : uri;
	}
	
	public String getTaskInstanceIdForTask(Long processInstanceId, String taskName) {
		if (processInstanceId == null || StringUtil.isEmpty(taskName)) {
			LOGGER.warning("Insufficient data: process instance ID: " + processInstanceId + ", task name: " + taskName);
			return null;
		}
		
		try {
			ProcessInstanceW piw = getBpmFactory().getProcessInstanceW(processInstanceId);
			
			TaskInstanceW gradingTIW = piw.getSingleUnfinishedTaskInstanceForTask(taskName);
			
			if (gradingTIW != null)
				return String.valueOf(gradingTIW.getTaskInstanceId());
			else {
				LOGGER.log(Level.WARNING, "No grading task found for processInstance="+processInstanceId);
				
				return null;
			}
			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting grading task instance for process = " + processInstanceId, e);
			return null;
		}
	}

	public BuilderLogicWrapper getBuilderLogicWrapper() {
		return builderLogicWrapper;
	}

	public void setBuilderLogicWrapper(BuilderLogicWrapper builderLogicWrapper) {
		this.builderLogicWrapper = builderLogicWrapper;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
}
