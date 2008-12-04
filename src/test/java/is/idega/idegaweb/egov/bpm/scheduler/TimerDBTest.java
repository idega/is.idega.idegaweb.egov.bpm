/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package is.idega.idegaweb.egov.bpm.scheduler;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Restrictions;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class TimerDBTest extends AbstractDbTestCase {
  private static Log log = LogFactory.getLog(Timer.class);

  static boolean isNoOpExecuted = false;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    isNoOpExecuted = false;
    jbpmContext.getJbpmConfiguration().startJobExecutor();
  }
  
  @Test
  public void testSaveTimer() {
    final Date now = Calendar.getInstance().getTime();

    Timer timer = new Timer();
    timer.setName("timer-name");
    timer.setDueDate(now);
    timer.setTransitionName("transition-name");
    timer.setRepeat("repeat-duration");

    session.save(timer);
    try {
      newTransaction();

      timer = (Timer) session.load(Timer.class, new Long(timer.getId()));
      assertEquals("timer-name", timer.getName());

      // we test for the same date in a simple format
      // DateFormat df = SimpleDateFormat.getDateInstance();
      // assertEquals(df.format(now), df.format(timer.getDueDate()));

      // we test for each part of the date to see where we fail per database
      // to help with debugging.
      Calendar ncal = new GregorianCalendar();
      ncal.setTime(now);
      Calendar tcal = new GregorianCalendar();
      tcal.setTime(timer.getDueDate());
      assertEquals(ncal.get(Calendar.YEAR), tcal.get(Calendar.YEAR));
      assertEquals(ncal.get(Calendar.MONTH), tcal.get(Calendar.MONTH));
      assertEquals(ncal.get(Calendar.DAY_OF_MONTH), tcal.get(Calendar.DAY_OF_MONTH));
      assertEquals(ncal.get(Calendar.HOUR_OF_DAY), tcal.get(Calendar.HOUR_OF_DAY));
      assertEquals(ncal.get(Calendar.MINUTE), tcal.get(Calendar.MINUTE));
      assertEquals(ncal.get(Calendar.SECOND), tcal.get(Calendar.SECOND));
//      assertEquals(DateDbTestUtil.getInstance().convertDateToSeconds(now),
//          DateDbTestUtil.getInstance().convertDateToSeconds(timer.getDueDate()));
      assertEquals("transition-name", timer.getTransitionName());
      assertEquals("repeat-duration", timer.getRepeat());
    }
    finally {
      session.delete(timer);
    }
  }

  @Test
  public void testTimerCreation() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='catch crooks' />"
        + "  </start-state>"
        + "  <state name='catch crooks'>"
        + "    <timer name='reminder' duedate='5 seconds' transition='toEnd'/>"
        + "    <transition name='toEnd' to='end'/>"
        + "  </state>"
        + "  <end-state name='end'/>"
        + "</process-definition>");

    graphSession.saveProcessDefinition(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      jbpmContext.save(processInstance);

      newTransaction();
      
      Thread.sleep(3000);

      Timer timer = (Timer) session.createQuery("from org.jbpm.job.Timer").uniqueResult();
      assertNotNull("Timer is not null", timer);
      assertEquals("reminder", timer.getName());
      assertEquals("catch crooks", timer.getGraphElement().getName());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  @Test
  public void testTimerCancellation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='catch crooks' />"
        + "  </start-state>"
        + "  <state name='catch crooks'>"
        + "    <timer name='reminder' duedate='5 seconds' />"
        + "    <transition to='end'/>"
        + "  </state>"
        + "  <end-state name='end'/>"
        + "</process-definition>");

    graphSession.saveProcessDefinition(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      processInstance.signal();

      newTransaction();

      assertEquals(0, getTimerCount());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  @Test
  public void testTimerAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='process'>"
        + "  <start-state>"
        + "    <transition to='sometask' />"
        + "  </start-state>"
        + "  <task-node name='sometask'>"
        + "    <timer name='reminder'"
        + "           duedate='1 business minutes'"
        + "           repeat='1 business minutes'"
        + "           transition='time-out-transition' >"
        + "      <action class='my-action-handler-class-name' />"
        + "    </timer>"
        + "    <task name='do something'/>"
        + "    <transition name='time-out-transition' to='sometask' />"
        + "  </task-node>"
        + "</process-definition>");

    graphSession.saveProcessDefinition(processDefinition);
    try {
      Node node = processDefinition.getNode("sometask");

      List actions = node.getEvent(Event.EVENTTYPE_NODE_ENTER).getActions();
      assertEquals(1, actions.size());
      
      actions = node.getEvent(Event.EVENTTYPE_NODE_LEAVE).getActions();
      assertEquals(1, actions.size());

      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);

      newTransaction();
      
      assertEquals(1, getTimerCount());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  @Test
  public void testTaskTimerExecution() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='timed task' />"
        + "  </start-state>"
        + "  <task-node name='timed task'>"
        + "    <task>"
        + "      <timer duedate='23 business seconds'>"
        + "        <action class='geftem-eu-shuppe-oender-ze-konte'/>"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      newTransaction();

      assertEquals(1, getTimerCount());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  @Test
  public void testTimerCancellationAtProcessEnd() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='s' />"
        + "  </start-state>"
        + "  <state name='s'>"
        + "    <event type='node-enter'>"
        + "      <create-timer duedate='3 seconds'>"
        + "        <action class='claim.you.are.Innocent' />"
        + "      </create-timer>"
        + "    </event>"
        + "    <transition to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      assertEquals(1, getTimerCount());
      processInstance.signal();
      
      newTransaction();

//      processJobs(12000);

      assertEquals(0, getTimerCount());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  @Test
  public void testFindTimersByName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='timed task' />"
        + "  </start-state>"
        + "  <task-node name='timed task'>"
        + "    <task name='find the hole in the market'>"
        + "      <timer name='reminder' duedate='23 business seconds'>"
        + "        <action class='geftem-eu-shuppe-oender-ze-konte'/>"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);

      List timersByName = session.createCriteria(Timer.class)
          .add(Restrictions.eq("name", "reminder"))
          .list();
      assertNotNull(timersByName);
      assertEquals(1, timersByName.size());

      Timer timer = (Timer) timersByName.get(0);
      assertEquals("geftem-eu-shuppe-oender-ze-konte", timer.getAction()
          .getActionDelegation()
          .getClassName());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  @Test
  public void testTimerRepeat() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer name='reminder' "
        + "           duedate='0 seconds'"
        + "           repeat='5 seconds' >"
        + "      <action class='is.idega.idegaweb.egov.bpm.scheduler.TimerDBTest$NoOp' />"
        + "    </timer>"
        + "    <transition to='b'/>"
        + "    <transition name='back' to='a'/>"
        + "  </state>"
        + "  <state name='b'/>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      // long before = System.currentTimeMillis();
      processInstance.signal();
      // long after = System.currentTimeMillis();
      jbpmContext.save(processInstance);

      newTransaction();

      Timer timer = (Timer) jobSession.getFirstAcquirableJob(null);
      
      assertNotNull(timer);
      Date date = timer.getDueDate();
      assertNotNull(date);
      // assertTrue(before <= date.getTime());
      // assertTrue(date.getTime() <= after);
      long origDueDate = date.getTime();

      timer.execute(jbpmContext);

      newTransaction();

      List timers = session.createQuery("from org.jbpm.job.Timer").list();
      assertEquals(1, timers.size());
      timer = (Timer) timers.get(0);

      assertEquals(origDueDate + 5000, timer.getDueDate().getTime());

      processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
      // before = System.currentTimeMillis();
      processInstance.signal("back");
      // after = System.currentTimeMillis();
      jbpmContext.save(processInstance);

      newTransaction();

      timer = (Timer) jobSession.getFirstAcquirableJob(null);
      assertNotNull(timer);
      date = timer.getDueDate();
      assertNotNull(date);
      // assertTrue(before <= date.getTime());
      // assertTrue(date.getTime() <= after);

      newTransaction();

      processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
      processInstance.signal();
      jbpmContext.save(processInstance);

      newTransaction();

      assertEquals(0, getTimerCount());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }

  }

  @Test
  public void testTimerUpdatingProcessVariables() throws Exception {
    // variable a will be a task instance local variable
    // variable b will be a process instance variable
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='wait for var updates'>"
        + "      <controller>"
        + "        <variable name ='a' />"
        + "      </controller>"
        + "      <timer name='update variables' "
        + "             duedate='0 seconds'"
        + "             repeat='5 seconds' >"
        + "        <action class='is.idega.idegaweb.egov.bpm.scheduler.TimerDBTest$UpdateVariables' />"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      ContextInstance contextInstance = processInstance.getContextInstance();
      contextInstance.setVariable("a", "value a");
      contextInstance.setVariable("b", "value b");
      processInstance.signal();
      jbpmContext.save(processInstance);

      newTransaction();

      Timer timer = (Timer) jobSession.getFirstAcquirableJob(null);
      timer.execute(jbpmContext);

      newTransaction();

      processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
      contextInstance = processInstance.getContextInstance();
      assertEquals("value a", contextInstance.getVariable("a"));
      assertEquals("value b updated", contextInstance.getVariable("b"));

      TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
          .getTaskInstances()
          .iterator()
          .next();
      assertEquals("value a updated", taskInstance.getVariable("a"));
      assertEquals("value b updated", taskInstance.getVariable("b"));
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public static class ConcurrentUpdateAction implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      executionContext.setVariable("a", "value a timer actioned updated");
    }
  }

  public static class NoOp implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      isNoOpExecuted = true;
    }
  }

  public static class UpdateVariables implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      executionContext.setVariable("a", "value a updated");
      executionContext.setVariable("b", "value b updated");
    }
  }
  
  public static class EnsureSingleTimerOnNode implements ActionHandler {

	    private static final long serialVersionUID = 1L;

	    public void execute(ExecutionContext executionContext) throws Exception {
	      SchedulerService schedulerService = executionContext.getJbpmContext().getServices().getSchedulerService();
	      Token token = null;
	      List<Token> tokens = executionContext.getProcessInstance().getRootToken().getChildrenAtNode(executionContext.getNode());
	      for(Token tok : tokens) {
	    	  if(!tok.equals(executionContext.getToken())) {
	    		  schedulerService.deleteTimersByName(executionContext.getNode().getName(), tok);
	    	  }
	      }
	    }
	  }
  
  @Test
  public void testTimerRepeatWithFork() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='fork' />"
        + "  </start-state>"
        + "  <fork name='fork'>"
		+ "    <transition to='timer' name='toTimer'></transition>"
		+ "	   <transition to='preTimer' name='toPreTimer'></transition>"
		+ "  </fork>"
        + "  <state name='preTimer'>"
        + "    <transition to='timer'/>"
        + "  </state>"
        + "  <state name='timer'>"
        + "    <event type='node-enter'>"
        + "      <action class='is.idega.idegaweb.egov.bpm.scheduler.TimerDBTest$EnsureSingleTimerOnNode' />"
        + "    </event>"
        + "    <timer duedate='5 seconds'>"
        + "      <action class='is.idega.idegaweb.egov.bpm.scheduler.TimerDBTest$NoOp' />"
        + "    </timer>"
        + "    <transition to='b'/>"
        + "    <transition name='back' to='a'/>"
        + "  </state>"
        + "  <state name='b'/>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);

      processInstance.signal();
     
      Collection<Token> tokens = processInstance.getRootToken().getChildren().values();
      assertEquals(2, tokens.size());
      
      Token timer = ((Token)tokens.toArray()[0]);
      Token preTimer = ((Token)tokens.toArray()[1]);
      
      assertEquals("toTimer", timer.getName());
      assertEquals("toPreTimer", preTimer.getName());
      
      assertEquals(1, getTimerCount());
      
      preTimer.signal();
      
      assertEquals(1, getTimerCount());
      
      preTimer.signal();
      assertEquals(0, getTimerCount());
      log.debug(processInstance.getRootToken().getName());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
