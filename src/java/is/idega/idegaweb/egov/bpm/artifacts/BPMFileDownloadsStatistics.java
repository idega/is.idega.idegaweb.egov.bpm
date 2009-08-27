package is.idega.idegaweb.egov.bpm.artifacts;

import is.idega.idegaweb.egov.bpm.business.ProcessAttachmentDownloadNotifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.jboss.jbpm.IWBundleStarter;

import com.idega.core.file.data.ICFile;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.presentation.IWContext;
import com.idega.presentation.file.FileDownloadStatisticsViewer;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;

public class BPMFileDownloadsStatistics extends FileDownloadStatisticsViewer {

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
	public String getNotifierAction(IWContext iwc, ICFile file, Collection<User> usersToInform) {
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

}
