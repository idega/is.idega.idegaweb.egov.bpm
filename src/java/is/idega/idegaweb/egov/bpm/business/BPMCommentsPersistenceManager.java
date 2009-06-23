package is.idega.idegaweb.egov.bpm.business;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.idega.block.article.bean.ArticleCommentAttachmentInfo;
import com.idega.block.article.bean.CommentEntry;
import com.idega.block.article.bean.CommentsViewerProperties;
import com.idega.block.article.business.CommentsPersistenceManager;
import com.idega.block.article.business.DefaultCommentsPersistenceManager;
import com.idega.block.article.component.ArticleCommentAttachmentStatisticsViewer;
import com.idega.block.article.data.Comment;
import com.idega.block.article.data.CommentHome;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.block.rss.business.RSSBusiness;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogic;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.content.business.ContentConstants;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.contact.data.Email;
import com.idega.core.file.data.ICFile;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
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

	private static final Logger LOGGER = Logger.getLogger(BPMCommentsPersistenceManager.class.getName());
	
	public static final String SPRING_BEAN_IDENTIFIER = "bpmCommentsPersistenceManagerImpl";
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	
	private WireFeedOutput wfo = new WireFeedOutput();
	
	@Override
	public String getLinkToCommentsXML(final String processInstanceIdStr) {
		return getLinkToCommentFolder(processInstanceIdStr) + "comments.xml";
	}
	
	private String getLinkToCommentFolder(final String prcInstId) {
		if (StringUtil.isEmpty(prcInstId)) {
			return null;
		}
		
		final Long processInstanceId = Long.valueOf(prcInstId);
		final String storePath = getBpmContext().execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				String processName = context.getProcessInstance(processInstanceId).getProcessDefinition().getName();
				String storePath = new StringBuilder(JBPMConstants.BPM_PATH).append(CoreConstants.SLASH).append(processName).append("/processComments/")
					.append(prcInstId).append(CoreConstants.SLASH).toString();
				
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
			LOGGER.log(Level.SEVERE, "User must be logged!", e);
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
			LOGGER.log(Level.SEVERE, "Error creating path to comments feed!");
			return null;
		}
		SyndFeed comments = rss.getFeedAuthenticatedByUser(pathToFeed, currentUser);
		if (comments == null) {
			LOGGER.log(Level.WARNING, "Unable to get comments feed ('"+pathToFeed+"') by current ("+currentUser+") user, trying with admin user");
			comments = rss.getFeedAuthenticatedByAdmin(pathToFeed);
		}
		if (comments == null) {
			LOGGER.log(Level.SEVERE, "Error getting comments feed ('"+pathToFeed+"') by super admin!");
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
			LOGGER.log(Level.SEVERE, "Error storing comments feed", e);
		}
		
		return false;
	}
	
	private IWSlideService getRepository() {
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(
			    IWMainApplication.getDefaultIWApplicationContext(),
			    IWSlideService.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting repository", e);
		}
		return null;
	}
	
	private RSSBusiness getRSSBusiness() {
		try {
			return (RSSBusiness) IBOLookup.getServiceInstance(IWMainApplication
			        .getDefaultIWApplicationContext(), RSSBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting RSSBusiness", e);
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
		
		ProcessInstanceW piw = getProcessInstance(processInstanceId);
		return piw.hasRight(Right.processHandler);
	}
	
	protected ProcessInstanceW getProcessInstance(String processInstanceId) {
		if (StringUtil.isEmpty(processInstanceId)) {
			return null;
		}
		
		return getProcessInstance(Long.valueOf(processInstanceId));
	}
	
	protected ProcessInstanceW getProcessInstance(Long processInstanceId) {
		return getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}
	
	@Override
	public String getHandlerRoleKey() {
		return null;	//	Should be defined by extended class
	}

	@Override
	public List<String> getPersonsToNotifyAboutComment(CommentsViewerProperties properties, Object commentId, boolean justPublished) {
		return null;	//	No implementation here
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
			LOGGER.warning("Error converting to number " + currentUser.getId());
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
			LOGGER.log(Level.WARNING, "Error getting comments by holder: " + properties.getIdentifier(), e);
			return null;
		}
		if (ListUtil.isEmpty(commentsByProcessInstance)) {
			return null;
		}
		
		for (Comment comment: commentsByProcessInstance) {
			Entry entry = entries.get(comment.getEntryId());
			if (entry != null && !filteredEntries.contains(entry)) {
				CommentEntry commentEntry = new CommentEntry(entry);
				
				if (properties.isFetchFully()) {
					fillWithAttachments(comment, commentEntry, properties, hasFullRights);
				}
				
				boolean visible = true;
				boolean checkIfRead = false;
				if (comment.isPrivateComment()) {
					if (hasFullRights) {
						//	Handler
						commentEntry.setPublishable(!comment.isAnnouncedToPublic());
						commentEntry.setReplyable(isReplyable(comment, currentUser));
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						fillWithReaders(properties, comment, commentEntry);
					} else if (comment.isAnnouncedToPublic()) {
						//	Comment was announced to public
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						checkIfRead = true;
					} else if (isReplyToPrivateComment(comment, currentUser)) {
						//	This is a reply-comment to private comment
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						checkIfRead = true;
					} else if (comment.getAuthorId().intValue() != userId) {
						//	Not this user's private comment
						visible = false;
					}
				} else {
					if (!hasFullRights && comment.isAnnouncedToPublic()) {
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						checkIfRead = true;
					}
					if (hasFullRights) {
						fillWithReaders(properties, comment, commentEntry);
					}
				}
				
				if (visible) {
					if (properties.isFetchFully()&& checkIfRead) {
						if (!isCommentRead(comment, currentUser)) {
							commentEntry.setReadable(true);
							markAsRead(comment, currentUser, commentEntry);
						}
					}
					
					filteredEntries.add(commentEntry);
				}
			}
		}
		
		return filteredEntries;
	}
	
	private void fillWithAttachments(Comment comment, CommentEntry commentEntry, CommentsViewerProperties properties, boolean fullRights) {
		Collection<ICFile> attachments = comment.getAllAttachments();
		if (ListUtil.isEmpty(attachments)) {
			return;
		}
		
		List<BinaryVariable> processAttachments = getAllProcessAttachments(properties.getIdentifier());
		String commentId = comment.getPrimaryKey().toString();
		for (ICFile attachment: attachments) {
			if (fullRights || isAttachmentVisible(processAttachments, attachment.getHash(), comment.isPrivateComment())) {
				try {
					String name = URLDecoder.decode(attachment.getName(), CoreConstants.ENCODING_UTF8);
					ArticleCommentAttachmentInfo info = new ArticleCommentAttachmentInfo();
					info.setName(name);
					info.setUri(getUriToAttachment(commentId, attachment, null));
					
					if (fullRights) {
						info.setCommentId(commentId);
						String attachmentId = attachment.getPrimaryKey().toString();
						info.setAttachmentId(attachmentId);
						info.setBeanIdentifier(properties.getSpringBeanIdentifier());
						info.setIdentifier(properties.getIdentifier());
						info.setStatisticsUri(BuilderLogic.getInstance().getUriToObject(ArticleCommentAttachmentStatisticsViewer.class, Arrays.asList(
								new AdvancedProperty(ArticleCommentAttachmentStatisticsViewer.COMMENT_ID_PARAMETER, commentId),
								new AdvancedProperty(ArticleCommentAttachmentStatisticsViewer.COMMENT_ATTACHMENT_ID_PARAMETER, attachmentId),
								new AdvancedProperty(ArticleCommentAttachmentStatisticsViewer.IDENTIFIER_PARAMETER, properties.getIdentifier()),
								new AdvancedProperty(ArticleCommentAttachmentStatisticsViewer.BEAN_IDENTIFIER_PARAMETER, properties.getSpringBeanIdentifier())
						)));
					}
					
					commentEntry.addAttachment(info);
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error adding attachment's link: " + attachment.getFileUri(), e);
				}
			}
		}
	}
	
	private boolean isAttachmentVisible(List<BinaryVariable> processAttachments, Integer hash, boolean privateComment) {
		if (privateComment) {
			return true;
		}
		
		if (ListUtil.isEmpty(processAttachments)) {
			//	No attachments for the process
			return false;
		}
		
		if (hash == null) {
			return false;
		}
		
		for (BinaryVariable processAttachment: processAttachments) {
			if (hash.intValue() == processAttachment.getHash().intValue()) {
				return true;
			}
		}
		
		return false;
	}
	
	private List<BinaryVariable> getAllProcessAttachments(String processInstanceId) {
		ProcessInstanceW piw = getProcessInstance(processInstanceId);
		if (piw == null) {
			return null;
		}
		return piw.getAttachments();
	}
	
	@SuppressWarnings("unchecked")
	private void fillWithReaders(CommentsViewerProperties properties, Comment comment, CommentEntry commentEntry) {
		if (!properties.isFetchFully()) {
			return;
		}
		
		Collection<User> readBy = comment.getReadBy();
		if (ListUtil.isEmpty(readBy)) {
			return;
		}
		
		for (User reader: readBy) {
			AdvancedProperty readerInfo = new AdvancedProperty(reader.getName());
			
			Collection<Email> emails = reader.getEmails();
			if (!ListUtil.isEmpty(emails)) {
				readerInfo.setValue(emails.iterator().next().getEmailAddress());
			}
			
			commentEntry.addReader(readerInfo);
		}
	}
	
	private void markAsRead(Comment comment, User user, CommentEntry commentEntry) {
		try {
			comment.addReadBy(user);
			comment.store();
			commentEntry.setReadable(false);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error marking as read", e);
		}
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
		
		try {
			if (originalComment.getAuthorId().intValue() == Integer.valueOf(user.getId()).intValue()) {
				return true;
			}
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error checking if current user is author of comment: " + originalComment, e);
		}
		return false;
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
	
	@Override
	public String getCommentFilesPath(CommentsViewerProperties properties) {
		if (properties == null) {
			return null;
		}
		
		return new StringBuilder(getLinkToCommentFolder(properties.getIdentifier())).append("files/").toString();
	}

	@Override
	public Object addComment(CommentsViewerProperties properties) {
		Object commentId = super.addComment(properties);
		if (commentId == null) {
			return null;
		}
		
		Comment comment = getComment(commentId);
		if (comment == null) {
			return commentId;
		}
		
		Collection<ICFile> attachments = comment.getAllAttachments();
		if (ListUtil.isEmpty(attachments)) {
			return commentId;
		}
		
		Long prcInsId = null;
		try {
			prcInsId = Long.valueOf(properties.getIdentifier());
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error parsing id from: " + properties.getIdentifier(), e);
		}
		if (prcInsId == null) {
			return commentId;
		}
		
		if (!hasFullRightsForComments(prcInsId)) {
			return commentId;
		}
		
		ProcessInstanceW piw = null;
		try {
			piw = getBpmFactory().getProcessManagerByProcessInstanceId(prcInsId).getProcessInstance(prcInsId);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting process instance by: " + prcInsId, e);
		}
		if (piw == null) {
			return commentId;
		}
		
		TaskInstanceW task = getSubmittedTaskInstance(piw, getTaskNameForAttachments());
		if (task == null) {
			return commentId;
		}
		
		IWResourceBundle iwrb = null;
		try {
			iwrb = IWMainApplication.getDefaultIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(CoreUtil.getIWContext());
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting " + IWResourceBundle.class, e);
		}
		String description = iwrb == null ? "Attached file for" : iwrb.getLocalizedString("attached_file_for", "Attached file for");
		String subject = StringUtil.isEmpty(properties.getSubject()) ?
				iwrb == null ? "comment" : iwrb.getLocalizedString("comment", "comment") :
				properties.getSubject();
		for (ICFile attachment: attachments) {
			InputStream is = getInputStreamForAttachment(attachment.getFileUri());
			if (is == null) {
				LOGGER.warning("Unable to attach file " + attachment.getFileUri() + " for task: " + task.getTaskInstanceId() + " in process: "
						+ piw.getProcessInstanceId());
			} else {
				Variable variable = new Variable("cmnt_attch", VariableDataType.FILE);
				String name = getAttachmentName(attachment.getName());
				BinaryVariable binVariable = task.addAttachment(variable, name,
						new StringBuilder(description).append(CoreConstants.COLON).append(CoreConstants.SPACE).append(subject).toString(), is);
				
				if (binVariable != null) {
					attachment.setHash(binVariable.getHash());
					attachment.store();
				}
				
				IOUtil.closeInputStream(is);
			}
		}
		
		return commentId;
	}
	
	private InputStream getInputStreamForAttachment(String uri) {
		try {
			IWSlideService slide = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
			WebdavResource resource = slide.getWebdavResourceAuthenticatedAsRoot(URLDecoder.decode(uri, CoreConstants.ENCODING_UTF8));
			if (resource != null && resource.exists()) {
				return resource.getMethodData();
			}
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting InputStream from: " + uri, e);
		}
		return null;
	}
	
	private String getAttachmentName(String defaultName) {
		try {
			return URLDecoder.decode(defaultName, CoreConstants.ENCODING_UTF8);
		} catch (UnsupportedEncodingException e) {
			LOGGER.log(Level.WARNING, "Error decoding: " + defaultName);
		}
		return defaultName;
	}
	
	protected TaskInstanceW getSubmittedTaskInstance(ProcessInstanceW piw, String taskName) {
		List<TaskInstanceW> submittedTasks = piw.getSubmittedTaskInstances();
		if (ListUtil.isEmpty(submittedTasks)) {
			return null;
		}

		TaskInstanceW latestTask = null;
		for (TaskInstanceW taskInstance: submittedTasks) {
			if (taskName.equals(taskInstance.getTaskInstance().getName())) {
				if (latestTask == null) {
					latestTask = taskInstance;
				} else if (taskInstance.getTaskInstance().getCreate().after(latestTask.getTaskInstance().getCreate())) {
					latestTask = taskInstance;
				}
			}
		}
		
		return latestTask;
	}
	
	@Override
	public boolean useFilesUploader(CommentsViewerProperties properties) {
		if (properties == null) {
			return false;
		}
		
		return true;
	}
	
}