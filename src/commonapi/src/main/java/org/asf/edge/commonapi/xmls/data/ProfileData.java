package org.asf.edge.commonapi.xmls.data;

import org.asf.edge.commonapi.xmls.auth.UserInfoData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ProfileData {

	public AvatarBlock avatar;

	@JsonProperty("ID")
	public String id;

	@JsonProperty("Ach")
	public int achievementCount;

	@JsonProperty("Mth")
	public int mythieCount;

	@JsonProperty("gc")
	public int coinCount;

	@JsonProperty("cc")
	public int gemCount;

	public int buddyCount;

	public int activityCount;

	public int rankID = 0;

	@JsonProperty("Groups")
	@JacksonXmlElementWrapper(useWrapping = false)
	public GroupBlock[] groups;

	@JsonProperty("Answer")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AnswerBlock answerData;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class AvatarBlock {

		public ObjectNode avatarData;

		public UserInfoData userInfo;

		@JsonProperty("UserSubscriptionInfo")
		public SubscriptionBlock subscription;

		public AchievementBlock achievementInfo;

		@JacksonXmlElementWrapper(useWrapping = false)
		public AchievementBlock[] achievements;

		@JacksonXmlElementWrapper(useWrapping = false)
		public RewardMultiplierBlock[] rewardMultipliers;

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class AchievementBlock {

			@JsonProperty("u")
			public String saveID;

			@JsonProperty("a")
			public int pointsTotal;

			@JsonProperty("r")
			public int rankID;

			@JsonProperty("p")
			public int pointTypeID;

		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class RewardMultiplierBlock {

			@JsonProperty("PT")
			public int typeID;

			@JsonProperty("MF")
			public int multiplierFactor;

			@JsonProperty("MET")
			public String expiryTime;

		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class SubscriptionBlock {

			@JsonProperty("ID")
			public String accountID;

			public int membershipID;

			// Not sure
			public int subscriptionID = -3;

			// 1 = member, 2 = non-member
			public int subscriptionTypeID = 2;

			// Not sure- 41 might be non-member
			public int subscriptionPlanID = 41;

			public String subscriptionDisplayName = "NonMember";

			public String lastBillDate;

			public boolean isActive;

			public boolean recurring;

			public float recurringAmount;

		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class GroupBlock {

		public int groupID;
		public int roleID;

		@JsonProperty("N")
		public String name;

		@JsonProperty("L")
		public String logo;

		@JsonProperty("C")
		public String color;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class AnswerBlock {

		@JsonProperty("ID")
		public String userID;

		@JacksonXmlElementWrapper(useWrapping = false)
		public AnswerDataBlock[] answers;

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class AnswerDataBlock {

			@JsonProperty("QID")
			public int questionID;

			@JsonProperty("AID")
			public int answerID;

		}

	}

}
