package is.idega.idegaweb.egov.bpm.cases.exe;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.CasesStatusVariables;
import is.idega.idegaweb.egov.bpm.cases.manager.CasesBPMCaseManagerImpl;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CaseManager;
import com.idega.block.process.data.CaseStatus;
import com.idega.bpm.exe.DefaultBPMProcessDefinitionW;
import com.idega.bpm.xformsview.XFormsView;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.AppProcBindDefinition;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWContext;
import com.idega.presentation.PresentationObject;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $
 *
 * Last modified: $Date: 2008/12/02 09:34:45 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPDW")
public class CasesBPMProcessDefinitionW extends DefaultBPMProcessDefinitionW {
	
	@Autowired private CasesBPMDAO casesBPMDAO;
	@Autowired private CaseIdentifier caseIdentifier;
	@Autowired private CaseManager caseManager;
	
	private static final Logger logger = Logger.getLogger(CasesBPMProcessDefinitionW.class.getName());
	@SuppressWarnings("unchecked")
	@Override
	public void startProcess(View view) {
		
		Long startTaskInstanceId = view.getTaskInstanceId();
		
		if(startTaskInstanceId == null)
			throw new IllegalArgumentException("View without taskInstanceId provided");
		
		Map<String, String> parameters = view.resolveParameters();
		
		final Integer userId;
		if(parameters.containsKey(CasesBPMProcessConstants.userIdActionVariableName))
			userId = Integer.parseInt(parameters.get(CasesBPMProcessConstants.userIdActionVariableName));
		else
			userId = null;
		
		Long caseCatId = Long.parseLong(parameters.get(CasesBPMProcessConstants.caseCategoryIdActionVariableName));
		Long caseTypeId = Long.parseLong(parameters.get(CasesBPMProcessConstants.caseTypeActionVariableName));
		Integer identifierNumber = Integer.parseInt(parameters.get(CasesBPMProcessConstants.caseIdentifierNumberParam));
		
		JbpmContext ctx = getBpmContext().createJbpmContext();
		
		try {
//			TODO: check if this is really start task instance id
			
			TaskInstance ti = ctx.getTaskInstance(startTaskInstanceId);
			
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			
			final User user;
			if(userId != null)
				user = getUserBusiness(iwc).getUser(userId);
			else
				user = null;
			
			ProcessInstance pi = ti.getProcessInstance();
			
			IWMainApplication iwma = iwc.getApplicationContext().getIWMainApplication();
			
			CasesBusiness casesBusiness = getCasesBusiness(iwc);		
			
			GeneralCase genCase = casesBusiness.storeGeneralCase(user, caseCatId, caseTypeId, /*attachment pk*/null, "This is simple cases-jbpm-formbuilder integration example.", null, CasesBPMCaseManagerImpl.caseHandlerType, /*isPrivate*/false, getCasesBusiness(iwc).getIWResourceBundleForUser(user, iwc, iwma.getBundle(PresentationObject.CORE_IW_BUNDLE_IDENTIFIER)), false);
			logger.log(Level.INFO, "Case (id="+genCase.getPrimaryKey()+") created for process instance "+pi.getId());

			pi.setStart(new Date());
			
			Map<String, Object> caseData = new HashMap<String, Object>();
			caseData.put(CasesBPMProcessConstants.caseIdVariableName, genCase.getPrimaryKey().toString());
			caseData.put(CasesBPMProcessConstants.caseTypeNameVariableName, genCase.getCaseType().getName());
			caseData.put(CasesBPMProcessConstants.caseCategoryNameVariableName, genCase.getCaseCategory().getName());
			caseData.put(CasesBPMProcessConstants.caseStatusVariableName, genCase.getCaseStatus().getStatus());
			caseData.put(CasesBPMProcessConstants.caseStatusClosedVariableName, casesBusiness.getCaseStatusReady().getStatus());
							
			Collection<CaseStatus>	allStatuses = casesBusiness.getCaseStatuses();
				
			for (CaseStatus caseStatus : allStatuses) 
				caseData.put(CasesStatusVariables.evaluateStatusVariableName(caseStatus.getStatus()), caseStatus.getStatus());
			
			IWTimestamp created = new IWTimestamp(genCase.getCreated());
			caseData.put(CasesBPMProcessConstants.caseCreatedDateVariableName, created.getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
			
			getVariablesHandler().submitVariables(caseData, startTaskInstanceId, false);
			submitVariablesAndProceedProcess(ti, view.resolveVariables(), true);
			
			CaseProcInstBind bind = new CaseProcInstBind();
			bind.setCaseId(new Integer(genCase.getPrimaryKey().toString()));
			bind.setProcInstId(ti.getProcessInstance().getId());
			bind.setCaseIdentierID(identifierNumber);
			bind.setDateCreated(created.getDate());
			getCasesBPMDAO().persist(bind);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			getBpmContext().closeAndCommit(ctx);
		}
	}
	
	@Override
	public View loadInitView(Integer initiatorId) {
		
		JbpmContext ctx = getBpmContext().createJbpmContext();
		
		try {
			Long processDefinitionId = getProcessDefinitionId();
			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(processDefinitionId);

//			TODO: if not bound, then create default case category, case type and bind to it
			CaseTypesProcDefBind bind = getCasesBPMDAO().find(CaseTypesProcDefBind.class, pd.getName());
			
			ProcessInstance pi = new ProcessInstance(pd);
			
			logger.log(Level.INFO, "New process instance created for the process "+pd.getName());
			
			TaskInstance taskInstance = pi.getTaskMgmtInstance().createStartTaskInstance();
			
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			View view = getBpmFactory().takeView(taskInstance.getId(), true, preferred);
			
			Object[] identifiers = getCaseIdentifier().generateNewCaseIdentifier();
			Integer identifierNumber = (Integer)identifiers[0];
			String identifier = (String)identifiers[1];
			
//			move this to protected method setupInitView(view:View):void
			Map<String, String> parameters = new HashMap<String, String>(6);
			
			parameters.put(ProcessConstants.START_PROCESS, ProcessConstants.START_PROCESS);
			parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstance.getId()));
			
			if(initiatorId != null)
				parameters.put(CasesBPMProcessConstants.userIdActionVariableName, initiatorId.toString());
			
			parameters.put(CasesBPMProcessConstants.caseCategoryIdActionVariableName, String.valueOf(bind.getCasesCategoryId()));
			parameters.put(CasesBPMProcessConstants.caseTypeActionVariableName, String.valueOf(bind.getCasesTypeId()));
			parameters.put(CasesBPMProcessConstants.caseIdentifierNumberParam, String.valueOf(identifierNumber));
			
			view.populateParameters(parameters);
			
			HashMap<String, Object> vars = new HashMap<String, Object>(1);
			vars.put(CasesBPMProcessConstants.caseIdentifier, identifier);
			view.populateVariables(vars);
			
//			--
			
			return view;
		
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
			
		} finally {
			
			getBpmContext().closeAndCommit(ctx);
		}
	}
	
	@Override
	public List<String> getRolesCanStartProcess(Object context) {
		
		Integer appId = new Integer(context.toString());
		
		ProcessDefinition pd = getProcessDefinition();
		AppProcBindDefinition def = (AppProcBindDefinition)pd.getDefinition(AppProcBindDefinition.class);
		
		final List<String> rolesCanStart;
		
		if(def != null) {
			
			rolesCanStart = def.getRolesCanStartProcess(appId);
		} else
			rolesCanStart = null;
		
		return rolesCanStart;
	}
	
	/**
	 * sets roles, whose users can start process (and see application).
	 * @param roles - idega roles keys (<b>not</b> process roles)
	 * @param context - some context depending implementation, e.g., roles can start process using applications - then context will be application id
	 */
	@Override
	public void setRolesCanStartProcess(List<String> roles, Object context) {
		
		ProcessDefinition pd = getProcessDefinition();
		AppProcBindDefinition def = (AppProcBindDefinition)pd.getDefinition(AppProcBindDefinition.class);
		
		if(def == null) {
			
			def = new AppProcBindDefinition();
			
			JbpmContext ctx = getBpmContext().createJbpmContext();
			
			try {
				getBpmContext().saveProcessEntity(def);
				
				pd = ctx.getGraphSession().getProcessDefinition(getProcessDefinitionId());
				pd.addDefinition(def);
				
			} finally {
				getBpmContext().closeAndCommit(ctx);
			}
			
			ctx = getBpmContext().createJbpmContext();
			
			try {
				pd = ctx.getGraphSession().loadProcessDefinition(getProcessDefinitionId());
				def = (AppProcBindDefinition)pd.getDefinition(AppProcBindDefinition.class);
				
			} finally {
				getBpmContext().closeAndCommit(ctx);
			}
		}
		
		Integer appId = new Integer(context.toString());
		def.updateRolesCanStartProcess(appId, roles);
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public CaseIdentifier getCaseIdentifier() {
		return caseIdentifier;
	}

	public void setCaseIdentifier(CaseIdentifier caseIdentifier) {
		this.caseIdentifier = caseIdentifier;
	}
	
	public CaseManager getCaseManager() {
		return caseManager;
	}

	public void setCaseManager(CaseManager caseManager) {
		this.caseManager = caseManager;
	}
	
	@Override
	public String getProcessName(Locale locale) {
	
		if (locale == null) {
			return null;
		}
		
		return getCaseManager().getProcessName(getProcessDefinitionId(), locale);
	}
}