package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ShowSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie show <secondary><flag></secondary>").hoverEvent(StringUtil.color("""
                <primary>Removes an item flag from your item.
                \s
                ◼ <secondary><flag><required></secondary> The item flag to remove.
                ◼ <secondary>[-all]<optional></secondary> Remove all item flags from the item.
                ◼ <secondary>[-view]<optional></secondary> View all your item's current flags.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("show")
                .then(Commands.literal("-all")
                        .executes(context -> updateItemFlags(context, meta -> {
                            meta.removeItemFlags(ItemFlag.values());
                            Utils.tell(context.getSource(), "<primary>Removed all item flags to your item.");
                            return meta;
                        }))
                )
                .then(Commands.literal("-view").executes(this::handleView))
                .then(Commands.argument("flag", GenericEnumArgument.generic(ItemFlag.class))
                        .executes(context -> updateItemFlags(context, meta -> {
                            final ItemFlag flag = context.getArgument("flag", ItemFlag.class);

                            if (!meta.hasItemFlag(flag)) {
                                Utils.tell(context.getSource(), "<red>Your item does not have the item flag: <yellow>" + flag.name());
                                return null;
                            }

                            meta.removeItemFlags(flag);
                            Utils.tell(context.getSource(), "<primary>Removed <secondary>" + flag.name().toLowerCase().replace("_", " ") + "</secondary> item flag from your item.");
                            return meta;
                        }))
                )
                ;
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        if (meta.getItemFlags().isEmpty()) {
            Utils.tell(user, "<primary>Your item doesn't have any item flags.");
            return 1;
        }

        Utils.tell(user, "<primary>Your item currently has the following item flags: <#D8D8F6>" + String.join("<gray>,</gray> ", meta.getItemFlags().stream()
                .map((flag) -> "<click:suggest_command:'/ie show " + flag.name().toLowerCase() + "'><hover:show_text:'<gray><i>Click to suggest the command to remove this flag!'>" + flag.name().toLowerCase().replace("_", " ") + "</hover></click>").toList()));
        Utils.tell(user, "<gray><i>Click a flag above to suggest the command to remove it!");
        return 1;
    }

    private int updateItemFlags(final @NotNull CommandContext<CommandSourceStack> context, final Function<ItemMeta, ItemMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        final ItemMeta newMeta = function.apply(meta);
        if (newMeta == null) return 0;

        stack.setItemMeta(newMeta);
        return 1;
    }

}