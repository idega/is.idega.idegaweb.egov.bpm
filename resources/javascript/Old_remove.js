var SEARCH_WINDOW_TITLE = 'Search for cases';
var INVALID_SEARCH_TEXT = 'Enter any text to search for';
var LOADING_TEXT = 'Loading...';

var SEARCH_WINDOW_CLASS = null;

var CASES_TABLES = new Array();

var SEARCH_EXECUTED = false;

function getLocalizationForCases() {
	CasesBPMAssets.getInfoForCases(getLocalizationForCasesCallback);
}

function getLocalizationForCasesCallback(values) {
	if (values == null) {
		return;
	}
	if (values.length == 0) {
		return;
	}
	
	SEARCH_WINDOW_TITLE = values[0];
	SEARCH_WINDOW_CLASS = values[1];
	INVALID_SEARCH_TEXT = values[2];
	LOADING_TEXT = values[3];
}

function initializeCasesTables() {
	$$('table.egovCasesTable').each(
		function(table) {
			var sortableCasesTable = new sortableTable(table.id);
			CASES_TABLES.push(sortableCasesTable);
		}
	);
}

function initializeActions(fullInitialization) {
	$$('td.casesListViewerTableRowCellStyle').each(
		function(cell) {
			cell.addEvent('click',  function() {
				setCaseForPreview(getMarkupAttributeValue(cell, 'caseid'));
			});
		}
	);
	
	if (fullInitialization) {
		$$('select.casesTypeSelectorStyle').each(
			function(select) {
				select.addEvent('change', function() {
					reloadCasesByNewType(select.value);
				});
			}
		);
	}
	
	initializeCasesTables();
}

function reloadCasesByNewType(caseType) {
	var casesTables = $$('table.egovCasesTable');
	if (casesTables == null) {
		return false;
	}
	if (casesTables.length == 0) {
		return false;
	}
	var casesTable = casesTables[0];
	var container = $$('div.casesListViewerContainerStyle')[0];
	if (container == null) {
		return false;
	}
	
	showLoadingMessage(LOADING_TEXT);
	CasesBPMAssets.getCasesList(caseType, {
		callback: function(casesList) {
			getCasesListCallback(casesList, container, casesTable);
		}
	});
}

function getCasesListCallback(casesList, container, casesTable) {
	closeAllLoadingMessages();
	if (casesList == null || container == null) {
		return false;
	}
	removeChildren(container);
	
	insertNodesToContainer(casesList, container);
	
	initializeActions(false);
}

function registerSearchWindowActions() {
	var dropdownMenus = $$('select.textInputForCasesSearchStyleClass');
	
	$$('input.textInputForCasesSearchStyleClass').each(
		function(input) {
			input.focus();
			input.addEvent('keyup', function(event) {
    			tryToSearchIfEnterPressed(new Event(event), input.id, dropdownMenus);
			});
		}
	);
}

function openTableFilter() {
	MOOdalBox.init({resizeDuration: 0, evalScripts: true, animateCaption: false});
	var result = MOOdalBox.open('/servlet/ObjectInstanciator?idegaweb_instance_class=' + SEARCH_WINDOW_CLASS, SEARCH_WINDOW_TITLE, '300 100');
	return false;
}

function searchForCasesInTable(textInputId, optionId) {
	clearSearchResults();
	
	SEARCH_EXECUTED = true;
	var textInput = $(textInputId);
	if (textInput.value == '' || textInput.value == null) {
		alert(INVALID_SEARCH_TEXT);
		return false;
	}
	
	var option = $(optionId);
	
	for (var i = 0; i < CASES_TABLES.length; i++) {
		CASES_TABLES[i].filterTableByParams(textInput.value, option.value);
	}
}

function tryToSearchIfEnterPressed(event, textInputId, dropdownMenus) {
	if (event.key == 'enter' && dropdownMenus) {
		if (dropdownMenus.length > 0) {
			searchForCasesInTable(textInputId, dropdownMenus[0].options[dropdownMenus[0].selectedIndex].id);
		}
	}
	return false;
}

function clearSearchResults() {
	SEARCH_EXECUTED = false;
	for (var i = 0; i < CASES_TABLES.length; i++) {
		CASES_TABLES[i].clearFilter();
	}
}

function clearSearchTerms(id) {
	clearSearchResults();
	$(id).setProperty('value', '');
}

function getMarkupAttributeValue(element, attrName) {
	if (element == null || attrName == null) {
		return null;
	}
	return element.getProperty(attrName);
}

function setCaseForPreview(caseId) {
	showLoadingMessage(LOADING_TEXT);
	CasesBPMAssets.getCaseOverview(caseId, getCaseOverviewCallback);
}

function getCaseOverviewCallback(component) {
	closeAllLoadingMessages();
	
	var container = $('caseOverviewContainer');
	removeChildren(container);
	
	if (component == null) {
		return false;
	}
	
	insertNodesToContainer(component, container);
}