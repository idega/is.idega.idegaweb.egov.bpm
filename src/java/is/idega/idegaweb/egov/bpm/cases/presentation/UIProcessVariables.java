package is.idega.idegaweb.egov.bpm.cases.presentation;

import java.io.IOException;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.presentation.IWBaseComponent;

public class UIProcessVariables extends IWBaseComponent {

	private static final String containerFacet = "container";
	
	@Override
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
		
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue("div");
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getFaceletURI("UIProcessVariables.xhtml"));

		div.getChildren().add(facelet);

		getFacets().put(containerFacet, div);
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		UIComponent container = getFacet(containerFacet);
		renderChild(context, container);
	}
}