package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.SendMessage;
import com.idega.bpm.process.messages.SendMessageType;
import com.idega.bpm.process.messages.SendMessagesHandler;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.util.CoreUtil;

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

	@Autowired
	private BPMFactory bpmFactory;

	/**
	 * defines the process instance, from which the message is sent. The case id
	 * is resolved by this process instance
	 */
	private Long processInstanceId;

	private Token token;

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

		msgs.setSendToRoles(sendToRoles);
		msgs.setRecipientUserId(recipientUserId);
		getSendMessage().send(null, new Integer(caseIdStr), pi, msgs, tkn);
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
}