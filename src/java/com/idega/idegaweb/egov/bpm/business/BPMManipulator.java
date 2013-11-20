package com.idega.idegaweb.egov.bpm.business;

public interface BPMManipulator {

	public boolean doReSubmitProcess(Long piId);
	public boolean doReSubmitCase(Integer caseId);

}