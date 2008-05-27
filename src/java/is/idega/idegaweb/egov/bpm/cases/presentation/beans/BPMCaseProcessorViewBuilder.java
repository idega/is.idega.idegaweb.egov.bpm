package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;
import is.idega.idegaweb.egov.cases.presentation.beans.GeneralCaseProcessorViewBuilder;

import java.rmi.RemoteException;

import javax.faces.component.UIComponent;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.SpringBeanLookup;
import com.idega.presentation.IWContext;

@Scope("request")
@Service(GeneralCaseProcessorViewBuilder.SPRING_BEAN_IDENTIFIER)
public class BPMCaseProcessorViewBuilder implements GeneralCaseProcessorViewBuilder {

	public UIComponent getCaseProcessorView(IWContext iwc) throws RemoteException {
		Integer caseID = null;
		try {
			caseID = Integer.valueOf(iwc.getParameter(CasesProcessor.PARAMETER_CASE_PK));
		} catch(NumberFormatException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		if (caseID == null) {
			throw new RemoteException("Unknown case ID!");
		}
		Long taskInstanceId = null;
		try {
			taskInstanceId = Long.valueOf(iwc.getParameter("taskInstanceId"));
		} catch(NumberFormatException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		if (taskInstanceId == null) {
			throw new RemoteException("Unknown ID for task instance");
		}
		
		CasesBPMAssetsState stateBean = (CasesBPMAssetsState) SpringBeanLookup.getInstance().getSpringBean(iwc.getServletContext(), CasesBPMAssetsState.beanIdentifier);
		stateBean.setCaseId(caseID);
		stateBean.setViewSelected(taskInstanceId);
		stateBean.selectView();
		stateBean.setDisplayPropertyForStyleAttribute(false);
		
		UICasesBPMAssets assets = (UICasesBPMAssets) iwc.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
		return assets;
	}

}