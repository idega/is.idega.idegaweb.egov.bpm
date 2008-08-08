package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler.LocalizedMessages;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.message.business.CommuneMessageBusiness;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.message.data.Message;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueHandler;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/08/08 16:17:41 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class SendMessageImpl implements SendMessage {
	
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private MessageValueHandler messageValueHandler;

	public void send(final ProcessInstance pi, final Integer caseId, final LocalizedMessages msgs, final Token tkn, final String sendToRoles) {
		
		final IWContext iwc = IWContext.getCurrentInstance();
		final CommuneMessageBusiness messageBusiness = getCommuneMessageBusiness(iwc);
		final UserBusiness userBusiness  = getUserBusiness(iwc);
		
		final Locale defaultLocale = iwc.getCurrentLocale();
		
		new Thread(new Runnable() {

			public void run() {
				
				try {
					CasesBusiness casesBusiness = getCasesBusiness(iwc);
					
					final GeneralCase theCase = casesBusiness.getGeneralCase(caseId);
					Collection<User> users = getUsersToSendMessageTo(iwc, sendToRoles, pi);
					
					HashMap<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);
					MessageValueContext mvCtx = new MessageValueContext(5);
					
					for (User user : users) {
						
						Locale preferredLocale = userBusiness.getUsersPreferredLocale(user);
						
						if(preferredLocale == null)
							preferredLocale = defaultLocale;
						
						String unformattedSubject;
						String unformattedMsg;
						
						if(!unformattedForLocales.containsKey(preferredLocale)) {
						
							unformattedSubject = msgs.getLocalizedSubject(preferredLocale);;
							unformattedMsg = msgs.getLocalizedMessage(preferredLocale);
							
							unformattedForLocales.put(preferredLocale, new String[] {unformattedSubject, unformattedMsg});
						} else {
							
							String[] unf = unformattedForLocales.get(preferredLocale);
							
							unformattedSubject = unf[0];
							unformattedMsg = unf[1];
						}
						
						String formattedMsg;
						String formattedSubject;
						
						mvCtx.setValue(MessageValueContext.userBean, user);
						
						if(unformattedMsg == null)
							formattedMsg = unformattedMsg;
						else
							formattedMsg = getFormattedMessage(unformattedMsg, msgs.getMessageValuesExp(), tkn, mvCtx);
						
						if(unformattedSubject == null)
							formattedSubject = unformattedSubject;
						else
							formattedSubject = getFormattedMessage(unformattedSubject, msgs.getSubjectValuesExp(), tkn, mvCtx);
						
						System.out.println("message="+formattedMsg);
						
						Message message = messageBusiness.createUserMessage(theCase, user, null, null, formattedSubject, formattedMsg, formattedMsg, null, false, null, false, true);
						message.store();
					}
					
				} catch (RemoteException e) {
					Logger.getLogger(SendCaseMessagesHandler.class.getName()).log(Level.SEVERE, "Exception while sending user message, some messages might be not sent", e);
				} catch (FinderException e) {
					Logger.getLogger(SendCaseMessagesHandler.class.getName()).log(Level.SEVERE, "Exception while sending user message, some messages might be not sent", e);
				}
			}
			
		}).start();
	}
	
	protected CommuneMessageBusiness getCommuneMessageBusiness(IWApplicationContext iwac) {
		try {
			return (CommuneMessageBusiness)IBOLookup.getServiceInstance(iwac, CommuneMessageBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness)IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public String getFormattedMessage(String unformattedMessage, String messageValues, Token tkn, MessageValueContext mvCtx) {
		
		return getMessageValueHandler().getFormattedMessage(unformattedMessage, messageValues, tkn, mvCtx);
	}
	
	public Collection<User> getUsersToSendMessageTo(IWContext iwc, String rolesNamesAggr, ProcessInstance pi) {
		
		Collection<User> allUsers;
		
		if(rolesNamesAggr != null) {
		
			String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);
			
			HashSet<String> rolesNamesSet = new HashSet<String>(rolesNames.length);
			
			for (int i = 0; i < rolesNames.length; i++)
				rolesNamesSet.add(rolesNames[i]);
			
			allUsers = getBpmFactory().getRolesManager().getAllUsersForRoles(rolesNamesSet, pi.getId());
		} else
			allUsers = new ArrayList<User>(0);
		
		return allUsers;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public MessageValueHandler getMessageValueHandler() {
		return messageValueHandler;
	}
}