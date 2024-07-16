package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie model <secondary><data></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the custom model data of the item.
                <gray><i>Pass null to remove the custom data.</i></gray>
                \s
                ◼ <secondary><data><required></secondary> The number to assign as the custom model data.
                ◼ <secondary>[-view]<optional></secondary> View the item's current custom model data.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie model "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("model")
                .then(Commands.literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (stack.isEmpty()) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            if (meta.hasCustomModelData()) {
                                Utils.tell(user, "<primary>Your item's custom model data is <secondary>" + meta.getCustomModelData() + "</secondary>!");
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have any custom model data!");
                            }

                            return 1;
                        })
                )
                .then(Commands.literal("null")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (stack.isEmpty()) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            meta.setCustomModelData(null);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Cleared your item's custom model data!");
                            return 1;
                        })
                )
                .then(Commands.argument("data", IntegerArgumentType.integer())
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (stack.isEmpty()) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            final int data = IntegerArgumentType.getInteger(context, "data");

                            meta.setCustomModelData(data);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Updated your item's custom model data to <secondary>" + data + "</secondary>!");
                            return 1;
                        })
                );
    }
}
