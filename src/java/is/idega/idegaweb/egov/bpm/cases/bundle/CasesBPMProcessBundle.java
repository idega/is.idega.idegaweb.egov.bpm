package is.idega.idegaweb.egov.bpm.cases.bundle;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMManagersCreator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWBundle;
import com.idega.jbpm.def.ProcessBundle;
import com.idega.jbpm.def.ViewResource;
import com.idega.jbpm.def.ViewToTask;
import com.idega.jbpm.def.ViewToTaskType;
import com.idega.util.CoreConstants;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 * 
 * Last modified: $Date: 2008/04/21 05:09:05 $ by $Author: civilis $
 * 
 */
@Scope("prototype")
@Service("casesBPMProcessBundle")
public class CasesBPMProcessBundle implements ProcessBundle {

	//private static final String propertiesFileName = "bundle.properties";
	private static final String processDefinitionFileName = "processdefinition.xml";
	private static final String formsPath = "forms/";
	private static final String dotRegExp = "\\.";
	private static final String taskPrefix = "task";
	
	private static final String initTaskProp = "init_task";
	private static final String taskNamePostfixProp = ".name";
	
	private static final String XFFileNamePropertyPostfix = ".view.xforms.file_name";

	private IWBundle bundle;
	private String bundlePropertiesLocationWithinBundle;
	private DocumentManagerFactory documentManagerFactory;
	
	private ViewToTask viewToTaskBinder;

	public ViewToTask getViewToTaskBinder() {
		return viewToTaskBinder;
	}

	@Autowired
	@ViewToTaskType("xforms")
	public void setViewToTaskBinder(ViewToTask viewToTaskBinder) {
		this.viewToTaskBinder = viewToTaskBinder;
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	@Autowired
	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public ProcessDefinition getProcessDefinition() throws IOException {

		String templateBundleLocationWithinBundle = getBundlePropertiesLocationWithinBundle().substring(0, getBundlePropertiesLocationWithinBundle().lastIndexOf(CoreConstants.SLASH)+1);

		if (templateBundleLocationWithinBundle == null)
			throw new IllegalStateException(
					"No templateBundleLocationWithinBundle set");

		InputStream pdIs = getBundle().getResourceInputStream(
				templateBundleLocationWithinBundle + processDefinitionFileName);
		ProcessDefinition pd = ProcessDefinition.parseXmlInputStream(pdIs);
		return pd;
	}
	
	public List<ViewResource> getViewResources(String taskName)
			throws IOException {

		String templateBundleLocationWithinBundle = getBundlePropertiesLocationWithinBundle().substring(0, getBundlePropertiesLocationWithinBundle().lastIndexOf(CoreConstants.SLASH)+1);

		if (templateBundleLocationWithinBundle == null)
			throw new IllegalStateException(
					"No templateBundleLocationWithinBundle set");

		String formsPathWithin = templateBundleLocationWithinBundle	+ formsPath;
		Properties properties = resolveBundleProperties();

		for (Entry<Object, Object> entry : properties.entrySet()) {

			if (taskName.equals(entry.getValue())) {

				String key = (String) entry.getKey();
				
				if(!key.startsWith(taskPrefix))
					continue;
				
				String taskIdentifier = key.split(dotRegExp)[0];
				String fileName = properties.getProperty(taskIdentifier
						+ XFFileNamePropertyPostfix);

				CasesBPMBundledFormViewResource resource = new CasesBPMBundledFormViewResource();
				resource.setTaskName(taskName);
				resource.setDocumentManagerFactory(getDocumentManagerFactory());
				String pathWithinBundle = formsPathWithin + fileName;
				resource.setResourceLocation(getBundle(), pathWithinBundle);

				ArrayList<ViewResource> viewResources = new ArrayList<ViewResource>(1);
				viewResources.add(resource);
				return viewResources;
			}
		}

		return null;
	}

	public IWBundle getBundle() {
		return bundle;
	}

	public void setBundle(IWBundle bundle) {
		this.bundle = bundle;
	}

	public String getBundlePropertiesLocationWithinBundle() {
		return bundlePropertiesLocationWithinBundle;
	}

	public void setBundlePropertiesLocationWithinBundle(
			String bundlePropertiesLocationWithinBundle) {
		this.bundlePropertiesLocationWithinBundle = bundlePropertiesLocationWithinBundle;
	}

	protected Properties resolveBundleProperties() throws IOException {
		
		InputStream propertiesIs = getBundle()
		.getResourceInputStream(getBundlePropertiesLocationWithinBundle());

		Properties properties = new Properties();
		properties.load(propertiesIs);
		return properties;
	}
	
	public void configure(ProcessDefinition pd) {
		
		try {
			Properties properties = resolveBundleProperties();
			String initTaskKey = properties.getProperty(initTaskProp);
			String initTaskName = properties.getProperty(initTaskKey+taskNamePostfixProp);
			Task initTask = pd.getTaskMgmtDefinition().getTask(initTaskName);
			pd.getTaskMgmtDefinition().setStartTask(initTask);
			
		} catch (IOException e) {
			throw new RuntimeException("IOException while accessing process bundle properties");
		}
	}
	
	public String getManagersType() {
		
		return CasesBPMManagersCreator.MANAGERS_TYPE;
	}
}