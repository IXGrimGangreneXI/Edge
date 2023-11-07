package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.common.webservices.annotations.LegacyFunction;
import org.asf.edge.common.webservices.annotations.LegacyFunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.auth.CommonLoginInfo;
import org.asf.edge.commonapi.xmls.auth.LoginStatusType;
import org.asf.edge.commonapi.xmls.auth.ParentLoginResponseData;
import org.asf.edge.commonapi.xmls.auth.RegistrationResultData;
import org.asf.edge.commonapi.xmls.registration.ParentRegistrationData;

import com.google.gson.JsonPrimitive;

public class RegistrationWebServiceV3Processor extends EdgeWebService<EdgeCommonApiServer> {

	private static AccountManager manager;
	public static final int COST_DELETE_PROFILE = 50;

	public RegistrationWebServiceV3Processor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new RegistrationWebServiceV3Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v3/RegistrationWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void deleteProfile(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle deletion request
		SodRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}
		String userID = req.payload.get("userID");

		// Find save
		AccountSaveContainer save = account.getSave(userID);
		if (save == null) {
			// Error, set response

			// Status PROFILE_NOT_FOUND = not found
			setResponseContent("text/xml", req.generateXmlValue("DeleteProfileStatus", "PROFILE_NOT_FOUND"));
			return;
		}

		// Check currency
		AccountKvDataContainer currencyAccWide = account.getAccountKeyValueContainer().getChildContainer("currency");
		int current = 0;
		if (currencyAccWide.entryExists("gems"))
			current = currencyAccWide.getEntry("gems").getAsInt();
		if (current < COST_DELETE_PROFILE) {
			// Error, set response

			// Status IN_SUFFICIENT_VCASH_FUNDS = not enough gems
			setResponseContent("text/xml", req.generateXmlValue("DeleteProfileStatus", "IN_SUFFICIENT_VCASH_FUNDS"));
			return;
		}

		// Remove gems
		current -= COST_DELETE_PROFILE;
		currencyAccWide.setEntry("gems", new JsonPrimitive(current));

		// Delete save
		save.deleteSave();

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("DeleteProfileStatus", "SUCCESS"));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void registerParent(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle registration request
		SodRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Registration
		String registrationData = req.getEncryptedValue("parentRegistrationData");
		ParentRegistrationData registration = req.parseXmlValue(registrationData, ParentRegistrationData.class);

		// Check policy
		if (registration.userPolicy == null || !registration.userPolicy.termsAndConditions
				|| !registration.userPolicy.privacyPolicy) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.UserPolicyNotAccepted;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check data
		if (registration.childList == null || registration.childList.length < 1) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.NoChildData;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check username validity
		if (!manager.isValidUsername(registration.childList[0].childName)) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.InvalidUserName;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check filters
		if (TextFilterService.getInstance().isFiltered(registration.childList[0].childName, true)) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.InvalidUserName;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check password validity
		if (!manager.isValidPassword(registration.password)) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.InvalidPassword;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check guest
		String guestID = null;
		if (registration.childList.length >= 2) {
			if (!registration.childList[1].isGuest) {
				// Error
				RegistrationResultData resp = new RegistrationResultData();
				resp.status = LoginStatusType.InvalidGuestChildUserName;
				setResponseContent("text/xml",
						req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
				return;
			} else {
				// Check guest account settings
				registration.childList[1].guestUserName = req.decryptString(registration.childList[1].guestUserName);
				guestID = registration.childList[1].guestUserName;
				if (guestID.length() != 72 || !guestID.matches("^[A-Za-z0-9\\-]+")) {
					// Error
					RegistrationResultData resp = new RegistrationResultData();
					resp.status = LoginStatusType.InvalidGuestChildUserName;
					setResponseContent("text/xml",
							req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
					return;
				} else if (manager.getGuestAccount(guestID) == null) {
					// Error
					RegistrationResultData resp = new RegistrationResultData();
					resp.status = LoginStatusType.GuestAccountNotFound;
					setResponseContent("text/xml",
							req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
					return;
				}
			}
		}

		// Verify email
		if (!registration.email.toLowerCase().matches(
				"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.InvalidEmail;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check if email is taken
		if (manager.getAccountIDByEmail(registration.email) != null) {
			// Error
			RegistrationResultData resp = new RegistrationResultData();
			resp.status = LoginStatusType.DuplicateEmail;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check if name is taken
		if (guestID != null) {
			// Check guest account profiles
			AccountObject guestAcc = manager.getGuestAccount(guestID);
			boolean nameFound = false;
			for (String id : guestAcc.getSaveIDs()) {
				if (guestAcc.getSave(id).getUsername().equalsIgnoreCase(registration.childList[0].childName)) {
					nameFound = true;
					break;
				}
			}
			if (!nameFound) {
				if (manager.isUsernameTaken(registration.childList[0].childName)) {
					// Error
					RegistrationResultData resp = new RegistrationResultData();
					resp.status = LoginStatusType.DuplicateUserName;
					setResponseContent("text/xml",
							req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
					return;
				}
			}
		} else {
			if (manager.isUsernameTaken(registration.childList[0].childName)) {
				// Error
				RegistrationResultData resp = new RegistrationResultData();
				resp.status = LoginStatusType.DuplicateUserName;
				setResponseContent("text/xml",
						req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
				return;
			}
		}

		// Find guest account
		AccountObject acc;
		if (guestID != null) {
			// Update guest account instead
			AccountObject guestAcc = manager.getGuestAccount(guestID);
			if (guestAcc == null) {
				// Error
				RegistrationResultData resp = new RegistrationResultData();
				resp.status = LoginStatusType.GuestAccountNotFound;
				setResponseContent("text/xml",
						req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
				return;
			}

			// Migrate
			if (!guestAcc.migrateToNormalAccountFromGuest(registration.childList[0].childName, registration.email,
					registration.password.toCharArray())) {
				// Error
				RegistrationResultData resp = new RegistrationResultData();
				resp.status = LoginStatusType.GuestAccountNotFound;
				setResponseContent("text/xml",
						req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
				return;
			}

			// Log
			getServerInstance().getLogger()
					.info("Account registration from IP " + func.getClient().getRemoteAddress() + ": registered "
							+ guestAcc.getAccountID() + " (username: " + guestAcc.getUsername()
							+ ", migrated guest account " + guestID + ")");

			// Set data
			AccountKvDataContainer cont = guestAcc.getAccountKeyValueContainer().getChildContainer("accountdata");
			cont.setEntry("sendupdates", new JsonPrimitive(registration.emailNotification == 1));
			cont.setEntry("isunderage", new JsonPrimitive(registration.childList[0].age < 13));
			if (registration.childList[0].age < 13) {
				guestAcc.setChatEnabled(false);
				guestAcc.setStrictChatFilterEnabled(true);
			}
			acc = guestAcc;
		} else {
			// Create new account
			acc = manager.registerAccount(registration.childList[0].childName, registration.email,
					registration.password.toCharArray());
			if (acc == null) {
				// Error
				RegistrationResultData resp = new RegistrationResultData();
				resp.status = LoginStatusType.InvalidUserName;
				setResponseContent("text/xml",
						req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
				return;
			}

			// Log
			getServerInstance().getLogger().info("Account registration from IP " + func.getClient().getRemoteAddress()
					+ ": registered " + acc.getAccountID() + " (username: " + acc.getUsername() + ")");

			// Set data
			AccountKvDataContainer cont = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
			cont.setEntry("sendupdates", new JsonPrimitive(registration.emailNotification == 1));
			cont.setEntry("isunderage", new JsonPrimitive(registration.childList[0].age < 13));
			if (registration.childList[0].age < 13) {
				acc.setChatEnabled(false);
				acc.setStrictChatFilterEnabled(true);
			}
		}

		// Build response
		RegistrationResultData resp = new RegistrationResultData();
		resp.status = LoginStatusType.Success;
		resp.userID = acc.getAccountID();

		// Add gems
		AccountKvDataContainer currencyAccWide = acc.getAccountKeyValueContainer().getChildContainer("currency");
		int current = 0;
		if (currencyAccWide.entryExists("gems"))
			current = currencyAccWide.getEntry("gems").getAsInt();
		currencyAccWide.setEntry("gems", new JsonPrimitive(current + 75));

		// Build token
		SessionToken tkn = new SessionToken();
		acc.updateLastLoginTime();
		tkn.accountID = acc.getAccountID();
		tkn.lastLoginTime = acc.getLastLoginTime();
		tkn.capabilities = new String[] { "api" };

		// Build parent login info object
		ParentLoginResponseData l = new ParentLoginResponseData();
		l.status = LoginStatusType.Success;
		l.username = acc.getUsername();
		l.apiToken = getUtilities().encodeToken(tkn.toTokenString());
		l.email = acc.getAccountEmail();
		l.sendActivationReminder = false;
		l.childList = Stream.of(acc.getSaveIDs()).map(t -> acc.getSave(t)).map(t -> {
			CommonLoginInfo ch = new CommonLoginInfo();
			ch.userID = t.getSaveID();
			ch.username = t.getUsername();
			return ch;
		}).toArray(t -> new CommonLoginInfo[t]);

		// Add fields
		resp.apiToken = tkn.toTokenString();
		resp.parentLoginInfo = req.generateXmlValue("ParentLoginInfo", resp);

		// Set response
		setResponseContent("text/xml", req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
	}

}
