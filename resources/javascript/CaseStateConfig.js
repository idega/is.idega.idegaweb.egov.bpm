CaseStateConfig = {};

CaseStateConfig.statrEdit = function(element){
	jQuery('.iw_state_form').each(function(){jQuery(this).hide();});
	jQuery('.iw_state_div').each(function(){jQuery(this).show();});
	jQuery(element).parent().siblings('.iw_state_form').show();
	jQuery(element).parent().hide();
}	



CaseStateConfig.cancelEdit = function(element){
	jQuery(element).parent().siblings('.iw_state_div').show();
	jQuery(element).parent().hide();
}

CaseStateConfig.processChanged = function(selected){
	jQuery('.iw_single_process').each(function(){jQuery(this).hide();});
	jQuery('.iw_single_process.'+selected).each(function(){jQuery(this).show();});
}

jQuery(document).ready(function(){
	jQuery('.processType').each(function(){
		CaseStateConfig.processChanged(jQuery(this).val());
	});
});