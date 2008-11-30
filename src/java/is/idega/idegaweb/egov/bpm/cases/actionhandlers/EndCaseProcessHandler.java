package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2008/11/30 08:22:36 $ by $Author: civilis $
 */
@Service("endCaseProcessHandler")
@Scope("prototype")
public class EndCaseProcessHandler implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;
	@Autowired
	private CasesBPMDAO casesBPMDAO;

	public void execute(ExecutionContext ctx) throws Exception {

		CaseProcInstBind bind = getCasesBPMDAO().find(CaseProcInstBind.class,
				ctx.getProcessInstance().getId());
		Integer caseId = bind.getCaseId();

		IWContext iwc = IWContext.getIWContext(FacesContext
				.getCurrentInstance());
		CasesBusiness casesBusiness = getCasesBusiness(iwc);

		GeneralCase theCase = casesBusiness.getGeneralCase(caseId);
		casesBusiness.changeCaseStatus(theCase, casesBusiness
				.getCaseStatusReady(), iwc.getCurrentUser());
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac,
					CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}
}