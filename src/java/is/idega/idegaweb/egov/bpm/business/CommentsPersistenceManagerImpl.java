package is.idega.idegaweb.egov.bpm.business;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.webdav.lib.WebdavResource;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.article.business.CommentsPersistenceManager;
import com.idega.block.rss.business.RSSBusiness;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.content.business.ContentConstants;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;
import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;

@Scope("singleton")
@Service(CommentsPersistenceManagerImpl.SPRING_BEAN_IDENTIFIER)
public class CommentsPersistenceManagerImpl implements
        CommentsPersistenceManager {
	
	private static final Logger logger = Logger
	        .getLogger(CommentsPersistenceManagerImpl.class.getName());
	
	public static final String SPRING_BEAN_IDENTIFIER = "bpmCommentsPersistenceManagerImpl";
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	
	private WireFeedOutput wfo = new WireFeedOutput();
	
	public String getLinkToCommentsXML(final String processInstanceIdStr) {
		
		if (StringUtil.isEmpty(processInstanceIdStr)) {
			return null;
		}
		
		final Long processInstanceId = new Long(processInstanceIdStr);
		final String storePath = getBpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				String processName = context.getProcessInstance(
				    processInstanceId).getProcessDefinition().getName();
				
				String storePath = new StringBuilder(JBPMConstants.BPM_PATH)
				        .append(CoreConstants.SLASH).append(processName)
				        .append("/processComments/").append(
				            processInstanceIdStr).append("/comments.xml")
				        .toString();
				
				return storePath;
			}
			
		});
		
		return storePath;
	}
	
	public boolean hasRightsToViewComments(String processInstanceId) {
		if (StringUtil.isEmpty(processInstanceId)) {
			return false;
		}
		
		return hasRightsToViewComments(getConvertedValue(processInstanceId));
	}
	
	public boolean hasRightsToViewComments(Long processInstanceId) {
		
		if (processInstanceId == null) {
			return false;
		}
		
		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId);
		
		return piw.hasRight(Right.processHandler);
	}
	
	private Long getConvertedValue(String value) {
		if (StringUtil.isEmpty(value)) {
			return null;
		}
		
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public String getFeedTitle(IWContext iwc, String processInstanceId) {
		// TODO: return application (BPM process name)?
		return "Comments of process";
	}
	
	public String getFeedSubtitle(IWContext iwc, String processInstanceId) {
		// TODO: return more precise explanation
		return "All process comments";
	}
	
	public Feed getCommentsFeed(IWContext iwc, String processInstanceId) {
		if (iwc == null) {
			return null;
		}
		String uri = getLinkToCommentsXML(processInstanceId);
		if (StringUtil.isEmpty(uri)) {
			return null;
		}
		
		IWSlideService slide = getRepository();
		if (slide == null) {
			return null;
		}
		WebdavResource commentsResource = null;
		try {
			commentsResource = slide.getWebdavResourceAuthenticatedAsRoot(uri);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (commentsResource == null || !commentsResource.getExistence()) {
			return null;
		}
		
		RSSBusiness rss = getRSSBusiness();
		if (rss == null) {
			return null;
		}
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch (NotLoggedOnException e) {
			logger.log(Level.SEVERE, "User must be logged!", e);
		}
		if (currentUser == null) {
			return null;
		}
		
		String pathToFeed = null;
		try {
			pathToFeed = new StringBuilder(slide.getWebdavServerURL().getURI())
			        .append(uri).toString();
		} catch (URIException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (StringUtil.isEmpty(pathToFeed)) {
			logger.log(Level.SEVERE, "Error creating path to comments feed!");
			return null;
		}
		SyndFeed comments = rss.getFeedAuthenticatedByUser(pathToFeed,
		    currentUser);
		if (comments == null) {
			logger
			        .log(Level.WARNING,
			            "Unable to get comments feed by current user, trying with admin user");
			comments = rss.getFeedAuthenticatedByAdmin(pathToFeed);
		}
		if (comments == null) {
			logger.log(Level.SEVERE, "Error getting comments feed!");
			return null;
		}
		
		WireFeed wireFeed = comments.createWireFeed();
		if (wireFeed instanceof Feed) {
			return (Feed) wireFeed;
		}
		return null;
	}
	
	public boolean storeFeed(String processInstanceId, Feed comments) {
		if (comments == null) {
			return false;
		}
		String url = getLinkToCommentsXML(processInstanceId);
		if (StringUtil.isEmpty(url)) {
			return false;
		}
		
		IWSlideService service = getRepository();
		if (service == null) {
			return false;
		}
		
		String commentsContent = null;
		try {
			commentsContent = wfo.outputString(comments);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (FeedException e) {
			e.printStackTrace();
			return false;
		}
		
		String fileBase = url;
		String fileName = url;
		int index = url.lastIndexOf(ContentConstants.SLASH);
		if (index != -1) {
			fileBase = url.substring(0, index + 1);
			fileName = url.substring(index + 1);
		}
		try {
			return service
			        .uploadFileAndCreateFoldersFromStringAsRoot(fileBase,
			            fileName, commentsContent,
			            ContentConstants.XML_MIME_TYPE, true);
		} catch (RemoteException e) {
			logger.log(Level.SEVERE, "Error storing comments feed", e);
		}
		
		return false;
	}
	
	private IWSlideService getRepository() {
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(
			    IWMainApplication.getDefaultIWApplicationContext(),
			    IWSlideService.class);
		} catch (IBOLookupException e) {
			logger.log(Level.SEVERE, "Error getting repository", e);
		}
		return null;
	}
	
	private RSSBusiness getRSSBusiness() {
		try {
			return (RSSBusiness) IBOLookup.getServiceInstance(IWMainApplication
			        .getDefaultIWApplicationContext(), RSSBusiness.class);
		} catch (IBOLookupException e) {
			logger.log(Level.SEVERE, "Error getting RSSBusiness", e);
		}
		return null;
	}
	
	// TODO: When access rights (to Slide resources) are fixed, use current user!
	public User getUserAvailableToReadWriteCommentsFeed(IWContext iwc) {
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch (NotLoggedOnException e) {
			e.printStackTrace();
		}
		if (currentUser == null) {
			return null;
		}
		
		AccessController accessController = iwc.getAccessController();
		try {
			if (iwc.isSuperAdmin()) {
				return currentUser;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			return accessController.getAdministratorUser();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	BPMContext getBpmContext() {
		return bpmContext;
	}
	
}
