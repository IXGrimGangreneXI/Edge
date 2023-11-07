package org.asf.edge.mmoserver.networking.channels.smartfox.system.handlers;

import java.io.IOException;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.mmoserver.EdgeMMOServer;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.events.players.PlayerAuthenticateEvent;
import org.asf.edge.mmoserver.events.players.PlayerJoinEvent;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundLoginResponsePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound.ServerboundLoginRequestPacket;
import org.asf.edge.mmoserver.networking.packets.PacketChannel;
import org.asf.edge.mmoserver.networking.packets.IPacketHandler;
import org.asf.edge.mmoserver.networking.sfs.SfsErrorCode;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventBus;
import org.bouncycastle.util.encoders.Base32;

public class LoginRequestHandler implements IPacketHandler<ServerboundLoginRequestPacket> {

	@Override
	public Class<ServerboundLoginRequestPacket> packetClass() {
		return ServerboundLoginRequestPacket.class;
	}

	@Override
	public boolean handle(ServerboundLoginRequestPacket packet, PacketChannel channel) throws IOException {
		// Retrieve objects
		SmartfoxClient client = channel.getClient();
		EdgeMMOServer server = client.getObject(EdgeMMOServer.class);
		Logger logger = server.getLogger();

		// Check existing player
		PlayerInfo pInfo = client.getObject(PlayerInfo.class);
		if (pInfo != null) {
			// Found existing player info, invalid request
			return false;
		}

		// Parse token
		String sessionTkn = new String(Base32.decode(packet.sessionToken.toUpperCase()), "UTF-8");

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(sessionTkn);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			ClientboundLoginResponsePacket resp = new ClientboundLoginResponsePacket();
			resp.hasError = true;
			resp.errorCode = SfsErrorCode.INVALID_USERNAME;
			resp.errorArgs = new String[] { "<token redacted>" };
			channel.sendPacket(packet);
			return true;
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);
		if (save == null) {
			// Error
			ClientboundLoginResponsePacket resp = new ClientboundLoginResponsePacket();
			resp.hasError = true;
			resp.errorCode = SfsErrorCode.INVALID_USERNAME;
			resp.errorArgs = new String[] { "<token redacted>" };
			channel.sendPacket(packet);
			logger.error("Account " + account.getAccountID() + " failed to connect to MMO server: save '" + tkn.saveID
					+ "' was not found");
			return true;
		}

		// Log
		logger.info("Account login from IP " + client.getRemoteAddress() + " to " + account.getAccountID()
				+ ": logging into MMO server as " + (account.isGuestAccount() ? "guest" : account.getUsername()));

		// Find zone
		GameZone zone = ZoneManager.getInstance().getZone(packet.zoneName);
		if (zone == null) {
			// Error
			ClientboundLoginResponsePacket resp = new ClientboundLoginResponsePacket();
			resp.hasError = true;
			resp.errorCode = SfsErrorCode.INVALID_ZONE;
			resp.errorArgs = new String[] { packet.zoneName };
			channel.sendPacket(packet);
			logger.error("Account " + account.getAccountID() + " failed to connect to MMO server: zone "
					+ packet.zoneName + " does not exist");
			return true;
		}

		// Check zone
		if (!zone.isActive()) {
			// Error
			ClientboundLoginResponsePacket resp = new ClientboundLoginResponsePacket();
			resp.hasError = true;
			resp.errorCode = SfsErrorCode.ZONE_INACTIVE;
			resp.errorArgs = new String[] { packet.zoneName };
			channel.sendPacket(packet);
			logger.error("Account " + account.getAccountID() + " failed to connect to MMO server: zone "
					+ packet.zoneName + " is inactive");
			return true;
		}

		// Moderation
		// TODO

		//
		// Authentication success!
		//

		// Create player info object
		PlayerInfo.PlayerDataController cont = PlayerInfo.createPlayerInfo(client, save, account, zone);
		PlayerInfo plr = cont.player;

		// Create response
		ClientboundLoginResponsePacket resp = new ClientboundLoginResponsePacket();
		resp.reconnectSeconds = 5;
		resp.zoneName = zone.getName();

		// Check permissions
		short priv = 1;
		if (account.isGuestAccount())
			priv = 0;
		switch (PermissionContext.getFor(account).getPermissionLevel()) {

		// Privilege IDs
		// 0 = guest
		// 1 = standard
		// 2 = moderator
		// 3 = admin

		case OPERATOR:
			priv = 3;
			break;
		case DEVELOPER:
			priv = 3;
			break;
		case ADMINISTRATOR:
			priv = 3;
			break;
		case MODERATOR:
			priv = 2;
			break;
		case TRIAL_MODERATOR:
			priv = 2;
			break;
		case PLAYER:
			priv = 1;
			break;
		case GUEST:
			priv = 0;
			break;

		}
		resp.privilegeID = priv;

		// Populate room info
		resp.roomList = zone.getAllRooms();

		// Dispatch authentication event
		PlayerAuthenticateEvent ev = new PlayerAuthenticateEvent(plr, zone, resp);
		EventBus.getInstance().dispatchEvent(ev);
		if (ev.hasFailed()) {
			// Failed
			logger.error("Account " + account.getAccountID()
					+ " failed to connect to MMO server: module rejected authentication attempt"
					+ (resp.hasError ? " with code " + ev.getLoginResponse().errorCode : ""));
			channel.sendPacket(resp);
			return true;
		}

		// Populate session details in response (doing this later to prevent modules
		// accessing token data)
		resp.userName = packet.sessionToken; // JumpStart why -_-
		resp.sessionNumericID = channel.getClient().getSessionNumericID();

		// Log success and dispatch event
		client.setObject(PlayerInfo.class, plr);
		logger.info("Account " + account.getAccountID() + " joined the MMO server as " + save.getUsername()
				+ " (save ID: " + save.getSaveID() + ")");
		EventBus.getInstance().dispatchEvent(new PlayerJoinEvent(plr));

		// Send response
		channel.sendPacket(resp);

		// Run completion
		cont.onJoinComplete.run();

		// Return
		return true;
	}

}
