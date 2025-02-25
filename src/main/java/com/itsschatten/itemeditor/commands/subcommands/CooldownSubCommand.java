package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class CooldownSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie cooldown <secondary><group> <seconds></secondary>").hoverEvent(StringUtil.color("""
                        <primary>""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie cooldown "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("cooldown")
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack hand = user.getItemInHand();
                            if (ItemValidator.isInvalid(hand)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (hand.hasData(DataComponentTypes.USE_COOLDOWN)) {
                                final UseCooldown cd = hand.getData(DataComponentTypes.USE_COOLDOWN);
                                // We can assume 'cd' will not be null here, unless something messes up.
                                assert cd != null;
                                final float seconds = cd.seconds();
                                Utils.tell(context, "<primary>Your item's use cooldown group is <secondary>" + cd.cooldownGroup().asMinimalString() + "</secondary> with a time of <secondary>" + (String.valueOf(seconds).endsWith(".0") ? String.valueOf(seconds).replace(".0", "") : seconds) + " seconds</secondary>.");
                            } else {
                                Utils.tell(context, "<primary>Your item doesn't have any use cooldown!");
                            }
                            return 1;
                        })
                )
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack hand = user.getItemInHand();
                            if (ItemValidator.isInvalid(hand)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (hand.hasData(DataComponentTypes.USE_COOLDOWN)) {
                                hand.unsetData(DataComponentTypes.USE_COOLDOWN);
                                Utils.tell(context, "<primary>Your item's use cooldown group has been removed.");
                            } else {
                                Utils.tell(context, "<primary>Your item doesn't have any use cooldown!");
                            }
                            return 1;
                        })
                )
                .then(literal("-reset")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack hand = user.getItemInHand();
                            if (ItemValidator.isInvalid(hand)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (hand.hasData(DataComponentTypes.USE_COOLDOWN)) {
                                hand.resetData(DataComponentTypes.USE_COOLDOWN);
                                Utils.tell(context, "<primary>Your item's use cooldown group has been reset.");
                            } else {
                                Utils.tell(context, "<primary>Your item doesn't have any use cooldown!");
                            }
                            return 1;
                        })
                )
                .then(argument("group", ArgumentTypes.key())
                        .then(argument("seconds", FloatArgumentType.floatArg(0))
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    final ItemStack hand = user.getItemInHand();
                                    if (ItemValidator.isInvalid(hand)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    final float seconds = FloatArgumentType.getFloat(context, "seconds");
                                    final Key group = context.getArgument("group", Key.class);

                                    hand.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(seconds).cooldownGroup(group).build());
                                    Utils.tell(context, "<primary>Set your item's cooldown group to <secondary>" + group.asMinimalString() + "</secondary> with a time of <secondary>" + (String.valueOf(seconds).endsWith(".0") ? String.valueOf(seconds).replace(".0", "") : seconds) + " seconds</secondary>.");
                                    Utils.tell(context, "<gray><i>Cooldowns are not enforced by this plugin, you may have to use another plugin to enforce it!");
                                    return 1;
                                })
                        )
                );
    }
}
