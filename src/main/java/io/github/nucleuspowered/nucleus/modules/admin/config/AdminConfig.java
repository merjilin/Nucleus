/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class AdminConfig {

    @Setting(value = "broadcast-message-template", comment = "config.broadcast.template")
    private BroadcastConfig broadcastMessage = new BroadcastConfig();

    // TODO: Make the default true in future versions?
    @Setting(value = "separate-gamemode-permissions", comment = "config.gamemode.separate")
    private boolean separateGamemodePermission = false;

    public BroadcastConfig getBroadcastMessage() {
        return broadcastMessage;
    }

    public boolean isSeparateGamemodePermission() {
        return separateGamemodePermission;
    }
}
