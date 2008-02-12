package is.idega.idegaweb.egov.bpm.cases.bundle;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMManagersCreator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.def.ProcessBundle;
import com.idega.jbpm.def.ViewResource;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 * 
 * Last modified: $Date: 2008/02/12 14:37:24 $ by $Author: civilis $
 * 
 */
public class CasesBPMProcessBundle implements ProcessBundle {

	private static final String propertiesFileName = "bundle.properties";
	private static final String processDefinitionFileName = "processdefinition.xml";
	private static final String formsPath = "forms/";
	private static final String dotRegExp = "\\.";
	private static final String taskPrefix = "task";
	
	private static final String initTaskProp = "init_task";
	private static final String taskNamePostfixProp = ".name";
	
	private static final String XFFileNamePropertyPostfix = ".view.xforms.file_name";

	private IWBundle bundle;
	private String templateBundleLocationWithinBundle;
	private DocumentManagerFactory documentManagerFactory;
	private CasesBPMDAO casesBPMDAO;
	
	private Long caseCategoryId;
	private Long caseTypeId;
	

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public ProcessDefinition getProcessDefinition() throws IOException {

		String templateBundleLocationWithinBundle = getTemplateBundleLocationWithinBundle();

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

		String templateBundleLocationWithinBundle = getTemplateBundleLocationWithinBundle();

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
		bundle = null;
		if (bundle == null) {
			IWMainApplication iwma = IWMainApplication
					.getIWMainApplication(FacesContext.getCurrentInstance());
			bundle = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		}
		return bundle;
	}

	public void setBundle(IWBundle bundle) {
		this.bundle = bundle;
	}

	public String getTemplateBundleLocationWithinBundle() {
		return templateBundleLocationWithinBundle;
	}

	public void setTemplateBundleLocationWithinBundle(
			String templateBundleLocationWithinBundle) {
		this.templateBundleLocationWithinBundle = templateBundleLocationWithinBundle;
	}
	
	private Properties resolveBundleProperties() throws IOException {
		
		InputStream propertiesIs = bundle
		.getResourceInputStream(templateBundleLocationWithinBundle
				+ propertiesFileName);

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

		if(caseCategoryId != null && caseTypeId != null) {
		
			CaseTypesProcDefBind bind = new CaseTypesProcDefBind();
			bind.setCasesCategoryId(caseCategoryId);
			bind.setCasesTypeId(caseTypeId);
			bind.setProcDefId(pd.getId());
			//bind.setInitTaskName(initTaskName);
			getCasesBPMDAO().persist(bind);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
	
	public void setCaseMetaInf(Long caseCategoryId, Long caseTypeId) {
		this.caseCategoryId = caseCategoryId;
		this.caseTypeId = caseTypeId;
	}
	
	public String getManagersType() {
		
		return CasesBPMManagersCreator.MANAGERS_TYPE;
	}
}