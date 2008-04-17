package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.bpm.cases.form.CasesBPMViewManager;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/04/17 23:57:41 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class EmailMessagesAttacher implements ApplicationListener {
	
	private CasesBPMDAO casesBPMDAO;
	private IdegaJbpmContext idegaJbpmContext;

	public void onApplicationEvent(ApplicationEvent ae) {
		
		if(ae instanceof ApplicationEmailEvent) {
			
			ApplicationEmailEvent ev = (ApplicationEmailEvent)ae;
			Map<String, Message> msgs = ev.getMessages();
			HashSet<Date> dates = new HashSet<Date>(msgs.size());
			HashSet<Integer> identifierIDs = new HashSet<Integer>(msgs.size());
			HashMap<PISFORMSG, Message> PISFORMSGMessage = new HashMap<PISFORMSG, Message>(msgs.size()); 
			
			for (Entry<String, Message> entry : msgs.entrySet()) {
				
				if(entry.getKey().startsWith(CasesBPMViewManager.IDENTIFIER_PREFIX)) {
					
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
					
					for (PISFORMSG pisformsg : pisformsgs) {
						
						if(PISFORMSGMessage.containsKey(pisformsg))
							attachEmailMsg(PISFORMSGMessage.get(pisformsg), pisformsg.pi);
					}
				}
			}
		}
	}
	
	protected void attachEmailMsg(Message msg, ProcessInstance pi) {
	
		try {
			
			
			System.out.println("attaching: "+msg.getSubject());
			System.out.println("piid: "+pi.getId());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
}