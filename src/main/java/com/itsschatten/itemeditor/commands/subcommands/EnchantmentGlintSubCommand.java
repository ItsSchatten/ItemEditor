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

public class EnchantmentGlintSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie glow <secondary><true|false|yes|no|remove></secondary>").hoverEvent(StringUtil.color("""
                <primary>Overrides the enchantment glint on your item.
                <info>Defaults to toggling between on and off if no arguments.</info>
                \s
                ◼ <secondary><true|false|yes|no><required></secondary> Set the enchantment glint override.
                ◼ <secondary>[-clear]<optional></secondary> Remove the enchantment glint override from the item.
                ◼ <secondary>[-view]<optional></secondary> View the current enchantment glint override for this item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie glow "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("glow")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> handleEnchantmentGlintUpdate(context, meta -> {
                            final Boolean bool = BoolArgumentType.getBool(context, "value");
                            meta.setEnchantmentGlintOverride(bool);

                            Utils.tell(context.getSource(), "<primary>Your item's enchantment glint override has been updated to <secondary>" + bool + "</secondary>!");
                            return meta;
                        }))
                )
                .then(Commands.literal("-clear")
                        .executes(context -> handleEnchantmentGlintUpdate(context, meta -> {
                            meta.setEnchantmentGlintOverride(null);
                            Utils.tell(context.getSource(), "<primary>Your item's enchantment glint override has been <secondary>removed</secondary>!");
                            return meta;
                        }))
                )
                .then(Commands.literal("-view").executes(this::handleView))
                .executes(context -> handleEnchantmentGlintUpdate(context, meta -> {
                    // Set to true if no override, or toggle.
                    meta.setEnchantmentGlintOverride(!meta.hasEnchantmentGlintOverride() || !meta.getEnchantmentGlintOverride());
                    Utils.tell(context.getSource(), "<primary>Your item's enchantment glint override has been updated to <secondary>" + meta.getEnchantmentGlintOverride() + "</secondary>!");
                    return meta;
                }));
    }

    private int handleEnchantmentGlintUpdate(final @NotNull CommandContext<CommandSourceStack> context, Function<ItemMeta, ItemMeta> function) {
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

        if (!meta.hasEnchantmentGlintOverride()) {
            Utils.tell(user, "<primary>Your item does not currently have an override for enchantment glint!");
        } else {
            Utils.tell(user, "<primary>Your item's enchantment glint override is <secondary>currently " + meta.getEnchantmentGlintOverride() + "</secondary>!");
        }
        return 1;
    }
}
