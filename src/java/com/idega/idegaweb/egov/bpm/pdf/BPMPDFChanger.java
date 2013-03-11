package com.idega.idegaweb.egov.bpm.pdf;

import java.util.Arrays;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.pdf.business.PDFChanger;
import com.idega.core.business.DefaultSpringBean;
import com.idega.util.ListUtil;
import com.idega.util.xml.XmlUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMPDFChanger extends DefaultSpringBean implements PDFChanger {

	@Override
	public Document getChangedDocument(Document doc) {
		if (doc == null)
			return doc;

		Element root = doc.getRootElement();
		List<String> elements = Arrays.asList("div", "span", "table");
		for (String element: elements) {
			List<Element> toDetach = getInvisibleElements(root, element);
			if (!ListUtil.isEmpty(toDetach))
				for (Element e: toDetach)
					e.detach();
		}
		List<Element> scripts = XmlUtil.getContentByXPath(root, "//script", Filters.element());
		if (!ListUtil.isEmpty(scripts))
			for (Element script: scripts)
				script.detach();

		return doc;
	}

	private List<Element> getInvisibleElements(Element root, String elementName) {
		return XmlUtil.getContentByXPath(root, "//" + elementName + "[@style='display:none;']", Filters.element());
	}

}
