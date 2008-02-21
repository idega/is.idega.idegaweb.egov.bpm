package is.idega.idegaweb.egov.bpm.application;

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlMessage;
import javax.faces.context.FacesContext;

import is.idega.idegaweb.egov.application.business.ApplicationType.ApplicationTypeHandlerComponent;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.presentation.ApplicationCreator;

import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.presentation.ui.Label;
import com.idega.webface.WFUtil;


/**
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/02/21 10:30:51 $ by $Author: anton $
 *
 */
public class UIApplicationTypeBPMHandler extends Block implements ApplicationTypeHandlerComponent {
	
	private Application application;
	static final String MENU_PARAM = "procDefId";

	
	@Override
	public void main(IWContext iwc) throws Exception {
		String procDef = iwc.getParameter(MENU_PARAM);		
		
		DropdownMenu menu = new DropdownMenu(MENU_PARAM);
		menu.setId(MENU_PARAM);
		menu.addMenuElement("-1", "Select");
		
		ApplicationTypeBPM appTypeBPM = getApplicationTypeBPM();
		appTypeBPM.fillMenu(menu);
		
		if(application != null) {		
			menu.setSelectedElement(getApplicationTypeBPM().getSelectedElement(application));
		}
		if(procDef != null && !procDef.equals("-1")) {
			menu.setSelectedElement(procDef);
		}
		
		Layer container = new Layer(Layer.SPAN);
		Layer errorItem = new Layer(Layer.SPAN);
		errorItem.setStyleClass("error");
		
		Label label = new Label("BPM process", menu);
		HtmlMessage msg = (HtmlMessage)iwc.getApplication().createComponent(HtmlMessage.COMPONENT_TYPE);
		msg.setFor(menu.getId());
		errorItem.add(msg);
		container.add(label);
		container.add(menu);
		container.add(errorItem);
		
		add(container);
	}

	public void setApplication(Application application) {
		this.application = application;
	}
	
	protected ApplicationTypeBPM getApplicationTypeBPM() {
		return (ApplicationTypeBPM)WFUtil.getBeanInstance(ApplicationTypeBPM.beanIdentifier);
	}
	
	public UIComponent getUIComponent(FacesContext ctx, Application app) {

		UIApplicationTypeBPMHandler h = new UIApplicationTypeBPMHandler();
		h.setApplication(app);
		
		return h;
	}
	
	public boolean validate(IWContext iwc) {
		boolean valid = true;
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		String procDef = iwc.getParameter(MENU_PARAM);
		String action = iwc.getParameter(ApplicationCreator.ACTION);
		
		if((procDef == null || procDef.equals("-1")) && ApplicationCreator.SAVE_ACTION.equals(action)) {
			iwc.addMessage(MENU_PARAM, new FacesMessage(iwrb.getLocalizedString("bpm_proc_select", "'BPM process' field value is not selected")));
			valid = false;
		}
		return valid;
	}
}
