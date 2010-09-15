package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.CasesStatusMapperHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $ Last modified: $Date: 2009/06/23 10:22:22 $ by $Author: valdas $
 */
@Service("casesStatusHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CasesStatusHandler implements ActionHandler {
	
	private static final long serialVersionUID = 7504445907540445936L;
	
	private static final Logger LOGGER = Logger.getLogger(CasesStatusHandler.class.getName());
	
	/**
	 * variable which contains string representation of case status to set
	 */
	private String caseStatus;
	
	private String caseStatusMappedName;
	
	/**
	 * if set, then it's checked, if the current status matches, and only then the status is changed
	 * (for instance, if status specified by ifCaseStatusExp is received, then it may change to in
	 * progress)
	 */
	private String ifCaseStatus;
	
	private String ifCaseStatusMappedName;
	
	/**
	 * performer, if not set, current user is used
	 */
	private Integer performerUserId;
	
	/**
	 * if set - is used. if no caseId or processInstanceId set explicitly, the mainProcessInstanceId
	 * is used
	 */
	private Integer caseId;
	
	/**
	 * used if set and caseId not set. if no caseId or processInstanceId set explicitly, the
	 * mainProcessInstanceId is used
	 */
	private Long processInstanceId;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private CasesStatusMapperHandler casesStatusMapperHandler;
	
	public void execute(ExecutionContext ectx) throws Exception {
		Integer caseId = null;
		try {
			caseId = getCaseId(ectx);
			if (caseId == null) {
				LOGGER.log(Level.WARNING, "No caseId resolved, skipping case status change");
				return;
			}
			
			final String status = getCaseStatus();
			Integer performerUserId = getPerformerUserId();
			final String ifCaseStatus = getIfCaseStatus();
			
			IWContext iwc = CoreUtil.getIWContext();
			IWApplicationContext iwac = getIWAC(iwc);
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			final Case theCase = casesBusiness.getCase(caseId);
			
			if (ifCaseStatus == null || ifCaseStatus.equals(theCase.getCaseStatus().getStatus())) {
				// only changing if ifCaseStatus equals current case status, or ifCaseStatus not set (i.e. change always)
				final User performer;
				if (performerUserId == null) {
					if (iwc != null) {
						if (iwc.isLoggedOn())
							performer = iwc.getCurrentUser();
						else
							performer = null;
					} else {
						LOGGER.warning("Cannot resolve current IWContext, so cannot resolve current user. Using no performer");
						performer = null;
					}
				} else {
					performer = getUserBusiness(iwac).getUser(performerUserId);
				}
				
				casesBusiness.changeCaseStatusDoNotSendUpdates(theCase, status, performer);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while changing case status for the case: " + caseId, e);
		}
	}
	
	public String getCaseStatus() {
		if (caseStatus == null) {
			if (!StringUtil.isEmpty(getCaseStatusMappedName())) {
				caseStatus = getCasesStatusMapperHandler().getStatusCodeByMappedName(getCaseStatusMappedName());
			}
		}
		
		return caseStatus;
	}
	
	public String getIfCaseStatus() {
		if (ifCaseStatus == null) {
			if (!StringUtil.isEmpty(getIfCaseStatusMappedName())) {
				ifCaseStatus = getCasesStatusMapperHandler().getStatusCodeByMappedName(getIfCaseStatusMappedName());
			}
		}
		
		return ifCaseStatus;
	}
	
	private Integer getCaseId(ExecutionContext ectx) {
		if (caseId == null) {
			Long processInstanceIdToUse;
			if (getProcessInstanceId() != null) {
				processInstanceIdToUse = getProcessInstanceId();
			} else {
				Long currentProcessInstanceId = ectx.getProcessInstance().getId();
				processInstanceIdToUse = getBpmFactory().getMainProcessInstance(currentProcessInstanceId).getId();
			}
			
			CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByProcessInstanceId(processInstanceIdToUse);
			if (cpi == null) {
				LOGGER.warning("No case process instance bind found for process instance id=" + processInstanceIdToUse);
			}
			
			caseId = cpi.getCaseId();
		}
		
		return caseId;
	}
	
	private IWApplicationContext getIWAC(final IWContext iwc) {
		return iwc == null ? IWMainApplication.getDefaultIWApplicationContext() : iwc;
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}
	
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
	
	public void setCaseStatus(String caseStatus) {
		this.caseStatus = caseStatus;
	}
	
	public void setIfCaseStatus(String ifCaseStatus) {
		this.ifCaseStatus = ifCaseStatus;
	}
	
	public Integer getPerformerUserId() {
		return performerUserId;
	}
	
	public void setPerformerUserId(Integer performerUserId) {
		this.performerUserId = performerUserId;
	}
	
	public Integer getCaseId() {
		return caseId;
	}
	
	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}
	
	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public String getCaseStatusMappedName() {
		return caseStatusMappedName;
	}
	
	public void setCaseStatusMappedName(String caseStatusMappedName) {
		this.caseStatusMappedName = caseStatusMappedName;
	}
	
	public String getIfCaseStatusMappedName() {
		return ifCaseStatusMappedName;
	}
	
	public void setIfCaseStatusMappedName(String ifCaseStatusMappedName) {
		this.ifCaseStatusMappedName = ifCaseStatusMappedName;
	}
	
	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	CasesStatusMapperHandler getCasesStatusMapperHandler() {
		return casesStatusMapperHandler;
	}
}