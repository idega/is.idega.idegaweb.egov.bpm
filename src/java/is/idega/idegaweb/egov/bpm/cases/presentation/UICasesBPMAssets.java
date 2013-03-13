package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.business.BPMCommentsPersistenceManager;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesEngineImp;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.article.business.CommentsPersistenceManager;
import com.idega.block.article.component.CommentsViewer;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.presentation.beans.CaseManagerState;
import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.Web2Business;
import com.idega.bpm.pdf.servlet.BPMTaskPDFPrinter;
import com.idega.bpm.pdf.servlet.CaseLogsToPDFWriter;
import com.idega.bpm.pdf.servlet.XFormToPDFWriter;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.DownloadLink;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.webface.WFUtil;

/**
 *
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.56 $
 *
 * Last modified: $Date: 2009/07/14 16:26:58 $ by $Author: valdas $
 *
 */
public class UICasesBPMAssets extends IWBaseComponent {

	public static final String COMPONENT_TYPE = "com.idega.UICasesBPMAssets";

	private static final String assetsFacet = "assets", assetViewFacet = "assetView";

	private boolean fullView = false, inCasesComponent = false, 
			usePdfDownloadColumn = true, allowPDFSigning = true, 
			hideEmptySection = true, showAttachmentStatistics, 
			showOnlyCreatorInContacts, showLogExportButton, showComments = true, 
			showContacts = true, nameFromExternalEntity = false, showUserProfilePicture = Boolean.TRUE;

	private String commentsPersistenceManagerIdentifier, specialBackPage;

	private Long processInstanceId;
	private Integer caseId;

	@Autowired
	private JQuery jQuery;
	@Autowired
	private Web2Business web2;

	@Override
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);

		String caseID = context.getExternalContext().getRequestParameterMap().get(ProcessManagerBind.caseIdParam);
		if (caseID != null && caseID.length() > 0) {
			setCaseId(new Integer(caseID));
		}

		ELUtil.getInstance().autowire(this);

		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		String clientId = null;
		if (CoreUtil.isSingleComponentRenderingProcess(context)) {
			Random numberGenerator = new Random();
			clientId = new StringBuilder(CoreConstants.UNDER).append(numberGenerator.nextInt(Integer.MAX_VALUE)).toString();
		} else
			clientId = context.getViewRoot().createUniqueId();
		div.setId(clientId);
		div.setStyleClass(clientId);
		div.setValue(divTag);

		HtmlTag linksContainer = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		linksContainer.setValue(divTag);
		linksContainer.setStyleClass("hiddenLinksForCasesContainerStyle");
		div.getChildren().add(linksContainer);

		DownloadLink attachmentLink = new DownloadLink();
		attachmentLink.setStyleClass(CasesEngineImp.FILE_DOWNLOAD_LINK_STYLE_CLASS);
		attachmentLink.setMediaWriterClass(AttachmentWriter.class);
		linksContainer.getChildren().add(attachmentLink);

		DownloadLink pdfLink = new DownloadLink();
		pdfLink.setStyleClass(CasesEngineImp.PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS);
		pdfLink.setMediaWriterClass(XFormToPDFWriter.class);
		linksContainer.getChildren().add(pdfLink);

		DownloadLink casePDFLink = new DownloadLink();
		casePDFLink.setStyleClass(CasesEngineImp.CASE_LOGS_PDF_DOWNLOAD_LINK_STYLE_CLASS);
		casePDFLink.setMediaWriterClass(CaseLogsToPDFWriter.class);
		linksContainer.getChildren().add(casePDFLink);

		DownloadLink taskInPdf = new DownloadLink();
		taskInPdf.setStyleClass(CasesEngineImp.DOWNLOAD_TASK_IN_PDF_LINK_STYLE_CLASS);
		taskInPdf.setMediaWriterClass(BPMTaskPDFPrinter.class);
		linksContainer.getChildren().add(taskInPdf);

		IWBundle bundle = getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(bundle.getFaceletURI("UICasesListAsset.xhtml"));
		div.getChildren().add(facelet);

		div.setValueExpression(renderedAtt, WFUtil.createValueExpression(context.getELContext(), "#{casesBPMAssetsState.assetsRendered}",
				Boolean.class));
		getFacets().put(assetsFacet, div);

		CasesBPMAssetsState stateBean = ELUtil.getInstance().getBean(CasesBPMAssetsState.beanIdentifier);
		stateBean.setCaseId(getCaseId());

		IWContext iwc = IWContext.getIWContext(context);

		if (isShowComments()) {
			String commentsManagerIdentifier = StringUtil.isEmpty(commentsPersistenceManagerIdentifier) ?
					BPMCommentsPersistenceManager.SPRING_BEAN_IDENTIFIER : commentsPersistenceManagerIdentifier;
			if (iwc.isParameterSet(CasesRetrievalManager.COMMENTS_PERSISTENCE_MANAGER_IDENTIFIER)) {
				commentsManagerIdentifier = iwc.getParameter(CasesRetrievalManager.COMMENTS_PERSISTENCE_MANAGER_IDENTIFIER);
			}
			CommentsPersistenceManager commentsManager = ELUtil.getInstance().getBean(commentsManagerIdentifier);
			if (commentsManager.hasRightsToViewComments(stateBean.getProcessInstanceId())) {
				CommentsViewer comments = new CommentsViewer();
				comments.setShowViewController(false);
				comments.setSpringBeanIdentifier(commentsManagerIdentifier);
				comments.setIdentifier(String.valueOf(stateBean.getProcessInstanceId()));
				comments.setNewestEntriesOnTop(true);
				comments.setShowCommentsList(stateBean.isAutoShowComments() ||
						(iwc.isParameterSet(CommentsViewer.AUTO_SHOW_COMMENTS) && iwc.getParameter(CommentsViewer.AUTO_SHOW_COMMENTS)
								.equals(Boolean.TRUE.toString())));
				comments.setAddLoginbyUUIDOnRSSFeedLink(true);
				comments.setStyleClass("commentsViewerForTaskViewerInCasesList");
				div.getChildren().add(comments);
			}
		}

		if (iwc.isParameterSet(CasesBPMAssetsState.CASES_ASSETS_SPECIAL_BACK_PAGE_PARAMETER)) {
			stateBean.setSpecialBackPage(iwc.getParameter(CasesBPMAssetsState.CASES_ASSETS_SPECIAL_BACK_PAGE_PARAMETER));
		} else if (!StringUtil.isEmpty(getSpecialBackPage()))
			stateBean.setSpecialBackPage(getSpecialBackPage());

		div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);

		facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(bundle.getFaceletURI("UICasesBPMAssetView.xhtml"));

		div.getChildren().add(facelet);
		div.setValueExpression(renderedAtt, WFUtil.createValueExpression(context.getELContext(), "#{casesBPMAssetsState.assetViewRendered}",
				Boolean.class));
		getFacets().put(assetViewFacet, div);

		if (!CoreUtil.isSingleComponentRenderingProcess(iwc)) {
			IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
			PresentationUtil.addJavaScriptActionToBody(iwc, "jQuery(document).ready(function() {showLoadingMessage('" +
					iwrb.getLocalizedString("loading", "Loading...") + "');});");
		}
	}

	public boolean isShowComments() {
		return showComments;
	}

	public void setShowComments(boolean showComments) {
		this.showComments = showComments;
	}

	public boolean isShowContacts() {
		return showContacts;
	}

	public void setShowContacts(boolean showContacts) {
		this.showContacts = showContacts;
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);

		UIComponent assets = getFacet(assetsFacet);
		UIComponent assetView = getFacet(assetViewFacet);

		if (assets.isRendered()) {
//			TODO: add assets grid client resources
			addClientResources(IWContext.getIWContext(context), assets);
			renderChild(context, assets);

		} else if (assetView.isRendered()) {
//			TODO: add asset client resources
			renderChild(context, assetView);
		}
	}

	public boolean isFullView() {
		return fullView;
	}

	public void setFullView(boolean fullView) {
		CaseManagerState caseHandlerState = WFUtil.getBeanInstance(CaseManagerState.beanIdentifier);
		caseHandlerState.setFullView(fullView);
		this.fullView = fullView;
	}

	public boolean isInCasesComponent() {
		return inCasesComponent;
	}

	public void setInCasesComponent(boolean inCasesComponent) {
		this.inCasesComponent = inCasesComponent;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected Long resolveProcessInstanceId(FacesContext fctx) {
		String piIdParam = fctx.getExternalContext().getRequestParameterMap().get(ProcessManagerBind.processInstanceIdParam);
		Long piId;

		if (piIdParam != null && !CoreConstants.EMPTY.equals(piIdParam)) {
			piId = new Long(piIdParam);
		} else
			piId = null;

		return piId;
	}

	public Long getProcessInstanceId(FacesContext fctx) {
		if (processInstanceId == null)
			processInstanceId = resolveProcessInstanceId(fctx);

		return processInstanceId;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public CasesBPMProcessView getCasesBPMProcessView() {
		return WFUtil.getBeanInstance(CasesBPMProcessView.BEAN_IDENTIFIER);
	}

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		if (caseId != null) {
			CasesBPMAssetsState stateBean = getBeanInstance(CasesBPMAssetsState.beanIdentifier);
			stateBean.setCaseId(caseId);
		}
		this.caseId = caseId;
	}

	private Web2Business getWeb2Business() {
		if (web2 == null)
			ELUtil.getInstance().autowire(this);

		return web2;
	}

	private JQuery getJQuery() {
		if (jQuery == null)
			ELUtil.getInstance().autowire(this);

		return jQuery;
	}

	@Autowired
	private CasesBPMDAO casesDAO;
	private CasesBPMDAO getCasesBPMDAO() {
		if (casesDAO == null)
			ELUtil.getInstance().autowire(this);
		return casesDAO;
	}

	private void addClientResources(IWContext iwc, UIComponent container) {
		IWBundle bundle = getBundle((FacesContext)iwc, IWBundleStarter.IW_BUNDLE_IDENTIFIER);

		Web2Business web2 = getWeb2Business();
		JQuery jQuery = getJQuery();

		//	CSS sources
		List<String> cssFiles = new ArrayList<String>();
		cssFiles.add(web2.getBundleUriToLinkLinksWithFilesStyleFile());
		cssFiles.add(web2.getBundleURIToJQGridStyles());
		cssFiles.add(web2.getBundleUriToHumanizedMessagesStyleSheet());
		cssFiles.add(iwc.getIWMainApplication().getBundle(CasesConstants.IW_BUNDLE_IDENTIFIER).getVirtualPathWithFileNameString("style/case.css"));
		cssFiles.add(web2.getBundleURIToFancyBoxStyleFile());
		PresentationUtil.addStyleSheetsToHeader(iwc, cssFiles);

		boolean isSingle = CoreUtil.isSingleComponentRenderingProcess(iwc);

		//	JS sources
		List<String> scripts = new ArrayList<String>();
		if (!isSingle) {
			scripts.add(jQuery.getBundleURIToJQueryLib());
		}
		scripts.addAll(web2.getBundleURIsToFancyBoxScriptFiles());
		scripts.add(web2.getBundleUriToLinkLinksWithFilesScriptFile());
		scripts.add(web2.getBundleURIToJQGrid());
		scripts.add(CoreConstants.DWR_ENGINE_SCRIPT);
		scripts.add(CoreConstants.DWR_UTIL_SCRIPT);
		scripts.add("/dwr/interface/BPMProcessAssets.js");
		if (isAllowPDFSigning()) {
			scripts.add("/dwr/interface/PDFGeneratorFromProcess.js");
		}
		scripts.add(web2.getBundleUriToHumanizedMessagesScript());
		scripts.add(bundle.getVirtualPathWithFileNameString("javascript/CasesBPMAssets.js"));
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, scripts);

		//	JS actions
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		String gridLocalization = new StringBuilder(
				"if(CasesBPMAssets.Loc == null || !CasesBPMAssets.Loc.inited) { \nif(CasesBPMAssets.Loc == null) { CasesBPMAssets.Loc = { inited: false }; }\n")

			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_CONTACT_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.human_name", "Name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_TASK_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.task_name", "Task name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FORM_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.document_name", "Document name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_SENDER = '")					.append(iwrb.getLocalizedString("sender", "Sender")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_DATE = '")						.append(iwrb.getLocalizedString("date", "Date")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_TAKEN_BY = '")					.append(iwrb.getLocalizedString("cases_bpm.assigned_to", "Taken by")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_EMAIL_ADDRESS = '")			.append(iwrb.getLocalizedString("email_address", "E-mail address")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_PHONE_NUMBER = '")				.append(iwrb.getLocalizedString("phone_number", "Phone number")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_ADDRESS = '")					.append(iwrb.getLocalizedString("address", "Address")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_SUBJECT = '")					.append(iwrb.getLocalizedString("cases_bpm.subject", "Subject")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_DESCRIPTION = '")			.append(iwrb.getLocalizedString("cases_bpm.file_description", "Descriptive name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_NAME = '")				.append(iwrb.getLocalizedString("cases_bpm.file_name", "File name")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS = '")		.append(iwrb.getLocalizedString("cases_bpm.change_access_rights", "Change access rights")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_DOWNLOAD_DOCUMENT_AS_PDF = '")	.append(iwrb.getLocalizedString("cases_bpm.get_document_as_pdf", "Download document")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_SIZE = '")				.append(iwrb.getLocalizedString("cases_bpm.file_size", "File size")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_SUBMITTED_BY = '")				.append(iwrb.getLocalizedString("cases_bpm.submitted_by", "Submitted by")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_GENERATING_PDF = '")			.append(iwrb.getLocalizedString("cases_bpm.generating_pdf", "Downloading PDF")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_LOADING = '")					.append(iwrb.getLocalizedString("cases_bpm.loading", "Loading...")).append("';\n")
			.append("CasesBPMAssets.Loc.CASE_GRID_STRING_ARE_YOU_SURE = '")				.append(iwrb.getLocalizedString("cases_bpm.are_you_sure", "Are you sure?")).append("';\n")

			.append("CasesBPMAssets.Loc.inited = true; }\n")

			.toString();

		String clientId = container.getClientId(iwc);
		if (clientId == null) {
			container.setId(iwc.getViewRoot().createUniqueId());
			clientId = container.getClientId(iwc);
		}

		CasesBPMAssetsState stateBean = getBeanInstance(CasesBPMAssetsState.beanIdentifier);
		Long processInstanceId = stateBean.getProcessInstanceId();
		Integer caseId = stateBean.getCaseId();
		if (caseId == null && processInstanceId != null) {
			caseId = getCasesBPMDAO().getCaseProcInstBindByProcessInstanceId(processInstanceId).getCaseId();
		}

		String specialBackPage = getSpecialBackPage();
		StringBuffer mainAction = new StringBuffer(gridLocalization).append("\n CasesBPMAssets.initGrid(jQuery('div.").append(clientId).append("')[0], ")
			.append(processInstanceId == null ? String.valueOf(-1) : processInstanceId.toString()).append(", ")
			.append(caseId == null ? String.valueOf(-1) : caseId.toString()).append(", ")
			.append(isUsePdfDownloadColumn()).append(", ").append(isAllowPDFSigning()).append(", ").append(isHideEmptySection()).append(", ")
			.append(isShowAttachmentStatistics()).append(", ").append(isShowOnlyCreatorInContacts()).append(", ").append(isShowLogExportButton())
			.append(", ").append(isShowComments()).append(", ").append(isShowContacts()).append(", ");
		if (StringUtil.isEmpty(specialBackPage))
			mainAction.append("null");
		else
			mainAction.append("'").append(specialBackPage).append("'");
		
		mainAction.append(CoreConstants.COMMA);
		mainAction.append(isNameFromExternalEntity()).append(CoreConstants.COMMA)
		.append(isShowUserProfilePicture()).append(");").toString();

		if (!isSingle)
			mainAction = new StringBuffer("jQuery(document).ready(function() {\n").append(mainAction.toString()).append("\n});");
		PresentationUtil.addJavaScriptActionToBody(iwc, mainAction.toString());
	}

	public boolean isUsePdfDownloadColumn() {
		return usePdfDownloadColumn;
	}

	public void setUsePdfDownloadColumn(boolean usePdfDownloadColumn) {
		this.usePdfDownloadColumn = usePdfDownloadColumn;
	}

	public boolean isAllowPDFSigning() {
		return allowPDFSigning;
	}

	public void setAllowPDFSigning(boolean allowPDFSigning) {
		this.allowPDFSigning = allowPDFSigning;
	}

	public boolean isHideEmptySection() {
		return hideEmptySection;
	}

	public void setHideEmptySection(boolean hideEmptySection) {
		this.hideEmptySection = hideEmptySection;
	}

	public String getCommentsPersistenceManagerIdentifier() {
		return commentsPersistenceManagerIdentifier;
	}

	public void setCommentsPersistenceManagerIdentifier(String commentsPersistenceManagerIdentifier) {
		this.commentsPersistenceManagerIdentifier = commentsPersistenceManagerIdentifier;
	}

	public boolean isShowAttachmentStatistics() {
		return showAttachmentStatistics;
	}

	public void setShowAttachmentStatistics(boolean showAttachmentStatistics) {
		this.showAttachmentStatistics = showAttachmentStatistics;
	}

	public boolean isShowOnlyCreatorInContacts() {
		return showOnlyCreatorInContacts;
	}

	public void setShowOnlyCreatorInContacts(boolean showOnlyCreatorInContacts) {
		this.showOnlyCreatorInContacts = showOnlyCreatorInContacts;
	}

	public boolean isShowLogExportButton() {
		return showLogExportButton;
	}

	public void setShowLogExportButton(boolean showLogExportButton) {
		this.showLogExportButton = showLogExportButton;
	}

	public String getSpecialBackPage() {
		return specialBackPage;
	}

	public void setSpecialBackPage(String specialBackPage) {
		this.specialBackPage = specialBackPage;
	}

	public boolean isNameFromExternalEntity() {
		return nameFromExternalEntity;
	}

	public void setNameFromExternalEntity(boolean nameFromExternalEntity) {
		this.nameFromExternalEntity = nameFromExternalEntity;
	}

	public boolean isShowUserProfilePicture() {
		return showUserProfilePicture;
	}

	public void setShowUserProfilePicture(boolean showUserProfilePicture) {
		this.showUserProfilePicture = showUserProfilePicture;
	}
}