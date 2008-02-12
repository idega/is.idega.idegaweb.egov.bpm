package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.faces.component.UIComponent;

import com.idega.block.process.business.CasesListColumn;
import com.idega.block.process.data.Case;
import com.idega.business.SpringBeanName;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableRow;
import com.idega.presentation.TableRowGroup;
import com.idega.presentation.text.Text;
import com.idega.repository.data.Singleton;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;

@SpringBeanName("casesPresentationHelper")
public class CasesPresentationHelper implements Singleton {
	
	private String tableHeaderCellStyleClass = "tableHeaderCell";
	private String axisAttribute = "axis";
	private String caseIdAttribute = "caseid";
	
	private boolean tableSortable = true;
	
	public UIComponent getCasesListViewer(List<Case> cases, IWContext iwc, IWResourceBundle iwrb, List<CasesListColumn> columns) {
		Layer container = new Layer();
		if (iwc == null || iwrb == null || columns == null) {
			container.add(new Text("Error!"));
			return container;
		}
		
		if (cases == null) {
			container.add(new Text(iwrb.getLocalizedString("no_cases", "No cases found")));
			return container;
		}
		
		Layer casesContainer = new Layer();
		container.add(casesContainer);
		
		Table2 casesTable = new Table2();
		casesTable.getId();
		casesTable.setCellpadding(0);
		casesTable.setCellspacing(0);
		if (tableSortable) {
			casesTable.setStyleClass("egovCasesTable");
		}
		casesContainer.add(casesTable);
		
		TableRowGroup header = casesTable.createHeaderRowGroup();
		TableRow row = header.createRow();
		
		CasesListColumn column = null;
		TableCell2 cell = null;
		for (int i = 0; i < columns.size(); i++) {
			column = columns.get(i);
			
			cell = row.createHeaderCell();
			cell.add(new Text(column.getName()));
			if (column.getType() != null) {
				cell.setMarkupAttribute(axisAttribute, column.getType());
			}
			cell.setId(column.getId());
			cell.setStyleClass(tableHeaderCellStyleClass);	
		}
		
		Case c = null;
		String noValue = "-";
		Locale locale = iwc.getIWMainApplication().getSettings().getDefaultLocale();
		IWTimestamp timestamp = null;
		String caseId = null;
		User creator = null;
		Group handler = null;
		for (int i = 0; i < cases.size(); i++) {
			c = cases.get(i);
			caseId = String.valueOf(new Random().nextInt());//c.getId();	//	TODO
			
			creator = c.getCreator();
			handler = c.getHandler();
			
			row = casesTable.createRow();
			if (i % 2 == 0) {
				row.setStyleClass("evenRow");
			}
			else {
				row.setStyleClass("oddRow");
			}
			if ((i+1) == cases.size()) {
				row.setStyleClass("lastRow");
			}
			
			row.setMarkupAttribute(caseIdAttribute, caseId);
			
			cell = getRowCell(row, caseId);
			cell.add(new Text(c.getCaseNumber()));
			
			cell = getRowCell(row, caseId);
			cell.add(new Text(c.getStatus()));
			
			cell = row.createCell();
			/*if (c.getAttachements() != null || c.getSubCases() != null) {	//	TODO
				cell.add(new Text("Expand"));
			}*/
			
			cell = getRowCell(row, caseId);
			cell.add(new Text(creator == null ? noValue : creator.getName()));
			
			cell = getRowCell(row, caseId);
			cell.add(new Text(handler == null ? noValue : handler.getName()));
			
		    timestamp = new IWTimestamp(c.getCreated());
			cell = getRowCell(row, caseId);
			cell.add(new Text(timestamp.getLocaleDate(locale, DateFormat.LONG)));
		}
		
		return container;
	}
	
	private TableCell2 getRowCell(TableRow row, String caseId) {
		TableCell2 cell = row.createCell();
		cell.setMarkupAttribute(caseIdAttribute, caseId);
		cell.setStyleClass("casesListViewerTableRowCellStyle");
		return cell;
	}

	public String getTableHeaderCellStyleClass() {
		return tableHeaderCellStyleClass;
	}

	public void setTableHeaderCellStyleClass(String tableHeaderCellStyleClass) {
		this.tableHeaderCellStyleClass = tableHeaderCellStyleClass;
	}

	public boolean isTableSortable() {
		return tableSortable;
	}

	public void setTableSortable(boolean tableSortable) {
		this.tableSortable = tableSortable;
	}

}
