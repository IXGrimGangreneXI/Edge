package org.asf.edge.common.commands.defaultcommands.debug;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.commands.TaskBasedCommand;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.entities.messages.defaultmessages.WsPluginMessage;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.util.TaggedMessageUtils;
import org.asf.edge.common.xmls.messages.MessageInfoData;
import org.asf.edge.common.util.InventoryUtil;
import org.asf.edge.common.util.inventory.ItemRedemptionInfo;
import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class DebugCommands extends TaskBasedCommand {

	@Override
	public String id() {
		return "debug";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Debug commands";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.OPERATOR;
	}

	@Override
	public String permNode() {
		return "commands.operator.debugcommands";
	}

	@Override
	public IEdgeServerCommand[] tasks() {
		return new IEdgeServerCommand[] {

				new IEdgeServerCommand() {

					@Override
					public String id() {
						return "commondatatest1";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return null;
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for common data management";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) {
						try {
							CommonDataManager.getInstance().getKeyValueContainer("test").setEntry("test",
									new JsonPrimitive("abc"));
							CommonDataManager.getInstance().getKeyValueContainer("test").getChildContainer("test1")
									.getChildContainer("test2").getChildContainer("test3")
									.setEntry("abc3", new JsonPrimitive("def"));
							CommonDataManager.getInstance().getKeyValueContainer("test").getChildContainer("test3")
									.setEntry("abc3", new JsonPrimitive("def2"));
							CommonDataManager.getInstance().getKeyValueContainer("test").getChildContainer("test1")
									.getChildContainer("test2").getChildContainers();
							CommonDataManager.getInstance().getKeyValueContainer("test").getChildContainer("test1")
									.getChildContainers();
							CommonDataManager.getInstance().getKeyValueContainer("test").getChildContainers();
							CommonDataManager.getInstance().getKeyValueContainer("test").getChildContainer("test1")
									.getChildContainer("test2").getChildContainer("test3").getEntryKeys();
							String r = (CommonDataManager.getInstance().getKeyValueContainer("test").getEntry("test")
									.getAsString().equals("abc") ? "Test passed" : "Test failed");
							return r;
						} catch (Exception e) {
						}
						return "Test errored";
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "commondatatest2";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return null;
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for common data management";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) {
						try {
							CommonDataManager.getInstance().getKeyValueContainer("test").deleteContainer();
							return "Test passed";
						} catch (Exception e) {
						}
						return "Test errored";
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "commondatatest3";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return null;
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for common data management";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) {
						try {
							CommonDataManager.getInstance().getKeyValueContainer("test").runForEntries((key, value) -> {

								return true;
							});
							CommonDataManager.getInstance().getKeyValueContainer("test").runForChildContainers(name -> {

								return true;
							});
							JsonElement e = CommonDataManager.getInstance().getKeyValueContainer("test")
									.findEntry((key, value) -> {
										return true;
									});
							if (e == null)
								return "Test failed";
							return "Test passed";
						} catch (Exception e) {
						}
						return "Test errored";
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "runforallaccs";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return null;
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for account management";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) {
						AccountManager.getInstance().runForAllAccounts(t -> {
							outputWriteLineCallback.accept(t.getUsername());
							return true;
						});
						return "Done";
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "itemtestadd";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "<defID> [<quantity>]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for inventory management";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						// Check
						if (!ctx.getCommandMemory().containsKey("active_profile")) {
							outputWriteLineCallback.accept(
									"Error: no active profile, please use 'profiles select' before using this command");
							return null;
						}
						AccountSaveContainer save = AccountManager.getInstance()
								.getAccount(ctx.getCommandMemory().get("active_account").toString())
								.getSave(ctx.getCommandMemory().get("active_profile").toString());
						int def = Integer.parseInt(args[0]);
						int quantity = 1;
						if (args.length > 1)
							quantity = Integer.parseInt(args[1]);

						// Test
						ItemRedemptionInfo info = new ItemRedemptionInfo();
						info.containerID = 1;
						info.defID = def;
						info.quantity = quantity;
						InventoryUpdateResponseData resp = InventoryUtil.redeemItems(new ItemRedemptionInfo[] { info },
								save.getAccount(), save, false);

						// Show response
						return "Response:\n" + new XmlMapper().writer().withDefaultPrettyPrinter()
								.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withRootName("CIRS")
								.writeValueAsString(resp).trim();
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "itemtestbuy";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "<shopID> <defID> [<quantity>]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for buying";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						// Check
						if (!ctx.getCommandMemory().containsKey("active_profile")) {
							outputWriteLineCallback.accept(
									"Error: no active profile, please use 'profiles select' before using this command");
							return null;
						}
						AccountSaveContainer save = AccountManager.getInstance()
								.getAccount(ctx.getCommandMemory().get("active_account").toString())
								.getSave(ctx.getCommandMemory().get("active_profile").toString());
						int shop = Integer.parseInt(args[0]);
						int def = Integer.parseInt(args[1]);
						int quantity = 1;
						if (args.length > 2)
							quantity = Integer.parseInt(args[2]);

						// Test
						ItemRedemptionInfo info = new ItemRedemptionInfo();
						info.containerID = 1;
						info.defID = def;
						info.quantity = quantity;
						InventoryUpdateResponseData resp = InventoryUtil.purchaseItems(shop,
								new ItemRedemptionInfo[] { info }, save.getAccount(), save, false);

						// Show response
						return "Response:\n" + new XmlMapper().writer().withDefaultPrettyPrinter()
								.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withRootName("CIRS")
								.writeValueAsString(resp).trim();
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "filtertest";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "<text> [<strictmode>]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for filtering";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) {
						return TextFilterService.getInstance().filterString(args[0],
								args.length > 1 ? args[1].equals("true") : false);
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "filtertest2";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "<text> <strictmode>";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for filtering";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						return "Response:\n"
								+ new XmlMapper().writer().withDefaultPrettyPrinter()
										.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
										.withRootName("FilterResult")
										.writeValueAsString(
												TextFilterService.getInstance().filter(args[0], args[1].equals("true")))
										.trim();
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "filtertest3";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "<text> <strictmode>";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for filtering";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						return "Response: "
								+ TextFilterService.getInstance().isFiltered(args[0], args[1].equals("true"));
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "testmessages1";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"[message]\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for messaging";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						return "Result:\n" + new XmlMapper().writer().withDefaultPrettyPrinter()
								.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
								.withRootName("TaggedMessage")
								.writeValueAsString(TaggedMessageUtils.parseTagged(args[0]));
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "testmessages2";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for messaging";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						WsPluginMessage msg = new WsPluginMessage();
						msg.messageData.put("abc", "def");
						msg.messageData.put("e", "gh");
						msg.messageData.put(";sl;sl;sl;", ";bb;bb;bb;bb;");
						MessageInfoData obj = new MessageInfoData();
						msg.serialize(obj);
						WsPluginMessage rec = new WsPluginMessage();
						rec.deserialize(obj);
						return "Result:\n"
								+ new XmlMapper().writer().withDefaultPrettyPrinter()
										.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
										.withRootName("PluginMessage").writeValueAsString(obj)
								+ "\n\nDecoded: "
								+ new XmlMapper().writer().withDefaultPrettyPrinter()
										.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
										.withRootName("PluginMessageData").writeValueAsString(rec.messageData);
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "sendmessage";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<message>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for messaging";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						// Check
						WsGenericMessage msg = new WsGenericMessage();
						msg.rawObject.typeID = 3;
						msg.rawObject.messageContentMembers = args[0];
						msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
						WsMessageService.getInstance().getMessengerFor(ctx.getAccountObject()).sendSessionMessage(msg);
						return "Sent";
					}

				}, new IEdgeServerCommand() {

					@Override
					public String id() {
						return "addxp";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "<type> <amount>";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Test command for XP";
					}

					@Override
					public PermissionLevel permLevel() {
						return PermissionLevel.OPERATOR;
					}

					@Override
					public String permNode() {
						return "commands.operator.debugcommands";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs)
							throws IOException {
						// Check
						if (!ctx.getCommandMemory().containsKey("active_profile")) {
							outputWriteLineCallback.accept(
									"Error: no active profile, please use 'profiles select' before using this command");
							return null;
						}
						AccountSaveContainer save = AccountManager.getInstance()
								.getAccount(ctx.getCommandMemory().get("active_account").toString())
								.getSave(ctx.getCommandMemory().get("active_profile").toString());
						int type = Integer.parseInt(args[0]);
						int amount = Integer.parseInt(args[1]);

						// Show response
						AchievementManager.getInstance().getRankForUser(save, RankTypeID.getByTypeID(type))
								.addPoints(amount);
						return "Response:\n" + new XmlMapper().writer().withDefaultPrettyPrinter()
								.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withRootName("Rank")
								.writeValueAsString(AchievementManager.getInstance().getRankForUser(save,
										RankTypeID.getByTypeID(type)))
								.trim();
					}

				}

		};
	}

}
