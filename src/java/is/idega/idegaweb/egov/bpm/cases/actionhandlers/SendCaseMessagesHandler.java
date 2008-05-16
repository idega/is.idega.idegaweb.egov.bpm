package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.message.business.CommuneMessageBusiness;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.block.process.message.data.Message;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/16 18:17:07 $ by $Author: civilis $
 */
public class SendCaseMessagesHandler implements ActionHandler {

	private static final long serialVersionUID = 1212382470685233437L;
	
	private static final String beanUserIdentifier = "bean:user";
	
	private String subjectKey;
	private String subjectValues;
	private String messageKey;
	private String messageValues;
	private String messagesBundle;
	private String sendToRoles;
	
	public String getSendToRoles() {
		return sendToRoles;
	}

	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}

	public void execute(ExecutionContext ctx) throws Exception {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		final IWContext iwc = IWContext.getIWContext(fctx);
		final String sendToRoles = getSendToRoles();
		final ProcessInstance pi = ctx.getProcessInstance();
		
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		String caseIdStr = (String)ctx.getVariable(CasesBPMProcessConstants.caseIdVariableName);
		final GeneralCase theCase = casesBusiness.getGeneralCase(new Integer(caseIdStr));
		final CommuneMessageBusiness messageBusiness = getCommuneMessageBusiness(iwc);
		
		final String subjectKey = getSubjectKey();
		final String msgKey = getMessageKey();

		final String subjectValuesExp = getSubjectValues();
		final String messageValuesExp = getMessageValues();
		final Token tkn = ctx.getToken();
		String bundleIdentifier = getMessagesBundle();
		
		if(bundleIdentifier == null)
			bundleIdentifier = IWBundleStarter.IW_BUNDLE_IDENTIFIER;
		
		final IWBundle iwb = iwc.getIWMainApplication().getBundle(bundleIdentifier);
		
		new Thread(new Runnable() {

			public void run() {
				
				try {
					SendCaseMessagesHandlerBean bean = (SendCaseMessagesHandlerBean)WFUtil.getBeanInstance(iwc, SendCaseMessagesHandlerBean.beanIdentifier);
					Collection<User> users = bean.getUsersToSendMessageTo(iwc, sendToRoles, pi);
					
					HashMap<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);
					MessageValueContext mvCtx = new MessageValueContext(5);
					
					for (User user : users) {
						
						String preferredLocaleStr = user.getPreferredLocale();
						Locale preferredLocale;
						
						if(preferredLocaleStr == null || CoreConstants.EMPTY.equals(preferredLocaleStr)) {

							preferredLocale = new Locale("is");
							
						} else {
							preferredLocale = new Locale(preferredLocaleStr); 
						}
						
						String unformattedSubject;
						String unformattedMsg;
						
						if(!unformattedForLocales.containsKey(preferredLocale)) {
						
							IWResourceBundle iwrb = iwb.getResourceBundle(preferredLocale);
						
							unformattedSubject = iwrb.getLocalizedString(subjectKey, CoreConstants.EMPTY);
							unformattedMsg = iwrb.getLocalizedString(msgKey, CoreConstants.EMPTY);
							
							unformattedForLocales.put(preferredLocale, new String[] {unformattedSubject, unformattedMsg});
						} else {
							
							String[] unf = unformattedForLocales.get(preferredLocale);
							
							unformattedSubject = unf[0];
							unformattedMsg = unf[1];
						}
						
						String formattedMsg;
						String formattedSubject;
						
						mvCtx.put(beanUserIdentifier, user);
						
						if(unformattedMsg == null)
							formattedMsg = unformattedMsg;
						else
							formattedMsg = bean.getFormattedMessage(unformattedMsg, messageValuesExp, tkn, mvCtx);
						
						if(unformattedSubject == null)
							formattedSubject = unformattedSubject;
						else
							formattedSubject = bean.getFormattedMessage(unformattedSubject, subjectValuesExp, tkn, mvCtx);
						
						Message message = messageBusiness.createUserMessage(theCase, user, null, null, formattedSubject, formattedMsg, formattedMsg, null, false, null, false, true);
						message.store();
					}
					
				} catch (RemoteException e) {
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
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getSubjectKey() {
		return subjectKey;
	}

	public void setSubjectKey(String subjectKey) {
		this.subjectKey = subjectKey;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getSubjectValues() {
		return subjectValues;
	}

	public void setSubjectValues(String subjectValues) {
		this.subjectValues = subjectValues;
	}

	public String getMessageValues() {
		return messageValues;
	}

	public void setMessageValues(String messageValues) {
		this.messageValues = messageValues;
	}

	public String getMessagesBundle() {
		return messagesBundle;
	}

	public void setMessagesBundle(String messagesBundle) {
		this.messagesBundle = messagesBundle;
	}
}