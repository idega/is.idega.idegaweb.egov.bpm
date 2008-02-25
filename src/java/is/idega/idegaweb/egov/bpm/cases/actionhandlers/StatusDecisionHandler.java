package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

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

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/25 16:16:25 $ by $Author: civilis $
 */
public class StatusDecisionHandler implements ActionHandler {

	private static final long serialVersionUID = -6527613958449076385L;
	private static final String toEndTransitionName = "toEnd";
	private static final String toCreateResponseTransitionName = "toCreateResponse";
	
	public StatusDecisionHandler() { }
	
	public StatusDecisionHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {
		
//		TODO: check the difference: ctx.getContextInstance().getVariable
		String status = (String)ctx.getVariable(CasesBPMProcessConstants.caseStatusVariableName);
		
		if(status == null) {
			
			Logger.getLogger(StatusDecisionHandler.class.getName()).log(Level.WARNING, "Status decision action handler called, but status not specified. Taking default transition: "+toCreateResponseTransitionName);
			ctx.leaveNode(toCreateResponseTransitionName);
			return;
		}

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		
		if(status.equals(casesBusiness.getCaseStatusReady().getStatus()) || status.equals(casesBusiness.getCaseStatusInactive().getStatus()))
			ctx.leaveNode(toEndTransitionName);
		else
			ctx.leaveNode(toCreateResponseTransitionName);
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}