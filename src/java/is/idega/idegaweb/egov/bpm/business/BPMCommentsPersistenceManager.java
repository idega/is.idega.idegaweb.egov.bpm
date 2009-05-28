package is.idega.idegaweb.egov.bpm.business;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.article.bean.CommentEntry;
import com.idega.block.article.bean.CommentsViewerProperties;
import com.idega.block.article.business.CommentsPersistenceManager;
import com.idega.block.article.business.DefaultCommentsPersistenceManager;
import com.idega.block.article.data.Comment;
import com.idega.block.article.data.CommentHome;
import com.idega.block.rss.business.RSSBusiness;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.content.business.ContentConstants;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.data.IDOLookup;
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
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(BPMCommentsPersistenceManager.SPRING_BEAN_IDENTIFIER)
public class BPMCommentsPersistenceManager extends DefaultCommentsPersistenceManager implements CommentsPersistenceManager {
	
	private static final Logger logger = Logger.getLogger(BPMCommentsPersistenceManager.class.getName());
	
	public static final String SPRING_BEAN_IDENTIFIER = "bpmCommentsPersistenceManagerImpl";
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	
	private WireFeedOutput wfo = new WireFeedOutput();
	
	@Override
	public String getLinkToCommentsXML(final String processInstanceIdStr) {
		
		if (StringUtil.isEmpty(processInstanceIdStr)) {
			return null;
		}
		
		final Long processInstanceId = new Long(processInstanceIdStr);
		final String storePath = getBpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				String processName = context.getProcessInstance(processInstanceId).getProcessDefinition().getName();
				String storePath = new StringBuilder(JBPMConstants.BPM_PATH).append(CoreConstants.SLASH).append(processName).append("/processComments/")
					.append(processInstanceIdStr).append("/comments.xml").toString();
				
				return storePath;
			}
			
		});
		
		return storePath;
	}
	
	@Override
	public boolean hasRightsToViewComments(String processInstanceId) {
		return hasRightsToViewComments(getConvertedValue(processInstanceId));
	}
	
	@Override
	public boolean hasRightsToViewComments(Long processInstanceId) {
		if (processInstanceId == null) {
			return false;
		}
		
		if (hasFullRightsForComments(processInstanceId)) {
			return true;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc != null && iwc.isLoggedOn()) {
			return hasRightToViewComments(processInstanceId, iwc.getCurrentUser());
		}
		
		return  false;
	}
	
	private boolean hasRightToViewComments(Long processInstanceId, User user) {
		return getBpmFactory().getRolesManager().canSeeComments(processInstanceId, user);
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
	
	@Override
	public String getFeedTitle(IWContext iwc, String processInstanceId) {
		// TODO: return application (BPM process name)?
		return "Comments of process";
	}
	
	@Override
	public String getFeedSubtitle(IWContext iwc, String processInstanceId) {
		// TODO: return more precise explanation
		return "All process comments";
	}
	
	@Override
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
			pathToFeed = new StringBuilder(slide.getWebdavServerURL().getURI()).append(uri).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (StringUtil.isEmpty(pathToFeed)) {
			logger.log(Level.SEVERE, "Error creating path to comments feed!");
			return null;
		}
		SyndFeed comments = rss.getFeedAuthenticatedByUser(pathToFeed, currentUser);
		if (comments == null) {
			logger.log(Level.WARNING, "Unable to get comments feed ('"+pathToFeed+"') by current ("+currentUser+") user, trying with admin user");
			comments = rss.getFeedAuthenticatedByAdmin(pathToFeed);
		}
		if (comments == null) {
			logger.log(Level.SEVERE, "Error getting comments feed ('"+pathToFeed+"') by super admin!");
			return null;
		}
		
		WireFeed wireFeed = comments.createWireFeed();
		if (wireFeed instanceof Feed) {
			return (Feed) wireFeed;
		}
		return null;
	}
	
	@Override
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
	@Override
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
	
	public BPMContext getBpmContext() {
		return bpmContext;
	}

	@Override
	public boolean hasFullRightsForComments(String processInstanceId) {
		return hasFullRightsForComments(getConvertedValue(processInstanceId));
	}

	@Override
	public boolean hasFullRightsForComments(Long processInstanceId) {
		if (processInstanceId == null) {
			return false;
		}
		
		ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
		return piw.hasRight(Right.processHandler);
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Entry> getEntriesToFormat(Feed comments, CommentsViewerProperties properties) {
		List<Entry> allEntries = comments.getEntries();
		if (ListUtil.isEmpty(allEntries)) {
			return null;
		}
		
		boolean hasFullRights = hasFullRightsForComments(properties.getIdentifier());

		User currentUser = getLoggedInUser();
		if (currentUser == null) {
			return null;
		}
		int userId = -1;
		try {
			userId = Integer.valueOf(currentUser.getId());
		} catch(NumberFormatException e) {
			logger.warning("Error converting to number " + currentUser.getId());
			return null;
		}
		
		List<Entry> filteredEntries = new ArrayList<Entry>(allEntries.size());
		Map<String, Entry> entries = new HashMap<String, Entry>(allEntries.size());
		for (Entry entry: allEntries) {
			entries.put(entry.getId(), entry);
		}
		
		Collection<Comment> commentsByProcessInstance = null;
		try {
			CommentHome commentHome = (CommentHome) IDOLookup.getHome(Comment.class);
			commentsByProcessInstance = commentHome.findAllCommentsByHolder(properties.getIdentifier());
		} catch(Exception e) {
			logger.log(Level.WARNING, "Error getting comments by holder: " + properties.getIdentifier(), e);
			return null;
		}
		if (ListUtil.isEmpty(commentsByProcessInstance)) {
			return null;
		}
		
		for (Comment comment: commentsByProcessInstance) {
			Entry entry = entries.get(comment.getEntryId());
			if (entry != null && !filteredEntries.contains(entry)) {
				if (comment.isPrivateComment()) {
					if (hasFullRights) {
						//	Handler
						CommentEntry commentEntry = new CommentEntry(entry);
						commentEntry.setPublishable(!comment.isAnnouncedToPublic());
						commentEntry.setReplyable(isReplyable(comment, currentUser));
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						filteredEntries.add(commentEntry);
					} else if (comment.getAuthorId().intValue() == userId) {
						//	Comment's author 
						filteredEntries.add(entry);
					} else if (comment.isAnnouncedToPublic()) {
						//	Comment was announced to public
						CommentEntry commentEntry = new CommentEntry(entry);
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						commentEntry.setReadable(!isCommentRead(comment, currentUser));
						filteredEntries.add(commentEntry);
					} else if (isReplyToPrivateComment(comment, currentUser)) {
						//	This is a reply-comment to private comment
						CommentEntry commentEntry = new CommentEntry(entry);
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						commentEntry.setReadable(!isCommentRead(comment, currentUser));
						filteredEntries.add(commentEntry);
					}
				} else {
					filteredEntries.add(entry);
				}
			}
		}
		
		return filteredEntries;
	}
	
	private boolean isReplyToPrivateComment(Comment comment, User user) {
		Integer replyToId = comment.getReplyForCommentId();
		if (replyToId == null || replyToId < 0) {
			return false;
		}
		
		Comment originalComment = getComment(replyToId);
		if (originalComment == null) {
			return false;
		}
		
		//	We might want to do recursion here
		return String.valueOf(originalComment.getAuthorId()).equals(user.getId());
	}
	
	private boolean isReplyable(Comment comment, User user) {
		if (comment.isAnnouncedToPublic()) {
			return false;
		}
		
		Integer replyToId = comment.getReplyForCommentId();
		return replyToId == null || replyToId == -1 ? true : false;
	}
	
	private boolean isCommentRead(Comment comment, User user) {
		Collection<User> readers = comment.getReadBy();
		return (ListUtil.isEmpty(readers)) ? false : readers.contains(user);
	}
	
}