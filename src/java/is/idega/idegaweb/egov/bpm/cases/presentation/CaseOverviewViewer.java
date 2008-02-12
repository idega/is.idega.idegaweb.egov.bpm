package is.idega.idegaweb.egov.bpm.cases.presentation;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMAssetsEngineBean;

import java.rmi.RemoteException;
import java.text.DateFormat;

import javax.faces.context.FacesContext;

import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.core.contact.data.PhoneType;
import com.idega.core.location.data.Address;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Span;
import com.idega.presentation.text.Heading1;
import com.idega.presentation.text.Text;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

public class CaseOverviewViewer extends IWBaseComponent {

	private String caseId = null;
	
	private IWResourceBundle iwrb = null;
	
	private String personInfoStyle = "personInfo";
	private String caseInfoItemStyle = "formItem";
	private String caseOverviewLabel = "caseOverviewLabel";
	private String caseOverviewValue = "caseOverviewValue";
	
	public Object saveState(FacesContext ctx) {
		Object values[] = new Object[5];
		values[0] = super.saveState(ctx);
		values[1] = personInfoStyle;
		values[2] = caseInfoItemStyle;
		values[3] = caseOverviewLabel;
		values[4] = caseOverviewValue;
		return values;
	}

	public void restoreState(FacesContext ctx, Object state) {
		Object values[] = (Object[]) state;
		super.restoreState(ctx, values[0]);
		personInfoStyle = (String) values[1];
		caseInfoItemStyle = (String) values[2];
		caseOverviewLabel = (String) values[3];
		caseOverviewValue = (String) values[4];
	}
	
	public CaseOverviewViewer(String caseId) {
		this.caseId = caseId;
	}
	
	protected void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		Layer container = new Layer();
		
		if (caseId == null) {
			container.add(new Text(getResourceBundle(iwc).getLocalizedString("error", "Error!")));
			return;
		}
		BPMAssetsEngineBean engine = (BPMAssetsEngineBean)getBeanInstance("casesBPMAssets");
		if (engine == null) {
			container.add(new Text(getResourceBundle(iwc).getLocalizedString("error", "Error!")));
			return;
		}
		Case userCase = engine.getCase(caseId);
		if (userCase == null) {
			container.add(new Text(getResourceBundle(iwc).getLocalizedString("case_not_found", "Sorry, unable to find case.")));
			return;
		}
		
		addSenderInfo(iwc, container, userCase);
		
		addCaseInfo(iwc, container, userCase);
		
		addButtons(iwc, container);
		
		add(container);
	}
	
	private IWResourceBundle getResourceBundle(IWContext iwc) {
		if (iwrb == null) {
			iwrb = getIWResourceBundle(iwc, ProcessConstants.IW_BUNDLE_IDENTIFIER);
		}
		return iwrb;
	}
	
	private void addSenderInfo(IWContext iwc, Layer container, Case userCase) {
		Layer senderInfo = new Layer();
		senderInfo.setStyleClass("info");
		container.add(senderInfo);
		
		User sender = null;//userCase.getSender();
		if (sender == null) {
			return;
		}
		
		String userID = sender.getId();
		if (userID == null) {
			return;
		}
		int userId = -1;
		try {
			userId = Integer.valueOf(userID);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return;
		}
		
		Object o = null;
		try {
			o = IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
		if (!(o instanceof UserBusiness)) {
			return;
		}
		UserBusiness userBusiness = (UserBusiness) o;
		
		//	Name
		String name = sender.getName();
		Layer nameContainer = new Layer();
		senderInfo.add(nameContainer);
		nameContainer.setId("name");
		nameContainer.setStyleClass(personInfoStyle);
		nameContainer.add(name == null ? new Text(CoreConstants.EMPTY) : new Text(name));
		
		//	Personal ID
		String personalId = sender.getPersonalID();
		Layer idContainer = new Layer();
		senderInfo.add(idContainer);
		idContainer.setId("personalID");
		idContainer.setStyleClass(personInfoStyle);
		idContainer.add(personalId == null ? new Text(CoreConstants.EMPTY) : new Text(personalId));
		
		//	Address
		Address address = null;
		try {
			address = userBusiness.getUsersMainAddress(sender);
		} catch (RemoteException e) {}
		Layer addressContainer = new Layer();
		senderInfo.add(addressContainer);
		addressContainer.setId("address");
		addressContainer.setStyleClass(personInfoStyle);
		if (address != null) {
			String streeAddress = address.getStreetAddress();
			addressContainer.add(streeAddress == null ? new Text(CoreConstants.EMPTY) : new Text(streeAddress));
		}
		
		//	Phone
		Phone phone = null;
		try {
			phone = userBusiness.getUserPhone(userId, PhoneType.HOME_PHONE_ID);
		} catch (RemoteException e) {}
		if (phone == null) {
			try {
				phone = userBusiness.getUserPhone(userId, PhoneType.MOBILE_PHONE_ID);
			} catch (RemoteException e) {}
		}
		if (phone == null) {
			try {
				phone = userBusiness.getUserPhone(userId, PhoneType.WORK_PHONE_ID);
			} catch (RemoteException e) {}
		}
		Layer phoneContainer = new Layer();
		senderInfo.add(phoneContainer);
		phoneContainer.setId("phone");
		phoneContainer.setStyleClass(personInfoStyle);
		if (phone != null) {
			String phoneNumber = phone.getNumber();
			phoneContainer.add(phoneNumber == null ? new Text(CoreConstants.EMPTY) : new Text(phoneNumber));
		}
		
		//	Postal
		Layer postalContainer = new Layer();
		senderInfo.add(postalContainer);
		postalContainer.setId("postal");
		postalContainer.setStyleClass(personInfoStyle);
		if (address != null) {
			String postal = address.getPostalAddress();
			postalContainer.add(postal == null ? new Text(CoreConstants.EMPTY) : new Text(postal));
		}
		
		//	Email
		Email email = null;
		try {
			email = userBusiness.getUsersMainEmail(sender);
		} catch (RemoteException e) {
		} catch (NoEmailFoundException e) {}
		Layer emailContainer = new Layer();
		senderInfo.add(emailContainer);
		emailContainer.setId("email");
		emailContainer.setStyleClass(personInfoStyle);
		if (email != null) {
			String emailAddress = email.getEmailAddress();
			emailContainer.add(emailAddress == null ? new Text(CoreConstants.EMPTY) : new Text(emailAddress));
		}
	}
	
	private void addCaseInfo(IWContext iwc, Layer container, Case userCase) {
		// 	TODO: implement real logic
		
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		Heading1 title = new Heading1(iwrb.getLocalizedString("case_overview", "Case overview"));
		title.setStyleClass("subHeader topSubHeader");
		container.add(title);
		
		Layer caseInfo = new Layer();
		caseInfo.setStyleClass("formSection");
		container.add(caseInfo);
		
		//	Category
		Layer infoItem = new Layer();
		caseInfo.add(infoItem);
		infoItem.setStyleClass(caseInfoItemStyle);
		Span category = new Span(new Text(iwrb.getLocalizedString("case_category", "Case category")));
		category.setStyleClass(caseOverviewLabel);
		infoItem.add(category);
		Span text = new Span();
		text.setStyleClass(caseOverviewValue);
		/*if (userCase.getMainCategory() != null) {
			text.add(new Text(userCase.getMainCategory()));
		}*/
		text.add(new Text("Electronic devices"));
		infoItem.add(text);
		
		//	Sub-category
		infoItem = new Layer();
		caseInfo.add(infoItem);
		infoItem.setStyleClass(caseInfoItemStyle);
		Span subCategory = new Span(new Text(iwrb.getLocalizedString("case_sub_category", "Case sub-category")));
		subCategory.setStyleClass(caseOverviewLabel);
		infoItem.add(subCategory);
		text = new Span();
		text.setStyleClass(caseOverviewValue);
		/*if (userCase.getSubCategory() != null) {
			text.add(new Text(userCase.getSubCategory()));
		}*/
		text.add(new Text("Computers"));
		infoItem.add(text);
		
		//	Date
		infoItem = new Layer();
		caseInfo.add(infoItem);
		infoItem.setStyleClass(caseInfoItemStyle);
		Span date = new Span(new Text(iwrb.getLocalizedString("created_date", "Created date")));
		date.setStyleClass(caseOverviewLabel);
		infoItem.add(date);
		text = new Span();
		text.setStyleClass(caseOverviewValue);
		if (userCase.getCreated() != null) {
			IWTimestamp timestamp = new IWTimestamp(userCase.getCreated());
			text.add(new Text(timestamp.getLocaleDate(iwc.getCurrentLocale(), DateFormat.LONG)));
		}
		infoItem.add(text);
		
		//	Info
		infoItem = new Layer();
		caseInfo.add(infoItem);
		infoItem.setStyleClass(caseInfoItemStyle);
		Span info = new Span(new Text(iwrb.getLocalizedString("message", "Message")));
		info.setStyleClass(caseOverviewLabel);
		infoItem.add(info);
		Object form = "My complaint here";//userCase.getForm();
		if (form instanceof String) {
			text = new Span();
			text.setStyleClass(caseOverviewValue);
			text.add(new Text(form.toString()));
			infoItem.add(text);
		}
		else {
			//	TODO
		}
	}
	
	private void addButtons(IWContext iwc, Layer container) {
		
	}
	
}
