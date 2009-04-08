package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import java.rmi.RemoteException;

import javax.faces.component.UIComponent;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.presentation.beans.GeneralCaseManagerViewBuilder;
import com.idega.block.process.presentation.beans.GeneralCasesListBuilder;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.Heading3;
import com.idega.webface.WFUtil;

@Scope("request")
@Service(GeneralCaseManagerViewBuilder.SPRING_BEAN_IDENTIFIER)
public class CaseBPMManagerViewBuilder implements GeneralCaseManagerViewBuilder {

	public UIComponent getCaseManagerView(IWContext iwc, String type) throws RemoteException {
		Integer caseId = null;
		try {
			caseId = Integer.valueOf(iwc.getParameter(CasesProcessor.PARAMETER_CASE_PK));
		} catch(NumberFormatException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		if (caseId == null) {
			throw new RemoteException("Unknown case ID!");
		}
		Long taskInstanceId = null;
		try {
			taskInstanceId = Long.valueOf(iwc.getParameter(CasesBPMAssetsState.TASK_INSTANCE_ID_PARAMETER));
		} catch(NumberFormatException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		if (taskInstanceId == null) {
			throw new RemoteException("Unknown ID for task instance");
		}

		GeneralCasesListBuilder listBuilder = WFUtil.getBeanInstance(iwc, GeneralCasesListBuilder.SPRING_BEAN_IDENTIFIER);
		UIComponent view = listBuilder.getCaseManagerView(iwc, caseId, type);
		
		if (view == null) {
			IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
			view = new Heading3(iwrb.getLocalizedString("cases_list.can_not_get_case_view", "Sorry, error occurred - can not generate case manager view."));
		}
		
		return view;
	}

}