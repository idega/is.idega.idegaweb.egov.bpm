package is.idega.idegaweb.egov.bpm.cases.messages;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.data.Case;
import com.idega.block.process.presentation.CaseBlock;
import com.idega.block.process.presentation.UserCases;
import com.idega.builder.business.BuilderLogic;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.builder.data.ICPage;
import com.idega.core.component.business.ICObjectBusiness;
import com.idega.core.component.data.ICObjectInstance;
import com.idega.core.component.data.ICObjectInstanceHome;
import com.idega.core.contact.data.Email;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWConstants;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.rights.Right;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIParam;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.presentation.ClosedCases;
import is.idega.idegaweb.egov.cases.presentation.HandlerCases;
import is.idega.idegaweb.egov.cases.presentation.MyCases;
import is.idega.idegaweb.egov.cases.presentation.OpenCases;
import is.idega.idegaweb.egov.cases.presentation.PublicCases;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 *          Last modified: $Date: 2009/01/22 17:29:22 $ by $Author: anton $
 */
public class CaseUserImpl {

	public static final String CHECK_CASE_STATUS = "bpm.check_case_status_for_url";
	public static final Logger LOGGER = Logger.getLogger(CaseUserImpl.class.getName());

	private User user;
	private ProcessInstanceW processInstanceW;
	private BPMUserFactory bpmUserFactory;
	private BPMFactory bpmFactory;
	@Autowired private CasesBPMDAO casesBPMDAO = null;
	private ICObjectInstanceHome icObjectInstanceHome = null;
	private CasesBusiness casesBusiness = null;
	private UserBusiness userBusiness = null;
	private BuilderLogic builder = null;
	private AccessController accessController = null;

	public CaseUserImpl(User user, ProcessInstanceW processInstanceW) {
		this.user = user;
		this.processInstanceW = processInstanceW;
	}

	protected CasesBPMDAO getCasesDAO() {
		if (this.casesBPMDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return casesBPMDAO;
	}

	protected ICObjectInstanceHome getICObjectInstanceHome() {
		if (this.icObjectInstanceHome != null) {
			return this.icObjectInstanceHome;
		}

		try {
			this.icObjectInstanceHome = (ICObjectInstanceHome) IDOLookup.getHome(ICObjectInstance.class);
		} catch (IDOLookupException e) {
			LOGGER.log(Level.WARNING, "Unable to get " + ICObjectInstanceHome.class.getName() + " cause of: ", e);
		}

		return this.icObjectInstanceHome;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		if (this.casesBusiness != null) {
			return this.casesBusiness;
		}

		try {
			this.casesBusiness = IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException ile) {
			LOGGER.log(Level.WARNING, "Unable to get " + CasesBusiness.class.getName() + " cause of: ", ile);
		}

		return this.casesBusiness;
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		if (this.userBusiness != null) {
			return this.userBusiness;
		}

		try {
			this.userBusiness = IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}

		return this.userBusiness;
	}

	protected BuilderLogic getBuilderLogic() {
		if (this.builder == null) {
			this.builder = BuilderLogic.getInstance();
		}

		return this.builder;
	}

	protected AccessController getAccessController(IWApplicationContext iwac) {
		if (this.accessController == null && iwac != null) {
			this.accessController = iwac.getIWMainApplication().getAccessController();
		}

		return this.accessController;
	}

	public User getUser() {
		return user;
	}

	/**
	 *
	 * if the user has login: if the user is handler -> link to opencases else
	 * -> link to usercases else link to case view using this user as bpm user
	 * (see participant invitation)
	 *
	 * @return
	 */
	public String getUrlToTheCase() {
		String fullUrl = null;
		final IWApplicationContext iwac = IWMainApplication.getDefaultIWApplicationContext();
		try {
			UserBusiness userBusiness = getUserBusiness(iwac);
			User user = getUser();

			boolean userHasLogin;

			boolean checkCaseStatus = iwac.getApplicationSettings().getBoolean(CHECK_CASE_STATUS, Boolean.FALSE);

			try {
				userHasLogin = userBusiness.hasUserLogin(user);
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}

			if (userHasLogin) {
				if (getProcessInstanceW().hasRight(Right.processHandler, user)) {
					//	Handler, get link to open cases
					if (checkCaseStatus) {
						fullUrl = getOpenCasesUrl(iwac, user, getProcessInstanceW().getProcessInstanceId());
					} else {
						fullUrl = getOpenCasesUrl(iwac, user);
					}

				} else {
					//	Not handler, get link to user cases
					fullUrl = getUserCasesUrl(iwac, user);
				}

				final URIUtil uriUtil = new URIUtil(fullUrl);
				uriUtil.setParameter(BPMUser.processInstanceIdParam, String.valueOf(getProcessInstanceW().getProcessInstanceId()));
				fullUrl = uriUtil.getUri();
			} else {
				//	Hasn't login, get link to case view
				UserPersonalData upd = new UserPersonalData();

				try {
					Email email = user.getUsersEmail();
					if (email != null) {
						upd.setUserEmail(email.getEmailAddress());
					}
				} catch (RemoteException e) {
					LOGGER.log(Level.WARNING, "Exception while resolving user ("+user.getPrimaryKey()+") email");
				}

	            upd.setFullName(user.getName());
	            upd.setUserType(BPMUser.USER_TYPE_NATURAL);
	            upd.setPersonalId(user.getPersonalID());

	            List<Role> roles = getBpmFactory().getRolesManager().getUserRoles(getProcessInstanceW().getProcessInstanceId(), user);

	            if (roles != null && !roles.isEmpty()) {
	            	//	Here we could find the most permissive role of the user, or add all roles for the bpm user (better way)
	            	Role role = roles.iterator().next();

					BPMUser bpmUser = getBpmUserFactory().createBPMUser(upd, role, getProcessInstanceW().getProcessInstanceId());

					fullUrl = getAssetsUrl(iwac, user);

					final URIUtil uriUtil = new URIUtil(fullUrl);
					List<URIParam> params = bpmUser.getParamsForBPMUserLink();

					for (URIParam param : params) {
		                uriUtil.setParameter(param.getParamName(), param.getParamValue());
	                }

					fullUrl = uriUtil.getUri();
	            } else {
	            	fullUrl = null;
	            	LOGGER.log(Level.WARNING, "No roles resolved for the user with id="+user.getPrimaryKey()+" and process instance id = "+
	            	getProcessInstanceW().getProcessInstanceId() + ", skipping creating url to the case");
	            }
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting url", e);
		}

		if (StringUtil.isEmpty(fullUrl)) {
			fullUrl = getDefaultUrl(iwac);
		}

		return fullUrl;
	}

	protected List<String> getStatusesToShow(BuilderLogic builder, String pageKey, String instanceId) {
		String show = getProperty(builder, pageKey, instanceId, ":method:1:implied:void:setCaseStatusesToShow:java.lang.String:");
		return StringUtil.isEmpty(show) ? null : new ArrayList<String>(Arrays.asList(show.split(CoreConstants.COMMA)));
	}

	protected String getProperty(BuilderLogic builder, String pageKey, String instanceId, String property) {
		return builder.getProperty(pageKey, instanceId, property);
	}

	/**
	 *
	 * @param instance is component, which link should be found;
	 * @param iwac is application context, where to search for links, not
	 * <code>null</code>;
	 * @param user, who should have access rights to required link, not
	 * <code>null</code>;
	 * @return {@link URL} of page by {@link ICObjectInstance}, accessible
	 * for given {@link User} or <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected String getCaseURL(ICObjectInstance instance, IWApplicationContext iwac, User user) {
		if (instance == null || iwac == null || user == null)
			return null;

		String uniqueId = instance.getUniqueId();
		if (StringUtil.isEmpty(uniqueId))
			return null;

		String instanceId = ICObjectBusiness.UUID_PREFIX.concat(uniqueId);

		ICPage page = getBuilderLogic().findPageForModule(iwac, instanceId);
		if (page == null || page.getDeleted())
			return null;

		if (!getAccessController(iwac).hasViewPermissionForPageKey(page.getPageKey(), CoreUtil.getIWContext())) {
			return null;
		}

		String pageKey = page.getId();

		List<String> statusesToShow = getStatusesToShow(getBuilderLogic(), pageKey, instanceId);
		if (ListUtil.isEmpty(statusesToShow)) {
			return null;
		}

		Case theCase = getCase(getProcessInstanceW().getProcessInstanceId());
		if (theCase == null || !statusesToShow.contains(theCase.getStatus())) {
			return null;
		}

		String uri = page.getDefaultPageURI();
		if (!uri.startsWith(CoreConstants.PAGES_URI_PREFIX))
			uri = CoreConstants.PAGES_URI_PREFIX + uri;

		URIUtil uriUtil = new URIUtil(uri);
		uriUtil.setParameter(
				LoginBusinessBean.PARAM_LOGIN_BY_UNIQUE_ID,
				user.getUniqueId()
		);
    	uriUtil.setParameter(
    			LoginBusinessBean.LoginStateParameter,
    			LoginBusinessBean.LOGIN_EVENT_LOGIN
    	);

    	return iwac.getIWMainApplication().getTranslatedURIWithContext(uriUtil.getUri());
	}

	/**
	 *
	 * <p>Searches for links to visual components, where required
	 * {@link ProcessInstance} can be viewed.</p>
	 * @param classType is type of cases should be viewed, for example:
	 * <li>OpenCases</li>
	 * <li>ClosedCases</li>
	 * <li>MyCases</li>
	 * <li>PublicCases</li>
	 * <li>UserCases</li>
	 * <li>HandlerCases</li>
	 * @param iwac is application context, where to search for links, not
	 * <code>null</code>;
	 * @param user, who should be available to see the {@link ProcessInstance},
	 * not <code>null</code>;
	 * @param processInstanceId is id of {@link ProcessInstance}, which should
	 * be viewed, not <code>null</code>;
	 * @return {@link List} of accessible links to {@link User} or
	 * {@link Collections#emptyList()};
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public ArrayList<String> getCasesURLs(
			Class<? extends CaseBlock> classType,
			IWApplicationContext iwac,
			User user,
			Long processInstanceId
	) {
		if (classType == null || iwac == null || user == null || processInstanceId == null) {
			return null;
		}

		Collection<ICObjectInstance> instances = null;
		try {
			instances = getICObjectInstanceHome().getByClassName(classType);
		} catch (FinderException e) {
			LOGGER.log(Level.WARNING, "Unable to find " + classType + " component", e);
		}

		if (ListUtil.isEmpty(instances)) {
			return null;
		}

		ArrayList<String> urls = new ArrayList<String>(instances.size());
		for (ICObjectInstance instance: instances) {
			String url = getCaseURL(instance, iwac, user);
			if (!StringUtil.isEmpty(url)) {
				urls.add(url);
			}
		}

		return urls;
	}

	/**
	 *
	 * <p>Searches for links to visual components, where required
	 * {@link ProcessInstance} can be viewed. Searches links for:
	 * <li>OpenCases</li>
	 * <li>ClosedCases</li>
	 * <li>MyCases</li>
	 * <li>PublicCases</li>
	 * <li>UserCases</li>
	 * <li>HandlerCases</li></p>
	 * @param iwac is application context, where to search for links, not
	 * <code>null</code>;
	 * @param user, who should be available to see the {@link ProcessInstance},
	 * not <code>null</code>;
	 * @param processInstanceId is id of {@link ProcessInstance}, which should
	 * be viewed, not <code>null</code>;
	 * @return {@link List} of accessible links to {@link User} or
	 * {@link Collections#emptyList()};
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public ArrayList<String> getCasesURLs(
			IWApplicationContext iwac,
			User user,
			Long processInstanceId
	) {
		if (iwac == null || user == null || processInstanceId == null) {
			return new ArrayList<String>(0);
		}

		List<Class<? extends CaseBlock>> classes = new ArrayList<Class<? extends CaseBlock>>();
		classes.add(OpenCases.class);
		classes.add(ClosedCases.class);
		classes.add(MyCases.class);
		classes.add(PublicCases.class);
		classes.add(UserCases.class);
		classes.add(HandlerCases.class);

		ArrayList<String> urls = new ArrayList<String>();
		for (Class<? extends CaseBlock> theClass : classes) {
			ArrayList<String> urlsForClass = getCasesURLs(theClass, iwac, user, processInstanceId);
			if (ListUtil.isEmpty(urlsForClass)) {
				continue;
			}

			urls.addAll(urlsForClass);
		}

		return urls;
	}

	/**
	 *
	 * <p>Searches {@link OpenCases} page in pages tree of application,
	 * accessible for current {@link User}.</p>
	 * @param iwac is current application context, where to search;
	 * @param user, who should be able to access page, not <code>null</code>;
	 * @param processInstanceId is id of {@link ProcessInstance}, which should
	 * be accessed;
	 * @return url to {@link OpenCases} page or <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	protected String getOpenCasesUrl(IWApplicationContext iwac, User user, Long processInstanceId) {
		if (user == null) {
			return null;
		}

		List<String> urls = getCasesURLs(OpenCases.class, iwac, user, processInstanceId);
		if (!ListUtil.isEmpty(urls)) {
			return urls.iterator().next();
		}

		return getUrl(iwac, user, OpenCases.pageType);
	}

	/**
	 *
	 * <p>Searches for {@link Case} by {@link ProcessInstance}.</p>
	 * @param processInstanceId is {@link ProcessInstance#getId()} of
	 * {@link Case} required, not <code>null</code>;
	 * @return {@link Case} by {@link ProcessInstance} or <code>null</code>
	 * on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	protected Case getCase(Long processInstanceId) {
		CaseProcInstBind bind = null;
		try {
			bind = getCasesDAO().getCaseProcInstBindByProcessInstanceId(processInstanceId);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting case for process instance: " + processInstanceId);
		}

		if (bind == null) {
			return null;
		}

		try {
			return getCasesBusiness(IWMainApplication.getDefaultIWApplicationContext()).getCase(bind.getCaseId());
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting case by id: " + bind.getCaseId(), e);
		}

		return null;
	}

	public ProcessInstanceW getProcessInstanceW() {
		return processInstanceW;
	}

	private String getUrl(IWApplicationContext iwac, User user, String pageType) {
		String url = null;
		try {
			url = getBuilderService(iwac).getFullPageUrlByPageType(user, pageType, true);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting page by type " + pageType + " for " + user, e);
		}

		if (StringUtil.isEmpty(url)) {
			url = getDefaultUrl(iwac);
			LOGGER.warning("Using default (" + url + ") page instead of page with type: " + pageType + " for " + user + " because failed to find page with type " + pageType);
		}

		return url;
	}

	private String getDefaultUrl(IWApplicationContext iwac) {
		String url = CoreConstants.PAGES_URI_PREFIX;

		IWContext iwc = CoreUtil.getIWContext();
		String server = iwc == null ?
						iwac.getIWMainApplication().getSettings().getProperty(IWConstants.SERVER_URL_PROPERTY_NAME) :
						iwc.getServerURL();
		if (server == null) {
			LOGGER.warning("Unknown server");
		} else {
			if (server.endsWith(CoreConstants.SLASH) && url.startsWith(CoreConstants.SLASH)) {
				url = url.substring(1);
			}
			url = server.concat(url);
		}
		return url;
	}

	private String getAssetsUrl(IWApplicationContext iwac, User currentUser) {
		return getUrl(iwac, currentUser, BPMUser.defaultAssetsViewPageType);
	}
	private String getOpenCasesUrl(IWApplicationContext iwac, User user) {
		return getUrl(iwac, user, OpenCases.pageType);
	}
	private String getUserCasesUrl(IWApplicationContext iwac, User currentUser) {
		return getUrl(iwac, currentUser, UserCases.pageType);
	}

	private BuilderService getBuilderService(IWApplicationContext iwc) {
		try {
			return BuilderServiceFactory.getBuilderService(iwc);
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		}
	}

	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}

	BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

}