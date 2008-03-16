package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/16 18:59:42 $ by $Author: civilis $
 */
public class EndCaseProcessHandler implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;
	
	private static final String casesBPMDAOBeanIdentifier = "casesBPMDAO";

	public EndCaseProcessHandler() { }
	
	public EndCaseProcessHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {
		
		CasesBPMDAO casesBPMDao = getCasesBPMDao();
		
		CaseProcInstBind bind = casesBPMDao.find(CaseProcInstBind.class, ctx.getProcessInstance().getId());
		Integer caseId = bind.getCaseId();
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		
		GeneralCase theCase = casesBusiness.getGeneralCase(caseId);
		casesBusiness.changeCaseStatus(theCase, casesBusiness.getCaseStatusInactive(), iwc.getCurrentUser());
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
	
	protected CasesBPMDAO getCasesBPMDao() {
		
		return (CasesBPMDAO)WFUtil.getBeanInstance(casesBPMDAOBeanIdentifier);
	}
}