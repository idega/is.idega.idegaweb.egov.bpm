package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.CasesStatusMapperHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 *          Last modified: $Date: 2009/06/23 10:22:22 $ by $Author: valdas $
 */
@Service("endCaseProcessHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EndCaseProcessHandler extends DefaultSpringBean implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private CasesStatusMapperHandler casesStatusMapper;

	private String caseStatus;

	@Override
	public void execute(ExecutionContext ctx) throws Exception {
		CaseProcInstBind bind = getCasesBPMDAO().find(CaseProcInstBind.class, ctx.getProcessInstance().getId());
		Integer caseId = bind.getCaseId();

		CasesBusiness casesBusiness = getCasesBusiness(IWMainApplication.getDefaultIWApplicationContext());

		GeneralCase theCase = casesBusiness.getGeneralCase(caseId);
		changeCaseStatus(casesBusiness, theCase, getCurrentUser(CoreUtil.getIWContext(), ctx));
	}

	protected User getCurrentUser(IWContext iwc, ExecutionContext ctx) {
		if (iwc == null)
			return super.getCurrentUser();
		return iwc.getCurrentUser();
	}

	protected void changeCaseStatus(CasesBusiness casesBusiness, GeneralCase theCase, User user) throws Exception {
		if (StringUtil.isEmpty(caseStatus)) {
			caseStatus = casesBusiness.getCaseStatusReady().getStatus();
		} else {
			caseStatus = getCasesStatusMapper().getStatusCodeByMappedName(caseStatus);
		}

		casesBusiness.changeCaseStatus(theCase, caseStatus, user);
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public CasesStatusMapperHandler getCasesStatusMapper() {
		return casesStatusMapper;
	}

	public void setCasesStatusMapper(CasesStatusMapperHandler casesStatusMapper) {
		this.casesStatusMapper = casesStatusMapper;
	}

	public String getCaseStatus() {
		return caseStatus;
	}

	public void setCaseStatus(String caseStatus) {
		this.caseStatus = caseStatus;
	}

}