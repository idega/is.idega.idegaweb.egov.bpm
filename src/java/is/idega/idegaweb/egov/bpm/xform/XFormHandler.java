package is.idega.idegaweb.egov.bpm.xform;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.data.XFormSubmission;
import com.idega.block.form.data.dao.XFormsDAO;
import com.idega.block.form.presentation.FormViewer;
import com.idega.bpm.data.XFormSubmissionData;
import com.idega.chiba.web.xml.xforms.validation.XFormSubmissionValidator;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.persistence.Param;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.presentation.BPMTaskViewer;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service("bpmApplicationXFormHandler")
public class XFormHandler extends DefaultSpringBean implements XFormSubmissionValidator {

	@Autowired
	private BPMDAO bpmDAO;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private XFormsDAO xformsDAO;

	private XFormsDAO getXFormsDAO() {
		if (xformsDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return xformsDAO;
	}

	private BPMFactory getBPMFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmFactory;
	}

	private BPMDAO getBPMDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmDAO;
	}

	private boolean isUserAbleToSubmitTask(User user, TaskInstanceW task) {
		if (user == null || task == null) {
			return false;
		}

		IWContext iwc = CoreUtil.getIWContext();
		List<BPMDocument> tasksForUser = task.getProcessInstanceW().getTaskDocumentsForUser(iwc, user, getCurrentLocale(), Boolean.FALSE);
		if (ListUtil.isEmpty(tasksForUser)) {
			getLogger().warning("There are no tasks available for the user " + user);
			return false;
		} else {
			boolean foundAvailableTask = false;
			long tiId = task.getTaskInstanceId();
			for (Iterator<BPMDocument> tasksIter = tasksForUser.iterator(); (!foundAvailableTask && tasksIter.hasNext());) {
				Serializable id = tasksIter.next().getTaskInstanceId();
				if (id instanceof Number) {
					foundAvailableTask = tiId == ((Number)id).longValue();
				}

			}

			if (foundAvailableTask) {
				return true;
			} else {
				getLogger().warning("Task with ID " + tiId + " is not available for submitting by the user " + user);
				return false;
			}
		}
	}

	@Override
	public boolean isPossibleToSubmitXForm(String uri) {
		if (StringUtil.isEmpty(uri))
		 {
			return true;	//	Error - can not determine
		}

		User user = null;
		try {
			user = getBPMFactory().getBpmUserFactory().getCurrentBPMUser().getUserToUse();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error resolving BPM user", e);
		}

		URIUtil uriUtil = new URIUtil(uri);
		Map<String, String> params = uriUtil.getParameters();
		if (params == null || params.isEmpty())
		 {
			return true;	//	Error - can not determine
		}

		try {
			Long procDefId = null;
			if (params.containsKey(BPMTaskViewer.PROCESS_DEFINITION_PROPERTY)) {
				procDefId = Long.valueOf(params.get(BPMTaskViewer.PROCESS_DEFINITION_PROPERTY));
			} else if (params.containsKey(CasesBPMAssetsState.TASK_INSTANCE_ID_PARAMETER)) {
				Long tiId = Long.valueOf(params.get(CasesBPMAssetsState.TASK_INSTANCE_ID_PARAMETER));
				TaskInstanceW task = getBPMFactory().getProcessManagerByTaskInstanceId(tiId).getTaskInstance(tiId);
				if (user == null) {
					procDefId = task.getProcessInstanceW().getProcessDefinitionW().getProcessDefinitionId();
				} else {
					return isUserAbleToSubmitTask(user, task);
				}
			} else if (params.containsKey(FormViewer.submissionIdParam)) {
				String submissionId = params.get(FormViewer.submissionIdParam);
				if (!StringHandler.isNumeric(submissionId)) {
					XFormSubmission submission = getXFormsDAO().getSubmissionBySubmissionUUID(submissionId);
					if (submission != null) {
						if (submission.getProvider() != null) {
							procDefId = getXFormsDAO().getSingleResultByInlineQuery(
									"select sd.processDefinition.id from " + XFormSubmissionData.class.getName() + " sd where sd.id = :provider",
									Long.class,
									new Param("provider", submission.getProvider())
							);
						} else {
							submissionId = String.valueOf(submission.getSubmissionId());
						}
					}
				}
				if (procDefId == null && StringHandler.isNumeric(submissionId)) {
					procDefId = getXFormsDAO().getSingleResultByInlineQuery(
							"select sd.processDefinition.id from " + XFormSubmissionData.class.getName() + " sd where sd.submission.submissionId = :submissionId",
							Long.class,
							new Param("submissionId", Long.valueOf(submissionId))
						);
				}
			}
			if (procDefId == null) {
				getLogger().warning("Unable to resolve process definition ID from the parameters: " + params);
				return true;	//	Error - can not determine
			}

			String appUrl = getBPMDAO().getProcessDefinitionNameByProcessDefinitionId(procDefId);
			if (StringUtil.isEmpty(appUrl)) {
				getLogger().warning("App URL can not be resolved from proc. def. ID: " + procDefId);
				return true;	//	Error - can not determine
			}

			ApplicationBusiness appBusiness = getServiceInstance(ApplicationBusiness.class);
			Collection<Application> apps = appBusiness.getApplicationHome().findAllByApplicationUrl(appUrl);
			if (ListUtil.isEmpty(apps)) {
				getLogger().warning("No applications were found by URL: " + appUrl);
				return getApplication().getSettings().getBoolean("submit_xform_if_logged_out", Boolean.TRUE);	//	Be default allowing to submit
			}

			for (Application app: apps) {
				if (app.getRequiresLogin() && user == null) {
					getLogger().warning("User must be logged in to submit the application: " + app.getNameByLocale(getCurrentLocale()));
					return false;
				}
			}
		} catch (Exception e) {
			String message = "Error resolving if xform at '" + uri + "' needs to be invalidated";
			CoreUtil.sendExceptionNotification(message, e);
			getLogger().log(Level.WARNING, message, e);
		}

		return true;
	}

}