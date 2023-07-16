package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.ProductRuleData;
import org.asf.edge.commonapi.xmls.auth.CommonLoginInfo;
import org.asf.edge.commonapi.xmls.auth.GuestLoginData;
import org.asf.edge.commonapi.xmls.auth.LoginStatusType;
import org.asf.edge.commonapi.xmls.auth.ParentLoginData;
import org.asf.edge.commonapi.xmls.auth.ParentLoginResponseData;

public class AuthenticationWebServiceV3Processor extends EdgeWebService<EdgeCommonApiServer> {

	private static AccountManager manager;
	private static HashMap<String, Integer> usernameLock = new HashMap<String, Integer>();

	static {
		Thread th = new Thread(() -> {
			while (true) {
				HashMap<String, Integer> passswordLock;
				while (true) {
					try {
						passswordLock = new HashMap<String, Integer>(AuthenticationWebServiceV3Processor.usernameLock);
						break;
					} catch (ConcurrentModificationException e) {
					}
				}

				for (String pwd : passswordLock.keySet()) {
					if (passswordLock.get(pwd) - 1 <= 0) {
						AuthenticationWebServiceV3Processor.usernameLock.remove(pwd);
					} else {
						AuthenticationWebServiceV3Processor.usernameLock.put(pwd, passswordLock.get(pwd) - 1);
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
		th.setDaemon(true);
		th.start();
	}

	public AuthenticationWebServiceV3Processor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AuthenticationWebServiceV3Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v3/AuthenticationWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getRules(LegacyFunctionInfo func) throws IOException {
		// Handle rules request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Build rule payload
		ProductRuleData data = new ProductRuleData();
		data.globalKey = "Edge does not use the hash validation system as its overly complex and TLS is more than enough";
		data.productRules = new ProductRuleData.ProductRulesBlock();
		data.productRules.sslRules = new ProductRuleData.ProductRulesBlock.RuleBlock[0];
		data.productRules.responseHashValidationRules = new ProductRuleData.ProductRulesBlock.RuleBlock[0];

		// Encode to XML
		String xml = req.generateXmlValue("getProductRulesResponse", data);
		String encrypted = req.generateEncryptedResponse(xml);

		// Set response
		setResponseContent("text/xml", encrypted);
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void authenticateUser(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle login request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String password = req.getEncryptedValue("password");
		String username = req.getEncryptedValue("username");

		// Authenticate
		String id = manager.getAccountID(username);
		if (id == null) {
			// Return failure
			setResponseContent(req.generateXmlValue("boolean", false));
			return;
		}

		// Check password
		if (!manager.verifyPassword(id, password)) {
			// Return failure
			setResponseContent(req.generateXmlValue("boolean", false));
			return;
		}

		// Return success
		setResponseContent(req.generateXmlValue("boolean", true));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void loginGuest(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle login request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Guest login
		String guestLoginData = req.getEncryptedValue("guestLoginData");
		GuestLoginData login = req.parseXmlValue(guestLoginData, GuestLoginData.class);

		// Decrypt guest ID
		login.username = req.decryptString(login.username);

		// Verify guest id
		if (login.username.length() != 72 || !login.username.matches("^[A-Za-z0-9\\-]+")) {
			// Error
			ParentLoginResponseData resp = new ParentLoginResponseData();
			resp.status = LoginStatusType.GuestAccountNotFound;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
			return;
		}

		// Find account
		AccountObject guestAcc;
		if (manager.getGuestAccount(login.username) == null) {
			// Check terms
			if (login.userPolicy == null || !login.userPolicy.termsAndConditions || !login.userPolicy.privacyPolicy) {
				// Error
				ParentLoginResponseData resp = new ParentLoginResponseData();
				resp.userID = "00000000-0000-0000-0000-000000000000";
				resp.status = LoginStatusType.UserPolicyNotAccepted;
				setResponseContent("text/xml",
						req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
				return;
			}

			// Register guest account
			guestAcc = manager.registerGuestAccount(login.username);

			// Create default save
			if (guestAcc != null)
				guestAcc.createSave("guest_" + System.currentTimeMillis());
		} else
			guestAcc = manager.getGuestAccount(login.username);

		// Check guest acc
		if (guestAcc == null) {
			// Error
			ParentLoginResponseData resp = new ParentLoginResponseData();
			resp.status = LoginStatusType.GuestAccountNotFound;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
			return;
		}

		// Check if its really a guest account
		if (!guestAcc.isGuestAccount()) {
			// Error
			ParentLoginResponseData resp = new ParentLoginResponseData();
			resp.status = LoginStatusType.GuestAccountNotFound;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
			return;
		}

		// Update login time
		guestAcc.updateLastLoginTime();

		// Create session token
		SessionToken tkn = new SessionToken();
		tkn.accountID = guestAcc.getAccountID();
		tkn.lastLoginTime = guestAcc.getLastLoginTime();
		tkn.capabilities = new String[] { "api" };

		// Create response
		ParentLoginResponseData resp = new ParentLoginResponseData();
		resp.status = LoginStatusType.Success;
		resp.username = guestAcc.getUsername();
		resp.apiToken = getUtilities().encodeToken(tkn.toTokenString());
		resp.email = guestAcc.getAccountEmail();
		resp.sendActivationReminder = false;
		resp.childList = Stream.of(guestAcc.getSaveIDs()).map(t -> guestAcc.getSave(t)).map(t -> {
			CommonLoginInfo ch = new CommonLoginInfo();
			ch.userID = t.getSaveID();
			ch.username = t.getUsername();
			return ch;
		}).toArray(t -> new CommonLoginInfo[t]);
		guestAcc.ping(true);

		// Set response
		setResponseContent("text/xml", req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void loginParent(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle login request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// User/password initial login
		String parentLoginData = req.getEncryptedValue("parentLoginData");
		ParentLoginData login = req.parseXmlValue(parentLoginData, ParentLoginData.class);

		// Return null if the username is on cooldown
		if (usernameLock.containsKey(login.username.toLowerCase())) {
			// Locked
			ParentLoginResponseData resp = new ParentLoginResponseData();
			resp.status = LoginStatusType.InvalidUserName;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
			return;
		}

		// Find account
		if (!manager.isValidUsername(login.username)) {
			// Invalid username
			invalidUserCallback(login.username, req);
			return;
		}
		if (!manager.isUsernameTaken(login.username)) {
			// Log
			getServerInstance().getLogger().warn("Account login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + login.username + ": account not found");

			// Account not found
			invalidUserCallback(login.username, req);
			return;
		}

		// Retrieve ID
		String id = manager.getAccountID(login.username);
		if (!manager.accountExists(id)) {
			// Log
			getServerInstance().getLogger().warn("Account login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + id + ": account not found");

			// ID does not exist
			invalidUserCallback(login.username, req);
			return;
		}

		// Password check
		if (!manager.verifyPassword(id, login.password)) {
			// Log
			getServerInstance().getLogger().warn("Account login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + id + ": invalid password");

			// Password incorrect
			invalidUserCallback(login.username, req);
			return;
		}

		// Find account
		AccountObject acc = manager.getAccount(id);
		if (acc.isGuestAccount()) {
			// Log
			getServerInstance().getLogger().warn("Account login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + acc.getAccountID() + ": guest accounts may not be directly logged in on");

			// NO
			invalidUserCallback(login.username, req);
			return;
		}

		// Update login time
		acc.updateLastLoginTime();

		// Create session token
		SessionToken tkn = new SessionToken();
		tkn.accountID = acc.getAccountID();
		tkn.lastLoginTime = acc.getLastLoginTime();
		tkn.capabilities = new String[] { "api" };

		// Log
		getServerInstance().getLogger().info("Account login from IP " + func.getClient().getRemoteAddress() + " to "
				+ acc.getAccountID() + ": logged in as " + acc.getUsername());

		// Create response
		ParentLoginResponseData resp = new ParentLoginResponseData();
		resp.status = LoginStatusType.Success;
		resp.username = acc.getUsername();
		resp.apiToken = getUtilities().encodeToken(tkn.toTokenString());
		resp.email = acc.getAccountEmail();
		resp.sendActivationReminder = false; // TODO
		resp.childList = Stream.of(acc.getSaveIDs()).map(t -> acc.getSave(t)).map(t -> {
			CommonLoginInfo ch = new CommonLoginInfo();
			ch.userID = t.getSaveID();
			ch.username = t.getUsername();
			return ch;
		}).toArray(t -> new CommonLoginInfo[t]);
		acc.ping(true);

		// Set response
		setResponseContent("text/xml", req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
	}

	private void invalidUserCallback(String username, ServiceRequestInfo req) throws IOException {
		ParentLoginResponseData resp = new ParentLoginResponseData();
		resp.status = LoginStatusType.InvalidUserName;
		setResponseContent("text/xml", req.generateEncryptedResponse(req.generateXmlValue("ParentLoginInfo", resp)));
		usernameLock.put(username.toLowerCase(), 8);
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
		}
	}

}
