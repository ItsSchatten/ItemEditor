package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ModelSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie model <secondary><data></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the model of the item.
                <gray><i>Pass null to remove the model.</i></gray>
                \s
                ◼ <secondary><data><required></secondary> The key for the item model to assign.
                ◼ <secondary>[-view]<optional></secondary> View the item's current custom model data.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie model "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("model")
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

                            if (meta.hasItemModel()) {
                                // We can safely assume that getItemModel returns SOMETHING here.
                                Utils.tell(user, "<primary>Your item's model is <secondary>" + Objects.requireNonNull(meta.getItemModel()).key().asMinimalString() + "</secondary>!");
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have an item model!");
                            }

                            return 1;
                        })
                )
                .then(literal("null")
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

                            meta.setItemModel(null);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Cleared your item's model!");
                            return 1;
                        })
                )
                .then(argument("data", ArgumentTypes.namespacedKey())
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

                            final NamespacedKey data = context.getArgument("data", NamespacedKey.class);
                            meta.setItemModel(data);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Updated your item's model to <secondary>" + data + "</secondary>!");
                            return 1;
                        })
                );
    }
}
