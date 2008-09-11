package is.idega.idegaweb.egov.bpm.cases.exe;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.manager.CasesBPMCaseManagerImpl;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.util.ArrayList;
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

import com.idega.block.form.process.XFormsView;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.AppProcBindDefinition;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWContext;
import com.idega.presentation.PresentationObject;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/09/11 14:39:35 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPDW")
public class CasesBPMProcessDefinitionW implements ProcessDefinitionW {
	
	private Long processDefinitionId;
	private ProcessDefinition processDefinition;
	
	@Autowired private BPMFactory bpmFactory;
	@Autowired private CasesBPMDAO casesBPMDAO;
	@Autowired private BPMContext bpmContext;
	@Autowired private VariablesHandler variablesHandler;
	@Autowired private CaseIdentifier caseIdentifier;
	
	private static final Logger logger = Logger.getLogger(CasesBPMProcessDefinitionW.class.getName());
	
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
			
			IWMainApplication iwma = iwc.getApplicationContext().getIWMainApplication();
			
			CasesBusiness casesBusiness = getCasesBusiness(iwc);
			
			GeneralCase genCase = casesBusiness.storeGeneralCase(user, caseCatId, caseTypeId, /*attachment pk*/null, "This is simple cases-jbpm-formbuilder integration example.", null, CasesBPMCaseManagerImpl.caseHandlerType, /*isPrivate*/false, getCasesBusiness(iwc).getIWResourceBundleForUser(user, iwc, iwma.getBundle(PresentationObject.CORE_IW_BUNDLE_IDENTIFIER)), false);

			ti.getProcessInstance().setStart(new Date());
			
			Map<String, Object> caseData = new HashMap<String, Object>();
			caseData.put(CasesBPMProcessConstants.caseIdVariableName, genCase.getPrimaryKey().toString());
			caseData.put(CasesBPMProcessConstants.caseTypeNameVariableName, genCase.getCaseType().getName());
			caseData.put(CasesBPMProcessConstants.caseCategoryNameVariableName, genCase.getCaseCategory().getName());
			caseData.put(CasesBPMProcessConstants.caseStatusVariableName, genCase.getCaseStatus().getStatus());
			caseData.put(CasesBPMProcessConstants.caseStatusReceivedVariableName, casesBusiness.getCaseStatusOpen().getStatus());
			caseData.put(CasesBPMProcessConstants.caseStatusInProgressVariableName, casesBusiness.getCaseStatusPending().getStatus());
			caseData.put(CasesBPMProcessConstants.caseStatusClosedVariableName, casesBusiness.getCaseStatusReady().getStatus());
			
			IWTimestamp created = new IWTimestamp(genCase.getCreated());
			caseData.put(CasesBPMProcessConstants.caseCreatedDateVariableName, created.getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
			
			getVariablesHandler().submitVariables(caseData, startTaskInstanceId, false);
			
			CaseProcInstBind bind = new CaseProcInstBind();
			bind.setCaseId(new Integer(genCase.getPrimaryKey().toString()));
			bind.setProcInstId(ti.getProcessInstance().getId());
			bind.setCaseIdentierID(identifierNumber);
			bind.setDateCreated(created.getDate());
			getCasesBPMDAO().persist(bind);
			
			submitVariablesAndProceedProcess(ti, view.resolveVariables(), true);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			getBpmContext().closeAndCommit(ctx);
		}
	}
	
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
	
	protected void submitVariablesAndProceedProcess(TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		
		getVariablesHandler().submitVariables(variables, ti.getId(), true);
		
		if(proceed) {
		
			String actionTaken = (String)ti.getVariable(CasesBPMProcessConstants.actionTakenVariableName);
	    	
	    	if(actionTaken != null && !CoreConstants.EMPTY.equals(actionTaken) && false)
	    		ti.end(actionTaken);
	    	else
	    		ti.end();
		} else {
			ti.setEnd(new Date());
		}
    	
//		TODO: perhaps some bpm user, and then later bind it to real user if that's created etc
		Integer usrId = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getIdToUse();
		
		if(usrId != null)
			ti.setActorId(usrId.toString());
	}
	
	public String getStartTaskName() {
		
		List<String> preferred = new ArrayList<String>(1);
		preferred.add(XFormsView.VIEW_TYPE);
		
		Long taskId = getProcessDefinition().getTaskMgmtDefinition().getStartTask().getId();
		
		View view = getBpmFactory().getViewByTask(taskId, false, preferred);
		
		return view.getDisplayName(new Locale("is","IS"));
	}
	
	public Long getProcessDefinitionId() {
		return processDefinitionId;
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

	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public ProcessDefinition getProcessDefinition() {
		
		if(processDefinition == null && getProcessDefinitionId() != null) {
		
			JbpmContext ctx = getBpmContext().createJbpmContext();
			
			try {
				processDefinition = ctx.getGraphSession().getProcessDefinition(getProcessDefinitionId());
				
			} finally {
				getBpmContext().closeAndCommit(ctx);
			}
		}
		
		return processDefinition;
	}

	public CaseIdentifier getCaseIdentifier() {
		return caseIdentifier;
	}

	public void setCaseIdentifier(CaseIdentifier caseIdentifier) {
		this.caseIdentifier = caseIdentifier;
	}
}