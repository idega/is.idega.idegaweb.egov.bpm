package com.idega.idegaweb.egov.bpm.view;


import java.util.ArrayList;
import java.util.Collection;

import javax.faces.context.FacesContext;

import org.jboss.jbpm.IWBundleStarter;

import com.idega.core.accesscontrol.business.StandardRoles;
import com.idega.core.view.ApplicationViewNode;
import com.idega.core.view.DefaultViewNode;
import com.idega.core.view.ViewManager;
import com.idega.core.view.ViewNode;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.repository.data.Singleton;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/10 18:10:25 $ by $Author: civilis $
 */
public class BPMViewManager implements Singleton  {

	private static final String VIEW_MANAGER_KEY = "iw_bpmviewmanager";
	private static final String VIEW_MANAGER_ID = "BPM";

	private ViewNode rootNode;
	private IWMainApplication iwma;

	private BPMViewManager(IWMainApplication iwma){
		this.iwma = iwma;
	}

	public static synchronized BPMViewManager getInstance(IWMainApplication iwma) {
		BPMViewManager vm = (BPMViewManager)iwma.getAttribute(VIEW_MANAGER_KEY);

		if (vm == null) {
			vm = new BPMViewManager(iwma);
			iwma.setAttribute(VIEW_MANAGER_KEY, vm);
	    }
	    return vm;
	}

	public static BPMViewManager getInstance(FacesContext context) {
		return getInstance(IWMainApplication.getIWMainApplication(context));
	}

	public ViewManager getViewManager() {
		return ViewManager.getInstance(iwma);
	}

	public ViewNode getContentNode() {
		IWBundle iwb = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);

		if (rootNode == null)
			rootNode = initalizeContentNode(iwb);

		return rootNode;
	}

	public ViewNode initalizeContentNode(IWBundle contentBundle) {
		ViewNode root = getViewManager().getWorkspaceRoot();
		DefaultViewNode node = new ApplicationViewNode(VIEW_MANAGER_ID, root);
		Collection<String> roles = new ArrayList<String>();
		roles.add(StandardRoles.ROLE_KEY_BUILDER);
		node.setAuthorizedRoles(roles);

		node.setFaceletUri(contentBundle.getFaceletURI("BPMAdmin.xhtml"));
		rootNode = node;
		return rootNode;
	}

	public void initializeStandardNodes(IWBundle bundle){
		ViewNode contentNode = initalizeContentNode(bundle);

		DefaultViewNode node = new DefaultViewNode(VIEW_MANAGER_ID, contentNode);
		node.setFaceletUri(bundle.getFaceletURI("BPMAdmin.xhtml"));
		node.setName(VIEW_MANAGER_ID);
		node.setVisibleInMenus(true);
	}

}