package com.idega.idegaweb.egov.bpm.business;

import java.util.List;

import com.idega.builder.bean.AdvancedProperty;

public interface BPMManipulator {

	public boolean doReSubmitProcess(Long piId, boolean onlyStart, boolean submitRepeatedTasks);
	public boolean doReSubmitCase(Integer caseId, boolean onlyStart, boolean submitRepeatedTasks);

	public boolean doReSubmitCaseWithVariables(Integer caseId, boolean onlyStart, boolean submitRepeatedTasks, String variablesEncodedBase64);
	public boolean doReSubmitCaseByIdentifierWithVariables(String caseIdentifier, boolean onlyStart, boolean submitRepeatedTasks, String variablesEncodedBase64);

	public boolean doSubmitVariables(Integer caseId, String variablesEncodedBase64);

	public boolean doExecuteHandler(Long procInstId, String handlerName, List<AdvancedProperty> params);

}