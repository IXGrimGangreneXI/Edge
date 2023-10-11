package org.asf.edge.mmoserver.networking.channels.smartfox.system.handlers;

import java.io.IOException;
import java.util.stream.Stream;

import org.asf.edge.mmoserver.networking.packets.PacketChannel;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.smartfox.SystemChannel;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundMessagePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound.ServerboundMessagePacket;
import org.asf.edge.mmoserver.networking.packets.IPacketHandler;

public class MessageHandler implements IPacketHandler<ServerboundMessagePacket> {

	@Override
	public Class<ServerboundMessagePacket> packetClass() {
		return ServerboundMessagePacket.class;
	}

	@Override
	public boolean handle(ServerboundMessagePacket packet, PacketChannel channel) throws IOException {
		// Find sender data
		SfsUser senderData = null;
		PlayerInfo plr = channel.getClient().getObject(PlayerInfo.class);
		if (plr != null) {
			for (RoomInfo r : plr.getJoinedRooms()) {
				SfsUser usr = r.getSfsUser(plr.getSave().getSaveID());
				if (usr != null) {
					senderData = usr;
					break;
				}
			}
			if (senderData == null) {
				for (RoomInfo r : plr.getSpectatingRooms()) {
					SfsUser usr = r.getSfsUser(plr.getSave().getSaveID());
					if (usr != null) {
						senderData = usr;
						break;
					}
				}
			}
		}

		// Handle type
		switch (packet.type) {

		// Public
		case 0: {
			// Broadcast

			// Create packet
			ClientboundMessagePacket pkt = new ClientboundMessagePacket();
			pkt.type = 0;
			pkt.roomID = packet.roomID;
			pkt.sender = channel.getClient().getSessionNumericID();
			pkt.message = packet.message;
			pkt.parameters = packet.parameters;

			// Broadcast to users
			for (SmartfoxClient cl : channel.getClient().getServer().getClients()) {
				// Check if not the source player
				PlayerInfo plr2 = cl.getObject(PlayerInfo.class);
				if (plr2 != null && cl != channel.getClient()) {
					// Check room
					if (Stream.of(plr2.getJoinedRooms()).anyMatch(t -> t.getRoomID() == packet.roomID)
							|| Stream.of(plr2.getSpectatingRooms()).anyMatch(t -> t.getRoomID() == packet.roomID)) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(pkt);
					}
				}
			}

			break;
		}

		// Private
		case 1: {
			// To specific user

			// Create packet
			ClientboundMessagePacket pkt = new ClientboundMessagePacket();
			pkt.type = 1;
			pkt.sender = channel.getClient().getSessionNumericID();
			pkt.senderData = senderData;
			pkt.message = packet.message;
			pkt.parameters = packet.parameters;

			// Find recipient
			int recipientID = packet.recipient;
			SmartfoxClient target = channel.getClient().getServer().getClientByNumericID(recipientID);

			// Send if possible
			if (target != null)
				target.getChannel(SystemChannel.class).sendPacket(pkt);

			break;
		}

		// Moderator & admin messages
		case 2:
		case 3: {
			// To moderators/admins

			// Create packet
			ClientboundMessagePacket pkt = new ClientboundMessagePacket();
			pkt.type = packet.type;
			pkt.sender = channel.getClient().getSessionNumericID();
			pkt.senderData = senderData;
			pkt.message = packet.message;
			pkt.parameters = packet.parameters;

			// Handle mode
			switch (packet.recipientMode) {

			// User
			case 0: {
				// Find user
				SmartfoxClient target = channel.getClient().getServer().getClientByNumericID(packet.recipient);
				if (target != null) {
					// Check player
					PlayerInfo targetPlayer = target.getObject(PlayerInfo.class);
					if (targetPlayer != null) {
						PermissionContext perms = PermissionContext.getFor(targetPlayer.getAccount());

						// Verify permissions
						if (pkt.type == 2) {
							// Moderator
							if (!perms.hasPermission("mmo.servicemessages.moderator", PermissionLevel.MODERATOR))
								break; // Deny
						} else {
							// Admin
							if (!perms.hasPermission("mmo.servicemessages.admin", PermissionLevel.ADMINISTRATOR))
								break; // Deny
						}

						// Send
						target.getChannel(SystemChannel.class).sendPacket(pkt);
					}
				}
				break;
			}

			// Room
			case 1: {
				// Find room
				RoomInfo targetRoom = null;
				for (GameZone zone : ZoneManager.getInstance().getZones()) {
					RoomInfo room = zone.getRoomByID(packet.recipient);
					if (room != null) {
						targetRoom = room;
						break;
					}
				}
				if (targetRoom != null) {
					// For all clients
					for (SmartfoxClient cl : channel.getClient().getServer().getClients()) {
						// Check player
						PlayerInfo targetPlayer = cl.getObject(PlayerInfo.class);
						if (targetPlayer != null) {
							PermissionContext perms = PermissionContext.getFor(targetPlayer.getAccount());

							// Verify permissions
							if (pkt.type == 2) {
								// Moderator
								if (!perms.hasPermission("mmo.servicemessages.moderator", PermissionLevel.MODERATOR))
									break; // Deny
							} else {
								// Admin
								if (!perms.hasPermission("mmo.servicemessages.admin", PermissionLevel.ADMINISTRATOR))
									break; // Deny
							}

							// Send
							cl.getChannel(SystemChannel.class).sendPacket(pkt);
						}
					}
				}
				break;
			}

			// Zone
			case 2: {
				break;
			}

			}

			break;
		}

		// Buddy
		case 5: {
			// FIXME: implement
			packet.message = packet.message;
			break;
		}

		}
		return true;
	}

}
