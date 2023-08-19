package org.asf.edge.common.services.leaderboard.impl;

import java.util.HashMap;

import org.asf.edge.common.services.leaderboard.Leaderboard;
import org.asf.edge.common.services.leaderboard.LeaderboardManager;

public class LeaderboardManagerImpl extends LeaderboardManager {

	private static HashMap<String, Leaderboard> leaderboards = new HashMap<String, Leaderboard>();

	@Override
	public void initService() {
	}

	@Override
	public void registerLeaderboard(String name) {
		// Trigger load
		if (!leaderboards.containsKey(name.toUpperCase()))
			leaderboards.put(name, new LeaderboardImpl(name.toUpperCase()));
	}

	@Override
	public Leaderboard getLeaderboard(String name) {
		// Load if needed
		if (!leaderboards.containsKey(name.toUpperCase()))
			registerLeaderboard(name.toUpperCase());
		return leaderboards.get(name.toUpperCase());
	}

}
