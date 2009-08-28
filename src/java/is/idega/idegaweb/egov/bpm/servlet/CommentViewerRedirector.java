package is.idega.idegaweb.egov.bpm.servlet;

import is.idega.idegaweb.egov.bpm.cases.messages.CaseUserFactory;

import java.io.IOException;
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

import com.idega.block.article.component.CommentsViewer;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.presentation.IWContext;
import com.idega.servlet.filter.BaseFilter;
import com.idega.user.data.User;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

public class CommentViewerRedirector extends BaseFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger(CommentViewerRedirector.class.getName());
	
	public static final String PARAMETER_REDIRECT_TO_COMMENT = "redirectToComment";
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private CaseUserFactory caseUserFactory;
	
	public void destroy() {}
	
	public void init(FilterConfig arg) throws ServletException {}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		IWContext iwc = getIWContext((HttpServletRequest) request, (HttpServletResponse) response);
		
		if (!iwc.isParameterSet(PARAMETER_REDIRECT_TO_COMMENT) || !iwc.isParameterSet(ProcessManagerBind.processInstanceIdParam)) {
			chain.doFilter(request, response);
			return;
		}
		
		User currentUser = iwc.isLoggedOn() ? iwc.getCurrentUser() : null;
		if (currentUser == null) {
			LOGGER.warning("Can not redirect to comments page because user is not loged in!");
			chain.doFilter(request, response);
			return;
		}
		
		Long processInstanceId = null;
		try {
			processInstanceId = Long.valueOf(iwc.getParameter(ProcessManagerBind.processInstanceIdParam));
		} catch (NumberFormatException e) {
			LOGGER.warning("Error coverting to Long: " + iwc.getParameter(ProcessManagerBind.processInstanceIdParam));
			return;
		}
		
		try {
			ProcessInstanceW piw = getProcessInstanceW(processInstanceId);
			String url = getCaseUserFactory().getCaseUser(currentUser, piw).getUrlToTheCase();
			URIUtil uriUtil = new URIUtil(url);
			uriUtil.setParameter(ProcessManagerBind.processInstanceIdParam, processInstanceId.toString());
			uriUtil.setParameter(CommentsViewer.AUTO_SHOW_COMMENTS, Boolean.TRUE.toString());
			((HttpServletResponse) response).sendRedirect(uriUtil.getUri());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while redirecting to user comments", e);
			chain.doFilter(request, response);
		}
	}

	private ProcessInstanceW getProcessInstanceW(Long processInstanceId) throws Exception {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmFactory.getProcessInstanceW(processInstanceId);
	}

	public CaseUserFactory getCaseUserFactory() {
		if (caseUserFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return caseUserFactory;
	}

	public void setCaseUserFactory(CaseUserFactory caseUserFactory) {
		this.caseUserFactory = caseUserFactory;
	}
	
	
}
