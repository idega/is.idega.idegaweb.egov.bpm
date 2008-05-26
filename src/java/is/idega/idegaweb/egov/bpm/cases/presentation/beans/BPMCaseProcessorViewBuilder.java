package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;
import is.idega.idegaweb.egov.cases.presentation.beans.GeneralCaseProcessorViewBuilder;

import java.rmi.RemoteException;

import javax.faces.component.UIComponent;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.SpringBeanLookup;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWBundle;
import com.idega.presentation.IWContext;

@Scope("request")
@Service(GeneralCaseProcessorViewBuilder.SPRING_BEAN_IDENTIFIER)
public class BPMCaseProcessorViewBuilder implements GeneralCaseProcessorViewBuilder {

	public UIComponent getCaseProcessorView(IWContext iwc) throws RemoteException {
		String recourceType = iwc.getParameter("bpm_reosurce_type");
		if (recourceType == null) {
			throw new NullPointerException("Unknown BPM resource type!");
		}
		String caseId = iwc.getParameter(CasesProcessor.PARAMETER_CASE_PK);
		if (caseId == null) {
			throw new RemoteException("Unknown case ID!");
		}
		Integer caseID = null;
		try {
			caseID = Integer.valueOf(caseId);
		} catch(NumberFormatException e) {
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
		
		return getCaseAssetView(iwc);
	}
	
	@SuppressWarnings("unchecked")
	public HtmlTag getCaseAssetView(IWContext iwc) {
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		HtmlTag div = (HtmlTag)iwc.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue("div");
		
		FaceletComponent facelet = (FaceletComponent)iwc.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(bundle.getFaceletURI("UICasesBPMAssetView.xhtml"));
		div.getChildren().add(facelet);
		div.setValueBinding("rendered", iwc.getApplication().createValueBinding("#{casesBPMAssetsState.assetViewRendered}"));
		
		return div;
	}

}
