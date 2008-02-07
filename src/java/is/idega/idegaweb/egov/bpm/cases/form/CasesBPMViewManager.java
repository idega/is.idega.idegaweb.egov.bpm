package is.idega.idegaweb.egov.bpm.cases.form;

import is.idega.idegaweb.egov.bpm.cases.CasesJbpmProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.block.form.process.XFormsView;
import com.idega.block.process.data.CaseStatus;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CasesBPMBind;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewCreator;
import com.idega.jbpm.def.ViewToTask;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.VariablesHandler;
import com.idega.jbpm.exe.ViewManager;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/02/07 13:57:04 $ by $Author: civilis $
 */
public class CasesBPMViewManager implements ViewManager {

	private DocumentManagerFactory documentManagerFactory;
	private ViewToTask viewToTaskBinder;
	private VariablesHandler variablesHandler;
	private ViewCreator viewCreator;
	private BpmBindsDAO bpmBindsDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private BPMFactory bpmFactory;
	
	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public View loadInitView(FacesContext context, Long processDefinitionId, int initiatorId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
//			TODO: make this generic bind
			CasesBPMBind bind = getBpmBindsDAO().find(CasesBPMBind.class, processDefinitionId);
			
			//String initTaskName = bind.getInitTaskName();
			
//			if(initTaskName == null || CoreConstants.EMPTY.equals(initTaskName))
//				throw new NullPointerException("Init task name not found on CasesJbpmBind.");
			
			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(processDefinitionId);
			
			System.out.println("name: "+pd.getName());
			
//			Task initTask = pd.getTaskMgmtDefinition().getTask(initTaskName);
			TaskMgmtDefinition mgdef = pd.getTaskMgmtDefinition();
			Task initTask = mgdef.getStartTask();
			
			System.out.println("start task: "+initTask);
			
//			View view = getViewToTaskBinder().getView(initTask.getId());
			View view = getBpmFactory().getView(initTask.getId(), true);
			System.out.println("	view identifier: "+view.getViewId());
			
//			move this to protected method setupInitView(view:View):void
			Map<String, String> parameters = new HashMap<String, String>(4);
			
			parameters.put(ProcessConstants.PROCESS_DEFINITION_ID, String.valueOf(processDefinitionId));
			parameters.put(CasesJbpmProcessConstants.userIdActionVariableName, String.valueOf(initiatorId));
			parameters.put(CasesJbpmProcessConstants.caseCategoryIdActionVariableName, String.valueOf(bind.getCasesCategoryId()));
			parameters.put(CasesJbpmProcessConstants.caseTypeActionVariableName, String.valueOf(bind.getCasesTypeId()));
			
			view.populateParameters(parameters);
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
	
	public View loadTaskInstanceView(FacesContext context, Long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			/*
			List<ViewTaskBind> viewTaskBinds = getJbpmBindsDao().getViewTaskBindsByTaskId(taskInstance.getTask().getId());
			
			if(viewTaskBinds.isEmpty())
				throw new RuntimeException("No view bind to task found for task by id: "+taskInstance.getTask().getId());
			
			View view = null;
			
			for (ViewTaskBind viewTaskBind : viewTaskBinds) {
				
//				we prefer xforms view here
				if(viewTaskBind.getViewType().equals(XFormsView.VIEW_TYPE)) {
					ViewFactory viewFactory = getViewCreator().getViewFactory(viewTaskBind.getViewType());
					view = viewFactory.getView(viewTaskBind.getViewIdentifier(), !taskInstance.hasEnded());
				}
			}
			
//			no xforms view found, taking anything
			if(view == null) {
				ViewTaskBind bind = viewTaskBinds.iterator().next();
				ViewFactory viewFactory = getViewCreator().getViewFactory(bind.getViewType());
				view = viewFactory.getView(bind.getViewIdentifier(), !taskInstance.hasEnded());
			}
			*/
			
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			View view = getBpmFactory().getView(taskInstance.getTask().getId(), true, preferred);
			
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
	
	/*
	Remove
	 
	public View loadProcessInstanceView(FacesContext context, Token token) {
		
		try {
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tis = token.getProcessInstance().getTaskMgmtInstance().getUnfinishedTasks(token);
			
			if(tis.size() == 0)
				throw new RuntimeException("No unfinished task instances on token: "+token);
			
			TaskInstance taskInstance = tis.iterator().next();
			
			View view = getViewToTaskBinder().getView(taskInstance.getTask().getId());
			
			Map<String, String> parameters = new HashMap<String, String>(1);
			parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstance.getId()));
			
			view.addParameters(parameters);
			view.populate(getVariablesHandler().populateVariables(taskInstance.getId()));
			
			return view;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
			
		}
	}
	*/
	
	/*
	public org.w3c.dom.Document loadProcessViewForm(FacesContext context, Long processInstanceId, int viewerId) {
		
//		roles - by view type
//		1 - owner 2 - case handler 3 - other
//		checking in this order. 3 - other should be resolved in the same way task view bindings are resolved.
		
		Session session = getHibernateResources().getGlobalSessionFactory().getCurrentSession();
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(session);
		
		try {
			ProcessInstance pi = ctx.getProcessInstance(processInstanceId);
			Long processDefinitionId = pi.getProcessDefinition().getId();
			
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			
			User viewer = getUserBusiness(iwc).getUser(viewerId);
			
			if(viewer == null)
				throw new RuntimeException("userId provided not correct - no User found. userId provided: "+viewerId);
			
			CasesBusiness casesBusiness = getCasesBusiness(iwc);
			
			@SuppressWarnings("unchecked")
			Collection<GeneralCase> genCases = casesBusiness.getCasesByCriteria(null, null, null, null, null, processInstanceId.intValue());
			
			if(genCases.isEmpty())
				throw new RuntimeException("No case found by processInstanceId provided: "+processInstanceId);
			
			GeneralCase genCase = genCases.iterator().next();
			
			User owner = genCase.getOwner();
			String formId;
			
			if(viewer.equals(owner)) {
//				viewer is owner
				ProcessViewByActor processView = ProcessViewByActor.getByViewerType(session, ProcessViewByActor.VIEWER_TYPE_OWNER, XFormsView.VIEW_TYPE, processDefinitionId);
				formId = processView.getViewIdentifier();
				
			} else {
				
				@SuppressWarnings("unchecked")
				Collection<User> handlers = getGroupBusiness(iwc).getUsers(genCase.getHandler());
				
				if(handlers.contains(viewer)) {
//					viewer is status handler
					ProcessViewByActor processView = ProcessViewByActor.getByViewerType(session, ProcessViewByActor.VIEWER_TYPE_CASE_HANDLERS, XFormsView.VIEW_TYPE, processDefinitionId);
					formId = processView.getViewIdentifier();
					
				} else {
//					viewer is someone from another group
//					TODO: implement this
					throw new IllegalStateException("User isn't case owner, and doesn't belong to case handlers group. This situation is not implemented yet");
				}
			}
			
			DocumentManager documentManager = getDocumentManagerFactory().newDocumentManager(context);
			Document form = documentManager.openForm(formId);
			
			getVariablesHandler().populateFromProcess(processInstanceId, form.getSubmissionInstanceElement());
			
			return form.getXformsDocument();
		
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
			
		} finally {
			
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	*/

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

	public ViewCreator getViewCreator() {
		return viewCreator;
	}

	public void setViewCreator(ViewCreator viewCreator) {
		this.viewCreator = viewCreator;
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
}