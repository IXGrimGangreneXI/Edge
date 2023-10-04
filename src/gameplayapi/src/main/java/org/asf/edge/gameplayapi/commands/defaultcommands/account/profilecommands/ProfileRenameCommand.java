package org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ProfileRenameCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "rename";
	}

	@Override
	public String syntax(CommandContext ctx) {
		if (ctx.getPermissions().hasPermission("commands.moderator.profiles.rename", PermissionLevel.MODERATOR))
			return "<id> \"<new username>\" \"[owner]\"";
		else
			return "<id>";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Renames profiles";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.rename";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) {
		AccountObject acc = ctx.getAccountObject();
		AccountManager manager = AccountManager.getInstance();
		if (args.length >= 3 && ctx.getPermissions().hasPermission("commands.moderator.profiles.rename",
				PermissionLevel.MODERATOR)) {
			// Find owner
			String username = args[2];
			String id = AccountManager.getInstance().getAccountIdBySaveUsername(username);
			if (id == null)
				id = AccountManager.getInstance().getAccountID(username);
			if (id == null) {
				outputWriteLineCallback.accept("Error: username not recognized");
				return null;
			}
			acc = AccountManager.getInstance().getAccount(id);
		}

		String id = null;
		AccountSaveContainer save = null;
		if (args.length >= 1) {
			id = args[0];
			save = ctx.getAccountObject().getSave(id);
			if (save == null)
				id = null;
		}
		if (id == null) {
			outputWriteLineCallback.accept("Error: invalid profile");
			return null;
		}

		// Check args
		if (args.length < 2) {
			outputWriteLineCallback.accept("Error: missing argument: new username");
			return null;
		}
		String username = args[1];

		// Check validity
		if (!manager.isValidUsername(username)) {
			// Invalid name
			outputWriteLineCallback.accept("Error: invalid username");
			return null;
		}

		// Check filters
		if (TextFilterService.getInstance().isFiltered(username, true)) {
			// Invalid name
			outputWriteLineCallback.accept("Error: invalid profile name");
			return null;
		}

		// Check if in use
		boolean inUse = false;
		if (!acc.getUsername().equalsIgnoreCase(username) && manager.isUsernameTaken(username)) {
			inUse = true;
		} else {
			// Check if in use by any saves
			AccountObject accF = acc;
			if (Stream.of(acc.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
				try {
					return t.getUsername().equalsIgnoreCase(username) && t.getSaveData().entryExists("avatar");
				} catch (IOException e) {
					return false;
				}
			})) {
				inUse = true;
			}
		}
		if (inUse) {
			// Invalid name
			outputWriteLineCallback.accept("Error: new username already in use");
			return null;
		}

		// Set username
		save.updateUsername(username);

		// Update avatar
		try {
			JsonElement avEle = save.getSaveData().getEntry("avatar");
			if (avEle != null) {
				// Pull avatar
				XmlMapper mapper = new XmlMapper();
				mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
				ObjectNode aviData = mapper.readValue(avEle.getAsString(), ObjectNode.class);

				// Set name
				aviData.set("DisplayName", new TextNode(username));

				// Save
				save.getSaveData().setEntry("avatar",
						new JsonPrimitive(mapper.writer().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
								.withRootName("AvatarData").writeValueAsString(aviData)));
			}
		} catch (IOException e) {
			logger.error("Failed to update avatar for username change of save " + save.getSaveID(), e);
			return "An unknown error occured";
		}

		// Return
		return "Username updated successfully";
	}

}
