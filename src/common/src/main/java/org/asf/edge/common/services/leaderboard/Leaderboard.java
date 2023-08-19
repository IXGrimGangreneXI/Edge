package org.asf.edge.common.services.leaderboard;

import java.util.Map;

/**
 * 
 * Leaderboard class
 * 
 * @author Sky Swimmer
 *
 */
public abstract class Leaderboard {

	public static class DateRange {
		public long dateStart;
		public long dateEnd;
	}

	/**
	 * Retrieves the weekly scores
	 * 
	 * @return Weekly leaderboard scores map (score by save ID)
	 */
	public abstract Map<String, Integer> getWeeklyScores();

	/**
	 * Retrieves the monthly scores
	 * 
	 * @return Monthly leaderboard scores map (score by save ID)
	 */
	public abstract Map<String, Integer> getMonthlyScores();

	/**
	 * Retrieves the daily scores
	 * 
	 * @return Daily leaderboard scores map (score by save ID)
	 */
	public abstract Map<String, Integer> getDailyScores();

	/**
	 * Retrieves the scores of all time
	 * 
	 * @return All-time leaderboard scores map (score by save ID)
	 */
	public abstract Map<String, Integer> getScoresAllTime();

	/**
	 * Retrieves the date range of daily scores
	 * 
	 * @return DateRange instance
	 */
	public abstract DateRange getDateRangeOfDailyScores();

	/**
	 * Retrieves the date range of weekly scores
	 * 
	 * @return DateRange instance
	 */
	public abstract DateRange getDateRangeOfWeeklyScores();

	/**
	 * Retrieves the date range of monthly scores
	 * 
	 * @return DateRange instance
	 */
	public abstract DateRange getDateRangeOfMonthlyScores();

}
