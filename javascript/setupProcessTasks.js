jQuery(document).ready(function(){

    var tasksList = new ProcessTasksList();
    var tbl = tasksList.createArtifactsTable("#tasksList", CasesJbpmProcessArtifacts);
});