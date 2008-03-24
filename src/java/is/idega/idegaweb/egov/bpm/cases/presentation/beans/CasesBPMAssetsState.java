package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMProcessViewBean;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView.CasesBPMTaskViewBean;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind.Status;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 *
 * Last modified: $Date: 2008/03/24 17:22:10 $ by $Author: civilis $
 *
 */
@Scope("request")
@Service("casesBPMAssetsState")
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;
	
	private static final String casesBPMDAOBeanIdentifier = "casesBPMDAO";
	private static final String casesBPMProcessViewBeanIdentifier = "casesBPMProcessView";
	
	private transient CasesBPMProcessView casesBPMProcessView;
	private Integer caseId;
	private Long processInstanceId;
	private Long viewSelected;
	private Boolean isWatched;
	private FacetRendered facetRendered = FacetRendered.ASSETS;
	
	private enum FacetRendered {
		
		ASSETS,
		ASSET_VIEW
	}
	
	public Long getViewSelected() {
		return viewSelected;
	}

	public void setViewSelected(Long viewSelected) {
		this.viewSelected = viewSelected;
	}

	public void selectTaskView() {
		facetRendered = FacetRendered.ASSET_VIEW;
	}
	
	public void selectDocumentView() {
		facetRendered = FacetRendered.ASSET_VIEW;
	}
	
	public boolean isAssetsRendered() {
		
		return (getProcessInstanceId() != null || getCaseId() != null) && facetRendered == FacetRendered.ASSETS;
	}
	
	public void showAssets() {
		facetRendered = FacetRendered.ASSETS;
	}
	
	public boolean isAssetViewRendered() {
		return (getProcessInstanceId() != null || getCaseId() != null) && facetRendered == FacetRendered.ASSET_VIEW;
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
	
	protected CaseProcInstBind getCPIBind(Integer caseId, Long processInstanceId) {
		
		if(caseId != null) {
		
			CaseProcInstBind bind = getCasesBPMDAO().getCaseProcInstBindByCaseId(caseId);
			
			if(bind != null) {
			
				return bind;
				
			} else {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "No case process instance bind found for caseId provided: "+caseId);
			}
			
		} else if(processInstanceId != null) {
			
			CaseProcInstBind bind;
			try {
				bind = getCasesBPMDAO().find(CaseProcInstBind.class, processInstanceId);
				
			} catch (Exception e) {
				bind = null;
			}
			
			if(bind != null) {
			
				return bind;
				
			} else {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "No case process instance bind found for process instanceid provided: "+processInstanceId);
			}
		}
		
		return null;
	}

	public Long getProcessInstanceId() {
		
		if(processInstanceId == null) {
			
			if(caseId == null) {
				processInstanceId = resolveProcessInstanceId();
				
				if(processInstanceId != null) {
				
					CaseProcInstBind b = getCPIBind(null, processInstanceId);
					caseId = b.getCaseId();
				}
				
			} else {
				
				CaseProcInstBind b = getCPIBind(caseId, null);
				processInstanceId = b.getProcInstId();
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

	public CasesBPMDAO getCasesBPMDAO() {
		return (CasesBPMDAO)WFUtil.getBeanInstance(casesBPMDAOBeanIdentifier);
	}
	
	public CasesBPMProcessView getCasesBPMProcessView() {
		
		if(casesBPMProcessView == null) 
			casesBPMProcessView = (CasesBPMProcessView)WFUtil.getBeanInstance(casesBPMProcessViewBeanIdentifier);

		return casesBPMProcessView;
	}

	public Integer getCaseId() {
		
		if(caseId == null) {
			
			if(processInstanceId == null) {
				caseId = resolveCaseId();
				
				if(caseId != null) {
				
					CaseProcInstBind b = getCPIBind(caseId, null);
					processInstanceId = b.getProcInstId();
				}
				
			} else {
				
				CaseProcInstBind b = getCPIBind(null, processInstanceId);
				caseId = b.getCaseId();
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
			CasesBPMDAO dao = getCasesBPMDAO();
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
			CasesBPMDAO dao = getCasesBPMDAO();
			User performer = iwc.getCurrentUser();
			
			ProcessUserBind caseUser = dao.getProcessUserBind(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
			caseUser.setStatus(null);
			
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
				CasesBPMDAO dao = getCasesBPMDAO();
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
			
			FacesContext ctx = FacesContext.getCurrentInstance();
			IWContext iwc = IWContext.getIWContext(ctx);
			int userId = iwc.getCurrentUserId();
			
			String errMsg = getCasesBPMProcessView().getCanStartTask(getViewSelected(), userId);
		
			if(errMsg == null)
				return true;
			else
				return false;
		}
		
		return false;
	}
	
	public boolean getCanTakeTask() {
		
		if(getViewSelected() != null) {
			
			FacesContext ctx = FacesContext.getCurrentInstance();
			IWContext iwc = IWContext.getIWContext(ctx);
			int userId = iwc.getCurrentUserId();
			
			String errMsg = getCasesBPMProcessView().getCanTakeTask(getViewSelected(), userId);
		
			if(errMsg == null)
				return true;
			else
				return false;
		}
		
		return false;
	}
	
	protected CasesBusiness getCaseBusiness(IWContext iwc) {
		
		try {
			return (CasesBusiness)IBOLookup.getServiceInstance(iwc, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}