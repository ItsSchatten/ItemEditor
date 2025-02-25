package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.TimeUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SuspiciousStewEffects;
import io.papermc.paper.potion.SuspiciousEffectEntry;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StewSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie stew <secondary><add|remove></secondary>").hoverEvent(StringUtil.color("""
                <primary>Alter a suspicious stew's effects.
                \s
                ◼ <secondary>add <type><required> <duration><optional> <info>(ticks)</info></secondary> Add a potion effect to the stew with a time limit.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie enchantable "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("stew")
                .then(literal("add")
                        .then(argument("potion", ArgumentTypes.resource(RegistryKey.MOB_EFFECT))
                                .then(argument("duration", IntegerArgumentType.integer(-1))
                                        .executes(context -> addStewEffect(context, () -> {
                                            final int duration = IntegerArgumentType.getInteger(context, "duration");
                                            return SuspiciousEffectEntry.create(context.getArgument("potion", PotionEffectType.class), duration <= 0 ? PotionEffect.INFINITE_DURATION : duration);
                                        }))
                                )
                        )
                )
                .then(literal("remove")
                        .then(argument("effect", IntegerArgumentType.integer(1))
                                .executes(context -> removeStewEffect(context, IntegerArgumentType.getInteger(context, "effect")))
                        )
                )
                .then(literal("-clear")
                        .executes(this::clearStewEffect)
                )
                .then(literal("-view")
                        .executes(this::handleView)
                );
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack, item -> item.getType() != Material.SUSPICIOUS_STEW)) {
            Utils.tell(user, "<red>You need to be holding suspicious stew in your hand.");
            return 0;
        }

        if (!stack.hasData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS)) {
            Utils.tell(context, "<primary>Your item doesn't have any suspicious stew effects.");
            return 0;
        }

        Utils.tell(context, "<primary>Your stew has the following effects:");
        stack.getData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS).effects().forEach(entry -> Utils.tell(context, " <primary>◼ <secondary>" + entry.effect().key() + " <gray>(" + TimeUtils.getMinecraftTimeClock(entry.duration()) + ")</gray></secondary>"));
        return 1;
    }

    private int clearStewEffect(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack, item -> item.getType() != Material.SUSPICIOUS_STEW)) {
            Utils.tell(user, "<red>You need to be holding suspicious stew in your hand.");
            return 0;
        }

        if (!stack.hasData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS)) {
            Utils.tell(user, "<red>Your stew doesn't have any effects.");
            return 0;
        }

        stack.unsetData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
        Utils.tell(context, "<primary>Your removed all stew effects from your stew.");
        return 1;
    }

    private int addStewEffect(final @NotNull CommandContext<CommandSourceStack> context, final Supplier<SuspiciousEffectEntry> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack, item -> item.getType() != Material.SUSPICIOUS_STEW)) {
            Utils.tell(user, "<red>You need to be holding suspicious stew in your hand.");
            return 0;
        }

        final SuspiciousStewEffects.Builder builder = stack.hasData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS) ? toBuilder(stack.getData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS)) : SuspiciousStewEffects.suspiciousStewEffects();
        final SuspiciousEffectEntry entry = function.get();
        builder.add(entry);
        stack.setData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, builder.build());
        Utils.tell(context, "<primary>Your added <secondary>" + entry.effect().key() + " <gray>(" + TimeUtils.getMinecraftTimeClock(entry.duration()) + ")</gray></secondary> to the stew.");
        return 1;
    }

    private int removeStewEffect(final @NotNull CommandContext<CommandSourceStack> context, final int location) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack, item -> item.getType() != Material.SUSPICIOUS_STEW)) {
            Utils.tell(user, "<red>You need to be holding suspicious stew in your hand.");
            return 0;
        }

        if (!stack.hasData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS)) {
            Utils.tell(user, "<red>Your stew doesn't have any effects to remove.");
            return 0;
        }

        final List<SuspiciousEffectEntry> effects = new ArrayList<>(stack.getData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS).effects());
        final SuspiciousEffectEntry entry = effects.remove(location);

        stack.setData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.suspiciousStewEffects(effects));
        Utils.tell(context, "<primary>You removed <secondary>" + entry.effect().key() + " <gray>(" + TimeUtils.getMinecraftTimeClock(entry.duration()) + ")</gray></secondary> from the stew.");
        return 1;
    }

    private SuspiciousStewEffects.@NotNull Builder toBuilder(final @NotNull SuspiciousStewEffects effects) {
        return SuspiciousStewEffects.suspiciousStewEffects().addAll(effects.effects());
    }

}
