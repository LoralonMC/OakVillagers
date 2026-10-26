package dev.oakheart.oakvillagers.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.oakheart.command.CommandRegistrar;
import dev.oakheart.message.MessageManager;
import dev.oakheart.oakvillagers.OakVillagers;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class OakVillagersCommand {

    private final OakVillagers plugin;
    private final MessageManager messageManager;

    public OakVillagersCommand(OakVillagers plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public void register() {
        CommandRegistrar.register(plugin, buildCommand(), "OakVillagers admin commands", List.of());
    }

    private LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("oakvillagers")
                .requires(src -> src.getSender().hasPermission("oakvillagers.reload"))
                // /oakvillagers reload
                .then(Commands.literal("reload")
                        .requires(src -> src.getSender().hasPermission("oakvillagers.reload"))
                        .executes(ctx -> executeReload(ctx.getSource().getSender())))
                .build();
    }

    private int executeReload(CommandSender sender) {
        try {
            if (plugin.reloadPlugin()) {
                messageManager.sendCommand(sender, "reload-success");
            } else {
                messageManager.sendCommand(sender, "reload-failed");
            }
        } catch (Exception e) {
            messageManager.sendCommand(sender, "reload-failed");
        }
        return Command.SINGLE_SUCCESS;
    }
}
