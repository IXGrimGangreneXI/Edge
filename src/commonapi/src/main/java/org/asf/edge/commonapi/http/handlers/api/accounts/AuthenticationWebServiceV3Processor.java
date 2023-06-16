package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.ProductRuleData;
import org.asf.edge.commonapi.xmls.auth.GuestLoginData;
import org.asf.edge.commonapi.xmls.auth.LoginStatusType;
import org.asf.edge.commonapi.xmls.auth.ParentLoginData;
import org.asf.edge.commonapi.xmls.auth.ParentLoginResponseData;

public class AuthenticationWebServiceV3Processor extends BaseApiHandler<EdgeCommonApiServer> {

	private AccountManager manager;
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

	@Function(allowedMethods = { "POST" })
	public void getRules(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void loginGuest(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle login request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Guest login
		String guestLoginData = req.getEncryptedValue("guestLoginData");
		GuestLoginData login = req.parseXmlValue(guestLoginData, GuestLoginData.class);

		// Find account

		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public void loginParent(FunctionInfo func) throws IOException {
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
			// Account not found
			invalidUserCallback(login.username, req);
			return;
		}

		// Retrieve ID
		String id = manager.getAccountID(login.username);
		if (!manager.accountExists(id)) {
			// ID does not exist
			invalidUserCallback(login.username, req);
			return;
		}

		// Password check
		if (!manager.verifyPassword(id, login.password)) {
			// Password incorrect
			invalidUserCallback(login.username, req);
			return;
		}

		// TODO
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
