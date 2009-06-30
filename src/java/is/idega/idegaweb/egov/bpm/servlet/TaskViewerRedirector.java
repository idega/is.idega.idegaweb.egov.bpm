package is.idega.idegaweb.egov.bpm.servlet;

import is.idega.idegaweb.egov.bpm.business.TaskViewerHelper;
import is.idega.idegaweb.egov.bpm.business.TaskViewerHelperImp;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.presentation.IWContext;
import com.idega.servlet.filter.BaseFilter;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class TaskViewerRedirector extends BaseFilter implements Filter {
	
	private static final Logger LOGGER = Logger.getLogger(TaskViewerRedirector.class.getName());
	
	@Autowired
	private TaskViewerHelper taskViewer;
	
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	public void destroy() {
	}

	public void doFilter(ServletRequest srequest, ServletResponse sresponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) srequest;
		HttpServletResponse response = (HttpServletResponse) sresponse;
		
		if (!canRedirect(request)) {
			chain.doFilter(srequest, sresponse);
			return;
		}
		
		String newUrl = getNewRedirectURL(request, response);
		if (StringUtil.isEmpty(newUrl)) {
			LOGGER.warning("Couldn't create uri to redirect to task viewer");
			chain.doFilter(srequest, sresponse);
			return;
		}
		response.sendRedirect(newUrl);
	}

	private boolean canRedirect(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map map = request.getParameterMap();
		return map.containsKey(TaskViewerHelperImp.TASK_VIEWER_PAGE_REQUESTED_PARAMETER) &&
				map.containsKey(TaskViewerHelperImp.PROCESS_INSTANCE_ID_PARAMETER) && map.containsKey(TaskViewerHelperImp.TASK_NAME_PARAMETER);
	}
	
	private String getTaskInstanceId(String processInstanceId, String taskName) {
		if (StringUtil.isEmpty(processInstanceId)) {
			return null;
		}
		
		Long piId = null;
		try {
			piId = Long.valueOf(processInstanceId);
		} catch(NumberFormatException e) {
			e.printStackTrace();
		}
		
		return getTaskViewer().getTaskInstanceIdForTask(piId, taskName);
	}
	
	private String getNewRedirectURL(HttpServletRequest request, HttpServletResponse response) {
		IWContext iwc = getIWContext(request, response);
		
		String taskInstanceId = getTaskInstanceId(iwc.getParameter(TaskViewerHelperImp.PROCESS_INSTANCE_ID_PARAMETER),
				iwc.getParameter(TaskViewerHelperImp.TASK_NAME_PARAMETER));
		if (StringUtil.isEmpty(taskInstanceId)) {
			return null;
		}
		
		String caseId = iwc.getParameter(TaskViewerHelperImp.CASE_ID_PARAMETER);
		if (StringUtil.isEmpty(caseId)) {
			return null;
		}
		String backPage = iwc.getParameter(TaskViewerHelperImp.BACK_PAGE_PARAMETER);
		
		return getTaskViewer().getLinkToTheTask(iwc, caseId, taskInstanceId, StringUtil.isEmpty(backPage) ? null : backPage);
	}

	public TaskViewerHelper getTaskViewer() {
		if (taskViewer == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting instance for: " + TaskViewerHelper.class, e);
			}
		}
		return taskViewer;
	}

	public void setTaskViewer(TaskViewerHelper taskViewer) {
		this.taskViewer = taskViewer;
	}

}
