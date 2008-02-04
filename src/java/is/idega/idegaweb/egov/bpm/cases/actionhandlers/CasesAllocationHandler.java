package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.CasesJbpmProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/04 19:05:37 $ by $Author: civilis $
 */
public class CasesAllocationHandler implements ActionHandler {

	private static final long serialVersionUID = -6527613958449076385L;
	
	public CasesAllocationHandler() { }
	
	public CasesAllocationHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {

		String allocateToId = (String)ctx.getVariable(CasesJbpmProcessConstants.caseAllocateToVariableName);
		String performerId = (String)ctx.getVariable(CasesJbpmProcessConstants.casePerformerIdVariableName);
		String caseId = (String)ctx.getVariable(CasesJbpmProcessConstants.caseIdVariableName);
		
		if(caseId == null) {
			Logger.getLogger(CasesAllocationHandler.class.getName()).log(Level.SEVERE, "Case id not provided for allocation handler");
			throw new NullPointerException("Case id not provided for allocation handler");
		}
		
		if(performerId == null) {
			Logger.getLogger(CasesAllocationHandler.class.getName()).log(Level.SEVERE, "Performer id not provided for allocation handler");
			throw new NullPointerException("Performer id not provided for allocation handler");
		}

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		
		GeneralCase theCase = casesBusiness.getGeneralCase(Integer.parseInt(caseId));
		User performer = getUserBusiness(iwc).getUser(Integer.parseInt(performerId));
		
		if(allocateToId == null || CoreConstants.EMPTY.equals(allocateToId) || performerId.equals(allocateToId))
			casesBusiness.takeCase(theCase, performer, iwc);
		else {

			User userToAllocate = getUserBusiness(iwc).getUser(Integer.parseInt(allocateToId));	
			casesBusiness.allocateCase(theCase, userToAllocate, "The case has been allocated to you", performer, iwc);
		}
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