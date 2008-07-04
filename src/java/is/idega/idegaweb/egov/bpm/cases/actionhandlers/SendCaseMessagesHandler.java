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
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.idega.block.process.message.data.Message;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2008/07/04 10:49:02 $ by $Author: civilis $
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
	private String sendFromProcessInstanceExp;
	
	public String getSendToRoles() {
		return sendToRoles;
	}

	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}

	public void execute(ExecutionContext ectx) throws Exception {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		final IWContext iwc = IWContext.getIWContext(fctx);
		final String sendToRoles = (String)JbpmExpressionEvaluator.evaluate(getSendToRoles(), ectx);
		final ProcessInstance pi;
		ProcessInstance candPI;
		String caseIdStr;
		
		if(getSendFromProcessInstanceExp() != null) {

//			resolving candidate process instance from expression, if present
			candPI = (ProcessInstance)JbpmExpressionEvaluator.evaluate(getSendFromProcessInstanceExp(), ectx);
			caseIdStr = (String)candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
			
		} else {
			
//			using current process instance candidate process instance
			candPI = ectx.getProcessInstance();
			caseIdStr = (String)ectx.getVariable(CasesBPMProcessConstants.caseIdVariableName);
		}
		
		if(caseIdStr == null) {
			
//			no case id variable found, trying to get it from super process

			Token superToken = candPI.getSuperProcessToken();
			
			if(superToken != null) {
				
//				found super process, trying to get variable from there
				candPI = superToken.getProcessInstance();
				caseIdStr = (String)candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
				
			} else {
				
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Case id not found in the process instance ("+candPI.getId()+"), and no superprocess found");
				return;
			}
			
			if(candPI == null) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Case id not found in the process instance ("+candPI.getId()+")");
				return;
			}
		}
		
		pi = candPI;
		
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		
		final GeneralCase theCase = casesBusiness.getGeneralCase(new Integer(caseIdStr));
		final CommuneMessageBusiness messageBusiness = getCommuneMessageBusiness(iwc);
		final UserBusiness userBusiness  = getUserBusiness(iwc);
		
		
		final String subjectKey = getSubjectKey();
		final String msgKey = getMessageKey();

		final String subjectValuesExp = getSubjectValues();
		final String messageValuesExp = getMessageValues();
		final Token tkn = ectx.getToken();
		String bundleIdentifier = getMessagesBundle();
		
		if(bundleIdentifier == null)
			bundleIdentifier = IWBundleStarter.IW_BUNDLE_IDENTIFIER;
		
		final IWBundle iwb = iwc.getIWMainApplication().getBundle(bundleIdentifier);
		final Locale defaultLocale = iwc.getCurrentLocale();
		
		new Thread(new Runnable() {

			public void run() {
				
				try {
					SendCaseMessagesHandlerBean bean = (SendCaseMessagesHandlerBean)WFUtil.getBeanInstance(iwc, SendCaseMessagesHandlerBean.beanIdentifier);
					Collection<User> users = bean.getUsersToSendMessageTo(iwc, sendToRoles, pi);
					
					HashMap<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);
					MessageValueContext mvCtx = new MessageValueContext(5);
					
					for (User user : users) {
						
						Locale preferredLocale = userBusiness.getUsersPreferredLocale(user);
						
						if(preferredLocale == null)
							preferredLocale = defaultLocale;
						
						String unformattedSubject;
						String unformattedMsg;
						
						if(!unformattedForLocales.containsKey(preferredLocale)) {
						
							IWResourceBundle iwrb = iwb.getResourceBundle(preferredLocale);
						
							unformattedSubject = iwrb.getLocalizedString(subjectKey, subjectKey);
							unformattedMsg = iwrb.getLocalizedString(msgKey, msgKey);
							
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

	/**
	 * If send not from current process. Optional
	 * @return Expression to resolve process instance
	 */
	public String getSendFromProcessInstanceExp() {
		return sendFromProcessInstanceExp;
	}

	public void setSendFromProcessInstanceExp(String sendFromProcessInstanceExp) {
		this.sendFromProcessInstanceExp = sendFromProcessInstanceExp;
	}
}