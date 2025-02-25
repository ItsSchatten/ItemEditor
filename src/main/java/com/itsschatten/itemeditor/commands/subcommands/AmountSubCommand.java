package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public final class AmountSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie amount <secondary><amount></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Set the amount of your item.
                        <gray>The amount is maxed to the item's default max stack size or custom max stack size.</gray>
                        \s
                        ◼ <secondary><amount><required></secondary> The amount to update the stack.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie amount "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("amount")
                .then(literal("max")
                        .then(literal("-view").executes(this::handleMaxView))
                        .then(literal("-clear")
                                .executes(context -> updateMaxStackSize(context, (stack, meta) -> {
                                    meta.setMaxStackSize(null);
                                    Utils.tell(context.getSource(), "<primary>Reset your item's maximum stack size to <secondary>" + stack.getType().getMaxStackSize() + "</secondary>!");
                                    return meta;
                                }))
                        )
                        .then(argument("maximum", IntegerArgumentType.integer(1, 99))
                                .executes(context -> updateMaxStackSize(context, (stack, meta) -> {
                                    final int amount = IntegerArgumentType.getInteger(context, "maximum");

                                    meta.setMaxStackSize(amount);
                                    Utils.tell(context.getSource(), "<primary>Updated your item's maximum stack size to <secondary>" + amount + "</secondary>!");
                                    return meta;
                                }))
                        )
                        // Default to setting the current amount to the maximum stack size.
                        .executes(context -> updateAmount(context, (stack, meta) -> {
                            final int amount = meta.hasMaxStackSize() ? meta.getMaxStackSize() : stack.getMaxStackSize();
                            Utils.tell(context, "<primary>Updated your item's stack size to <secondary>" + amount + "</secondary>!");
                            return amount;
                        }))
                )
                .then(argument("amount", IntegerArgumentType.integer(1, 99))
                        .executes(context -> updateAmount(context, (stack, meta) -> {
                            final int amount = IntegerArgumentType.getInteger(context, "amount");
                            Utils.tell(context, "<primary>Updated your item's stack size to <secondary>" + amount + "</secondary>!");
                            return amount;
                        }))
                );
    }

    private int updateAmount(final @NotNull CommandContext<CommandSourceStack> context, final BiFunction<ItemStack, ItemMeta, Integer> function) {
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

        stack.setAmount(function.apply(stack, meta));
        return 1;
    }

    private int handleMaxView(@NotNull CommandContext<CommandSourceStack> context) {
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

        if (meta.hasMaxStackSize()) {
            Utils.tell(context.getSource(), "<primary>Your item's max stack size is custom!" +
                    "\nMax stack size: <secondary>" + meta.getMaxStackSize() + "</secondary> <gray>(" + stack.getType().getMaxStackSize() + ")</gray>!");
        } else {
            Utils.tell(context.getSource(), "<primary>Your item's max stack size is vanilla." +
                    "\nMax stack size: <secondary>" + stack.getType().getMaxStackSize() + "</secondary>!");
        }
        return 1;
    }

    private int updateMaxStackSize(final @NotNull CommandContext<CommandSourceStack> context, final BiFunction<ItemStack, ItemMeta, ItemMeta> function) {
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

        stack.setItemMeta(function.apply(stack, meta));
        return 1;
    }

}
