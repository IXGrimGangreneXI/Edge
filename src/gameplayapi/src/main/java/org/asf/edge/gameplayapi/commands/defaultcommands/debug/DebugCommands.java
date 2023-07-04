package org.asf.edge.gameplayapi.commands.defaultcommands.debug;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.commands.TaskBasedCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;

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

				}

		};
	}

}
