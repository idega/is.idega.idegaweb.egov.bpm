package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesEngine;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;
import is.idega.idegaweb.egov.cases.util.CaseConstants;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.div.Div;
import org.apache.myfaces.custom.div.DivTag;
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
import com.idega.presentation.Layer;
import com.idega.presentation.text.DownloadLink;
import com.idega.presentation.text.Text;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $
 *
 * Last modified: $Date: 2008/06/02 19:10:57 $ by $Author: civilis $
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
		String clientId = context.getViewRoot().createUniqueId();
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
		
//		DownloadLink link = new DownloadLink("DL");
//		link.setId("casesBPMAttachmentDownloader");
//		link.setStyleAttribute("display: none;");
//		link.setMediaWriterClass(AttachmentWriter.class);
//		
//		div.getChildren().add(link);
		
//		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
//		facelet.setFaceletURI("/idegaweb/bundles/is.idega.idegaweb.egov.bpm.bundle/facelets/UICasesBPMAssets.xhtml");
//		
//		div.getChildren().add(facelet);
//		div.setValueBinding(renderedAtt, context.getApplication().createValueBinding("#{casesBPMAssetsState.assetsRendered}"));
//		getFacets().put(assetsFacet, div);
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
		
		List<String> scripts = new ArrayList<String>();
		
		try {
			scripts.add(web2Business.getBundleURIToJQueryLib());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		scripts.add(web2Business.getBundleURIToJQGrid()); //
		scripts.add(web2Business.getBundleURIToJQueryUILib(JQueryUIType.UI_EDITABLE)); //
		//scripts.add(bundle.getVirtualPathWithFileNameString("javascript/CasesListHelper.js")); // shouldn't be needed
		scripts.add(CoreConstants.DWR_ENGINE_SCRIPT);
		scripts.add(CoreConstants.DWR_UTIL_SCRIPT);
		//scripts.add("/dwr/interface/CasesEngine.js");
		scripts.add("/dwr/interface/BPMProcessAssets.js"); //
		
		//scripts.add(getBundle((FacesContext)iwc, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/javascript/CasesBPMAssets.js");
//		resource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/javascript/CasesBPMAssets.js");
	
		List<String> css = new ArrayList<String>();
		css.add(web2Business.getBundleURIToJQGridStyles());
		
//		if (caseId == null || CoreConstants.EMPTY.equals(caseId)) {
//			caseId = iwc.getParameter(CasesProcessor.PARAMETER_CASE_PK + "_id");
//		}
		/*
		StringBuilder action = new StringBuilder("initializeCasesList(");
		if (caseId == null || CoreConstants.EMPTY.equals(action)) {
			action.append("null");
		}
		else {
			action.append("'").append(caseId).append("'");
		}
		action.append(");");
		*/
		
		if (CoreUtil.isSingleComponentRenderingProcess(iwc)) {
			container.getChildren().add(new Text(PresentationUtil.getJavaScriptSourceLinesIncludeOnce(scripts)));
			container.getChildren().add(new Text(PresentationUtil.getStyleSheetsSourceLines(css)));
			//container.add(PresentationUtil.getJavaScriptAction(action.toString()));
			
			String clientId = container.getClientId(iwc);
			
			if(clientId == null) {
				
				container.setId(iwc.getViewRoot().createUniqueId());
				clientId = container.getClientId(iwc);
			}
			
			String casesBPMAssetsScript = getBundle((FacesContext)iwc, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/javascript/CasesBPMAssets.js";
			
			CasesBPMAssetsState stateBean = getBeanInstance(CasesBPMAssetsState.beanIdentifier);
			Long processInstanceId = stateBean.getProcessInstanceId();
			Integer caseId = stateBean.getCaseId();
			
			String action = "jQuery.getScript('"+casesBPMAssetsScript+"', function() { CasesBPMAssets.initGrid(jQuery('."+clientId+"')[0], "+processInstanceId.toString()+", "+caseId.toString()+");" +
					"});";
			container.getChildren().add(new Text(PresentationUtil.getJavaScriptAction(action)));
			
		} else {
			PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, scripts);
			PresentationUtil.addStyleSheetsToHeader(iwc, css);
			//PresentationUtil.addJavaScriptActionToBody(iwc, "jQuery(document).ready(function() {"+action.toString()+"});");
		}
	}
}