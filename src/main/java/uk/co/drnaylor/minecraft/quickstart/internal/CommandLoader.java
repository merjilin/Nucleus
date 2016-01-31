package uk.co.drnaylor.minecraft.quickstart.internal;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.spi.Message;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.minecraft.quickstart.QuickStart;
import uk.co.drnaylor.minecraft.quickstart.commands.afk.AFKCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.core.QuickStartCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.environment.SetTimeCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.environment.TimeCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.environment.WeatherCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.kick.KickAllCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.kick.KickCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.mail.MailCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.message.MessageCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.message.ReplyCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.message.SocialSpyCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.misc.GodCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.mute.CheckMuteCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.mute.MuteCommand;
import uk.co.drnaylor.minecraft.quickstart.commands.warp.WarpsCommand;
import uk.co.drnaylor.minecraft.quickstart.config.CommandsConfig;
import uk.co.drnaylor.minecraft.quickstart.internal.annotations.ConfigCommandAlias;

import java.io.IOException;
import java.util.Set;

public class CommandLoader {
    private final QuickStart quickStart;
    private final BaseLoader<CommandBase> base = new BaseLoader<>();

    public CommandLoader(QuickStart quickStart) {
        this.quickStart = quickStart;
    }

    private Set<Class<? extends CommandBase>> getCommands() {
        Set<Class<? extends CommandBase>> cmds = Sets.newHashSet();
        cmds.add(QuickStartCommand.class);

        // AFK
        cmds.add(AFKCommand.class);

        // Warps
        cmds.add(WarpsCommand.class);

        // Chat
        cmds.add(MuteCommand.class);
        cmds.add(CheckMuteCommand.class);

        // Messages
        cmds.add(MessageCommand.class);
        cmds.add(ReplyCommand.class);
        cmds.add(SocialSpyCommand.class);

        // Kick
        cmds.add(KickAllCommand.class);
        cmds.add(KickCommand.class);

        // Environment
        cmds.add(WeatherCommand.class);
        cmds.add(TimeCommand.class);

        // Misc
        cmds.add(GodCommand.class);

        // Mail
        cmds.add(MailCommand.class);

        return cmds;
    }

    public void loadCommands() {
        Set<Class<? extends CommandBase>> commandsToLoad = base.filterOutModules(getCommands());
        Injector injector = quickStart.getInjector();

        // Commands config!

        CommandsConfig cc = quickStart.getConfig(CommandsConfig.class).get();
        CommentedConfigurationNode sn = SimpleCommentedConfigurationNode.root();
        commandsToLoad.stream().map(x -> {
            try {
                CommandBase cb = x.newInstance();
                injector.injectMembers(cb);
                return cb;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(x -> x != null).forEach(c -> {
            // Merge in config defaults.
            if (c.mergeDefaults()) {
                sn.getNode(c.getAliases()[0].toLowerCase()).setValue(c.getDefaults());
            }

            // Register the commands.
            Sponge.getCommandManager().register(quickStart, c.createSpec(), c.getAliases());
        });

        try {
            cc.mergeDefaults(sn);
            cc.save();
        } catch (IOException | ObjectMappingException e) {
            quickStart.getLogger().error("Could not save defaults.");
            e.printStackTrace();
        }
    }
}
