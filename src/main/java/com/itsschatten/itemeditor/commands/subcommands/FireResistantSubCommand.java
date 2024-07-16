package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class FireResistantSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie fireresistant <secondary><true|false></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set whether your item is fire resistant or not.
                \s
                ◼ <secondary><true|false><optional></secondary> Set whether your item will burn in lava or fire.
                ◼ <secondary>[-view]<optional></secondary> View the item's current fire resistance status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie fireresistant "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("fireresistant")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> updateFireResistance(context, meta -> {
                            final boolean value = BoolArgumentType.getBool(context, "value");

                            // Set it the value.
                            meta.setFireResistant(value);
                            Utils.tell(context.getSource(), "<primary>Your item is <secondary>" + (meta.isFireResistant() ? "now" : "no longer") + "</secondary> fire resistant!");
                            return meta;
                        }))
                )
                .then(Commands.literal("-view")
                        .executes(this::handleView)
                )
                .executes(context -> updateFireResistance(context, meta -> {
                    meta.setFireResistant(!meta.isFireResistant());
                    Utils.tell(context.getSource(), "<primary>Your item is <secondary>" + (meta.isFireResistant() ? "now" : "no longer") + "</secondary> fire resistant!");
                    return meta;
                }));
    }

    private int updateFireResistance(final @NotNull CommandContext<CommandSourceStack> context, final Function<ItemMeta, ItemMeta> function) {
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

        stack.setItemMeta(function.apply(meta));
        return 1;
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
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

        Utils.tell(user, "<primary>Your item is <secondary>currently" + (meta.isFireResistant() ? "" : " not") + "</secondary> fire resistant!");
        return 1;
    }
}
