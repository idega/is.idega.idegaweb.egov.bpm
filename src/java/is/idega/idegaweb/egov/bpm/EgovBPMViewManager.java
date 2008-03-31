package is.idega.idegaweb.egov.bpm;

import java.util.ArrayList;
import java.util.Collection;

import javax.faces.context.FacesContext;

import com.idega.core.accesscontrol.business.StandardRoles;
import com.idega.core.view.ApplicationViewNode;
import com.idega.core.view.DefaultViewNode;
import com.idega.core.view.ViewManager;
import com.idega.core.view.ViewNode;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.repository.data.Singleton;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/31 15:40:45 $ by $Author: civilis $
 *
 */
public class EgovBPMViewManager implements Singleton  {

	private static final String VIEW_MANAGER_KEY = "iw_egovbpmviewmanager";
	private static final String BPM_ID = "bpm";
	private static final String BPM_IDENTITY_ID = "bpm_identity";
	private static final String BPM_PROCESS_ID = "bpm_process";
	
	private ViewNode rootNode;
	private IWMainApplication iwma;
	
	private EgovBPMViewManager(IWMainApplication iwma){
		
		this.iwma = iwma;
	}

	public static synchronized EgovBPMViewManager getInstance(IWMainApplication iwma) {
		EgovBPMViewManager viewManager = (EgovBPMViewManager)iwma.getAttribute(VIEW_MANAGER_KEY);
		
		if(viewManager == null) {
			viewManager = new EgovBPMViewManager(iwma);
			iwma.setAttribute(VIEW_MANAGER_KEY, viewManager);
	    }
	    return viewManager;
	}	
	
	public static EgovBPMViewManager getInstance(FacesContext context) {
		return getInstance(IWMainApplication.getIWMainApplication(context));
	}
	
	public ViewManager getViewManager() {
		return ViewManager.getInstance(iwma);
	}
	
	
	public ViewNode getContentNode() {
		IWBundle iwb = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		if(rootNode == null)
			rootNode = initalizeContentNode(iwb);
		
		return rootNode;
	}
	
	public ViewNode initalizeContentNode(IWBundle bundle) {
		
		ViewNode root = getViewManager().getWorkspaceRoot();
		DefaultViewNode node = new ApplicationViewNode(BPM_ID, root);
		node.setName("BPM");
		Collection<String> roles = new ArrayList<String>();
		roles.add(StandardRoles.ROLE_KEY_BUILDER);
		node.setAuthorizedRoles(roles);
		
		node.setFaceletUri(bundle.getFaceletURI("UIEgovBPM.xhtml"));
		rootNode = node;
		return rootNode;
	}
	
	public void initializeStandardNodes(IWBundle bundle){
		ViewNode contentNode = initalizeContentNode(bundle);
		
		DefaultViewNode node = new DefaultViewNode(BPM_IDENTITY_ID, contentNode);
		node.setFaceletUri(bundle.getFaceletURI("UIEgovBPMIdentityMgmt.xhtml"));
		node.setName("Assignments management");
		node.setVisibleInMenus(true);
		
		node = new DefaultViewNode(BPM_PROCESS_ID, contentNode);
		node.setFaceletUri(bundle.getFaceletURI("UIEgovBPMProcessMgmt.xhtml"));
		node.setName("Process management");
		node.setVisibleInMenus(true);
	}
}