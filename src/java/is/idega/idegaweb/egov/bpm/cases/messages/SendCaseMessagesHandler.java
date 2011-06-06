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

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 * 
 *          Last modified: $Date: 2008/12/05 05:45:47 $ by $Author: civilis $
 */
@Service("sendCaseMessagesHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SendCaseMessagesHandler extends SendMessagesHandler {

	private static final long serialVersionUID = 1212382470685233437L;

	private static final Logger LOGGER = Logger.getLogger(SendCaseMessagesHandler.class.getName());
	
	private SendMessage sendMessage;
	
	/**
	 * defines the process instance, from which the message is sent. The case id
	 * is resolved by this process instance
	 */
	private Long processInstanceId;

	@Override
	public void execute(ExecutionContext ectx) throws Exception {
		final String sendToRoles = getSendToRoles();
		final Integer recipientUserId = getRecipientUserID();

		ProcessInstance candPI;
		String caseIdStr;

		if (getProcessInstanceId() != null) {
			// resolving candidate process instance from expression, if present
			candPI = ectx.getJbpmContext().getProcessInstance(getProcessInstanceId());
			caseIdStr = (String) candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
		} else {
			// using current process instance candidate process instance
			candPI = ectx.getProcessInstance();
			caseIdStr = (String) ectx.getVariable(CasesBPMProcessConstants.caseIdVariableName);
		}

		if (caseIdStr == null) {
			// no case id variable found, trying to get it from super process
			Token superToken = candPI.getSuperProcessToken();
			
			// TODO: propagate searching to the last super token
			if (superToken != null) {
				// found super process, trying to get variable from there
				candPI = superToken.getProcessInstance();
				caseIdStr = (String) candPI.getContextInstance().getVariable(CasesBPMProcessConstants.caseIdVariableName);
			} else {
				LOGGER.warning("Case id not found in the process instance (" + candPI.getId() + "), and no superprocess found");
				return;
			}

			if (caseIdStr == null) {
				LOGGER.warning("Case id not found in the process instance (" + candPI.getId() + ")");
				return;
			}
		}

		final ProcessInstance pi = candPI;
		final Token tkn = ectx.getToken();

		LocalizedMessages msgs = getLocalizedMessages();
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