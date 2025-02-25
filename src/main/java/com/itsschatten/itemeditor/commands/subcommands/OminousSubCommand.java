package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.OminousBottleAmplifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class OminousSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie ominous <secondary><amount></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Set the ominous amplifier of your item.
                        \s
                        ◼ <secondary><amount><required></secondary> The amount to update the amplifier.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie ominous "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("ominous")
                .then(literal("-reset")
                        .executes(this::resetAmplifier)
                )
                .then(literal("-clear")
                        .executes(this::clearAmplifier)
                )
                .then(argument("amp", IntegerArgumentType.integer(1, 5))
                        .executes(context -> updateAmplifier(context, () -> Math.max(IntegerArgumentType.getInteger(context, "amp") - 1, 0)))
                );
    }

    private int resetAmplifier(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.resetData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);
        Utils.tell(context, "<primary>Reset your item's ominous bottle amplifier to default.");
        return 1;
    }

    private int clearAmplifier(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.unsetData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);
        Utils.tell(context, "<primary>Cleared your item's ominous bottle amplifier.");
        return 1;
    }

    private int updateAmplifier(final @NotNull CommandContext<CommandSourceStack> context, final Supplier<Integer> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final OminousBottleAmplifier ampl = OminousBottleAmplifier.amplifier(function.get());
        stack.setData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, ampl);
        Utils.tell(context, "<primary>Set your ominous bottle amplifier to <secondary>" + ampl.amplifier() + "</secondary>.");
        return 1;
    }

}
