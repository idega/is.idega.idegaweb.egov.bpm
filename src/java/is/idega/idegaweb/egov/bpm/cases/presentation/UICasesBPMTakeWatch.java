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
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/25 16:16:25 $ by $Author: civilis $
 *
 */
public class UICasesBPMTakeWatch extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UICasesBPMTakeWatch";

	private static final String takeWatchFacet = "takeWatch";

	@Override
	@SuppressWarnings("unchecked")
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
	
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/is.idega.idegaweb.egov.bpm.bundle/facelets/UICasesBPMTakeWatch.xhtml");
		
		div.getChildren().add(facelet);
		getFacets().put(takeWatchFacet, div);
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		UIComponent takeWatch = getFacet(takeWatchFacet);
		renderChild(context, takeWatch);
	}
}