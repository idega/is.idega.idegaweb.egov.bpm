package is.idega.idegaweb.egov.bpm.cases.business;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.presentation.FormViewer;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.BPMConstants;
import com.idega.bpm.pdf.business.ProcessTaskInstanceConverterToPDF;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Scope("singleton")
@Service(ProcessTaskInstanceConverterToPDF.STRING_BEAN_IDENTIFIER)
public class ProcessTaskInstanceConverterToPDFBean implements ProcessTaskInstanceConverterToPDF {

	private static final Logger logger = Logger.getLogger(ProcessTaskInstanceConverterToPDFBean.class.getName());
	
	@Autowired private BPMFactory bpmFactory;
	
	public String getGeneratedPDFFromXForm(String taskInstanceId, String formId, String formSubmitionId, String uploadPath, boolean checkExistence) {
		if (StringUtil.isEmpty(taskInstanceId) && StringUtil.isEmpty(formId) && StringUtil.isEmpty(formSubmitionId)) {
			logger.log(Level.SEVERE, "Do not know what to generate!");
			return null;
		}
		if (StringUtil.isEmpty(uploadPath)) {
			uploadPath = BPMConstants.PDF_OF_XFORMS_PATH_IN_SLIDE;
		}
		if (!uploadPath.startsWith(CoreConstants.SLASH)) {
			uploadPath = CoreConstants.SLASH + uploadPath;
		}
		if (!uploadPath.endsWith(CoreConstants.SLASH)) {
			uploadPath += CoreConstants.SLASH;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			logger.log(Level.SEVERE, "IWContext is unavailable!");
			return null;
		}
		
		addStyleSheetsForPDF(iwc);
		
		String xformInPDF = getXFormInPDF(iwc, taskInstanceId, formId, formSubmitionId, uploadPath, checkExistence);
		if (StringUtil.isEmpty(xformInPDF)) {
			logger.log(Level.SEVERE, new StringBuilder("Unable to get 'XForm' with ").append(StringUtil.isEmpty(formId) ? "task instance id: " + taskInstanceId :
														"form id: " + formId).toString());
			return null;
		}
		return xformInPDF;
	}
	
	@SuppressWarnings("unchecked")
	private void addStyleSheetsForPDF(IWContext iwc) {
		IWBundle bundle = iwc.getIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER);
		String pdfCss = bundle.getVirtualPathWithFileNameString("style/pdf.css");
		List<String> resources = null;
		Object o = iwc.getSessionAttribute(PresentationUtil.ATTRIBUTE_CSS_SOURCE_LINE_FOR_HEADER);
		if (o instanceof List) {
			resources = (List) o;
		}
		else {
			resources = new ArrayList<String>();
		}
		if (!resources.contains(pdfCss)) {
			resources.add(pdfCss);
			iwc.setSessionAttribute(PresentationUtil.ATTRIBUTE_CSS_SOURCE_LINE_FOR_HEADER, resources);
		}
	}
	
	public String getHashValueForGeneratedPDFFromXForm(String taskInstanceId, boolean checkExistence) {
		if (StringUtil.isEmpty(taskInstanceId)) {
			logger.log(Level.INFO, "Only taks instances can have binary variables!");
			return null;
		}
		Long taskId = null;
		try {
			taskId = Long.valueOf(taskInstanceId);
		} catch(NumberFormatException e) {
			logger.log(Level.SEVERE, "Invalid task instance id: " + taskInstanceId, e);
		}
		if (taskId == null) {
			return null;
		}
		
		TaskInstanceW taskInstance = null;
		try {
			taskInstance = getBpmFactory().getProcessManagerByTaskInstanceId(taskId).getTaskInstance(taskId);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error getting task instance by id: " + taskInstanceId, e);
		}
		if (taskInstance == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		PDFGenerator generator = ELUtil.getInstance().getBean(CoreConstants.SPRING_BEAN_NAME_PDF_GENERATOR);
		if (generator == null) {
			return null;
		}
		
		addStyleSheetsForPDF(iwc);
		
		UIComponent viewer = getComponentToRender(iwc, taskInstanceId, null, null);
		if (viewer == null) {
			logger.log(Level.SEVERE, "Unable to get viewer for taskInstance: " + taskInstanceId);
			return null;
		}
		boolean isFormViewer = viewer instanceof FormViewer;
		if (isFormViewer) {
			((FormViewer) viewer).setPdfViewer(true);
		}
		InputStream streamToPDF = generator.getStreamToGeneratedPDF(iwc, viewer, true, isFormViewer);
		if (streamToPDF == null) {
			return null;
		}
		
		ProcessArtifacts procArtifacts = ELUtil.getInstance().getBean(CoreConstants.SPRING_BEAN_NAME_PROCESS_ARTIFACTS);
		String fileName = procArtifacts.getFileNameForGeneratedPDFFromTaskInstance(taskInstanceId);
		String description = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc)
		.getLocalizedString("auto_generated_pdf", "Generated PDF from document");
		
		BinaryVariable newAttachment = null;
		Variable variable = new Variable("generated_pdf_from_document_task", VariableDataType.FILE);
		try {
			newAttachment = taskInstance.addAttachment(variable, fileName, description, streamToPDF);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Unable to set binary variable for task instance: " + taskInstanceId, e);
			return null;
		} finally {
			closeInputStream(streamToPDF);
		}
		
		return newAttachment == null ? null : String.valueOf(newAttachment.getHash());
	}
	
	private void closeInputStream(InputStream stream) {
		if (stream == null) {
			return;
		}
		
		try {
			stream.close();
		} catch (IOException e) {}
	}
	
	private IWSlideService getSlideService() {
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
		} catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private String getXFormInPDF(IWContext iwc, String taskInstanceId, String formId, String formSubmitionId, String pathInSlide, boolean checkExistence) {
		IWSlideService slide = getSlideService();
		if (slide == null) {
			return null;
		}
		
		String prefix = formId == null ? taskInstanceId : formId;
		prefix = prefix == null ? String.valueOf(System.currentTimeMillis()) : prefix;
		String pdfName = new StringBuilder("Form_").append(prefix).append(".pdf").toString();
		String pathToForm = pathInSlide + pdfName;
		
		boolean needToGenerate = true;
		if (checkExistence) {
			WebdavResource xformInPDF = getXFormInPDFResource(slide, pathToForm);
			needToGenerate = xformInPDF == null || !xformInPDF.exists();
		}
		if (needToGenerate) {
			return generatePDF(iwc, slide, taskInstanceId, formId, formSubmitionId, pathInSlide, pdfName, pathToForm);
		}
		
		return pathToForm;
	}
	
	private WebdavResource getXFormInPDFResource(IWSlideService slide, String pathToForm) {
		if (slide == null || StringUtil.isEmpty(pathToForm)) {
			return null;
		}
		
		WebdavResource xformInPDF = null;
		try {
			xformInPDF = slide.getWebdavResourceAuthenticatedAsRoot(pathToForm);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return xformInPDF;
	}
	
	private UIComponent getComponentToRender(IWContext iwc, String taskInstanceId, String formId, String formSubmitionId) {
		UIComponent viewer = null;
		if (!StringUtil.isEmpty(taskInstanceId)) {
			ProcessArtifacts bean = ELUtil.getInstance().getBean(CoreConstants.SPRING_BEAN_NAME_PROCESS_ARTIFACTS);
			if (bean == null) {
				return null;
			}
			try {
				return bean.getViewInUIComponent(Long.valueOf(taskInstanceId));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (!StringUtil.isEmpty(formId) || !StringUtil.isEmpty(formSubmitionId)) {
			Application application = iwc.getApplication();
			viewer = application.createComponent(FormViewer.COMPONENT_TYPE);
		
			((FormViewer) viewer).setPdfViewer(true);
			if (!StringUtil.isEmpty(formId)) {
				((FormViewer) viewer).setFormId(formId);
			}
			else {
				((FormViewer) viewer).setSubmissionId(formSubmitionId);
			}
		}
		
		return viewer;
	}
	
	private String generatePDF(IWContext iwc, IWSlideService slide, String taskInstanceId, String formId, String formSubmitionId, String pathInSlide, String pdfName, String pathToForm) {
		UIComponent viewer = getComponentToRender(iwc, taskInstanceId, formId, formSubmitionId);
		if (viewer == null) {
			logger.log(Level.SEVERE, "Unable to get viewer for " + taskInstanceId == null ? "xform: " + formId : "taskInstance: " + taskInstanceId);
			return null;
		}
		boolean isFormViewer = viewer instanceof FormViewer;
		if (isFormViewer) {
			((FormViewer) viewer).setPdfViewer(true);
		}
		
		PDFGenerator generator = ELUtil.getInstance().getBean(CoreConstants.SPRING_BEAN_NAME_PDF_GENERATOR);
		if (generator == null) {
			return null;
		}
		if (generator.generatePDF(iwc, viewer, pdfName, pathInSlide, true, isFormViewer)) {
			return pathToForm;
		}
		
		logger.log(Level.SEVERE, "Unable to generate PDF for " + taskInstanceId == null ? "xform: " + formId: "taskInstance: " + taskInstanceId);
		return null;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

}
