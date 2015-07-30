package com.idega.idegaweb.egov.bpm.presentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.faces.application.Application;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.myfaces.context.servlet.ServletExternalContextImpl;
import org.apache.myfaces.el.unified.FacesELContext;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HtmlResponseWriterImpl;
import org.chiba.web.WebFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.idega.chiba.web.session.impl.IdegaXFormHttpSession;
import com.idega.core.builder.data.ICDomain;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.BPMUserImpl;
import com.idega.presentation.IWContext;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.RequestUtil;

public class IWContextMockUp extends IWContext {

	private static final long serialVersionUID = -4508560923047512886L;

	private Map<String, Object> contextAttributes = new HashMap<String, Object>();

	private HttpServletRequest request;
	private HttpServletResponse response;

	private HttpSession session;

	private ResponseWriter writer;

	private UIViewRoot viewRoot;

	private ExternalContext externalContext;

	private ELContext elContext;

	public IWContextMockUp() {
		setCurrentInstance(this);
	}

	@Override
	public ELContext getELContext() {
        if (elContext != null)
        	return elContext;

        elContext = new FacesELContext(getApplication().getELResolver(), this);

        ELContextEvent event = new ELContextEvent(elContext);
        for (ELContextListener listener : getApplication().getELContextListeners())
            listener.contextCreated(event);

        return elContext;
    }

	@Override
	public ExternalContext getExternalContext() {
		if (externalContext == null) {
			IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
			externalContext = new ServletExternalContextImpl(iwma.getServletContext(), getRequest(), getResponse());
		}
		return externalContext;
	}

	@Override
	public Locale getCurrentLocale() {
		Locale locale = IWMainApplication.getDefaultIWMainApplication().getDefaultLocale();
		return locale == null ? ICLocaleBusiness.getLocaleFromLocaleString("is_IS") : locale;
	}

	@Override
	public String getUserAgent() {
		Object agent = getRequest().getAttribute(RequestUtil.HEADER_USER_AGENT);
		if (agent == null)
			return "Request mock-up";
		return agent.toString();
	}

	@Override
	public void setViewRoot(UIViewRoot viewRoot) {
		this.viewRoot  = viewRoot;
	}

	@Override
	public UIViewRoot getViewRoot() {
		return this.viewRoot;
	}

	@Override
	public ResponseWriter getResponseWriter() {
		if (writer == null) {
			writer = new HtmlResponseWriterImpl(new StringWriter(), MimeTypeUtil.MIME_TYPE_HTML, CoreConstants.ENCODING_UTF8);
		}
		return writer;
	}

	@Override
	public void setResponseWriter(ResponseWriter writer) {
		this.writer = writer;
	}

	@Override
	public void setSessionAttribute(String key, Object value) {
		contextAttributes.put(key, value);
	}

	@Override
	public Object getSessionAttribute(String key) {
		return contextAttributes.get(key);
	}

	@Override
	public void removeSessionAttribute(String key) {
		contextAttributes.remove(key);
	}

	@Override
	public Application getApplication() {
		return IWMainApplication.getDefaultIWMainApplication();
	}

	@Override
	public HttpSession getSession() {
		if (session == null) {
			session = new IdegaXFormHttpSession(
					IWMainApplication.getDefaultIWMainApplication().getServletContext(),
					String.valueOf(new Random().nextInt(Integer.MAX_VALUE)),
					System.currentTimeMillis()
			);
		}
		return session;
	}

	@Override
	public HttpServletResponse getResponse() {
		if (response == null) {
			response = new HttpServletResponse() {

				@Override
				public void setLocale(Locale loc) {


				}

				@Override
				public void setContentType(String type) {


				}

				@Override
				public void setContentLength(int len) {


				}

				@Override
				public void setCharacterEncoding(String charset) {


				}

				@Override
				public void setBufferSize(int size) {


				}

				@Override
				public void resetBuffer() {


				}

				@Override
				public void reset() {


				}

				@Override
				public boolean isCommitted() {

					return false;
				}

				@Override
				public PrintWriter getWriter() throws IOException {

					return null;
				}

				@Override
				public ServletOutputStream getOutputStream() throws IOException {

					return null;
				}

				@Override
				public Locale getLocale() {

					return null;
				}

				@Override
				public String getContentType() {

					return null;
				}

				@Override
				public String getCharacterEncoding() {

					return null;
				}

				@Override
				public int getBufferSize() {

					return 0;
				}

				@Override
				public void flushBuffer() throws IOException {


				}

				@Override
				public void setStatus(int sc, String sm) {


				}

				@Override
				public void setStatus(int sc) {


				}

				@Override
				public void setIntHeader(String name, int value) {


				}

				@Override
				public void setHeader(String name, String value) {


				}

				@Override
				public void setDateHeader(String name, long date) {


				}

				@Override
				public void sendRedirect(String location) throws IOException {


				}

				@Override
				public void sendError(int sc, String msg) throws IOException {


				}

				@Override
				public void sendError(int sc) throws IOException {


				}

				@Override
				public String encodeUrl(String url) {
					try {
						return URLEncoder.encode(url, CoreConstants.ENCODING_UTF8);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					return url;
				}

				@Override
				public String encodeURL(String url) {
					return encodeUrl(url);
				}

				@Override
				public String encodeRedirectUrl(String url) {

					return null;
				}

				@Override
				public String encodeRedirectURL(String url) {

					return null;
				}

				@Override
				public boolean containsHeader(String name) {

					return false;
				}

				@Override
				public void addIntHeader(String name, int value) {


				}

				@Override
				public void addHeader(String name, String value) {


				}

				@Override
				public void addDateHeader(String name, long date) {


				}

				@Override
				public void addCookie(Cookie cookie) {


				}

				@Override
				public void setContentLengthLong(long len) {
					// TODO Auto-generated method stub

				}

				@Override
				public int getStatus() {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public String getHeader(String name) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<String> getHeaders(String name) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<String> getHeaderNames() {
					// TODO Auto-generated method stub
					return null;
				}
			};
		}
		return response;
	}

	@Override
	public HttpServletRequest getRequest() {
		if (request == null) {
			request = new HttpServletRequestMockUp(new HttpServletRequest() {

				@Override
				public void setCharacterEncoding(String env)
						throws UnsupportedEncodingException {


				}

				@Override
				public void setAttribute(String name, Object o) {
				}

				@Override
				public void removeAttribute(String name) {
				}

				@Override
				public boolean isSecure() {

					return false;
				}

				@Override
				public int getServerPort() {

					return 0;
				}

				@Override
				public String getServerName() {

					return null;
				}

				@Override
				public String getScheme() {

					return null;
				}

				@Override
				public RequestDispatcher getRequestDispatcher(String path) {

					return null;
				}

				@Override
				public int getRemotePort() {

					return 0;
				}

				@Override
				public String getRemoteHost() {

					return null;
				}

				@Override
				public String getRemoteAddr() {

					return null;
				}

				@Override
				public String getRealPath(String path) {

					return null;
				}

				@Override
				public BufferedReader getReader() throws IOException {

					return null;
				}

				@Override
				public String getProtocol() {

					return null;
				}

				@Override
				public String[] getParameterValues(String name) {

					return null;
				}

				private List<String> parameterNames = new ArrayList<String>();
				@Override
				public Enumeration<String> getParameterNames() {
					return Collections.enumeration(parameterNames);
				}

				@Override
				public Map getParameterMap() {

					return null;
				}

				@Override
				public String getParameter(String name) {

					return null;
				}

				@Override
				public Enumeration<Locale> getLocales() {

					return null;
				}

				@Override
				public Locale getLocale() {

					return null;
				}

				@Override
				public int getLocalPort() {

					return 0;
				}

				@Override
				public String getLocalName() {

					return null;
				}

				@Override
				public String getLocalAddr() {

					return null;
				}

				@Override
				public ServletInputStream getInputStream() throws IOException {

					return null;
				}

				@Override
				public String getContentType() {

					return null;
				}

				@Override
				public int getContentLength() {

					return 0;
				}

				@Override
				public String getCharacterEncoding() {

					return null;
				}

				@Override
				public Enumeration getAttributeNames() {

					return null;
				}

				@Override
				public Object getAttribute(String name) {
					return null;
				}

				@Override
				public boolean isUserInRole(String role) {

					return false;
				}

				@Override
				public boolean isRequestedSessionIdValid() {

					return false;
				}

				@Override
				public boolean isRequestedSessionIdFromUrl() {

					return false;
				}

				@Override
				public boolean isRequestedSessionIdFromURL() {

					return false;
				}

				@Override
				public boolean isRequestedSessionIdFromCookie() {

					return false;
				}

				@Override
				public Principal getUserPrincipal() {

					return null;
				}

				@Override
				public HttpSession getSession(boolean create) {

					return null;
				}

				@Override
				public HttpSession getSession() {

					return null;
				}

				@Override
				public String getServletPath() {

					return null;
				}

				@Override
				public String getRequestedSessionId() {

					return null;
				}

				@Override
				public StringBuffer getRequestURL() {
					return new StringBuffer(CoreConstants.PAGES_URI_PREFIX);
				}

				@Override
				public String getRequestURI() {

					return null;
				}

				@Override
				public String getRemoteUser() {

					return null;
				}

				@Override
				public String getQueryString() {

					return null;
				}

				@Override
				public String getPathTranslated() {

					return null;
				}

				@Override
				public String getPathInfo() {

					return null;
				}

				@Override
				public String getMethod() {

					return null;
				}

				@Override
				public int getIntHeader(String name) {

					return 0;
				}

				@Override
				public Enumeration getHeaders(String name) {

					return null;
				}

				@Override
				public Enumeration getHeaderNames() {

					return null;
				}

				@Override
				public String getHeader(String name) {

					return null;
				}

				@Override
				public long getDateHeader(String name) {

					return 0;
				}

				@Override
				public Cookie[] getCookies() {

					return null;
				}

				@Override
				public String getContextPath() {

					return null;
				}

				@Override
				public String getAuthType() {

					return null;
				}

				@Override
				public long getContentLengthLong() {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public ServletContext getServletContext() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public AsyncContext startAsync() throws IllegalStateException {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public AsyncContext startAsync(ServletRequest servletRequest,
						ServletResponse servletResponse)
						throws IllegalStateException {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean isAsyncStarted() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean isAsyncSupported() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public AsyncContext getAsyncContext() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public DispatcherType getDispatcherType() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String changeSessionId() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean authenticate(HttpServletResponse response)
						throws IOException, ServletException {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public void login(String username, String password)
						throws ServletException {
					// TODO Auto-generated method stub

				}

				@Override
				public void logout() throws ServletException {
					// TODO Auto-generated method stub

				}

				@Override
				public Collection<Part> getParts() throws IOException,
						ServletException {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Part getPart(String name) throws IOException,
						ServletException {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public <T extends HttpUpgradeHandler> T upgrade(
						Class<T> handlerClass) throws IOException,
						ServletException {
					// TODO Auto-generated method stub
					return null;
				}
			});
			request.setAttribute(WebFactory.SCRIPTED, Boolean.TRUE.toString());
			request.setAttribute(RequestUtil.HEADER_USER_AGENT, "Request mock-up");

			setBPMUser(((HttpServletRequestMockUp) request).requestAttributes);

			RequestContextHolder.setRequestAttributes(new RequestAttributes() {
				@Override
				public void setAttribute(String name, Object value, int scope) {
				}

				@Override
				public void removeAttribute(String name, int scope) {
				}

				@Override
				public void registerDestructionCallback(String name, Runnable callback, int scope) {
				}

				@Override
				public Object getSessionMutex() {
					return getSession();
				}

				@Override
				public String getSessionId() {
					return null;
				}

				@Override
				public String[] getAttributeNames(int scope) {
					return null;
				}

				@Override
				public Object getAttribute(String name, int scope) {
					return null;
				}

				@Override
				public Object resolveReference(String arg0) {
					return null;
				}
			});
		}
		return request;
	}

	private class HttpServletRequestMockUp extends HttpServletRequestWrapper {

		private Map<String, Object> requestAttributes = new HashMap<String, Object>();

		private HttpServletRequestMockUp(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getScheme() {
			ICDomain domain = IWMainApplication.getDefaultIWApplicationContext().getDomain();
			return domain.getServerProtocol();
		}
		@Override
		public String getServerName() {
			ICDomain domain = IWMainApplication.getDefaultIWApplicationContext().getDomain();
			return domain.getServerName();
		}
		@Override
		public int getServerPort() {
			ICDomain domain = IWMainApplication.getDefaultIWApplicationContext().getDomain();
			return domain.getServerPort();
		}
		@Override
		public String getContextPath() {
			return CoreConstants.PAGES_URI_PREFIX;
		}

		@Override
		public void setAttribute(String key, Object value) {
			requestAttributes.put(key, value);
		}

		@Override
		public Object getAttribute(String key) {
			return requestAttributes.get(key);
		}

		@Override
		public void removeAttribute(String key) {
			requestAttributes.remove(key);
		}
	}

	@SuppressWarnings("unchecked")
	private <V extends Object> void setBPMUser(Map<String, V> attributes) {
		try {
			User admin = IWMainApplication.getDefaultIWMainApplication().getAccessController().getAdministratorUser();
			attributes.put(BPMUserImpl.bpmUsrParam, (V) admin.getUniqueId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}