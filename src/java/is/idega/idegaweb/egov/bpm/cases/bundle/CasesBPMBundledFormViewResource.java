package is.idega.idegaweb.egov.bpm.cases.bundle;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import com.idega.block.form.process.XFormsView;
import com.idega.documentmanager.business.DocumentManager;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewResource;
import com.idega.util.xml.XmlUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/04/02 19:23:30 $ by $Author: civilis $
 * 
 */
public class CasesBPMBundledFormViewResource implements ViewResource {

	private String taskName;
	private View view;
	private IWBundle bundle;
	private String pathWithinBundle;
	private DocumentManagerFactory documentManagerFactory;

	public View store(IWMainApplication iwma) throws IOException {

		if (view == null) {

			try {
				if (pathWithinBundle == null || bundle == null)
					throw new IllegalStateException(
							"Resource location not initialized");

				InputStream is = bundle
						.getResourceInputStream(pathWithinBundle);
				DocumentManager documentManager = getDocumentManagerFactory()
						.newDocumentManager(iwma);
				DocumentBuilder builder = XmlUtil.getDocumentBuilder();

				Document xformXml = builder.parse(is);
				com.idega.documentmanager.business.Document form = documentManager
						.openFormAndGenerateId(xformXml);
				form.save();
				
				XFormsView view = new XFormsView();
				view.setFormDocument(form);
				this.view = view;

			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return view;
	}

	public String getTaskName() {

		return taskName;
	}

	public void setTaskName(String taskName) {

		this.taskName = taskName;
	}

	public void setResourceLocation(IWBundle bundle, String pathWithinBundle) {

		this.bundle = bundle;
		this.pathWithinBundle = pathWithinBundle;
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}
}