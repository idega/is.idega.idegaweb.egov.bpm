package is.idega.idegaweb.egov.bpm.cases.email.business;

import is.idega.idegaweb.egov.bpm.cases.email.bean.BPMEmailMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.idega.bpm.BPMConstants;
import com.idega.core.messaging.EmailMessage;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.datastructures.map.MapUtil;
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
	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	private ApplicationEmailEvent emailEvent;

	public EmailMessagesAttacherWorker(ApplicationEmailEvent emailEvent) {
		this.emailEvent = emailEvent;

		ELUtil.getInstance().autowire(this);
	}

	@Override
	public void run() {
		parseAndAttachMessages();
	}

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

		Map<?, ?> parsersProviders = null;
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

		for (final BPMEmailMessage message: allParsedMessages) {
			try {
				if (!doAttachMessageIfNeeded(message)) {
					//	TODO: save message and try attach later?
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error attaching message " + message, e);
			}
		}
	}

	private void addParsedMessages(Collection<BPMEmailMessage> allParsedMessages, Collection<? extends EmailMessage> parsedMessages) {
		if (ListUtil.isEmpty(parsedMessages))
			return;

		for (EmailMessage message: parsedMessages) {
			if (message instanceof BPMEmailMessage && !allParsedMessages.contains(message)) {
				allParsedMessages.add((BPMEmailMessage) message);
			}
		}
	}

	private boolean doAttachMessageIfNeeded(BPMEmailMessage message) {
		if (message == null || message.isParsed()) {
			return true;
		}

		List<Long> subProcInstIds = getBpmFactory().getBPMDAO().getSubProcInstIdsByParentProcInstIdAndProcDefName(
				message.getProcessInstanceId(),
				EmailMessagesAttacher.email_fetch_process_name
		);
		if (ListUtil.isEmpty(subProcInstIds)) {
			return false;
		}
		for (Long subProcInstId: subProcInstIds) {
			if (subProcInstId == null) {
				continue;
			}

			String subject = message.getSubject();
			String text = message.getBody();
			if (text == null) {
				text = CoreConstants.EMPTY;
			}
			String senderPersonalName = message.getSenderName();
			String fromAddress = message.getFromAddress();

			Map<String, List<Serializable>> variablesWithValues = new HashMap<String, List<Serializable>>();
			variablesWithValues.put(BPMConstants.VAR_SUBJECT, Arrays.asList((Serializable) subject));
			variablesWithValues.put(BPMConstants.VAR_FROM, Arrays.asList((Serializable) senderPersonalName));
			variablesWithValues.put(BPMConstants.VAR_FROM_ADDRESS, Arrays.asList((Serializable) fromAddress));
			Map<Long, Map<String, VariableInstanceInfo>> vars = variablesQuerier.getVariablesByNamesAndValuesByProcesses(
					variablesWithValues,
					Arrays.asList(BPMConstants.VAR_TEXT),
					null,
					Arrays.asList(subProcInstId),
					null
			);
			if (!MapUtil.isEmpty(vars)) {
				for (Map<String, VariableInstanceInfo> existingValues: vars.values()) {
					VariableInstanceInfo existingVar = existingValues.get(BPMConstants.VAR_TEXT);
					if (existingVar != null && text.startsWith(existingVar.getValue().toString())) {
						LOGGER.warning("BPM message (subject: '" + subject + "', text: '" + text + "', from: '" + senderPersonalName +
								"', address: '" + fromAddress+ "') is duplicated, dropping it");
						message.setParsed(true);
						return true;
					}
				}
			}

			Map<String, InputStream> attachments = message.getAttachments();
			if (attachments == null) {
				attachments = new HashMap<String, InputStream>(1);
			}
			if (doAttachEmailToProcess(
					subProcInstId,
					subject, text,
					senderPersonalName,
					fromAddress,
					attachments,
					message.getAttachedFiles()
			)) {
				message.setParsed(true);
			} else {
				return false;
			}
		}
		return true;
	}

	@Transactional
	private Boolean doAttachEmailToProcess(
			final Long subProcInstId,
			final String subject,
			final String text,
			final String senderPersonalName,
			final String fromAddress,
			final Map<String, InputStream> attachments,
			final Collection<File> attachedFiles
	) {
		Boolean result = getIdegaJbpmContext().execute(new JbpmCallback() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				try {
					ProcessInstance subPI = context.getProcessInstance(subProcInstId);
					TaskInstance ti = subPI.getTaskMgmtInstance().createStartTaskInstance();
					ti.setName(subject);

					Map<String, Object> newVars = new HashMap<String, Object>(2);
					newVars.put(BPMConstants.VAR_SUBJECT, subject);
					newVars.put(BPMConstants.VAR_TEXT, text);
					newVars.put(BPMConstants.VAR_FROM, senderPersonalName);
					newVars.put(BPMConstants.VAR_FROM_ADDRESS, fromAddress);

					// taking here view for new task instance
					long tiId = ti.getId();
					View view = getBpmFactory().takeView(tiId, false, null);
					LOGGER.info("Task instance ID for email message to attach: " + tiId + ". View: " + view);

					long pdId = ti.getProcessInstance().getProcessDefinition().getId();

					ViewSubmission emailViewSubmission = getBpmFactory().getViewSubmission();
					emailViewSubmission.populateVariables(newVars);

					TaskInstanceW taskInstance = bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId());
					taskInstance.submit(emailViewSubmission, false);
					LOGGER.info("Submitted task instance " + taskInstance.getTaskInstanceId() + " with data from email message (sender: " + fromAddress +
							", subject: " + subject + ") for process instance: " + taskInstance.getProcessInstanceW().getProcessInstanceId());

					if (!ListUtil.isEmpty(attachedFiles)) {
						for (File attachedFile: attachedFiles) {
							if (attachedFile != null) {
								attachments.put(attachedFile.getName(), new FileInputStream(attachedFile));
							}
						}
					}
					if (!MapUtil.isEmpty(attachments)) {
						Variable variable = new Variable("attachments", VariableDataType.FILES);

						InputStream fileStream = null;
						for (String fileName: attachments.keySet()) {
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
					LOGGER.log(Level.SEVERE, "Exception while attaching email msg (subject: " + subject + ", text: " +
							text + "). Sub-process ID: " + subProcInstId, e);
				}
				return false;
			}
		});
		return result;
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