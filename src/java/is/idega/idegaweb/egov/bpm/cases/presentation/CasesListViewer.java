package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMAssetsEngineBean;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesPresentationHelper;

import java.util.List;

import javax.faces.context.FacesContext;

import com.idega.block.process.business.CasesListColumn;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.business.SpringBeanLookup;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Page;
import com.idega.presentation.PresentationObjectUtil;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;

public class CasesListViewer extends IWBaseComponent {
	
	private String caseType = null;
	
	private BPMAssetsEngineBean casesEngine = null;
	
	private CasesPresentationHelper helper = null;
	
	private User currentUser = null;
	
	private List<Case> cases = null;
	
	private IWResourceBundle iwrb = null;
	
	public CasesListViewer() {}
	
	public CasesListViewer(String caseType) {
		this();
		this.caseType = caseType;
	}
	
	protected void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		Layer container = new Layer();
		add(container);
		
		if (!initialize(iwc)) {
			Page page = PresentationObjectUtil.getParentPage(this);
			if (page != null) {
				iwc.forwardToURL(page, "/login");
			}
			return;
		}
		
		if (caseType == null || CoreConstants.EMPTY.equals(caseType)) {
			Object activeProcessDefinitionId = iwc.getApplicationAttribute(ProcessConstants.ACTIVE_PROCESS_DEFINITION);
			caseType = activeProcessDefinitionId == null ? null : activeProcessDefinitionId.toString();
		}
		cases = casesEngine.getCases(currentUser, caseType);
		addMyCasesList(container, iwc);
	}
	
	protected void updateComponent(FacesContext context) {
		this.initializeComponent(context);
	}
	
	private IWResourceBundle getResourceBundle(IWContext iwc) {
		if (iwrb == null) {
			iwrb = getIWResourceBundle(iwc, ProcessConstants.IW_BUNDLE_IDENTIFIER);
		}
		return iwrb;
	}
	
	private void addMyCasesList(Layer container, IWContext iwc) {
		/*WFTabbedPane tabbedPane = new WFTabbedPane();
		tabbedPane.addTab("tab1", "My Cases", helper.getCasesListViewer(cases, iwc, getResourceBundle(iwc)));
		tabbedPane.addTab("tab2", "Proceeded", new Text("proceeded cases here"));
		tabbedPane.addTab("tab3", "Rejected", new Text("rejected cases here"));
		tabbedPane.setSelectedMenuItemId("tab1");
		container.add(tabbedPane);*/
		
		List<CasesListColumn> columns = ProcessConstants.getCasesListMainColumns(getResourceBundle(iwc));
		iwc.setSessionAttribute(ProcessConstants.getKeyForCasesColumnsAttribute(iwc), columns);
		container.add(helper.getCasesListViewer(cases, iwc, getResourceBundle(iwc), columns));
	}
	
	private boolean initialize(IWContext iwc) {
		if (iwc == null) {
			return false;
		}
		
		casesEngine = (BPMAssetsEngineBean)getBeanInstance("casesBPMAssets");
		if (casesEngine == null) {
			return false;
		}
		helper = SpringBeanLookup.getInstance().getSpringBean(iwc, CasesPresentationHelper.class);
		if (helper == null) {
			return false;
		}
		currentUser = iwc.getCurrentUser();
		if (currentUser == null) {
			return false;
		}
		
		return true;
	}
	
	public Object saveState(FacesContext ctx) {
		Object values[] = new Object[2];
		values[0] = super.saveState(ctx);
		values[1] = caseType;
		return values;
	}

	public void restoreState(FacesContext ctx, Object state) {
		Object values[] = (Object[]) state;
		super.restoreState(ctx, values[0]);
		if (values[1] instanceof String) {
			caseType = (String) values[1];
		}
	}
}