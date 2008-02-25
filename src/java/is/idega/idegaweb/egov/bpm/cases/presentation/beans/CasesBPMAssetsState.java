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

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseUser;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/02/25 16:16:25 $ by $Author: civilis $
 *
 */
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;
	
	private static final String casesBPMDAOBeanIdentifier = "casesBPMDAO";
	private static final String casesBPMProcessViewBeanIdentifier = "casesBPMProcessView";

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
		return facetRendered == FacetRendered.ASSETS;
	}
	
	public void showAssets() {
		facetRendered = FacetRendered.ASSETS;
	}
	
	public boolean isAssetViewRendered() {
		return facetRendered == FacetRendered.ASSET_VIEW;
	}
	
	protected Long resolveProcessInstanceId(FacesContext context) {

		Long processInstanceId = null;
		
		Integer caseId = getCaseId();
		
		if(caseId != null) {
			
			CaseProcInstBind bind = getCasesBPMDAO().getCaseProcInstBindByCaseId(caseId);
			
			if(bind != null) {
			
				processInstanceId = bind.getProcInstId();
				
			} else {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No case process instance bind found for caseId provided: "+caseId);
			}
		}
		
		return processInstanceId;
	}

	public Long getProcessInstanceId() {
		
		if(processInstanceId == null)
			processInstanceId = resolveProcessInstanceId(FacesContext.getCurrentInstance());
		
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public CasesBPMProcessViewBean getProcessView() {
		
		return getCasesBPMProcessView().getProcessView(getProcessInstanceId());
	}
	
	public CasesBPMTaskViewBean getTaskView() {
		
		return getCasesBPMProcessView().getTaskView(getViewSelected());
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return (CasesBPMDAO)WFUtil.getBeanInstance(casesBPMDAOBeanIdentifier);
	}
	
	public CasesBPMProcessView getCasesBPMProcessView() {

		return (CasesBPMProcessView)WFUtil.getBeanInstance(casesBPMProcessViewBeanIdentifier);
	}

	public Integer getCaseId() {
		
		if(caseId == null) {

			String caseIdParam = (String)FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(CasesProcessor.PARAMETER_CASE_PK);
			
			if(caseIdParam != null && !CoreConstants.EMPTY.equals(caseIdParam)) {

				Integer caseId = new Integer(caseIdParam);
				this.caseId = caseId;
			}
		}

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
			
			CaseUser caseUser = dao.getCaseUser(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
			caseUser.setStatus(CaseUser.PROCESS_WATCHED_STATUS);
			
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
			
			CaseUser caseUser = dao.getCaseUser(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
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
	
	public boolean isWatched() {
		
		if(isWatched == null) {
			
			FacesContext ctx = FacesContext.getCurrentInstance();
			IWContext iwc = IWContext.getIWContext(ctx);
			
			try {
				CasesBPMDAO dao = getCasesBPMDAO();
				User performer = iwc.getCurrentUser();
				
				CaseUser caseUser = dao.getCaseUser(getProcessInstanceId(), new Integer(performer.getPrimaryKey().toString()), true);
				
				isWatched = CaseUser.PROCESS_WATCHED_STATUS.equals(caseUser.getStatus());
				
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while checking CaseUser status", e);
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "We were unable to fulfill your request, try again later", null);
				ctx.addMessage(null, msg);
			}
		}
		
		return isWatched;
	}
	
	public CasesBusiness getCaseBusiness(IWContext iwc) {
		
		try {
			return (CasesBusiness)IBOLookup.getServiceInstance(iwc, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}