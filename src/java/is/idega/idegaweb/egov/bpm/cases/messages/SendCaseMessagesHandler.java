package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWBundle;
import com.idega.presentation.IWContext;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/09/09 06:02:43 $ by $Author: arunas $
 */
public class SendCaseMessagesHandler implements ActionHandler {

	private static final long serialVersionUID = 1212382470685233437L;
	
	private String subjectKey;
	private String subjectValues;
	private String messageKey;
	private String messageValues;
	private String messagesBundle;
	private String sendToRoles;
	private String sendFromProcessInstanceExp;
	private Map<String, String> inlineSubject;
	private Map<String, String> inlineMessage;
	
	@Autowired
	private SendMessage sendMessage;
	
	public String getSendToRoles() {
		return sendToRoles;
	}

	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}

	public void execute(ExecutionContext ectx) throws Exception {
	
		ELUtil.getInstance().autowire(this);
		
		final String sendToRoles = (String)JbpmExpressionEvaluator.evaluate(getSendToRoles(), ectx);
		
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
//			TODO: propagate searching to the last super token
			
			if(superToken != null) {
				
//				found super process, trying to get variable from there
				candPI = superToken.getProcessInstance();
				caseIdStr = (String)candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
				
			} else {
				
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Case id not found in the process instance ("+candPI.getId()+"), and no superprocess found");
				return;
			}
			
			if(caseIdStr == null) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Case id not found in the process instance ("+candPI.getId()+")");
				return;
			}
		}
		
		final ProcessInstance pi = candPI;
		final Token tkn = ectx.getToken();
		
		getSendMessage().send(pi, new Integer(caseIdStr), getLocalizedMessages(), tkn, sendToRoles);
	}
	
	protected LocalizedMessages getLocalizedMessages() {
		
		final LocalizedMessages msgs = new LocalizedMessages();
		
		msgs.setSubjectValuesExp(getSubjectValues());
		msgs.setMessageValuesExp(getMessageValues());
		
		if(getMessageKey() == null && getSubjectKey() == null) {
//			using inline messages
			
			if(getInlineSubject() != null && !getInlineSubject().isEmpty()) {
			
				HashMap<Locale, String> subjects = new HashMap<Locale, String>(getInlineSubject().size());
				
				for (Entry<String, String> entry : getInlineSubject().entrySet()) {
				    
				    	Locale subjectLocale = ICLocaleBusiness.getLocaleFromLocaleString(entry.getKey());
				    	subjects.put(subjectLocale, entry.getValue());
				}
				
				msgs.setInlineSubjects(subjects);
			}
			
			if(getInlineMessage() != null && !getInlineMessage().isEmpty()) {
				
				HashMap<Locale, String> messages = new HashMap<Locale, String>(getInlineMessage().size());
				
				for (Entry<String, String> entry : getInlineMessage().entrySet()) {
					Locale msgLocale = ICLocaleBusiness.getLocaleFromLocaleString(entry.getKey());
					messages.put(msgLocale, entry.getValue());
				}
				
				msgs.setInlineMessages(messages);
			}

		} else {
//			using message keys
			
			String bundleIdentifier = getMessagesBundle();
		
			if(bundleIdentifier == null)
				bundleIdentifier = IWBundleStarter.IW_BUNDLE_IDENTIFIER;
			
			final IWBundle iwb = IWContext.getCurrentInstance().getIWMainApplication().getBundle(bundleIdentifier);
			msgs.setIwb(iwb);
			msgs.setSubjectKey(getSubjectKey());
			msgs.setMsgKey(getMessageKey());
		}
		
		return msgs;
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
	
	public class LocalizedMessages {
		
		private String subjectValuesExp;
		private String messageValuesExp;
		
		private IWBundle iwb;
		
		private String subjectKey;
		private String msgKey;
		
		private Map<Locale, String> inlineSubjects;
		private Map<Locale, String> inlineMessages;
		
		public void setSubjectKey(String subjectKey) {
			this.subjectKey = subjectKey;
		}
		public void setMsgKey(String msgKey) {
			this.msgKey = msgKey;
		}
		public void setInlineSubjects(Map<Locale, String> inlineSubjects) {
			this.inlineSubjects = inlineSubjects;
		}
		public void setInlineMessages(Map<Locale, String> inlineMessages) {
			this.inlineMessages = inlineMessages;
		}
		public void setIwb(IWBundle iwb) {
			this.iwb = iwb;
		}
		
		public String getLocalizedSubject(Locale locale) {
		
			if(iwb != null) {
				return iwb.getResourceBundle(locale).getLocalizedString(subjectKey, subjectKey);
			} else if (inlineSubjects != null) {
				return inlineSubjects.get(locale);
			} else {
				
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get localized subject, but neither iwb, nor inlineSubjects set");
				return null;
			}
		}
		
		public String getLocalizedMessage(Locale locale) {
			
			if(iwb != null) {
				return iwb.getResourceBundle(locale).getLocalizedString(msgKey, msgKey);
			} else if (inlineMessages != null) {
				return inlineMessages.get(locale);
			} else {
				
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get localized message, but neither iwb, nor inlineMessages set");
				return null;
			}
		}
		public String getSubjectValuesExp() {
			return subjectValuesExp;
		}
		public void setSubjectValuesExp(String subjectValuesExp) {
			this.subjectValuesExp = subjectValuesExp;
		}
		public String getMessageValuesExp() {
			return messageValuesExp;
		}
		public void setMessageValuesExp(String messageValuesExp) {
			this.messageValuesExp = messageValuesExp;
		}
	}

	public SendMessage getSendMessage() {
		return sendMessage;
	}

	public void setSendMessage(SendMessage sendMessage) {
		this.sendMessage = sendMessage;
	}

	public Map<String, String> getInlineSubject() {
		return inlineSubject;
	}

	public void setInlineSubject(Map<String, String> inlineSubject) {
		this.inlineSubject = inlineSubject;
	}

	public Map<String, String> getInlineMessage() {
		return inlineMessage;
	}

	public void setInlineMessage(Map<String, String> inlineMessage) {
		this.inlineMessage = inlineMessage;
	}
}