package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;

import com.idega.block.web2.business.JQueryUIType;
import com.idega.block.web2.business.Web2Business;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.jbpm.exe.AttachmentWriter;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.text.DownloadLink;
import com.idega.util.CoreConstants;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/03/30 11:12:33 $ by $Author: civilis $
 *
 */
public class UICasesBPMAssets extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UICasesBPMAssets";

	private static final String assetsFacet = "assets";
	private static final String assetViewFacet = "assetView";
	private static final String web2BeanIdentifier = "web2bean";
	
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
			
			
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQueryUILib(JQueryUIType.UI_TABS_CSS));
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, web2Business.getBundleURIToJQGridStyles());
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, "/idegaweb/bundles/org.jboss.jbpm.bundle/resources/style/processArtifactsList.css");

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
		this.fullView = fullView;
	}

	public boolean isInCasesComponent() {
		return inCasesComponent;
	}

	public void setInCasesComponent(boolean inCasesComponent) {
		this.inCasesComponent = inCasesComponent;
	}
}