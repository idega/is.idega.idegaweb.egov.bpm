package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.CasesJbpmProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/04 19:05:37 $ by $Author: civilis $
 */
public class CasesStatusHandler implements ActionHandler {

	private static final long serialVersionUID = -6527613958449076385L;
	
	public CasesStatusHandler() { }
	
	public CasesStatusHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {
		
		String status = (String)ctx.getVariable(CasesJbpmProcessConstants.caseStatusVariableName);
		String caseId = (String)ctx.getVariable(CasesJbpmProcessConstants.caseIdVariableName);
		String performerId = (String)ctx.getVariable(CasesJbpmProcessConstants.casePerformerIdVariableName);
		
		if(status == null || caseId == null || performerId == null) {
			Logger.getLogger(CasesStatusHandler.class.getName()).log(Level.WARNING, 
					new StringBuilder("Case Status action handler called, but no required parameters was found in the process instance.")
					.append("\nstatus: ").append(status)
					.append("\ncaseId: ").append(caseId)
					.append("\nperformerIds: ").append(performerId)
					.toString()
			);
			return;
		}
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		Case theCase = getCasesBusiness(iwc).getCase(Integer.parseInt(caseId));
		User user = getUserBusiness(iwc).getUser(Integer.parseInt(performerId));
		getCasesBusiness(iwc).changeCaseStatusDoNotSendUpdates(theCase, status, user);
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}