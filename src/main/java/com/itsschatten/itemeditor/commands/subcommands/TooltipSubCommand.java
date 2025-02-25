package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class TooltipSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie tooltip <secondary><tooltip></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Set the tooltip style for your item.
                        """).asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie tooltip "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("tooltip")
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack hand = user.getItemInHand();
                            if (ItemValidator.isInvalid(hand)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (hand.hasData(DataComponentTypes.TOOLTIP_STYLE)) {
                                hand.resetData(DataComponentTypes.TOOLTIP_STYLE);
                                Utils.tell(user, "<primary>Removed your item's tooltip style!");
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have a tooltip style!");
                            }

                            return 1;
                        })
                )
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack hand = user.getItemInHand();
                            if (ItemValidator.isInvalid(hand)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (hand.hasData(DataComponentTypes.TOOLTIP_STYLE)) {
                                Utils.tell(user, "<primary>Your item's current tooltip style is <secondary>" + Objects.requireNonNull(hand.getData(DataComponentTypes.TOOLTIP_STYLE)).asMinimalString() + "</secondary>!");
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have a tooltip style!");
                            }

                            return 1;
                        })
                )
                .then(argument("key", ArgumentTypes.key())
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack hand = user.getItemInHand();
                            if (ItemValidator.isInvalid(hand)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            final Key key = context.getArgument("key", Key.class);
                            hand.setData(DataComponentTypes.TOOLTIP_STYLE, key);
                            Utils.tell(user, "<primary>Set your item's tooltip style to <secondary>" + key.asMinimalString() + "</secondary>!");
                            return 1;
                        })
                );
    }
}
