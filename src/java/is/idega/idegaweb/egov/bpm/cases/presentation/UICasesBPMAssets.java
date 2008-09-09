package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesEngine;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;

import com.idega.block.form.business.XFormToPDFWriter;
import com.idega.block.process.presentation.beans.CaseManagerState;
import com.idega.block.web2.business.Web2Business;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.DownloadLink;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.29 $
 *
 * Last modified: $Date: 2008/09/09 09:30:06 $ by $Author: arunas $
 *
 */
public class UICasesBPMAssets extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UICasesBPMAssets";

	private static final String assetsFacet = "assets";
	private static final String assetViewFacet = "assetView";
	
	private boolean fullView = false;
	private boolean inCasesComponent = false;
	private boolean usePdfDownloadColumn = true;
	
	private Long processInstanceId;
	private Integer caseId;

	@Override
	@SuppressWarnings("unchecked")
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);

//		<assets grid component>
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		String clientId = null;
		if (CoreUtil.isSingleComponentRenderingProcess(context)) {
			Random numberGenerator = new Random();
			clientId = new StringBuilder(CoreConstants.UNDER).append(numberGenerator.nextInt(Integer.MAX_VALUE)).toString();
		}
		else {
			clientId = context.getViewRoot().createUniqueId();
		}
		div.setId(clientId);
		div.setStyleClass(clientId);
		div.setValue(divTag);
		
		HtmlTag linksContainer = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		linksContainer.setValue(divTag);
		linksContainer.setStyleClass("hiddenLinksForCasesContainerStyle");
		div.getChildren().add(linksContainer);
		
		DownloadLink attachmentLink = new DownloadLink();
		attachmentLink.setStyleClass(CasesEngine.FILE_DOWNLOAD_LINK_STYLE_CLASS);
		attachmentLink.setMediaWriterClass(AttachmentWriter.class);
		linksContainer.getChildren().add(attachmentLink);
		
		DownloadLink pdfLink = new DownloadLink();
		pdfLink.setStyleClass(CasesEngine.PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS);
		pdfLink.setMediaWriterClass(XFormToPDFWriter.class);
		linksContainer.getChildren().add(pdfLink);
		
		IWBundle bundle = getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(bundle.getFaceletURI("UICasesListAsset.xhtml"));
		div.getChildren().add(facelet);
		
		div.setValueBinding(renderedAtt, context.getApplication().createValueBinding("#{casesBPMAssetsState.assetsRendered}"));
		getFacets().put(assetsFacet, div);
//		</assets grid component>
//		<asset view>
		
		div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(bundle.getFaceletURI("UICasesBPMAssetView.xhtml"));

		div.getChildren().add(facelet);
		div.setValueBinding(renderedAtt, context.getApplication().createValueBinding("#{casesBPMAssetsState.assetViewRendered}"));
		getFacets().put(assetViewFacet, div);
//		</asset view>
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		UIComponent assets = getFacet(assetsFacet);
		UIComponent assetView = getFacet(assetViewFacet);
		
		if(assets.isRendered()) {
			
//			TODO: add assets grid client resources
			addClientResources(IWContext.getIWContext(context), assets);
			renderChild(context, assets);
			
		} else if(assetView.isRendered()) {
//			TODO: add asset client resources
			renderChild(context, assetView);
		}
	}

	public boolean isFullView() {
		return fullView;
	}

	public void setFullView(boolean fullView) {
		CaseManagerState caseHandlerState = (CaseManagerState)WFUtil.getBeanInstance(CaseManagerState.beanIdentifier);
		caseHandlerState.setFullView(fullView);
		this.fullView = fullView;
	}

	public boolean isInCasesComponent() {
		return inCasesComponent;
	}

	public void setInCasesComponent(boolean inCasesComponent) {
		this.inCasesComponent = inCasesComponent;
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected Long resolveProcessInstanceId(FacesContext fctx) {
		
		String piIdParam = (String)fctx.getExternalContext().getRequestParameterMap().get("piId");
		Long piId;
		
		if(piIdParam != null && !CoreConstants.EMPTY.equals(piIdParam)) {

			piId = new Long(piIdParam);
		} else
			piId = null;
		
		return piId;
	}
	
	public Long getProcessInstanceId(FacesContext fctx) {
		
		if(processInstanceId == null) {
			
			processInstanceId = resolveProcessInstanceId(fctx);
		}
		
		return processInstanceId;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public CasesBPMProcessView getCasesBPMProcessView() {
		
		return (CasesBPMProcessView)WFUtil.getBeanInstance(CasesBPMProcessView.BEAN_IDENTIFIER);
	}
	
	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		
		if (caseId != null) {
			CasesBPMAssetsState stateBean = getBeanInstance(CasesBPMAssetsState.beanIdentifier);
			stateBean.setCaseId(caseId);
		}
		this.caseId = caseId;
	}
	
	private void addClientResources(IWContext iwc, UIComponent container) {
		Web2Business web2Business = getBeanInstance("web2bean");
		IWBundle bundle = getBundle((FacesContext)iwc, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		//	CSS sources
		PresentationUtil.addStyleSheetToHeader(iwc, web2Business.getBundleURIToJQGridStyles());
		PresentationUtil.addStyleSheetToHeader(iwc, web2Business.getBundleUriToHumanizedMessagesStyleSheet());
		
		boolean isSingle = CoreUtil.isSingleComponentRenderingProcess(iwc);
		
		//	JS sources
		List<String> scripts = new ArrayList<String>();
		if (!isSingle) {
			scripts.add(web2Business.getBundleURIToJQueryLib());
		}
		scripts.add(web2Business.getBundleURIToJQGrid());
		scripts.add(CoreConstants.DWR_ENGINE_SCRIPT);
		scripts.add(CoreConstants.DWR_UTIL_SCRIPT);
		scripts.add("/dwr/interface/BPMProcessAssets.js");
		scripts.add(web2Business.getBundleUriToHumanizedMessagesScript());
		scripts.add(bundle.getResourcesVirtualPath()+"/javascript/CasesBPMAssets.js");
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, scripts);

		//	JS actions
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		String gridLocalization = new StringBuilder(
				"if(CasesBPMAssets.Loc == null || !CasesBPMAssets.Loc.inited) { \nif(CasesBPMAssets.Loc == null) { CasesBPMAssets.Loc = { inited: false }; }\n")
		
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_CONTACT_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.human_name", "Name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_TASK_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.task_name", "Task name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FORM_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.document_name", "Document name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_SENDER = '")					.append(iwrb.getLocalizedString("sender", "Sender")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_DATE = '")						.append(iwrb.getLocalizedString("date", "Date")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_TAKEN_BY = '")					.append(iwrb.getLocalizedString("cases_bpm.assigned_to", "Taken by")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_EMAIL_ADDRESS = '")			.append(iwrb.getLocalizedString("email_address", "E-mail address")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_PHONE_NUMBER = '")				.append(iwrb.getLocalizedString("phone_number", "Phone number")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_ADDRESS = '")					.append(iwrb.getLocalizedString("address", "Address")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_SUBJECT = '")					.append(iwrb.getLocalizedString("cases_bpm.subject", "Subject")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_DESCRIPTION = '")			.append(iwrb.getLocalizedString("cases_bpm.file_description", "Descriptive name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.file_name", "File name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS = '")		.append(iwrb.getLocalizedString("cases_bpm.change_access_rights", "Change access rights")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_DOWNLOAD_DOCUMENT_AS_PDF = '")	.append(iwrb.getLocalizedString("cases_bpm.get_document_as_pdf", "Download document")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_SIZE = '")				.append(iwrb.getLocalizedString("cases_bpm.file_size", "File size")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_SUBMITTED_BY = '")				.append(iwrb.getLocalizedString("cases_bpm.submitted_by", "Submitted by")).append("';\n")
			
			.append("CasesBPMAssets.Loc.inited = true; }\n")
			
			.toString();
		
		String clientId = container.getClientId(iwc);
		if (clientId == null) {
			container.setId(iwc.getViewRoot().createUniqueId());
			clientId = container.getClientId(iwc);
		}
		
		CasesBPMAssetsState stateBean = getBeanInstance(CasesBPMAssetsState.beanIdentifier);
		Long processInstanceId = stateBean.getProcessInstanceId();
		Integer caseId = stateBean.getCaseId();
		
		String mainAction = new StringBuffer(gridLocalization).append("\n CasesBPMAssets.initGrid(jQuery('div.").append(clientId).append("')[0], ")
			.append(processInstanceId.toString()).append(", ").append(caseId.toString()).append(", ").append(isUsePdfDownloadColumn()).append(");").toString();
		
		if (!isSingle) {
			mainAction = new StringBuffer("jQuery(document).ready(function() {\n").append(mainAction).append("\n});").toString();
		}
		
		PresentationUtil.addJavaScriptActionToBody(iwc, mainAction);
	}

	public boolean isUsePdfDownloadColumn() {
		return usePdfDownloadColumn;
	}

	public void setUsePdfDownloadColumn(boolean usePdfDownloadColumn) {
		this.usePdfDownloadColumn = usePdfDownloadColumn;
	}
	
}