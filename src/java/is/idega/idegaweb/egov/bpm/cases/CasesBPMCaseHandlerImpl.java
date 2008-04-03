package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;
import is.idega.idegaweb.egov.cases.presentation.ClosedCases;
import is.idega.idegaweb.egov.cases.presentation.MyCases;
import is.idega.idegaweb.egov.cases.presentation.OpenCases;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.idega.block.process.business.CaseManager;
import com.idega.block.process.business.CaseManagerPluggedInEvent;
import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind.Status;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.Link;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 * Last modified: $Date: 2008/04/03 13:37:22 $ by $Author: civilis $
 */
public class CasesBPMCaseHandlerImpl implements CaseManager, ApplicationContextAware, ApplicationListener {

	public static final String PARAMETER_PROCESS_INSTANCE_PK = "pr_inst_pk";
	
	private ApplicationContext ctx;
	private CasesBPMDAO casesBPMDAO;
	private static final String beanIdentifier = "casesBPMCaseHandler";
	public static final String caseHandlerType = "CasesBPM";
	
	private static final String user_assets_page_type = "bpm_user_assets";
	private static final String handler_assets_page_type = "bpm_handler_assets";
	private static final String PARAMETER_ACTION = "cbcAct";
	private static final String ACTION_OPEN_PROCESS = "cbcActOP";
	
	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ctx = applicationcontext;
	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {
		
		if(applicationevent instanceof ContextRefreshedEvent) {
			
			//publish xforms factory registration
			ctx.publishEvent(new CaseManagerPluggedInEvent(this));
		}
	}

	public String getBeanIdentifier() {
		return beanIdentifier;
	}

	public String getType() {
		return caseHandlerType;
	}

	public List<Link> getCaseLinks(Case theCase, String casesComponentType) {
		
		FacesContext ctx = FacesContext.getCurrentInstance();
		IWContext iwc = IWContext.getIWContext(ctx);
		IWMainApplication iwma = IWMainApplication.getIWMainApplication(ctx);
		IWBundle bundle = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		List<Link> links = new ArrayList<Link>();
		
//		tmp solution
		String pageType;
		
		if("temp_usercases".equals(casesComponentType)) {
			
			pageType = user_assets_page_type;
		} else
			pageType = handler_assets_page_type;
		
		String pageUri = getPageUri(iwc, pageType);
		
		Link link2 = new Link(bundle.getImage("images/folder-exec-16x16.png", bundle.getLocalizedString("openBPMProcess", "Open BPM process")), pageUri);
		link2.addParameter(CasesProcessor.PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		
		links.add(link2);
		
		if(true)
			return links;
		
		Link link = new Link(bundle.getImage("images/folder-exec-16x16.png", bundle.getLocalizedString("openBPMProcess", "Open BPM process")));
		
		link.addParameter(PARAMETER_PROCESS_INSTANCE_PK, String.valueOf(theCase.getCaseManagerType()));
		link.addParameter(CasesProcessor.PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		link.addParameter(CasesProcessor.PARAMETER_ACTION, CasesProcessor.SHOW_CASE_HANDLER);
		link.addParameter(PARAMETER_ACTION, ACTION_OPEN_PROCESS);
		
		links.add(link);

		return links;
	}
	
	

	public UIComponent getView(IWContext iwc, Case theCase) {
		
		FacesContext context = FacesContext.getCurrentInstance();
		UICasesBPMAssets assets = (UICasesBPMAssets)context.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
		assets.setId(context.getViewRoot().createUniqueId());
		return assets;
	}

	public Collection<? extends Case> getCases(User user, String casesComponentType) {
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		try {
			CasesBusiness casesBusiness = getCasesBusiness(iwc);
			UserBusiness userBusiness = getUserBusiness(iwc);
			Collection<GeneralCase> cases;
			
			if(OpenCases.TYPE.equals(casesComponentType)) {
				
				cases = OpenCases.getOpenCases(user, iwc.getIWMainApplication(), iwc, userBusiness, casesBusiness, new String[] {getType()});
			
			} else if(ClosedCases.TYPE.equals(casesComponentType)) {
				
				cases = ClosedCases.getClosedCases(user, iwc.getIWMainApplication(), iwc, userBusiness, casesBusiness, new String[] {getType()});
				
			} else if(MyCases.TYPE.equals(casesComponentType)) {
				
				cases = OpenCases.getOpenCases(user, iwc.getIWMainApplication(), iwc, userBusiness, casesBusiness, new String[] {getType()});
				
				HashMap<Integer, GeneralCase> casesNPKs = new HashMap<Integer, GeneralCase>(cases.size());
				
				for (GeneralCase caze : cases) {
					
					casesNPKs.put(new Integer(caze.getPrimaryKey().toString()), caze);
				}
				
				List<ProcessUserBind> binds = getCasesBPMDAO().getProcessUserBinds(new Integer(user.getPrimaryKey().toString()), casesNPKs.keySet());
				List<GeneralCase> casesToReturn = new ArrayList<GeneralCase>(binds.size());
				
				for (ProcessUserBind processUserBind : binds) {

					if(Status.PROCESS_WATCHED == processUserBind.getStatus()) {
						
						if(casesNPKs.containsKey(processUserBind.getCaseProcessBind().getCaseId())) {
							
							casesToReturn.add(casesNPKs.get(processUserBind.getCaseProcessBind().getCaseId()));
						}
					}
				}
				
				cases = casesToReturn;
				
			} else
				cases = null;
			
			return cases;
			
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
	
	public CasesBusiness getCasesBusiness(IWContext iwc) {
		
		try {
			return (CasesBusiness)IBOLookup.getServiceInstance(iwc, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public UserBusiness getUserBusiness(IWContext iwc) {
		
		try {
			return (UserBusiness)IBOLookup.getServiceInstance(iwc, UserBusiness.class);
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
	
	/*
	protected String composeFullUrl(IWContext iwc, Token token) {
		
		String serverURL = iwc.getServerURL();
		String pageUri = getPageUri(iwc);
		
		final URIUtil uriUtil = new URIUtil(pageUri);
		uriUtil.setParameter(tokenParam, String.valueOf(token.getId()));
		pageUri = uriUtil.getUri();
		
		serverURL = serverURL.endsWith(CoreConstants.SLASH) ? serverURL.substring(0, serverURL.length()-1) : serverURL;
		
		String fullURL = new StringBuilder(serverURL)
		.append(pageUri.startsWith(CoreConstants.SLASH) ? CoreConstants.EMPTY : CoreConstants.SLASH)
		.append(pageUri)
		.toString();
		
		return fullURL;
	}
	*/
	
	private final HashMap<String, String> uris = new HashMap<String, String>(2);
	
	protected synchronized String getPageUri(IWApplicationContext iwac, String pageType) {
		
		if(uris.containsKey(pageType))
			return uris.get(pageType);
		
		Collection<ICPage> icpages = getPages(pageType);
		
		ICPage icPage = null;
		
		if(icpages == null || icpages.isEmpty()) {
			
//			TODO: create egov bpm page, as not found
			throw new RuntimeException("No page found by page type: "+pageType);			
		}
		
		if(icPage == null)
			icPage = icpages.iterator().next();
		
		String uri = icPage.getDefaultPageURI();
		
		if(!uri.startsWith("/pages"))
			uri = "/pages"+uri;
		
		String ruri = iwac.getIWMainApplication().getTranslatedURIWithContext(uri);
		uris.put(pageType, ruri);
		
		return ruri;
	}
	
	protected Collection<ICPage> getPages(String pageSubType) {
		
		try {
		
			ICPageHome home = (ICPageHome) IDOLookup.getHome(ICPage.class);
			@SuppressWarnings("unchecked")
			Collection<ICPage> icpages = home.findBySubType(pageSubType, false);
			
			return icpages;
			
		} catch (Exception e) {
			throw new RuntimeException("Exception while resolving icpages by subType: "+pageSubType, e);
		}
	}
}