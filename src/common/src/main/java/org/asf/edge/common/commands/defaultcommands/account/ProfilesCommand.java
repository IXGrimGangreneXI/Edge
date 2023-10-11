package org.asf.edge.common.commands.defaultcommands.account;

import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.commands.TaskBasedCommand;
import org.asf.edge.common.commands.defaultcommands.account.profilecommands.ProfileCreateCommand;
import org.asf.edge.common.commands.defaultcommands.account.profilecommands.ProfileDeleteCommand;
import org.asf.edge.common.commands.defaultcommands.account.profilecommands.ProfileListCommand;
import org.asf.edge.common.commands.defaultcommands.account.profilecommands.ProfileRenameCommand;
import org.asf.edge.common.commands.defaultcommands.account.profilecommands.ProfileResetLocationCommand;
import org.asf.edge.common.commands.defaultcommands.account.profilecommands.ProfileSelectCommand;
import org.asf.edge.common.permissions.PermissionLevel;

public class ProfilesCommand extends TaskBasedCommand {

	@Override
	public String id() {
		return "profiles";
	}

	@Override
	public String description(CommandContext ctx) {
		return "profile selection and management";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles";
	}

	@Override
	public IEdgeServerCommand[] tasks() {
		return new IEdgeServerCommand[] {

				new ProfileListCommand(),

				new ProfileCreateCommand(),

				new ProfileRenameCommand(),

				new ProfileDeleteCommand(),

				new ProfileSelectCommand(),

				new ProfileResetLocationCommand()

		};
	}

}
