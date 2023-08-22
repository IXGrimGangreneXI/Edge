package org.asf.edge.modules.gridapi.commands.defaultcommands;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.edge.modules.gridapi.commands.IGridServerCommand;
import org.asf.edge.modules.gridapi.commands.TaskBasedCommand;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.identities.PropertyInfo;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;

public class IdentityCommands extends TaskBasedCommand {

	@Override
	public String id() {
		return "identities";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Identity configuration commands";
	}

	@Override
	public IGridServerCommand[] tasks() {
		return new IGridServerCommand[] {

				// Identity register
				new IGridServerCommand() {

					@Override
					public String id() {
						return "create";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<name>\" [\"<display-name>\"] [properties... <name> <is-readonly:true/false> <value>]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Creates Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: name");
							return null;
						}

						// Load request into memory
						String name = args[0];
						String display = args.length >= 2 ? args[1] : null;
						HashMap<String, PropertyInfo> props = new HashMap<String, PropertyInfo>();
						PropertyInfo p1 = new PropertyInfo();
						p1.isReadonly = true;
						p1.value = name;
						props.put("name", p1);
						if (display != null) {
							PropertyInfo p2 = new PropertyInfo();
							p2.isReadonly = false;
							p2.value = display;
							props.put("displayName", p2);
						} else {
							PropertyInfo p2 = new PropertyInfo();
							p2.isReadonly = false;
							p2.value = name;
							props.put("displayName", p2);
						}
						for (int i = 2; i < args.length; i += 3) {
							if (i + 1 > args.length) {
								outputWriteLineCallback.accept("Missing argument: property read-only mode");
								return null;
							}
							if (i + 2 > args.length) {
								outputWriteLineCallback.accept("Missing argument: property value");
								return null;
							}
							if (args[i + 1].equalsIgnoreCase("false") && args[i + 1].equalsIgnoreCase("true")) {
								outputWriteLineCallback.accept("Invalid argument: property read-only mode: true/false");
								return null;
							}
							if (props.containsKey(args[i])) {
								outputWriteLineCallback.accept("Invalid argument: name: property already present");
								return null;
							}
							PropertyInfo prop = new PropertyInfo();
							prop.value = args[i + 2];
							prop.isReadonly = args[i + 1].equalsIgnoreCase("true");
							props.put(args[i], prop);
						}

						// Create
						IdentityDef id = IdentityUtils.createIdentity(props);

						// Return
						return "Identity created successfully, ID: " + id.identity;
					}

				},

				// Identity show
				new IGridServerCommand() {

					@Override
					public String id() {
						return "show";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Shows Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}

						// Load request into memory
						String id = args[0];

						// Check identity
						if (!IdentityUtils.identityExists(id)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: identity not found");
							return null;
						}

						// Show
						IdentityDef identity = IdentityUtils.getIdentity(id);
						String message = "Identity properties for: " + identity.identity + ":";
						for (String key : identity.properties.keySet())
							message += "\n - " + key + " = " + identity.properties.get(key).value
									+ (identity.properties.get(key).isReadonly ? " [READ-ONLY]" : "");
						return message;
					}

				},

				// Identity update
				new IGridServerCommand() {

					@Override
					public String id() {
						return "update";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\" [properties... <name> <value>]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Updates Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}
						if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: properties");
							return null;
						}
						HashMap<String, String> props = new HashMap<String, String>();
						for (int i = 2; i < args.length; i += 2) {
							if (i + 1 > args.length) {
								outputWriteLineCallback.accept("Missing argument: property value");
								return null;
							}
							if (props.containsKey(args[i])) {
								outputWriteLineCallback.accept("Invalid argument: name: property already present");
								return null;
							}
							props.put(args[i], args[i + 1]);
						}

						// Load request into memory
						String id = args[0];

						// Check identity
						if (!IdentityUtils.identityExists(id)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: identity not found");
							return null;
						}

						// Update
						if (IdentityUtils.updateIdentity(id, props)) {
							// Invalid name
							outputWriteLineCallback
									.accept("Error: properties invalid (please verify existence of properties)");
							return null;
						}

						return "Identity updated successfully";
					}

				},

				// Identity delete
				new IGridServerCommand() {

					@Override
					public String id() {
						return "delete";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Deletes Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}

						// Load request into memory
						String id = args[0];

						// Check identity
						if (!IdentityUtils.identityExists(id)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: identity not found");
							return null;
						}

						// Delete
						IdentityUtils.deleteIdentity(id);
						return "Identity deleted successfully";
					}

				},

				// Identity hostban
				new IGridServerCommand() {

					@Override
					public String id() {
						return "hostban";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Host-bans Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}

						// Load request into memory
						String id = args[0];

						// Check identity
						if (!IdentityUtils.identityExists(id)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: identity not found");
							return null;
						}

						// Ban
						IdentityUtils.updateIdentity(id, Map.of("hostBanned", "true"));
						return "Identity host-banned successfully";
					}

				},

				// Identity pardonhostingban
				new IGridServerCommand() {

					@Override
					public String id() {
						return "pardonhostingban";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Pardons host-banned Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}

						// Load request into memory
						String id = args[0];

						// Check identity
						if (!IdentityUtils.identityExists(id)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: identity not found");
							return null;
						}

						// Ban
						IdentityUtils.updateIdentity(id, Map.of("hostBanned", "false"));
						return "Identity pardoned successfully";
					}

				},

				// Identity list
				new IGridServerCommand() {

					@Override
					public String id() {
						return "list";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return null;
					}

					@Override
					public String description(CommandContext ctx) {
						return "Lists all Phoenix identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						StringBuilder message = new StringBuilder("List of all Phoenix identities:");
						for (IdentityDef id : IdentityUtils.getAll()) {
							if (!id.properties.containsKey("serverCertificateProperties"))
								message.append("\n - " + id.identity + " - "
										+ (id.properties.containsKey("name") ? id.properties.get("name").value
												: "UNKNOWN")
										+ " (display name: "
										+ (id.properties.containsKey("displayName")
												? id.properties.get("displayName").value
												: "UNKNOWN")
										+ ")");
						}
						return message.toString();
					}

				}

		};
	}

}
