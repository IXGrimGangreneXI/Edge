package org.asf.edge.common.entities.minigamedata;

import java.util.Map;

public class MinigameSaveRequest {

	public int difficulty;
	public int gameLevel;
	public boolean isWin;
	public boolean isLoss;

	public Map<String, Integer> data;

}
