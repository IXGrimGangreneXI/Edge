package org.asf.edge.common.xmls.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MessageInfoData {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "http://api.jumpstart.com/";

	@JsonProperty("UserMessageQueueID")
	@JsonInclude(Include.NON_DEFAULT)
	public int messageQueueID = -1;

	@JsonProperty("FromUserID")
	public String fromUser;

	@JsonProperty("MessageTypeID")
	@JsonInclude(Include.NON_DEFAULT)
	public int typeID = -1;

	@JsonProperty("MessageTypeName")
	@JsonInclude(Include.NON_NULL)
	public String typeName;

	@JsonProperty("MemberMessage")
	public String messageContentMembers;

	@JsonProperty("NonMemberMessage")
	public String messageContentNonMembers;

	@JsonProperty("MemberLinkUrl")
	public String linkUrlMembers;

	@JsonProperty("NonMemberLinkUrl")
	public String linkUrlNonMembers;

	@JsonProperty("MemberAudioUrl")
	public String audioUrlMembers;

	@JsonProperty("NonMemberAudioUrl")
	public String audioUrlNonMembers;

	@JsonProperty("Data")
	public String data;

}
