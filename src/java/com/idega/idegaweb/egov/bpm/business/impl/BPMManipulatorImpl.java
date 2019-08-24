package com.idega.idegaweb.egov.bpm.business.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseBMPBean;
import com.idega.block.process.data.CaseHome;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.xformsview.converters.DataConvertersFactory;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.accesscontrol.dao.UserLoginDAO;
import com.idega.core.accesscontrol.data.bean.UserLogin;
import com.idega.core.business.DefaultSpringBean;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.egov.bpm.business.BPMManipulator;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.VariableInstanceType;
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
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
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

	@Autowired
	private DataConvertersFactory convertersFactory;

	private DataConvertersFactory getConvertersFactory() {
		if (convertersFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return convertersFactory;
	}

	@Override
	@RemoteMethod
	public boolean doReSubmitProcess(Long piId, boolean onlyStart, boolean submitRepeatedTasks) {
		if (piId == null) {
			getLogger().warning("Proc. inst. ID is not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO.getCaseProcInstBindByProcessInstanceId(piId);
		return doReSubmit(bind, onlyStart, submitRepeatedTasks);
	}

	@Override
	@RemoteMethod
	public boolean doReSubmitCase(Integer caseId, boolean onlyStart, boolean submitRepeatedTasks) {
		return doReSubmitCaseWithVariables(caseId, onlyStart, submitRepeatedTasks, null);
	}

	@Override
	@RemoteMethod
	public boolean doReSubmitCaseWithVariables(Integer caseId, boolean onlyStart, boolean submitRepeatedTasks, String variablesEncodedBase64) {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO.getCaseProcInstBindByCaseId(caseId);
		if (!doReSubmit(bind, onlyStart, submitRepeatedTasks)) {
			getLogger().warning("Failed to re-submit case with ID " + caseId);
			return false;
		}

		if (StringUtil.isEmpty(variablesEncodedBase64)) {
			return true;
		}

		return doSubmitVariables(caseId, variablesEncodedBase64);
	}

	@Override
	@RemoteMethod
	public boolean doReSubmitCaseByIdentifierWithVariables(String caseIdentifier, boolean onlyStart, boolean submitRepeatedTasks, String variablesEncodedBase64) {
		if (StringUtil.isEmpty(caseIdentifier)) {
			getLogger().warning("Case's identifier is not provided");
			return false;
		}

		try {
			CaseHome caseHome = (CaseHome) IDOLookup.getHome(Case.class);
			Collection<Case> cases = caseHome.findCasesByCaseIdentifier(caseIdentifier);
			if (ListUtil.isEmpty(cases)) {
				getLogger().warning("Case was not found with identifier " + caseIdentifier);
				return false;
			}

			Integer caseId = (Integer) cases.iterator().next().getPrimaryKey();

			if (cases.size() > 1) {
				getLogger().info("Found multiple cases (" + cases + ") for identifier " + caseIdentifier + ", using " + caseId);
			}

			return doReSubmitCaseWithVariables(caseId, onlyStart, submitRepeatedTasks, variablesEncodedBase64);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while re-submitting case with identifier: " + caseIdentifier, e);
		}

		return false;
	}

	@Override
	@RemoteMethod
	public boolean doSubmitVariables(Integer caseId, String variablesEncodedBase64) {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return false;
		}
		if (StringUtil.isEmpty(variablesEncodedBase64)) {
			getLogger().warning("Variables not provided");
			return false;
		}

		CaseProcInstBind bind = casesDAO.getCaseProcInstBindByCaseId(caseId);
		if (bind == null) {
			getLogger().warning("Process instance not found for case: " + caseId);
			return false;
		}

		return doSubmitVariables(caseId, bind.getProcInstId(), variablesEncodedBase64);
	}

	private boolean doReSubmit(final CaseProcInstBind bind, final boolean onlyStart, final boolean submitRepeatedTasks) {
		if (bind == null) {
			getLogger().warning("Case and proc. inst. bind is not provided");
			return false;
		}

		final IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || !iwc.isLoggedOn() || !iwc.isSuperAdmin()) {
			getLogger().warning("You do not have rights to re-submit process");
			return false;
		}

		if (!getApplication().getSettings().getBoolean("bpm.allow_resubmit_proc", Boolean.FALSE)) {
			getLogger().warning("It is forbidden to re-submit process");
			return false;
		}

		return doReSubmit(iwc.getLoggedInUser(), iwc, bind, onlyStart, submitRepeatedTasks);
	}

	@Override
	public boolean doReSubmit(User user, IWContext iwc, final CaseProcInstBind bind, final boolean onlyStart, final boolean submitRepeatedTasks) {
		try {
			if (user == null || (iwc != null && !iwc.isSuperAdmin()) || iwc.getAccessController().getAdministratorUser().getId().intValue() != user.getId().intValue()) {
				getLogger().warning("Wrong user: " + user);
				return false;
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error resolving access rights", e);
			return false;
		}

		return bpmContext.execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				UserLogin login = null;
				boolean hasLogin = false;
				LoginBusinessBean loginBusiness = LoginBusinessBean.getLoginBusinessBean(iwc);
				try {
					Long piId = bind == null ? null : bind.getProcInstId();
					Integer caseId = bind == null ? null : bind.getCaseId();
					if (bind == null || caseId == null) {
						getLogger().warning("Unable to find case and proc. inst. bind for proc. inst. ID " + piId);
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
						author = author == null ? getUser(owner) : author;
						hasLogin = hasLogin(author);
						if (!hasLogin) {
							login = doCreateUserLogin(author);
							if (login == null || login.getId() == null) {
								getLogger().warning("Failed to create temp. login for " + author);
								return false;
							}
						}
						loginBusiness.logInAsAnotherUser(iwc, author);
					}

					ProcessInstanceW piW = bpmFactory.getProcessInstanceW(piId);
					TaskInstanceW startTiW = piW.getStartTaskInstance(iwc);
					if (startTiW == null || !startTiW.isSubmitted()) {
						getLogger().warning("Unable to find start task instance for proc. inst. ID " + piId + " or start task (" + startTiW + ") is not submitted");
						return false;
					}

					Long newProcInstId = getIdOfNewProcess(iwc, context, piW, startTiW, caseId);
					if (newProcInstId == null) {
						getLogger().warning("Failed to start new proc. inst. for old proc. inst.: " + piId);
						return false;
					}

					List<TaskInstanceW> allSubmittedTasks = onlyStart ? null : piW.getSubmittedTaskInstances(iwc);
					if (!ListUtil.isEmpty(allSubmittedTasks)) {
						Collections.sort(allSubmittedTasks, new Comparator<TaskInstanceW>() {

							@Override
							public int compare(TaskInstanceW t1, TaskInstanceW t2) {
								TaskInstance ti1 = t1.getTaskInstance();
								TaskInstance ti2 = t2.getTaskInstance();

								Date d1 = ti1.getEnd();
								Date d2 = ti2.getEnd();
								return d1.compareTo(d2);
							}
						});

						Long tiId = startTiW.getTaskInstanceId();
						long startTaskInstId = tiId.longValue();
						ProcessInstanceW newPiw = bpmFactory.getProcessInstanceW(newProcInstId);
						Map<String, Boolean> submitted = new HashMap<String, Boolean>();
						for (TaskInstanceW submittedTask : allSubmittedTasks) {
							String name = submittedTask.getTaskInstanceName();
							if (!submitRepeatedTasks && submitted.containsKey(name)) {
								getLogger().info("Task instance by name " + name + " was already submitted, not submitting repeating tasks");
								continue;
							}

							Long submittedTaskInstanceId = submittedTask.getTaskInstanceId();
							if (submittedTaskInstanceId.longValue() != startTaskInstId) {
								if (doSubmitTask(iwc, context, piW, submittedTask, newPiw)) {
									submitted.put(name, Boolean.TRUE);
								} else {
									getLogger().warning("Unable to re-submit task instance " + submittedTask + " for proc. inst. " + piId);
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
					getLogger().log(Level.WARNING, "Failed to re-submit case/process for bind " + bind, e);
				} finally {
					try {
						loginBusiness.logInAsAnotherUser(iwc, iwc.getAccessController().getAdministratorUser());
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error logging in as super admin", e);
					}
					doDeleteLogin(context.getSession(), login);
				}
				return false;
			}
		});
	}

	private boolean doSubmitTask(
			IWContext iwc,
			JbpmContext context,
			ProcessInstanceW oldPiW,
			TaskInstanceW oldTiW,
			ProcessInstanceW newPiW
	) {
		try {
			org.jbpm.taskmgmt.exe.TaskInstance oldTi = context.getTaskInstance(oldTiW.getTaskInstanceId());
			org.jbpm.taskmgmt.def.Task task = oldTi.getTask();

			org.jbpm.graph.exe.Token token = oldTi.getToken();
			Map<String, Object> variables = oldTiW.getVariables(iwc, token);
			List<BinaryVariable> attachments = oldTiW.getAttachments(iwc);

			Object mainProcessInstanceId = variables.get("mainProcessInstanceId");
			if (mainProcessInstanceId instanceof Long) {
				variables.put("mainProcessInstanceId", newPiW.getProcessInstanceId());
			}
			variables.remove("files_attachments");

			TaskInstanceW submittedTaskInstW = newPiW.getSubmittedTaskInstance(iwc, task.getName(), variables);
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
								"Failed to create new binary variable (variable: "
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
			getLogger().log(Level.WARNING, 	"Error updating task instance " + newTaskInstW + " by old one: " + oldTaskInst.getId(), e);
		}
		return false;
	}

	private Long getIdOfNewProcess(
			IWContext iwc,
			JbpmContext context,
			ProcessInstanceW piW,
			TaskInstanceW startTiW,
			final Integer caseId
	) {
		try {
			List<BinaryVariable> attachments = startTiW.getAttachments(iwc);

			String procDefName = piW.getProcessDefinitionW(context).getProcessDefinitionName();

			TaskInstance startTaskInstance = startTiW.getTaskInstance();
			String actoriId = startTaskInstance.getActorId();
			User creator = getActor(actoriId);
			Integer processCreatorId = creator == null ? null : creator.getId();

			CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
			Case theCase = caseBusiness.getCase(caseId);

			View initView = bpmFactory
					.getProcessManager(procDefName)
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
			Map<String, Object> variables = startTiW.getVariables(iwc, token);
			if (variables.containsKey("mainProcessInstanceId")) {
				variables.remove("mainProcessInstanceId");
			}
			variables.remove("files_attachments"); // Will be re-added

			viewSubmission.populateVariables(variables);
			viewSubmission.populateParameters(parameters);

			ProcessDefinitionW pdw = bpmFactory.getProcessManager(processDefinitionId).getProcessDefinition(processDefinitionId);
			Long piId = pdw.startProcess(iwc, viewSubmission);
			if (piId != null) {
				ProcessInstanceW newPiW = bpmFactory.getProcessInstanceW(context, piId);
				if (doUpdateTaskInstance(context, ti, newPiW.getStartTaskInstance(iwc), attachments)) {
					return piId;
				}
			}

			return null;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Failed to create new proc. inst. for old proc. inst.: " + piW.getProcessInstanceId(), e);
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
			if (user == null) {
				return null;
			}

			UserLoginDAO loginDAO = ELUtil.getInstance().getBean(UserLoginDAO.class);
			com.idega.user.data.User legacyUser = getLegacyUser(user);
			List<String> logins = LoginDBHandler.getPossibleGeneratedUserLogins(legacyUser);
			if (ListUtil.isEmpty(logins)) {
				return null;
			}

			String password = LoginDBHandler.getGeneratedPasswordForUser(legacyUser);
			if (StringUtil.isEmpty(password)) {
				return null;
			}
			return loginDAO.createLogin(user, logins.get(0), password, true, true, true, 1, true);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error creating login for " + user, e);
		}
		return null;
	}

	private boolean hasLogin(User user) {
		try {
			if (user == null) {
				return false;
			}

			UserLoginDAO loginDAO = ELUtil.getInstance().getBean(UserLoginDAO.class);
			UserLogin login = loginDAO.findLoginForUser(user);
			return login != null;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while checking if " + user + " has login", e);
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

	private boolean doSubmitVariables(Integer caseId, Long procInstId, String encoded) {
		if (procInstId == null) {
			return false;
		}

		Map<String, String> allVariables = new HashMap<>();
		String decoded = new String(Base64Utils.decodeFromString(encoded));

		ContentWithVariablesValues objLists = getObjectsLists(decoded);
		if (objLists != null) {
			decoded = objLists.content;
			decoded = StringHandler.replace(decoded, ", , ", ", ");
			Map<String, String> objListsData = objLists.variables;
			MapUtil.append(allVariables, objListsData);
		}

		ContentWithVariablesValues variablesData = getVariables(decoded);
		if (variablesData != null) {
			decoded = variablesData.content;
			Map<String, String> otherVariables = variablesData.variables;
			MapUtil.append(allVariables, otherVariables);
		}

		ProcessInstanceW piW = bpmFactory.getProcessInstanceW(procInstId);
		IWContext iwc = CoreUtil.getIWContext();
		TaskInstanceW startTaskInstance = piW.getStartTaskInstance(iwc);
		if (startTaskInstance == null) {
			getLogger().warning("Start task is unknown for proc. inst.: " + procInstId);
			return false;
		}

		Long startTaskInstanceId = startTaskInstance.getTaskInstanceId();
		for (String name: allVariables.keySet()) {
			String value = null;
			Variable variable = null;
			try {
				value = allVariables.get(name);
				VariableDataType dataType = VariableDataType.getVariableType(name);
				variable = new Variable(name, dataType);
				Object variableValue = null;
				try {
					variableValue = getConvertersFactory().createConverter(dataType).convert(value);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error converting '" + value + "' into real value for variable " + name + " for task instance " + startTaskInstanceId, e);
				}
				if (variableValue == null || StringUtil.isEmpty(variableValue.toString())) {
					getLogger().warning("Failed to convert '" + value + "' into real value for variable " + name + " for task instance " + startTaskInstanceId);
					continue;
				}

				getLogger().info("Addind variable (name: " + name + ", type: " + dataType + ", value: " + variableValue + ") for task instance " + startTaskInstanceId);
				startTaskInstance.addVariable(variable, variableValue);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error adding variable '" + name + "' with value '" + value + "' (" + variable + ") for start task instance: " + startTaskInstanceId, e);
				return false;
			}
		}

		String description = allVariables.get(com.idega.block.process.business.ProcessConstants.CASE_DESCRIPTION);
		if (caseId != null && !StringUtil.isEmpty(description)) {
			try {
				CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
				Case theCase = caseBusiness.getCase(caseId);
				theCase.setSubject(description);
				theCase.store();
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error setting description '" + description + "' for case: " + caseId, e);
			}
		}
		if (StringUtil.isEmpty(description)) {
			getLogger().warning("Unable to resolve description for case " + caseId);
		}

		return true;
	}

	private class ContentWithVariablesValues {

		private String content;
		private Map<String, String> variables;

		private ContentWithVariablesValues(String content, Map<String, String> variables) {
			this.content = content;
			this.variables = variables;
		}

	}

	private ContentWithVariablesValues getVariables(String content, String start, String end, List<String> afterEnd) {
		if (StringUtil.isEmpty(content)) {
			return null;
		}

		if (content.indexOf(start) == -1) {
			return null;
		}

		if (content.startsWith(CoreConstants.CURLY_BRACKET_LEFT)) {
			content = content.substring(1);
		}
		if (content.endsWith(CoreConstants.CURLY_BRACKET_RIGHT)) {
			content = content.substring(0, content.length() - 1);
		}

		Map<String, String> data = new HashMap<>();

		int startIndex = 0;
		while (!StringUtil.isEmpty(content) && (startIndex = content.indexOf(start)) != -1) {
			String name = CoreConstants.EMPTY;
			int nameStartIndex = startIndex;
			boolean foundName = false;
			while (name != null && nameStartIndex > -1 && !foundName) {
				if (name.startsWith(CoreConstants.SPACE)) {
					name = name.trim();
					foundName = true;
					continue;
				}
				if (name.startsWith("{")) {
					name = name.substring(1);
					foundName = true;
					continue;
				}

				nameStartIndex--;
				name = content.substring(nameStartIndex < 0 ? 0 : nameStartIndex, startIndex);
			}
			if (StringUtil.isEmpty(name)) {
				continue;
			}

			String tmpEnd = end;
			int endIndex = content.indexOf(tmpEnd);
			if (endIndex == 0) {
				content = content.replaceFirst(end, CoreConstants.EMPTY);
				startIndex = content.indexOf(start);
				endIndex = content.indexOf(tmpEnd);
			}
			int dropEnd = 0;
			if (ListUtil.isEmpty(afterEnd)) {
				dropEnd = tmpEnd.length() - 1;
			} else {
				boolean foundEnd = false;
				int shortestEnd = -1;
				for (Iterator<String> afterEndIter = afterEnd.iterator(); (!foundEnd && afterEndIter.hasNext());) {
					tmpEnd = end.concat(afterEndIter.next());
					int tmpEndIndex = content.indexOf(tmpEnd);
					if (tmpEndIndex == endIndex) {
						foundEnd = true;
					} else if (tmpEndIndex != -1) {
						int valueEndIndex = startIndex + 1;
						String value = CoreConstants.EMPTY;
						boolean foundValue = false;
						while (valueEndIndex < content.length() && !foundValue) {
							valueEndIndex++;
							value = content.substring(startIndex + 1, valueEndIndex);
							foundValue = value.endsWith(tmpEnd);
						}

						if (valueEndIndex > -1 && foundValue) {
							endIndex = valueEndIndex - tmpEnd.length();
							shortestEnd = shortestEnd < 0 ?
								endIndex :
								shortestEnd > endIndex ? endIndex : shortestEnd;
						}
					}
				}
				endIndex = shortestEnd > -1 ? shortestEnd : endIndex;
			}

			endIndex = endIndex < 0 ? content.length() : endIndex;

			if (startIndex < 0 || endIndex < 0 || endIndex < startIndex || (startIndex + 1 > endIndex + dropEnd)) {
				getLogger().warning("Failed to find end index (end symbol: '" + end + "') for variable '" + name + "' in " + content);
				return null;
			}

			String value = content.substring(startIndex + 1, endIndex + dropEnd);
			if (StringUtil.isEmpty(value)) {
				getLogger().warning("Failed to find value for variable '" + name + "' in " + content);
				return null;
			}

			data.put(name, value);

			content = StringHandler.replace(content, name.concat(CoreConstants.EQ).concat(value), CoreConstants.EMPTY);
			if (content.startsWith(end)) {
				content = content.replaceFirst(end, CoreConstants.EMPTY);
			}
		}
		return new ContentWithVariablesValues(content, data);
	}

	private ContentWithVariablesValues getVariables(String content) {
		List<String> afterEnd = new ArrayList<>();
		for (VariableInstanceType type: VariableInstanceType.values()) {
			afterEnd.add(type.getPrefix());
		}

		return getVariables(content, "=", ", ", afterEnd);
	}

	private ContentWithVariablesValues getObjectsLists(String content) {
		return getVariables(content, "=[{", "]}}],", null);
	}

	@Override
	@RemoteMethod
	public boolean doExecuteHandler(Long procInstId, String handlerName, List<AdvancedProperty> params) {
		try {
			IWContext iwc = CoreUtil.getIWContext();
			if (iwc == null || !iwc.isLoggedOn() || !iwc.isSuperAdmin()) {
				getLogger().warning("You do not have rights to execute handler");
				return false;
			}

			if (!getApplication().getSettings().getBoolean("bpm.allow_execute_handler", Boolean.FALSE)) {
				getLogger().warning("It is forbidden to execute handler");
				return false;
			}

			if (procInstId == null) {
				getLogger().warning("Proc. inst. ID not provided");
				return false;
			}
			if (StringUtil.isEmpty(handlerName)) {
				getLogger().warning("Handler's name not provided");
				return false;
			}

			ActionHandler handler = ELUtil.getInstance().getBean(handlerName);
			if (handler == null) {
				getLogger().warning("Handler by name " + handlerName + " not found");
				return false;
			}

			if (!ListUtil.isEmpty(params)) {
				for (AdvancedProperty param: params) {
					String name = null;
					Object value = null;
					try {
						name = param.getName();
						if (StringUtil.isEmpty(name)) {
							continue;
						}
						value = param.getValue();
						if (value == null) {
							continue;
						}

						String type = param.getId();
						Method m = handler.getClass().getMethod(name, StringUtil.isEmpty(type) ? null : Class.forName(type));
						if (m == null) {
							getLogger().warning("Can't find method '" + name + "' at " + handler.getClass().getName() + ". All methods: " + Arrays.asList(handler.getClass().getMethods()));
							continue;
						}

						if (!StringUtil.isEmpty(type)) {
							Object tmp = null;
							if (type.equals(Long.class.getName())) {
								tmp = StringHandler.isNumeric(value.toString()) ? Long.valueOf(value.toString()) : null;

							} else if (type.equals(Integer.class.getName())) {
								tmp = StringHandler.isNumeric(value.toString()) ? Integer.valueOf(value.toString()) : null;

							} else {
								getLogger().warning("Do not know how to convert " + value + " (" + value.getClass().getName() + ") to " + type);
							}

							if (tmp == null) {
								getLogger().warning("Do not know how to convert " + value + " (" + value.getClass().getName() + ") to " + type);
							}

							value = tmp;
						}

						m.invoke(handler, value);
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error setting " + value + " (" + value.getClass().getName() + ") for " + name + " at " + handler.getClass().getName(), e);
					}
				}
			}

			Boolean success = bpmContext.execute(new JbpmCallback<Boolean>() {

				@Override
				public Boolean doInJbpm(JbpmContext context) throws JbpmException {
					try {
						ProcessInstance pi = context.getProcessInstance(procInstId);

						ExecutionContext ectx = new ExecutionContext(pi.getRootToken());
						handler.execute(ectx);

						return Boolean.TRUE;
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error executing " + handler.getClass().getName() + ". Proc. inst. " + procInstId + ", params: " + params, e);
					}

					return Boolean.FALSE;
				}

			});

			return success == null ? false : success;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing handler " + handlerName + " for proc. inst. " + procInstId + ", params: " + params, e);
		}

		return false;
	}

}