package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.Locale;

import junit.framework.AssertionFailedError;

import is.idega.idegaweb.egov.bpm.cases.messages.SendMessage;
import is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler.LocalizedMessages;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/07 09:38:43 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class SendMessageMockupImpl implements SendMessage {
	
	public void send(final ProcessInstance pi, final Integer caseId, final LocalizedMessages msgs, final Token tkn, final String sendToRoles) {

		if(!"english message".equals(msgs.getLocalizedMessage(new Locale("en"))))
			throw new AssertionFailedError();
		if(!"icelandic message".equals(msgs.getLocalizedMessage(new Locale("is_IS"))))
			throw new AssertionFailedError();
		if(!"english subject".equals(msgs.getLocalizedSubject(new Locale("en"))))
			throw new AssertionFailedError();
		if(!"icelandic subject".equals(msgs.getLocalizedSubject(new Locale("is_IS"))))
			throw new AssertionFailedError();
	}
}