package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

// TODO: Complete this when BlocksAttacks component is properly updated.
public final class BlocksAttacksSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie shield <secondary><variant|pattern|body|body color|pattern color></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the fish variant within a bucket.
                \s
                <gray><i>If holding a tropical fish bucket.</i></gray>
                ◼ <secondary><pattern|tropical pattern><required> [color]<optional></secondary> Set the pattern and, if provided, the color.
                ◼ <secondary><body|bodycolor|patterncolor><required> <color><required></secondary> Set's the color of a body or a pattern.
                ◼ <secondary>[-view]<optional></secondary> View the current pattern, pattern color, and body color.
                <gray><i>If holding an axolotl bucket.</i></gray>
                ◼ <secondary><variant><required></secondary> The variant to set for the axolotl in the bucket.
                ◼ <secondary>[-view]<optional></secondary> View the current variant.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie bucket "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("shield")
                .then(literal("-view")
                        .executes(this::handleView)
                )
                .then(literal("delay"))
                .then(literal("bypassed_by"))
                .then(literal("damage_reduction"))
                .then(literal("disable_scale"))
                .then(literal("damage"))
                .then(literal("sound"))
                .then(literal("disable_sound"))
                ;
    }

    private BlocksAttacks.@NotNull Builder toBuilder(@NotNull BlocksAttacks attacks) {
        return BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(attacks.blockDelaySeconds())
                .blockSound(attacks.blockSound())
                .bypassedBy(attacks.bypassedBy())
                .disableSound(attacks.disableSound())

                //.
                ;
    }

    private int updateBlocksAttacks(final @NotNull CommandContext<CommandSourceStack> context, UnaryOperator<BlocksAttacks.Builder> operator) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.setData(DataComponentTypes.BLOCKS_ATTACKS, operator.apply(!stack.hasData(DataComponentTypes.BLOCKS_ATTACKS) ? BlocksAttacks.blocksAttacks() :
                toBuilder(stack.getData(DataComponentTypes.BLOCKS_ATTACKS))));
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

        return SUCCESS;
    }

}
