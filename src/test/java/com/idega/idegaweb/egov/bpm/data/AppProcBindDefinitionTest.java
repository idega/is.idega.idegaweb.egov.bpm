package com.idega.idegaweb.egov.bpm.data;

import is.idega.idegaweb.egov.bpm.cases.testbase.EgovBPMBaseTest;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.module.def.ModuleDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.IdegaJbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 *          Last modified: $Date: 2009/03/14 12:01:16 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public final class AppProcBindDefinitionTest extends EgovBPMBaseTest {

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Test
	public void testAPBDefinitionAddition() throws Exception {
		
		if(true)
			return;

		JbpmContext jbpmContext = bpmContext.createJbpmContext();
		Long pdId;

		try {

			ProcessDefinition somePD = ProcessDefinition
					.createNewProcessDefinition();
			somePD.setName("x");

			AppProcBindDefinition apbd = new AppProcBindDefinition();
			somePD.addDefinition(apbd);

			jbpmContext.deployProcessDefinition(somePD);
			pdId = somePD.getId();

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}

		jbpmContext = bpmContext.createJbpmContext();

		try {

			ProcessDefinition somePD = jbpmContext.getGraphSession()
					.getProcessDefinition(pdId);

			ModuleDefinition def = somePD
					.getDefinition(AppProcBindDefinition.class);

			// printDefinitions(somePD);
			assertNotNull(def);
			assertEquals(AppProcBindDefinition.class, def.getClass());

			// System.out.println("def="+def.getClass().getName());

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
	}

	@Test
	public void testApplicationRolesSupportMgmt() throws Exception {
		
		if(true)
			return;

		JbpmContext jbpmContext = bpmContext.createJbpmContext();
		Long pdId;

		String roleKey = "role1";
		Integer appId = 1;

		try {
			ProcessDefinition somePD = ProcessDefinition
					.createNewProcessDefinition();
			somePD.setName("x");

			AppProcBindDefinition apbd = new AppProcBindDefinition();
			somePD.addDefinition(apbd);

			jbpmContext.deployProcessDefinition(somePD);
			pdId = somePD.getId();

			AppSupports as = new AppSupports();
			as.setApplicationId(appId);
			as.setProcessName(somePD.getName());
			as.setRoleKey(roleKey);

			casesBPMDAO.persist(as);

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}

		jbpmContext = bpmContext.createJbpmContext();

		try {
			ProcessDefinition somePD = jbpmContext.getGraphSession()
					.getProcessDefinition(pdId);

			AppProcBindDefinition def = (AppProcBindDefinition) somePD
					.getDefinition(AppProcBindDefinition.class);

			List<AppSupports> sups = def.getAppSupports(appId);

			assertNotNull(def);
			assertNotNull(sups);
			assertFalse(sups.isEmpty());

			for (AppSupports sup : sups) {

				assertEquals(roleKey, sup.getRoleKey());
			}

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
	}

	void printDefinitions(ProcessDefinition pd) {

		@SuppressWarnings("unchecked")
		Map<String, ModuleDefinition> defs = pd.getDefinitions();

		for (Entry<String, ModuleDefinition> entry : defs.entrySet()) {

			System.out.println("_____Class=" + entry.getKey());
			System.out.println("_____Deefinition=" + entry.getValue());
		}
	}
}