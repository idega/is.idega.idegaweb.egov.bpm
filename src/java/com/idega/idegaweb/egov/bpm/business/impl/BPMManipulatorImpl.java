package com.idega.idegaweb.egov.bpm.business.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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
import com.idega.block.process.data.CaseBMPBean;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.accesscontrol.dao.UserLoginDAO;
import com.idega.core.accesscontrol.data.bean.UserLogin;
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
		@Param(name = "javascript", value = BPMManipulatorImpl.DWR_OBJECT) }, name = BPMManipulatorImpl.DWR_OBJECT)
public class BPMManipulatorImpl extends DefaultSpringBean implements
		BPMManipulator {

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
	public boolean doReSubmitProcess(Long piId, boolean onlyStart,
			boolean submitRepeatedTasks) {
		if (piId == null) {
			getLogger().warning("Proc. inst. ID is not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO
				.getCaseProcInstBindByProcessInstanceId(piId);
		return doReSubmit(bind, onlyStart, submitRepeatedTasks);
	}

	@Override
	@RemoteMethod
	public boolean doReSubmitCase(Integer caseId, boolean onlyStart,
			boolean submitRepeatedTasks) {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO.getCaseProcInstBindByCaseId(caseId);
		return doReSubmit(bind, onlyStart, submitRepeatedTasks);
	}

	private boolean doReSubmit(final CaseProcInstBind bind,
			final boolean onlyStart, final boolean submitRepeatedTasks) {
		if (bind == null) {
			getLogger().warning("Case and proc. inst. bind is not provided");
			return false;
		}

		final IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || !iwc.isLoggedOn() || !iwc.isSuperAdmin()) {
			getLogger().warning("You do not have rights to re-submit process");
			return false;
		}

		if (!getApplication().getSettings().getBoolean(
				"bpm.allow_resubmit_proc", Boolean.FALSE)) {
			getLogger().warning("It is forbidden to re-submit process");
			return false;
		}

		return bpmContext.execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				UserLogin login = null;
				boolean hasLogin = false;
				LoginBusinessBean loginBusiness = LoginBusinessBean
						.getLoginBusinessBean(iwc);
				try {
					Long piId = bind == null ? null : bind.getProcInstId();
					Integer caseId = bind == null ? null : bind.getCaseId();
					if (bind == null || caseId == null) {
						getLogger().warning(
								"Unable to find case and proc. inst. bind for proc. inst. ID "
										+ piId);
						return false;
					}

					CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
					Case theCase = caseBusiness.getCase(caseId);
					com.idega.user.data.User owner = theCase.getOwner();
					if (owner == null) {
						owner = theCase.getCreator();
					}
					if (owner != null) {
						User author = getActor(owner.getPersonalID());
						hasLogin = hasLogin(author);
						if (!hasLogin) {
							login = doCreateUserLogin(author);
							if (login == null || login.getId() == null) {
								getLogger().warning(
										"Failed to create temp. login for "
												+ author);
								return false;
							}
						}
						loginBusiness.logInAsAnotherUser(iwc, author);
					}

					ProcessInstanceW piW = bpmFactory.getProcessInstanceW(piId);
					TaskInstanceW startTiW = piW.getStartTaskInstance();
					if (startTiW == null || !startTiW.isSubmitted()) {
						getLogger().warning(
								"Unable to find start task instance for proc. ins. ID "
										+ piId + " or it (" + startTiW
										+ ") is not submitted");
						return false;
					}

					Long newProcInstId = getIdOfNewProcess(context, piW,
							startTiW, caseId);
					if (newProcInstId == null) {
						getLogger().warning(
								"Failed to start new proc. inst. for old proc. inst.: "
										+ piId);
						return false;
					}

					List<TaskInstanceW> allSubmittedTasks = onlyStart ? null
							: piW.getSubmittedTaskInstances();
					if (!ListUtil.isEmpty(allSubmittedTasks)) {
						Collections.sort(allSubmittedTasks,
								new Comparator<TaskInstanceW>() {
									@Override
									public int compare(TaskInstanceW t1,
											TaskInstanceW t2) {
										Date d1 = t1.getTaskInstance().getEnd();
										Date d2 = t1.getTaskInstance().getEnd();
										return d1.compareTo(d2);
									}
								});

						long startTaskInstId = startTiW.getTaskInstanceId()
								.longValue();
						ProcessInstanceW newPiw = bpmFactory
								.getProcessInstanceW(newProcInstId);
						Map<String, Boolean> submitted = new HashMap<String, Boolean>();
						for (TaskInstanceW submittedTask : allSubmittedTasks) {
							String name = submittedTask.getTaskInstance()
									.getName();
							if (!submitRepeatedTasks
									&& submitted.containsKey(name)) {
								getLogger()
										.info("Task instance by name "
												+ name
												+ " was already submitted, not submitting repeating tasks");
								continue;
							}

							if (submittedTask.getTaskInstanceId().longValue() != startTaskInstId) {
								if (doSubmitTask(context, piW, submittedTask,
										newPiw)) {
									submitted.put(name, Boolean.TRUE);
								} else {
									getLogger().warning(
											"Unable to re-submit task instance "
													+ submittedTask
													+ " for proc. inst. "
													+ piId);
									return false;
								}
							}
						}
					}

					if (onlyStart) {
						theCase.setStatus(CaseBMPBean.CASE_STATUS_OPEN_KEY);
						theCase.store();
					}

					return true;
				} catch (Exception e) {
					getLogger()
							.log(Level.WARNING,
									"Failed to re-submit case/process for bind "
											+ bind, e);
				} finally {
					try {
						loginBusiness.logInAsAnotherUser(iwc, iwc
								.getAccessController().getAdministratorUser());
					} catch (Exception e) {
						getLogger().log(Level.WARNING,
								"Error logging in as super admin", e);
					}
					doDeleteLogin(context.getSession(), login);
				}
				return false;
			}
		});
	}

	private boolean doSubmitTask(
			JbpmContext context,
			ProcessInstanceW oldPiW,
			TaskInstanceW oldTiW,
			ProcessInstanceW newPiW
	) {
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
			getLogger().log(Level.WARNING, "Error submitting task instance for old proc. inst.: " + oldPiW + ", new proc. inst.: " + newPiW +
					", old task instance: " + oldTiW, e);
		}
		return false;
	}

	@Transactional(readOnly = false)
	private boolean doUpdateTaskInstance(
			JbpmContext context,
			org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst,
			TaskInstanceW newTaskInstW, List<BinaryVariable> attachments
	) {
		try {
			org.jbpm.taskmgmt.exe.TaskInstance newTi = context.getTaskInstance(newTaskInstW.getTaskInstanceId());
			newTi.setCreate(oldTaskInst.getCreate());
			newTi.setEnd(oldTaskInst.getEnd());
			newTi.setActorId(oldTaskInst.getActorId());

			if (!ListUtil.isEmpty(attachments)) {
				for (BinaryVariable attachment: attachments) {
					com.idega.block.process.variables.Variable variable = attachment.getVariable();
					String fileName = attachment.getFileName();
					String description = attachment.getDescription();
					String identifier = attachment.getIdentifier();
					BinaryVariable binVar = newTaskInstW.addAttachment(variable, fileName, description, identifier);
					if (binVar == null) {
						getLogger().warning(
								"Failed to create new bin. variable (variable: "
										+ variable + ", file name: " + fileName
										+ ", description: " + description
										+ ", identifier: " + identifier
										+ ") for task inst " + newTaskInstW);
					} else {
						getLogger().info(
								"Created new bin. variable " + binVar
										+ " for task inst. " + newTaskInstW);
					}
				}
			}

			context.getSession().save(newTi);
			return true;
		} catch (Exception e) {
			getLogger().log(
					Level.WARNING,
					"Error updating task instance " + newTaskInstW
							+ " by old one: " + oldTaskInst.getId(), e);
		}
		return false;
	}

	private Long getIdOfNewProcess(
			JbpmContext context,
			ProcessInstanceW piW,
			TaskInstanceW startTiW,
			final Integer caseId
	) {
		try {
			List<BinaryVariable> attachments = startTiW.getAttachments();

			String procDefName = piW.getProcessDefinitionW(context).getProcessDefinition().getName();

			String actoriId = startTiW.getTaskInstance().getActorId();
			User creator = getActor(actoriId);
			Integer processCreatorId = creator == null ? null : creator.getId();

			CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
			Case theCase = caseBusiness.getCase(caseId);

			View initView = bpmFactory
					.getProcessManager(procDefName)
					.getProcessDefinition(procDefName)
					.loadInitView(processCreatorId, theCase == null ? null : theCase.getCaseIdentifier());

			Map<String, String> parameters = initView.resolveParameters();
			Long processDefinitionId = new Long(
					parameters.get(ProcessConstants.PROCESS_DEFINITION_ID));
			String viewId = parameters.get(ProcessConstants.VIEW_ID);
			String viewType = parameters.get(ProcessConstants.VIEW_TYPE);

			ViewSubmission viewSubmission = bpmFactory.getViewSubmission();

			viewSubmission.setProcessDefinitionId(processDefinitionId);
			viewSubmission.setViewId(viewId);
			viewSubmission.setViewType(viewType);

			parameters.put(
					com.idega.block.process.business.ProcessConstants.CASE_ID,
					String.valueOf(caseId));

			viewSubmission.populateParameters(parameters);

			org.jbpm.taskmgmt.exe.TaskInstance ti = context
					.getTaskInstance(startTiW.getTaskInstanceId());
			org.jbpm.graph.exe.Token token = ti.getToken();
			Map<String, Object> variables = startTiW.getVariables(token);
			if (variables.containsKey("mainProcessInstanceId")) {
				variables.remove("mainProcessInstanceId");
			}
			variables.remove("files_attachments"); // Will be re-added

			viewSubmission.populateVariables(variables);
			viewSubmission.populateParameters(parameters);

			ProcessDefinitionW pdw = bpmFactory.getProcessManager(
					processDefinitionId).getProcessDefinition(
					processDefinitionId);
			Long piId = pdw.startProcess(viewSubmission);
			if (piId != null) {
				ProcessInstanceW newPiW = bpmFactory.getProcessInstanceW(
						context, piId);
				if (doUpdateTaskInstance(context, ti,
						newPiW.getStartTaskInstance(), attachments)) {
					return piId;
				}
			}

			return null;
		} catch (Exception e) {
			getLogger().log(
					Level.WARNING,
					"Failed to create new proc. inst. for old proc. inst.: "
							+ piW.getProcessInstanceId(), e);
		}
		return null;
	}

	private void doDeleteLogin(org.hibernate.Session session, UserLogin login) {
		if (login == null) {
			return;
		}

		try {
			session.refresh(login);
			session.delete(login);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error deleting login " + login, e);
		}
	}

	private UserLogin doCreateUserLogin(User user) {
		try {
			UserLoginDAO loginDAO = ELUtil.getInstance().getBean(
					UserLoginDAO.class);
			com.idega.user.data.User legacyUser = getLegacyUser(user);
			List<String> logins = LoginDBHandler
					.getPossibleGeneratedUserLogins(legacyUser);
			if (ListUtil.isEmpty(logins)) {
				return null;
			}

			String password = LoginDBHandler
					.getGeneratedPasswordForUser(legacyUser);
			if (StringUtil.isEmpty(password)) {
				return null;
			}
			return loginDAO.createLogin(user, logins.get(0), password, true,
					true, true, 1, true);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error creating login for " + user,
					e);
		}
		return null;
	}

	private boolean hasLogin(User user) {
		try {
			UserLoginDAO loginDAO = ELUtil.getInstance().getBean(
					UserLoginDAO.class);
			UserLogin login = loginDAO.findLoginForUser(user);
			return login != null;
		} catch (Exception e) {
			getLogger().log(Level.WARNING,
					"Error while checking if " + user + " has login", e);
		}
		return false;
	}

	private User getActor(String id) {
		if (StringUtil.isEmpty(id)) {
			return null;
		}

		try {
			UserDAO userDAO = ELUtil.getInstance().getBean(UserDAO.class);
			if (StringHandler.isNumeric(id)) {
				if (id.length() == 10) {
					return userDAO.getUser(id);
				} else {
					return userDAO.getUser(Integer.valueOf(id));
				}
			}

			return userDAO.getUser(id);
		} catch (Exception e) {
			getLogger().warning("Unable to find user by ID: " + id);
		}
		return null;
	}

}