package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

public final class ArmorTrimSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie trim <secondary><trim|-clear> <material></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the armor trim on an armor item.
                \s
                ◼ <secondary><-clear><required></secondary> Remove the armor trim from the item.
                ◼ <secondary><trim><required></secondary> The armor trim to apply.
                ◼ <secondary><material><required></secondary> The armor trim material.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie trim "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("trim")
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null,
                            // it really shouldn't be but safety.
                            if (!(stack.getItemMeta() instanceof final ArmorMeta meta)) {
                                Utils.tell(user, "<red>You are not holding a piece of armor that can have trim applied to it!");
                                return 0;
                            }

                            meta.setTrim(null);
                            stack.setItemMeta(meta);

                            Utils.tell(user, "<primary>Your armor's trim has been removed.");
                            return 1;
                        })
                )
                .then(argument("pattern", ArgumentTypes.resource(RegistryKey.TRIM_PATTERN))
                        .then(argument("material", ArgumentTypes.resource(RegistryKey.TRIM_MATERIAL))
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    // Get the item's meta and check if it's null,
                                    // it really shouldn't be but safety.
                                    if (!(stack.getItemMeta() instanceof final ArmorMeta meta)) {
                                        Utils.tell(user, "<red>You are not holding a piece of armor that can have trim applied to it!");
                                        return 0;
                                    }

                                    final TrimPattern pattern = context.getArgument("pattern", TrimPattern.class);
                                    final TrimMaterial material = context.getArgument("material", TrimMaterial.class);

                                    final ArmorTrim trim = new ArmorTrim(material, pattern);
                                    meta.setTrim(trim);
                                    stack.setItemMeta(meta);

                                    Utils.tell(user, StringUtil.color("<primary>Set your armor's trim to").appendSpace()
                                            .append(pattern.description().color(TextColor.fromHexString("#D8D8F6"))).appendSpace()
                                            .append(StringUtil.color("<primary>with the material ")).append(material.description()).append(StringUtil.color("<primary>.")));
                                    return 1;
                                })
                        )
                );
    }
}
