package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.presentation.IWContext;

/**
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0
 * 
 *          Last modified: Jun 27, 2008 by Author: Anton
 * 
 */
@Service("setProcessDescriptionHandler")
@Scope("prototype")
public class SetProcessDescriptionHandler implements ActionHandler {
	private static final long serialVersionUID = -4735864635886588195L;

	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private BPMFactory bpmFactory;

	public void execute(ExecutionContext ctx) throws Exception {

		final ProcessInstance pi = ctx.getProcessInstance();

		ProcessInstanceW piw = getBpmFactory()
				.getProcessManagerByProcessInstanceId(pi.getId())
				.getProcessInstance(pi.getId());
		String processDescription = piw.getProcessDescription();

		CaseProcInstBind cpi = getBpmBindsDAO().find(CaseProcInstBind.class,
				pi.getId());
		Integer caseId = cpi.getCaseId();

		CasesBusiness casesBusiness = getCasesBusiness(getIWAC());
		final Case theCase = casesBusiness.getCase(caseId);

		theCase.setSubject(processDescription);
		theCase.store();
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac,
					CasesBusiness.class);
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

	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}