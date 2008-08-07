package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.messages.SendMessage;
import is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler.LocalizedMessages;

import java.util.Locale;

import junit.framework.AssertionFailedError;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/08/07 18:06:01 $ by $Author: civilis $
 */
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