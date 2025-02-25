package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public final class HideTooltipSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie hidetooltip <secondary><true|false></secondary>").hoverEvent(StringUtil.color("""
                <primary>Hides the item's entire tooltip when hovered in an inventory..
                \s
                ◼ <secondary><true|false><optional></secondary> Set whether to hide the tooltip or not.
                ◼ <secondary>[-view]<optional></secondary> View the item's current hide tooltip status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("hidetooltip")
                .then(argument("value", BoolArgumentType.bool())
                        .executes(context -> updateTooltip(context, meta -> {
                            final boolean value = BoolArgumentType.getBool(context, "value");

                            // Set it the value.
                            meta.setHideTooltip(value);
                            Utils.tell(context.getSource(), "<primary>Your item's tooltip is <secondary>" + (meta.isHideTooltip() ? "now" : "no longer") + "</secondary> hidden!");
                            return meta;
                        }))
                )
                .then(literal("-view")
                        .executes(this::handleView)
                )
                .executes(context -> updateTooltip(context, meta -> {
                    meta.setHideTooltip(!meta.isHideTooltip());
                    Utils.tell(context.getSource(), "<primary>Your item's tooltip is <secondary>" + (meta.isHideTooltip() ? "now" : "no longer") + "</secondary> hidden!");
                    return meta;
                }));
    }

    private int updateTooltip(final @NotNull CommandContext<CommandSourceStack> context, final UnaryOperator<ItemMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
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
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        Utils.tell(user, "<primary>Your item's full tooltip is <secondary>currently" + (meta.isHideTooltip() ? "" : " not") + "</secondary> hidden!");
        return 1;
    }
}
