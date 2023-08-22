package org.asf.edge.modules.gridapi.http.services.phoenix;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.*;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.identities.PropertyInfo;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;
import org.asf.edge.modules.gridapi.utils.TokenUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PhoenixIdentityWebService extends EdgeWebService<EdgeGridApiServer> {

	public PhoenixIdentityWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new PhoenixIdentityWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/identities";
	}

	@Function(value = "create", allowedMethods = { "POST" })
	public FunctionResult createIdentity(FunctionInfo func) throws NoSuchAlgorithmException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "idgen");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Parse payload
		if (!payload.has("properties") || !payload.has("name") || !payload.get("properties").isJsonObject()) {
			return response(400, "Bad Request");
		}

		// Preprare response
		JsonObject resp = new JsonObject();

		// Create identity
		HashMap<String, PropertyInfo> props = new HashMap<String, PropertyInfo>();
		props.put("name", new PropertyInfo() {
			{
				value = payload.get("name").getAsString();
				isReadonly = true;
			}
		});
		if (payload.has("displayName"))
			props.put("displayName", new PropertyInfo() {
				{
					value = payload.get("displayName").getAsString();
					isReadonly = false;
				}
			});
		else
			props.put("displayName", new PropertyInfo() {
				{
					value = payload.get("name").getAsString();
					isReadonly = false;
				}
			});
		JsonObject propPayload = payload.getAsJsonObject("properties").getAsJsonObject();
		for (String key : propPayload.keySet()) {
			if (!propPayload.get(key).isJsonObject())
				return response(400, "Bad Request");
			JsonObject obj = propPayload.get(key).getAsJsonObject();
			if (!obj.has("isReadonly") || !obj.has("value")) {
				return response(400, "Bad Request");
			}
			try {
				if (props.containsKey(key))
					return response(400, "Bad Request");
				PropertyInfo info = new PropertyInfo();
				info.isReadonly = obj.get("isReadonly").getAsBoolean();
				info.value = obj.get("value").getAsString();
				props.put(key, info);
			} catch (Exception e) {
				return response(400, "Bad Request");
			}
		}
		IdentityDef id = IdentityUtils.createIdentity(props);

		// Add information to response
		resp.addProperty("identity", id.identity);
		resp.addProperty("lastUpdateTime", id.lastUpdateTime);
		JsonObject propertyMap = new JsonObject();
		id.properties.forEach((key, data) -> {
			if (key.equals("serverCertificateProperties"))
				return;
			JsonObject obj = new JsonObject();
			obj.addProperty("isReadonly", data.isReadonly);
			obj.addProperty("value", data.value);
			propertyMap.add(key, obj);
		});
		resp.add("properties", propertyMap);

		// Set response
		return ok("application/json", resp.toString());
	}

	@Function(value = "pull", allowSubPaths = true)
	public FunctionResult getIdentity(FunctionInfo func) throws NoSuchAlgorithmException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "idget");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Find target identity
		if (func.getRequestedPath().split("/").length != 2)
			return response(400, "Bad Request");
		String targetId = func.getRequestedPath().split("/")[1];

		// Find identity
		IdentityDef id = IdentityUtils.getIdentity(targetId);
		if (id == null)
			return response(404, "Not found");

		// Preprare response
		JsonObject resp = new JsonObject();

		// Add information to response
		resp.addProperty("identity", id.identity);
		resp.addProperty("lastUpdateTime", id.lastUpdateTime);
		JsonObject propertyMap = new JsonObject();
		id.properties.forEach((key, data) -> {
			if (key.equals("serverCertificateProperties"))
				return;
			JsonObject obj = new JsonObject();
			obj.addProperty("isReadonly", data.isReadonly);
			obj.addProperty("value", data.value);
			propertyMap.add(key, obj);
		});
		resp.add("properties", propertyMap);

		// Set response
		return ok("application/json", resp.toString());
	}

	@Function(value = "pullcurrent")
	public FunctionResult getCurrentIdentity(FunctionInfo func) throws NoSuchAlgorithmException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func);
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Find identity
		IdentityDef id = ctx.identity;

		// Preprare response
		JsonObject resp = new JsonObject();

		// Add information to response
		resp.addProperty("identity", id.identity);
		resp.addProperty("lastUpdateTime", id.lastUpdateTime);
		JsonObject propertyMap = new JsonObject();
		id.properties.forEach((key, data) -> {
			if (key.equals("serverCertificateProperties"))
				return;
			JsonObject obj = new JsonObject();
			obj.addProperty("isReadonly", data.isReadonly);
			obj.addProperty("value", data.value);
			propertyMap.add(key, obj);
		});
		resp.add("properties", propertyMap);

		// Set response
		return ok("application/json", resp.toString());
	}

	@Function(value = "list")
	public FunctionResult listIdentities(FunctionInfo func) throws NoSuchAlgorithmException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "idlist");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Preprare response
		JsonObject resp = new JsonObject();

		// Build list
		JsonArray identities = new JsonArray();
		for (IdentityDef id : IdentityUtils.getAll()) {
			JsonObject identity = new JsonObject();
			identity.addProperty("uuid", id.identity);
			if (id.properties.containsKey("name"))
				identity.addProperty("name", id.properties.get("name").value);
			else
				identity.addProperty("name", "UNDEFINED");
			if (id.properties.containsKey("displayName"))
				identity.addProperty("displayName", id.properties.get("displayName").value);
			else
				identity.addProperty("displayName", "UNDEFINED");
			identities.add(identity);
		}

		// Add information to response
		resp.addProperty("count", identities.size());
		resp.add("identities", identities);

		// Set response
		return ok("application/json", resp.toString());
	}

	@Function(value = "delete", allowedMethods = { "POST" }, allowSubPaths = true)
	public FunctionResult deleteIdentity(FunctionInfo func) throws NoSuchAlgorithmException {
		// Check authentication header
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "iddel");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Find target identity
		if (func.getRequestedPath().split("/").length != 2)
			return response(400, "Bad Request");
		String targetId = func.getRequestedPath().split("/")[1];

		// Find identity
		IdentityDef id = IdentityUtils.getIdentity(targetId);
		if (id == null)
			return response(404, "Not found");

		// Preprare response
		JsonObject resp = new JsonObject();

		// Delete
		IdentityUtils.deleteIdentity(id.identity);

		// Add information to response
		resp.addProperty("deleted", true);
		resp.addProperty("identity", id.identity);
		resp.addProperty("deletedAt", System.currentTimeMillis());

		// Set response
		return ok("application/json", resp.toString());
	}

	@Function(value = "update", allowedMethods = { "POST" })
	public FunctionResult updateIdentity(FunctionInfo func) throws NoSuchAlgorithmException {
		// Check authentication header
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "idupdate");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Verify payload
		if (!payload.has("identity") || !payload.has("properties") || !payload.get("properties").isJsonObject()) {
			return response(400, "Bad Request");
		}

		// Find target identity
		String targetId = payload.get("identity").getAsString();

		// Find identity
		IdentityDef id = IdentityUtils.getIdentity(targetId);
		if (id == null) {
			return response(404, "Not found");
		}

		// Apply changes
		HashMap<String, String> properties = new HashMap<String, String>();
		JsonObject props = payload.get("properties").getAsJsonObject();
		for (String key : props.keySet()) {
			try {
				properties.put(key, props.get(key).getAsString());
			} catch (Exception e) {
				return response(400, "Bad Request");
			}
		}
		if (!IdentityUtils.updateIdentity(id.identity, properties)) {
			return response(400, "Bad Request");
		}
		id = IdentityUtils.getIdentity(id.identity);

		// Preprare response
		JsonObject resp = new JsonObject();

		// Add information to response
		resp.addProperty("identity", id.identity);
		resp.addProperty("lastUpdateTime", id.lastUpdateTime);
		JsonObject propertyMap = new JsonObject();
		id.properties.forEach((key, data) -> {
			JsonObject obj = new JsonObject();
			obj.addProperty("isReadonly", data.isReadonly);
			obj.addProperty("value", data.value);
			propertyMap.add(key, obj);
		});
		resp.add("properties", propertyMap);

		// Set response
		return ok("application/json", resp.toString());
	}

}
