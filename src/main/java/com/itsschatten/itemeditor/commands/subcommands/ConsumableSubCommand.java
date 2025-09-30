package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.menus.ConsumableEffectMenu;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.itemeditor.utils.StringHelper;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.itsschatten.yggdrasil.commands.arguments.GenericEnumArgument;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataAdapterContext;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.ListPersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public final class ConsumableSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie consumable <secondary>[...]</secondary>").hoverEvent(StringUtil.color("""
                <primary>
                \s
                ◼ <secondary><gray><i>no arguments</i></gray></secondary> List information about this consumable.
                ◼ <secondary><effects<optional></secondary> List all effects.
                ◼ <secondary><effects menu><required></secondary> Open a menu to apply or remove effects.
                ◼ <secondary><effects <type> [...]><required></secondary> Apply an effect to your consumable.
                ◼ <secondary><effects remove effect><required></secondary> Remove an effect.
                ◼ <secondary><sound <sound>><required></secondary> The consume sound.
                ◼ <secondary><time <time>><required></secondary> Set the time to consume the item.
                ◼ <secondary><time -clear><required></secondary> Reset to default 1.6s consumption time.
                ◼ <secondary><animation <animation>><required></secondary> The eating animation.
                ◼ <secondary><particles <true|false>><required></secondary> If particles play while eating.
                ◼ <secondary>[-view]<optional></secondary> View the item's current name.
                ◼ <secondary>[-clear]<optional></secondary> Clear the name.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie consumable "))
                ;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("consumable")
                .then(literal("-clear")
                        .executes(this::removeConsumable)
                )
                .then(literal("-reset")
                        .executes(this::resetItem)
                )
                .then(literal("effects")
                        .then(literal("menu")
                                .executes(context -> handleComponent(context, (consumable, stack) -> new ConsumableEffectMenu(stack, consumable.toBuilder()).displayTo(MenuHolder.wrap((Player) context.getSource().getSender()))))
                        )
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
                                                                    return consumable.toBuilder().addEffect(ConsumeEffect.applyStatusEffects(List.of(key.createEffect(duration, amplifier)), probability)).build();
                                                                }))
                                                        )
                                                        .executes(context -> applyComponent(context, consumable -> {
                                                            final PotionEffectType key = context.getArgument("effect", PotionEffectType.class);
                                                            final int duration = IntegerArgumentType.getInteger(context, "duration");
                                                            final int amplifier = IntegerArgumentType.getInteger(context, "amplifier");
                                                            return consumable.toBuilder().addEffect(ConsumeEffect.applyStatusEffects(List.of(key.createEffect(duration, amplifier)), 1F)).build();
                                                        }))
                                                )
                                        )
                                )
                        )
                        .then(literal("remove_effect")
                                .then(argument("effect", ArgumentTypes.resourceKey(RegistryKey.MOB_EFFECT))
                                        .executes(context -> applyComponent(context, consumable -> {
                                                    final TypedKey<PotionEffectType> key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.MOB_EFFECT, "effect");
                                                    return consumable.toBuilder().addEffect(ConsumeEffect.removeEffects(RegistrySet.keySet(RegistryKey.MOB_EFFECT, key))).build();
                                                })
                                        )
                                )
                        )
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
                                            if (stack.getData(DataComponentTypes.CONSUMABLE) != null) {
                                                return SharedSuggestionProvider.suggest(IntStream.range(1, stack.getData(DataComponentTypes.CONSUMABLE).consumeEffects().size() + 1).mapToObj(Integer::toString).toList(), builder);
                                            } else {
                                                return builder.buildFuture();
                                            }
                                        })
                                        .executes(context -> applyComponent(context, consumable -> {
                                            final List<ConsumeEffect> effects = new ArrayList<>(consumable.consumeEffects());
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
                                                final Consumable consume = toNoEffectsBuilder(consumable).addEffects(effects).build();
                                                Utils.tell(context, "<primary>Removed <secondary>" + DeathProtectionSubCommand.consumeEffectToString(effect) + "</secondary> from your item!");
                                                return consume;
                                            }

                                            return consumable;
                                        }))
                                )
                        )
                        .then(literal("clear_all")
                                .executes(context -> applyComponent(context, (consumable) -> {
                                    Utils.tell(context, "<primary>Your consumable will now clear all effects!");
                                    Utils.tell(context, "<gray><i>Tip: If you're trying to clear all effects from your consumable, use '<click:run_command:'/ie consumable effects -clear'><hover:show_text:'<gray>Click to run this command!'>/ie consumable effects -clear</hover></click>'!");
                                    return consumable.toBuilder().addEffect(ConsumeEffect.clearAllStatusEffects()).build();
                                })))
                        .then(literal("-clear")
                                .executes(context -> applyComponent(context, (consumable) -> {
                                    final Consumable finalConsumable = toNoEffectsBuilder(consumable).build();

                                    Utils.tell(context, "<primary>Cleared all effects from your consumable!");
                                    return finalConsumable;
                                })))
                        .then(literal("rtp")
                                .then(argument("diameter", FloatArgumentType.floatArg(1.0F))
                                        .executes(context -> applyComponent(context, consumable -> {
                                                    final float diameter = FloatArgumentType.getFloat(context, "diameter");
                                                    Utils.tell(context, "<primary>Added the random teleport effect with a diameter of <secondary>" + diameter + " blocks</secondary>!");
                                                    return consumable.toBuilder().addEffect(ConsumeEffect.teleportRandomlyEffect(diameter)).build();
                                                })
                                        )
                                )
                                .executes(context -> applyComponent(context,
                                        consumable -> {
                                            Utils.tell(context, "<primary>Added the random teleport effect with a diameter of <secondary>5 blocks</secondary>!");
                                            return consumable.toBuilder().addEffect(ConsumeEffect.teleportRandomlyEffect(5)).build();
                                        })
                                )
                        )
                        // Consume sound.
                        .then(literal("sound")
                                .then(argument("sound resource", ArgumentTypes.resourceKey(RegistryKey.SOUND_EVENT))
                                        .executes(context -> applyComponent(context, consumable -> {
                                            final Key key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.SOUND_EVENT, "sound resource").key();
                                            Utils.tell(context, "<primary>Added the play sound effect which will play <secondary>" + key.asMinimalString() + "</secondary>!");
                                            return consumable.toBuilder().addEffect(ConsumeEffect.playSoundConsumeEffect(key)).build();
                                        }))
                                )
                        )

                        .executes(context -> handleComponent(context, (consumable, stack) -> {
                            if (consumable.consumeEffects().isEmpty()) {
                                Utils.tell(context, "<primary>Your consumable has no effects.");
                                Utils.tell(context, "<gray><i><click:run_command:'/ie consumable effects menu'><hover:show_text:'<gray>Click to run the command '/ie consumable effects menu''>[Click to open Menu]");
                                return;
                            }
                            Utils.tell(context, "<primary>Your consumables effects:");
                            consumable.consumeEffects().forEach(effect -> Utils.tell(context, " <primary>◼</primary> " + convertToHoverString(effect)));
                            Utils.tell(context, "<br><gray><i><click:run_command:'/ie consumable effects menu'><hover:show_text:'<gray>Click to run the command '/ie consumable effects menu''>[Click to open Menu]");
                        }))
                )
                // Eat sound.
                .then(literal("sound")
                        .then(argument("sound resource", ArgumentTypes.resourceKey(RegistryKey.SOUND_EVENT))
                                .executes(context -> applyComponent(context, consumable -> {
                                    final Key key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.SOUND_EVENT, "sound resource").key();
                                    Utils.tell(context, "<primary>Set the consume sound to <secondary>" + key.asMinimalString() + "</secondary>!");
                                    return consumable.toBuilder().sound(key).build();
                                }))
                        )
                )
                .then(literal("time")
                        .then(literal("-reset")
                                .executes(context -> applyComponent(context, consumable -> {
                                    Utils.tell(context, "<primary>Set the consume time to <secondary>1.6 seconds</secondary>!");
                                    return consumable.toBuilder().consumeSeconds(1.6F).build();
                                }))
                        )
                        .then(argument("seconds", FloatArgumentType.floatArg(0.0f))
                                .executes(context -> applyComponent(context, consumable -> {
                                    final float seconds = FloatArgumentType.getFloat(context, "seconds");
                                    Utils.tell(context, "<primary>Set the consume time to <secondary>" + seconds + " seconds</secondary>!");
                                    return consumable.toBuilder().consumeSeconds(seconds).build();
                                }))
                        )
                )
                .then(literal("animation")
                        .then(argument("animation", GenericEnumArgument.generic(ItemUseAnimation.class))
                                .executes(context -> applyComponent(context, consumable -> {
                                    final ItemUseAnimation animation = context.getArgument("animation", ItemUseAnimation.class);
                                    Utils.tell(context, "<primary>Set the consume animation to <secondary>" + animation.name().toLowerCase().replace("_", " ") + "</secondary>!");
                                    return consumable.toBuilder().animation(animation).build();
                                }))
                        )
                )
                .then(literal("particles")
                        .then(argument("particles", BoolArgumentType.bool())
                                .executes(context -> applyComponent(context, consumable -> {
                                    final boolean particles = BoolArgumentType.getBool(context, "particles");
                                    Utils.tell(context, "<primary>Set the consume particles to <secondary>" + particles + "</secondary>!");
                                    return consumable.toBuilder().hasConsumeParticles(particles).build();
                                }))
                        )
                        .executes(context -> applyComponent(context, consumable -> {
                                    final boolean particles = !consumable.hasConsumeParticles();
                                    Utils.tell(context, "<primary>Set the consume particles to <secondary>" + particles + "</secondary>!");
                                    return consumable.toBuilder().hasConsumeParticles(particles).build();
                                })
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

                    if (!stack.hasData(DataComponentTypes.CONSUMABLE)) {
                        Utils.tell(player, "<primary>Your item is not a consumable!");
                        return 0;
                    }

                    final Consumable consumable = stack.getData(DataComponentTypes.CONSUMABLE);
                    if (consumable == null) {
                        Utils.tell(player, "<primary>Your item is not a consumable!");
                        return 0;
                    }

                    Utils.tell(context, """
                            <primary>Your consumable:
                            <value>Particles</value> <arrow> <secondary>{particles}</secondary>
                            <value>Animation</value> <arrow> <secondary>{animation}</secondary>
                            <value>Time</value> (<gray>(seconds)</gray>) <arrow> <secondary>{time}</secondary>
                            <value>Sound</value> <arrow> <secondary>{sound}</secondary>
                            <value>Total Effects</value> <arrow> <secondary>{total}</secondary> <hover:show_text:'<gray>Click to open the effects menu.'><click:run_command:'/ie consumable effects menu'><dark_gray>[Click to open Menu]"""
                            .replace("{particles}", consumable.hasConsumeParticles() ? "<green>✔</green>" : "<red>✘</red>")
                            .replace("{animation}", consumable.animation().name())
                            .replace("{time}", StringHelper.conditionString(String.valueOf(consumable.consumeSeconds()), String.valueOf(consumable.consumeSeconds()).replace(".0", ""), (string) -> string.endsWith(".0")))
                            .replace("{sound}", consumable.sound().asMinimalString())
                            .replace("{total}", String.valueOf(consumable.consumeEffects().size()))
                    );
                    return 1;
                });
    }

    /**
     * Converts a built {@link Consumable} into a builder without any potion effects.
     *
     * @param consumable The {@link Consumable} instance to make the builder from.
     * @return Returns {@link Consumable#consumable()} with the provided consumable's settings ignoring the effects.
     */
    private Consumable.@NotNull Builder toNoEffectsBuilder(final @NotNull Consumable consumable) {
        return Consumable.consumable()
                .sound(consumable.sound())
                .consumeSeconds(consumable.consumeSeconds())
                .hasConsumeParticles(consumable.hasConsumeParticles())
                .animation(consumable.animation());
    }

    /**
     * Quickly resets the item's {@link DataComponentTypes#CONSUMABLE} status.
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

        if (stack.hasData(DataComponentTypes.CONSUMABLE)) {
            stack.resetData(DataComponentTypes.CONSUMABLE);
            Utils.tell(player, "<primary>Your consumable has been reset to default!");
            return 1;
        }

        Utils.tell(player, "<red>Your item is not consumable!");
        return 0;
    }

    /**
     * Quickly removes the {@link DataComponentTypes#CONSUMABLE} from the item.
     *
     * @param context The {@link CommandContext} used to get the sender and the item they are holding.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int removeConsumable(@NotNull CommandContext<CommandSourceStack> context) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        if (stack.hasData(DataComponentTypes.CONSUMABLE)) {
            stack.unsetData(DataComponentTypes.CONSUMABLE);
            Utils.tell(player, "<primary>Your item is no longer consumable!");
            return 1;
        }

        Utils.tell(player, "<red>Your item is not consumable!");
        return 1;
    }

    /**
     * Handles the {@link Consumable} to be put on the item.
     *
     * @param context  The {@link CommandContext} used to get the sender and the item they are holding.
     * @param function A {@link BiConsumer} that takes an {@link ItemStack} and {@link Consumable} and is used to update the ItemStack.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int handleComponent(@NotNull CommandContext<CommandSourceStack> context, @NotNull BiConsumer<Consumable, ItemStack> function) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        final Consumable consumable = stack.getData(DataComponentTypes.CONSUMABLE) == null ? Consumable.consumable().build() : stack.getData(DataComponentTypes.CONSUMABLE);
        function.accept(consumable, stack);
        return 1;
    }


    /**
     * Sets the {@link Consumable} component on the {@link ItemStack} held by the command sender.
     *
     * @param context  The {@link CommandContext} used to get the sender and the item they are holding.
     * @param function A {@link UnaryOperator} that takes and produces a {@link Consumable} to be set on the item held by the command sender.
     * @return Returns {@code 1} if the command was successful, {@code 0} otherwise.
     */
    private int applyComponent(@NotNull CommandContext<CommandSourceStack> context, @NotNull UnaryOperator<Consumable> function) {
        final Player player = (Player) context.getSource().getSender();
        final ItemStack stack = player.getInventory().getItemInMainHand();

        // Ignore if the item is invalid.
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(player, "<red>You must be holding an item to make it consumable!");
            return 0;
        }

        final Consumable consumable = stack.getData(DataComponentTypes.CONSUMABLE) == null ? function.apply(Consumable.consumable().build()) : function.apply(stack.getData(DataComponentTypes.CONSUMABLE));
        stack.setData(DataComponentTypes.CONSUMABLE, consumable);
        return 1;
    }

    /**
     * Utility method to quickly convert a {@link ConsumeEffect} into a {@link String} to be used in a hover event.
     *
     * @param consumeEffect The effect to convert to a string.
     * @return Returns a {@link  String}.
     * @see DeathProtectionSubCommand#consumeEffectAsHover(ConsumeEffect)
     */
    private @NotNull String convertToHoverString(final @NotNull ConsumeEffect consumeEffect) {
        return DeathProtectionSubCommand.consumeEffectAsHover(consumeEffect);
    }

}
