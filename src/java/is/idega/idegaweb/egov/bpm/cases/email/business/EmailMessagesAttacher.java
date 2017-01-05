package is.idega.idegaweb.egov.bpm.cases.email.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.data.dao.IXFormViewFactory;
import com.idega.jbpm.data.impl.DefaultBPMProcessInstanceW;
import com.idega.jbpm.exe.BPMFactory;

/**
 * Event's handler calls another thread {@link EmailMessagesAttacherWorker} to parse and attach email(s)
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:56:21 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class EmailMessagesAttacher implements ApplicationListener<ApplicationEmailEvent> {

	private CasesBPMDAO casesBPMDAO;
	private BPMContext idegaJbpmContext;
	private BPMFactory bpmFactory;
	private IXFormViewFactory xfvFact;
	private TmpFilesManager fileUploadManager;
	private TmpFileResolver uploadedResourceResolver;

	protected static final String email_fetch_process_name = DefaultBPMProcessInstanceW.email_fetch_process_name;

	@Override
	public void onApplicationEvent(ApplicationEmailEvent ae) {
		Thread worker = new Thread(new EmailMessagesAttacherWorker(ae));
		worker.start();
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public IXFormViewFactory getXfvFact() {
		return xfvFact;
	}

	@Autowired
	public void setXfvFact(IXFormViewFactory xfvFact) {
		this.xfvFact = xfvFact;
	}

	public TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	@Autowired
	public void setFileUploadManager(TmpFilesManager fileUploadManager) {
		this.fileUploadManager = fileUploadManager;
	}

	public TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	@Autowired
	public void setUploadedResourceResolver(
	        @TmpFileResolverType("defaultResolver") TmpFileResolver uploadedResourceResolver) {
		this.uploadedResourceResolver = uploadedResourceResolver;
	}

}