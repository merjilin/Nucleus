/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.Optional;

@RegisterCommand({"hat", "head"})
@NoCooldown
@NoWarmup
@NoCost
@Permissions(supportsSelectors = true, supportsOthers = true)
@EssentialsEquivalent({"hat", "head"})
public class HatCommand extends AbstractCommand.SimpleTargetOtherPlayer {

    @Override protected CommandResult executeWithPlayer(CommandSource player, Player pl, CommandContext args, boolean isSelf) throws Exception {
        Optional<ItemStack> helmetOptional = pl.getHelmet();

        ItemStack stack = pl.getItemInHand(HandTypes.MAIN_HAND).orElseThrow(() -> new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.generalerror.handempty")));
        stack.setQuantity(1);
        pl.setHelmet(stack);
        Text itemName = stack.get(Keys.DISPLAY_NAME).orElse(Text.of(Util.getTranslatableIfPresentOnCatalogType(stack.getItem())));

        if (pl.get(Keys.GAME_MODE).get() == GameModes.SURVIVAL) {
            stack = pl.getItemInHand(HandTypes.MAIN_HAND).get();

            if (stack.getQuantity() > 1) {
                stack.setQuantity(stack.getQuantity() - 1);
                pl.setItemInHand(HandTypes.MAIN_HAND, stack);
            } else {
                pl.setItemInHand(HandTypes.MAIN_HAND, null);
            }
        }

        // If the old item can't be placed back in the subject inventory, drop the item.
        helmetOptional.ifPresent(itemStack -> Util.getStandardInventory(pl).offer(itemStack)
                .getRejectedItems().forEach(x -> Util.dropItemOnFloorAtLocation(x, pl.getWorld(), pl.getLocation().getPosition())));

        if (!isSelf) {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat("command.hat.success", plugin.getNameUtil().getName(pl), itemName));
        }

        pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat("command.hat.successself", itemName));
        return CommandResult.success();
    }
}
