package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesEngine;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;

import com.idega.bpm.pdf.XFormToPDFWriter;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWBundle;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.text.DownloadLink;

public class UICasesListAsset extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UICasesListAsset";
	private static final String CASES_LIST_COMPONENT =  "casesListFaceletBasedComponent";
	
	private Integer caseId;
	private boolean downloadDocument = false;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
		
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		if (caseId != null) {
			CasesBPMAssetsState stateBean = (CasesBPMAssetsState) getBeanInstance(CasesBPMAssetsState.beanIdentifier);
			stateBean.setCaseId(caseId);
		}
		
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
		getFacets().put(CASES_LIST_COMPONENT, div);
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		UIComponent assets = getFacet(CASES_LIST_COMPONENT);
		if (assets.isRendered()) {
			renderChild(context, assets);
		}
	}

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}

	public boolean isDownloadDocument() {
		return downloadDocument;
	}

	public void setDownloadDocument(boolean downloadDocument) {
		this.downloadDocument = downloadDocument;
	}
}
