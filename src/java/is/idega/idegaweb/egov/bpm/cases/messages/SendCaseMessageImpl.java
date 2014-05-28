package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.events.BPMNewMessageEvent;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.message.business.CommuneMessageBusiness;
import is.idega.idegaweb.egov.message.business.MessageValue;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.message.data.Message;
import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.SendMailMessageImpl;
import com.idega.bpm.process.messages.SendMessageType;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.TypeRef;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2009/01/22 17:29:22 $ by $Author: anton $
 */

@Service
@SendMessageType("caseMessage")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SendCaseMessageImpl extends SendMailMessageImpl {

	public static final TypeRef caseUserBean = new TypeRef("bean", "caseUser");

	@Autowired
	private CaseUserFactory caseUserFactory;

	@Override
	public void send(MessageValueContext mvCtx, final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn) {
		final Integer caseId = (Integer)context;

		final IWContext iwc = CoreUtil.getIWContext();
		final IWMainApplication iwma = iwc == null ? IWMainApplication.getDefaultIWMainApplication() : iwc.getIWMainApplication();

		final CommuneMessageBusiness messageBusiness = getCommuneMessageBusiness(iwc);
		final UserBusiness userBusiness  = getUserBusiness(iwc);

		Locale defaultLocale = iwma.getDefaultLocale();
		//	Making sure default locale is not null
		defaultLocale = iwc == null ? defaultLocale : iwc.getCurrentLocale();
		defaultLocale = defaultLocale == null ? Locale.ENGLISH : defaultLocale;

		Collection<User> users = null;
		final List<MessageValue> msgValsToSend = new ArrayList<MessageValue>();
		try {
			CasesBusiness casesBusiness = getCasesBusiness(iwc);

			final GeneralCase theCase = casesBusiness.getGeneralCase(caseId);
			if (msgs.getRecipientUserId() != null) {
				users = new ArrayList<User>();
				users.add(getUserBusiness(iwc).getUser(msgs.getRecipientUserId()));
			} else if (!StringUtil.isEmpty(msgs.getSendToRoles())) {
				users = getUsersToSendMessageTo(msgs.getSendToRoles(), pi);
			}

			if (ListUtil.isEmpty(users)) {
				getLogger().warning("There are no recipients to send message " + msgs);
			} else {
				getLogger().info("Sending message " + msgs + " to " + users + " for case " + caseId);
			}

			long pid = pi.getId();
			ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(pid).getProcessInstance(pid);

			Map<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);

			if (mvCtx == null)
				mvCtx = new MessageValueContext(3);

			for (User user: users) {
				Locale preferredLocale = userBusiness.getUsersPreferredLocale(user);

				if (preferredLocale == null)
					preferredLocale = defaultLocale;

				CaseUserImpl caseUser = getCaseUserFactory().getCaseUser(user, piw);

				mvCtx.setValue(MessageValueContext.userBean, user);
				mvCtx.setValue(caseUserBean, caseUser);
				mvCtx.setValue(MessageValueContext.piwBean, piw);

				Timestamp creationDate = theCase.getCreated();
				if (creationDate != null) {
					IWTimestamp iwCreationDate = new IWTimestamp(creationDate);
					mvCtx.setValue(TypeRef.CREATION_DATE, iwCreationDate.getLocaleDate(preferredLocale, DateFormat.MEDIUM));
					mvCtx.setValue(TypeRef.CREATION_TIME, iwCreationDate.getLocaleTime(preferredLocale, DateFormat.FULL));
				}

				String[] subjNMsg = getFormattedMessage(mvCtx, preferredLocale, msgs, unformattedForLocales, tkn);

				String subject = subjNMsg[0];
				String text = subjNMsg[1];
				if (subject == null || text == null) {
					getLogger().warning("Unable to send message because subject (" + subject + ") or/and text (" + text + ") is null!");
					continue;
				}

				getLogger().info("Will create case user message with subject="+subject+", text="+text+" for user (id="+user.getPrimaryKey()+
						") name="+user.getName());

				//Hard coding of the death!
				if (subject.equals("Vinsamlega endurnýjið veiðileyfi fyrir komandi fiskveiðiár")) {
					//Don't add message...
				} else {
					MessageValue mv = messageBusiness.createUserMessageValue(theCase, user, null, null, subject, text, text, null, false, null,
							false, true);
					msgValsToSend.add(mv);
				}
			}
		} catch (Exception e) {
			String message = "Error sending message (" + msgs + " to " + users + " for case " + caseId+ ") to users " + users;
			getLogger().log(Level.SEVERE, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}

		if (ListUtil.isEmpty(msgValsToSend)) {
			getLogger().warning("There are no messages to send!");
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (MessageValue messageValue: msgValsToSend) {
						Message message = messageBusiness.createUserMessage(messageValue);
						if (message == null) {
							throw new RuntimeException("User message was not created: " + messageValue);
						}

						message.store();

						/* Will inform notifications system */
						ELUtil.getInstance().publishEvent(
								new BPMNewMessageEvent(
										message,
										messageValue.getReceiver())
						);
					}
				} catch (Exception e) {
					String message = "Exception while sending user messages (" + msgValsToSend + "), some messages might be not sent";
					getLogger().log(Level.SEVERE, message, e);
					CoreUtil.sendExceptionNotification(message, e);
				}
			}
		}).start();
	}

	protected CommuneMessageBusiness getCommuneMessageBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, CommuneMessageBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	@Override
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	CaseUserFactory getCaseUserFactory() {
		return caseUserFactory;
	}

	void setCaseUserFactory(CaseUserFactory caseUserFactory) {
		this.caseUserFactory = caseUserFactory;
	}
}