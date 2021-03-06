/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.kit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.events.NucleusKitEvent;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.datamodules.KitUserDataModule;
import io.github.nucleuspowered.nucleus.modules.kit.events.KitEvent;
import io.github.nucleuspowered.nucleus.modules.kit.handlers.KitHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Allows a user to redeem a kit.
 *
 * Command Usage: /kit Permission: plugin.kit.base
 */
@Permissions(suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand("kit")
@NoCooldown // This is determined by the kit itself.
@NoCost // This is determined by the kit itself.
@EssentialsEquivalent(value = "kit, kits", isExact = false, notes = "'/kit' redeems, '/kits' lists.")
public class KitCommand extends AbstractCommand<Player> {

    private final String kit = "kit";

    @Inject private KitHandler kitConfig;
    @Inject private KitConfigAdapter kca;
    @Inject private UserDataManager userConfigLoader;
    @Inject private EconHelper econHelper;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(new KitArgument(Text.of(kit), kca, kitConfig, true))};
    }

    @Override
    protected Map<String, PermissionInformation> permissionsToRegister() {
        Map<String, PermissionInformation> pi = Maps.newHashMap();
        pi.put(PermissionRegistry.PERMISSIONS_PREFIX + "kits",
                PermissionInformation.getWithTranslation("permission.kits", SuggestedLevel.ADMIN));
        return pi;
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> pi = Maps.newHashMap();
        pi.put("exempt.cooldown", PermissionInformation.getWithTranslation("permission.kit.exempt.cooldown", SuggestedLevel.ADMIN));
        pi.put("exempt.onetime", PermissionInformation.getWithTranslation("permission.kit.exempt.onetime", SuggestedLevel.ADMIN));
        return pi;
    }

    @Override
    public CommandResult executeCommand(Player player, CommandContext args) throws Exception {
        KitArgument.KitInfo kitInfo = args.<KitArgument.KitInfo>getOne(kit).get();
        KitUserDataModule user = userConfigLoader.get(player.getUniqueId()).get().get(KitUserDataModule.class);
        Kit kit = kitInfo.kit;
        String kitName = kitInfo.name;
        Instant now = Instant.now();

        double cost = kitInfo.kit.getCost();
        if (permissions.testCostExempt(player)) {
            // If exempt - no cost.
            cost = 0;
        }

        // If we have a cost for the kit, check we have funds.
        if (cost > 0 && !econHelper.hasBalance(player, cost)) {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.notenough", kitName, econHelper.getCurrencySymbol(cost)));
            return CommandResult.empty();
        }

        // If the kit was used before...
        Optional<Instant> oi = Util.getValueIgnoreCase(user.getKitLastUsedTime(), kitName);
        if (oi.isPresent()) {

            // if it's one time only and the user does not have an exemption...
            if (kit.isOneTime() && !player.hasPermission(permissions.getPermissionWithSuffix("exempt.onetime"))) {
                // tell the user.
                player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.onetime.alreadyredeemed", kitName));
                return CommandResult.empty();
            }

            // If we have a cooldown for the kit, and we don't have permission to
            // bypass it...
            if (!permissions.testCooldownExempt(player) && kit.getInterval().getSeconds() > 0) {

                // ...and we haven't reached the cooldown point yet...
                Instant timeForNextUse = oi.get().plus(kit.getInterval());
                if (timeForNextUse.isAfter(now)) {
                    Duration d = Duration.between(now, timeForNextUse);

                    // tell the user.
                    player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.cooldown", Util.getTimeStringFromSeconds(d.getSeconds()), kitName));
                    return CommandResult.empty();
                }
            }
        }

        // Kit pre redeem
        Cause cause = Cause.of(NamedCause.owner(player));
        NucleusKitEvent.Redeem.Pre preEvent = new KitEvent.PreRedeem(cause, oi.orElse(null), kitInfo.name, kitInfo.kit, player);
        if (Sponge.getEventManager().post(preEvent)) {
            throw new ReturnMessageException(preEvent.getCancelMessage()
                .orElseGet(() -> (plugin.getMessageProvider().getTextMessageWithFormat("command.kit.cancelledpre", kitName))));
        }

        boolean mustConsumeAll = kca.getNodeOrDefault().isMustGetAll();
        boolean dropItems = kca.getNodeOrDefault().isDropKitIfFull();

        List<Optional<ItemStackSnapshot>> slotList = Lists.newArrayList();
        Util.getStandardInventory(player).slots().forEach(x -> slotList.add(x.peek().map(ItemStack::createSnapshot)));

        Tristate tristate = Util.addToStandardInventory(player, kit.getStacks(), !mustConsumeAll && dropItems, kca.getNodeOrDefault().isProcessTokens());
        if (tristate != Tristate.TRUE) {
            if (mustConsumeAll) {
                Inventory inventory = Util.getStandardInventory(player);

                // Slots
                Iterator<Inventory> slot = inventory.slots().iterator();

                // Slots to restore
                slotList.forEach(x -> {
                    Inventory i = slot.next();
                    i.clear();
                    x.ifPresent(y -> i.offer(y.createStack()));
                });

                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.fullinventorynosave", kitName));
            }

            if (dropItems) {
                player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.itemsdropped"));
            } else {
                player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.fullinventory"));
            }
        }

        // If something was consumed, consider a success.
        if (dropItems || tristate != Tristate.FALSE) {
            // Charge, if necessary
            if (cost > 0 && econHelper.economyServiceExists()) {
                econHelper.withdrawFromPlayer(player, cost);
            }

            kit.redeemKitCommands(player);

            // Register the last used time. Do it for everyone, in case
            // permissions or cooldowns change later
            user.addKitLastUsedTime(kitName, now);

            Sponge.getEventManager().post(new KitEvent.PostRedeem(cause, oi.orElse(null), kitInfo.name, kitInfo.kit, player));
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.spawned", kitName));
            return CommandResult.success();
        } else {
            // Failed.
            Sponge.getEventManager().post(new KitEvent.FailedRedeem(cause, oi.orElse(null), kitInfo.name, kitInfo.kit, player));
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.fail", kitName));
            return CommandResult.empty();
        }
    }
}
