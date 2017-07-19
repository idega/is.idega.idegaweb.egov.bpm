package is.idega.idegaweb.egov.bpm.business;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Scope("request")
@Service(CaseListVariableCache.NAME)
public class CaseListVariableCache {

	public static final String NAME = "caseListVariableCache";

	private Map<String, Object> caches = new HashMap<String, Object>();

	public Boolean contains(String name){
		return this.caches.containsKey(name);
	}

	public Object getCache(String name){
		return this.caches.get(name);
	}

	public void addCache(String name, Object value){
		this.caches.put(name, value);
	}

}
