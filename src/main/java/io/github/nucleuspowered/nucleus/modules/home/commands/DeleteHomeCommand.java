/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.argumentparsers.HomeArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.home.handlers.HomeHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;

@Permissions(mainOverride = "home", suggestedLevel = SuggestedLevel.USER)
@NoCooldown
@NoCost
@NoWarmup
@RegisterCommand({"deletehome", "delhome"})
@EssentialsEquivalent({"delhome", "remhome", "rmhome"})
public class DeleteHomeCommand extends AbstractCommand<Player> {

    private final String homeKey = "home";

    @Inject private CoreConfigAdapter cca;
    @Inject private HomeHandler homeHandler;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(new HomeArgument(Text.of(homeKey), plugin, cca))};
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        Home wl = args.<Home>getOne(homeKey).get();

        homeHandler.removeHomeInternal(Cause.of(NamedCause.owner(src)), wl);
        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.home.delete.success", wl.getName()));
        return CommandResult.success();
    }
}
