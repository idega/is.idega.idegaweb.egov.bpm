package is.idega.idegaweb.egov.bpm;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.idegaweb.egov.bpm.view.BPMViewManager;

/**
 *
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/05 14:04:10 $ by $Author: civilis $
 *
 */
public class IWBundleStarter implements IWBundleStartable {

	public static final String IW_BUNDLE_IDENTIFIER = BPMConstants.IW_BUNDLE_IDENTIFIER;

	@Override
	public void start(IWBundle starterBundle) {
		BPMViewManager vm = BPMViewManager.getInstance(starterBundle.getApplication());
		vm.initializeStandardNodes(starterBundle);
	}

	@Override
	public void stop(IWBundle starterBundle) {
	}

}