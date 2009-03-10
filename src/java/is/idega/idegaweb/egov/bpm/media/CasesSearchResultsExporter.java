package is.idega.idegaweb.egov.bpm.media;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.io.MemoryFileBuffer;
import com.idega.presentation.IWContext;
import com.idega.util.FileUtil;
import com.idega.util.expression.ELUtil;

public class CasesSearchResultsExporter extends DownloadWriter implements MediaWritable {

	public static final String ID_PARAMETER = "casesSearchResultsExportId";
	
	private MemoryFileBuffer memory;
	
	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_EXCEL_2;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		CasesSearchResultsHolder searchResultHolder = ELUtil.getInstance().getBean(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER);
		String id = iwc.getParameter(ID_PARAMETER);
		memory = searchResultHolder.getExportedSearchResults(id);
		
		memory.setMimeType(MimeTypeUtil.MIME_TYPE_EXCEL_2);
		setAsDownload(iwc, new StringBuilder(iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc)
								.getLocalizedString("exported_search_results_in_excel_file_name", "Exported search results")).append(".xls").toString(),
					memory.length());
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
