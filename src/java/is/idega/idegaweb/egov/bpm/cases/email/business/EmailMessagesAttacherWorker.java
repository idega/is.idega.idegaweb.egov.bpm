package is.idega.idegaweb.egov.bpm.cases.email.business;

import is.idega.idegaweb.egov.bpm.cases.email.bean.BPMEmailMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.business.EmailsParsersProvider;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.parser.EmailParser;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.core.messaging.EmailMessage;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

/**
 * Resolves messages to attach and attaches
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:56:21 $ by $Author: valdas $
 */

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EmailMessagesAttacherWorker implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(EmailMessagesAttacherWorker.class.getName());
	
	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private BPMFactory bpmFactory;
	
	private ApplicationEmailEvent emailEvent;
	
	public EmailMessagesAttacherWorker(ApplicationEmailEvent emailEvent) {
		this.emailEvent = emailEvent;
		
		ELUtil.getInstance().autowire(this);
	}
	
	public void run() {
		parseAndAttachMessages();
	}

	@SuppressWarnings("unchecked")
	private void parseAndAttachMessages() {
		Map<String, FoundMessagesInfo> messagesToParse = new HashMap<String, FoundMessagesInfo>();
		
		Map<String, FoundMessagesInfo> messagesInfo = emailEvent.getMessages();
		if (messagesInfo != null && !messagesInfo.isEmpty()) {
			for (Entry<String, FoundMessagesInfo> entry: messagesInfo.entrySet()) {
				if (entry.getValue().getParserType() == MessageParserType.BPM) {
					messagesToParse.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		Map<Object, Object> parsersProviders = null;
		try {
			parsersProviders = WebApplicationContextUtils.getWebApplicationContext(IWMainApplication.getDefaultIWMainApplication()
					.getServletContext()).getBeansOfType(EmailsParsersProvider.class);
		} catch(BeansException e) {
			LOGGER.log(Level.SEVERE, "Error getting beans of type: " + EmailsParsersProvider.class, e);
		}
		if (parsersProviders == null || parsersProviders.isEmpty()) {
			return;
		}
		
		EmailParams params = emailEvent.getEmailParams();
		
		Collection<BPMEmailMessage> allParsedMessages = new ArrayList<BPMEmailMessage>();
		for (Object bean: parsersProviders.values()) {
			if (bean instanceof EmailsParsersProvider) {
				for (EmailParser parser: ((EmailsParsersProvider) bean).getAllParsers()) {
					Collection<? extends EmailMessage> parsedMessages = parser.getParsedMessagesCollection(messagesToParse, params);
					addParsedMessages(allParsedMessages, parsedMessages);
					
					parsedMessages = parser.getParsedMessages(emailEvent);
					addParsedMessages(allParsedMessages, parsedMessages);
				}
			}
		}
		
		if (ListUtil.isEmpty(allParsedMessages)) {
			return;
		}
		
		final Collection<BPMEmailMessage> messagesToAttach = allParsedMessages;
		getIdegaJbpmContext().execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				for (BPMEmailMessage message: messagesToAttach) {
					if (!attachEmailMessage(context, message)) {
						//	TODO: save message and try attach later?
					}
				}
				return null;
			}
		});
	}
	
	private void addParsedMessages(Collection<BPMEmailMessage> allParsedMessages, Collection<? extends EmailMessage> parsedMessages) {
		if (ListUtil.isEmpty(parsedMessages)) {
			return;
		}
		
		for (EmailMessage message: parsedMessages) {
			if (message instanceof BPMEmailMessage && !allParsedMessages.contains(message)) {
				allParsedMessages.add((BPMEmailMessage) message);
			}
		}
	}
	
	@Transactional
	private boolean attachEmailMessage(JbpmContext ctx, BPMEmailMessage message) {
		if (ctx == null || message == null) {
			return false;
		}
		
		ProcessInstance pi = ctx.getProcessInstance(message.getProcessInstanceId());
		@SuppressWarnings("unchecked")
		List<Token> tkns = pi.findAllTokens();
		if (ListUtil.isEmpty(tkns)) {
			return false;
		}
		
		for (Token tkn : tkns) {
			
			ProcessInstance subPI = tkn.getSubProcessInstance();
			
			if (subPI != null && EmailMessagesAttacher.email_fetch_process_name.equals(subPI.getProcessDefinition().getName())) {
				
				try {
					TaskInstance ti = subPI.getTaskMgmtInstance().createStartTaskInstance();
					String subject = message.getSubject();
					ti.setName(subject);
					
					String text = message.getBody();
					
					if (text == null)
						text = CoreConstants.EMPTY;
					
					HashMap<String, Object> vars = new HashMap<String, Object>(2);
					
					String senderPersonalName = message.getSenderName();
					String fromAddress = message.getFromAddress();
					
					vars.put("string_subject", subject);
					vars.put("string_text", text);
					vars.put("string_fromPersonal", senderPersonalName);
					vars.put("string_fromAddress", fromAddress);
					
					BPMFactory bpmFactory = getBpmFactory();
					
					// taking here view for new task instance
					getBpmFactory().takeView(ti.getId(), false, null);
					
					long pdId = ti.getProcessInstance().getProcessDefinition().getId();
					
					ViewSubmission emailViewSubmission = getBpmFactory().getViewSubmission();
					emailViewSubmission.populateVariables(vars);
					
					TaskInstanceW taskInstance = bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId());
					taskInstance.submit(emailViewSubmission, false);
					
					Map<String, InputStream> attachments = message.getAttachments();
					Collection<File> attachedFiles = message.getAttachedFiles();
					if (!ListUtil.isEmpty(attachedFiles)) {
						for (File attachedFile: attachedFiles) {
							if (attachedFile != null) {
								if (attachments == null) {
									attachments = new HashMap<String, InputStream>(1);
								}
								attachments.put(attachedFile.getName(), new FileInputStream(attachedFile));
							}
						}
					}
					
					if (attachments != null && !attachments.isEmpty()) {
						Variable variable = new Variable("attachments", VariableDataType.FILES);
						
						InputStream fileStream = null;
						for (String fileName : attachments.keySet()) {
							fileStream = attachments.get(fileName);
							try {
								taskInstance.addAttachment(variable, fileName, fileName, fileStream);
							} catch (Exception e) {
								LOGGER.log(Level.SEVERE, "Unable to set binary variable for task instance: " + ti.getId(), e);
							} finally {
								IOUtil.closeInputStream(fileStream);
								if (!ListUtil.isEmpty(attachedFiles)) {
									for (File attachedFile: attachedFiles) {
										attachedFile.delete();
									}
								}
							}
						}
					}
					
					return true;
					
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Exception while attaching email msg", e);
				}
			}
		}
		
		return false;
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
}
