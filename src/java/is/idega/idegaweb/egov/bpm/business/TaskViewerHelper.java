package is.idega.idegaweb.egov.bpm.business;

import com.idega.presentation.IWContext;

public interface TaskViewerHelper {

	public String getPageUriForTaskViewer(IWContext iwc);
	
	public String getLinkToTheTaskRedirector(IWContext iwc, String basePage, String caseId, Long processInstanceId, String backPage, String taskName);
	
	public String getLinkToTheTask(IWContext iwc, String caseId, String taskInstanceId, String backPage);
	
	public String getTaskInstanceIdForTask(Long processInstanceId, String taskName);
	
}
