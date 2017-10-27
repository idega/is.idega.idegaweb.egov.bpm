package com.idega.idegaweb.egov.bpm.business;

public interface BPMManipulator {

	public boolean doReSubmitProcess(Long piId, boolean onlyStart, boolean submitRepeatedTasks);
	public boolean doReSubmitCase(Integer caseId, boolean onlyStart, boolean submitRepeatedTasks);

	public boolean doReSubmitCaseWithVariables(Integer caseId, boolean onlyStart, boolean submitRepeatedTasks, String variablesEncodedBase64);

	public boolean doSubmitVariables(Integer caseId, String variablesEncodedBase64);

}