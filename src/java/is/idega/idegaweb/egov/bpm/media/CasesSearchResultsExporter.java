package is.idega.idegaweb.egov.bpm.media;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.io.MemoryFileBuffer;
import com.idega.presentation.IWContext;
import com.idega.util.FileUtil;
import com.idega.util.expression.ELUtil;

public class CasesSearchResultsExporter extends DownloadWriter implements MediaWritable {

	public static final String ID_PARAMETER = "casesSearchResultsExportId",
								ALL_CASES_DATA = "allCasesExportedId",
								EXPORT_CONTACTS = "is-export-contacts",
								SHOW_USER_COMPANY = "show-company";

	private MemoryFileBuffer memory;

	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_EXCEL_2;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		String fileName = null;
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);

		CasesSearchResultsHolder searchResultHolder = ELUtil.getInstance().getBean(
				CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER);
		boolean exportContacts = "y".equals(iwc.getParameter(EXPORT_CONTACTS));
		boolean showCompany = "y".equals(iwc.getParameter(SHOW_USER_COMPANY));

		if (iwc.isParameterSet(ID_PARAMETER)) {
			String id = iwc.getParameter(ID_PARAMETER);
			memory = searchResultHolder.getExportedSearchResults(id, exportContacts, showCompany);
			fileName = iwrb.getLocalizedString("exported_search_results_in_excel_file_name", "Exported search results");
		} else if (iwc.isParameterSet(ALL_CASES_DATA)) {
			String instanceId = iwc.getParameter(ALL_CASES_DATA);
			memory = searchResultHolder.getExportedCases(instanceId, exportContacts, showCompany);
			fileName = iwrb.getLocalizedString("exported_all_cases_data", "Exported cases");
		} else
			return;

		memory.setMimeType(MimeTypeUtil.MIME_TYPE_EXCEL_2);
		setAsDownload(iwc, fileName.concat(".xls"),	memory.length());
	}

	@Override
	public void writeTo(OutputStream streamOut) throws IOException {
		InputStream streamIn = new ByteArrayInputStream(memory.buffer());
		FileUtil.streamToOutputStream(streamIn, streamOut);

		streamOut.flush();
		streamOut.close();
		streamIn.close();
	}

}
