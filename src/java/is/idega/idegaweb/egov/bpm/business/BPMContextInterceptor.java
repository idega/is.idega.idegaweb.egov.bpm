package is.idega.idegaweb.egov.bpm.business;

import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.form.bean.SubmissionDataBean;
import com.idega.block.process.data.bean.Case;
import com.idega.block.process.data.dao.CaseDAO;
import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.data.CaseProcInstBind;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.event.BPMContextSavedEvent;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMContextInterceptor extends DefaultSpringBean implements ApplicationListener<BPMContextSavedEvent> {

	@Autowired
	private CaseDAO caseDAO;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private BPMDAO bpmDAO;

	@Override
	public void onApplicationEvent(BPMContextSavedEvent event) {
		try {
			Long processInstanceId = event.getProcInstId();
			String subject = getSubject(processInstanceId);

			setSubject(processInstanceId, subject);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error updating case's subject. Event: " + event, e);
		}
	}

	public void setSubject(Long procInstId, String subject) {
		if (procInstId == null) {
			getLogger().warning("Proc. inst. ID not provided");
			return;
		}

		CaseProcInstBind bind = getCasesBPMDAO().getCaseProcInstBindByProcessInstanceId(procInstId);
		Integer id = bind.getCaseId();

		setSubject(id, subject);
	}

	private String getSubject(Long procInstId) {
		if (procInstId == null) {
			getLogger().warning("Proc. inst. ID not provided");
			return null;
		}

		List<Variable> variables = getBpmDAO().getVariablesByNameAndProcessInstance(SubmissionDataBean.VARIABLE_CASE_DESCRIPTION, procInstId);
		if (ListUtil.isEmpty(variables)) {
			getLogger().warning("No variables found for proc. inst. with ID: " + procInstId + " and variable " + SubmissionDataBean.VARIABLE_CASE_DESCRIPTION);
			return null;
		}

		String subject = variables.get(0).getValue();
		return subject;
	}

	@Transactional(readOnly = false)
	private void setSubject(Integer caseId, String subject) {
		if (caseId == null) {
			getLogger().warning("Case ID is unknown");
			return;
		}
		if (StringUtil.isEmpty(subject)) {
			getLogger().warning("Subject is not provided. Case ID: " + caseId);
			return;
		}

		Case theCase = getCaseDAO().getCaseById(caseId);

		getLogger().info("Setting new subject: " + subject + ". Current subject: " + theCase.getSubject() + ", case ID: " + theCase.getId());

		theCase.setSubject(subject);
		getCaseDAO().merge(theCase);

		CoreUtil.clearAllCaches();
	}

	public CaseDAO getCaseDAO() {
		return caseDAO;
	}

	public void setCaseDAO(CaseDAO caseDAO) {
		this.caseDAO = caseDAO;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public BPMDAO getBpmDAO() {
		return bpmDAO;
	}

	public void setBpmDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}

}