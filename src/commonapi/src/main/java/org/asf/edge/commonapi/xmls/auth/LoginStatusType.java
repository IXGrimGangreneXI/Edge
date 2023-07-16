package org.asf.edge.commonapi.xmls.auth;

public enum LoginStatusType
{
	Success,
	InvalidUserName,
	InvalidPassword,
	InvalidEmail,
	
	UsernameRestriction,
	UsernameContainsBadWord,
	NoChildData,
	
	GuestAccountNotFound,
	InvalidGuestChildUserName,
	
	InvalidChildUserName,

	UserIsBanned,
	IPAddressBlocked,

	DuplicateUserName,
	DuplicateEmail,

	UserPolicyNotAccepted
}
