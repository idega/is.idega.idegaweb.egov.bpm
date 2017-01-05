package is.idega.idegaweb.egov.bpm.business;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import com.idega.block.process.data.Case;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.presentation.IWContext;

public interface TaskViewerHelper {

	public String getPageUriForTaskViewer(IWContext iwc);
	
	public String getLinkToTheTaskRedirector(IWContext iwc, String basePage, String caseId, Long processInstanceId, String backPage, String taskName);
	
	public String getLinkToTheTask(IWContext iwc, String caseId, String taskInstanceId, String backPage);
	
	public String getTaskInstanceIdForTask(Long processInstanceId, String taskName);

	/**
	 * 
	 * <p>Creates links to views of {@link Case}s.</p>
	 * @param iwc is current FacesContext, not <code>null</code>;
	 * @param relations is {@link Map} of {@link Case#getPrimaryKey()} and 
	 * {@link ProcessInstance}, not <code>null</code>;
	 * @param backPage - <code>true</code> if it is required to get back to
	 * to page the link was called;
	 * @param taskName is {@link Task#getName()}, which should be shown. 
	 * Not <code>null</code>;
	 * @return {@link Map} of {@link Case#getPrimaryKey()} and {@link URL} 
	 * to preview or {@link Collections#emptyList()} on failure;
	 */
	public Map<Long, String> getLinksToTheTaskRedirector(IWContext iwc,
			Map<Long, ProcessInstanceW> relations, boolean backPage,
			String taskName);
}
