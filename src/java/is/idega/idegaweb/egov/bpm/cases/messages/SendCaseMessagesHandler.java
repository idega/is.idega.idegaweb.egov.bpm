package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.SendMessage;
import com.idega.bpm.process.messages.SendMessageType;
import com.idega.bpm.process.messages.SendMessagesHandler;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/10/22 15:02:44 $ by $Author: civilis $
 */
public class SendCaseMessagesHandler extends SendMessagesHandler {

	private static final long serialVersionUID = 1212382470685233437L;

	private SendMessage sendMessage;
	
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
		
		LocalizedMessages msgs = getLocalizedMessages();
		msgs.setSendToRoles(sendToRoles);
		getSendMessage().send(null, new Integer(caseIdStr), pi, msgs, tkn);
	}

	public SendMessage getSendMessage() {
		return sendMessage;
	}

	@Autowired
	public void setSendMessage(@SendMessageType("caseMessage") SendMessage sendMessage) {
		this.sendMessage = sendMessage;
	}
}