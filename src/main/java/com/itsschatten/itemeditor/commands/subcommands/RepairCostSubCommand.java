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
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

public final class RepairCostSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie repaircost <secondary><cost></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets the repair cost of your item.
                \s
                ◼ <secondary><cost><required></secondary> The repair cost penalty for this item.
                ◼ <secondary>[-view]<optional></secondary> See the current repair cost.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie repaircost "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("repaircost")
                .then(argument("cost", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();

                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            if (!(stack.getItemMeta() instanceof final Repairable meta)) {
                                Utils.tell(user, "<red>Your item cannot be repaired in an anvil!");
                                return 0;
                            }

                            // Set the repair cost.
                            final int repairCost = IntegerArgumentType.getInteger(context, "cost");
                            meta.setRepairCost(repairCost);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Set the repair cost of your item to <secondary>" + repairCost + "</secondary>.");
                            return 1;
                        })
                )
                .then(literal("-view")
                        .executes(this::handleView)
                );
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final Repairable meta)) {
            Utils.tell(user, "<red>Your item cannot be repaired in an anvil!");
            return 0;
        }

        Utils.tell(user, "<primary>Your item's repair cost is currently <secondary>" + meta.getRepairCost() + "</secondary>.");
        return 1;
    }
}
