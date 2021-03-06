/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.events.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularGeneralService;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnModule;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.spawn.datamodules.SpawnGeneralDataModule;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.scheduler.Task;

import java.util.function.Predicate;

@ConditionalListener(FirstSpawnConditionalListener.Condition.class)
public class FirstSpawnConditionalListener extends ListenerBase {

    @Inject private ModularGeneralService store;

    @Listener(order = Order.LATE)
    public void onJoin(NucleusFirstJoinEvent event, @Getter("getTargetEntity") Player player) {
        // Try to force a subject location in a tick.
        Task.builder().execute(() -> store.get(SpawnGeneralDataModule.class).getFirstSpawn().ifPresent(player::setTransform)).delayTicks(1)
                .submit(plugin);
    }

    public static class Condition implements Predicate<Nucleus> {

        @Override public boolean test(Nucleus nucleus) {
            try {
                return nucleus.getModuleContainer().getConfigAdapterForModule(SpawnModule.ID, SpawnConfigAdapter.class)
                    .getNodeOrDefault().isForceFirstSpawn();
            } catch (Exception e) {
                if (nucleus.isDebugMode()) {
                    e.printStackTrace();
                }

                return false;
            }
        }
    }
}
