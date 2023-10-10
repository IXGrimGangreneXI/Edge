package org.asf.edge.gameplayapi.http.handlers.itemstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.items.ItemCategoryInfo;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.ItemSaleInfo;
import org.asf.edge.common.entities.items.ItemStoreInfo;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.xmls.items.GetStoreRequestData;
import org.asf.edge.gameplayapi.xmls.items.GetStoreResponseData;
import org.asf.edge.gameplayapi.xmls.items.ItemStoreResponseObject;
import org.asf.edge.gameplayapi.xmls.items.ItemStoreResponseObject.SaleBlock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ItemStoreWebServiceProcessor extends EdgeWebService<EdgeGameplayApiServer> {

	private static ItemManager itemManager;
	private static boolean popularItemManagementInited = false;

	public ItemStoreWebServiceProcessor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ItemStoreWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ItemStoreWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getRankAttributeData(LegacyFunctionInfo info) throws IOException {
		// Handle request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("achievementdata/rankattrs.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		setResponseContent("text/xml", data);
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAnnouncementsByUser(LegacyFunctionInfo info) throws IOException {
		// Handle request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}
		AccountSaveContainer save = account.getSave(tkn.saveID);
		if (save == null) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// FIXME: dummied
		setResponseContent("text/xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<Announcements xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />");
	}

	private static synchronized void initPopularItemManager() {
		if (popularItemManagementInited)
			return;
		popularItemManagementInited = true;
		initPopularItemManager();

		// Refresh popular items in the background
		CommonDataContainer cont = CommonDataManager.getInstance().getContainer("POPULARITEMS");
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				try {
					// Check if a refresh should be done, refreshes are weekly
					boolean requiresRefresh = false;
					if (!cont.entryExists("lastupdate") || (System.currentTimeMillis()
							- cont.getEntry("lastupdate").getAsLong()) > (7 * 24 * 60 * 60 * 1000)) {
						// Refresh needed
						requiresRefresh = true;
					}

					if (requiresRefresh) {
						// Set last update
						cont.setEntry("lastupdate", new JsonPrimitive(System.currentTimeMillis()));

						// Check for each store
						int[] storeIDs = ItemManager.getInstance().getStoreIds();
						for (int store : storeIDs) {
							// Update store
							if (cont.entryExists("current-" + store)) {
								JsonObject current = cont.getEntry("current-" + store).getAsJsonObject();
								cont.setEntry("last-" + store, current);
								cont.setEntry("current-" + store, new JsonObject());
							}
						}
					}
				} catch (IOException e) {
					// Error
					LogManager.getLogger("ItemManager")
							.error("Failed to check if popular items need to be refreshed due to a database error.", e);
				}

				// Wait 30 seconds
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getStore(LegacyFunctionInfo info) throws IOException {
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Init if needed
		if (!popularItemManagementInited) {
			initPopularItemManager();
		}

		// Handle store request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Find stores
		GetStoreRequestData stores = req.parseXmlValue(req.payload.get("getStoreRequest"), GetStoreRequestData.class);
		GetStoreResponseData resp = new GetStoreResponseData();
		resp.stores = new ItemStoreResponseObject[stores.storeIDs.length];
		for (int i = 0; i < resp.stores.length; i++) {
			// Find store
			ItemStoreInfo store = itemManager.getStore(stores.storeIDs[i]);
			if (store != null) {
				// Create object
				ItemStoreResponseObject storeData = new ItemStoreResponseObject();
				storeData.storeID = store.getID();
				storeData.storeName = store.getName();
				storeData.storeDescription = store.getDescription();
				storeData.items = Stream.of(store.getItems()).map(t -> t.getRawObject())
						.toArray(t -> new ItemDefData[t]);

				// Load popular items
				ArrayList<ItemStoreResponseObject.PopularItemBlock> items = new ArrayList<ItemStoreResponseObject.PopularItemBlock>();

				// Go through all items in store
				CommonDataContainer cont = CommonDataManager.getInstance().getContainer("POPULARITEMS");
				if (cont.entryExists("last-" + storeData.storeID)) {
					JsonObject popularItems = cont.getEntry("last-" + storeData.storeID).getAsJsonObject();
					HashMap<Integer, Integer> itms = new HashMap<Integer, Integer>();
					for (String key : popularItems.keySet()) {
						itms.put(Integer.parseInt(key), popularItems.get(key).getAsInt());
					}
					int i2 = 0;
					ItemInfo[] storeItems = ItemManager.getInstance().getStore(storeData.storeID).getItems();
					int limit = (storeItems.length / 15);
					if (limit <= 0)
						limit = 1;
					for (Integer id : itms.keySet().stream()
							.sorted((t1, t2) -> -Integer.compare(itms.get(t1), itms.get(t2)))
							.toArray(t -> new Integer[t])) {
						if (i2 >= limit)
							break;

						// Add item info
						ItemStoreResponseObject.PopularItemBlock itm = new ItemStoreResponseObject.PopularItemBlock();
						itm.itemID = id;
						itm.rank = itms.get(id);
						items.add(itm);

						// Increase index
						i2++;
					}
				}

				// Add to object
				storeData.popularItems = items.toArray(t -> new ItemStoreResponseObject.PopularItemBlock[t]);

				// Find categories
				ArrayList<Integer> categoryIds = new ArrayList<Integer>();
				for (ItemInfo itm : store.getItems()) {
					ItemCategoryInfo[] cats = itm.getCategories();
					for (ItemCategoryInfo cat : cats) {
						if (!categoryIds.contains(cat.getCategoryID()))
							categoryIds.add(cat.getCategoryID());
					}
				}

				// Load sales
				ArrayList<SaleBlock> sales = new ArrayList<SaleBlock>();
				ObjectMapper mapper = new ObjectMapper();
				for (ItemSaleInfo sale : itemManager.getSales()) {
					if (sale.isActive() || sale.isUpcoming()) {
						// Check sale
						boolean valid = false;
						if (IntStream.of(sale.getItemIDs())
								.anyMatch(t -> Stream.of(store.getItems()).anyMatch(t2 -> t2.getID() == t)))
							valid = true;
						else if (IntStream.of(sale.getCategories())
								.anyMatch(t -> categoryIds.stream().anyMatch(t2 -> t2 == t)))
							valid = true;
						if (!valid)
							continue;

						// Create sale block
						SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
						fmt2.setTimeZone(TimeZone.getTimeZone("UTC"));
						SaleBlock block = new SaleBlock();
						block.modifier = sale.getSaleModifier();
						block.saleID = idHash(mapper.writeValueAsString(sale));
						block.memberOnly = sale.isMemberOnly();
						block.categoryIDs = sale.getCategories();
						block.itemIDs = sale.getItemIDs();
						block.startDate = fmt2.format(new Date(sale.getStartTime()));
						block.endDate = fmt2.format(new Date(sale.getEndTime()));
						if (sales.stream().anyMatch(t -> t.saleID == block.saleID))
							LogManager.getLogger("ItemManager")
									.error("Duplicate sale ID due to failure in hashing code for sale '"
											+ sale.getName()
											+ "', this must immediately be reported as the store will break down!");
						sales.add(block);
					}
				}
				storeData.itemSales = sales.toArray(t -> new SaleBlock[t]);

				// Add to response
				resp.stores[i] = storeData;
			} else {
				resp.stores[i] = new ItemStoreResponseObject();
			}
		}

		// Set response
		setResponseContent("text/xml",
				new XmlMapper().writer().withFeatures(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
						.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withDefaultPrettyPrinter()
						.withRootName("GetStoreResponse").writeValueAsString(resp));
	}

	private int idHash(String s) {
		try {
			// IK, MD5, its not for security, just need a way to create 4-byte ids
			// represented as integers
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(s.getBytes("UTF-8"));

			byte[] sub1 = Arrays.copyOfRange(digest, 0, 4);
			byte[] sub2 = Arrays.copyOfRange(digest, 4, 8);
			byte[] sub3 = Arrays.copyOfRange(digest, 8, 12);
			byte[] sub4 = Arrays.copyOfRange(digest, 12, 16);
			int x1 = ByteBuffer.wrap(sub1).getInt();
			int x2 = ByteBuffer.wrap(sub2).getInt();
			int x3 = ByteBuffer.wrap(sub3).getInt();
			int x4 = ByteBuffer.wrap(sub4).getInt();

			return x1 ^ x2 ^ x3 ^ x4;
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getItem(LegacyFunctionInfo info) throws IOException {
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle store request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Find item
		int item = Integer.parseInt(req.payload.get("itemId"));
		ItemInfo def = itemManager.getItemDefinition(item);
		if (def == null) {
			// Empty
			setResponseContent("text/xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
					+ "<I xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
					+ "  <ct>0</ct>\n" + "  <ct2>0</ct2>\n" + "  <cp>0</cp>\n" + "  <im>0</im>\n" + "  <id>0</id>\n"
					+ "  <l>false</l>\n" + "  <ro xsi:nil=\"true\" />\n" + "  <rid xsi:nil=\"true\" />\n"
					+ "  <s>false</s>\n" + "  <as>false</as>\n" + "  <sf>0</sf>\n" + "  <u>0</u>\n"
					+ "  <g xsi:nil=\"true\" />\n" + "  <rf>0</rf>\n" + "  <rtid>0</rtid>\n"
					+ "  <p xsi:nil=\"true\" />\n" + "  <ir xsi:nil=\"true\" />\n" + "  <ipsm xsi:nil=\"true\" />\n"
					+ "  <ism xsi:nil=\"true\" />\n" + "  <bp xsi:nil=\"true\" />\n" + "</I>\n" + "");
			return;
		}

		// Set response
		setResponseContent("text/xml",
				new XmlMapper().writer().withFeatures(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
						.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withDefaultPrettyPrinter()
						.withRootName("I").writeValueAsString(def.getRawObject()));
	}

}
