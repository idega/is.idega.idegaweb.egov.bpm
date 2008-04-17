package is.idega.idegaweb.egov.bpm.cases.form;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.form.process.XFormsView;
import com.idega.block.process.data.CaseStatus;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewToTask;
import com.idega.jbpm.def.ViewToTaskType;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.VariablesHandler;
import com.idega.jbpm.exe.ViewManager;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $
 *
 * Last modified: $Date: 2008/04/17 23:57:39 $ by $Author: civilis $
 */
public class CasesBPMViewManager implements ViewManager {
	
	public static final String IDENTIFIER_PREFIX = "P";

	private DocumentManagerFactory documentManagerFactory;
	private ViewToTask viewToTaskBinder;
	private VariablesHandler variablesHandler;
	private BPMDAO bpmBindsDAO;
	private CasesBPMDAO casesBPMDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private BPMFactory bpmFactory;
	
	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public View loadInitView(long processDefinitionId, int initiatorId, FacesContext context) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(processDefinitionId);

//			TODO: if not bound, then create default case category, case type and bind to it
			CaseTypesProcDefBind bind = getBpmBindsDAO().find(CaseTypesProcDefBind.class, pd.getName());
			
			ProcessInstance pi = new ProcessInstance(pd);
			TaskInstance taskInstance = pi.getTaskMgmtInstance().createStartTaskInstance();
			
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			View view = getBpmFactory().takeView(taskInstance.getId(), true, preferred);
			
			Object[] identifiers = generateNewCaseIdentifier();
			Integer identifierNumber = (Integer)identifiers[0];
			String identifier = (String)identifiers[1];
			
//			move this to protected method setupInitView(view:View):void
			Map<String, String> parameters = new HashMap<String, String>(6);
			
			parameters.put(ProcessConstants.START_PROCESS, ProcessConstants.START_PROCESS);
			parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstance.getId()));
			parameters.put(CasesBPMProcessConstants.userIdActionVariableName, String.valueOf(initiatorId));
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
			
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	class CaseIdentifier {
		
		IWTimestamp time;
		Integer number;
		
		String generate() {
			
			String nr = String.valueOf(++number);
			
			while(nr.length() < 4)
				nr = "0"+nr;
			
			return new StringBuffer(IDENTIFIER_PREFIX)
			.append(CoreConstants.MINUS)
			.append(time.getYear())
			.append(CoreConstants.MINUS)
			.append(time.getMonth() < 10 ? "0"+time.getMonth() : time.getMonth())
			.append(CoreConstants.MINUS)
			.append(time.getDay() < 10 ? "0"+time.getDay() : time.getDay())
			.append(CoreConstants.MINUS)
			.append(nr)
			.toString();
		}
	}
	
	private CaseIdentifier lastCaseIdentifierNumber;
	
	protected synchronized Object[] generateNewCaseIdentifier() {
		
		IWTimestamp currentTime = new IWTimestamp();
		
		if(lastCaseIdentifierNumber == null || !currentTime.equals(lastCaseIdentifierNumber.time)) {

			lastCaseIdentifierNumber = new CaseIdentifier();
			
			CaseProcInstBind b = getCasesBPMDAO().getCaseProcInstBindLatestByDateQN(new Date());
			
			if(b != null && b.getDateCreated() != null && b.getCaseIdentierID() != null) {
				
				lastCaseIdentifierNumber.time = new IWTimestamp(b.getDateCreated());
				lastCaseIdentifierNumber.number = b.getCaseIdentierID();
			} else {
			
				lastCaseIdentifierNumber.time = currentTime;
				lastCaseIdentifierNumber.number = 0;
			}
		}
		
		String generated = lastCaseIdentifierNumber.generate();
		
		return new Object[] {lastCaseIdentifierNumber.number, generated};
	}
	
	public View loadTaskInstanceView(long taskInstanceId, FacesContext context) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			
			View view;
			
			if(taskInstance.hasEnded()) {
				
				view = getBpmFactory().getViewByTaskInstance(taskInstanceId, false, preferred);
				
			} else {
				
				view = getBpmFactory().takeView(taskInstanceId, true, preferred);
			}
			
			Map<String, String> parameters = new HashMap<String, String>(1);
			parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstance.getId()));
			view.populateParameters(parameters);
			view.populateVariables(getVariablesHandler().populateVariables(taskInstance.getId()));
			
			return view;
		
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public ViewToTask getViewToTaskBinder() {
		return viewToTaskBinder;
	}

	@Autowired
	@ViewToTaskType("xforms")
	public void setViewToTaskBinder(ViewToTask viewToTaskBinder) {
		this.viewToTaskBinder = viewToTaskBinder;
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
	
	protected GroupBusiness getGroupBusiness(IWApplicationContext iwac) {
		try {
			return (GroupBusiness) IBOLookup.getServiceInstance(iwac, GroupBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected CaseStatus getInitCaseStatus(IWApplicationContext iwac) {
		
		try {
			return getCasesBusiness(iwac).getCaseStatusWaiting();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
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

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}