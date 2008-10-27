package is.idega.idegaweb.egov.bpm.cases.email;

import is.idega.idegaweb.egov.bpm.cases.exe.CaseIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.xformsview.IXFormViewFactory;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.artifacts.impl.ProcessArtifactsProviderImpl;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.View;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.StringUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2008/10/27 14:21:11 $ by $Author: juozas $
 */
@Scope("singleton")
@Service
public class EmailMessagesAttacher implements ApplicationListener {
	
	private CasesBPMDAO casesBPMDAO;
	private BPMContext idegaJbpmContext;
	private BPMFactory bpmFactory;
	private IXFormViewFactory xfvFact;
	private TmpFilesManager fileUploadManager;
	private TmpFileResolver uploadedResourceResolver;
	
	private static final String TEXT_PLAIN_TYPE = "text/plain";
	private static final String MULTIPART_MIXED_TYPE = "multipart/Mixed";
	private static final String TEXT_HTML_TYPE = "text/html";
	private static final String MULTI_ALTERNATIVE_TYPE = "multipart/alternative";
	private static final String HTML_EXTENSION = ".html";

	public void onApplicationEvent(ApplicationEvent ae) {
		
		if(ae instanceof ApplicationEmailEvent) {
			
			ApplicationEmailEvent ev = (ApplicationEmailEvent)ae;
			Map<String, Message> msgs = ev.getMessages();
			HashSet<Date> dates = new HashSet<Date>(msgs.size());
			HashSet<Integer> identifierIDs = new HashSet<Integer>(msgs.size());
			HashMap<PISFORMSG, Message> PISFORMSGMessage = new HashMap<PISFORMSG, Message>(msgs.size()); 
			
			for (Entry<String, Message> entry : msgs.entrySet()) {
				
				if(entry.getKey().startsWith(CaseIdentifier.IDENTIFIER_PREFIX)) {
					
					try {
						String[] keyParts = entry.getKey().split(CoreConstants.MINUS);
						
						String yearStr = keyParts[1];
						String monthStr = keyParts[2];
						String dayStr = keyParts[3];
						String identifierIDStr = keyParts[4];
						
						IWTimestamp iwt = new IWTimestamp(new Integer(yearStr), new Integer(monthStr), new Integer(dayStr));
						iwt.setYear(new Integer(yearStr));
						iwt.setMonth(new Integer(monthStr));
						iwt.setDay(new Integer(dayStr));
						
						Date date = iwt.getDate();
						Integer identifierID = new Integer(identifierIDStr);
						
						dates.add(date);
						identifierIDs.add(identifierID);
						
						PISFORMSGMessage.put(new PISFORMSG(date, identifierID, null), entry.getValue());
						
					} catch (Exception e) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while parsing identifier: "+entry, e);
					}
				}
			}
			
			if(!dates.isEmpty() && !identifierIDs.isEmpty()) {
				
				Set<PISFORMSG> pisformsgs = resolveProcessInstances(dates, identifierIDs);
				
				if(!pisformsgs.isEmpty()) {
					
					JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
					
					try {
						for (PISFORMSG pisformsg : pisformsgs) {
							
							if(PISFORMSGMessage.containsKey(pisformsg))
								attachEmailMsg(ctx, PISFORMSGMessage.get(pisformsg), pisformsg.pi);
						}
					} finally {
						getIdegaJbpmContext().closeAndCommit(ctx);
					}
				}
			}
		}
	}
	
	protected void attachEmailMsg(JbpmContext ctx, Message msg, ProcessInstance prin) {
	
//		TODO: if attaching fails (exception or email subprocess not found), keep msg somewhere for later try
		
		//List<Token> tkns = getCasesBPMDAO().getCaseProcInstBindSubprocessBySubprocessName(prin.getId());
		
		ProcessInstance pi = ctx.getProcessInstance(prin.getId());
		@SuppressWarnings("unchecked")
		List<Token> tkns = pi.findAllTokens();
		
		if(tkns != null) {
			
			for (Token tkn : tkns) {
				
				ProcessInstance subPI = tkn.getSubProcessInstance();
				
				if(subPI != null && ProcessArtifactsProviderImpl.email_fetch_process_name.equals(subPI.getProcessDefinition().getName())) {

					try {
						TaskInstance ti = subPI.getTaskMgmtInstance().createStartTaskInstance();
						
				    	String subject = msg.getSubject();
				    	ti.setName(subject);
				    	
				    	Object[] msgAndAttachments = parseContent(msg, ti.getId());
				    	
				    	String text = (String)msgAndAttachments[0];
				    	
				    	if(text == null)
				    		text = CoreConstants.EMPTY;
				    	
				    	HashMap<String, Object> vars = new HashMap<String, Object>(2);
				    	
				    	Address[] froms = msg.getFrom();
				    	
				    	String fromPersonal = null;
				    	String fromAddress = null;
				    	
				    	for (Address address : froms) {
							
				    		if(address instanceof InternetAddress) {
				    			
				    			InternetAddress iaddr = (InternetAddress)address;
				    			fromAddress = iaddr.getAddress();
				    			fromPersonal = iaddr.getPersonal();
				    			break;
				    		}
						}
				    	
				    	vars.put("string_subject", subject);
				    	vars.put("string_text", text);
				    	vars.put("string_fromPersonal", fromPersonal);
				    	vars.put("string_fromAddress", fromAddress);
				    	
						BPMFactory bpmFactory = getBpmFactory();
						
						long pdId = ti.getProcessInstance().getProcessDefinition().getId();
						View emailView = bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId()).loadView();
						emailView.setTaskInstanceId(ti.getId());
						emailView.populateVariables(vars);
						
						bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId()).submit(emailView, false);
						
						
						TaskInstanceW taskInstance = bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId());
				    	
						
						Variable variable = new Variable("files_attachments", VariableDataType.FILES);
						
						@SuppressWarnings("unchecked")
						Map<String, InputStream> files = (Map<String, InputStream>)msgAndAttachments[1];
						
						for( String fileName:files.keySet()){
							//BinaryVariable newAttachment = null;
							try {
								taskInstance.addAttachment(variable, fileName, fileName, files.get(fileName));
							} catch(Exception e) {
								//TODO: fix this!
								//logger.log(Level.SEVERE, "Unable to set binary variable for task instance: " + taskInstanceId, e);
								e.printStackTrace();
							}
						}
						
						return;
						
					} catch (MessagingException e) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while reading email msg", e);
					}
				}
			}
		}
	}
	
	protected Object[] parseContent(Message msg, long taskID) {
		Object[] msgAndAttachments = new Object[2];
		try {
			Object content = msg.getContent();
			Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
			msgAndAttachments[1] = attachemntMap;
			if (msg.isMimeType(TEXT_PLAIN_TYPE)){
			    
			    if (content instanceof String)
			    	msgAndAttachments[0] = parsePlainTextMessage((String)content);
			    
			} else if (msg.isMimeType(TEXT_HTML_TYPE) ){
				
				if (content instanceof String)
					msgAndAttachments[0] = parseHTMLMessage((String)content);
				
 			} else if (msg.isMimeType(MULTIPART_MIXED_TYPE)) {
				
				Multipart messageMultiPart = (Multipart) content;
				String msgText = CoreConstants.EMPTY;
				
				for (int i = 0; i < messageMultiPart.getCount(); i++) {
				    
				    Part messagePart = messageMultiPart.getBodyPart(i);
				    String disposition = messagePart.getDisposition();
				    //it is attachment
				    if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE)))){
			
						InputStream input =  messagePart.getInputStream();
						
						
						String fileName = messagePart.getFileName();
						if(fileName != null){
							fileName = MimeUtility.decodeText(fileName);
						}else if (messagePart.getContentType().indexOf("name*=") != -1){
							//When attachments send from evolution mail client, there is errors so we do what we can.
							fileName = messagePart.getContentType().substring(messagePart.getContentType().indexOf("name*=")+6);
							//maybe we are lucky to decode it, if not, well better something then nothing.
							fileName =  MimeUtility.decodeText(fileName);
							
						}else{
							//well not much can be done then can it?:)
							fileName = "UnknownFile";
						}
						attachemntMap.put(fileName, input);
					//It's a message body	
				    }else if (messagePart.getContent() instanceof String) {
				    	if (messagePart.isMimeType(TEXT_HTML_TYPE))	
				    			msgText = parseHTMLMessage((String)messagePart.getContent());
				    	//it's plain text
				    	else msgText = (String) messagePart.getContent();
				    	
				    	// "multipart/Mixed" can have multipart/alternative sub type.
				    }else if (messagePart.getContent() instanceof MimeMultipart && messagePart.getContentType().startsWith(MULTI_ALTERNATIVE_TYPE)){
				    	msgText = parseMultipartAlternative((MimeMultipart)messagePart.getContent());
				    }
				}
				msgAndAttachments[0] = msgText;
			}else if(msg.isMimeType(MULTI_ALTERNATIVE_TYPE)){
				msgAndAttachments[0] = parseMultipartAlternative((MimeMultipart)msg.getContent());
			}
			
		} catch (MessagingException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving content text from email msg", e);
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving content text from email msg", e);
		}
		return msgAndAttachments;
	}
	
	protected Set<PISFORMSG> resolveProcessInstances(Set<Date> dates, Set<Integer> identifierIDs) {
		
		List<Object[]> cps = getCasesBPMDAO().getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(dates, identifierIDs);
		HashSet<PISFORMSG> pisformsgs = new HashSet<PISFORMSG>(cps.size());
		
		for (Object[] objects : cps) {
			
			CaseProcInstBind cp = (CaseProcInstBind)objects[0];
			ProcessInstance pi = (ProcessInstance)objects[1];
			
			PISFORMSG pisformsg = new PISFORMSG(cp.getDateCreated(), cp.getCaseIdentierID(), pi);
			pisformsgs.add(pisformsg);
		}
		
		return pisformsgs;
	}
	
	private String parseMultipartAlternative(MimeMultipart multipart) throws MessagingException, IOException{
		
		String returnStr = null;
		for (int i = 0; i<multipart.getCount();i++){
			Part part = multipart.getBodyPart(i);
			if(part.isMimeType(TEXT_HTML_TYPE)){
				return parseHTMLMessage((String)part.getContent());
			}else if(part.isMimeType(TEXT_PLAIN_TYPE)){
				returnStr = parsePlainTextMessage((String)part.getContent());
			}
		}
		
		return returnStr;
	}
	
	private String parseHTMLMessage(String message){
		return message;// "<[!CDATA ["+ message+"]]>";
	}
	
	private String parsePlainTextMessage(String message){
		return StringUtil.escapeHTMLSpecialChars(message);
	}
	
	
	
	class PISFORMSG {

		Date date;
		Integer identifierID;
		ProcessInstance pi;
		
		public PISFORMSG(Date date, Integer identifierID, ProcessInstance pi) {
			this.date = date;
			this.identifierID = identifierID;
			this.pi = pi;
		}
		
		@Override
		public boolean equals(Object obj) {
			
			if(!super.equals(obj)) {
				
				if(date != null && identifierID != null && obj instanceof PISFORMSG) {
					
					PISFORMSG another = (PISFORMSG)obj;
					return date.equals(another.date) && identifierID.equals(another.identifierID);
				}
			} else
				return true;
			
			return false;
		}
		
		@Override
		public int hashCode() {

			int hashCode;
			
			if(date == null || identifierID == null)
				hashCode = super.hashCode();
			else
				hashCode = identifierID.hashCode() + date.hashCode();
			
			return hashCode;
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public IXFormViewFactory getXfvFact() {
		return xfvFact;
	}

	@Autowired
	public void setXfvFact(IXFormViewFactory xfvFact) {
		this.xfvFact = xfvFact;
	}

	public TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	@Autowired
	public void setFileUploadManager(TmpFilesManager fileUploadManager) {
		this.fileUploadManager = fileUploadManager;
	}

	public TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	@Autowired
	public void setUploadedResourceResolver(@TmpFileResolverType("defaultResolver")
			TmpFileResolver uploadedResourceResolver) {
		this.uploadedResourceResolver = uploadedResourceResolver;
	}
	



	
}