package is.idega.idegaweb.egov.bpm.scheduler;

import is.idega.idegaweb.egov.bpm.cases.testbase.EgovBPMBaseTest;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JobSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.IdegaJbpmContext;

public abstract class AbstractDbTestCase extends EgovBPMBaseTest {
	private static final Log log = LogFactory.getLog(AbstractDbTestCase.class);

	@Autowired
	private IdegaJbpmContext bpmContext;

	protected JbpmConfiguration jbpmConfiguration;

	protected JbpmContext jbpmContext;
	protected SchemaExport schemaExport;

	protected Session session;
	protected GraphSession graphSession;
	protected TaskMgmtSession taskMgmtSession;
	protected ContextSession contextSession;
	protected JobSession jobSession;
	protected LoggingSession loggingSession;

	protected JobExecutor jobExecutor;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		beginSessionTransaction();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		commitAndCloseSession();
//		ensureCleanDatabase();

		super.tearDown();
	}

//	private void ensureCleanDatabase() {
//		boolean hasLeftOvers = false;
//
//		DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) getJbpmConfiguration()
//				.getServiceFactory("persistence");
//		Configuration configuration = dbPersistenceServiceFactory
//				.getConfiguration();
//		JbpmSchema jbpmSchema = new JbpmSchema(configuration);
//
//		Map jbpmTablesRecordCount = jbpmSchema.getJbpmTablesRecordCount();
//		Iterator iter = jbpmTablesRecordCount.entrySet().iterator();
//		while (iter.hasNext()) {
//			Map.Entry entry = (Map.Entry) iter.next();
//			String tableName = (String) entry.getKey();
//			Integer count = (Integer) entry.getValue();
//
//			if ((count == null) || (count != 0)) {
//				hasLeftOvers = true;
//				// [JBPM-1812] Fix tests that don't cleanup the database
//				// System.err.println("FIXME: " + getClass().getName() + "." +
//				// getName() + " left " + count + " records in " + tableName);
//			}
//		}
//
//		if (hasLeftOvers) {
//			// TODO: JBPM-1781
//			// jbpmSchema.cleanSchema();
//		}
//	}

	public void beginSessionTransaction() {
		createJbpmContext();
		initializeMembers();
	}

	public void commitAndCloseSession() {
		closeJbpmContext();
		resetMembers();
	}

	protected void newTransaction() {
		commitAndCloseSession();
		beginSessionTransaction();
	}

	public ProcessInstance saveAndReload(ProcessInstance pi) {
		jbpmContext.save(pi);
		
		newTransaction();
		return graphSession.loadProcessInstance(pi.getId());
	}

	public TaskInstance saveAndReload(TaskInstance taskInstance) {
		jbpmContext.save(taskInstance);
		newTransaction();
		return (TaskInstance) session.load(TaskInstance.class, new Long(
				taskInstance.getId()));
	}

	public ProcessDefinition saveAndReload(ProcessDefinition pd) {
		graphSession.saveProcessDefinition(pd);
		newTransaction();
		return graphSession.loadProcessDefinition(pd.getId());
	}

	public ProcessLog saveAndReload(ProcessLog processLog) {
		loggingSession.saveProcessLog(processLog);
		newTransaction();
		return loggingSession.loadProcessLog(processLog.getId());
	}

//	protected void createSchema() {
//		getJbpmConfiguration().createSchema();
//	}
//
//	protected void cleanSchema() {
//		getJbpmConfiguration().cleanSchema();
//	}
//
//	protected void dropSchema() {
//		getJbpmConfiguration().dropSchema();
//	}

//	protected String getJbpmTestConfig() {
//		return "org/jbpm/db/jbpm.db.test.cfg.xml";
//	}
//
//	protected JbpmConfiguration getJbpmConfiguration() {
//		if (jbpmConfiguration == null) {
//			String jbpmTestConfiguration = getJbpmTestConfig();
//			jbpmConfiguration = JbpmContext.getCurrentJbpmContext()
//					.getJbpmConfiguration();
//		}
//		return jbpmConfiguration;
//	}

	protected void createJbpmContext() {
		jbpmContext = bpmContext.createJbpmContext();
	}

	protected void closeJbpmContext() {
		if (jbpmContext != null) {
			jbpmContext.close();
			jbpmContext = null;
		}
	}

	protected void startJobExecutor() {
		JbpmContext jbpmContext = bpmContext.createJbpmContext();
		jobExecutor = jbpmContext.getJbpmConfiguration().getJobExecutor();
		jobExecutor.start();
	}

	protected void waitForJobs(long timeout) {
		// install a timer that will interrupt if it takes too long
		// if that happens, it will lead to an interrupted exception and the
		// test
		// will fail
		TimerTask interruptTask = new TimerTask() {
			Thread testThread = Thread.currentThread();

			@Override
			public void run() {
				log.debug("test " + getName()
						+ " took too long. going to interrupt...");
				testThread.interrupt();
			}
		};
		Timer timer = new Timer();
		timer.schedule(interruptTask, timeout);

		try {
			while (getNbrOfJobsAvailable() > 0) {
				log
						.debug("going to sleep for 200 millis, waiting for the job executor to process more jobs");
				Thread.sleep(200);
			}
		} catch (InterruptedException e) {
			fail("test execution exceeded treshold of " + timeout
					+ " milliseconds");
		} finally {
			timer.cancel();
		}
	}

	protected int getNbrOfJobsAvailable() {
		if (session != null) {
			return getNbrOfJobsAvailable(session);
		} else {
			beginSessionTransaction();
			try {
				return getNbrOfJobsAvailable(session);
			} finally {
				commitAndCloseSession();
			}
		}
	}

	private int getNbrOfJobsAvailable(Session session) {
		int nbrOfJobsAvailable = 0;
		Number jobs = (Number) session.createQuery(
				"select count(*) from org.jbpm.job.Job").uniqueResult();
		log.debug("there are " + jobs + " jobs in the database");
		if (jobs != null) {
			nbrOfJobsAvailable = jobs.intValue();
		}
		return nbrOfJobsAvailable;
	}

	protected int getTimerCount() {
		Number timerCount = (Number) session.createQuery(
				"select count(*) from org.jbpm.job.Timer").uniqueResult();
		log.debug("there are " + timerCount + " timers in the database");
		return timerCount.intValue();
	}

	protected Job getJob() {
		return (Job) session.createQuery("from org.jbpm.job.Job")
				.uniqueResult();
	}

	public void processJobs(long maxWait) {
		commitAndCloseSession();
		startJobExecutor();
		try {
			waitForJobs(maxWait);
		} finally {
			stopJobExecutor();
			beginSessionTransaction();
		}
	}

	protected void stopJobExecutor() {
		if (jobExecutor != null) {
			try {
				jobExecutor.stopAndJoin();
			} catch (InterruptedException e) {
				throw new RuntimeException(
						"waiting for job executor to stop and join got interrupted",
						e);
			}
		}
	}

	protected void initializeMembers() {
		session = jbpmContext.getSession();
		graphSession = jbpmContext.getGraphSession();
		taskMgmtSession = jbpmContext.getTaskMgmtSession();
		loggingSession = jbpmContext.getLoggingSession();
		jobSession = jbpmContext.getJobSession();
		contextSession = jbpmContext.getContextSession();
	}

	protected void resetMembers() {
		session = null;
		graphSession = null;
		taskMgmtSession = null;
		loggingSession = null;
		jobSession = null;
		contextSession = null;
	}
}
