package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class UnbreakableSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie unbreakable <secondary><true|false></secondary>").hoverEvent(StringUtil.color("""
                <primary>Sets an item to be unbreakable or not.
                \s
                ◼ <secondary><true|false><optional></secondary> true/false to determine if it's unbreakable or not, if not provided it will toggle the status.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie unbreakable "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("unbreakable")
                .then(literal("-view")
                        .executes(context -> {
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

                            Utils.tell(user, "<primary>Your item is <secondary>currently" + (meta.isUnbreakable() ? "" : " not") + "</secondary> unbreakable!");
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

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            final boolean value = BoolArgumentType.getBool(context, "value");
                            // No change required.
                            if (meta.isUnbreakable() == value) {
                                Utils.tell(user, "<primary>Your item is <secondary>already" + (meta.isUnbreakable() ? "" : " not") + "</secondary> unbreakable!");
                                return 1;
                            }

                            // Set it the value.
                            meta.setUnbreakable(value);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Your item is <secondary>" + (meta.isUnbreakable() ? "now" : "no longer") + "</secondary> unbreakable!");
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

                    // Get the item's meta and check if it's null, it really shouldn't be but safety.
                    final ItemMeta meta = stack.getItemMeta();
                    if (meta == null) {
                        Utils.tell(user, "<red>For some reason the item's meta is null!");
                        return 0;
                    }

                    meta.setUnbreakable(!meta.isUnbreakable());
                    stack.setItemMeta(meta);
                    Utils.tell(user, "<primary>Your item is <secondary>" + (meta.isUnbreakable() ? "now" : "no longer") + "</secondary> unbreakable!");
                    return 1;
                });
    }
}
