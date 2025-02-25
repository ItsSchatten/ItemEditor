package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.itemeditor.utils.StringHelper;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class DeathProtectionSubCommand extends BrigadierCommand {

    // This is admittedly TERRIBLE looking. It needs some serious refactoring.

    @NotNull
    public static String consumeEffectAsHover(@NotNull ConsumeEffect consumeEffect) {
        final StringBuilder builder = new StringBuilder();

        switch (consumeEffect) {
            case final ConsumeEffect.ApplyStatusEffects effect ->
                    builder.append("<hover:show_text:'").append(DeathProtectionSubCommand.consumeEffectToString(effect)).append("'><secondary>").append("Apply Status Effects").append("</secondary>");
            case final ConsumeEffect.ClearAllStatusEffects effect ->
                    builder.append("<hover:show_text:'").append(DeathProtectionSubCommand.consumeEffectToString(effect)).append("'><secondary>").append("Clear All Effects").append("</secondary>");
            case final ConsumeEffect.PlaySound effect ->
                    builder.append("<hover:show_text:'").append(DeathProtectionSubCommand.consumeEffectToString(effect)).append("'><secondary>").append("Play Sound").append("</secondary>");
            case final ConsumeEffect.RemoveStatusEffects effect ->
                    builder.append("<hover:show_text:'").append(DeathProtectionSubCommand.consumeEffectToString(effect)).append("'><secondary>").append("Remove Stats Effects").append("</secondary>");
            case final ConsumeEffect.TeleportRandomly effect ->
                    builder.append("<hover:show_text:'").append(DeathProtectionSubCommand.consumeEffectToString(effect)).append("'><secondary>").append("Random Teleport").append("</secondary>");
            default -> throw new IllegalArgumentException("Unknown effect: " + consumeEffect);
        }

        return builder.toString();
    }

    @NotNull
    public static String consumeEffectToString(@NotNull ConsumeEffect consumeEffect) {
        final StringBuilder builder = new StringBuilder();

        switch (consumeEffect) {
            case final ConsumeEffect.ApplyStatusEffects effect -> {
                final List<PotionEffect> effects = effect.effects();
                builder.append("<dark_aqua><b>Type</b> <arrow> <secondary>").append("Apply Status Effects").append("</secondary><br>")
                        .append("<b>Probability</b> <arrow> <secondary>").append(effect.probability() * 100F).append("</secondary><br>")
                        .append("<b>Effects</b> <arrow> <secondary>").append(StringHelper.firstEffect(effects)).append("</secondary><br>");

                if (!effects.isEmpty()) {
                    effects.forEach(potion -> builder.append("<secondary>").append(StringHelper.potionEffectToString(potion)).append("</secondary><br>"));
                }
            }
            case final ConsumeEffect.ClearAllStatusEffects ignored ->
                    builder.append("<dark_aqua><b>Type</b> <arrow> <secondary>").append("Clear All Status Effects").append("</secondary><br>")
                            .append("<primary>All of the consumers effects will be cleared.");
            case final ConsumeEffect.PlaySound effect ->
                    builder.append("<dark_aqua><b>Type</b> <arrow> <secondary>").append("Play Sound").append("</secondary><br>")
                            .append("<b>Sound</b> <arrow> <secondary>").append(effect.sound().asMinimalString()).append("</secondary><br>");
            case final ConsumeEffect.RemoveStatusEffects effect -> {
                final List<PotionEffectType> effects = new ArrayList<>(effect.removeEffects().resolve(Registry.EFFECT));
                builder.append("<dark_aqua><b>Type</b> <arrow> <secondary>").append("Remove Status Effects").append("</secondary><br>")
                        .append("<b>Effects</b> <arrow> <secondary>").append(StringHelper.firstTwoEffectTypes(effects)).append("</secondary><br>");

                if (!effects.isEmpty()) {
                    effects.forEach(type -> builder.append("<secondary>").append(type.key().asMinimalString()).append("</secondary>").append("<gray>,</gray>"));
                }

                final int last = builder.lastIndexOf("<gray>,");
                if (last != -1)
                    builder.replace(last, builder.lastIndexOf("</gray>") + 7, "");
            }
            case final ConsumeEffect.TeleportRandomly effect -> {
                final String diameter = String.valueOf(effect.diameter());
                builder.append("<dark_aqua><b>Type</b> <arrow> <secondary>").append("Random Teleport").append("</secondary><br>")
                        .append("<b>Diameter</b> <arrow> <secondary>")
                        .append(StringHelper.conditionString(diameter, diameter.replace(".0", ""), (string) -> string.endsWith(".0")))
                        .append("</secondary><br>");
            }
            default -> throw new IllegalArgumentException("Unknown effect: " + consumeEffect);
        }

        return builder.toString();
    }

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie deathprot <secondary>[...]</secondary>").hoverEvent(StringUtil.color("""
                <primary>
                \s
                ◼ <secondary><gray><i>no arguments</i></gray></secondary> List information about this consumable.
                ◼ <secondary><effects<optional></secondary> List all effects.
                ◼ <secondary><effects menu><required></secondary> Open a menu to apply or remove effects.
                ◼ <secondary><add <type> [...]><required></secondary> Apply an effect to your consumable.
                ◼ <secondary><remove effect><required></secondary> Remove an effect.
                ◼ <secondary>[-view]<optional></secondary> View the item's current name.
                ◼ <secondary>[-clear]<optional></secondary> Clear the name.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie deathprot "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("deathprot")
                .then(literal("-clear")
                        .executes(this::removeDeathProtection)
                )
                .then(literal("-reset")
                        .executes(this::resetItem)
                )
                .then(literal("add")
                        .then(literal("apply_effect")
                                .then(argument("effect", ArgumentTypes.resource(RegistryKey.MOB_EFFECT))
                                        .then(argument("duration", IntegerArgumentType.integer(-1))
                                                .then(argument("amplifier", IntegerArgumentType.integer(0))
                                                        .then(argument("probability", FloatArgumentType.floatArg(0F, 1F))
                                                                .executes(context -> applyComponent(context, consumable -> {
                                                                    final PotionEffectType key = context.getArgument("effect", PotionEffectType.class);
                                                                    final int duration = IntegerArgumentType.getInteger(context, "duration");
                                                                    final int amplifier = IntegerArgumentType.getInteger(context, "amplifier");
                                                                    final float probability = FloatArgumentType.getFloat(context, "probability");
                                                                    return toBuilder(consumable).addEffect(ConsumeEffect.applyStatusEffects(List.of(key.createEffect(duration, amplifier)), probability)).build();
                                                                }))
                                                        )
                                                        .executes(context -> applyComponent(context, consumable -> {
                                                            final PotionEffectType key = context.getArgument("effect", PotionEffectType.class);
                                                            final int duration = IntegerArgumentType.getInteger(context, "duration");
                                                            final int amplifier = IntegerArgumentType.getInteger(context, "amplifier");
                                                            return toBuilder(consumable).addEffect(ConsumeEffect.applyStatusEffects(List.of(key.createEffect(duration, amplifier)), 1F)).build();
                                                        }))
                                                )
                                        )
                                )
                        )
                        .then(literal("remove_effect")
                                .then(argument("effect", ArgumentTypes.resourceKey(RegistryKey.MOB_EFFECT))
                                        .executes(context -> applyComponent(context, consumable -> {
                                                    final TypedKey<PotionEffectType> key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.MOB_EFFECT, "effect");
                                                    return toBuilder(consumable).addEffect(ConsumeEffect.removeEffects(RegistrySet.keySet(RegistryKey.MOB_EFFECT, key))).build();
                                                })
                                        )
                                )
                        )
                        .then(literal("clear_all")
                                .executes(context -> applyComponent(context, (consumable) -> {
                                    Utils.tell(context, "<primary>Your death protection will now clear all effects!");
                                    return toBuilder(consumable).addEffect(ConsumeEffect.clearAllStatusEffects()).build();
                                })))
                        .then(literal("rtp")
                                .then(argument("diameter", FloatArgumentType.floatArg(1.0F))
                                        .executes(context -> applyComponent(context, consumable -> {
                                                    final float diameter = FloatArgumentType.getFloat(context, "diameter");
                                                    Utils.tell(context, "<primary>Added the random teleport effect with a diameter of <secondary>" + diameter + " blocks</secondary>!");
                                                    return toBuilder(consumable).addEffect(ConsumeEffect.teleportRandomlyEffect(diameter)).build();
                                                })
                                        )
                                )
                                .executes(context -> applyComponent(context,
                                        consumable -> {
                                            Utils.tell(context, "<primary>Added the random teleport effect with a diameter of <secondary>5 blocks</secondary>!");
                                            return toBuilder(consumable).addEffect(ConsumeEffect.teleportRandomlyEffect(5)).build();
                                        })
                                )
                        )
                        .executes(context -> handleComponent(context, (consumable, stack) -> {
                            if (consumable.deathEffects().isEmpty()) {
                                Utils.tell(context, "<primary>Your death protection has no effects.");
                                return;
                            }
                            Utils.tell(context, "<primary>Your death protection effects:");
                            consumable.deathEffects().forEach(effect -> Utils.tell(context, " <primary>◼</primary> " + consumeEffectToString(effect)));
                            Utils.tell(context, "<br><gray><i><click:run_command:'/ie consumable effects menu'><hover:show_text:'<gray>Click to run the command '/ie consumable effects menu''>[Click to open Menu]");
                        }))
                )
                .then(literal("-clear")
                        .executes(context -> applyComponent(context, (consumable) -> {
                            final DeathProtection finalConsumable = toBuilder(consumable).build();

                            Utils.tell(context, "<primary>Cleared all effects from your death protection!");
                            return finalConsumable;
                        })))
                .then(literal("remove")
                        .then(argument("position", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isValid(stack, (item) -> ItemValidator.isInvalid(stack) || !stack.hasData(DataComponentTypes.CONSUMABLE))) {
                                        return builder.buildFuture();
                                    }

                                    // We could ignore the nullity of the consumable component here as the above is valid check should block any calls from a null value.
                                    if (stack.getData(DataComponentTypes.DEATH_PROTECTION) != null) {
                                        return SharedSuggestionProvider.suggest(IntStream.range(1, stack.getData(DataComponentTypes.DEATH_PROTECTION).deathEffects().size() + 1).mapToObj(Integer::toString).toList(), builder);
                                    } else {
                                        return builder.buildFuture();
                                    }
                                })
                                .executes(context -> applyComponent(context, consumable -> {
                                    final List<ConsumeEffect> effects = new ArrayList<>(consumable.deathEffects());
                                    final int position = IntegerArgumentType.getInteger(context, "position");
                                    if (effects.size() < position) {
                                        if (effects.isEmpty())
                                            Utils.tell(context, "<red>Your consumable doesn't have any effects!");
                                        else
                                            Utils.tell(context, "<red>Your consumable doesn't have that many effects! Must be in range from 1 to " + effects.size());
                                        return consumable;
                                    }

                                    final ConsumeEffect effect = effects.remove(position - 1);

                                    if (effect != null) {
                                        final DeathProtection consume = toBuilder(consumable).addEffects(effects).build();
                                        Utils.tell(context, "<primary>Removed <secondary>" + consumeEffectToString(effect) + "</secondary> from your item!");
                                        return consume;
                                    }

                                    return consumable;
                                }))
                        )
                )
                .executes(context -> {
                    final Player player = (Player) context.getSource().getSender();
                    final ItemStack stack = player.getInventory().getItemInMainHand();

                    // Ignore if the item is invalid.
                    if (ItemValidator.isInvalid(stack)) {
                        Utils.tell(player, "<red>You must be holding an item to make it consumable!");
                        return 0;
                    }

                    if (!stack.hasData(DataComponentTypes.DEATH_PROTECTION)) {
                        Utils.tell(player, "<primary>Your item is cannot protect from death!");
                        return 0;
                    }

                    final DeathProtection consumable = stack.getData(DataComponentTypes.DEATH_PROTECTION);
                    if (consumable == null) {
                        Utils.tell(player, "<primary>Your item is not a consumable!");
                        return 0;
                    }

                    Utils.tell(context, """
                            <primary>Your item can protect against death:
                            <dark_aqua>Total Effects</dark_aqua> <arrow> <secondary>{total}</secondary> <hover:show_text:'<gray>Click to open the effects menu.'><click:run_command:'/ie consumable effects menu'><dark_gray>[Click to open Menu]"""
                            .replace("{total}", String.valueOf(consumable.deathEffects().size()))
                    );
                    return 1;
                });
    }

    /**
     * Converts a built {@link DeathProtection} into its builder.
     *
     * @param deathProtection The {@link DeathProtection} to convert.
     * @return Returns {@link DeathProtection#deathProtection()}
     */
    private DeathProtection.@NotNull Builder toBuilder(final @NotNull DeathProtection deathProtection) {
        return DeathProtection.deathProtection()
                .addEffects(deathProtection.deathEffects());
    }

    /**
     * Resets the item's {@link DataComponentTypes#DEATH_PROTECTION} back to default.
     *
     * @param context The {@link CommandContext} used to get the sender and the item they are holding.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int resetItem(@NotNull CommandContext<CommandSourceStack> context) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        if (stack.hasData(DataComponentTypes.DEATH_PROTECTION)) {
            stack.resetData(DataComponentTypes.DEATH_PROTECTION);
            Utils.tell(player, "<primary>Your item's death protection has been reset!");
            return 1;
        }

        Utils.tell(player, "<red>Your item does not protect against death!");
        return 0;
    }

    /**
     * Removes the item's {@link DataComponentTypes#DEATH_PROTECTION}.
     *
     * @param context The {@link CommandContext} used to get the sender and the item they are holding.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int removeDeathProtection(@NotNull CommandContext<CommandSourceStack> context) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        if (stack.hasData(DataComponentTypes.DEATH_PROTECTION)) {
            stack.unsetData(DataComponentTypes.DEATH_PROTECTION);
            Utils.tell(player, "<primary>Your item no longer protects against death!");
            return 1;
        }

        Utils.tell(player, "<red>Your item does not protect against death!");
        return 1;
    }

    /**
     * Updates the items {@link DataComponentTypes#DEATH_PROTECTION} component.
     *
     * @param context  The {@link CommandContext} used to get the sender and the item they are holding.
     * @param function A {@link BiConsumer} that takes {@link DeathProtection} and {@link ItemStack}
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int handleComponent(@NotNull CommandContext<CommandSourceStack> context, @NotNull BiConsumer<DeathProtection, ItemStack> function) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        final DeathProtection deathProtection = stack.getData(DataComponentTypes.DEATH_PROTECTION) == null ? DeathProtection.deathProtection().build() : stack.getData(DataComponentTypes.DEATH_PROTECTION);
        function.accept(deathProtection, stack);
        return 1;
    }

    /**
     * Applies the {@link DeathProtection} component to the item.}
     *
     * @param context  The {@link CommandContext} used to get the sender and the item they are holding.
     * @param function
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int applyComponent(@NotNull CommandContext<CommandSourceStack> context, @NotNull UnaryOperator<DeathProtection> function) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        final DeathProtection consumable = stack.getData(DataComponentTypes.DEATH_PROTECTION) == null ? function.apply(DeathProtection.deathProtection().build()) : function.apply(stack.getData(DataComponentTypes.DEATH_PROTECTION));
        stack.setData(DataComponentTypes.DEATH_PROTECTION, consumable);
        return 1;
    }

}
