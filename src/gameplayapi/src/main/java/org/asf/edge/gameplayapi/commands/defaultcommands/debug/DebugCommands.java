package org.asf.edge.gameplayapi.commands.defaultcommands.debug;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.commands.TaskBasedCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;
import org.asf.edge.gameplayapi.util.InventoryUtils;
import org.asf.edge.gameplayapi.util.inventory.ItemRedemptionInfo;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
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
							CommonDataManager.getInstance().getContainer("test").setEntry("test",
									new JsonPrimitive("abc"));
							return (CommonDataManager.getInstance().getContainer("test").getEntry("test").getAsString()
									.equals("abc") ? "Test passed" : "Test failed");
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
							CommonDataManager.getInstance().getContainer("test").deleteContainer();
							return "Test passed";
						} catch (Exception e) {
						}
						return "Test errored";
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
						AccountSaveContainer save = (AccountSaveContainer) ctx.getCommandMemory().get("active_profile");
						int def = Integer.parseInt(args[0]);
						int quantity = 1;
						if (args.length > 1)
							quantity = Integer.parseInt(args[1]);

						// Test
						ItemRedemptionInfo info = new ItemRedemptionInfo();
						info.containerID = 1;
						info.defID = def;
						info.quantity = quantity;
						InventoryUpdateResponseData resp = InventoryUtils.redeemItems(new ItemRedemptionInfo[] { info },
								save.getAccount(), save, false);

						// Show response
						return "Response:\n" + new XmlMapper().writer().withDefaultPrettyPrinter()
								.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withRootName("CIRS")
								.writeValueAsString(resp).trim();
					}

				}

		};
	}

}
