package is.idega.idegaweb.egov.bpm.scheduler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
//@Transactional
public class UnsafeSessionUsageTest {//extends _AbstractDbTestCase {
	
	@Test
	public void testDummy() {
		
	}
	/*
  @Test
  public void testTimerRepeat() {
	  
	  if(true)
		  return;
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <timer name='reminder' duedate='5 seconds' >" +
      "      <action class='is.idega.idegaweb.egov.bpm.scheduler.TimerDBTest$NoOp' />" +
      "    </timer>" +
      "    <transition to='b'/>" +
      "    <transition name='back' to='a'/>" +
      "  </state>" +
      "  <state name='b'/>" +
      "</process-definition>"
    );
    processDefinition = saveAndReload(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      
      jbpmContext.save(processInstance);

      processJobs(6000);
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
  */
}
