package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.message.business.CommuneMessageBusiness;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/02 19:22:55 $ by $Author: civilis $
 */
public class SendCaseMessagesHandler implements ActionHandler {

	private static final long serialVersionUID = -692710623676132049L;
	private String parm;
	
	public SendCaseMessagesHandler() {
		
		System.out.println("simple constructor");
	}

	public SendCaseMessagesHandler(String parm) { 
		
		System.out.println("params got: "+parm);
		this.parm = parm;
	}

	public void execute(ExecutionContext ctx) throws Exception {
	
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		CommuneMessageBusiness casesBusiness = getCommuneMessageBusiness(iwc);
		
		System.out.println("__");
		System.out.println("params: "+parm);
		
		//MessageValue msg = new MessageValue();
		//msg.setSubject(subject)
		
		//casesBusiness.createMessage(msg);
	}
	
	protected CommuneMessageBusiness getCommuneMessageBusiness(IWApplicationContext iwac) {
		try {
			return (CommuneMessageBusiness)IBOLookup.getServiceInstance(iwac, CommuneMessageBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getParm() {
		System.out.println("get param________"+parm);
		return parm;
	}

	public void setParm(String parm) {
		System.out.println("set param___ "+parm);
		this.parm = parm;
	}
}