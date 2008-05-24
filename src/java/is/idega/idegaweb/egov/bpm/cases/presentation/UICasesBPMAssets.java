package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;

import com.idega.block.process.presentation.beans.CaseManagerState;
import com.idega.block.web2.business.JQueryUIType;
import com.idega.block.web2.business.Web2Business;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.DownloadLink;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $
 *
 * Last modified: $Date: 2008/05/24 16:23:43 $ by $Author: eiki $
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

	@Override
	@SuppressWarnings("unchecked")
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
	
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		DownloadLink link = new DownloadLink("DL");
		link.setId("casesBPMAttachmentDownloader");
		link.setStyleAttribute("display: none;");
		link.setMediaWriterClass(AttachmentWriter.class);
		
		div.getChildren().add(link);
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/is.idega.idegaweb.egov.bpm.bundle/facelets/UICasesBPMAssets.xhtml");
		
		div.getChildren().add(facelet);
		div.setValueBinding(renderedAtt, context.getApplication().createValueBinding("#{casesBPMAssetsState.assetsRendered}"));
		getFacets().put(assetsFacet, div);
		
		div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/is.idega.idegaweb.egov.bpm.bundle/facelets/UICasesBPMAssetView.xhtml");

		div.getChildren().add(facelet);
		div.setValueBinding(renderedAtt, context.getApplication().createValueBinding("#{casesBPMAssetsState.assetViewRendered}"));
		getFacets().put(assetViewFacet, div);
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
					.append("Localization.SUBJECT = '")			.append(iwrb.getLocalizedString("cases_bpm.subject", "Subject")).append("';\n")
					.append("Localization.FROM = '")			.append(iwrb.getLocalizedString("cases_bpm.from", "From")).append("';\n")
					.append("Localization.RECIEVE_DATE = '")	.append(iwrb.getLocalizedString("cases_bpm.recieve_date", "Recieve date")).append("';\n")
					.append("Localization.FILE_NAME = '")		.append(iwrb.getLocalizedString("cases_bpm.file_name", "File name")).append("';\n")
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
			addClientResources(context);
			renderChild(context, assets);
			
		} else if(assetView.isRendered()) {
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
}