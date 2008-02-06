package is.idega.idegaweb.egov.bpm.application;

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
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/06 11:49:26 $ by $Author: civilis $
 *
 */
public class UIApplicationTypeBPMHandler extends Block {
	
	private Application application;
	static final String menuParam = "procDefId";
	
	@Override
	public void main(IWContext iwc) throws Exception {
		
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		DropdownMenu menu = new DropdownMenu(menuParam);
		menu.addMenuElement("-1", "Select");
		
		ApplicationTypeBPM appTypeBPM = getApplicationTypeBPM();
		appTypeBPM.fillMenu(menu);
		
		if(application != null) {
		
			menu.setSelectedElement(getApplicationTypeBPM().getSelectedElement(application));
		}
		
		Layer container = new Layer(Layer.SPAN);
		
		Label label = new Label("BPM process", menu);
		container.add(label);
		container.add(menu);
		
		add(container);
	}

	public void setApplication(Application application) {
		this.application = application;
	}
	
	protected ApplicationTypeBPM getApplicationTypeBPM() {
		return (ApplicationTypeBPM)WFUtil.getBeanInstance(ApplicationTypeBPM.beanIdentifier);
	}
}