package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.itsschatten.yggdrasil.commands.arguments.GenericEnumArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class AnimationSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie animation <secondary><type> <duration (ticks)></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Set the attack animation of your item.
                        <gray>The time defaults to 6 game ticks, or roughly 0.3 seconds.</gray>
                        \s
                        ◼ <secondary><type><required></secondary> The animation to use.
                        ◼ <secondary><duration><optional></secondary> The duration of the animation.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie animation "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("animation")
                .then(literal("-view").executes(this::handleView))
                .then(literal("attack")
                        .then(literal("-reset")
                                .executes(context -> consumeItem(context, (stack) -> {
                                    stack.resetData(DataComponentTypes.ATTACK_ANIMATION);
                                    Utils.tell(context.getSource(), "<primary>Reset your item's attack animation to it's default!");
                                }))
                        )
                        .then(literal("-clear")
                                .executes(context -> consumeItem(context, (stack) -> {
                                    stack.unsetData(DataComponentTypes.ATTACK_ANIMATION);
                                    Utils.tell(context.getSource(), "<primary>Removed your item's attack animation to it's default!");
                                }))
                        )
                        .then(argument("type", GenericEnumArgument.generic(SwingAnimation.Animation.class))
                                .then(argument("duration", IntegerArgumentType.integer(1))
                                        .executes(this::updateAttackAnimation)
                                )
                                .executes(this::updateAttackAnimation)
                        )
                )
                .then(literal("interaction")
                        .then(literal("-reset")
                                .executes(context -> consumeItem(context, (stack) -> {
                                    stack.resetData(DataComponentTypes.INTERACT_ANIMATION);
                                    Utils.tell(context.getSource(), "<primary>Reset your item's attack animation to it's default!");
                                }))
                        )
                        .then(literal("-clear")
                                .executes(context -> consumeItem(context, (stack) -> {
                                    stack.unsetData(DataComponentTypes.INTERACT_ANIMATION);
                                    Utils.tell(context.getSource(), "<primary>Removed your item's attack animation to it's default!");
                                }))
                        )
                        .then(argument("type", GenericEnumArgument.generic(SwingAnimation.Animation.class))
                                .then(argument("duration", IntegerArgumentType.integer(1))
                                        .executes(this::updateInteractAnimation)
                                )
                                .executes(this::updateInteractAnimation)
                        )
                );
    }

    private int consumeItem(final @NotNull CommandContext<CommandSourceStack> context, Consumer<ItemStack> consumer) {
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

        consumer.accept(stack);
        return 1;
    }

    private int updateAttackAnimation(final @NotNull CommandContext<CommandSourceStack> context) {
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

        final SwingAnimation.Animation animation = context.getArgument("type", SwingAnimation.Animation.class);
        int duration;
        try {
            duration = IntegerArgumentType.getInteger(context, "duration");
        } catch (Exception ignored) {
            duration = 6;
        }
        stack.setData(DataComponentTypes.ATTACK_ANIMATION, SwingAnimation.swingAnimation().type(animation).duration(duration).build());
        Utils.tell(user, "<primary>Updated your item's attack animation to <secondary>" + animation.name() + "</secondary> for <secondary>" + duration + " ticks</secondary>!");
        return 1;
    }

    private int updateInteractAnimation(final @NotNull CommandContext<CommandSourceStack> context) {
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

        final SwingAnimation.Animation animation = context.getArgument("type", SwingAnimation.Animation.class);
        int duration;
        try {
            duration = IntegerArgumentType.getInteger(context, "duration");
        } catch (Exception ignored) {
            duration = 6;
        }
        stack.setData(DataComponentTypes.INTERACT_ANIMATION, SwingAnimation.swingAnimation().type(animation).duration(duration).build());
        Utils.tell(user, "<primary>Updated your item's interaction animation to <secondary>" + animation.name() + "</secondary> for <secondary>" + duration + " ticks</secondary>!");
        return 1;
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
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

        final SwingAnimation attack = stack.getData(DataComponentTypes.ATTACK_ANIMATION);
        final SwingAnimation interact = stack.getData(DataComponentTypes.INTERACT_ANIMATION);
        if (attack != null)
            Utils.tell(user, "<primary>Attack animation: <secondary>" + attack.type().name() + "</secondary> for <secondary>" + attack.duration() + " ticks</secondary>!");
        else
            Utils.tell(user, "<primary>Your item doesn't have an attack animation set.");

        if (interact != null)
            Utils.tell(user, "<primary>Interact animation: <secondary>" + interact.type().name() + "</secondary> for <secondary>" + interact.duration() + " ticks</secondary>!");
        else
            Utils.tell(user, "<primary>Your item doesn't have an interact animation set.");
        return 1;
    }

}
