jQuery(document).ready(function(){

    console.log('sap');
    var artifactsList = new ProcessArtifactsList();
    
    console.log('CasesJbpmProcessArtifacts: '+CasesJbpmProcessArtifacts.getProcessArtifactsList);
    var tbl = artifactsList.createArtifactsTable("#artifactsList", CasesJbpmProcessArtifacts);
});