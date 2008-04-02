package is.idega.idegaweb.egov.bpm.cases.presentation;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;
import org.jboss.jbpm.IWBundleStarter;

import com.idega.facelets.ui.FaceletComponent;
import com.idega.presentation.IWBaseComponent;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/02 19:23:56 $ by $Author: civilis $
 *
 */
public class UICasesBPMProcessMgmt extends IWBaseComponent {
	
	private static final String containerFacet = "container";

	@Override
	@SuppressWarnings("unchecked")
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
		
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue("div");
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/is.idega.idegaweb.egov.bpm.bundle/facelets/UICasesBPMProcessMgmt.xhtml");

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
		
		try {
			AddResource resource = AddResourceFactory.getInstance(context);
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/style/egovBPM.css");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		UIComponent container = getFacet(containerFacet);
		renderChild(context, container);
	}
}