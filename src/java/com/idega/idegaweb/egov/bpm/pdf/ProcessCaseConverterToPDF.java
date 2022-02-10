package com.idega.idegaweb.egov.bpm.pdf;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.apache.myfaces.renderkit.html.util.HtmlBufferResponseWriterWrapper;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.business.FormConverterToPDF;
import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.file.CaseAttachment;
import com.idega.block.process.business.pdf.CaseConverterToPDF;
import com.idega.block.process.business.pdf.CasePDF;
import com.idega.block.process.data.Case;
import com.idega.bpm.pdf.business.FormConverterToPDFBean;
import com.idega.core.accesscontrol.business.LoggedOnInfo;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginSession;
import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUserImpl;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

import is.idega.idegaweb.egov.bpm.presentation.IWContextMockUp;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ProcessCaseConverterToPDF extends DefaultSpringBean implements CaseConverterToPDF {

	@Autowired
	@Lazy
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

	private List<TaskInstanceW> getFinishedTasks(IWContext iwc, ProcessInstanceW piW) {
		if (piW == null) {
			return null;
		}

		return piW.getSubmittedTaskInstances(iwc);
	}

	@Override
	public List<CasePDF> getPDFsForCase(Integer caseId) throws Exception {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return null;
		}

		return getPDFsAndAttachmentsForCase(null, caseId, false, true, false);
	}

	@Override
	public List<CasePDF> getPDFsForCase(Case theCase) throws Exception {
		return getPDFsForCase(theCase, false);
	}
	@Override
	public List<CasePDF> getPDFsForCase(Case theCase, boolean resetContex) throws Exception {
		if (theCase == null) {
			getLogger().warning("Case is not provided");
			return null;
		}

		return getPDFsAndAttachmentsForCase(theCase, null, false, true, resetContex);
	}

	@Override
	public List<CasePDF> getPDFsAndAttachmentsForCase(Integer caseId) throws Exception {
		if (caseId == null) {
			getLogger().warning("Case ID is not provided");
			return null;
		}

		return getPDFsAndAttachmentsForCase(null, caseId, true, true, false);
	}

	private List<CasePDF> getPDFsAndAttachmentsForCase(Case theCase, Integer caseId, boolean loadAttachments, boolean switchUser, boolean resetContext) throws Exception {
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
		LoggedOnInfo loggedOnInfo = null;
		Locale locale = iwc == null ? getCurrentLocale() : iwc.getCurrentLocale();
		try {
			if (switchUser && iwc != null) {
				login = LoginBusinessBean.getLoginBusinessBean(iwc);

				User admin = iwc.getAccessController().getAdministratorUser();
				if (currentUser == null || !admin.getId().equals(currentUser.getId())) {
					loggedOnInfo = LoginBusinessBean.getLoggedOnInfo(iwc);
					login.logOutUser(iwc);
					login.logInAsAnotherUser(iwc, admin);

					iwc.getRequest().setAttribute(BPMUserImpl.bpmUsrParam, admin.getUniqueId());
				}
			}

			ProcessInstanceW piW = getProcessInstance(caseId);
			if (piW == null) {
				getLogger().warning("Proc. inst. ID can not be found for case: " + caseId);
				return null;
			}
			List<TaskInstanceW> finishedTasks = getFinishedTasks(iwc, piW);
			if (ListUtil.isEmpty(finishedTasks)) {
				getLogger().info("There are no submitted documents for case " + theCase + ", proc. inst. ID: " + piW.getProcessInstanceId() + ", identifier: " + identifier);
				return null;
			}

			iwc = iwc == null ? getIWContext() : iwc;
			HttpServletRequest request = null;
			if (iwc != null) {
				request = iwc.getRequest();
			}

			List<CasePDF> pdfs = new ArrayList<>();
			for (TaskInstanceW tiW: finishedTasks) {
				String taskInstanceId = null;
				try {
					Serializable value = tiW.getTaskInstanceId();
					if (value != null) {
						taskInstanceId = String.valueOf(value);
					}
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Failed to get task instance id, cause of: ", e);
				}
				TaskInstance ti = tiW.getTaskInstance();
				String taskEnd = new IWTimestamp(ti.getEnd()).getDateString("yyyy-MM-dd_HH-mm-ss");
				CasePDF casePDF = null;
				try {

					if (request != null) {
						request.setAttribute(FormConverterToPDF.RENDERING_TASK_INSTANCE, taskInstanceId);
					}

					formConverter.addStyleSheetsForPDF(iwc);

					UIComponent component = null;
					try {
						component = formConverter.getComponentToRender(iwc, taskInstanceId, null, null);
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error getting UI component for task instance: " + taskInstanceId, e);
					}
					if (component == null) {
						getLogger().warning("Failed to get UI component for task instance: " + taskInstanceId);
						continue;
					}
					byte[] bytes = pdfGenerator.getBytesOfGeneratedPDF(iwc, component, true, true, resetContext);
					if (bytes == null) {
						getLogger().warning("Failed to generate PDF for task instance: " + taskInstanceId);
						continue;
					}

					String name = tiW.getName(iwc, locale);
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
					if (request != null) {
						request.removeAttribute(FormConverterToPDF.RENDERING_TASK_INSTANCE);
					}
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
					LoginSession loginSession = LoginBusinessBean.getLoginSessionBean();
					loginSession.setLoggedOnInfo(loggedOnInfo);
					loginSession.setUser(currentUser);
				}

				iwc.setCurrentLocale(locale);
			}

			if (resetContext || iwc instanceof IWContextMockUp) {
				doResetContext(iwc);
			} else {
				getLogger().info("Not reseting " + iwc.getClass().getName());
			}
		}
	}

	private IWContext getIWContext() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			getLogger().info("Creating IWContext mockup");
			iwc = new IWContextMockUp();
		}

		return iwc;
	}

	private void doResetContext(IWContext iwc) {
		if (iwc == null) {
			getLogger().warning("IWContext is unknown");
			return;
		}

		getLogger().info("Reseting " + iwc.getClass().getName());

		ResponseWriter rw = iwc.getResponseWriter();
		if (rw == null) {
			getLogger().warning("Response writer is unknown");
		} else {
			if (rw instanceof HtmlBufferResponseWriterWrapper) {
				ResponseWriter irw = ((HtmlBufferResponseWriterWrapper) rw).getInitialWriter();
				doResetResponseWriter(irw);
				irw = null;
			}

			doResetResponseWriter(rw);
			rw = null;
		}

		iwc.setResponseWriter(null);
		iwc = null;
	}

	private void doResetResponseWriter(ResponseWriter rw) {
		if (rw == null) {
			return;
		}

		try {
			rw.flush();
			rw.close();
			rw = null;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error resetting response writer " + rw + ", class: " + rw.getClass().getName(), e);
		}
	}
}