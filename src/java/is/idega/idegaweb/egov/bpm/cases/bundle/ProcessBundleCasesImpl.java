package is.idega.idegaweb.egov.bpm.cases.bundle;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bundle.ProcessBundleDefaultImpl;
import com.idega.user.business.GroupBusiness;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;

/**
 *
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 *          Last modified: $Date: 2009/01/25 15:45:46 $ by $Author: civilis $
 *
 */
@Scope("prototype")
@Service("casesBPMProcessBundle")
public class ProcessBundleCasesImpl extends ProcessBundleDefaultImpl {

	public static final String defaultCaseTypeName = "BPM";
	public static final String defaultCaseCategoryName = "BPM";
	public static final String defaultCaseHandlersGroupName = "BPM Cases Handlers";

	private CasesBPMDAO casesBPMDAO;

	@Override
	public void configure(ProcessDefinition pd) {

		super.configure(pd);
		assignToDefaultCaseTypes(pd);
	}

	protected void assignToDefaultCaseTypes(ProcessDefinition pd) {
		getCasesBPMDAO().getConfiguredCaseTypesProcDefBind(pd.getName());
	}

	protected CasesBusiness getCasesBusiness() {

		try {
			FacesContext fctx = FacesContext.getCurrentInstance();
			IWApplicationContext iwac;

			if (fctx == null)
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			else
				iwac = IWMainApplication.getIWMainApplication(fctx)
						.getIWApplicationContext();

			return IBOLookup.getServiceInstance(iwac,
					CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected GroupBusiness getGroupBusiness() {

		try {
			FacesContext fctx = FacesContext.getCurrentInstance();
			IWApplicationContext iwac;

			if (fctx == null)
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			else
				iwac = IWMainApplication.getIWMainApplication(fctx)
						.getIWApplicationContext();

			return IBOLookup.getServiceInstance(iwac,
					GroupBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}