package is.idega.idegaweb.egov.bpm.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.io.MemoryFileBuffer;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.FileUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

public class ProcessUsersExporter extends DownloadWriter implements MediaWritable {

	public static final String PROCESS_INSTANCE_ID = "pr-inst-id";
	public static final String SHOW_USER_COMPANY = "show-u-c";

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private ProcessArtifacts processArtifacts;

	private MemoryFileBuffer memory = null;

	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_EXCEL_2;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		if (iwc == null || !iwc.isLoggedOn()) {
			return;
		}

		ELUtil.getInstance().autowire(this);
		String id = iwc.getParameter(PROCESS_INSTANCE_ID);
		long processInstanceId = Long.valueOf(id);
		ProcessManager processManager = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId);
		ProcessInstanceW piw = processManager.getProcessInstance(processInstanceId);
		Collection<User> users = processArtifacts.getUsersConnectedToProces(piw);
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);

		String fileName = iwrb.getLocalizedString("exported_contacts", "Exported contacts");

		CasesSearchResultsHolder searchResultHolder = ELUtil.getInstance().getBean(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER);
		boolean showCompany = "y".equals(iwc.getParameter(SHOW_USER_COMPANY));

		memory = searchResultHolder.getUsersExport(users, iwc.getCurrentLocale(), showCompany);

		memory.setMimeType(MimeTypeUtil.MIME_TYPE_EXCEL_2);
		setAsDownload(iwc, fileName.concat(".xls"),	memory.length());
	}

	@Override
	public void writeTo(IWContext iwc, OutputStream streamOut) throws IOException {
		InputStream streamIn = new ByteArrayInputStream(memory.buffer());
		FileUtil.streamToOutputStream(streamIn, streamOut);

		streamOut.flush();
		streamOut.close();
		streamIn.close();
	}

}