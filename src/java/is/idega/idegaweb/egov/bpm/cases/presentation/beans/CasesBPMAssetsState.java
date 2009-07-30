package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMProcessViewBean;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMTaskViewBean;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.block.process.presentation.beans.GeneralCasesListBuilder;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.rights.Right;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.47 $
 *
 * Last modified: $Date: 2009/07/14 16:26:58 $ by $Author: valdas $
 *
 */
@Scope("request")
@Service(CasesBPMAssetsState.beanIdentifier)
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;
	
	private static final Logger LOGGER = Logger.getLogger(CasesBPMAssetsState.class.getName());
	
	public static final String beanIdentifier = "casesBPMAssetsState";
	
	public static final String CASES_ASSETS_SPECIAL_BACK_PAGE_PARAMETER = "casesAssetsSpecialBackPage";
	public static final String TASK_INSTANCE_ID_PARAMETER = "tiId";
	
	@Autowired
	private transient CasesBPMProcessView casesBPMProcessView;
	
	private transient ProcessWatch processWatcher;
	
	@Autowired
	private transient BPMFactory bpmFactory;
				
	@Autowired
	private transient GeneralCasesListBuilder casesListBuilder;
	
	private Integer caseId;
	private Long processInstanceId;
	private Long viewSelected;
	private Boolean isWatched;
	private String displayPropertyForStyleAttribute = "block";
	private Boolean usePDFDownloadColumn = Boolean.TRUE;
	private Boolean allowPDFSigning = Boolean.TRUE;
	private Boolean standAloneComponent = Boolean.TRUE;
	private Boolean hideEmptySection = Boolean.FALSE;
	private Boolean showAttachmentStatistics = Boolean.FALSE;
	private Boolean showOnlyCreatorInContacts = Boolean.FALSE;
	private String specialBackPage;
	private String commentsPersistenceManagerIdentifier;
	
	private Boolean showNextTask;
	private Long nextProcessInstanceId;
	private Long nextTaskId;
	private Integer nextCaseId;
	private String currentTaskInstanceName;
	private boolean specialBackPageDecoded;
	private String systemEmailAddress;
	
	public Long getViewSelected() {
		if (viewSelected == null) {
			viewSelected = getResolvedTaskInstanceId();
		}
		return viewSelected;
	}
	
	private Long getResolvedTaskInstanceId() {
		Object newValue = resolveObject(nextTaskId, "nextTaskInstanceIdParameter");
		if (newValue instanceof Long) {
			viewSelected = (Long) newValue;
		} else {
			viewSelected = resolveTaskInstanceId();
		}
		
		return viewSelected;
	}
	
	protected Long resolveTaskInstanceId() {
		
		String tiIdParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(TASK_INSTANCE_ID_PARAMETER);
		Long tiId;
		
		if(tiIdParam != null && !CoreConstants.EMPTY.equals(tiIdParam)) {

			tiId = new Long(tiIdParam);
		} else
			tiId = null;
		
		return tiId;
	}

	public void setViewSelected(Long viewSelected) {
		this.viewSelected = viewSelected;
	}

//	public void selectView() {
//		facetRendered = FacetRendered.ASSET_VIEW;
//	}
	
	public boolean isAssetsRendered() {
		
		return (getViewSelected() == null && (getProcessInstanceId() != null || getCaseId() != null)) /* && facetRendered == FacetRendered.ASSETS_GRID*/;
	}
	
	public boolean isAssetViewRendered() {
		return (getProcessInstanceId() != null || getCaseId() != null) && getViewSelected() != null/* && facetRendered == FacetRendered.ASSET_VIEW*/;
	}
	
	public void showAssets() {
		setViewSelected(null);
	}
	
	public boolean isShowSpecialBackPage() {
		return !StringUtil.isEmpty(specialBackPage);
	}
	
	protected Long resolveProcessInstanceId() {
		
		String piIdParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(ProcessManagerBind.processInstanceIdParam);
		Long piId;
		
		if(piIdParam != null && !CoreConstants.EMPTY.equals(piIdParam)) {

			piId = new Long(piIdParam);
		} else
			piId = null;
		
		return piId;
	}
	
	private Long getResolvedProcessInstanceId() {
		Object newValue = resolveObject(nextProcessInstanceId, "nextProcessInstanceIdParameter");
		if (newValue instanceof Long) {
			processInstanceId = (Long) newValue;
		}
		else {
			if (caseId == null) {
				processInstanceId = resolveProcessInstanceId();
				
				if(processInstanceId != null) {
				
					caseId = getCasesBPMProcessView().getCaseId(processInstanceId);
				}
			} else {
				processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
			}
		}

		return processInstanceId;
	}
	
	public Long getProcessInstanceId() {
		if (processInstanceId == null) {
			processInstanceId = getResolvedProcessInstanceId();
		}
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public CasesBPMProcessViewBean getProcessView() {
		
		return getCasesBPMProcessView().getProcessView(getProcessInstanceId(), getCaseId());
	}
	
	public CasesBPMTaskViewBean getTaskView() {
		
		return getTaskView(getViewSelected());
	}
	
	private CasesBPMTaskViewBean getTaskView(Long taskId) {
		if (taskId == null) {
			return null;
		}
		return getCasesBPMProcessView().getTaskView(taskId);
	}

	public CasesBPMProcessView getCasesBPMProcessView() {
		
		if(casesBPMProcessView == null){ 
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMProcessView;
	}

	public Integer getCaseId() {
		if (caseId == null) {
			caseId = getResolvedCaseId();
		}
		return caseId;
	}
	
	public Integer getResolvedCaseId() {
		Object newValue = resolveObject(nextCaseId, "nextCaseIdParameter");
		if (newValue instanceof Integer) {
			caseId = (Integer) newValue;
		} else {
			if (processInstanceId == null) {
				caseId = resolveCaseId();
				
				if (caseId != null) {
					processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
				}
			} else {
				caseId = getCasesBPMProcessView().getCaseId(processInstanceId);
			}
		}
		
		return caseId;
	}
	
	//	TODO: should be replaced with action listener that would set all properties "naturally"
	private Object resolveObject(Object valueHolder, String parameterName) {
		if (valueHolder == null) {
			return null;
		}
	
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		if (iwc.isParameterSet(parameterName)) {
			return valueHolder;
		}
		
		return null;
	}
	
	protected Integer resolveCaseId() {
		
		String caseIdParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(CasesProcessor.PARAMETER_CASE_PK);
		Integer caseId;
		
		if(caseIdParam != null && !CoreConstants.EMPTY.equals(caseIdParam)) {

			caseId = new Integer(caseIdParam);
		} else
			caseId = null;
		
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
		
		viewSelected = null;
		showNextTask = null;
		nextProcessInstanceId = null;
		nextTaskId = null;
		nextCaseId = null;
	}
	
	public void takeWatch() {
		boolean result = getProcessWatch().takeWatch(getProcessInstanceId());
		isWatched = null;
		
		String message = result ? "Case added to your cases list (My Cases)" : "We were unable to add this case to your watch list due to internal error";
		FacesMessage msg = new FacesMessage(result ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_ERROR, message, null);
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}
	
	public void removeWatch() {
		boolean result = getProcessWatch().removeWatch(getProcessInstanceId());
		isWatched = null;
		
		String message = result ? "Case removed from your cases list (My Cases)" : "We were unable to remove this case from your watch list due to internal error";
		FacesMessage msg = new FacesMessage(result ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_ERROR, message, null);
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}
	
	public String getWatchCaseStatusLabel() {
		return getProcessWatch().getWatchCaseStatusLabel(isWatched());
	}
	
	public String getTasksVisibilityProperty() {
		Boolean processHasEnded = getProcessView().getEnded();
		if (processHasEnded != null && processHasEnded) {
			return "caseListTasksSectionNotVisibleStyleClass";
		}
		
		return "caseListTasksSectionVisibleStyleClass";
	}
	
	public String getGridStyleClasses() {
		String styleClasses = "caseGrids";
		
		if (getBpmFactory() == null) {
			return styleClasses;
		}
		
		ProcessInstanceW piw = getCurrentProcess();
		if (piw == null) {
			return styleClasses;
		}
		if (piw.hasRight(Right.processHandler)) {
			styleClasses = new StringBuilder(styleClasses).append(" bpmHandler").toString();
		}
		
		return styleClasses;
	}
	
	private ProcessInstanceW getProcessInstance(Long processInstanceId) {
		if (processInstanceId == null) {
			return null;
		}
		try {
			return getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
		
	public void startTask() {
		
		if(getViewSelected() != null) {
			
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			getCasesBPMProcessView().startTask(getViewSelected(), iwc.getCurrentUserId());
			
		} else
			throw new RuntimeException("No view selected");
	}
	
	public void assignTask() {
		
		if(getViewSelected() != null) {
			
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			getCasesBPMProcessView().assignTask(getViewSelected(), iwc.getCurrentUserId());	
		
		} else
			throw new RuntimeException("No view selected");
	}
	
	public boolean isWatched() {
		
		if(isWatched == null) {
			isWatched = getProcessWatch().isWatching(getProcessInstanceId());
		}
		
		return isWatched == null ? false : isWatched;
	}
	
	public boolean getCanStartTask() {
		
		if(getViewSelected() != null) {
			
			Integer userId = getCurrentBPMUser().getIdToUse();
			
			if(userId != null) {
				
				String errMsg = getCasesBPMProcessView().getCanStartTask(getViewSelected(), userId);
				
				if(errMsg == null)
					return true;
			}
		}
		
		return false;
	}
	
	public boolean getCanTakeTask() {
		
		if(getViewSelected() != null) {
			
			Integer userId = getCurrentBPMUser().getIdToUse();
			
			if(userId != null) {
				
				String errMsg = getCasesBPMProcessView().getCanTakeTask(getViewSelected(), userId);
				
				if(errMsg == null)
					return true;
			}
		}
		
		return false;
	}
	
//	protected CasesBusiness getCaseBusiness(IWContext iwc) {
//		
//		try {
//			return (CasesBusiness)IBOLookup.getServiceInstance(iwc, CasesBusiness.class);
//		}
//		catch (IBOLookupException ile) {
//			throw new IBORuntimeException(ile);
//		}
//	}

//	public Integer getTabSelected() {
//		return tabSelected == null ? 0 : tabSelected;
//	}
//
//	public void setTabSelected(Integer tabSelected) {
//		this.tabSelected = tabSelected;
//	}
	
	public BPMUser getCurrentBPMUser() {
		
		return getCasesBPMProcessView().getCurrentBPMUser();
	}

	public String getDisplayPropertyForStyleAttribute() {
		return new StringBuilder("display: ").append(displayPropertyForStyleAttribute).append(CoreConstants.SEMICOLON).toString();
	}

	public void setDisplayPropertyForStyleAttribute(boolean displayPropertyForStyleAttribute) {
		this.displayPropertyForStyleAttribute = displayPropertyForStyleAttribute ? "block" : "none";
	}

	private ProcessWatch getProcessWatch() {
		
		if (processWatcher == null) {
			processWatcher = getCasesBPMProcessView().getBPMFactory().getProcessManagerByProcessInstanceId(getProcessInstanceId()).getProcessInstance(getProcessInstanceId()).getProcessWatcher();
		}
	
		return processWatcher;
	}

	public Boolean getUsePDFDownloadColumn() {
		return usePDFDownloadColumn;
	}

	public void setUsePDFDownloadColumn(Boolean usePDFDownloadColumn) {
		this.usePDFDownloadColumn = usePDFDownloadColumn;
	}

	public Boolean getAllowPDFSigning() {
		return allowPDFSigning;
	}

	public void setAllowPDFSigning(Boolean allowPDFSigning) {
		this.allowPDFSigning = allowPDFSigning;
	}

	public BPMFactory getBpmFactory() {
		if(bpmFactory == null){
			ELUtil.getInstance().autowire(this);
		}
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public GeneralCasesListBuilder getCasesListBuilder() {
		if (casesListBuilder == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return casesListBuilder;
	}

	public void setCasesListBuilder(GeneralCasesListBuilder casesListBuilder) {
		this.casesListBuilder = casesListBuilder;
	}

	public String getSendEmailImage() {
		return getCasesListBuilder().getSendEmailImage();
	}

	public String getCaseEmailSubject() {
		if (systemEmailAddress == null) {
			systemEmailAddress = CoreConstants.EMPTY;
			
			Long processInstanceId = getProcessInstanceId();
			String processIdentifier = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId)
				.getProcessIdentifier();
			if (StringUtil.isEmpty(processIdentifier)) {
				LOGGER.warning("Unkown process identifier for process instance: " + processInstanceId);
				return null;
			}
			
			String emailAdress = getCasesListBuilder().getEmailAddressMailtoFormattedWithSubject(processIdentifier);
			if (StringUtil.isEmpty(emailAdress) || emailAdress.equals(processIdentifier)) {
				LOGGER.warning("Email adress is invalid: " + emailAdress);
				return null;
			}
			
			systemEmailAddress = emailAdress;
		}
		
		return systemEmailAddress;
	}
	
	public String getSendEmailTitle() {
		return getCasesListBuilder().getTitleSendEmail();
	}
	
	public Boolean getStandAloneComponent() {
		return standAloneComponent;
	}

	public void setStandAloneComponent(Boolean standAloneComponent) {
		this.standAloneComponent = standAloneComponent;
	}

	public boolean getRenderCaseEmailContainer() {
		return getStandAloneComponent() && !StringUtil.isEmpty(getCaseEmailSubject());
	}

	public Boolean getHideEmptySection() {
		return hideEmptySection;
	}

	public void setHideEmptySection(Boolean hideEmptySection) {
		this.hideEmptySection = hideEmptySection;
	}
	
	public boolean isShowNextTask() {
		if (showNextTask == null) {
			IWContext iwc = CoreUtil.getIWContext();
			String id = iwc.getRequestURI();
			
			showNextTask = Boolean.FALSE;
			
			if (!getCasesSearchResultsHolder().isSearchResultStored(id)) {
				return showNextTask;
			}
			
			Long nextTaskId = getNextTaskId(id, getNextCaseId());
			if (nextTaskId == null) {
				LOGGER.info("NOT shoing next task - couldn't resolve IDs for next task");
				return showNextTask;
			}

			showNextTask = Boolean.TRUE;
		}
		return showNextTask;
	}
	
	public Integer getNextCaseId() {
		IWContext iwc = CoreUtil.getIWContext();
		if (nextCaseId == null || (iwc != null && iwc.isParameterSet("nextCaseIdParameter"))) {
			String id = iwc.getRequestURI();
			nextCaseId = getNextCaseId(id, getCaseId());
		}
		return nextCaseId;
	}
	
	private Integer getNextCaseId(String id, Integer caseId) {
		ProcessInstanceW currentProcess = getCurrentProcess();
		if (currentProcess == null) {
			LOGGER.info("Cannot resolve next case id: current process is unknown! Case ID: " + caseId);
			return null;
		}
		
		return getCasesSearchResultsHolder().getNextCaseId(id, caseId);
	}
	
	public Long getNextTaskId() {
		if (nextTaskId == null) {
			IWContext iwc = CoreUtil.getIWContext();
			String id = iwc.getRequestURI();
			nextTaskId = getNextTaskId(id, getNextCaseId());
		}
		return nextTaskId;
	}
	
	private Long getNextTaskId(String id, Integer nextCaseId) {
		if (nextTaskId == null) {
			if (nextCaseId == null) {
				return null;
			}
			
			ProcessInstanceW nextProcessInstance = getProcessInstance(getCasesBPMProcessView().getProcessInstanceId(nextCaseId));
			if (nextProcessInstance == null) {
				LOGGER.warning("Process instance was not found for case: " + nextCaseId);
				return getNextTaskId(id, getNextCaseId(id, nextCaseId));
			}
			
			String currentTaskName = getCurrentTaskInstanceName();
			if (StringUtil.isEmpty(currentTaskName)) {
				LOGGER.warning("Cannot resolve current task's name for task instance: " + getViewSelected());
				return null;
			}

			List<TaskInstanceW> allUnfinishedTasks = null;
			try {
				allUnfinishedTasks = nextProcessInstance.getAllUnfinishedTaskInstances();
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting unfinished tasks for process instance: " + nextProcessInstance.getProcessInstanceId(), e);
				return getNextTaskId(id, getNextCaseId(id, nextCaseId));
			}
			if (ListUtil.isEmpty(allUnfinishedTasks)) {
				LOGGER.warning("There are no unfinished tasks for process instance: " + nextProcessInstance.getProcessInstanceId());
				return getNextTaskId(id, getNextCaseId(id, nextCaseId));
			}
			
			boolean found = false;
			for (Iterator<TaskInstanceW> tasksIter = allUnfinishedTasks.iterator(); (!found && tasksIter.hasNext());) {
				TaskInstanceW task = tasksIter.next();
				
				if (currentTaskName.equals(task.getTaskInstance().getName())) {
					this.nextProcessInstanceId = nextProcessInstance.getProcessInstanceId();
					this.nextTaskId = task.getTaskInstanceId();
					this.nextCaseId = nextCaseId;
					
					found = true;
				}
			}
			
			if (nextTaskId == null) {
				//	Particular task was not found - searching for it in next process instance
				return getNextTaskId(id, getNextCaseId(id, nextCaseId));
			}
		}
		return nextTaskId;
	}
	
	public String getNextTaskName() {
		try {
			IWContext iwc = CoreUtil.getIWContext();
			IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
			return new StringBuilder(iwrb.getLocalizedString("cases_bpm.go_to_next_task", "Go to next task")).append(": ")
				.append(getTaskView(getNextTaskId()).getTaskName()).toString();
		} catch(Exception e) {}
		return null;
	}
	
	private ProcessInstanceW getCurrentProcess() {
		return getProcessInstance(processInstanceId);
	}
	
	private String getCurrentTaskInstanceName() {
		if (StringUtil.isEmpty(currentTaskInstanceName)) {
			currentTaskInstanceName = getBpmFactory().getProcessManagerByTaskInstanceId(getViewSelected()).getTaskInstance(getViewSelected()).getTaskInstance()
				.getName();
		}
		return currentTaskInstanceName;
	}
	
	private CasesSearchResultsHolder getCasesSearchResultsHolder() {
		return ELUtil.getInstance().getBean(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER);
	}

	public String getSpecialBackPage() {
		if (!specialBackPageDecoded) {
			specialBackPageDecoded = true;
			try {
				specialBackPage = URLDecoder.decode(specialBackPage, CoreConstants.ENCODING_UTF8);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return specialBackPage;
	}

	public void setSpecialBackPage(String specialBackPage) {
		this.specialBackPage = specialBackPage;
	}

	public String getCommentsPersistenceManagerIdentifier() {
		return commentsPersistenceManagerIdentifier;
	}

	public void setCommentsPersistenceManagerIdentifier(String commentsPersistenceManagerIdentifier) {
		this.commentsPersistenceManagerIdentifier = commentsPersistenceManagerIdentifier;
	}

	public Boolean getShowAttachmentStatistics() {
		return showAttachmentStatistics;
	}

	public void setShowAttachmentStatistics(Boolean showAttachmentStatistics) {
		this.showAttachmentStatistics = showAttachmentStatistics;
	}

	public Boolean getShowOnlyCreatorInContacts() {
		return showOnlyCreatorInContacts;
	}

	public void setShowOnlyCreatorInContacts(Boolean showOnlyCreatorInContacts) {
		this.showOnlyCreatorInContacts = showOnlyCreatorInContacts;
	}

}