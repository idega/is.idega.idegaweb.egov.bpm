package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/15 12:37:22 $ by $Author: civilis $
 *
 */
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;

	private Long processInstanceId;
	private String viewSelected;
	private FacetRendered facetRendered = FacetRendered.ASSETS;
	
	public String getViewSelected() {
		return viewSelected;
	}

	public void setViewSelected(String viewSelected) {
		System.out.println("setviewselected: "+viewSelected);
		this.viewSelected = viewSelected;
	}
	
	public void selectTaskView() {
	
		System.out.println("selecting task view: "+getViewSelected());
		facetRendered = FacetRendered.ASSET_VIEW;
	}
	
	public void selectDocumentView() {
		
		System.out.println("selecting document view");
		facetRendered = FacetRendered.ASSET_VIEW;
	}
	
	public boolean isAssetsRendered() {
	
		System.out.println("is assets rendered: "+(facetRendered == FacetRendered.ASSETS));
		return facetRendered == FacetRendered.ASSETS;
	}
	
	public boolean isAssetViewRendered() {
		
		System.out.println("is assetView rendered: "+(facetRendered == FacetRendered.ASSET_VIEW));
		return facetRendered == FacetRendered.ASSET_VIEW;
	}
	
	private enum FacetRendered {
		
		ASSETS,
		ASSET_VIEW
	}
	
	protected Long resolveProcessInstanceId(FacesContext context) {

		String caseIdParam = (String)context.getExternalContext().getRequestParameterMap().get(CasesProcessor.PARAMETER_CASE_PK);
		
		Long processInstanceId = null;
		
		if(caseIdParam != null && !CoreConstants.EMPTY.equals(caseIdParam)) {

			Integer caseId = new Integer(caseIdParam);
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

	public CasesBPMDAO getCasesBPMDAO() {
		return (CasesBPMDAO)WFUtil.getBeanInstance("casesBPMDAO");
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
	}
}