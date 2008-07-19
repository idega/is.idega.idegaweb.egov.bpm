package is.idega.idegaweb.egov.bpm.cases.bundle;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import com.idega.block.form.process.XFormsView;
import com.idega.documentmanager.business.DocumentManager;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.bundle.ProcessBundleResources;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewResource;
import com.idega.util.xml.XmlUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 * 
 * Last modified: $Date: 2008/07/19 20:43:59 $ by $Author: civilis $
 * 
 */
public class CasesBPMBundledFormViewResource implements ViewResource {

	private String taskName;
	private View view;
	//private IWBundle bundle;
	//private InputStream bundleInputStream;
	private String pathWithinBundle;
	private DocumentManagerFactory documentManagerFactory;
	private ProcessBundleResources bundleResources;

	public View store(IWMainApplication iwma) throws IOException {

		if (view == null) {

			InputStream is = null;
			
			try {
				is = getBundleResources().getResourceIS(getPathWithinBundle());
				DocumentManager documentManager = getDocumentManagerFactory()
						.newDocumentManager(iwma);
				DocumentBuilder builder = XmlUtil.getDocumentBuilder();

				Document xformXml = builder.parse(is);
				com.idega.documentmanager.business.Document form = documentManager
					.openForm(xformXml);
				
				form.setFormType(XFormsView.FORM_TYPE);
				form.save();
				
				XFormsView view = new XFormsView();
				view.setFormDocument(form);
				this.view = view;

			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				
				if(is != null)
					is.close();
			}
		}

		return view;
	}
	
	/*
	protected InputStream getResourceInputStream() {
		
		if (getPathWithinBundle() == null)
			throw new IllegalStateException("Resource location not initialized");
		
		InputStream is = null;
		
		try {
		
			is = getBundleResources().getResourceIS(getPathWithinBundle());
			
			if(getBundle() != null) {
				 is = getBundle().getResourceInputStream(getPathWithinBundle());
				 
			} else if(getBundleInputStream() != null) {
				
				String fileName = getPathWithinBundle();
				InputStream bundleInputStream = getBundleInputStream();
				ZipInputStream zipStream = new ZipInputStream(bundleInputStream);
				ZipEntry entry;
				
				while ((entry = zipStream.getNextEntry()) != null) {
					
					String entryName = entry.getName();
					System.out.println("entryname="+entryName);
					
					if(fileName.equals(entryName)) {
						
						ZipInstaller zip = new ZipInstaller();
					
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						zip.writeFromStreamToStream(bundleInputStream, os);
						is = new ByteArrayInputStream(os.toByteArray());
						zipStream.closeEntry();
						break;
					}

					zipStream.closeEntry();
				}
				
			} else {
				throw new IllegalStateException("No bundle nor bundle zip stream provided");
			}
			
			return is;
			
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving resource input stream", e);
		}
		
		return null;
	}
	*/

	public String getTaskName() {

		return taskName;
	}

	public void setTaskName(String taskName) {

		this.taskName = taskName;
	}

//	public void setResourceLocation(IWBundle bundle, String pathWithinBundle) {
//
//		this.bundle = bundle;
//		this.pathWithinBundle = pathWithinBundle;
//	}
	
	public void setResourceLocation(ProcessBundleResources bundleResources, String pathWithinBundle) {

		this.bundleResources = bundleResources;
		this.pathWithinBundle = pathWithinBundle;
	}
	
//	public void setResourceLocation(InputStream bundleInputStream, String pathWithinZip) {
//
//		this.pathWithinBundle = pathWithinZip;
//		this.bundleInputStream = bundleInputStream;
//	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

//	public InputStream getBundleInputStream() {
//		return bundleInputStream;
//	}
//
//	public IWBundle getBundle() {
//		return bundle;
//	}

	public String getPathWithinBundle() {
		return pathWithinBundle;
	}

	public ProcessBundleResources getBundleResources() {
		return bundleResources;
	}
}