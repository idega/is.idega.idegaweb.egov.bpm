package is.idega.idegaweb.egov.bpm.cases.bundle;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMManagersCreator;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.CaseType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.def.ProcessBundle;
import com.idega.jbpm.def.ViewResource;
import com.idega.jbpm.def.ViewToTask;
import com.idega.jbpm.def.ViewToTaskType;
import com.idega.user.business.GroupBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreConstants;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 * 
 * Last modified: $Date: 2008/04/26 02:32:11 $ by $Author: civilis $
 * 
 */
@Scope("prototype")
@Service("casesBPMProcessBundle")
public class CasesBPMProcessBundle implements ProcessBundle {
	
	public static final String defaultCaseTypeName = "BPM";
	public static final String defaultCaseCategoryName = "BPM";
	public static final String defaultCaseHandlersGroupName = "BPM Cases Handlers";

	//private static final String propertiesFileName = "bundle.properties";
	private static final String processDefinitionFileName = "processdefinition.xml";
	private static final String formsPath = "forms/";
	private static final String dotRegExp = "\\.";
	private static final String taskPrefix = "task";
	
	private static final String initTaskProp = "init_task";
	private static final String taskNamePostfixProp = ".name";
	
	private static final String XFFileNamePropertyPostfix = ".view.xforms.file_name";

	private IWBundle bundle;
	private CasesBPMDAO casesBPMDAO;
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
			
			assignToDefaultCaseTypes(pd);
			
		} catch (IOException e) {
			throw new RuntimeException("IOException while accessing process bundle properties");
		}
	}

	protected void assignToDefaultCaseTypes(ProcessDefinition pd) {
		
		CaseTypesProcDefBind ctpd = getCasesBPMDAO().getCaseTypesProcDefBindByPDName(pd.getName());
		
		if(ctpd == null) {
			
			try {
				String caseCategoryName = defaultCaseCategoryName;
				String caseTypeName = defaultCaseTypeName;
				String caseHandlersGroupName = defaultCaseHandlersGroupName;
				
				CasesBusiness casesBusiness = getCasesBusiness();
				Collection<CaseCategory> caseCategories = casesBusiness.getCaseCategoriesByName(caseCategoryName);
				Collection<CaseType> caseTypes = casesBusiness.getCaseTypesByName(caseTypeName);
				
				CaseCategory caseCategory;
				CaseType caseType;
				
				if(caseCategories == null || caseCategories.isEmpty()) {
					
					GroupBusiness groupBusiness = getGroupBusiness();

					@SuppressWarnings("unchecked")
					Collection<Group> caseHandlersGroups = groupBusiness.getGroupsByGroupName(caseHandlersGroupName);
					Group caseHandlersGroup;
					
					if(caseHandlersGroups == null || caseHandlersGroups.isEmpty()) {

						caseHandlersGroup = groupBusiness.createGroup(caseHandlersGroupName, "Default bpm cases handlers group");
					} else
						caseHandlersGroup = caseHandlersGroups.iterator().next();
					
					int localeId = ICLocaleBusiness.getLocaleId(new Locale("en"));
					caseCategory = casesBusiness.storeCaseCategory(null, null, caseCategoryName, "Default bpm case category", caseHandlersGroup, localeId, -1);
				} else {
					caseCategory = caseCategories.iterator().next();
				}
				
				if(caseTypes == null || caseTypes.isEmpty()) {
			
					caseType = casesBusiness.storeCaseType(null, caseTypeName, "Default bpm case type", -1);
					
				} else {
					caseType = caseTypes.iterator().next();
				}
				
				CaseTypesProcDefBind bind = new CaseTypesProcDefBind();
				bind.setCasesCategoryId(new Long(caseCategory.getPrimaryKey().toString()));
				bind.setCasesTypeId(new Long(caseType.getPrimaryKey().toString()));
				bind.setProcessDefinitionName(pd.getName());
				getCasesBPMDAO().persist(bind);
				
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public String getManagersType() {
		
		return CasesBPMManagersCreator.MANAGERS_TYPE;
	}
	
	protected CasesBusiness getCasesBusiness() {
		
		try {
			FacesContext fctx = FacesContext.getCurrentInstance();
			IWApplicationContext iwac;
			
			if(fctx == null)
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			else
				iwac = IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
			
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected GroupBusiness getGroupBusiness() {
		
		try {
			FacesContext fctx = FacesContext.getCurrentInstance();
			IWApplicationContext iwac;
			
			if(fctx == null)
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			else
				iwac = IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
			
			return (GroupBusiness) IBOLookup.getServiceInstance(iwac, GroupBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}