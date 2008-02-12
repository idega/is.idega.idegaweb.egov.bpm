package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.CaseOverviewViewer;
import is.idega.idegaweb.egov.bpm.cases.presentation.CasesListViewer;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jdom.Document;

import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseBMPBean;
import com.idega.block.process.presentation.CasesFinder;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;

public class BPMAssetsEngineBean {
	
	private BuilderService service = null;
	//private JbpmProcessBusinessBean jbpmBean = null;
	
	/*public CasesEngineBean(JbpmProcessBusinessBean jbpmBean) {
		this.jbpmBean = jbpmBean;
	}*/
	
	private BuilderService getBuilderService(IWContext iwc) throws NullPointerException {
		if (service == null) {
			try {
				service = BuilderServiceFactory.getBuilderService(iwc);
			} catch (RemoteException e) {
				e.printStackTrace();
				return null;
			}
		}
		return service;
	}
	
	public List<Case> getCases(User user, String caseType) {
		if (user == null || caseType == null) {
			return null;
		}
		
		List<Case> cases = new ArrayList<Case>();
		
//		ProcessDefinition pd = jbpmBean.getProcessDefinition(caseType);
//		if (pd == null) {
//			return null;
//		}
//		List<ProcessInstance> instances = jbpmBean.getProcessInstances(pd);
//		if (instances == null) {
//			return null;
//		}
		
		//	TODO: implement real logic
		Case fake = new CaseBMPBean();
		fake.setCreator(user);
		fake.setCaseNumber(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		fake.setStatus("Processed");
		fake.setCreated(new Timestamp(System.currentTimeMillis()));
		cases.add(fake);
		
		fake = new CaseBMPBean();
		fake.setCreator(user);
		fake.setCaseNumber(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		fake.setStatus("Handled");
		fake.setCreated(new Timestamp(System.currentTimeMillis()));
		cases.add(fake);
		
		fake = new CaseBMPBean();
		fake.setCreator(user);
		fake.setCaseNumber(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		fake.setStatus("Submitted");
		fake.setCreated(new Timestamp(System.currentTimeMillis()));
		cases.add(fake);
		
		fake = new CaseBMPBean();
		fake.setCreator(user);
		fake.setCaseNumber(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		fake.setStatus("Rejected");
		fake.setCreated(new Timestamp(System.currentTimeMillis()));
		cases.add(fake);
		
		fake = new CaseBMPBean();
		fake.setCreator(user);
		fake.setCaseNumber(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		fake.setStatus("-");
		fake.setCreated(new Timestamp(System.currentTimeMillis()));
		cases.add(fake);
		
		return cases;
	}
	
	public List<String> getInfoForCases() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		IWResourceBundle iwrb = null;
		try {
			iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		if (iwrb == null) {
			return null;
		}
		
		List<String> info = new ArrayList<String>();
		
		info.add(iwrb.getLocalizedString("search_window_title", "Search for cases"));					//	0
		info.add(CasesFinder.class.getName());															//	1
		info.add(iwrb.getLocalizedString("enter_search_text", "Enter any text to search for"));			//	2
		info.add(iwrb.getLocalizedString("loading", "Loading..."));										//	3
		
		return info;
	}
	
	public Case getCase(String caseId) {
		if (caseId == null) {
			return null;
		}
		
		//	TODO: implement real logic
		Case fake = new CaseBMPBean();
		fake.setCreator(CoreUtil.getIWContext().getCurrentUser());
		fake.setCaseNumber(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		fake.setStatus("Submitted");
		fake.setCreated(new Timestamp(System.currentTimeMillis()));
		return fake;
	}

	public Document getCaseOverview(String caseId) {
		if (caseId == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		return getBuilderService(iwc).getRenderedComponent(iwc, new CaseOverviewViewer(caseId), false);
	}
	
	public Document getCasesList(String caseType) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		return getBuilderService(iwc).getRenderedComponent(iwc, new CasesListViewer(caseType), true);
	}
	
	public List<AdvancedProperty> getCasesTypes(IWContext iwc, boolean sort) {
		List<AdvancedProperty> types = new ArrayList<AdvancedProperty>();
		
		/*List<ProcessDefinition> definitions = jbpmBean.getProcessList();
		if (definitions == null) {
			return null;
		}
		
		ProcessDefinition pd = null;
		for (int i = 0; i < definitions.size(); i++) {
			pd = definitions.get(i);
			types.add(new AdvancedProperty(String.valueOf(pd.getId()), pd.getName()));
		}*/
		
		//	TODO: implement real logic
		types.add(new AdvancedProperty("1", "Electronic Devices"));
		types.add(new AdvancedProperty("2", "Dangerous Products"));
		
		if (types.size() == 0) {
			return null;
		}
		
		if (sort) {
			Collections.sort(types, new AdvancedPropertyComparator());
		}
		
		return types;
	}
	
}
