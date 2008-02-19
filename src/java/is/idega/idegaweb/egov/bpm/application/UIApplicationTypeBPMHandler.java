package is.idega.idegaweb.egov.bpm.application;

import javax.faces.application.FacesMessage;
import javax.faces.component.html.HtmlMessage;

import is.idega.idegaweb.egov.application.data.Application;

import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.presentation.ui.Label;
import com.idega.webface.WFUtil;


/**
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/02/19 16:55:12 $ by $Author: anton $
 *
 */
public class UIApplicationTypeBPMHandler extends Block {
	
	private Application application;
	static final String menuParam = "procDefId";
	
	@Override
	public void main(IWContext iwc) throws Exception {
		IWResourceBundle iwrb = getResourceBundle(iwc);
		String procDef = iwc.getParameter(menuParam);
		if(procDef == null || procDef.equals("-1")) {
			iwc.addMessage(menuParam, new FacesMessage(iwrb.getLocalizedString("bpm_proc_select", "'BPM process' field value is not selected")));
		}
				
		DropdownMenu menu = new DropdownMenu(menuParam);
		menu.setId(menuParam);
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
		Layer errorItem = new Layer(Layer.DIV);
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
}
