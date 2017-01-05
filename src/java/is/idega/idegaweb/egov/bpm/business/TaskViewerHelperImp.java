package is.idega.idegaweb.egov.bpm.business;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.cases.presentation.CaseViewer;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.datastructures.map.MapUtil;

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

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.business.TaskViewerHelper#getLinksToTheTaskRedirector(com.idega.presentation.IWContext, java.util.Map, java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getLinksToTheTaskRedirector(
			IWContext iwc, 
			Map<Long, ProcessInstanceW> relations, 
			boolean backPage, 
			String taskName) {

		if (iwc == null || StringUtil.isEmpty(taskName) || MapUtil.isEmpty(relations)) {
			return Collections.emptyMap();
		}

		Map<Long, String> casesWithLinks = new HashMap<Long, String>(relations.size());
		String pageURIToAssetsView = BuilderLogic.getInstance().getFullPageUrlByPageType(
				iwc, "bpm_assets_view", true);
		String baseUri = getCurrentPageUri(iwc);
		if (StringUtil.isEmpty(baseUri)) {
			LOGGER.warning("Failed to get base uri for page!");
			return Collections.emptyMap();
		}

		for (Long theCase: relations.keySet()) {
			ProcessInstanceW piw = relations.get(theCase);
			URIUtil uriUtil = null;
			if (piw.hasEnded()) {
				uriUtil = new URIUtil(pageURIToAssetsView);
				uriUtil.setParameter("piId", String.valueOf(piw.getId()));
			} else {
				uriUtil = new URIUtil(baseUri);
				uriUtil.setParameter(TASK_VIEWER_PAGE_REQUESTED_PARAMETER, Boolean.TRUE.toString());
				uriUtil.setParameter(CASE_ID_PARAMETER, theCase.toString());
				uriUtil.setParameter(PROCESS_INSTANCE_ID_PARAMETER, String.valueOf(piw.getId()));
				uriUtil.setParameter(TASK_NAME_PARAMETER, taskName);
				if (backPage) {
					uriUtil.setParameter(BACK_PAGE_PARAMETER, baseUri);
				}
			}

			casesWithLinks.put(
					theCase, 
					iwc.getIWMainApplication().getTranslatedURIWithContext(uriUtil.getUri()));
		}		

		return casesWithLinks;
	}

	/**
	 * 
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected String getCurrentPageUri(IWContext iwc) {
		String uri = null;

		if (CoreUtil.isSingleComponentRenderingProcess(iwc)) {
			try {
				uri = getBuilderLogicWrapper().getBuilderService(iwc).getCurrentPageURI(iwc);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting current page's uri!", e);
			}
		}

		if (StringUtil.isEmpty(uri)) {
			uri = iwc.getRequestURI();
		}

		return uri;
	}

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
