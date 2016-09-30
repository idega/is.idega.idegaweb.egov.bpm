package is.idega.idegaweb.egov.bpm.business;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.security.Privilege;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.article.bean.ArticleCommentAttachmentInfo;
import com.idega.block.article.bean.CommentEntry;
import com.idega.block.article.bean.CommentsViewerProperties;
import com.idega.block.article.business.CommentsPersistenceManager;
import com.idega.block.article.business.DefaultCommentsPersistenceManager;
import com.idega.block.article.component.ArticleCommentAttachmentStatisticsViewer;
import com.idega.block.article.component.CommentsViewer;
import com.idega.block.article.data.Comment;
import com.idega.block.article.data.CommentHome;
import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.block.rss.business.RSSBusiness;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogic;
import com.idega.business.file.FileDownloadNotificationProperties;
import com.idega.content.business.ContentConstants;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.contact.data.Email;
import com.idega.core.file.data.ICFile;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jackrabbit.repository.access.JackrabbitAccessControlEntry;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.repository.RepositoryConstants;
import com.idega.repository.access.AccessControlEntry;
import com.idega.repository.access.AccessControlList;
import com.idega.repository.access.RepositoryPrivilege;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.synd.SyndFeed;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserFactory;
import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserImpl;
import is.idega.idegaweb.egov.bpm.servlet.CommentViewerRedirector;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(BPMCommentsPersistenceManager.SPRING_BEAN_IDENTIFIER)
public class BPMCommentsPersistenceManager extends DefaultCommentsPersistenceManager implements CommentsPersistenceManager {

	private static final Logger LOGGER = Logger.getLogger(BPMCommentsPersistenceManager.class.getName());

	public static final String SPRING_BEAN_IDENTIFIER = "bpmCommentsPersistenceManagerImpl";

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private CaseUserFactory caseUserFactory;

	@Autowired
	private CasesBPMDAO casesDAO;

	@Override
	public String getLinkToCommentsXML(final String processInstanceIdStr) {
		return getLinkToCommentFolder(processInstanceIdStr) + "comments.xml";
	}

	private String getLinkToCommentFolder(final String prcInstId) {
		if (StringUtil.isEmpty(prcInstId)) {
			return null;
		}

		final Long processInstanceId = Long.valueOf(prcInstId);
		final String storePath = getBpmContext().execute(new JbpmCallback<String>() {
			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {

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

		User currentUser = getLegacyUser(getCurrentUser());
		if (currentUser != null) {
			return hasRightToViewComments(processInstanceId, currentUser);
		}

		return false;
	}

	@Override
	public boolean hasRightsToWriteComments(Long identifier) {
		if (identifier == null)
			return false;

		User currentUser = getLegacyUser(getCurrentUser());
		if (currentUser != null) {
			return hasRightToWriteComments(identifier, currentUser);
		}

		return false;
	}

	protected boolean hasRightToWriteComments(Long processInstanceId, User user) {
		return getBpmFactory().getRolesManager().canWriteComments(processInstanceId, user);
	}

	protected boolean hasRightToViewComments(Long processInstanceId, User user) {
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

	private IWResourceBundle getResourceBundle(IWContext iwc) {
		return iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
	}

	@Transactional(readOnly = true)
	@Override
	public String getFeedTitle(IWContext iwc, String processInstanceId) {
		String defaultTitle = "Comments of process";
		String localizedTitle = null;
		try {
			ProcessInstanceW piw = getProcessInstance(processInstanceId);
			ApplicationBusiness appBusiness = getApplicationBusiness(iwc);
			Collection<Application> apps = appBusiness.getApplicationHome().findAllByApplicationUrl(piw.getProcessDefinitionW().getName());
			if (ListUtil.isEmpty(apps)) {
				return defaultTitle;
			}

			localizedTitle = appBusiness.getApplicationName(apps.iterator().next(), iwc.getCurrentLocale());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting localized name for process instance's ('"+defaultTitle+"') application", e);
		}

		localizedTitle = StringUtil.isEmpty(localizedTitle) ? defaultTitle : localizedTitle;
		return getResourceBundle(iwc).getLocalizedString("feed.comments_of_process", defaultTitle) + (defaultTitle.equals(localizedTitle) ?
				CoreConstants.EMPTY : (" " + localizedTitle));
	}

	@Override
	public String getFeedSubtitle(IWContext iwc, String processInstanceId) {
		return getResourceBundle(iwc).getLocalizedString("feed.all_comments_of_a_process", "All comments of a process");
	}

	private void updateAccessRights(String uri) {
		AccessControlList acl = null;
		try {
			if (StringUtil.isEmpty(uri) || !getRepositoryService().getExistence(uri))
				return;	//	Resource does not exist

			acl = getRepositoryService().getAccessControlList(uri);
			if (acl == null) {
				return;
			}

			AccessControlEntry ace = new JackrabbitAccessControlEntry(RepositoryConstants.SUBJECT_URI_AUTHENTICATED);
			ace.addPrivilege(new RepositoryPrivilege(Privilege.JCR_READ));
			acl.add(ace);

			getRepositoryService().storeAccessControlList(acl);
		} catch (Exception e) {}
	}

	@Override
	public Feed getCommentsFeed(String processInstanceId) {
		String uri = getLinkToCommentsXML(processInstanceId);
		if (StringUtil.isEmpty(uri)) {
			LOGGER.warning("URI to comments feed was not constructed!");
			return null;
		}

		try {
			if (!getRepositoryService().getExistence(uri)) {
				return null;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error checking resource's existence", e);
			return null;
		}

		updateAccessRights(uri);

		RSSBusiness rss = getRSSBusiness();
		if (rss == null) {
			LOGGER.warning(RSSBusiness.class + " service is not available!");
			return null;
		}
		User currentUser = getLegacyUser(getCurrentUser());
		if (currentUser == null) {
			LOGGER.warning("User is not logged in!");
			return null;
		}

		String pathToFeed = null;
		try {
			pathToFeed = new StringBuilder(getHost()).append(getRepositoryService().getWebdavServerURL()).append(uri).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (StringUtil.isEmpty(pathToFeed)) {
			LOGGER.log(Level.SEVERE, "Error creating path to comments feed!");
			return null;
		}
		SyndFeed comments = rss.getFeedAuthenticatedByUser(pathToFeed, currentUser);
		if (comments == null) {
			LOGGER.log(Level.WARNING, "Unable to get comments feed ('" + pathToFeed + "') by current (" + currentUser + ") user, trying with admin user");
			comments = rss.getFeedAuthenticatedByAdmin(pathToFeed);
		}
		if (comments == null) {
			LOGGER.log(Level.SEVERE, "Error getting comments feed ('" + pathToFeed + "') by super admin!");
			return null;
		}

		if (comments instanceof Feed) {
			return (Feed) comments;
		}

		WireFeed wireFeed = comments.createWireFeed();
		if (wireFeed instanceof Feed) {
			return (Feed) wireFeed;
		}

		LOGGER.warning("Object " + wireFeed + " (" + wireFeed.getClass() + ") is not instance of: " + Feed.class);
		return null;
	}

	@Override
	public boolean storeFeed(String processInstanceId, Feed comments) {
		if (comments == null) {
			LOGGER.warning("Comments feed is undefined!");
			return false;
		}

		String url = getLinkToCommentsXML(processInstanceId);
		if (StringUtil.isEmpty(url)) {
			LOGGER.warning("Unable to resolve link to comments using process instance id: " + processInstanceId + " for comments feed: " + comments);
			return false;
		}

		String commentsContent = getFeedContent(comments);
		if (commentsContent == null) {
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
			return getRepositoryService().uploadFileAndCreateFoldersFromStringAsRoot(fileBase, fileName, commentsContent, ContentConstants.XML_MIME_TYPE);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error storing comments feed", e);
		}

		LOGGER.warning("Unable to upload comments " + commentsContent + " to: " + fileBase + fileName);
		return false;
	}

	private RSSBusiness getRSSBusiness() {
		return getServiceInstance(RSSBusiness.class);
	}

	// TODO: When access rights (to repository resources) are fixed, use current user!
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
			return getLegacyUser(accessController.getAdministratorUser());
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
		Integer authorId = null;
		try {
			Comment comment = getComment(commentId);
			authorId = comment.getAuthorId();
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting author for comment: " + commentId, e);
		}

		return getAllFeedSubscribers(properties.getIdentifier(), authorId);
	}

	protected boolean canReadAndWriteAllComments(Long identifier, User user) {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Entry> getEntriesToFormat(Feed comments, CommentsViewerProperties properties) {
		List<Entry> allEntries = comments.getEntries();
		if (ListUtil.isEmpty(allEntries)) {
			return null;
		}

		boolean hasFullRights = hasFullRightsForComments(properties.getIdentifier());

		User currentUser = getLegacyUser(getCurrentUser());
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

		boolean canSeeAndWriteCommentsAllComments = canReadAndWriteAllComments(getConvertedValue(properties.getIdentifier()), currentUser);

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
						commentEntry.setPublishable(isRelyToComment(comment) != null);
						commentEntry.setPublished(comment.isAnnouncedToPublic());
						commentEntry.setReplyable(isReplyable(comment, currentUser));
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						fillWithReaders(properties, comment, commentEntry);
					} else if (comment.isAnnouncedToPublic() || canSeeAndWriteCommentsAllComments) {
						//	Comment was announced to public
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						checkIfRead = true;
					} else if (isReplyToPrivateComment(comment, currentUser) || canSeeAndWriteCommentsAllComments) {
						//	This is a reply-comment to private comment
						commentEntry.setPrimaryKey(comment.getPrimaryKey().toString());
						checkIfRead = true;
					} else if (comment.getAuthorId().intValue() != userId || canSeeAndWriteCommentsAllComments) {
						//	Not this user's private comment
						visible = false;
					}
				} else {
					if (!hasFullRights && comment.isAnnouncedToPublic() || canSeeAndWriteCommentsAllComments) {
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

	private Comment isRelyToComment(Comment comment) {
		Integer replyToId = comment.getReplyForCommentId();
		if (replyToId == null || replyToId < 0) {
			return null;
		}

		return getComment(replyToId);
	}

	private boolean isReplyToPrivateComment(Comment comment, User user) {
		Comment originalComment = isRelyToComment(comment);
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
			LOGGER.warning("Unable to create comment in DB table!");
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
			LOGGER.warning("Do not know for which task to attach file(s)");
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
			return getRepositoryService().getInputStreamAsRoot(URLDecoder.decode(uri, CoreConstants.ENCODING_UTF8));
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
		if (piw == null || StringUtil.isEmpty(taskName)) {
			return null;
		}

		List<TaskInstanceW> submittedTasks = piw.getSubmittedTaskInstances();
		if (ListUtil.isEmpty(submittedTasks)) {
			return null;
		}

		TaskInstanceW latestTask = null;
		for (TaskInstanceW taskInstance: submittedTasks) {
			TaskInstance ti = taskInstance.getTaskInstance();
			java.util.Date created = ti.getCreate();

			if (taskName.equals(ti.getName())) {
				if (latestTask == null) {
					latestTask = taskInstance;
				} else if (created.after(latestTask.getTaskInstance().getCreate())) {
					latestTask = taskInstance;
				}
			}
		}

		return latestTask;
	}

	@Override
	protected String getLinkForRecipient(String recipient, CommentsViewerProperties properties) {
		ProcessInstanceW piw = getProcessInstance(properties.getIdentifier());
		if (piw == null) {
			return super.getLinkForRecipient(recipient, properties);
		}

		UserBusiness userBusiness = getUserBusiness();
		if (userBusiness == null) {
			return super.getLinkForRecipient(recipient, properties);
		}

		Collection<User> recipients = userBusiness.getUsersByEmail(recipient);
		if (ListUtil.isEmpty(recipients)) {
			return super.getLinkForRecipient(recipient, properties);
		}

		CaseUserImpl caseUser = getCaseUserFactory().getCaseUser(recipients.iterator().next(), piw);
		String url = caseUser.getUrlToTheCase();
		if (StringUtil.isEmpty(url)) {
			return super.getLinkForRecipient(recipient, properties);
		}

		URIUtil uriUtil = new URIUtil(url);
		uriUtil.setParameter(CommentsViewer.AUTO_SHOW_COMMENTS, Boolean.TRUE.toString());
		return uriUtil.getUri();
	}

	public CaseUserFactory getCaseUserFactory() {
		return caseUserFactory;
	}

	public void setCaseUserFactory(CaseUserFactory caseUserFactory) {
		this.caseUserFactory = caseUserFactory;
	}

	@Override
	protected List<User> getCaseHandlers(String identifier) {
		ProcessInstanceW piw = getProcessInstance(identifier);
		if (piw == null) {
			return null;
		}

		Integer handlerId = piw.getHandlerId();
		if (handlerId == null) {
			return null;
		}

		UserBusiness userBusiness = getUserBusiness();
		User handler = null;
		try {
			handler = userBusiness.getUser(handlerId);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting user by id: " + handlerId, e);
		}

		if (handler == null) {
			return null;
		}

		List<User> handlers = new ArrayList<User>();
		handlers.add(handler);
		return handlers;
	}

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
			return getCaseBusiness(IWMainApplication.getDefaultIWApplicationContext()).getCase(bind.getCaseId());
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting case by id: " + bind.getCaseId(), e);
		}

		return null;
	}

	public CasesBPMDAO getCasesDAO() {
		return casesDAO;
	}

	public void setCasesDAO(CasesBPMDAO casesDAO) {
		this.casesDAO = casesDAO;
	}

	protected CaseBusiness getCaseBusiness(IWApplicationContext iwac) {
		return getServiceInstance(iwac, CaseBusiness.class);
	}

	protected ApplicationBusiness getApplicationBusiness(IWApplicationContext iwac) {
		return getServiceInstance(iwac, ApplicationBusiness.class);
	}

	@Override
	public Map<String, String> getUriToDocument(FileDownloadNotificationProperties properties, String identifier, List<User> users) {
		if (StringUtil.isEmpty(identifier)) {
			return super.getUriToDocument(properties, identifier, users);
		}

		ProcessInstanceW piw = getProcessInstance(identifier);
		if (piw == null) {
			return super.getUriToDocument(properties, identifier, users);
		}

		Map<String, String> linksForUsers = new HashMap<String, String>();
		for (User user: users) {
			CaseUserImpl caseUser = null;
			try {
				caseUser = caseUserFactory.getCaseUser(user, piw);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting url to user's case for user " + user + " and process instance: " + piw.getProcessInstanceId(), e);
			}

			String uri = null;
			if (caseUser == null) {
				uri = properties.getUrl();
			} else {
				String url = caseUser.getUrlToTheCase();
				if (StringUtil.isEmpty(url)) {
					uri = properties.getUrl();
				} else {
					URIUtil uriUtil = new URIUtil(url);
					uriUtil.setParameter(CommentsViewer.AUTO_SHOW_COMMENTS, Boolean.TRUE.toString());
					uri = uriUtil.getUri();
				}
			}

			uri = StringUtil.isEmpty(uri) ? properties.getUrl() : uri;
			linksForUsers.put(user.getId(), uri);
		}

		return linksForUsers;
	}

	@Override
	public String getUriForCommentLink(CommentsViewerProperties properties) {
		if (properties == null) {
			return super.getUriForCommentLink(properties);
		}

		String processInstanceId = properties.getIdentifier();
		if (StringUtil.isEmpty(processInstanceId)) {
			return super.getUriForCommentLink(properties);
		}

		URIUtil uriUtil = new URIUtil(new StringBuilder(CoreConstants.SLASH).append(CommentViewerRedirector.class.getSimpleName()).append(CoreConstants.SLASH)
				.toString());
		uriUtil.setParameter(CommentViewerRedirector.PARAMETER_REDIRECT_TO_COMMENT, Boolean.TRUE.toString());
		uriUtil.setParameter(ProcessManagerBind.processInstanceIdParam, processInstanceId);
		return uriUtil.getUri();
	}

}