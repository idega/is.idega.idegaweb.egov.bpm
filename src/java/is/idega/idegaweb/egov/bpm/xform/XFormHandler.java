package is.idega.idegaweb.egov.bpm.xform;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.chiba.web.xml.xforms.validation.XFormSubmissionValidator;
import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.presentation.BPMTaskViewer;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service("bpmApplicationXFormHandler")
public class XFormHandler extends DefaultSpringBean implements XFormSubmissionValidator {

	@Autowired
	private BPMDAO bpmDAO;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	private BPMFactory getBPMFactory() {
		if (bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		return bpmFactory;
	}
	
	private BPMDAO getBPMDAO() {
		if (bpmDAO == null)
			ELUtil.getInstance().autowire(this);
		return bpmDAO;
	}
	
	public boolean isRequiredToBeLoggedIn(String uri) {
		if (StringUtil.isEmpty(uri))
			return false;	//	Error
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			getLogger().warning("Unable to get instance of " + IWContext.class.getSimpleName());
			return false;	//	Error
		}
		if (iwc.isLoggedOn())
			return false;	//	Everything is fine
		
		URIUtil uriUtil = new URIUtil(uri);
		Map<String, String> params = uriUtil.getParameters();
		if (params == null || params.isEmpty())
			return false;	//	Error
		
		try {
			Long procDefId = null;
			if (params.containsKey(BPMTaskViewer.PROCESS_DEFINITION_PROPERTY)) {
				procDefId = Long.valueOf(params.get(BPMTaskViewer.PROCESS_DEFINITION_PROPERTY));
			} else if (params.containsKey(CasesBPMAssetsState.TASK_INSTANCE_ID_PARAMETER)) {
				Long tiId = Long.valueOf(params.get(CasesBPMAssetsState.TASK_INSTANCE_ID_PARAMETER));
				TaskInstanceW task = getBPMFactory().getProcessManagerByTaskInstanceId(tiId).getTaskInstance(tiId);
				procDefId = task.getProcessInstanceW().getProcessDefinitionW().getProcessDefinitionId();
			}
			if (procDefId == null) {
				getLogger().warning("Unable to resolve process definition ID from the parameters: " + params);
				return false;	//	Error
			}
			
			String appUrl = getBPMDAO().getProcessDefinitionNameByProcessDefinitionId(procDefId);
			if (StringUtil.isEmpty(appUrl))
				return false;	//	Error
			
			ApplicationBusiness appBusiness = getServiceInstance(ApplicationBusiness.class);
			Collection<Application> apps = appBusiness.getApplicationHome().findAllByApplicationUrl(appUrl);
			if (ListUtil.isEmpty(apps)) {
				getLogger().warning("No applications were found by URL: " + appUrl);
				return getApplication().getSettings().getBoolean("invalidate_xform_if_not_logged", Boolean.FALSE);
			}
			
			for (Application app: apps) {
				if (app.getRequiresLogin())
					return true;
			}
		} catch (Exception e) {
			String message = "Error resolving if xform at '" + uri + "' needs to be invalidated";
			CoreUtil.sendExceptionNotification(message, e);
			getLogger().log(Level.WARNING, message, e);
		}
		
		return false;
	}
	
}