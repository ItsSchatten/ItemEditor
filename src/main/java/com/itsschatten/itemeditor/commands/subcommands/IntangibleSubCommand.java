package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class IntangibleSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie intangible <secondary><true|false></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be intangible or not when fired.
                <gray>Intangible projectiles may only be picked up by a creative mode player.</gray>
                \s
                ◼ <secondary><true|false><optional></secondary> true/false to determine if it's intangible or not, if not provided it will toggle the status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie intangible "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("intangible")
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            Utils.tell(user, "<primary>Your item is <secondary>currently" + (stack.hasData(DataComponentTypes.INTANGIBLE_PROJECTILE) ? "" : " not") + "</secondary> intangible!");
                            return 1;
                        })
                )
                .then(argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            final boolean value = BoolArgumentType.getBool(context, "value");
                            // No change required.
                            if (stack.hasData(DataComponentTypes.INTANGIBLE_PROJECTILE) && value) {
                                Utils.tell(user, "<primary>Your item is <secondary>already" + (stack.hasData(DataComponentTypes.INTANGIBLE_PROJECTILE) && value ? "" : " not") + "</secondary> intangible!");
                                return 1;
                            }

                            if (value) {
                                stack.setData(DataComponentTypes.INTANGIBLE_PROJECTILE);
                                Utils.tell(user, "<primary>Your item is <secondary>now</secondary> intangible!");
                            } else {
                                stack.unsetData(DataComponentTypes.INTANGIBLE_PROJECTILE);
                                Utils.tell(user, "<primary>Your item is <secondary>no longer</secondary> intangible!");
                            }
                            return 1;
                        })
                )
                .executes(context -> {
                    final Player user = (Player) context.getSource().getSender();
                    // Get the item stack in the user's main hand.
                    final ItemStack stack = user.getInventory().getItemInMainHand();
                    if (ItemValidator.isInvalid(stack)) {
                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                        return 0;
                    }

                    if (stack.hasData(DataComponentTypes.INTANGIBLE_PROJECTILE)) {
                        stack.unsetData(DataComponentTypes.INTANGIBLE_PROJECTILE);
                        Utils.tell(user, "<primary>Your item is <secondary>no longer</secondary> intangible!");
                    } else {
                        stack.setData(DataComponentTypes.INTANGIBLE_PROJECTILE);
                        Utils.tell(user, "<primary>Your item is <secondary>now</secondary> intangible!");
                    }
                    return 1;
                });
    }
}
