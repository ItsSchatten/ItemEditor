package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GliderSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie glider <secondary><value></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be a glider (Elytra) or not.
                \s
                ◼ <secondary><value><optional></secondary> The enchantibility of the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie glider "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("glider")
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (stack.hasData(DataComponentTypes.GLIDER)) {
                                Utils.tell(context, "<primary>Your item <secondary>is a glider</secondary>.");
                            } else {
                                Utils.tell(user, "<primary>Your item is <secondary>not a glider</secondary>!");
                            }

                            return 1;
                        })
                )
                .then(literal("-reset")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            stack.resetData(DataComponentTypes.GLIDER);
                            Utils.tell(user, "<primary>Reset if your item is a glider or not!");
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

                            if (value) {
                                stack.setData(DataComponentTypes.GLIDER);
                                Utils.tell(user, "<primary>Your item is <secondary>now</secondary> a glider");
                            } else {
                                stack.unsetData(DataComponentTypes.GLIDER);
                                Utils.tell(user, "<primary>Your item is <secondary>no longer</secondary> a glider");
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

                    if (!stack.hasData(DataComponentTypes.GLIDER)) {
                        stack.setData(DataComponentTypes.GLIDER);
                        Utils.tell(user, "<primary>Your item is <secondary>now</secondary> a glider");
                    } else {
                        stack.unsetData(DataComponentTypes.GLIDER);
                        Utils.tell(user, "<primary>Your item is <secondary>no longer</secondary> a glider");
                    }
                    return 1;
                });
    }
}
