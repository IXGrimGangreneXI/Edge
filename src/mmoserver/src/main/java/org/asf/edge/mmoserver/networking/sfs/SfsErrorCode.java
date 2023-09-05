package org.asf.edge.mmoserver.networking.sfs;

/**
 * 
 * Smartfox-compatible error codes
 * 
 * @author Sky Swimmer
 * 
 */
public enum SfsErrorCode {

	/**
	 * Obsolete API version error message <br/>
	 * <br/>
	 * Requires parameters: [0] = API version, [1] = expected version
	 */
	INVALID_API((short) 0),

	/**
	 * Invalid zone name <br/>
	 * <br/>
	 * Requires parameters: [0] = zone name
	 */
	INVALID_ZONE((short) 1),

	/**
	 * Invalid user name <br/>
	 * <br/>
	 * Requires parameters: [0] = username
	 */
	INVALID_USERNAME((short) 2),

	/**
	 * Invalid user password <br/>
	 * <br/>
	 * Requires parameters: [0] = username
	 */
	INVALID_PASSWORD((short) 3),

	/**
	 * User is banned <br/>
	 * <br/>
	 * Requires parameters: [0] = username
	 */
	USER_BANNED((short) 4),

	/**
	 * Zone is full <br/>
	 * <br/>
	 * Requires parameters: [0] = zone name
	 */
	ZONE_FULL((short) 5),

	/**
	 * User already logged into zone <br/>
	 * <br/>
	 * Requires parameters: [0] = username, [1] = zone name
	 */
	ALREADY_LOGGED_INTO_ZONE((short) 6),

	/**
	 * Server full
	 */
	SERVER_FULL((short) 7),

	/**
	 * Zone inactive<br/>
	 * <br/>
	 * Requires parameters: [0] = zone name
	 */
	ZONE_INACTIVE((short) 8),

	/**
	 * Inappropriate usernames <br/>
	 * <br/>
	 * Requires parameters: [0] = username, [1] = what was filtered
	 * 
	 */
	USERNAME_INAPPROPRIATE((short) 9),

	/**
	 * Guest not allowed in zone <br/>
	 * <br/>
	 * Requires parameters: [0] = zone name
	 */
	GUEST_DENIED_IN_ZONE((short) 10),

	/**
	 * IP banned<br/>
	 * <br/>
	 * Requires parameters: [0] = address
	 */
	IP_BANNED((short) 11),

	/**
	 * Room already exists<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_EXISTS((short) 12),

	/**
	 * Group unavailable<br/>
	 * <br/>
	 * Requires parameters: [0] = room name, [1] = group name
	 */
	GROUP_UNAVAILABLE((short) 13),

	/**
	 * Invalid room name length<br/>
	 * <br/>
	 * Requires parameters: [0] = room name, [1] = group name
	 */
	BAD_ROOM_NAME_LENGTH((short) 14),

	/**
	 * Inappropriate room name<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	INAPPRPRIATE_ROOM_NAME((short) 15),

	/**
	 * Too many rooms in zone
	 */
	TOO_MANY_ROOMS_IN_ZONE((short) 16),

	/**
	 * Too many rooms created in the session<br/>
	 * <br/>
	 * Requires parameters: [0] = limit
	 */
	EXCEEDED_ROOM_SESSION_LIMIT((short) 17),

	/**
	 * Room creation failure due to incorrect parameter<br/>
	 * <br/>
	 * Requires parameters: [0] = room parameter name
	 */
	ROOM_CREATION_FAILED((short) 18),

	/**
	 * Already in room<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_ALREADY_JOINED((short) 19),

	/**
	 * Room full<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_FULL((short) 20),

	/**
	 * Invalid room password<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	INVALID_ROOM_PASSWORD((short) 21),

	/**
	 * Room not found
	 */
	ROOM_NOT_FOUND((short) 22),

	/**
	 * Room locked<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_LOCKED((short) 23),

	/**
	 * Already subscribed to group<br/>
	 * <br/>
	 * Requires parameters: [0] = group name
	 */
	GROUP_ALREADY_SUBSCRIBED((short) 24),

	/**
	 * Group not found<br/>
	 * <br/>
	 * Requires parameters: [0] = group name
	 */
	GROUP_NOT_FOUND((short) 25),

	/**
	 * Not subscribed to group<br/>
	 * <br/>
	 * Requires parameters: [0] = group name
	 */
	GROUP_NOT_SUBSCRIBED((short) 26),

	/**
	 * Generic error<br/>
	 * <br/>
	 * Requires parameters: [0] = error message
	 */
	GENERIC((short) 28),

	/**
	 * Room cannot be renamed<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_RENAME_DENIED((short) 29),

	/**
	 * Room cannot have its password changed<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_PASSWORD_CHANGE_DENIED((short) 30),

	/**
	 * Room cannot have its capacity changed<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	ROOM_CAPACITY_CHANGE_DENIED((short) 31),

	/**
	 * No player slots available in room<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	SWITCH_FAILED_NO_PLAYER_SLOTS_AVAILABLE((short) 32),

	/**
	 * No spectator slots available in room<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	SWITCH_FAILED_NO_SPECTATOR_SLOTS_AVAILABLE((short) 33),

	/**
	 * The requested room is not a game room<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	SWITCH_FAILED_NON_GAME_ROOM((short) 34),

	/**
	 * The user is not in the room<br/>
	 * <br/>
	 * Requires parameters: [0] = room name
	 */
	SWITCH_FAILED_NOT_JOINED_IN_ROOM((short) 35),

	/**
	 * Buddy list load error<br/>
	 * <br/>
	 * Requires parameters: [0] = error message
	 */
	BUDDY_LIST_ERROR((short) 36),

	/**
	 * Buddy list full<br/>
	 * <br/>
	 * Requires parameters: [0] = buddy list size
	 */
	BUDDY_LIST_FULL((short) 37),

	/**
	 * Too many buddy variables<br/>
	 * <br/>
	 * Requires parameters: [0] = limit
	 */
	TOO_MANY_BUDDY_VARIABLES((short) 39),

	/**
	 * Game join failed due to missing access criteria<br/>
	 * <br/>
	 * Requires parameters: [0] = game name
	 */
	GAME_ACCESS_DENIED((short) 40),

	/**
	 * No matching rooms for quick join
	 */
	QUICK_JOIN_FAILED_NO_MATCHING_ROOMS((short) 41),

	/**
	 * Invalid reply
	 */
	INVITE_REPLY_INVALID((short) 42);

	private short value;

	public static SfsErrorCode fromShort(short value) {
		for (SfsErrorCode c : values()) {
			if (c.value == value)
				return c;
		}
		return SfsErrorCode.GENERIC;
	}

	public short toShort() {
		return value;
	}

	SfsErrorCode(short value) {
		this.value = value;
	}

}
