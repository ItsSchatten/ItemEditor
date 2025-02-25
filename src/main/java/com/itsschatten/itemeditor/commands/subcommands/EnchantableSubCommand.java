package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class EnchantableSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie enchantable <secondary><value></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be enchantable or not with it's enchantability.
                \s
                ◼ <secondary><value><optional></secondary> The enchantibility of the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie enchantable "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("enchantable")
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (stack.hasData(DataComponentTypes.ENCHANTABLE)) {
                                final Enchantable enchantable = stack.getData(DataComponentTypes.ENCHANTABLE);
                                Utils.tell(context, "<primary>Your item is <secondary>enchantable</secondary> with a value of <secondary>" + enchantable.value() + "</secondary>.");
                            } else {
                                Utils.tell(user, "<primary>Your item is <secondary>not enchantable</secondary>!");
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

                            stack.resetData(DataComponentTypes.ENCHANTABLE);
                            Utils.tell(user, "<primary>Reset the enchantability of your item!");
                            return 1;
                        })
                )
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            stack.unsetData(DataComponentTypes.ENCHANTABLE);
                            Utils.tell(user, "<primary>Your item is no longer enchantable!");
                            return 1;
                        })
                )
                .then(argument("value", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            final int value = IntegerArgumentType.getInteger(context, "value");

                            final Enchantable enchantable = Enchantable.enchantable(value);
                            stack.setData(DataComponentTypes.ENCHANTABLE, enchantable);
                            Utils.tell(user, "<primary>Your item is <secondary>enchantable</secondary> with a value of <secondary>" + value + "</secondary>!");
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

                    final Enchantable enchantable = Enchantable.enchantable(1);
                    stack.setData(DataComponentTypes.ENCHANTABLE, enchantable);
                    Utils.tell(user, "<primary>Your item is <secondary>enchantable</secondary> with a value of <secondary>1</secondary>!");
                    return 1;
                });
    }
}
