package org.asf.edge.gameplayapi.http.handlers.achievements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.leaderboard.Leaderboard;
import org.asf.edge.common.services.leaderboard.LeaderboardManager;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskData;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskSetRequestList;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskSetResponseData;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskSetResponseList;
import org.asf.edge.gameplayapi.xmls.achievements.leaderboard.UdtLeaderboardRequestData;
import org.asf.edge.gameplayapi.xmls.achievements.leaderboard.UdtLeaderboardResponseData;

public class AchievementWebServiceV2Processor extends EdgeWebService<EdgeGameplayApiServer> {

	public AchievementWebServiceV2Processor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AchievementWebServiceV2Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v2/AchievementWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult setUserAchievementTask(FunctionInfo func) throws IOException {
		// Handle task reward request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);

		// Parse request
		String request = req.getEncryptedValue("achievementTaskSetRequest");
		AchievementTaskSetRequestList lst = req.parseXmlValue(request, AchievementTaskSetRequestList.class);

		// Prepare response
		// TODO: stubbed
		ArrayList<AchievementTaskSetResponseData> responses = new ArrayList<AchievementTaskSetResponseData>();
		for (AchievementTaskData reqA : lst.requests) {
			// TODO: stubbed
			AchievementTaskSetResponseData resp = new AchievementTaskSetResponseData();
			resp.success = false;
			responses.add(resp);
		}

		// Create response
		AchievementTaskSetResponseList resp = new AchievementTaskSetResponseList();
		resp.responses = responses.toArray(t -> new AchievementTaskSetResponseData[t]);
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementTaskSetResponse", resp));
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getTopAchievementPointUsers(FunctionInfo func) throws IOException {
		// Handle task reward request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");

		// Parse request
		String request = req.payload.get("request");
		UdtLeaderboardRequestData rq = req.parseXmlValue(request, UdtLeaderboardRequestData.class);

		// Prepare response
		UdtLeaderboardResponseData resp = new UdtLeaderboardResponseData();
		ArrayList<UdtLeaderboardResponseData.UdtLeaderboardEntryBlock> responses = new ArrayList<UdtLeaderboardResponseData.UdtLeaderboardEntryBlock>();

		// Retrieve
		if (rq.pointTypeID == 12) {
			// UDT request
			int offset = (rq.pageNumber - 1) * rq.pageSize;
			int end = (rq.pageNumber * rq.pageSize) + 1;
			Leaderboard leaderboard = LeaderboardManager.getInstance().getLeaderboard("UDT");
			Map<String, Integer> scores = Map.of();
			Leaderboard.DateRange dateRange = null;
			switch (rq.mode) {

			// All time
			case 1: {
				scores = leaderboard.getScoresAllTime();
				break;
			}

			// Daily
			case 2: {
				dateRange = leaderboard.getDateRangeOfDailyScores();
				scores = leaderboard.getDailyScores();
				break;
			}

			// Weekly
			case 3: {
				dateRange = leaderboard.getDateRangeOfWeeklyScores();
				scores = leaderboard.getWeeklyScores();
				break;
			}

			// Monthly
			case 4: {
				dateRange = leaderboard.getDateRangeOfMonthlyScores();
				scores = leaderboard.getMonthlyScores();
				break;
			}

			}

			// List
			int index = 0;
			for (String id : scores.keySet()) {
				if (index >= end) {
					break;
				}

				// Retrieve save
				AccountSaveContainer save = AccountManager.getInstance().getSaveByID(id);
				if (save != null) {
					if (index < offset) {
						index++;
						continue;
					}
					index++;

					// Add response
					UdtLeaderboardResponseData.UdtLeaderboardEntryBlock entry = new UdtLeaderboardResponseData.UdtLeaderboardEntryBlock();
					entry.userID = id;
					entry.pointsTotal = scores.get(id);
					entry.pointTypeID = rq.pointTypeID;
					entry.userName = save.getUsername();
					responses.add(entry);
				}
			}

			// Set range
			if (dateRange != null) {
				resp.dateRange = new UdtLeaderboardResponseData.UdtDateRangeBlock();

				SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				resp.dateRange.dateStart = fmt.format(new Date(dateRange.dateStart));
				resp.dateRange.dateEnd = fmt.format(new Date(dateRange.dateEnd));
			} else {
				resp.dateRange = new UdtLeaderboardResponseData.UdtDateRangeBlock();
				SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
				resp.dateRange.dateStart = fmt.format(new Date(0));
				resp.dateRange.dateEnd = fmt.format(new Date(System.currentTimeMillis()));
			}
		} else {
			// Error
			LogManager.getLogger("Leaderboards").error("Unable to retrieve leaderboard by point type ID "
					+ rq.pointTypeID + " as Edge doesnt have support for this type ID.");
		}

		// Populate response
		resp.entries = responses.toArray(t -> new UdtLeaderboardResponseData.UdtLeaderboardEntryBlock[t]);
		return ok("text/xml", req.generateXmlValue("UAIR", resp));
	}

}
