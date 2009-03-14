package is.idega.idegaweb.egov.bpm.process.business;

import is.idega.idegaweb.egov.bpm.cases.testbase.EgovBPMBaseTest;

import java.io.InputStream;
import java.util.Collection;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.IdegaJbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2009/03/14 12:01:16 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@SuppressWarnings("unchecked")
public final class FollowupResponsesProcessTest extends EgovBPMBaseTest {

	@Autowired
	private BPMContext bpmContext;
	
	void deployProcessDefinitions() throws Exception {

		JbpmContext jctx = bpmContext.createJbpmContext();
		InputStream is = null;
		
		try {
			ClassPathResource cpr = new ClassPathResource("/is/idega/idegaweb/egov/bpm/process/business/definition/followupresponse/processdefinition.xml", getClass());
			is = cpr.getInputStream();
			
			ProcessDefinition followupProcess = ProcessDefinition.parseXmlInputStream(is);
			jctx.deployProcessDefinition(followupProcess);
			
			ProcessDefinition superProcess = ProcessDefinition.parseXmlString(
						      "<process-definition name='super'>" +
						      "<task-node name=\"submitOwnerActionTaken\">" +
						      "		<task name='Task for subprocess'>" +
						      "			<controller>" +
						      "				<variable name='string_ownerKennitala' access='read,write,required'></variable>"+
						      "			</controller>" +
						      /*
						      "<assignment class='com.idega.jbpm.identity.JSONAssignmentHandler'>"+
						      "<expression>" +
						      "{taskAssignment: {roles: {role: [" +
						      "{roleName: \"bpm_handler\", accesses: {access: [read]}}," +
						      "{roleName: \"bpm_owner\", accesses: {access: [read, write]}, scope: PI, assignIdentities: {string_ [\"current_user\"]}}," +
						      "{roleName: \"bpm_invited\", accesses: {access: [read]}, scope: PI}" +
						      "]} }}" +
						      "</expression>" +
						      "</assignment>" +
						      */
						      "		</task>" +
						      "</task-node>" +
						      "<task-node name=\"submitOwnerActionTaken2\">" +
						      "		<task name='Task for subprocess 2'>" +
						      "			<controller>" +
						      "				<variable name='string_ownerKennitala' access='read,write,required'></variable>"+
						      "			</controller>" +
						      "		</task>" +
						      "</task-node>" +
						      "  <start-state>" +
						      "    <transition name='with subprocess' to='subprocess' />" +
						      "  </start-state>" +
						      "  <process-state name='subprocess'>" +
						      "    <sub-process name='followupResponses' binding='late' />" +
						      
						      "<variable name='tasksToSubprocess' access='read' mapped-name='followupTasks' />" +
						      "	<event type='node-enter'>" +
						      "		<script>"+
						      "			<expression>" +
						      "				tasks = new ArrayList(2);" +
						      "				task = executionContext.getTaskMgmtInstance().getTaskMgmtDefinition().getTask(\"Task for subprocess\");" +
						      "				bean = new com.idega.jbpm.invitation.AssignTasksForRolesUsersBean();" +
						      "				bean.setTask(task);" +
						      "				bean.setToken(token);" +
						      "				bean.setRoles(new String[] {\"bpm_handler\"});" +
						      "				tasks.add(bean);" +
						      "				task = executionContext.getTaskMgmtInstance().getTaskMgmtDefinition().getTask(\"Task for subprocess 2\");" +
						      "				bean = new com.idega.jbpm.invitation.AssignTasksForRolesUsersBean();" +
						      "				bean.setTask(task);" +
						      "				bean.setRoles(new String[] {\"bpm_owner\"});" +
						      "				bean.setToken(token);" +
						      "				tasks.add(bean);" +
						      "			</expression>" +
						      "			<variable name='tasksToSubprocess' access='write' mapped-name='tasks' />" +
						      "		</script>"+
						      "	</event>" +
						      "    <transition name='toEnd' to='pause' />" +
						      "  </process-state>" +
						      "<state name='pause'></state>" +
						      "<end-state name='end'></end-state>" +
						      "</process-definition>"
						    );
			jctx.deployProcessDefinition(superProcess);
			
			
		} finally {
			bpmContext.closeAndCommit(jctx);
			
			if(is != null)
				is.close();
		}
	}
	
	@Test
	public void testFollowup() throws Exception {
		
		if(true)
			return;

		deployProcessDefinitions();
		
		JbpmContext jbpmContext = bpmContext.createJbpmContext();
		Long piId = null; 
		
		try {
			ProcessInstance pi = jbpmContext.newProcessInstance("super");
			piId = pi.getId();
			System.out.println("superprocess ID="+pi.getId());
		    pi.signal("with subprocess");

		    Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getTaskInstances();
		    System.out.println("alltis="+tis);
		    
		    System.out.println("current node="+pi.getRootToken().getNode());
		    System.out.println("tokens="+pi.getRootToken());
		    Collection<TaskInstance> tis2 = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
		    System.out.println("unfinishedtis="+tis2);
		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
		
		jbpmContext = bpmContext.createJbpmContext();
		
		try {
			ProcessInstance pi = jbpmContext.getProcessInstance(piId);

		    Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getTaskInstances();
		    System.out.println("alltis="+tis);
		    
		    System.out.println("current node="+pi.getRootToken().getNode());
		    System.out.println("tokens="+pi.getRootToken());
		    Collection<TaskInstance> tis2 = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
		    System.out.println("unfinishedtis="+tis2);
			
		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
	    
	    		
	}
}