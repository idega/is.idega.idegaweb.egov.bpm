package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMProcessViewBean;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMTaskViewBean;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.BPMUserImpl;
import com.idega.jbpm.rights.Right;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.expression.ELUtil;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $
 *
 * Last modified: $Date: 2008/10/16 18:15:35 $ by $Author: juozas $
 *
 */
@Scope("request")
@Service(CasesBPMAssetsState.beanIdentifier)
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;
	
	public static final String beanIdentifier = "casesBPMAssetsState";
	
	@Autowired private transient CasesBPMProcessView casesBPMProcessView;
	private transient ProcessWatch processWatcher;
	
	@Autowired private transient BPMFactory bpmFactory;
	
	private Integer caseId;
	private Long processInstanceId;
	private Long viewSelected;
	private Boolean isWatched;
	//private Integer tabSelected;
	//private FacetRendered facetRendered = FacetRendered.ASSETS_GRID;
	private String displayPropertyForStyleAttribute = "block";
	private Boolean usePDFDownloadColumn = Boolean.TRUE;
	private Boolean allowPDFSigning = Boolean.TRUE;
	
//	private enum FacetRendered {
//		
//		ASSETS_GRID,
//		ASSET_VIEW
//	}
	
	public Long getViewSelected() {
		
		if(viewSelected == null) {
			
			viewSelected = resolveTaskInstanceId();
		}
		
		return viewSelected;
	}
	
	protected Long resolveTaskInstanceId() {
		
		String tiIdParam = (String)FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("tiId");
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
	
	protected Long resolveProcessInstanceId() {
		
		String piIdParam = (String)FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("piId");
		Long piId;
		
		if(piIdParam != null && !CoreConstants.EMPTY.equals(piIdParam)) {

			piId = new Long(piIdParam);
		} else
			piId = null;
		
		return piId;
	}
	
	public Long getProcessInstanceId() {
		
		if(processInstanceId == null) {
			
			if(caseId == null) {
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

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public CasesBPMProcessViewBean getProcessView() {
		
		return getCasesBPMProcessView().getProcessView(getProcessInstanceId(), getCaseId());
	}
	
	public CasesBPMTaskViewBean getTaskView() {
		
		return getCasesBPMProcessView().getTaskView(getViewSelected());
	}

	public CasesBPMProcessView getCasesBPMProcessView() {
		
		if(casesBPMProcessView == null){ 
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMProcessView;
	}

	public Integer getCaseId() {
		
		if(caseId == null) {
			
			if(processInstanceId == null) {
				caseId = resolveCaseId();
				
				if(caseId != null) {
				
					processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
				}
				
			} else {

				caseId = getCasesBPMProcessView().getCaseId(processInstanceId);
			}
		}

		return caseId;
	}
	
	protected Integer resolveCaseId() {
		
		String caseIdParam = (String)FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(CasesProcessor.PARAMETER_CASE_PK);
		Integer caseId;
		
		if(caseIdParam != null && !CoreConstants.EMPTY.equals(caseIdParam)) {

			caseId = new Integer(caseIdParam);
		} else
			caseId = null;
		
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
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
			return "display: none";
		}
		
		return "display: block";
	}
	
	public String getGridStyleClasses() {
		String styleClasses = "caseGrids";
		
		if (getBpmFactory() == null) {
			return styleClasses;
		}
		
		ProcessInstanceW piw = null;
		try {
			piw = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (piw == null) {
			return styleClasses;
		}
		if (piw.hasRight(Right.processHandler)) {
			styleClasses = new StringBuilder(styleClasses).append(" bpmHandler").toString();
		}
		
		return styleClasses;
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
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		String bpmUsrIdStr = (String)fctx.getExternalContext().getRequestParameterMap().get(BPMUserImpl.bpmUsrParam);
		Integer bpmUsrId = bpmUsrIdStr != null && !CoreConstants.EMPTY.equals(bpmUsrIdStr) ? new Integer(bpmUsrIdStr) : null;
		
		BPMUser bpmUsr = getCasesBPMProcessView().getBPMUser(bpmUsrId, null);
		return bpmUsr;
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

}