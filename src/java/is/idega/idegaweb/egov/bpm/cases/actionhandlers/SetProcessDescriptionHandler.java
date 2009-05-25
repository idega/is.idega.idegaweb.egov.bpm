package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationHome;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.Collection;
import java.util.Iterator;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseCode;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.presentation.IWContext;
import com.idega.util.ListUtil;

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

	private String description;

	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private BPMFactory bpmFactory;

	public void execute(ExecutionContext ctx) throws Exception {

		final ProcessInstance pi = ctx.getProcessInstance();

		ProcessInstanceW piw = getBpmFactory()
				.getProcessManagerByProcessInstanceId(pi.getId())
				.getProcessInstance(pi.getId());

		final String processDescription;
		if (getDescription() != null) {
			processDescription = getDescription();
		} else {
			processDescription = piw.getProcessDescription();
		}

		CaseProcInstBind cpi = getBpmBindsDAO().find(CaseProcInstBind.class,
				pi.getId());
		Integer caseId = cpi.getCaseId();

		CasesBusiness casesBusiness = getCasesBusiness(getIWAC());
		final Case theCase = casesBusiness.getCase(caseId);

		setCaseCode(theCase, piw.getProcessDefinitionW().getProcessDefinition().getName());
		theCase.setSubject(processDescription);
		theCase.store();
	}
	
	private void setCaseCode(Case theCase, String processDefinitionName) {
		ApplicationHome applicationHome = null;
		try {
			applicationHome = (ApplicationHome) IDOLookup.getHome(Application.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (applicationHome == null) {
			return;
		}
		
		Collection<Application> applications = null;
		try {
			applications = applicationHome.findAllByApplicationUrl(processDefinitionName);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (ListUtil.isEmpty(applications)) {
			return;
		}
		
		CaseCode code = null;
		for (Iterator<Application> appsIter = applications.iterator(); (appsIter.hasNext() && code == null);) {
			code = appsIter.next().getCaseCode();
		}
		if (code != null) {
			theCase.setCaseCode(code);
		}
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

	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}