package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class TypeSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie type <secondary><material></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Changes the material of your item.
                        <info>Item components supplied are ignored.</info>
                        \s
                        ◼ <secondary><material><required></secondary> The material to update the item to.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie type "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("type")
                .then(Commands.argument("material", ArgumentTypes.itemStack())
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

                            final Material material = context.getArgument("material", ItemStack.class).getType();

                            // Make sure it's an item material.
                            if (!material.isItem()) {
                                Utils.tell(user, "<red>The material you specified is not a item.");
                                return 0;
                            }

                            // Update the item to a copy of the current stack with the new material.
                            user.getInventory().setItemInMainHand(stack.withType(material));
                            Utils.tell(user, "<primary>Updated your item's material to <secondary>" + material.getKey().getKey() + "</secondary>!");
                            return 1;
                        })
                );
    }
}
