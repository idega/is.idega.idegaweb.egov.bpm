package is.idega.idegaweb.egov.bpm.artifacts;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.jbpm.IWBundleStarter;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.presentation.file.FileDownloadStatisticsViewer;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.business.ProcessAttachmentDownloadNotifier;

public class BPMFileDownloadsStatistics extends FileDownloadStatisticsViewer {

	@Autowired
	private BPMFactory bpmFactory;

	@Override
	public boolean hasRights(IWContext iwc) {
		return Boolean.TRUE;
	}

	@Override
	public String getBundleIdentifier() {
		return IWBundleStarter.IW_BUNDLE_IDENTIFIER;
	}

	@Override
	public Collection<User> getPotentialDownloaders(IWContext iwc) {
		String caseId = iwc.getParameter("caseId");
		if (StringUtil.isEmpty(caseId) || "-1".equals(caseId)) {
			return null;
		}

		setFileHolderIdentifier(caseId);

		return super.getPotentialDownloaders(iwc);
	}

	@Override
	public String getNotifierAction(IWContext iwc, AdvancedProperty file, Collection<User> usersToInform) {
		String taskId = iwc.getParameter(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID);
		String varHash = iwc.getParameter(AttachmentWriter.PARAMETER_VARIABLE_HASH);
		if (StringUtil.isEmpty(taskId) || StringUtil.isEmpty(varHash)) {
			return null;
		}

		StringBuilder realAction = new StringBuilder("CasesBPMAssets.notifyToDownloadAttachment({taskId: ").append(taskId)
			.append(", hash: ").append(varHash).append(", file: ").append(file == null ? "null" : "'"+file.getId()+"'").append(", users: [");
		for (Iterator<User> usersIter = usersToInform.iterator(); usersIter.hasNext();) {
			realAction.append(CoreConstants.QOUTE_SINGLE_MARK).append(usersIter.next().getId()).append(CoreConstants.QOUTE_SINGLE_MARK);
			if (usersIter.hasNext()) {
				realAction.append(CoreConstants.COMMA);
			}
		}
		realAction.append("]});");

		return PresentationUtil.getJavaScriptLinesLoadedLazily(Arrays.asList(
				CoreConstants.DWR_ENGINE_SCRIPT,
				"/dwr/interface/" + ProcessAttachmentDownloadNotifier.DWR_OBJECT + ".js"
		), realAction.toString());
	}

	@Override
	protected AdvancedProperty getFile(IWContext iwc) {
		AdvancedProperty file = super.getFile(iwc);
		if (file != null) {
			return file;
		}

		String taskId = iwc.getParameter(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID);
		String varHash = iwc.getParameter(AttachmentWriter.PARAMETER_VARIABLE_HASH);
		if (StringUtil.isEmpty(taskId) || StringUtil.isEmpty(varHash)) {
			return null;
		}

		ELUtil.getInstance().autowire(this);

		Long taskID = null;
		try {
			taskID = Long.valueOf(taskId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (taskID == null) {
			return null;
		}

		Integer hash = null;
		try {
			hash = Integer.valueOf(varHash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (hash == null) {
			return null;
		}

		ProcessManager processManager = bpmFactory.getProcessManagerByTaskInstanceId(taskID);
		TaskInstanceW tiw = processManager.getTaskInstance(taskID);
		List<BinaryVariable> attachments = tiw.getAttachments();
		if (!ListUtil.isEmpty(attachments)) {
			for (Iterator<BinaryVariable> variablesIter = attachments.iterator(); (variablesIter.hasNext() && file == null);) {
				BinaryVariable attachment = variablesIter.next();

				Integer attachmenHash = attachment.getHash();
				if (attachmenHash != null && attachmenHash.intValue() == hash.intValue()) {
					file = new AdvancedProperty(varHash, attachment.getFileName());
				}
			}
		}

		if (file != null) {
			setFile(file);
		}
		return file;
	}

	@Override
	public String getMessageNobodyIsInterested(IWContext iwc) {
		return getResourceBundle(iwc).getLocalizedString("there_are_no_users_interested_in_this_case", "There are no users interesed in this case");
	}

}