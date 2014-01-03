var CasesExporter = {};

CasesExporter.FINSHED_EXPORT = false;

CasesExporter.doExportCases = function(params) {
	var procDefId = jQuery('#' + params.dropdownId).val();
	CasesExporter.FINSHED_EXPORT = false;
	
	showLoadingMessage(params.loading);
	CasesEngine.getExportedCasesToPDF(procDefId, params.id, {
		callback: function(result) {
			CasesExporter.FINSHED_EXPORT = true;
			closeAllLoadingMessages();
			if (result == null) {
				humanMsg.displayMsg('Error', {timeout: 3000});
				return;
			}
			
			humanMsg.displayMsg(result.value, {timeout: 3000});
			
			if (result.id == 'true') {
				jQuery('#' + params.resultsId).text(result.name);
				jQuery('#' + params.resultsId).removeAttr('style');
			}
		}, errorHandler: function(o1, o2) {
			closeAllLoadingMessages();
			humanMsg.displayMsg('Error', {timeout: 3000});
			return;
		}
	});
	
	CasesExporter.doFetchStatus(params.id);
}

CasesExporter.doFetchStatus = function(id) {
	if (CasesExporter.FINSHED_EXPORT) {
		closeAllLoadingMessages();
		return;
	}
	
	CasesEngine.getStatusOfExport(id, {
		callback: function(result) {
			if (result == null) {
				window.setTimeout(function() {CasesExporter.doFetchStatus(id);}, 1000);
			} else {
				jQuery('#loadingtext').html(result.value);
				window.setTimeout(function() {CasesExporter.doFetchStatus(id);}, 1000);
			}
		}
	});
}