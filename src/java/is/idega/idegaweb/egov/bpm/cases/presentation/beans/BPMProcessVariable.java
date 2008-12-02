package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class BPMProcessVariable {
	
	public static final List<String> DATE_TYPES = Collections.unmodifiableList(Arrays.asList("D"));
	public static final List<String> DOUBLE_TYPES = Collections.unmodifiableList(Arrays.asList("O"));
	public static final List<String> LONG_TYPES = Collections.unmodifiableList(Arrays.asList("H", "L"));
	public static final List<String> STRING_TYPES = Collections.unmodifiableList(Arrays.asList("I", "S"));
	
	private String name;
	private String value;
	private String type;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public boolean isTypeOf(List<String> types) {
		return (ListUtil.isEmpty(types) || StringUtil.isEmpty(getType())) ? false : types.contains(getType());
	}
	
	public boolean isDateType() {
		return isTypeOf(DATE_TYPES);
	}
	
	public boolean isDoubleType() {
		return isTypeOf(DOUBLE_TYPES);
	}
	
	public boolean isLongType() {
		return isTypeOf(LONG_TYPES);
	}
	
	public boolean isStringType() {
		return isTypeOf(STRING_TYPES);
	}
}
