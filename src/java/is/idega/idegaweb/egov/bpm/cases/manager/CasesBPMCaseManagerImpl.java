package is.idega.idegaweb.egov.bpm.cases.manager;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesListAsset;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.ClosedCases;
import is.idega.idegaweb.egov.cases.presentation.MyCases;
import is.idega.idegaweb.egov.cases.presentation.OpenCases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CaseManager;
import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.core.persistence.Param;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.Link;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/06/02 19:10:24 $ by $Author: civilis $
 */
@Scope("singleton")
@Service(CasesBPMCaseManagerImpl.beanIdentifier)
public class CasesBPMCaseManagerImpl implements CaseManager {

	public static final String PARAMETER_PROCESS_INSTANCE_PK = "pr_inst_pk";
	
	private CasesBPMDAO casesBPMDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private BPMFactory bpmFactory;
	
	static final String beanIdentifier = "casesBPMCaseHandler";
	public static final String caseHandlerType = "CasesBPM";
	
//	private static final String user_assets_page_type = "bpm_user_assets";
//	private static final String handler_assets_page_type = "bpm_handler_assets";
//	private static final String PARAMETER_ACTION = "cbcAct";
//	private static final String ACTION_OPEN_PROCESS = "cbcActOP";

	public String getBeanIdentifier() {
		return beanIdentifier;
	}

	public String getType() {
		return caseHandlerType;
	}
	
	public String getProcessIdentifier(Case theCase) {
		
		Integer caseId = theCase.getPrimaryKey() instanceof Integer ? (Integer)theCase.getPrimaryKey() : new Integer(theCase.getPrimaryKey().toString());
		
		CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByCaseId(caseId);
		
		if(cpi != null) {
		
			Long piId = cpi.getProcInstId();
			
			JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				String identifier = (String)ctx.getProcessInstance(piId).getContextInstance().getVariable(CasesBPMProcessConstants.caseIdentifier);
				return identifier;
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(ctx);
			}
		}
		
		return null;
	}

	public List<Link> getCaseLinks(Case theCase, String casesComponentType) {
		
		throw new UnsupportedOperationException("Implement with correct pages if needed");
		
		/*
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
		*/
		
		/*
		if(true)
			return links;
		
		Link link = new Link(bundle.getImage("images/folder-exec-16x16.png", bundle.getLocalizedString("openBPMProcess", "Open BPM process")));
		
		link.addParameter(PARAMETER_PROCESS_INSTANCE_PK, String.valueOf(theCase.getCaseManagerType()));
		link.addParameter(CasesProcessor.PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		//link.addParameter(CasesProcessor.PARAMETER_ACTION, CasesProcessor.SHOW_CASE_HANDLER);
		link.addParameter(PARAMETER_ACTION, ACTION_OPEN_PROCESS);
		
		links.add(link);
		*/

//		return links;
	}

	public UIComponent getView(IWContext iwc, Case theCase) {
		
		UICasesBPMAssets casesAssets = (UICasesBPMAssets)iwc.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
		UIViewRoot viewRoot = iwc.getViewRoot();
		if (viewRoot != null) {
			casesAssets.setId(viewRoot.createUniqueId());
		}
		
		if (theCase != null) {
			Integer caseId = null;
			try {
				caseId = Integer.valueOf(theCase.getPrimaryKey().toString());
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
			if (caseId != null) {
				casesAssets.setCaseId(caseId);
			}
		}
		
		return casesAssets;
		
		/*
		UICasesListAsset casesList = (UICasesListAsset)iwc.getApplication().createComponent(UICasesListAsset.COMPONENT_TYPE);
		UIViewRoot viewRoot = iwc.getViewRoot();
		if (viewRoot != null) {
			casesList.setId(viewRoot.createUniqueId());
		}
		
		if (theCase != null) {
			Integer caseId = null;
			try {
				caseId = Integer.valueOf(theCase.getPrimaryKey().toString());
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
			if (caseId != null) {
				casesList.setCaseId(caseId);
			}
		}
		
		return casesList;
		*/
	}

	public Collection<? extends Case> getCases(User user, String casesComponentType) {
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		RolesManager rolesManager = getBpmFactory().getRolesManager();
		List<Long> processInstancesIds = rolesManager.getProcessInstancesIdsForCurrentUser();
		
		Collection<GeneralCase> cases;
		
		if(processInstancesIds != null && !processInstancesIds.isEmpty()) {
			
			try {
				List<Integer> casesIds;
				
				if(OpenCases.TYPE.equals(casesComponentType)) {
					
 					casesIds = getCasesBPMDAO().getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded, Integer.class,
							new Param(CaseProcInstBind.procInstIdProp, processInstancesIds)
					);
				
				} else if(ClosedCases.TYPE.equals(casesComponentType)) {
					
					casesIds = getCasesBPMDAO().getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceEnded, Integer.class,
							new Param(CaseProcInstBind.procInstIdProp, processInstancesIds)
					);
					
				} else if(MyCases.TYPE.equals(casesComponentType)) {
					
					casesIds = getCasesBPMDAO().getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIdsAndProcessUserStatus, Integer.class,
							new Param(CaseProcInstBind.procInstIdProp, processInstancesIds),
							new Param(ProcessUserBind.statusProp, ProcessUserBind.Status.PROCESS_WATCHED.toString())
					);
					
				} else
					casesIds = null;
				
				if(casesIds != null && !casesIds.isEmpty()) {
				
					cases = getCasesBusiness(iwc).getGeneralCaseHome().findAllByIds(casesIds);
					
				} else
					cases = null;
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		} else
			cases = null;
		
		return cases == null ? new ArrayList<GeneralCase>(0) : cases;
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

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}