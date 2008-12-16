package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/12/16 19:55:29 $ by $Author: civilis $
 */
@Service("assignCaseOwnerHandler")
@Scope("prototype")
public class AssignCaseOwnerHandler implements ActionHandler {

	private static final long serialVersionUID = 340054091051722366L;
	private Long processInstanceId;
	private Integer ownerUserId;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	public void execute(ExecutionContext ectx) throws Exception {

		CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class,
				processInstanceId);

		Integer caseId = cpi.getCaseId();

		IWApplicationContext iwac = getIWAC();
		CasesBusiness casesBusiness = getCasesBusiness(iwac);
		GeneralCase genCase = casesBusiness.getGeneralCase(caseId);

		JBPMConstants.bpmLogger.fine("Setting new owner for a case (" + caseId
				+ ") to be user id = " + getOwnerUserId());

		User ownerUser = getUserBusiness(iwac).getUser(getOwnerUserId());
		genCase.setOwner(ownerUser);
		genCase.store();
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public Integer getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(Integer ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac,
					CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac,
					UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	private IWApplicationContext getIWAC() {

		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
		// trying to get iwma from iwc, if available, downgrading to default
		// iwma, if not

		if (iwc != null) {

			iwac = iwc;

		} else {
			iwac = IWMainApplication.getDefaultIWApplicationContext();
		}

		return iwac;
	}

	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}
}