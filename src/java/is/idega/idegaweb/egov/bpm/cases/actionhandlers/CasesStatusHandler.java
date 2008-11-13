package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/11/13 15:08:42 $ by $Author: juozas $
 */
@Service("casesStatusHandler")
@Scope("prototype")
public class CasesStatusHandler implements ActionHandler {

	private static final long serialVersionUID = 7504445907540445936L;
	
	/**
	 * variable which contains string representation of case status to set
	 */
	private String caseStatusExp;
	
	/**
	 * if set, then it's checked, if the current status matches, and only then the status is changed (for instance, if status specified by ifCaseStatusExp is received, then it may change to in progress) 
	 */
	private String ifCaseStatusExp;
	/**
	 * performer, if not set, current user is used
	 */
	private Integer performerUserIdExp;
	/**
	 * caseId, if not set, caseId from econtext process instance id is resolved
	 */
	private Integer caseIdExp;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		try {
			final String status = getCaseStatusExp();//(String)JbpmExpressionEvaluator.evaluate(getCaseStatusExp(), ectx);
			Integer performerUserId = getPerformerUserIdExp();// getPerformerUserIdExp() != null ? (Integer)JbpmExpressionEvaluator.evaluate(getPerformerUserIdExp(), ectx) : null;
			Integer caseId = getCaseIdExp();//getCaseIdExp() != null ? (Integer)JbpmExpressionEvaluator.evaluate(getCaseIdExp(), ectx) : null;
			final String ifCaseStatus = getIfCaseStatusExp();//getIfCaseStatusExp() != null ? (String)JbpmExpressionEvaluator.evaluate(getIfCaseStatusExp(), ectx) : null;
			
			if(caseId == null) {
				
				ELUtil.getInstance().autowire(this);
				
				CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class, ectx.getProcessInstance().getId());
				
				if(cpi == null) {
					
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "No case process instance bind found for process instance id="+ectx.getProcessInstance().getId()+", skipping case status change");
					return;
				}
				
				caseId = cpi.getCaseId();
			}
			
			IWContext iwc = IWContext.getCurrentInstance();
			IWApplicationContext iwac = getIWAC(iwc);
			final Case theCase = getCasesBusiness(iwac).getCase(caseId);
			
			if(ifCaseStatus == null || ifCaseStatus.equals(theCase.getCaseStatus().getStatus())) {
//				only changing if ifCaseStatus equals current case status, or ifCaseStatus not set (i.e. change always)
			
				final User performer;
				
				if(performerUserId == null) {
				
					if(iwc != null)
						performer = iwc.getCurrentUser();
					else {
						
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot resolve current IWContext, so cannot resolve current user. Using no performer");
						performer = null;
					}
					
				} else {
				
					performer = getUserBusiness(iwac).getUser(performerUserId);
				}
				
				getCasesBusiness(iwc).changeCaseStatusDoNotSendUpdates(theCase, status, performer);
			}
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while changing case status", e);
		}
	}
	
	private IWApplicationContext getIWAC(final IWContext iwc) {
		
		final IWApplicationContext iwac;
		
		if(iwc != null) {
			iwac = iwc;
		} else {
			
			iwac = IWMainApplication.getDefaultIWApplicationContext();
		}
		
		return iwac;
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getCaseStatusExp() {
		return caseStatusExp;
	}

	public void setCaseStatusExp(String caseStatusExp) {
		this.caseStatusExp = caseStatusExp;
	}

	public Integer getPerformerUserIdExp() {
		return performerUserIdExp;
	}

	public void setPerformerUserIdExp(Integer performerUserIdExp) {
		this.performerUserIdExp = performerUserIdExp;
	}

	public Integer getCaseIdExp() {
		return caseIdExp;
	}

	public void setCaseIdExp(Integer caseIdExp) {
		this.caseIdExp = caseIdExp;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public String getIfCaseStatusExp() {
		return ifCaseStatusExp;
	}

	public void setIfCaseStatusExp(String ifCaseStatusExp) {
		this.ifCaseStatusExp = ifCaseStatusExp;
	}
}