package com.idega.idegaweb.egov.bpm.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.business.FormConverterToPDF;
import com.idega.block.process.business.pdf.CaseConverterToPDF;
import com.idega.block.process.business.pdf.CasePDF;
import com.idega.block.process.data.Case;
import com.idega.bpm.pdf.business.FormConverterToPDFBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.idegaweb.egov.bpm.presentation.IWContextMockUp;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ProcessCaseConverterToPDF extends DefaultSpringBean implements CaseConverterToPDF {

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private PDFGenerator pdfGenerator;

	@Autowired
	private FormConverterToPDFBean formConverter;

	private ProcessInstanceW getProcessInstance(Integer caseId) throws Exception {
		CaseProcInstBind bind = casesBPMDAO.getCaseProcInstBindByCaseId(caseId);
		if (bind == null) {
			getLogger().warning("Unable to get case and process bind by case " + caseId);
			return null;
		}

		return bpmFactory.getProcessInstanceW(bind.getProcInstId());
	}

	private List<TaskInstanceW> getFinishedTasks(ProcessInstanceW piW) {
		if (piW == null)
			return null;

		return piW.getSubmittedTaskInstances();
	}

	@Override
	public List<CasePDF> getPDFsForCase(Integer caseId) throws Exception {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return null;
		}

		ProcessInstanceW piW = getProcessInstance(caseId);
		List<TaskInstanceW> finishedTasks = getFinishedTasks(piW);
		if (ListUtil.isEmpty(finishedTasks))
			return null;

		Locale locale = getCurrentLocale();
		IWContext iwc = getIWContext();
		HttpServletRequest request = iwc.getRequest();
		List<CasePDF> pdfs = new ArrayList<CasePDF>();
		for (TaskInstanceW tiW: finishedTasks) {
			String taskInstanceId = String.valueOf(tiW.getTaskInstanceId());
			try {
				request.setAttribute(FormConverterToPDF.RENDERING_TASK_INSTANCE, taskInstanceId);

				formConverter.addStyleSheetsForPDF(iwc);

				UIComponent component = formConverter.getComponentToRender(iwc, taskInstanceId, null, null);
				byte[] bytes = pdfGenerator.getBytesOfGeneratedPDF(iwc, component, true, true);
				if (bytes == null) {
					getLogger().warning("Failed to generate PDF for task instance: " + taskInstanceId);
					continue;
				}

				String name = tiW.getName(locale);
				name = StringHandler.stripNonRomanCharacters(name, new char[] {'-', '_', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
				pdfs.add(new CasePDF(caseId, name.concat(".pdf"), bytes));
			} finally {
				request.removeAttribute(FormConverterToPDF.RENDERING_TASK_INSTANCE);
			}
		}
		return pdfs;
	}

	@Override
	public List<CasePDF> getPDFsForCase(Case theCase) throws Exception {
		if (theCase == null) {
			getLogger().warning("Case is not provided");
			return null;
		}

		return getPDFsForCase(Integer.valueOf(theCase.getId()));
	}

	private IWContext getIWContext() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null)
			iwc = new IWContextMockUp();

		return iwc;
	}

}