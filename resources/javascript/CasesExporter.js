var CasesExporter = {};

CasesExporter.FINSHED_EXPORT = false;

CasesExporter.doExportCases = function(params) {
	var container = jQuery('#' + params.dropdownId).parent().parent();
	var procDefId = jQuery('#' + params.dropdownId).val();
	CasesExporter.FINSHED_EXPORT = false;
	jQuery('#' + params.resultsId).html('');
	
	var status = jQuery('select[name="cf_prm_case_status"]', container).val();
	var dateRange = jQuery('input[name="dateRange"]', container).val();
	var from = null;
	var to = null;
	if (dateRange != null && dateRange.indexOf(' - ') != -1) {
		var range = dateRange.split(' - ');
		if (range != null && range.length == 2) {
			from = range[0];
			to = range[1];
		}
	}
	
	if ((status == null || status == '-1') && (from == null || to == null)) {
		humanMsg.displayMsg(params.selectCriterias, {timeout: 3000});
		return false;
	}
	
	showLoadingMessage(params.exporting);
	var exportParams = {
		processDefinitionId: procDefId,
		id: params.id,
		status: status,
		dateFrom: from,
		dateTo: to
	};
	LazyLoader.loadMultiple(['/dwr/engine.js', '/dwr/interface/CasesEngine.js'], function() {
		CasesEngine.getExportedCasesToPDF(exportParams, {
			callback: function(result) {
				closeAllLoadingMessages();
				if (result == null) {
					humanMsg.displayMsg('Error', {timeout: 3000});
					return;
				}
				
				if (result.name != '0' && window.confirm(result.value)) {
					showLoadingMessage(params.exporting);
					CasesEngine.doActualExport(params.id, {
						callback: function(result) {
							CasesExporter.FINSHED_EXPORT = true;
							humanMsg.displayMsg(result.value, {timeout: 3000});
					
							if (result.id == 'true') {
								CasesExporter.doShowExportedCases(params);
							}
						}
					});
					CasesExporter.doFetchStatus(params.id);
				} else {
					closeAllLoadingMessages();
					if (result.name == '0') {
						humanMsg.displayMsg(result.value, {timeout: 3000});
					}
					CasesEngine.doRemoveFromMemory(params.id);
				}
			},
			errorHandler: function(o1, o2) {
				closeAllLoadingMessages();
				CasesExporter.doShowExportedCases(params);
				return;
			},
			timeout: 86400000
		});
	}, null);
}

CasesExporter.doShowExportedCases = function(params) {
	CasesExporter.FINSHED_EXPORT = true;
	showLoadingMessage(params.loading);
	IWCORE.getRenderedComponentByClassName({
		className: params.resultsUI,
		properties: [
			{id: 'setExportId', value: params.id}	
		],
		container: params.resultsId,
		rewrite: true,
		callback: function() {
			closeAllLoadingMessages();
		}
	});
}

CasesExporter.doFetchStatus = function(id) {
	if (CasesExporter.FINSHED_EXPORT) {
		closeAllLoadingMessages();
		return;
	}
	
	CasesEngine.getStatusOfExport(id, {
		callback: function(result) {
			if (result == null) {
				window.setTimeout(function() {CasesExporter.doFetchStatus(id);}, 5000);
			} else {
				jQuery('#loadingtext').html(result.value);
				window.setTimeout(function() {CasesExporter.doFetchStatus(id);}, 5000);
			}
		}
	});
}

CasesExporter.doDownload = function(id, caseIdentifier) {
	var identifiers = null;
	if (caseIdentifier != null && caseIdentifier != '') {
		identifiers = [];
		identifiers.push(caseIdentifier);
	}
	CasesEngine.getLinkForZippedCases(id, identifiers, {
		callback: function(result) {
			if (result == null) {
				return false;
			} else if (result.id == 'false') {
				humanMsg.displayMsg(result.value, {timeout: 3000});
			} else if (result.id == 'true') {
				window.location.href = result.value;
				humanMsg.displayMsg(result.name, {timeout: 3000});
			}
		}
	});
}


