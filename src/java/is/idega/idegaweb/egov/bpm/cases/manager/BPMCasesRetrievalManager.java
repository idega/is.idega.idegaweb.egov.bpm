/**
 * @(#)BPMCasesRetrievalManager.java    1.0.0 4:03:16 PM
 *
 * Idega Software hf. Source Code Licence Agreement x
 *
 * This agreement, made this 10th of February 2006 by and between 
 * Idega Software hf., a business formed and operating under laws 
 * of Iceland, having its principal place of business in Reykjavik, 
 * Iceland, hereinafter after referred to as "Manufacturer" and Agura 
 * IT hereinafter referred to as "Licensee".
 * 1.  License Grant: Upon completion of this agreement, the source 
 *     code that may be made available according to the documentation for 
 *     a particular software product (Software) from Manufacturer 
 *     (Source Code) shall be provided to Licensee, provided that 
 *     (1) funds have been received for payment of the License for Software and 
 *     (2) the appropriate License has been purchased as stated in the 
 *     documentation for Software. As used in this License Agreement, 
 *     Licensee shall also mean the individual using or installing 
 *     the source code together with any individual or entity, including 
 *     but not limited to your employer, on whose behalf you are acting 
 *     in using or installing the Source Code. By completing this agreement, 
 *     Licensee agrees to be bound by the terms and conditions of this Source 
 *     Code License Agreement. This Source Code License Agreement shall 
 *     be an extension of the Software License Agreement for the associated 
 *     product. No additional amendment or modification shall be made 
 *     to this Agreement except in writing signed by Licensee and 
 *     Manufacturer. This Agreement is effective indefinitely and once
 *     completed, cannot be terminated. Manufacturer hereby grants to 
 *     Licensee a non-transferable, worldwide license during the term of 
 *     this Agreement to use the Source Code for the associated product 
 *     purchased. In the event the Software License Agreement to the 
 *     associated product is terminated; (1) Licensee's rights to use 
 *     the Source Code are revoked and (2) Licensee shall destroy all 
 *     copies of the Source Code including any Source Code used in 
 *     Licensee's applications.
 * 2.  License Limitations
 *     2.1 Licensee may not resell, rent, lease or distribute the 
 *         Source Code alone, it shall only be distributed as a 
 *         compiled component of an application.
 *     2.2 Licensee shall protect and keep secure all Source Code 
 *         provided by this this Source Code License Agreement. 
 *         All Source Code provided by this Agreement that is used 
 *         with an application that is distributed or accessible outside
 *         Licensee's organization (including use from the Internet), 
 *         must be protected to the extent that it cannot be easily 
 *         extracted or decompiled.
 *     2.3 The Licensee shall not resell, rent, lease or distribute 
 *         the products created from the Source Code in any way that 
 *         would compete with Idega Software.
 *     2.4 Manufacturer's copyright notices may not be removed from 
 *         the Source Code.
 *     2.5 All modifications on the source code by Licencee must 
 *         be submitted to or provided to Manufacturer.
 * 3.  Copyright: Manufacturer's source code is copyrighted and contains 
 *     proprietary information. Licensee shall not distribute or 
 *     reveal the Source Code to anyone other than the software 
 *     developers of Licensee's organization. Licensee may be held 
 *     legally responsible for any infringement of intellectual property 
 *     rights that is caused or encouraged by Licensee's failure to abide 
 *     by the terms of this Agreement. Licensee may make copies of the 
 *     Source Code provided the copyright and trademark notices are 
 *     reproduced in their entirety on the copy. Manufacturer reserves 
 *     all rights not specifically granted to Licensee.
 *
 * 4.  Warranty & Risks: Although efforts have been made to assure that the 
 *     Source Code is correct, reliable, date compliant, and technically 
 *     accurate, the Source Code is licensed to Licensee as is and without 
 *     warranties as to performance of merchantability, fitness for a 
 *     particular purpose or use, or any other warranties whether 
 *     expressed or implied. Licensee's organization and all users 
 *     of the source code assume all risks when using it. The manufacturers, 
 *     distributors and resellers of the Source Code shall not be liable 
 *     for any consequential, incidental, punitive or special damages 
 *     arising out of the use of or inability to use the source code or 
 *     the provision of or failure to provide support services, even if we 
 *     have been advised of the possibility of such damages. In any case, 
 *     the entire liability under any provision of this agreement shall be 
 *     limited to the greater of the amount actually paid by Licensee for the 
 *     Software or 5.00 USD. No returns will be provided for the associated 
 *     License that was purchased to become eligible to receive the Source 
 *     Code after Licensee receives the source code. 
 */
package is.idega.idegaweb.egov.bpm.cases.manager;

import is.idega.idegaweb.egov.application.data.Application;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.context.ApplicationListener;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.core.accesscontrol.data.ICRole;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMEmailDocument;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.user.data.User;

/**
 * <p>Extension to {@link CasesRetrievalManager}</p>
 * <p>You can report about problems to: 
 * <a href="mailto:martynas@idega.is">Martynas Stakė</a></p>
 *
 * @version 1.0.0 May 16, 2013
 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
 */
public interface BPMCasesRetrievalManager extends CasesRetrievalManager, ApplicationListener{
	/**
	 * 
	 * @param processInstances where to search connected {@link User}s,
	 * not <code>null</code>;
	 * @return {@link User} connected to process or 
	 * {@link Collections#emptyList()} on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<User> getConnectedUsers(
			Collection<ProcessInstanceW> processInstances);
	
	/**
	 * 
	 * @param theCase where users should be found, not <code>null</code>;
	 * @return list of {@link User}, who can see or modify {@link Case} or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<User> getConnectedUsers(Case theCase);
	
	/**
	 * 
	 * @param processInstances where to search {@link BPMEmailDocument},
	 * not <code>null</code>;
	 * @param owner is {@link User}, who can see these 
	 * {@link BPMEmailDocument}s, not <code>null</code>;
	 * @return {@link BPMEmailDocument}s or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<BPMEmailDocument> getBPMEmailDocuments(
			Collection<ProcessInstanceW> processInstances,
			User owner);
	
	/**
	 * 
	 * @param processInstances where to search {@link Task}s, not
	 * <code>null</code>;
	 * @param owner who can see {@link Task}s, not <code>null</code>;
	 * @param locale to which {@link BPMDocument}s should be translated, 
	 * uses current {@link Locale} when <code>null</code>;
	 * @return {@link BPMDocument}s for {@link User} or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<BPMDocument> getTaskBPMDocuments(
			Collection<ProcessInstanceW> processInstances,
			User owner, Locale locale);
	
	/**
	 * 
	 * @param processInstances where to search for documents, not
	 * <code>null</code>;
	 * @param owner is {@link User} who can see submitted 
	 * {@link BPMDocument}s, not <code>null</code>;
	 * @param locale in which documents should be translated, 
	 * current locale will be used if <code>null</code>;
	 * @return submitted {@link BPMDocument}s or 
	 * {@link Collections#emptyList()} on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<BPMDocument> getSubmittedBPMDocuments(
			Collection<ProcessInstanceW> processInstances,
			User owner, Locale locale);
	
	/**
	 * 
	 * @param applicationPrimaryKey is {@link Application#getPrimaryKey()},
	 * not <code>null</code>;
	 * @return {@link List} of {@link ProcessInstanceW} or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessInstanceW> getProcessInstancesW(Object applicationPrimaryKey);
	
	/**
	 * 
	 * @param application to get {@link ProcessInstanceW}s for, 
	 * not <code>null</code>;
	 * @return {@link List} of {@link ProcessInstanceW} or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessInstanceW> getProcessInstancesW(Application application);
	
	/**
	 * 
	 * @param processDefinitionName is {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return {@link List} of {@link ProcessInstanceW} or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessInstanceW> getProcessInstancesW(String processDefinitionName);
	
	/**
	 * 
	 * @param theCase for which process instance should be found, 
	 * not <code>null</code>;
	 * @return {@link ProcessInstanceW} of {@link Case} or <code>null</code>
	 * on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public ProcessInstanceW getProcessInstancesW(Case theCase);
	
	/**
	 * 
	 * @param processDefinitionName is {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return {@link ProcessInstance}s by given {@link ProcessDefinition} 
	 * name or {@link Collections#emptyList()} on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessInstance> getProcessInstances(String processDefinitionName);
	
	/**
	 * 
	 * @param processDefinitionName is {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return {@link List} of {@link ProcessInstance#getId()} by given 
	 * {@link ProcessDefinition} name or {@link Collections#emptyList()} 
	 * on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<Long> getProcessInstancesIDs(String processDefinitionName);
	
	/**
	 * 
	 * @param processDefinitionName is {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return all {@link ProcessDefinition}s by given name or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessDefinition> getProcessDefinitions(String processDefinitionName);
	
	/**
	 * 
	 * @param processDefinitionName is {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return all id's of {@link ProcessDefinition}s by given name or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<Long> getProcessDefinitionsIDs(String processDefinitionName);
	
	/**
	 * 
	 * @param application is application, which {@link ProcessDefinition}s are
	 * required, not <code>null</code>;
	 * not <code>null</code>;
	 * @return all {@link ProcessDefinition}s by given name or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessDefinition> getProcessDefinitions(Application application);
	
	/**
	 * 
	 * @param application of which process definition IDs is required, not
	 * <code>null</code>;
	 * @return all id's of {@link ProcessDefinition}s by given name or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<Long> getProcessDefinitionsIDs(Application application);
	
	/**
	 * 
	 * @param applicationPrimaryKey is {@link Application#getPrimaryKey()} 
	 * of which {@link ProcessDefinition}s are required, not
	 * <code>null</code>;
	 * @return all {@link ProcessDefinition}s by given name or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessDefinition> getProcessDefinitions(Object applicationPrimaryKey);
	
	/**
	 * 
	 * @param applicationPrimaryKey is {@link Application#getPrimaryKey()} 
	 * of which process definition IDs is required, not
	 * <code>null</code>;
	 * @return all id's of {@link ProcessDefinition}s by given name or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<Long> getProcessDefinitionsIDs(Object applicationPrimaryKey);

	/**
	 * <p>Only PROC_CASE</p>
	 * @param processDefinitionNames is {@link Collection} of 
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @return array of {@link Case#getPrimaryKey()} by criteria or 
	 * <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public String[] getCasesPrimaryKeys(Collection<String> processDefinitionNames, Collection<String> caseStatuses);

	/**
	 * <p>Only PROC_CASE</p>
	 * @param processDefinitionNames is {@link Collection} of 
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @param subscribers is {@link Collection} of {@link User}, who
	 * is subscribed "{@link Case#addSubscriber(User)}". If <code>null</code>
	 * then this option will be skipped;
	 * @return array of {@link Case#getPrimaryKey()} by criteria or 
	 * <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public String[] getCasesPrimaryKeys(
			Collection<String> processDefinitionNames, 
			Collection<String> caseStatuses, 
			Collection<User> subscribers);

	/**
	 * <p>Only PROC_CASE</p>
	 * @param processDefinitionNames is {@link Collection} of 
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @param subscribers is {@link Collection} of {@link User}, who
	 * is subscribed "{@link Case#addSubscriber(User)}". If <code>null</code>
	 * then this option will be skipped;
	 * @param caseManagerTypes is {@link Collection} of 
	 * {@link Case#getCaseManagerType()}, if <code>null</code> then option
	 * will be skipped;
	 * @return array of {@link Case#getPrimaryKey()} by criteria or 
	 * <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public String[] getCasesPrimaryKeys(
			Collection<String> processDefinitionNames,
			Collection<String> caseStatuses, 
			Collection<User> subscribers,
			Collection<String> caseManagerTypes);
	
	/**
	 * <p>Only PROC_CASE</p>
	 * @param processDefinitionNames is {@link Collection} of 
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @return {@link List} of {@link Case}s by criteria or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public List<Case> getCases(Collection<String> processDefinitionNames,
			Collection<String> caseStatuses);

	/**
	 * <p>Only PROC_CASE</p>
	 * @param processDefinitionNames is {@link Collection} of 
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @param subscribers is {@link Collection} of {@link User}, who
	 * is subscribed "{@link Case#addSubscriber(User)}". If <code>null</code>
	 * then this option will be skipped;
	 * @return {@link List} of {@link Case}s by criteria or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public List<Case> getCases(Collection<String> processDefinitionNames,
			Collection<String> caseStatuses, Collection<User> subscribers);

	/**
	 * <p>Only PROC_CASE</p>
	 * @param processDefinitionNames is {@link Collection} of 
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @param subscribers is {@link Collection} of {@link User}, who
	 * is subscribed "{@link Case#addSubscriber(User)}". If <code>null</code>
	 * then this option will be skipped;
	 * @param caseManagerTypes is {@link Collection} of 
	 * {@link Case#getCaseManagerType()}, if <code>null</code> then option
	 * will be skipped;
	 * @return {@link List} of {@link Case}s by criteria or 
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public List<Case> getCases(
			Collection<String> processDefinitionNames,
			Collection<String> caseStatuses, 
			Collection<User> subscribers,
			Collection<String> caseManagerTypes);

	/**
	 * 
	 * @param caseIdentifiers is {@link Collection} of 
	 * {@link Case#getCaseIdentifier()}, to search role names for, 
	 * not <code>null</code>;
	 * @return {@link ICRole}s for {@link Case}s, which has managerRoleName
	 * permissions or <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public String[] getManagerRoleNames(Collection<String> caseIdentifiers);

	/**
	 * 
	 * <p>Checks if current {@link User} has cases manager access for at least
	 * one of give {@link Case#getCaseIdentifier()}.</p>
	 * @param caseIdentifiers is {@link Case#getCaseIdentifier()} to check
	 * form managing possibility, not <code>null</code>;
	 * @return <code>true</code> if current {@link User} has cases manager
	 * access to at least one of {@link Case}s or <code>false</code>
	 * if all of them are not visible to {@link User};
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public boolean hasManagerAccess(Collection<String> caseIdentifiers);
}
