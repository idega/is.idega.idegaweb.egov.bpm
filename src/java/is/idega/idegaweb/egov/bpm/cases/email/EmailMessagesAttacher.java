package is.idega.idegaweb.egov.bpm.cases.email;

import is.idega.idegaweb.egov.bpm.cases.exe.CasesBPMResources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import com.idega.block.form.process.IXFormViewFactory;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.artifacts.impl.ProcessArtifactsProviderImpl;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.view.View;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/06/13 12:52:53 $ by $Author: arunas $
 */
@Scope("singleton")
@Service
public class EmailMessagesAttacher implements ApplicationListener {
	
	private CasesBPMDAO casesBPMDAO;
	private IdegaJbpmContext idegaJbpmContext;
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
				
				if(entry.getKey().startsWith(CasesBPMResources.IDENTIFIER_PREFIX)) {
					
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
				    	
				    	vars.put("string:subject", subject);
				    	vars.put("string:text", text);
				    	vars.put("string:fromPersonal", fromPersonal);
				    	vars.put("string:fromAddress", fromAddress);

				    	@SuppressWarnings("unchecked")
				    	List<File> files = (List<File>)msgAndAttachments[1];
				    	
				    	if(files != null)
				    		vars.put("files:attachments", files);
				    	
						BPMFactory bpmFactory = getBpmFactory();
						
						long pdId = ti.getProcessInstance().getProcessDefinition().getId();
						View emailView = bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId()).loadView();
						emailView.setTaskInstanceId(ti.getId());
						emailView.populateVariables(vars);
						
						bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId()).submit(emailView, false);
						
						return;
						
					} catch (MessagingException e) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while reading email msg", e);
					}
				}
			}
		}
	}
	
	protected Object[] parseContent(Message msg, long taskID) {
		
//		contentType: TEXT/PLAIN; charset=UTF-8; format=flowed
		
		Object[] msgAndAttachments = new Object[2];
		
		try {
			Object content = msg.getContent();
			
			if (msg.isMimeType(TEXT_PLAIN_TYPE)){
			    
			    if (content instanceof String)
				msgAndAttachments[0] = content;
				
			} else 
			    
			    if (msg.isMimeType(TEXT_HTML_TYPE) || msg.isMimeType(MULTI_ALTERNATIVE_TYPE)){
			    			    			    
				String filesfolder = taskID + CoreConstants.SLASH;
				
				createAttachmentFile (content, filesfolder);    
			    			   			    
				msgAndAttachments[1] = getFileUploadManager().getFiles(filesfolder, null, getUploadedResourceResolver());
 
			} else 	
			      
			    if (msg.isMimeType(MULTIPART_MIXED_TYPE)) {
				
				Multipart messageMultiPart = (Multipart) content;
				
				String filesfolder = taskID + CoreConstants.SLASH;
				
				String msgText = CoreConstants.EMPTY;
				
				for (int i = 0; i < messageMultiPart.getCount(); i++) {
				    
				    Part messagePart = messageMultiPart.getBodyPart(i);
				    
					    
				    if (messagePart.getContent() instanceof String) {
					
					if (messagePart.isMimeType(TEXT_HTML_TYPE))	
					    createAttachmentFile (messagePart.getContent(), filesfolder);
					else
					    msgText = msgText + messagePart.getContent();
				    }	    
				    
				    
				    String disposition = messagePart.getDisposition();
				
				    if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE)))){
					
						InputStream input =  messagePart.getInputStream();
						
						String fileName = messagePart.getFileName();
						
						getFileUploadManager().uploadToTmpDir(filesfolder, fileName, input, getUploadedResourceResolver());
				    }
					
				}//for end
			    
				msgAndAttachments[0] = msgText;
				msgAndAttachments[1] = getFileUploadManager().getFiles(filesfolder, null, getUploadedResourceResolver());
				
			    }// else if end
			
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
	
	protected void createAttachmentFile (Object content, String filesfolder) {
	    
	    String fileName = System.currentTimeMillis() + HTML_EXTENSION;
	    
	    try {
		
		if (content instanceof String) {
		    
		    try{
			
			FileUtil.createFileAndFolder(getUploadedResourceResolver().getRealBasePath() + filesfolder, fileName);
			    
			FileWriter fstream = new FileWriter(getUploadedResourceResolver().getRealBasePath() + filesfolder + fileName);
			    
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(content.toString());
			
			out.close();
			    
		    }catch (Exception e){
			e.printStackTrace();
		    }	    
		    
		} else {
		    
		    Multipart messageMultiPart = (Multipart)content;
    	    		for (int i = 0; i < messageMultiPart.getCount(); i++) {
    		    
    	    		    Part messagePart = messageMultiPart.getBodyPart(i);
    
    	    		    if (messagePart.isMimeType(TEXT_HTML_TYPE)) {
    		    					
    	    			Object htmlContent = messagePart.getContent();
    			
    	    			try{
    				    FileUtil.createFileAndFolder(getUploadedResourceResolver().getRealBasePath() + filesfolder, fileName);
    				    
    				    FileWriter fstream = new FileWriter(getUploadedResourceResolver().getRealBasePath() + filesfolder + fileName);
    				    
    				    BufferedWriter out = new BufferedWriter(fstream);
    				    out.write(htmlContent.toString());
    				
    				    out.close();
    				    
    	    			}catch (Exception e){
    	    			    e.printStackTrace();
    	    			}
    		
    	    		    }			
    		
    	    		}
    	    	
	     }//end if	
	    
	    } catch (MessagingException e) {
		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving content text from email msg", e);
	    }
	    catch (IOException e) {
		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving content text from email msg", e);
	    }
	    
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
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
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