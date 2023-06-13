package org.asf.edge.commonapi.xmls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRuleData {

	public String globalKey;
	public ProductRulesBlock productRules;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProductRulesBlock {

		@JacksonXmlElementWrapper(useWrapping = false)
		public RuleBlock[] sslRules;

		@JacksonXmlElementWrapper(useWrapping = false)
		public RuleBlock[] responseHashValidationRules;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class RuleBlock {
			@JacksonXmlProperty(localName = "urlContains", isAttribute = true)
			public String urlContains;

			@JacksonXmlProperty(localName = "enableAll", isAttribute = true)
			public boolean enableAll;

			@JacksonXmlElementWrapper(useWrapping = false)
			public String[] enable;
		}

	}

}
