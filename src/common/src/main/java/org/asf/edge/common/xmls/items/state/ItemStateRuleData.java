package org.asf.edge.common.xmls.items.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStateRuleData {

	@JsonProperty("Criterias")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemStateCriteriaData[] criteria = new ItemStateCriteriaData[0];

	@JsonProperty("CompletionAction")
	public ItemStateCompletionAction completionAction;

}
