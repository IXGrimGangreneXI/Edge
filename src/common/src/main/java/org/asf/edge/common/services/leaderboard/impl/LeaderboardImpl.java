package org.asf.edge.common.services.leaderboard.impl;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.leaderboard.Leaderboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class LeaderboardImpl extends Leaderboard {

	private String name;
	private CommonDataContainer container;
	private Logger logger = LogManager.getLogger("Leaderboards");

	private Object refreshAllTimeLock = new Object();
	private Object refreshDailyLock = new Object();
	private Object refreshWeeklyLock = new Object();
	private Object refreshMonthlyLock = new Object();

	public LeaderboardImpl(String name) {
		// Load container
		this.name = name;
		container = CommonDataManager.getInstance().getContainer("LEADERBOARD" + name.toUpperCase());

		// Refresh
		refreshLeaderboard();

		// Start refresh
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Wait 12 hours
				try {
					Thread.sleep(12 * 60 * 60 * 1000);
				} catch (InterruptedException e) {
					break;
				}

				// Refresh
				refreshLeaderboard();
			}
		});
	}

	private void refreshLeaderboard() {
		// Log
		logger.info("Refreshing " + name + " leaderboard...");

		// Refresh
		logger.info("Gathering user scores...");
		HashMap<String, Integer> userScores = new HashMap<String, Integer>();
		AccountManager.getInstance().runForAllAccounts((account) -> {
			// Check last login
			long lastLogin = account.getLastLoginTime();
			if (lastLogin == -1 || ((System.currentTimeMillis() / 1000l) - lastLogin) > 32 * 24 * 60 * 60) {
				// Skip since they have been offline for over a month, no point in adding their
				// scores, lets keep data usage minimal
				return true;
			}

			// Get score for all saves
			for (String save : account.getSaveIDs()) {
				// TODO: change to not be hardcoded
				userScores.put(save, AchievementManager.getInstance()
						.getRank(account.getSave(save), save, RankTypeID.UDT).getTotalScore());
			}

			return true;
		});
		refreshAllTimeScores(userScores);
		refreshDailyScoresIfNeeded(userScores);
		refreshWeeklyScoresIfNeeded(userScores);
		refreshMonthlyScoresIfNeeded(userScores);
	}

	private void refreshAllTimeScores(HashMap<String, Integer> scores) {
		// Refresh all times scores
		try {
			CommonDataContainer scoresAllTime = container.getChildContainer("alltime");
			int addedScores = 0;

			// Read last time
			long lastRefresh = -1;
			if (scoresAllTime.entryExists("last_refresh"))
				lastRefresh = scoresAllTime.getEntry("last_refresh").getAsLong();

			// Check refresh
			if (lastRefresh != -1 && (System.currentTimeMillis() - lastRefresh) < 12 * 60 * 60 * 1000)
				return;

			// Refresh
			scoresAllTime.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
			logger.info("Refreshing " + name + " scores of all time...");

			// Block
			synchronized (refreshAllTimeLock) {
				// Load container
				scoresAllTime = scoresAllTime.getChildContainer("scores");

				// Clear scores
				scoresAllTime.deleteContainer();

				// Add scores
				for (String id : scores.keySet().stream()
						.sorted((t1, t2) -> -Integer.compare(scores.get(t1), scores.get(t2)))
						.toArray(t -> new String[t])) {
					if (scores.get(id) <= 0)
						continue;

					// Add only a thousand
					if (addedScores >= 1000)
						break;
					addedScores++;

					// Add
					scoresAllTime.setEntry("s-" + id, new JsonPrimitive(scores.get(id)));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void refreshDailyScoresIfNeeded(HashMap<String, Integer> scores) {
		try {
			Calendar cal = Calendar.getInstance();
			CommonDataContainer scoresDaily = container.getChildContainer("daily");

			// Check last date
			JsonElement ent = scoresDaily.getEntry("last_day");
			int lastDay = ent == null ? -1 : ent.getAsInt();
			if (lastDay != cal.get(Calendar.DAY_OF_YEAR)) {
				// Read last time
				long lastRefresh = -1;
				if (scoresDaily.entryExists("last_refresh"))
					lastRefresh = scoresDaily.getEntry("last_refresh").getAsLong();

				// Check refresh
				if (lastRefresh != -1 && (System.currentTimeMillis() - lastRefresh) > 12 * 60 * 60 * 1000)
					return;

				// Refresh
				scoresDaily.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
				scoresDaily.setEntry("last_day", new JsonPrimitive(cal.get(Calendar.DAY_OF_YEAR)));
				logger.info("Refreshing daily " + name + " score...");

				// Block
				synchronized (refreshDailyLock) {
					CommonDataContainer current = scoresDaily.getChildContainer("scores_current");
					CommonDataContainer last = scoresDaily.getChildContainer("scores_last");

					// Load last scores
					HashMap<String, Integer> lastScores = new HashMap<String, Integer>();
					last.runForEntries((key, value) -> {
						if (key.startsWith("s-")) {
							String id = key.substring(2);
							lastScores.put(id, value.getAsInt());
						}
						return true;
					});

					// Compute current score changes
					HashMap<String, Integer> newScores = new HashMap<String, Integer>();
					for (String id : scores.keySet()) {
						if (!lastScores.containsKey(id))
							continue;
						int scoreChange = scores.get(id) - lastScores.get(id);
						if (scoreChange > 0)
							newScores.put(id, scoreChange);
					}

					// Clear current scores
					int addedScores = 0;
					current.deleteContainer();
					current.setEntry("dataholder", new JsonPrimitive(true));
					current.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
					current.setEntry("last_refresh_prev",
							last.entryExists("last_refresh") ? last.getEntry("last_refresh")
									: new JsonPrimitive(System.currentTimeMillis()));
					for (String id : newScores.keySet().stream()
							.sorted((t1, t2) -> -Integer.compare(newScores.get(t1), newScores.get(t2)))
							.toArray(t -> new String[t])) {
						// Add only a thousand
						if (addedScores >= 1000)
							break;
						addedScores++;

						// Add
						current.setEntry("s-" + id, new JsonPrimitive(newScores.get(id)));
					}

					// Delete last scores
					last.deleteContainer();

					// Populate last scores
					last.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
					for (String id : scores.keySet()) {
						last.setEntry("s-" + id, new JsonPrimitive(scores.get(id)));
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void refreshWeeklyScoresIfNeeded(HashMap<String, Integer> scores) {
		try {
			Calendar cal = Calendar.getInstance();
			CommonDataContainer scoresWeekly = container.getChildContainer("weekly");

			// Check last date
			JsonElement ent = scoresWeekly.getEntry("last_week");
			int lastWeek = ent == null ? -1 : ent.getAsInt();
			if (lastWeek != cal.get(Calendar.WEEK_OF_YEAR)) {
				// Read last time
				long lastRefresh = -1;
				if (scoresWeekly.entryExists("last_refresh"))
					lastRefresh = scoresWeekly.getEntry("last_refresh").getAsLong();

				// Check refresh
				if (lastRefresh != -1 && (System.currentTimeMillis() - lastRefresh) > 12 * 60 * 60 * 1000)
					return;

				// Refresh
				scoresWeekly.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
				scoresWeekly.setEntry("last_week", new JsonPrimitive(cal.get(Calendar.WEEK_OF_YEAR)));
				logger.info("Refreshing weekly " + name + " score...");

				// Block
				synchronized (refreshWeeklyLock) {
					CommonDataContainer current = scoresWeekly.getChildContainer("scores_current");
					CommonDataContainer last = scoresWeekly.getChildContainer("scores_last");

					// Load last scores
					HashMap<String, Integer> lastScores = new HashMap<String, Integer>();
					last.runForEntries((key, value) -> {
						if (key.startsWith("s-")) {
							String id = key.substring(2);
							lastScores.put(id, value.getAsInt());
						}
						return true;
					});

					// Compute current score changes
					HashMap<String, Integer> newScores = new HashMap<String, Integer>();
					for (String id : scores.keySet()) {
						if (!lastScores.containsKey(id))
							continue;
						int scoreChange = scores.get(id) - lastScores.get(id);
						if (scoreChange > 0)
							newScores.put(id, scoreChange);
					}

					// Clear current scores
					int addedScores = 0;
					current.deleteContainer();
					current.setEntry("dataholder", new JsonPrimitive(true));
					current.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
					current.setEntry("last_refresh_prev",
							last.entryExists("last_refresh") ? last.getEntry("last_refresh")
									: new JsonPrimitive(System.currentTimeMillis()));
					for (String id : newScores.keySet().stream()
							.sorted((t1, t2) -> -Integer.compare(newScores.get(t1), newScores.get(t2)))
							.toArray(t -> new String[t])) {
						// Add only a thousand
						if (addedScores >= 1000)
							break;
						addedScores++;

						// Add
						current.setEntry("s-" + id, new JsonPrimitive(newScores.get(id)));
					}

					// Delete last scores
					last.deleteContainer();

					// Populate last scores
					last.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
					for (String id : scores.keySet()) {
						last.setEntry("s-" + id, new JsonPrimitive(scores.get(id)));
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void refreshMonthlyScoresIfNeeded(HashMap<String, Integer> scores) {
		try {
			Calendar cal = Calendar.getInstance();
			CommonDataContainer scoresMonthly = container.getChildContainer("monthly");

			// Check last date
			JsonElement ent = scoresMonthly.getEntry("last_month");
			int lastMonth = ent == null ? -1 : ent.getAsInt();
			if (lastMonth != cal.get(Calendar.MONTH)) {
				// Read last time
				long lastRefresh = -1;
				if (scoresMonthly.entryExists("last_refresh"))
					lastRefresh = scoresMonthly.getEntry("last_refresh").getAsLong();

				// Check refresh
				if (lastRefresh != -1 && (System.currentTimeMillis() - lastRefresh) > 12 * 60 * 60 * 1000)
					return;

				// Refresh
				scoresMonthly.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
				scoresMonthly.setEntry("last_month", new JsonPrimitive(cal.get(Calendar.MONTH)));
				logger.info("Refreshing monthly " + name + " score...");

				// Block
				synchronized (refreshMonthlyLock) {
					CommonDataContainer current = scoresMonthly.getChildContainer("scores_current");
					CommonDataContainer last = scoresMonthly.getChildContainer("scores_last");

					// Load last scores
					HashMap<String, Integer> lastScores = new HashMap<String, Integer>();
					last.runForEntries((key, value) -> {
						if (key.startsWith("s-")) {
							String id = key.substring(2);
							lastScores.put(id, value.getAsInt());
						}
						return true;
					});

					// Compute current score changes
					HashMap<String, Integer> newScores = new HashMap<String, Integer>();
					for (String id : scores.keySet()) {
						if (!lastScores.containsKey(id))
							continue;
						int scoreChange = scores.get(id) - lastScores.get(id);
						if (scoreChange > 0)
							newScores.put(id, scoreChange);
					}

					// Clear current scores
					int addedScores = 0;
					current.deleteContainer();
					current.setEntry("dataholder", new JsonPrimitive(true));
					current.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
					current.setEntry("last_refresh_prev",
							last.entryExists("last_refresh") ? last.getEntry("last_refresh")
									: new JsonPrimitive(System.currentTimeMillis()));
					for (String id : newScores.keySet().stream()
							.sorted((t1, t2) -> -Integer.compare(newScores.get(t1), newScores.get(t2)))
							.toArray(t -> new String[t])) {
						// Add only a thousand
						if (addedScores >= 1000)
							break;
						addedScores++;

						// Add
						current.setEntry("s-" + id, new JsonPrimitive(newScores.get(id)));
					}

					// Delete last scores
					last.deleteContainer();

					// Populate last scores
					last.setEntry("last_refresh", new JsonPrimitive(System.currentTimeMillis()));
					for (String id : scores.keySet()) {
						last.setEntry("s-" + id, new JsonPrimitive(scores.get(id)));
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Integer> getWeeklyScores() {
		try {
			synchronized (refreshWeeklyLock) {
				// Load scores
				HashMap<String, Integer> userScores = new HashMap<String, Integer>();

				// Load container
				CommonDataContainer scores = container.getChildContainer("weekly");
				scores = scores.getChildContainer("scores_current");

				// Add entries
				scores.runForEntries((key, value) -> {
					if (key.startsWith("s-")) {
						String id = key.substring(2);
						userScores.put(id, value.getAsInt());
					}
					return true;
				});

				// Sort
				LinkedHashMap<String, Integer> userScoresSorted = new LinkedHashMap<String, Integer>();
				for (String id : userScores.keySet().stream()
						.sorted((t1, t2) -> -Integer.compare(userScores.get(t1), userScores.get(t2)))
						.toArray(t -> new String[t])) {
					userScoresSorted.put(id, userScores.get(id));
				}
				return userScoresSorted;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Integer> getMonthlyScores() {
		try {
			synchronized (refreshMonthlyLock) {
				// Load scores
				HashMap<String, Integer> userScores = new HashMap<String, Integer>();

				// Load container
				CommonDataContainer scores = container.getChildContainer("monthly");
				scores = scores.getChildContainer("scores_current");

				// Add entries
				scores.runForEntries((key, value) -> {
					if (key.startsWith("s-")) {
						String id = key.substring(2);
						userScores.put(id, value.getAsInt());
					}
					return true;
				});

				// Sort
				LinkedHashMap<String, Integer> userScoresSorted = new LinkedHashMap<String, Integer>();
				for (String id : userScores.keySet().stream()
						.sorted((t1, t2) -> -Integer.compare(userScores.get(t1), userScores.get(t2)))
						.toArray(t -> new String[t])) {
					userScoresSorted.put(id, userScores.get(id));
				}
				return userScoresSorted;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Integer> getDailyScores() {
		try {
			synchronized (refreshDailyLock) {
				// Load scores
				HashMap<String, Integer> userScores = new HashMap<String, Integer>();

				// Load container
				CommonDataContainer scores = container.getChildContainer("daily");
				scores = scores.getChildContainer("scores_current");

				// Add entries
				scores.runForEntries((key, value) -> {
					if (key.startsWith("s-")) {
						String id = key.substring(2);
						userScores.put(id, value.getAsInt());
					}
					return true;
				});

				// Sort
				LinkedHashMap<String, Integer> userScoresSorted = new LinkedHashMap<String, Integer>();
				for (String id : userScores.keySet().stream()
						.sorted((t1, t2) -> -Integer.compare(userScores.get(t1), userScores.get(t2)))
						.toArray(t -> new String[t])) {
					userScoresSorted.put(id, userScores.get(id));
				}
				return userScoresSorted;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Integer> getScoresAllTime() {
		try {
			synchronized (refreshAllTimeLock) {
				// Load scores
				HashMap<String, Integer> userScores = new HashMap<String, Integer>();

				// Load container
				CommonDataContainer scoresAllTime = container.getChildContainer("alltime");
				scoresAllTime = scoresAllTime.getChildContainer("scores");

				// Add entries
				scoresAllTime.runForEntries((key, value) -> {
					if (key.startsWith("s-")) {
						String id = key.substring(2);
						userScores.put(id, value.getAsInt());
					}
					return true;
				});

				// Sort
				LinkedHashMap<String, Integer> userScoresSorted = new LinkedHashMap<String, Integer>();
				for (String id : userScores.keySet().stream()
						.sorted((t1, t2) -> -Integer.compare(userScores.get(t1), userScores.get(t2)))
						.toArray(t -> new String[t])) {
					userScoresSorted.put(id, userScores.get(id));
				}
				return userScoresSorted;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DateRange getDateRangeOfDailyScores() {
		try {
			synchronized (refreshDailyLock) {
				// Load container
				CommonDataContainer scores = container.getChildContainer("daily");
				scores = scores.getChildContainer("scores_current");

				// Retrieve times
				DateRange range = new DateRange();
				range.dateEnd = scores.getEntry("last_refresh").getAsLong();
				range.dateStart = scores.getEntry("last_refresh_prev").getAsLong();
				return range;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DateRange getDateRangeOfWeeklyScores() {
		try {
			synchronized (refreshWeeklyLock) {
				// Load container
				CommonDataContainer scores = container.getChildContainer("weekly");
				scores = scores.getChildContainer("scores_current");

				// Retrieve times
				DateRange range = new DateRange();
				range.dateEnd = scores.getEntry("last_refresh").getAsLong();
				range.dateStart = scores.getEntry("last_refresh_prev").getAsLong();
				return range;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DateRange getDateRangeOfMonthlyScores() {
		try {
			synchronized (refreshMonthlyLock) {
				// Load container
				CommonDataContainer scores = container.getChildContainer("monthly");
				scores = scores.getChildContainer("scores_current");

				// Retrieve times
				DateRange range = new DateRange();
				range.dateEnd = scores.getEntry("last_refresh").getAsLong();
				range.dateStart = scores.getEntry("last_refresh_prev").getAsLong();
				return range;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
