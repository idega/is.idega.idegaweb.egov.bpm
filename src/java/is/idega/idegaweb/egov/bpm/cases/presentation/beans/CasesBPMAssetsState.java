package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMProcessViewBean;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMTaskViewBean;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind.Status;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.BPMUserImpl;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.23 $
 *
 * Last modified: $Date: 2008/06/03 09:59:14 $ by $Author: valdas $
 *
 */
@Scope("request")
@Service(CasesBPMAssetsState.beanIdentifier)
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;
	
	public static final String beanIdentifier = "casesBPMAssetsState";
	
	private transient CasesBPMProcessView casesBPMProcessView;
	private Integer caseId;
	private Long processInstanceId;
	private Long viewSelected;
	private Boolean isWatched;
	//private Integer tabSelected;
	//private FacetRendered facetRendered = FacetRendered.ASSETS_GRID;
	private String displayPropertyForStyleAttribute = "block";
	
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
		
		if(casesBPMProcessView == null) 
			casesBPMProcessView = (CasesBPMProcessView)WFUtil.getBeanInstance(CasesBPMProcessView.BEAN_IDENTIFIER);

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
		
		FacesContext ctx = FacesContext.getCurrentInstance();
		IWContext iwc = IWContext.getIWContext(ctx);
		
		try {
			CasesBPMDAO dao = getCasesBPMProcessView().getCasesBPMDAO();
			User performer = iwc.getCurrentUser();
			
			ProcessUserBind caseUser = dao.getProcessUserBind(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
			
			caseUser.setStatus(Status.PROCESS_WATCHED);
			
			dao.merge(caseUser);
			isWatched = null;
			
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Case added to your cases list (My Cases)", null);
			ctx.addMessage(null, msg);
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while updating CaseUser status", e);
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "We were unable to add this case to your watch list due to internal error", null);
			ctx.addMessage(null, msg);
		}
	}
	
	public void removeWatch() {
		
		FacesContext ctx = FacesContext.getCurrentInstance();
		IWContext iwc = IWContext.getIWContext(ctx);
		
		try {
			CasesBPMDAO dao = getCasesBPMProcessView().getCasesBPMDAO();
			User performer = iwc.getCurrentUser();
			
			ProcessUserBind caseUser = dao.getProcessUserBind(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
			
			if(caseUser.getStatus() != null && caseUser.getStatus() == Status.PROCESS_WATCHED)
				caseUser.setStatus(Status.NO_STATUS);
			
			dao.merge(caseUser);
			isWatched = null;
			
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Case removed from your cases list (My Cases)", null);
			ctx.addMessage(null, msg);
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while updating CaseUser status", e);
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "We were unable to remove this case from your watch list due to internal error", null);
			ctx.addMessage(null, msg);
		}
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
			
			FacesContext ctx = FacesContext.getCurrentInstance();
			IWContext iwc = IWContext.getIWContext(ctx);
			
			try {
				CasesBPMDAO dao = getCasesBPMProcessView().getCasesBPMDAO();
				User performer = iwc.getCurrentUser();
				
				ProcessUserBind caseUser = dao.getProcessUserBind(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
				isWatched = Status.PROCESS_WATCHED == caseUser.getStatus();
				
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while checking CaseUser status", e);
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "We were unable to fulfill your request, try again later", null);
				ctx.addMessage(null, msg);
			}
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
}