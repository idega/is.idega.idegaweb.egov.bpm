package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.Locale;

import junit.framework.AssertionFailedError;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.SendMessage;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/09/19 15:19:29 $ by $Author: civilis $
 */
public class SendMessageMockupImpl implements SendMessage {
	
	public void send(final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn) {

		if(!"english message".equals(msgs.getLocalizedMessage(new Locale("en"))))
			throw new AssertionFailedError();
		if(!"icelandic message".equals(msgs.getLocalizedMessage(new Locale("is","IS"))))
			throw new AssertionFailedError();
		if(!"english subject".equals(msgs.getLocalizedSubject(new Locale("en"))))
			throw new AssertionFailedError();
		if(!"icelandic subject".equals(msgs.getLocalizedSubject(new Locale("is", "IS"))))
			throw new AssertionFailedError();
	}
}