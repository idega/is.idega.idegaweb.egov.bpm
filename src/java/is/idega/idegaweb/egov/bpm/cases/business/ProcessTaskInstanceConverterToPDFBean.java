package is.idega.idegaweb.egov.bpm.cases.business;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.presentation.FormViewer;
import com.idega.bpm.BPMConstants;
import com.idega.bpm.pdf.business.ProcessTaskInstanceConverterToPDF;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Scope("session")
@Service(ProcessTaskInstanceConverterToPDF.STRING_BEAN_IDENTIFIER)
public class ProcessTaskInstanceConverterToPDFBean implements ProcessTaskInstanceConverterToPDF {

	private static final Logger logger = Logger.getLogger(ProcessTaskInstanceConverterToPDFBean.class.getName());
	
	public String getGeneratedPDFFromXForm(String taskInstanceId, String formId, String uploadPath, boolean checkExistence) {
		if (StringUtil.isEmpty(taskInstanceId) && StringUtil.isEmpty(formId)) {
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
		
		String xformInPDF = getXFormInPDF(iwc, taskInstanceId, formId, uploadPath, checkExistence);
		if (StringUtil.isEmpty(xformInPDF)) {
			logger.log(Level.SEVERE, new StringBuilder("Unable to get 'XForm' with ").append(StringUtil.isEmpty(formId) ? "task instance id: " + taskInstanceId :
														"form id: " + formId).toString());
			return null;
		}
		return xformInPDF;
	}
	
	private String getXFormInPDF(IWContext iwc, String taskInstanceId, String formId, String pathInSlide, boolean checkExistence) {
		IWSlideService slide = null;
		try {
			slide = (IWSlideService) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
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
			return generatePDF(iwc, slide, taskInstanceId, formId, pathInSlide, pdfName, pathToForm);
		}
		
		return pathToForm;
	}
	
	private WebdavResource getXFormInPDFResource(IWSlideService slide, String pathToForm) {
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
	
	private UIComponent getComponentToRender(IWContext iwc, String taskInstanceId, String formId) {
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
		else if (!StringUtil.isEmpty(formId)) {
			Application application = iwc.getApplication();
			viewer = application.createComponent(FormViewer.COMPONENT_TYPE);
			((FormViewer) viewer).setFormId(formId);
		}
		
		return viewer;
	}
	
	private String generatePDF(IWContext iwc, IWSlideService slide, String taskInstanceId, String formId, String pathInSlide, String pdfName, String pathToForm) {
		UIComponent viewer = getComponentToRender(iwc, taskInstanceId, formId);
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

}
