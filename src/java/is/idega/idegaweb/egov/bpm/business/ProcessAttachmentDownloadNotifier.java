package is.idega.idegaweb.egov.bpm.business;

import java.util.HashMap;
import java.util.Iterator;
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

import com.idega.block.article.component.CommentsViewer;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.file.FileDownloadNotificationProperties;
import com.idega.business.file.FileDownloadNotifier;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.dwr.business.DWRAnnotationPersistance;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MediaWritable;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

import is.idega.idegaweb.egov.bpm.bean.BPMAttachmentDownloadNotificationProperties;
import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserFactory;
import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserImpl;

@Service(ProcessAttachmentDownloadNotifier.BEAN_IDENTIFIER)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@RemoteProxy(creator=SpringCreator.class, creatorParams={
	@Param(name="beanName", value=ProcessAttachmentDownloadNotifier.BEAN_IDENTIFIER),
	@Param(name="javascript", value=ProcessAttachmentDownloadNotifier.DWR_OBJECT)
}, name=ProcessAttachmentDownloadNotifier.DWR_OBJECT)
public class ProcessAttachmentDownloadNotifier extends FileDownloadNotifier implements DWRAnnotationPersistance {

	private static final long serialVersionUID = 6760529129704014601L;

	public static final String BEAN_IDENTIFIER = "processAttachmentDownloadNotifier";
	public static final String DWR_OBJECT = "ProcessAttachmentDownloadNotifier";

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
	protected AdvancedProperty getFile(FileDownloadNotificationProperties properties) {
		AdvancedProperty file = super.getFile(properties);

		if (file == null && properties instanceof BPMAttachmentDownloadNotificationProperties) {
			BPMAttachmentDownloadNotificationProperties realProperties = (BPMAttachmentDownloadNotificationProperties) properties;

			file = getFile(realProperties.getHash());

			Integer fileHash = realProperties.getHash();
			if (file == null && fileHash != null) {
				IWContext iwc = CoreUtil.getIWContext();
				ProcessManager processManager = bpmFactory.getProcessManagerByTaskInstanceId(realProperties.getTaskId());
				TaskInstanceW tiw = processManager.getTaskInstance(realProperties.getTaskId());
				List<BinaryVariable> attachments = tiw.getAttachments(iwc);
				if (!ListUtil.isEmpty(attachments)) {
					for (Iterator<BinaryVariable> variablesIter = attachments.iterator(); (variablesIter.hasNext() && file == null);) {
						BinaryVariable attachment = variablesIter.next();

						Integer attachmenHash = attachment.getHash();
						if (attachmenHash != null && attachmenHash.intValue() == fileHash.intValue()) {
							file = new AdvancedProperty(String.valueOf(fileHash), attachment.getFileName());
						}
					}
				}
			}
		}

		return file;
	}

	@Override
	public Map<String, String> getUriToDocument(FileDownloadNotificationProperties properties, List<User> users) {
		if (ListUtil.isEmpty(users) || !(properties instanceof BPMAttachmentDownloadNotificationProperties)) {
			return null;
		}

		BPMAttachmentDownloadNotificationProperties realProperties = (BPMAttachmentDownloadNotificationProperties) properties;

		ProcessInstanceW piw = null;
		try {
			ProcessManager processManager = bpmFactory.getProcessManagerByTaskInstanceId(realProperties.getTaskId());
			TaskInstanceW tiw = processManager.getTaskInstance(realProperties.getTaskId());
			piw = tiw.getProcessInstanceW();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (piw == null) {
			return null;
		}

		Map<String, String> linksForUsers = new HashMap<String, String>();
		for (User user: users) {
			CaseUserImpl caseUser = null;
			try {
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
					uriUtil.setParameter(CommentsViewer.AUTO_SHOW_COMMENTS, Boolean.TRUE.toString());
					uri = uriUtil.getUri();
				}
			}

			uri = StringUtil.isEmpty(uri) ? properties.getUrl() : uri;
			linksForUsers.put(user.getId(), uri);
		}

		return linksForUsers;
	}

}