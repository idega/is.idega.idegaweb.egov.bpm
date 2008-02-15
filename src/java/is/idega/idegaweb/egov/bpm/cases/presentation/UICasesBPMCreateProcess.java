package is.idega.idegaweb.egov.bpm.cases.presentation;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;

import com.idega.facelets.ui.FaceletComponent;
import com.idega.presentation.IWBaseComponent;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/02/15 10:19:35 $ by $Author: civilis $
 *
 */
public class UICasesBPMCreateProcess extends IWBaseComponent {
	
	private static final String containerFacet = "container";

	@Override
	@SuppressWarnings("unchecked")
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
		
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue("div");
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/is.idega.idegaweb.egov.bpm.bundle/facelets/UICasesBPMCreateProcess.xhtml");

		div.getChildren().add(facelet);
		
//		HtmlTag xx = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
//		xx.setValue("div");
//		div.getChildren().add(xx);
		
		///div.getChildren().add(form);
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
		
		if(container != null) {
//			Form form = new Form();
//			form.add(container);
			container.setRendered(true);
			renderChild(context, container);
		}
	}
}