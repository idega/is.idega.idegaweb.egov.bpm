package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.Collection;
import java.util.Iterator;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseCode;
import com.idega.block.process.data.model.CaseCodeModel;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.business.DefaultSpringBean;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.presentation.IWContext;
import com.idega.util.ListUtil;

import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationHome;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

/**
 *
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0
 *
 *          Last modified: Jun 27, 2008 by Author: Anton
 *
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service(SetProcessDescriptionHandler.BEAN_NAME)
public class SetProcessDescriptionHandler extends DefaultSpringBean implements ActionHandler {

	private static final long serialVersionUID = -4735864635886588195L;

	public static final String BEAN_NAME = "setProcessDescriptionHandler";

	private String description;

	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private BPMFactory bpmFactory;

	@Override
	public void execute(ExecutionContext ctx) throws Exception {
		final ProcessInstance pi = ctx.getProcessInstance();

		ProcessInstanceW piw = getBpmFactory()
				.getProcessManagerByProcessInstanceId(pi.getId())
				.getProcessInstance(pi.getId()
		);

		String processDescription = getDescription() == null ? piw.getProcessDescription() : getDescription();
		String procDefName = piw.getProcessDefinitionW(ctx.getJbpmContext()).getProcessDefinition().getName();
		setCaseSubject(pi.getId(), processDescription, procDefName);
	}

	protected Case getCase(Long processInstanceId) throws Exception {
		CaseProcInstBind cpi = getBpmBindsDAO().find(CaseProcInstBind.class, processInstanceId);
		if (cpi == null) {
			return null;
		}

		Integer caseId = cpi.getCaseId();

		CasesBusiness casesBusiness = getCasesBusiness(getIWAC());
		return casesBusiness.getCase(caseId);
	}

	protected void setCaseSubject(Long processInstanceId, String caseSubject, String caseCode) throws Exception {
		final Case theCase = getCase(processInstanceId);
		if (theCase != null) {
			setCaseCode(theCase, caseCode);
			theCase.setSubject(caseSubject);
			theCase.store();
		} else {
			getLogger().warning("Failed to store case " + theCase + " by process instance id: " + processInstanceId);
		}
	}

	public void setCaseCode(Case theCase, String processDefinitionName) {
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
			CaseCodeModel caseCodeModel = appsIter.next().getCaseCode();
			if (caseCodeModel instanceof CaseCode) {
				code = (CaseCode) caseCodeModel;
			} else {
				getLogger().warning("Incorrect type of " + caseCodeModel + ". Expected " + CaseCode.class.getName() + ", got " + (caseCodeModel == null ? "null" : caseCodeModel.getClass().getName()));
			}
		}
		if (code != null) {
			theCase.setCaseCode(code);
		}
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac,
					CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	private IWApplicationContext getIWAC() {
		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
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