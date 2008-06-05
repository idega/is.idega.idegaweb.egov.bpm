package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesEngine;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;

import com.idega.block.form.business.XFormToPDFWriter;
import com.idega.block.process.presentation.beans.CaseManagerState;
import com.idega.block.web2.business.JQueryUIType;
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
import com.idega.presentation.text.Text;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.25 $
 *
 * Last modified: $Date: 2008/06/05 16:23:16 $ by $Author: civilis $
 *
 */
public class UICasesBPMAssets extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UICasesBPMAssets";

	private static final String assetsFacet = "assets";
	private static final String assetViewFacet = "assetView";
	private static final String web2BeanIdentifier = "web2bean";
	
	private static final String BPM_ASSETS_JS_SRC = "javascript/CasesBPMAssets.js";
	
	private boolean fullView = false;
	private boolean inCasesComponent = false;
	
	private Long processInstanceId;
	private Integer caseId;
	
//	Long processInstanceId = getProcessInstanceId(context);
//	
//	if(processInstanceId != null) {
//	
//		CasesBPMProcessView pv = getCasesBPMProcessView();
//		Integer caseId = pv.getCaseId(processInstanceId);
//		setCaseId(caseId);
//		
//		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
//		div.setValue("div");
//		div.setStyleClass("casesListGridExpanderStyleClass");
//		UIComponent caseAssets = pv.getCaseManagerView(IWContext.getIWContext(context), processInstanceId, null);
//		div.getChildren().add(caseAssets);
//		getFacets().put(assetsFacet, div);
//	}

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
	
	protected void addClientResources(FacesContext context) {
		
		try {
			Web2Business web2Business = (Web2Business)getBeanInstance(web2BeanIdentifier);
		
			AddResource resource = AddResourceFactory.getInstance(context);
			
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQueryLib());
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQueryUILib(JQueryUIType.UI_TABS));
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQGrid());
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/javascript/CasesBPMAssets.js");
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, CoreConstants.DWR_ENGINE_SCRIPT);
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, "/dwr/util.js");
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, "/dwr/interface/BPMProcessAssets.js");
			
			IWBundle bundle = getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			IWResourceBundle iwrb = bundle.getResourceBundle(IWContext.getIWContext(context));
			
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, bundle.getVirtualPathWithFileNameString(BPM_ASSETS_JS_SRC));
			
			resource.addInlineScriptAtPosition(context, AddResource.HEADER_BEGIN, 
					new StringBuilder("if(Localization == null) var Localization = {};\n")
					.append("Localization.DOCUMENT_NAME = '")	.append(iwrb.getLocalizedString("cases_bpm.document_name", "Document name")).append("';\n")
					.append("Localization.DATE_SUBMITTED = '")	.append(iwrb.getLocalizedString("cases_bpm.date_submitted", "Date submitted")).append("';\n")
					.append("Localization.DATE_CREATED = '")	.append(iwrb.getLocalizedString("cases_bpm.date_created", "Date created")).append("';\n")
					.append("Localization.SUBMITTED_BY = '")	.append(iwrb.getLocalizedString("cases_bpm.submitted_by", "Submitted by")).append("';\n")
					.append("Localization.DOWNLOAD_AS_PDF = '")	.append(iwrb.getLocalizedString("cases_bpm.get_document_as_pdf", "Download document")).append("';\n")
					.append("Localization.SUBJECT = '")			.append(iwrb.getLocalizedString("cases_bpm.subject", "Subject")).append("';\n")
					.append("Localization.FROM = '")			.append(iwrb.getLocalizedString("cases_bpm.from", "From")).append("';\n")
					.append("Localization.RECEIVE_DATE = '")	.append(iwrb.getLocalizedString("cases_bpm.receive_date", "Receive date")).append("';\n")
					.append("Localization.FILE_NAME = '")		.append(iwrb.getLocalizedString("cases_bpm.file_name", "File name")).append("';\n")
					.append("Localization.FILE_DESCRIPTION = '").append(iwrb.getLocalizedString("cases_bpm.file_description", "File description")).append("';\n")
					.append("Localization.FILE_SIZE = '")		.append(iwrb.getLocalizedString("cases_bpm.file_size", "File size")).append("';\n")
					.append("Localization.TASK_NAME = '")		.append(iwrb.getLocalizedString("cases_bpm.task_name", "Task name")).append("';\n")
					.append("Localization.TAKEN_BY = '")		.append(iwrb.getLocalizedString("cases_bpm.taken_by", "Taken by")).append("';\n")
					.append("Localization.STATUS = '")			.append(iwrb.getLocalizedString("cases_bpm.status", "Status")).append("';\n")
					
					.toString()
			);
		
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQueryUILib(JQueryUIType.UI_TABS_CSS));
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQGridStyles());

			/*
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToMootoolsLib());
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, web2Business.getMoodalboxScriptPath());
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, web2Business.getMoodalboxStylePath());
			
			
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, "/dwr/interface/CasesBPMAssets.js");
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, "/dwr/engine.js");
			
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/javascript/sortableTable.js");
			resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/javascript/CasesListHelper.js");
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/style/cases.css");
			*/
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		UIComponent assets = getFacet(assetsFacet);
		UIComponent assetView = getFacet(assetViewFacet);
		
		if(assets.isRendered()) {
			
//			TODO: add assets grid client resources
			
			//addClientResources(context);
			
			IWContext iwc = IWContext.getIWContext(context);
			addClientResources(iwc, assets);
//			totally crap
//			IWContext iwc = IWContext.getIWContext(context);
//			IWBundle bundle = iwc.getIWMainApplication().getBundle(CaseConstants.IW_BUNDLE_IDENTIFIER);
//			String caseId = getCaseId() == null ? null : getCaseId().toString();
//			CasesListBuilderImpl.addWeb2Stuff(caseId, iwc, bundle, null);
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
		
		List<String> scripts = new ArrayList<String>();
		
		boolean isSingle = CoreUtil.isSingleComponentRenderingProcess(iwc);
		
		if(!isSingle)
//			TODO: do the same with single when iw_core will be able to include js+callback without jquery
			scripts.add(web2Business.getBundleURIToJQueryLib());
		
//		include always
		scripts.add(web2Business.getBundleURIToJQGrid());
		scripts.add(web2Business.getBundleURIToJQueryUILib(JQueryUIType.UI_EDITABLE));
		scripts.add(CoreConstants.DWR_ENGINE_SCRIPT);
		scripts.add(CoreConstants.DWR_UTIL_SCRIPT);
		scripts.add("/dwr/interface/BPMProcessAssets.js");
		scripts.add(bundle.getResourcesVirtualPath()+"/javascript/CasesBPMAssets.js");
		
		List<String> css = new ArrayList<String>();
		css.add(web2Business.getBundleURIToJQGridStyles());
		
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
			
			.append("CasesBPMAssets.Loc.inited = true; }")
			
			.toString();
		
		String clientId = container.getClientId(iwc);
		
		if(clientId == null) {
			
			container.setId(iwc.getViewRoot().createUniqueId());
			clientId = container.getClientId(iwc);
		}
		
		CasesBPMAssetsState stateBean = getBeanInstance(CasesBPMAssetsState.beanIdentifier);
		Long processInstanceId = stateBean.getProcessInstanceId();
		Integer caseId = stateBean.getCaseId();
		
		if (isSingle) {
			
			StringBuilder scriptsPathes = new StringBuilder("[");
			
			for (Iterator<String> iterator = scripts.iterator(); iterator.hasNext();) {
				String script = iterator.next();
				
				scriptsPathes.append("'")
				.append(script);
				
				if(iterator.hasNext())
					scriptsPathes.append("', ");
				else
					scriptsPathes.append("']");
			}
			
			String callback = "function() { "+gridLocalization+" CasesBPMAssets.initGrid(jQuery('div."+clientId+"')[0], "+processInstanceId.toString()+", "+caseId.toString()+");}";
			
			String action = 
				"IWCORE.includeScriptsBatch(" +
				scriptsPathes + ", "+callback+
				");";
			
			@SuppressWarnings("unchecked")
			List<UIComponent> containerChildren = container.getChildren();
			containerChildren.add(new Text(PresentationUtil.getJavaScriptAction(action)));
			containerChildren.add(new Text(PresentationUtil.getStyleSheetsSourceLinesIncludeOnce(css)));
			
		} else {
			
			String action = 
				"CasesBPMAssets.initGrid(jQuery('div."+clientId+"')[0], "+processInstanceId.toString()+", "+caseId.toString()+");";
			
			PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, scripts);
			PresentationUtil.addStyleSheetsToHeader(iwc, css);
			PresentationUtil.addJavaScriptActionToBody(iwc, "jQuery(document).ready(function() {"+action.toString()+"});");
		}
	}
}