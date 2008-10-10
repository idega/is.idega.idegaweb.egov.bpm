package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.testbase.EgovBPMBaseTest;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.IdegaJbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/10/10 11:07:00 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public final class SendCaseMessagesHandlerTest extends EgovBPMBaseTest {

	@Autowired
	private IdegaJbpmContext bpmContext;
	
	void deployProcessDefinitions() throws Exception {

		JbpmContext jctx = bpmContext.createJbpmContext();
		
		try {
			ProcessDefinition superProcess = ProcessDefinition.parseXmlString(
						      "<process-definition name='super'>" +
						      
						      "  <start-state>" +
						      "    <transition name='toSendNewCaseArrivedMessagesNode' to='SendNewCaseArrivedMessagesNode' />" +
						      "  </start-state>" +
						      "<node name=\"SendNewCaseArrivedMessagesNode\">" +
						      "<event type=\"node-leave\">" +
						      "<action name=\"SendNewCaseArrivedMessages\" class=\"is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler\">" +
						      "<sendToRoles>bpm_lawyers_handler</sendToRoles>" +
						      "<inlineSubject key-type='java.lang.String' value-type='java.lang.String'>" +
							      "  <entry><key>en</key><value>english subject</value></entry>" +
							      "  <entry><key>is_IS</key><value>icelandic subject</value></entry>" +
						      "</inlineSubject>" +
						      "<inlineMessage key-type='java.lang.String' value-type='java.lang.String'>" +
						      "  <entry><key>en</key><value>english message</value></entry>" +
						      "  <entry><key>is_IS</key><value>icelandic message</value></entry>" +
						      "</inlineMessage>" +
						      "</action>" +
						      "</event>" +
						      "    <transition name='toEnd' to='end' />" +
						      "</node>" +
						      "<end-state name='end'></end-state>"+
						      "</process-definition>"   
						    );
			jctx.deployProcessDefinition(superProcess);
			
		} finally {
			bpmContext.closeAndCommit(jctx);
		}
	}
	
	@Test
	public void testSend() throws Exception {
		
		if(true)
			return;
		
		deployProcessDefinitions();
		
		JbpmContext jbpmContext = bpmContext.createJbpmContext();
		
		try {
			ProcessInstance pi = jbpmContext.newProcessInstance("super");
			pi.getContextInstance().setVariable(CasesBPMProcessConstants.caseIdVariableName, "1");
			pi.signal();

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
	}
}