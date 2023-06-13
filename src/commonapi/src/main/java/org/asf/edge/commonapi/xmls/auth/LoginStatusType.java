package org.asf.edge.commonapi.xmls.auth;

public enum LoginStatusType
{
	Success,
	InvalidUserName,
	InvalidPassword,
	InvalidEmail,

	UserIsBanned,
	IPAddressBlocked,

	UserNameDoesNotExists,
	UserNameLengthMismatch,

	UserPolicyNotAccepted
}
