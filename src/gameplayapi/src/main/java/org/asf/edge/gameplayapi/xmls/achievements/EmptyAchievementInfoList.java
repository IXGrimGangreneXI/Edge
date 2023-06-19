package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class EmptyAchievementInfoList {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "http://api.jumpstart.com/";

	@JacksonXmlProperty(localName = "xmlns:xsi", isAttribute = true)
	public String ns = "http://www.w3.org/2001/XMLSchema-instance";

	@JacksonXmlProperty(localName = "xsi:nil", isAttribute = true)
	public String nil = "true";

}
