package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

public final class RepairableSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie enchantable <secondary><items></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be enchantable or not with it's enchantability.
                \s
                ◼ <secondary><true|false><optional></secondary> If it is a glider or not, if unprovided defaults to toggling.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie enchantable "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("repairable")
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (stack.hasData(DataComponentTypes.REPAIRABLE)) {
                                final Repairable repairable = stack.getData(DataComponentTypes.REPAIRABLE);
                                Utils.tell(context, "<primary>Your item is <secondary>repairable</secondary> with any of the following items:");
                                repairable.types().forEach(item -> Utils.tell(context, " <primary>◼ <secondary>" + item.key().asString()));
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

                            stack.resetData(DataComponentTypes.REPAIRABLE);
                            Utils.tell(user, "<primary>Reset the repairability of your item!");
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

                            stack.unsetData(DataComponentTypes.REPAIRABLE);
                            Utils.tell(user, "<primary>Your item is no longer repairable!");
                            return 1;
                        })
                )
                .then(argument("item", ArgumentTypes.resourceKey(RegistryKey.ITEM))
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            final TypedKey<ItemType> item = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.ITEM, "item");
                            final Repairable repair = Repairable.repairable(RegistrySet.keySet(RegistryKey.ITEM, item));
                            stack.setData(DataComponentTypes.REPAIRABLE, repair);
                            Utils.tell(user, "<primary>Your item is <secondary>repairable</secondary> with a <secondary>" + item.key().asString() + "</secondary>!");
                            return 1;
                        })
                );
    }
}
