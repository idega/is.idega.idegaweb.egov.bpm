package is.idega.idegaweb.egov.bpm.business;

import is.idega.idegaweb.egov.bpm.bean.BPMAttachmentDownloadNotificationProperties;
import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserFactory;
import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserImpl;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.file.FileDownloadNotificationProperties;
import com.idega.business.file.FileDownloadNotifier;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.file.data.ICFile;
import com.idega.dwr.business.DWRAnnotationPersistance;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MediaWritable;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.user.data.User;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(BPMAttachmentDownloadNotifier.BEAN_IDENTIFIER)
@RemoteProxy(creator=SpringCreator.class, creatorParams={
	@Param(name="beanName", value=BPMAttachmentDownloadNotifier.BEAN_IDENTIFIER),
	@Param(name="javascript", value=BPMAttachmentDownloadNotifier.DWR_OBJECT)
}, name=BPMAttachmentDownloadNotifier.DWR_OBJECT)
public class BPMAttachmentDownloadNotifier extends FileDownloadNotifier implements DWRAnnotationPersistance {

	private static final long serialVersionUID = -6038272277947394981L;

	public static final String BEAN_IDENTIFIER = "bpmAttachmentDownloadNotifier";
	public static final String DWR_OBJECT = "BPMAttachmentDownloadNotifier";
	
	@Autowired
	private CaseUserFactory caseUserFactory;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@RemoteMethod
	public AdvancedProperty sendDownloadNotifications(BPMAttachmentDownloadNotificationProperties properties) { 
		return super.sendNotifications(properties);
	}

	@Override
	public String getUriToAttachment(FileDownloadNotificationProperties properties, User user) {
		if (!(properties instanceof BPMAttachmentDownloadNotificationProperties)) {
			return null;
		}
		
		BPMAttachmentDownloadNotificationProperties bpmProperties = (BPMAttachmentDownloadNotificationProperties) properties;
		
		URIUtil uri = new URIUtil(IWMainApplication.getDefaultIWMainApplication().getMediaServletURI());
		
		uri.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(AttachmentWriter.class));
		uri.setParameter(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID, bpmProperties.getTaskId().toString());
		uri.setParameter(AttachmentWriter.PARAMETER_VARIABLE_HASH, bpmProperties.getHash().toString());
		
		if (user != null) {
			uri.setParameter(LoginBusinessBean.PARAM_LOGIN_BY_UNIQUE_ID, user.getUniqueId());
			uri.setParameter(LoginBusinessBean.LoginStateParameter, LoginBusinessBean.LOGIN_EVENT_LOGIN);
		}
		
		return uri.getUri();
	}

	@Override
	protected ICFile getFile(FileDownloadNotificationProperties properties) {
		ICFile file = super.getFile(properties);
		
		if (file == null && properties instanceof BPMAttachmentDownloadNotificationProperties) {
			return getFile(((BPMAttachmentDownloadNotificationProperties) properties).getHash());
		}
		
		return file;
	}

	@Transactional(readOnly = true)
	@Override
	public Map<String, String> getUriToDocument(FileDownloadNotificationProperties properties, List<User> users) {
		if (ListUtil.isEmpty(users) || !(properties instanceof BPMAttachmentDownloadNotificationProperties)) {
			return null;
		}
		
		BPMAttachmentDownloadNotificationProperties realProperties = (BPMAttachmentDownloadNotificationProperties) properties;
		
		Map<String, String> linksForUsers = new HashMap<String, String>();
		for (User user: users) {
			CaseUserImpl caseUser = null;
			try {
				ProcessManager processManager = bpmFactory.getProcessManagerByTaskInstanceId(realProperties.getTaskId());
				TaskInstanceW tiw = processManager.getTaskInstance(realProperties.getTaskId());
				ProcessInstanceW piw = tiw.getProcessInstanceW();
				caseUser = caseUserFactory.getCaseUser(user, piw);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String uri = null;
			if (caseUser == null) {
				uri = properties.getUrl();
			} else {
				String url = caseUser.getUrlToTheCase();
				if (StringUtil.isEmpty(url)) {
					uri = properties.getUrl();
				} else {
					URIUtil uriUtil = new URIUtil(url);
					uriUtil.setParameter(UICasesBPMAssets.AUTO_SHOW_COMMENTS, Boolean.TRUE.toString());
					uri = uriUtil.getUri();
				}
			}
			
			uri = StringUtil.isEmpty(uri) ? properties.getUrl() : uri;
			linksForUsers.put(user.getId(), uri);
		}
		
		return linksForUsers;
	}

}
