package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.Locale;

import junit.framework.AssertionFailedError;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.SendMessage;
import com.idega.jbpm.process.business.messages.MessageValueContext;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/10/22 15:03:09 $ by $Author: civilis $
 */
public class SendMessageMockupImpl implements SendMessage {
	
	public void send(MessageValueContext mvCtx, final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn) {

		if(!"english message".equals(msgs.getLocalizedMessage(new Locale("en"))))
			throw new AssertionFailedError();
		if(!"icelandic message".equals(msgs.getLocalizedMessage(new Locale("is","IS"))))
			throw new AssertionFailedError();
		if(!"english subject".equals(msgs.getLocalizedSubject(new Locale("en"))))
			throw new AssertionFailedError();
		if(!"icelandic subject".equals(msgs.getLocalizedSubject(new Locale("is", "IS"))))
			throw new AssertionFailedError();
	}

	@Override
	public String getSubject() {
		return null;
	}
}