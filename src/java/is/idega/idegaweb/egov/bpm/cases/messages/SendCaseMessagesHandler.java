package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.SendMessage;
import com.idega.bpm.process.messages.SendMessageType;
import com.idega.bpm.process.messages.SendMessagesHandler;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.TypeRef;
import com.idega.util.CoreUtil;
import com.idega.util.EmailValidator;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 *          Last modified: $Date: 2008/12/05 05:45:47 $ by $Author: civilis $
 */
@Service(SendCaseMessagesHandler.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SendCaseMessagesHandler extends SendMessagesHandler {

	private static final long serialVersionUID = 1212382470685233437L;

	private static final Logger LOGGER = Logger.getLogger(SendCaseMessagesHandler.class.getName());

	public static final String BEAN_NAME = "sendCaseMessagesHandler";

	private SendMessage sendMessage;

	private SendMessage emailSender;

	@Autowired
	private BPMFactory bpmFactory;

	/**
	 * defines the process instance, from which the message is sent. The case id
	 * is resolved by this process instance
	 */
	private Long processInstanceId;

	private Token token;

	private String sendToEmail, sendToCCEmail;

	public void setToken(Token token) {
		this.token = token;
	}

	@Override
	public void execute(ExecutionContext ectx) throws Exception {
		final String sendToRoles = getSendToRoles();
		final Integer recipientUserId = getRecipientUserID();
		LOGGER.info("Will send message to role(s) " + sendToRoles + " or/and user " + recipientUserId);

		ProcessInstance candPI;
		String caseIdStr;

		if (getProcessInstanceId() != null) {
			// resolving candidate process instance from expression, if present
			if (ectx == null) {
				candPI = bpmFactory.getProcessManagerByProcessInstanceId(getProcessInstanceId()).getProcessInstance(getProcessInstanceId())
						.getProcessInstance();
			} else
				candPI = ectx.getJbpmContext().getProcessInstance(getProcessInstanceId());

			caseIdStr = (String) candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
		} else {
			// using current process instance candidate process instance
			candPI = ectx.getProcessInstance();
			caseIdStr = (String) ectx.getVariable(CasesBPMProcessConstants.caseIdVariableName);
		}

		LocalizedMessages msgs = getLocalizedMessages();
		if (msgs == null) {
			String msg = "Unable to resolve localized message, will not send message to role(s) " + sendToRoles + " or/and user with ID " +
					recipientUserId;
			LOGGER.warning(msg);
			CoreUtil.sendExceptionNotification(msg, null);
		}

		if (candPI == null) {
			String msg = "Unable to resolve process instance, will not send message to role(s) " + sendToRoles + " or/and user with ID " +
					recipientUserId + " and message:\n" + msgs;
			LOGGER.warning(msg);
			CoreUtil.sendExceptionNotification(msg, null);
		} else
			LOGGER.info("Sending message to process instance " + candPI.getId());

		if (caseIdStr == null) {
			// no case id variable found, trying to get it from super process
			Token superToken = candPI.getSuperProcessToken();

			// TODO: propagate searching to the last super token
			if (superToken != null) {
				// found super process, trying to get variable from there
				candPI = superToken.getProcessInstance();
				caseIdStr = (String) candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
			} else {
				String msg = "Case id not found in the process instance (" + candPI.getId() +
						"), and no superprocess found, will not send message to role(s) " + sendToRoles + " or/and user with ID " +
						recipientUserId + " and message:\n" + msgs;
				LOGGER.warning(msg);
				CoreUtil.sendExceptionNotification(msg, null);
				return;
			}

			if (caseIdStr == null) {
				String msg = "Case id not found in the process instance (" + candPI.getId() + "), will not send message to role(s) " +
						sendToRoles + " or/and user with ID " + recipientUserId + " and message:\n" + msgs;
				LOGGER.warning(msg);
				CoreUtil.sendExceptionNotification(msg, null);
				return;
			}
		}

		final ProcessInstance pi = candPI;
		final Token tkn = ectx == null ? token : ectx.getToken();

		String sendToCCEmail = getSendToCCEmail();
		if (EmailValidator.getInstance().isValid(sendToCCEmail)) {
			List<String> sendCcEmails = msgs.getSendCcEmails();
			if (sendCcEmails == null) {
				sendCcEmails = new ArrayList<String>();
			} else {
				sendCcEmails = new ArrayList<String>(sendCcEmails);
			}
			sendCcEmails.add(sendToCCEmail);
			msgs.setSendCcEmails(sendCcEmails);
		}

		msgs.setSendToRoles(sendToRoles);
		msgs.setRecipientUserId(recipientUserId);
		boolean validSendToEmail = EmailValidator.getInstance().validateEmail(getSendToEmail());
		if (validSendToEmail) {
			msgs.setSendToEmails(Arrays.asList(getSendToEmail()));
		}
		getSendMessage().send(null, Integer.valueOf(caseIdStr), pi, msgs, tkn);

		if (isSendViaEmail() || validSendToEmail) {
			MessageValueContext mvCtx = new MessageValueContext();
			CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
			Case theCase = null;
			try {
				theCase = caseBusiness.getCase(caseIdStr);
			} catch (Exception e) {
				getLogger().warning("Error getting case by ID: " + caseIdStr);
			}
			if (theCase != null) {
				Timestamp creationDate = theCase.getCreated();
				if (creationDate != null) {
					IWTimestamp iwCreationDate = new IWTimestamp(creationDate);
					Locale locale = getCurrentLocale();
					mvCtx.setValue(TypeRef.CREATION_DATE, iwCreationDate.getLocaleDate(locale, DateFormat.MEDIUM));
					mvCtx.setValue(TypeRef.CREATION_TIME, iwCreationDate.getLocaleTime(locale, DateFormat.FULL));
				}
			}

			getEmailSender().send(mvCtx, ectx, pi, msgs, tkn);
		}
	}

	public SendMessage getEmailSender() {
		return emailSender;
	}

	@Autowired
	public void setEmailSender(@SendMessageType("email") SendMessage emailSender) {
		this.emailSender = emailSender;
	}

	@Override
	public SendMessage getSendMessage() {
		return sendMessage;
	}

	@Override
	@Autowired
	public void setSendMessage(
		@SendMessageType("caseMessage") SendMessage sendMessage) {
		this.sendMessage = sendMessage;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public String getSendToEmail() {
		return sendToEmail;
	}

	public void setSendToEmail(String sendToEmail) {
		this.sendToEmail = sendToEmail;
	}

	public String getSendToCCEmail() {
		return sendToCCEmail;
	}

	public void setSendToCCEmail(String sendToCCEmail) {
		this.sendToCCEmail = sendToCCEmail;
	}

}