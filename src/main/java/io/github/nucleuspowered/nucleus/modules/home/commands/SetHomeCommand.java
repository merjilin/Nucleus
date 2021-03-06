/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.api.exceptions.NucleusException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.api.service.NucleusHomeService;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.home.handlers.HomeHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

@SuppressWarnings("ALL")
@Permissions(prefix = "home", mainOverride = "set", suggestedLevel = SuggestedLevel.USER)
@RegisterCommand({"homeset", "sethome"})
@EssentialsEquivalent({"sethome", "createhome"})
public class SetHomeCommand extends AbstractCommand<Player> {

    private final String homeKey = "home";

    @Inject private HomeHandler homeHandler;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("o", "-overwrite").buildWith(
                GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.string(Text.of(homeKey))))
            )
        };
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("unlimited", PermissionInformation.getWithTranslation("permission.homes.unlimited", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        // Get the home key.
        String home = args.<String>getOne(homeKey).orElse(NucleusHomeService.DEFAULT_HOME_NAME).toLowerCase();

        if (!NucleusHomeService.HOME_NAME_PATTERN.matcher(home).matches()) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.sethome.name"));
        }

        Optional<Home> currentHome = homeHandler.getHome(src, home);
        boolean overwrite = currentHome.isPresent() && args.hasAny("o");
        if (currentHome.isPresent() && !overwrite) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.sethome.seterror", home));
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.sethome.tooverwrite", home).toBuilder()
                .onClick(TextActions.runCommand("/sethome " + home + " -o")).build());
            return CommandResult.empty();
        }

        Cause cause = Cause.of(NamedCause.owner(src));
        try {
            if (overwrite) {
                Home current = currentHome.get();
                homeHandler.modifyHomeInternal(cause, current, src.getLocation(), src.getRotation());
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.sethome.overwrite", home));
            } else {
                homeHandler.createHomeInternal(cause, src, home, src.getLocation(), src.getRotation());
            }
        } catch (NucleusException e) {
            throw new ReturnMessageException(e.getText(), e);
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.sethome.set", home));
        return CommandResult.success();
    }
}
