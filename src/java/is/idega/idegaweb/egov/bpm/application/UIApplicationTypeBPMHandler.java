package is.idega.idegaweb.egov.bpm.application;

import is.idega.idegaweb.egov.application.business.ApplicationType.ApplicationTypeHandlerComponent;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.presentation.ApplicationCreator;
import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlMessage;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.web2.business.Web2Business;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.data.ICRole;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.IWUserContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.ui.CheckBox;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.presentation.ui.Label;
import com.idega.presentation.ui.SelectOption;
import com.idega.presentation.ui.SelectPanel;
import com.idega.util.expression.ELUtil;


/**
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/09/02 12:56:20 $ by $Author: civilis $
 *
 */
public class UIApplicationTypeBPMHandler extends Block implements ApplicationTypeHandlerComponent {

	public static final String rolesToStartCaseNeedToBeCheckedParam = "rolesToStartCaseNeedToBeChecked";
	public static final String rolesToStartCaseParam = "rolesToStartCase";
	
	private Application application;
	static final String MENU_PARAM = "procDefId";
	
	@Autowired private BPMFactory bpmFactory;
	@Autowired private ApplicationTypeBPM applicationTypeBPM;
	@Autowired private Web2Business web2business;

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
		
		Layer rolesContainer = new Layer(Layer.DIV);
		container.add(rolesContainer);
		
		CheckBox cb = new CheckBox(rolesToStartCaseNeedToBeCheckedParam, Boolean.TRUE.toString());
		
		Web2Business w2b = getWeb2business();
		
		label = new Label("Only roles selected can submit application", cb);
		
		rolesContainer.add(label);
		rolesContainer.add(cb);
		
		rolesContainer = new Layer(Layer.DIV);
		container.add(rolesContainer);
		
		AccessController ac = iwc.getAccessController();
		
		Collection<ICRole> roles = ac.getAllRoles();
		
		final List<String> selectedRoles;
		
		boolean isSelected = iwc.isParameterSet(rolesToStartCaseNeedToBeCheckedParam);

		if(iwc.isParameterSet(rolesToStartCaseParam)) {
			
			String[] vals = iwc.getParameterValues(rolesToStartCaseParam);
			selectedRoles = Arrays.asList(vals);
			
		} else if((procDef != null && !procDef.equals("-1")) || application != null) {
			
			final Long pdId;

			if(procDef != null && !procDef.equals("-1")) {
				
				pdId = new Long(procDef);
				
			} else
				pdId = new Long(getApplicationTypeBPM().getSelectedElement(application));
			
			selectedRoles = getApplicationTypeBPM().getRolesCanStartProcess(pdId, application.getPrimaryKey());
			isSelected = selectedRoles != null && !selectedRoles.isEmpty();
			
		} else
			selectedRoles = null;
		
		if(isSelected)
			cb.setChecked(true, true);
		
		SelectPanel rolesMenu = new SelectPanel(rolesToStartCaseParam);
		rolesMenu.setSize(10);
		rolesMenu.setMultiple(true);
		
		if(roles != null) {
			
			for (ICRole role : roles) {
		
				SelectOption option = new SelectOption(role.getRoleKey(), role.getRoleKey());
				
				if(selectedRoles != null && selectedRoles.contains(role.getRoleKey()))
					option.setSelected(true);
				
				rolesMenu.addOption(option);
			}
		}
		
		Layer rolesSpan = new Layer(Layer.SPAN);
		
		if(!isSelected)
			rolesSpan.setStyleAttribute("display: none");
		
		label = new Label("Select roles", rolesMenu);
		
		rolesSpan.add(label);
		rolesSpan.add(rolesMenu);
		rolesContainer.add(rolesSpan);
		
		
		IWResourceBundle iwrb = getResourceBundle(iwc);
		IWBundle bundle = getBundle(iwc);
		
		//iwrb.getBundle("").
		//iwrb.get
		
		String act = "LazyLoader.loadMultiple(['"+w2b.getBundleURIToJQueryLib()+"', '"+bundle.getVirtualPathWithFileNameString("javascript/ApplicationTypeBPMHandler.js")+"'], function() {AppTypeBPM.processRolesCheckbox('"+cb.getId()+"', '"+rolesSpan.getId()+"', '"+rolesMenu.getId()+"')});";
			
		cb.setOnClick(act);
		
		add(container);
		
		//PresentationUtil.addJavaScriptActionToBody(iwc, "");
	}

	public void setApplication(Application application) {
		this.application = application;
	}
	
	@Override
	public String getBundleIdentifier() {
		return IWBundleStarter.IW_BUNDLE_IDENTIFIER;
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

	public BPMFactory getBpmFactory() {
		
		if(bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public void setApplicationTypeBPM(ApplicationTypeBPM applicationTypeBPM) {
		this.applicationTypeBPM = applicationTypeBPM;
	}

	public ApplicationTypeBPM getApplicationTypeBPM() {
		
		if(applicationTypeBPM == null)
			ELUtil.getInstance().autowire(this);
		
		return applicationTypeBPM;
	}

	public Web2Business getWeb2business() {
		
		if(web2business == null)
			ELUtil.getInstance().autowire(this);
		
		return web2business;
	}

	public void setWeb2business(Web2Business web2business) {
		this.web2business = web2business;
	}
}
