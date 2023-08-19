package org.asf.edge.common.services.minigamedata.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.edge.common.entities.minigamedata.MinigameData;
import org.asf.edge.common.entities.minigamedata.MinigameDataRequest;
import org.asf.edge.common.entities.minigamedata.MinigameSaveRequest;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.minigamedata.MinigameDataManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;

public class MinigameDataManagerImpl extends MinigameDataManager {

	private CommonDataContainer globalData;

	public static class MinigameDataContainer {
		public String userID;

		public long timePlayed;
		public int timesWon;
		public int timesLost;

		public HashMap<String, Integer> data = new HashMap<String, Integer>();
	}

	@Override
	public void initService() {
		// Retrieve container
		globalData = CommonDataManager.getInstance().getContainer("MINIGAMEDATA");
	}

	@Override
	public void deleteDataFor(String saveID) {
		try {
			// Delete from all minigame saves
			globalData.getChildContainer("save-" + saveID).deleteContainer();

			// Delete data from user
			AccountSaveContainer sv = AccountManager.getInstance().getSaveByID(saveID);
			if (sv != null) {
				sv.getSaveData().getChildContainer("minigamedata").deleteContainer();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void saveGameData(AccountSaveContainer save, int gameId, MinigameSaveRequest saveRequest) {
		try {
			// Retrieve container
			AccountDataContainer data = save.getSaveData().getChildContainer("minigamedata");

			// Load old data
			MinigameDataContainer old = null;
			ObjectMapper mapper = new ObjectMapper();
			if (data.entryExists("minigame-" + gameId + "-" + saveRequest.gameLevel)) {
				old = mapper.readValue(data.getEntry("minigame-" + gameId + "-" + saveRequest.gameLevel).toString(),
						MinigameDataContainer.class);
			}

			// Create if needed
			if (old == null)
				old = new MinigameDataContainer();

			// Add information
			old.userID = save.getSaveID();
			for (String key : saveRequest.data.keySet()) {
				if (!key.equals("highscore") || !old.data.containsKey(key)
						|| old.data.get(key) < saveRequest.data.get(key))
					saveRequest.data.put(key, saveRequest.data.get(key));
			}
			old.timePlayed = System.currentTimeMillis();
			if (saveRequest.isLoss)
				old.timesLost++;
			if (saveRequest.isWin)
				old.timesWon++;

			// Save
			data.setEntry("minigame-" + gameId + "-" + saveRequest.gameLevel,
					JsonParser.parseString(mapper.writeValueAsString(old)));

			// Save to global data
			CommonDataContainer dataG = globalData.getChildContainer("save-" + save.getSaveID());
			dataG.setEntry("minigame-" + gameId + "-" + saveRequest.gameLevel,
					JsonParser.parseString(mapper.writeValueAsString(old)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public MinigameData getGameDataOf(AccountSaveContainer save, int gameId, MinigameDataRequest dataRequest) {
		try {
			// Create result
			MinigameData res = new MinigameData();
			res.userID = save.getSaveID();

			// Retrieve container
			AccountDataContainer data = save.getSaveData().getChildContainer("minigamedata");

			// Load data
			MinigameDataContainer c = null;
			ObjectMapper mapper = new ObjectMapper();
			if (data.entryExists("minigame-" + gameId + "-" + dataRequest.gameLevel)) {
				c = mapper.readValue(data.getEntry("minigame-" + gameId + "-" + dataRequest.gameLevel).toString(),
						MinigameDataContainer.class);
			}
			res.timePlayed = c.timePlayed;
			res.timesLost = c.timesLost;
			res.timesWon = c.timesWon;
			res.value = c.data.getOrDefault(dataRequest.key, 0);

			// Return
			return res;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MinigameData[] getAllGameData(String requestingID, int gameId, MinigameDataRequest dataRequest) {
		try {
			// Create result
			ArrayList<MinigameData> lst = new ArrayList<MinigameData>();

			// Check mode
			if (dataRequest.friendsOnly) {
//				// Sort and apply limit
//				ArrayList<MinigameData> l = new ArrayList<MinigameData>();
//				for (MinigameData data : lst.stream().sorted((t1, t2) -> -Integer.compare(t1.value, t2.value))
//						.toArray(t -> new MinigameData[t])) {
//					if (l.size() >= dataRequest.maxEntries)
//						break;
//					l.add(data);
//					lst.remove(data);
//				}
//				lst.clear();
//
//				// Return
//				return l.toArray(t -> new MinigameData[t]);
			}

			// List all
			globalData.runForChildContainers(container -> {
				try {
					// Load minigame data
					if (container.startsWith("save-")) {
						CommonDataContainer data = globalData.getChildContainer(container);
						String saveID = container.substring(5);

						// Create object
						MinigameData res = new MinigameData();
						res.userID = saveID;

						// Load data
						if (data.entryExists("minigame-" + gameId + "-" + dataRequest.gameLevel)) {
							ObjectMapper mapper = new ObjectMapper();
							MinigameDataContainer c = mapper.readValue(
									data.getEntry("minigame-" + gameId + "-" + dataRequest.gameLevel).toString(),
									MinigameDataContainer.class);
							res.timePlayed = c.timePlayed;
							res.timesLost = c.timesLost;
							res.timesWon = c.timesWon;
							res.value = c.data.getOrDefault(dataRequest.key, 0);

							// Check friend
							if (dataRequest.friendsOnly) {
								// TODO
								if (!requestingID.equals(res.userID))
									return true;
							}

							// Check
							if (dataRequest.minimalPlayedAtTime != -1
									&& res.timePlayed < dataRequest.minimalPlayedAtTime)
								return true;
							else if (dataRequest.maximumPlayedAtTime != -1
									&& res.timePlayed > dataRequest.maximumPlayedAtTime)
								return true;

							// Add
							lst.add(res);
						} else {
							res.timePlayed = System.currentTimeMillis();
							res.timesLost = 0;
							res.timesWon = 0;
							lst.add(res);
						}

					}
					return true;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			// Sort and apply limit
			ArrayList<MinigameData> l = new ArrayList<MinigameData>();
			for (MinigameData data : lst.stream().sorted((t1, t2) -> -Integer.compare(t1.value, t2.value))
					.toArray(t -> new MinigameData[t])) {
				if (l.size() >= dataRequest.maxEntries)
					break;
				l.add(data);
				lst.remove(data);
			}
			lst.clear();

			// Return
			return l.toArray(t -> new MinigameData[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
