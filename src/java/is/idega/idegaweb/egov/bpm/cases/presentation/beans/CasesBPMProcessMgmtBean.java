package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.CaseType;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.bundle.ProcessBundle;
import com.idega.jbpm.bundle.ProcessBundleManager;
import com.idega.util.CoreConstants;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/05/19 13:53:25 $ by $Author: civilis $
 *
 */
@Scope("request")
@Service("CasesBPMProcessMgmt")
public class CasesBPMProcessMgmtBean {
	
	private String message;
	private String caseCategory;
	private String caseType;
	private Long processDefinitionId;

	private ProcessBundleManager processBundleManager;
	private CasesBPMDAO casesBPMDAO;
	private ProcessBundle processBundle;
	private IdegaJbpmContext idegaJbpmContext;
	
	private List<SelectItem> casesTypes = new ArrayList<SelectItem>();
	private List<SelectItem> casesCategories = new ArrayList<SelectItem>();
	private List<SelectItem> processesDefinitions = new ArrayList<SelectItem>();

	public String assignProcessToCaseMeta() {
		
		if(getProcessDefinitionId() == null) {
			setMessage("Process definition not chosen");
			return null;
		}
		
		if(getCaseCategory() == null || CoreConstants.EMPTY.equals(getCaseCategory())) {
			setMessage("Case category not chosen");
			return null;
		}
		
		if(getCaseType() == null || CoreConstants.EMPTY.equals(getCaseType())) {
			setMessage("Case type not chosen");
			return null;
		}
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
			
		try {
			Long caseCategoryId = new Long(getCaseCategory());
			Long caseTypeId = new Long(getCaseType());
			Long pdId = getProcessDefinitionId();
			
			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(pdId);
			CaseTypesProcDefBind ctpd = getCasesBPMDAO().getCaseTypesProcDefBindByPDName(pd.getName());
			
			if(ctpd != null) {
				
				ctpd.setCasesCategoryId(caseCategoryId);
				ctpd.setCasesTypeId(caseTypeId);
				getCasesBPMDAO().updateCaseTypesProcDefBind(ctpd);
				
			} else {
			
				CaseTypesProcDefBind bind = new CaseTypesProcDefBind();
				bind.setCasesCategoryId(caseCategoryId);
				bind.setCasesTypeId(caseTypeId);
				bind.setProcessDefinitionName(pd.getName());
				getCasesBPMDAO().persist(bind);
			}
			
		} catch (Exception e) {
			setMessage("Exception occured");
			e.printStackTrace();
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
		
		return null;
	}
	
	public String getMessage() {
		return message == null ? CoreConstants.EMPTY : message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<SelectItem> getCasesTypes() {
		
		casesTypes.clear();
		
		try {
			
			@SuppressWarnings("unchecked")
			Collection<CaseType> types = getCasesBusiness(IWMainApplication.getIWMainApplication(FacesContext.getCurrentInstance()).getIWApplicationContext())
			.getCaseTypes();
			
			for (CaseType caseType : types) {
				
				SelectItem item = new SelectItem();
				
//				it is done in the same manner (toString for primary key), so anyway.. :\ 
				item.setValue(caseType.getPrimaryKey().toString());
				item.setLabel(caseType.getName());
				casesTypes.add(item);
			}
			
			return casesTypes;
			
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setCasesTypes(List<SelectItem> casesTypes) {
		this.casesTypes = casesTypes;
	}

	public List<SelectItem> getCasesCategories() {
		
		casesCategories.clear();
		
		try {
			
			@SuppressWarnings("unchecked")
			Collection<CaseCategory> categories = getCasesBusiness(IWMainApplication.getIWMainApplication(FacesContext.getCurrentInstance()).getIWApplicationContext())
			.getCaseCategories();
			
			for (CaseCategory caseCategory : categories) {
				
				SelectItem item = new SelectItem();
				
//				it is done in the same manner (toString for primary key), so anyway.. :\ 
				item.setValue(caseCategory.getPrimaryKey().toString());
				item.setLabel(caseCategory.getName());
				casesCategories.add(item);
			}
			
			return casesCategories;
			
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setCasesCategories(List<SelectItem> casesCategories) {
		this.casesCategories = casesCategories;
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getCaseCategory() {
		
		if(caseCategory == null && getProcessDefinitionId() != null) {
			
			JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(getProcessDefinitionId());
				CaseTypesProcDefBind ctpd = getCasesBPMDAO().getCaseTypesProcDefBindByPDName(pd.getName());
				
				if(ctpd != null) {
					
					setCaseCategory(String.valueOf(ctpd.getCasesCategoryId()));
					setCaseType(String.valueOf(ctpd.getCasesTypeId()));
				}
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(ctx);
			}
		}
		return caseCategory;
	}

	public void setCaseCategory(String caseCategory) {
		this.caseCategory = caseCategory;
	}

	public String getCaseType() {
		
		if(caseType == null && getProcessDefinitionId() != null) {
			
			JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(getProcessDefinitionId());
				CaseTypesProcDefBind ctpd = getCasesBPMDAO().getCaseTypesProcDefBindByPDName(pd.getName());
				
				if(ctpd != null) {
					
					setCaseCategory(String.valueOf(ctpd.getCasesCategoryId()));
					setCaseType(String.valueOf(ctpd.getCasesTypeId()));
				}
			} finally {
				getIdegaJbpmContext().closeAndCommit(ctx);
			}
		}
		return caseType;
	}

	public void setCaseType(String caseType) {
		this.caseType = caseType;
	}
	
//	rename to getLatestProcessDefinitions
	public List<SelectItem> getCasesProcessesDefinitions() {

		processesDefinitions.clear();
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			@SuppressWarnings("unchecked")
			List<ProcessDefinition> pds = ctx.getGraphSession().findLatestProcessDefinitions();
			
			for (ProcessDefinition processDefinition : pds) {
				
				SelectItem item = new SelectItem();
				
				item.setValue(processDefinition.getId());
				item.setLabel(processDefinition.getName());
				processesDefinitions.add(item);
			}
			
		} catch (Exception e) {
			setMessage("Exception occured");
			e.printStackTrace();
			processesDefinitions.clear();
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
		
		return processesDefinitions;
	}

	public ProcessBundleManager getProcessBundleManager() {
		return processBundleManager;
	}

	@Autowired
	public void setProcessBundleManager(ProcessBundleManager processBundleManager) {
		this.processBundleManager = processBundleManager;
	}

	public ProcessBundle getProcessBundle() {
		return processBundle;
	}

	@Autowired
	public void setProcessBundle(ProcessBundle processBundle) {
		this.processBundle = processBundle;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public void selectedProcessChanged(ValueChangeEvent event) {
		
		PhaseId phaseId = event.getPhaseId();
		
		if (phaseId.equals(PhaseId.ANY_PHASE)) {
			
			event.setPhaseId(PhaseId.UPDATE_MODEL_VALUES);
			event.queue();
			
		} else if (phaseId.equals(PhaseId.UPDATE_MODEL_VALUES)) {
			
			if(event.getNewValue() != null && event.getOldValue() != null && !event.getNewValue().equals(event.getOldValue())) {
				caseCategory = null;
				caseType = null;
			}
			
			processDefinitionId = (Long)event.getNewValue();
		}
	}
}