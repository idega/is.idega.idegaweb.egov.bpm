package com.idega.idegaweb.egov.bpm.pdf;

import java.io.InputStream;
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
import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.file.CaseAttachment;
import com.idega.block.process.business.pdf.CaseConverterToPDF;
import com.idega.block.process.business.pdf.CasePDF;
import com.idega.block.process.data.Case;
import com.idega.bpm.pdf.business.FormConverterToPDFBean;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.idegaweb.egov.bpm.presentation.IWContextMockUp;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUserImpl;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

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

	@Autowired
	private BinaryVariablesHandler attachmentsHandler;

	private ProcessInstanceW getProcessInstance(Integer caseId) throws Exception {
		CaseProcInstBind bind = casesBPMDAO.getCaseProcInstBindByCaseId(caseId);
		if (bind == null) {
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

		return getPDFsAndAttachmentsForCase(null, caseId, false, true);
	}

	@Override
	public List<CasePDF> getPDFsForCase(Case theCase) throws Exception {
		if (theCase == null) {
			getLogger().warning("Case is not provided");
			return null;
		}

		return getPDFsAndAttachmentsForCase(theCase, null, false, true);
	}

	@Override
	public List<CasePDF> getPDFsAndAttachmentsForCase(Integer caseId) throws Exception {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return null;
		}

		return getPDFsAndAttachmentsForCase(null, caseId, true, true);
	}

	private List<CasePDF> getPDFsAndAttachmentsForCase(Case theCase, Integer caseId, boolean loadAttachments, boolean switchUser) throws Exception {
		if (theCase == null && caseId == null) {
			return null;
		}

		if (caseId == null) {
			caseId = Integer.valueOf(theCase.getId());
		}
		if (theCase == null) {
			CaseBusiness caseBusiness = getServiceInstance(CaseBusiness.class);
			theCase = caseBusiness.getCase(caseId);
		}

		String identifier = theCase.getCaseIdentifier();

		IWContext iwc = CoreUtil.getIWContext();
		LoginBusinessBean login = null;
		User currentUser = iwc == null ? null : getCurrentUser();
		Locale locale = iwc == null ? getCurrentLocale() : iwc.getCurrentLocale();
		try {
			if (switchUser && iwc != null) {
				login = LoginBusinessBean.getLoginBusinessBean(iwc);

				User admin = iwc.getAccessController().getAdministratorUser();
				if (currentUser == null || !admin.getId().equals(currentUser.getId())) {
					login.logOutUser(iwc);
					login.logInAsAnotherUser(iwc, admin);

					iwc.getRequest().setAttribute(BPMUserImpl.bpmUsrParam, admin.getUniqueId());
				}
			}

			ProcessInstanceW piW = getProcessInstance(caseId);
			List<TaskInstanceW> finishedTasks = getFinishedTasks(piW);
			if (ListUtil.isEmpty(finishedTasks))
				return null;

			iwc = iwc == null ? getIWContext() : iwc;
			HttpServletRequest request = iwc.getRequest();
			List<CasePDF> pdfs = new ArrayList<CasePDF>();
			for (TaskInstanceW tiW: finishedTasks) {
				String taskInstanceId = String.valueOf(tiW.getTaskInstanceId());
				String taskEnd = new IWTimestamp(tiW.getTaskInstance().getEnd()).getDateString("yyyy-MM-dd_HH-mm-ss");
				CasePDF casePDF = null;
				try {
					request.setAttribute(FormConverterToPDF.RENDERING_TASK_INSTANCE, taskInstanceId);

					formConverter.addStyleSheetsForPDF(iwc);

					UIComponent component = null;
					try {
						component = formConverter.getComponentToRender(iwc, taskInstanceId, null, null);
					} catch (Exception e) {

					}
					if (component == null) {
						getLogger().warning("Failed to get UI component for task instance: " + taskInstanceId);
						continue;
					}
					byte[] bytes = pdfGenerator.getBytesOfGeneratedPDF(iwc, component, true, true);
					if (bytes == null) {
						getLogger().warning("Failed to generate PDF for task instance: " + taskInstanceId);
						continue;
					}

					String name = tiW.getName(locale);
					if (StringUtil.isEmpty(name)) {
						getLogger().warning("Failed to resolve name for task instance: " + taskInstanceId);
						continue;
					}

					name = StringHandler.stripNonRomanCharacters(name, new char[] {'-', '_', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
					casePDF = new CasePDF(
							caseId,
							name.concat(CoreConstants.UNDER).concat(taskEnd).concat(CoreConstants.DOT).concat("pdf"),
							identifier,
							bytes
					);
				} finally {
					request.removeAttribute(FormConverterToPDF.RENDERING_TASK_INSTANCE);
				}

				if (casePDF != null) {
					pdfs.add(casePDF);

					if (loadAttachments) {
						List<BinaryVariable> attachments = tiW.getAttachments();
						if (!ListUtil.isEmpty(attachments)) {
							for (BinaryVariable attachment: attachments) {
								InputStream stream = attachmentsHandler.getBinaryVariableContent(attachment);
								casePDF.addAttachment(new CaseAttachment(
										String.valueOf(attachment.hashCode()).concat(CoreConstants.UNDER).concat(attachment.getFileName()),
										IOUtil.getBytesFromInputStream(stream))
								);
							}
						}
					}
				}
			}
			return pdfs;
		} finally {
			if (switchUser && login != null) {
				login.logOutUser(iwc);
				iwc.getRequest().removeAttribute(BPMUserImpl.bpmUsrParam);

				if (currentUser != null) {
					login.logInAsAnotherUser(iwc, currentUser);
					iwc.getRequest().setAttribute(BPMUserImpl.bpmUsrParam, currentUser.getUniqueId());
					LoginBusinessBean.getLoginSessionBean().setUser(currentUser);
				}

				iwc.setCurrentLocale(locale);
			}
		}
	}

	private IWContext getIWContext() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null)
			iwc = new IWContextMockUp();

		return iwc;
	}

}