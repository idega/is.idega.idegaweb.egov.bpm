package is.idega.idegaweb.egov.bpm.application;

import is.idega.idegaweb.egov.application.business.ApplicationType;
import is.idega.idegaweb.egov.application.data.Application;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.NativeRolesPermissionsHandler;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.presentation.BPMTaskViewer;
import com.idega.presentation.IWContext;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.user.data.User;
import com.idega.util.URIUtil;

/**
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $
 * 
 *          Last modified: $Date: 2008/12/28 11:58:48 $ by $Author: civilis $
 * 
 */
@Scope("singleton")
@Service(ApplicationTypeBPM.beanIdentifier)
public class ApplicationTypeBPM implements ApplicationType {

	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private PermissionsFactory permissionsFactory;

	static final String beanIdentifier = "appTypeBPM";
	private static final String appType = "EGOV_BPM";
	private static final String egovBPMPageType = "bpm_app_starter";

	public ApplicationTypeHandlerComponent getHandlerComponent() {
		UIApplicationTypeBPMHandler h = new UIApplicationTypeBPMHandler();
		return h;
	}

	public String getLabel(IWContext iwc) {
		return "EGOV BPM";
	}

	public String getType() {
		return appType;
	}

	public String getBeanIdentifier() {
		return beanIdentifier;
	}

	public void beforeStore(IWContext iwc, Application app) {

		String procDef = iwc
				.getParameter(UIApplicationTypeBPMHandler.MENU_PARAM);

		try {
			Long pdId = new Long(procDef);
			String processName = getBpmFactory().getProcessManager(pdId)
					.getProcessDefinition(pdId).getProcessDefinition()
					.getName();
			app.setUrl(processName);

		} catch (Exception exp) {
			iwc.addMessage(null, new FacesMessage("Exception:"
					+ exp.getMessage()));
		}

		app.setElectronic(true);
	}

	public boolean afterStore(IWContext iwc, Application app) {

		String procDef = iwc
				.getParameter(UIApplicationTypeBPMHandler.MENU_PARAM);

		try {
			Long pdId = new Long(procDef);

			ProcessDefinitionW pdw = getBpmFactory().getProcessManager(pdId)
					.getProcessDefinition(pdId);

			if (iwc
					.isParameterSet(UIApplicationTypeBPMHandler.rolesToStartCaseNeedToBeCheckedParam)
					&& iwc
							.getParameterValues(UIApplicationTypeBPMHandler.rolesToStartCaseParam) != null
					&& iwc
							.getParameterValues(UIApplicationTypeBPMHandler.rolesToStartCaseParam).length != 0) {

				// setting roles, that can start process
				List<String> vals = Arrays
						.asList(iwc
								.getParameterValues(UIApplicationTypeBPMHandler.rolesToStartCaseParam));
				pdw.setRolesCanStartProcess(vals, app.getPrimaryKey());

			} else {

				pdw.setRolesCanStartProcess(null, app.getPrimaryKey());
			}

		} catch (Exception exp) {
			iwc.addMessage(null, new FacesMessage("Exception:"
					+ exp.getMessage()));
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", exp);
		}

		return false;
	}

	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public void fillMenu(DropdownMenu menu) {

		List<CaseTypesProcDefBind> casesProcesses = getCasesBPMDAO()
				.getAllCaseTypes();

		BPMDAO bpmDAO = getBpmFactory().getBPMDAO();

		for (CaseTypesProcDefBind caseTypesProcDefBind : casesProcesses) {

			ProcessDefinition pd = bpmDAO
					.findLatestProcessDefinition(caseTypesProcDefBind
							.getProcessDefinitionName());
			menu.addMenuElement(String.valueOf(pd.getId()), pd.getName());
		}
	}

	public String getSelectedElement(Application app) {

		final String pdName = app.getUrl();

		if (pdName != null) {

			long latestPDId = getBpmFactory().getBPMDAO()
					.findLatestProcessDefinition(pdName).getId();
			return String.valueOf(latestPDId);
		}

		return "-1";
	}

	public List<String> getRolesCanStartProcessDWR(Long pdId,
			String applicationId) {

		return getRolesCanStartProcess(pdId, applicationId);
	}

	public List<String> getRolesCanStartProcess(Long pdId, Object applicationId) {

		return getBpmFactory().getProcessManager(pdId).getProcessDefinition(
				pdId).getRolesCanStartProcess(applicationId);
	}

	protected BuilderService getBuilderService(IWApplicationContext iwac) {

		try {
			return BuilderServiceFactory.getBuilderService(iwac);
		} catch (RemoteException e) {
			throw new RuntimeException("Failed to resolve builder service", e);
		}
	}

	public String getUrl(IWContext iwc, Application app) {

		String pdName = app.getUrl();

		if (pdName == null)
			return "#";

		/*
		 * Collection<ICPage> icpages = getPages(egovBPMPageType);
		 * 
		 * ICPage icPage = null;
		 * 
		 * if(icpages == null || icpages.isEmpty()) {
		 * 
		 * // TODO: create egov bpm page, as not found throw new
		 * RuntimeException("No egov bpm page found yet"); }
		 * 
		 * if(icPage == null) icPage = icpages.iterator().next();
		 * 
		 * String uri = icPage.getDefaultPageURI();
		 * 
		 * if(!uri.startsWith("/pages")) uri = "/pages"+uri;
		 */

		String uri = getBuilderService(iwc).getFullPageUrlByPageType(iwc,
				egovBPMPageType, true);

		long pdId = getBpmFactory().getBPMDAO().findLatestProcessDefinition(
				pdName).getId();

		URIUtil uriUtil = new URIUtil(uri);
		uriUtil.setParameter(BPMTaskViewer.PROCESS_DEFINITION_PROPERTY, String
				.valueOf(pdId));
		uri = uriUtil.getUri();
		return iwc.getIWMainApplication().getTranslatedURIWithContext(uri);
	}

	public Collection<ICPage> getPages(String pageSubType) {

		try {

			ICPageHome home = (ICPageHome) IDOLookup.getHome(ICPage.class);
			@SuppressWarnings("unchecked")
			Collection<ICPage> icpages = home.findBySubType(pageSubType, false);

			return icpages;

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while resolving icpages by subType: "
							+ pageSubType, e);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext idegaJbpmContext) {
		this.bpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	/**
	 * checks, if the application is visible for current user This is
	 * implemented only for applications category list. In other words, if the
	 * link of the start form is given to anyone, then that user will be able to
	 * open the form (and submit)
	 */
	public boolean isVisible(Application app) {

		final Long pdId = new Long(getSelectedElement(app));

		final ProcessDefinitionW pdw = getBpmFactory().getProcessManager(pdId)
				.getProcessDefinition(pdId);

		final List<String> rolesCanStart = pdw.getRolesCanStartProcess(app
				.getPrimaryKey());

		if (rolesCanStart != null && !rolesCanStart.isEmpty()) {

			IWContext iwc = IWContext.getCurrentInstance();

			if (iwc != null && iwc.isLoggedOn()) {

				if (iwc.isSuperAdmin())
					return true;

				User usr = iwc.getCurrentUser();

				BPMTypedPermission permission = getPermissionsFactory()
						.getTypedPermission(
								NativeRolesPermissionsHandler.handlerType);
				permission.setAttribute(NativeRolesPermissionsHandler.userAtt,
						usr);
				permission.setAttribute(NativeRolesPermissionsHandler.rolesAtt,
						rolesCanStart);

				try {
					getBpmFactory().getRolesManager().checkPermission(
							(Permission) permission);
					return true;

				} catch (AccessControlException e) {
					return false;
				}
			}

			return false;

		} else {

			return true;
		}
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}
}