package is.idega.idegaweb.egov.bpm.cases.attachment;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.file.CaseAttachmentStatisticsProvider;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.user.data.User;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMCaseAttachmentStatisticsProvider extends CaseAttachmentStatisticsProvider {

	private static final long serialVersionUID = 819463146044694606L;

	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Override
	public Collection<User> getPotentialDownloaders(String fileHolderIdentifier) {
		if (StringUtil.isEmpty(fileHolderIdentifier)) {
			return null;
		}
		
		Long processInstanceId = null;
		try {
			processInstanceId = Long.valueOf(fileHolderIdentifier);
		} catch(NumberFormatException e) {}
		if (processInstanceId == null) {
			return null;
		}
		
		CaseProcInstBind bind = null;
		try {
			bind = getCasesBPMDAO().getCaseProcInstBindByProcessInstanceId(processInstanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return bind == null ? null : super.getPotentialDownloaders(bind.getCaseId().toString());
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}
