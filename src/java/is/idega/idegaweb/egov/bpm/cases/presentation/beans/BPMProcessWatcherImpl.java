package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind.Status;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.artifacts.presentation.bean.BPMProcessWatcher;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;

@Scope("singleton")
@Service(BPMProcessWatcher.SPRING_BEAN_IDENTIFIER)
public class BPMProcessWatcherImpl implements BPMProcessWatcher {
	
	private CasesBPMProcessView processView = null;

	public boolean isWatching(Long processInstanceId) {
		try {
			CasesBPMDAO dao = getProcessView().getCasesBPMDAO();
			User performer = CoreUtil.getIWContext().getCurrentUser();
			
			ProcessUserBind caseUser = dao.getProcessUserBind(processInstanceId, Integer.valueOf(performer.getPrimaryKey().toString()), true);
			return Status.PROCESS_WATCHED == caseUser.getStatus();
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while checking if process is being watched", e);
		}
		
		return false;
	}

	public boolean removeWatch(Long processInstanceId) {
		try {
			CasesBPMDAO dao = getProcessView().getCasesBPMDAO();
			User performer = CoreUtil.getIWContext().getCurrentUser();
			
			ProcessUserBind caseUser = dao.getProcessUserBind(processInstanceId, Integer.valueOf(performer.getPrimaryKey().toString()), true);
			
			if(caseUser.getStatus() != null && caseUser.getStatus() == Status.PROCESS_WATCHED)
				caseUser.setStatus(Status.NO_STATUS);
			
			dao.merge(caseUser);

			return true;
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while trying remove watch for process: " + processInstanceId, e);
		}
		
		return false;
	}

	public boolean takeWatch(Long processInstanceId) {
		try {
			CasesBPMDAO dao = getProcessView().getCasesBPMDAO();
			User performer = CoreUtil.getIWContext().getCurrentUser();
			
			ProcessUserBind caseUser = dao.getProcessUserBind(processInstanceId, Integer.valueOf(performer.getPrimaryKey().toString()), true);
			
			caseUser.setStatus(Status.PROCESS_WATCHED);
			
			dao.merge(caseUser);
	
			return true;
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while trying take watch for process: " + processInstanceId, e);
		}
		
		return false;
	}

	public CasesBPMProcessView getProcessView() {
		return processView;
	}

	@Autowired
	public void setProcessView(CasesBPMProcessView processView) {
		this.processView = processView;
	}

	public String getWatchCaseStatusMessage(boolean isWatched) {
		IWResourceBundle iwrb = getResourceBundle();
		if (iwrb == null) {
			return null;
		}
		
		if (isWatched) {
			return iwrb.getLocalizedString("cases_bpm.process_is_being_watched", "Current case was added to your watch list");
		}
		
		return iwrb.getLocalizedString("cases_bpm.process_was_removed_from_watch_list", "Current case was removed from your watch list");
	}

	private IWResourceBundle getResourceBundle() {
		IWContext iwc = CoreUtil.getIWContext();
		try {
			return iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		} catch(Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception getting IWResourceBundle", e);
		}
		
		return null;
	}
	
	public String getWatchCaseStatusLabel(boolean isWatched)  {
		IWResourceBundle iwrb = getResourceBundle();
		if (iwrb == null) {
			return null;
		}
		
		if (isWatched) {
			return iwrb.getLocalizedString("cases_bpm.remove_case", "Remove case from watchlist");
		}
		
		return iwrb.getLocalizedString("cases_bpm.watch_case", "Watch case");
	}
}
