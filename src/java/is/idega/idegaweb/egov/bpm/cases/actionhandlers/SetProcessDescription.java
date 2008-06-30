package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.persistence.Param;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.jbpm.artifacts.ProcessArtifactsProvider;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.presentation.IWContext;
import com.idega.util.expression.ELUtil;

/**
 *
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 
 *
 * Last modified: Jun 27, 2008 by Author: Anton 
 *
 */
public class SetProcessDescription implements ActionHandler {
	private static final long serialVersionUID = -4735864635886588195L;
	private String processDescription;
	private BPMDAO bpmBindsDAO;
	private ProcessArtifactsProvider processArtifactsProvider;

	public String getProcessDescription() {
		return processDescription;
	}

	public void setProcessDescription(String processDescription) {
		this.processDescription = processDescription;
	}

	public void execute(ExecutionContext ctx) throws Exception {
		ELUtil.getInstance().autowire(this);
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		final IWContext iwc = IWContext.getIWContext(fctx);
		final ProcessInstance pi = ctx.getProcessInstance();
		
		setProcessDescription(getProcessArtifactsProvider().getProcessDescription(pi.getId()));
		
		Integer caseId = getBpmBindsDAO().getSingleResult(CaseProcInstBind.getCaseIdByProcessInstanceId, Integer.class,
				new Param(CaseProcInstBind.procInstIdProp, pi.getId()));

		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		final Case theCase = casesBusiness.getCase(caseId.intValue());
		
		theCase.setSubject(getProcessDescription());
		theCase.store();
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	public ProcessArtifactsProvider getProcessArtifactsProvider() {
		return processArtifactsProvider;
	}

	@Autowired
	public void setProcessArtifactsProvider(
			ProcessArtifactsProvider processArtifactsProvider) {
		this.processArtifactsProvider = processArtifactsProvider;
	}
}
