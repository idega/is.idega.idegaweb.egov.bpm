package is.idega.idegaweb.egov.bpm.cases.email.business;

import is.idega.idegaweb.egov.bpm.cases.email.parsers.AttachedMessagesParser;
import is.idega.idegaweb.egov.bpm.cases.email.parsers.MessageAttributesParser;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.business.EmailsParsersProvider;
import com.idega.block.email.parser.EmailParser;

/**
 * Provides e-mails' parsers
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/04/22 14:44:40 $ by $Author: valdas $
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class EmailMessagesParsersProvider implements EmailsParsersProvider {

	@Autowired
	private AttachedMessagesParser attachedMessagesParser;
	
	@Autowired
	private MessageAttributesParser messageAttributesParser;
	
	public List<EmailParser> getAllParsers() {
		return Arrays.asList(
				(EmailParser) getAttachedMessagesParser(),
				(EmailParser) getMessageAttributesParser()
		);
	}

	public AttachedMessagesParser getAttachedMessagesParser() {
		return attachedMessagesParser;
	}

	public void setAttachedMessagesParser(AttachedMessagesParser attachedMessagesParser) {
		this.attachedMessagesParser = attachedMessagesParser;
	}

	public MessageAttributesParser getMessageAttributesParser() {
		return messageAttributesParser;
	}

	public void setMessageAttributesParser(MessageAttributesParser messageAttributesParser) {
		this.messageAttributesParser = messageAttributesParser;
	}
	
}
