package org.asf.edge.contentserver.xmls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ProductConfigData {

	public String contentServerURL;
	public String contentServerV2URL;
	public String contentServerV3URL;
	public String contentServerV4URL;

	public String analyticsServerURL;
	public String configurationServerURL;
	public String membershipServerURL;

	public String authenticationServerURL;
	public String authenticationServerV3URL;

	public String avatarWebServiceURL;
	public String messagingServerURL;

	public String achievementServerURL;
	public String achievementServerV2URL;

	public String itemStoreServerURL;
	public String missionServerURL;

	public String subscriptionServerURL;
	public String trackServerURL;
	public String scoreServerURL;

	public String ratingServerURL;
	public String ratingServerV2URL;

	public String localeServerURL;
	public String localeServerV2URL;

	public String userServerURL;

	public String messageServerURL;
	public String messageServerV2URL;
	public String messageServerV3URL;

	public String chatServerURL;

	public String challengeServerURL;
	public String inviteServerV2URL;

	public String registrationServerV3URL;
	public String registrationServerV4URL;

	public String pushNotificationURL;
	public String mobileStoreURL;

	public String groupServerURL;
	public String groupServerV2URL;

	public String paymentServerURL;
	public String paymentServerV2URL;

	public String prizeCodeServerURL;
	public String prizeCodeServerV2URL;

	public String calendarServerURL;

	public String tokenExpiredURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] manifests;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] rootURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] contentDataURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] dataURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] sceneURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] sharedDataURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] soundURL;

	@JacksonXmlElementWrapper(useWrapping = false)
	public String[] moviesURL;

	public long unityCacheSize;
	public boolean enablePlayfab;
	public String logEventServer;

}
