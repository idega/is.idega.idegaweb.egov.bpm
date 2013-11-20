package com.idega.idegaweb.egov.bpm.business.impl;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.egov.bpm.business.BPMManipulator;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.user.dao.UserDAO;
import com.idega.user.data.bean.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Service(BPMManipulatorImpl.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@RemoteProxy(creator = SpringCreator.class, creatorParams = {
	@Param(name = "beanName", value = BPMManipulatorImpl.BEAN_NAME),
	@Param(name = "javascript", value = BPMManipulatorImpl.DWR_OBJECT)
}, name = BPMManipulatorImpl.DWR_OBJECT)
public class BPMManipulatorImpl extends DefaultSpringBean implements BPMManipulator {

	static final String BEAN_NAME = "bpmManipulator",
						DWR_OBJECT = "BPMManipulator";

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private CasesBPMDAO casesDAO;

	@Autowired
	private BPMContext bpmContext;

	@Override
	@RemoteMethod
	public boolean doReSubmitProcess(Long piId) {
		if (piId == null) {
			getLogger().warning("Proc. inst. ID is not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO.getCaseProcInstBindByProcessInstanceId(piId);
		return doReSubmit(bind);
	}

	@Override
	@RemoteMethod
	public boolean doReSubmitCase(Integer caseId) {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO.getCaseProcInstBindByCaseId(caseId);
		return doReSubmit(bind);
	}

	private boolean doReSubmit(CaseProcInstBind bind) {
		if (bind == null) {
			getLogger().warning("Case and proc. inst. bind is not provided");
			return false;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || !iwc.isLoggedOn() || !iwc.isSuperAdmin()) {
			getLogger().warning("You do not have rights to re-submit process");
			return false;
		}

		if (!getApplication().getSettings().getBoolean("bpm.allow_resubmit_proc", Boolean.FALSE)) {
			getLogger().warning("It is forbidden to re-submit process");
			return false;
		}

		try {
			Long piId = bind == null ? null : bind.getProcInstId();
			Integer caseId = bind == null ? null : bind.getCaseId();
			if (bind == null || caseId == null) {
				getLogger().warning("Unable to find case and proc. inst. bind for proc. inst. ID " + piId);
				return false;
			}

			ProcessInstanceW piW = bpmFactory.getProcessInstanceW(piId);
			TaskInstanceW startTiW = piW.getStartTaskInstance();
			if (startTiW == null || !startTiW.isSubmitted()) {
				getLogger().warning("Unable to find start task instance for proc. ins. ID " + piId + " or it (" + startTiW + ") is not submitted");
				return false;
			}

			Long newProcInstId = getIdOfNewProcess(piW, startTiW, caseId);
			if (newProcInstId == null) {
				getLogger().warning("Failed to start new proc. inst. for old proc. inst.: " + piId);
				return false;
			}

			long startTaskInstId = startTiW.getTaskInstanceId().longValue();
			List<TaskInstanceW> allSubmittedTasks = piW.getSubmittedTaskInstances();
			if (!ListUtil.isEmpty(allSubmittedTasks)) {
				ProcessInstanceW newPiw = bpmFactory.getProcessInstanceW(newProcInstId);
				for (TaskInstanceW submittedTask: allSubmittedTasks) {
					if (submittedTask.getTaskInstanceId().longValue() != startTaskInstId) {
						if (!doSubmitTask(piW, submittedTask, newPiw)) {
							getLogger().warning("Unable to re-submit task instance " + submittedTask + " for proc. inst. " + piId);
							return false;
						}
					}
				}
			}

			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Failed to re-submit case/process for bind " + bind, e);
		}

		return false;
	}

	private boolean doSubmitTask(final ProcessInstanceW oldPiW, final TaskInstanceW oldTiW, final ProcessInstanceW newPiW) {
		return bpmContext.execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				try {
					org.jbpm.taskmgmt.exe.TaskInstance oldTi = context.getTaskInstance(oldTiW.getTaskInstanceId());
					org.jbpm.taskmgmt.def.Task task = oldTi.getTask();

					org.jbpm.graph.exe.Token token = oldTi.getToken();
					Map<String, Object> variables = oldTiW.getVariables(token);
					List<BinaryVariable> attachments = oldTiW.getAttachments();

					Object mainProcessInstanceId = variables.get("mainProcessInstanceId");
					if (mainProcessInstanceId instanceof Long) {
						variables.put("mainProcessInstanceId", newPiW.getProcessInstanceId());
					}
					variables.remove("files_attachments");

					TaskInstanceW submittedTaskInstW = newPiW.getSubmittedTaskInstance(task.getName(), variables);
					if (submittedTaskInstW == null) {
						return false;
					}

					return doUpdateTaskInstance(context, oldTi, submittedTaskInstW, attachments);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error submitting task instance for old proc. inst.: " + oldPiW + ", new proc. inst.: " +
							newPiW + ", old task instance: " + oldTiW, e);
				}
				return false;
			}
		});
	}

	@Transactional(readOnly = false)
	private boolean doUpdateTaskInstance(
			JbpmContext context,
			org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst,
			TaskInstanceW newTaskInstW,
			List<BinaryVariable> attachments
	) {
		try {
			org.jbpm.taskmgmt.exe.TaskInstance newTi = context.getTaskInstance(newTaskInstW.getTaskInstanceId());
			newTi.setCreate(oldTaskInst.getCreate());
			newTi.setEnd(oldTaskInst.getEnd());

			String actorId = oldTaskInst.getActorId();
			if (!StringUtil.isEmpty(actorId)) {
				newTi.setActorId(actorId);
			}

			if (!ListUtil.isEmpty(attachments)) {
				for (BinaryVariable attachment: attachments) {
					com.idega.block.process.variables.Variable variable = attachment.getVariable();
					String fileName = attachment.getFileName();
					String description = attachment.getDescription();
					String identifier = attachment.getIdentifier();
					BinaryVariable binVar = newTaskInstW.addAttachment(
							variable,
							fileName,
							description,
							identifier
					);
					if (binVar == null) {
						getLogger().warning("Failed to create new bin. variable (variable: " + variable + ", file name: " + fileName +
								", description: " +	description + ", identifier: " + identifier + ") for task inst " + newTaskInstW);
					} else {
						getLogger().info("Created new bin. variable " + binVar + " for task inst. " + newTaskInstW);
					}
				}
			}

			context.getSession().save(newTi);
			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error updating task instance " + newTaskInstW + " by old one: " + oldTaskInst.getId(), e);
		}
		return false;
	}

	private Long getIdOfNewProcess(final ProcessInstanceW piW, final TaskInstanceW startTiW, final Integer caseId) {
		return bpmContext.execute(new JbpmCallback<Long>() {

			@Override
			public Long doInJbpm(JbpmContext context) throws JbpmException {
				try {
					List<BinaryVariable> attachments = startTiW.getAttachments();

					String procDefName = piW.getProcessDefinitionW(context).getProcessDefinition().getName();

					String actoriId = startTiW.getTaskInstance().getActorId();
					User creator = getActor(actoriId);
					Integer processCreatorId = creator == null ? null : creator.getId();

					CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
					Case theCase = caseBusiness.getCase(caseId);

					View initView = bpmFactory.getProcessManager(procDefName)
												.getProcessDefinition(procDefName)
												.loadInitView(processCreatorId, theCase == null ? null : theCase.getCaseIdentifier());

					Map<String, String> parameters = initView.resolveParameters();
					Long processDefinitionId = new Long(parameters.get(ProcessConstants.PROCESS_DEFINITION_ID));
					String viewId = parameters.get(ProcessConstants.VIEW_ID);
					String viewType = parameters.get(ProcessConstants.VIEW_TYPE);

					ViewSubmission viewSubmission = bpmFactory.getViewSubmission();

					viewSubmission.setProcessDefinitionId(processDefinitionId);
					viewSubmission.setViewId(viewId);
					viewSubmission.setViewType(viewType);

					parameters.put(com.idega.block.process.business.ProcessConstants.CASE_ID, String.valueOf(caseId));

					viewSubmission.populateParameters(parameters);

					org.jbpm.taskmgmt.exe.TaskInstance ti = context.getTaskInstance(startTiW.getTaskInstanceId());
					org.jbpm.graph.exe.Token token = ti.getToken();
					Map<String, Object> variables = startTiW.getVariables(token);
					if (variables.containsKey("mainProcessInstanceId")) {
						variables.remove("mainProcessInstanceId");
					}
					variables.remove("files_attachments");	//	Will be re-added

					viewSubmission.populateVariables(variables);
					viewSubmission.populateParameters(parameters);

					ProcessDefinitionW pdw = bpmFactory.getProcessManager(processDefinitionId).getProcessDefinition(processDefinitionId);
					Long piId = pdw.startProcess(viewSubmission);
					if (piId != null) {
						ProcessInstanceW newPiW = bpmFactory.getProcessInstanceW(context, piId);
						if (doUpdateTaskInstance(context, ti, newPiW.getStartTaskInstance(), attachments)) {
							return piId;
						}
					}

					return null;
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Failed to create new proc. inst. for old proc. inst.: " + piW.getProcessInstanceId(), e);
				}
				return null;
			}
		});
	}

	private User getActor(String id) {
		if (StringUtil.isEmpty(id)) {
			return null;
		}

		try {
			UserDAO userDAO = ELUtil.getInstance().getBean(UserDAO.class);
			if (StringHandler.isNumeric(id)) {
				return userDAO.getUser(Integer.valueOf(id));
			}

			return userDAO.getUser(id);
		} catch (Exception e) {
			getLogger().warning("Unable to find user by ID: " + id);
		}
		return null;
	}

}