package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringPaginator;
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
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.UnaryOperator;

public final class BlocksAttacksSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie shield <secondary><...></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set shield values for an item.
                \s
                ◼ <secondary>delay <seconds><required></secondary> Set the block delay.
                ◼ <secondary>disable_scale <multiplier><required></secondary> A multiplier for how long an item can disable this shield.
                ◼ <secondary>reduction add<required> <damage><required> <base><required> <factor><required> <angle><optional></secondary> Add a damage reduction to this shield.
                ◼ <secondary>reduction remove<required> <damage type><required></secondary> Remove a damage reduction from this shield.
                ◼ <secondary>reduction -clear<required></secondary> Clear the damage reductions on the shield.
                ◼ <secondary>item_damage <threshold><required> <base><required> <factor><required></secondary> Set the function for how damage is calculated for blocking damage.
                ◼ <secondary>bypassed_by <damage><required></secondary> A damage type tag that can bypass the shield's blocking.
                ◼ <secondary>sound <sound><required></secondary> Set the block sound for the shield.
                ◼ <secondary>disabled_sound <sound><required></secondary> Set the shield disable sound.
                ◼ <secondary>[-view]<optional></secondary> View the information on your shield.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie shield "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("shield")
                .then(literal("-view")
                        .then(argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> handleView(context, IntegerArgumentType.getInteger(context, "page")))
                        )
                        .executes(context -> handleView(context, 1))
                )
                .then(literal("delay")
                        .then(argument("seconds", FloatArgumentType.floatArg(0))
                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                    final float seconds = FloatArgumentType.getFloat(context, "seconds");
                                    builder.blockDelaySeconds(seconds);
                                    Utils.tell(context, "<primary>Your shield's block delay has been set to <secondary>" + seconds + " seconds</secondary>.");
                                    return builder;
                                }))
                        )
                )
                .then(literal("disable_scale")
                        .then(argument("multiplier", FloatArgumentType.floatArg(0))
                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                    final float multiplier = FloatArgumentType.getFloat(context, "multiplier");
                                    builder.blockDelaySeconds(multiplier);
                                    Utils.tell(context, "<primary>Your shield's disable cooldown scale has been set to <secondary>" + multiplier + "</secondary>.");
                                    return builder;
                                }))
                        )
                )
                .then(literal("reduction")
                        .then(literal("-clear")
                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                    builder.damageReductions(List.of());
                                    Utils.tell(context, "<primary>Cleared the damage reductions from your shield.");
                                    return builder;
                                }))
                        )
                        .then(literal("add")
                                .then(argument("damage", ArgumentTypes.resourceKey(RegistryKey.DAMAGE_TYPE))
                                        .then(argument("base", FloatArgumentType.floatArg(0))
                                                .then(argument("factor", FloatArgumentType.floatArg(0))
                                                        .then(argument("angle ", FloatArgumentType.floatArg(0))
                                                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                                                    final TypedKey<DamageType> damageKey = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.DAMAGE_TYPE, "damage");
                                                                    final float base = FloatArgumentType.getFloat(context, "base");
                                                                    final float factor = FloatArgumentType.getFloat(context, "factor");
                                                                    final float angle = FloatArgumentType.getFloat(context, "angle");

                                                                    builder.addDamageReduction(DamageReduction.damageReduction()
                                                                            .base(base)
                                                                            .factor(factor)
                                                                            .type(RegistrySet.keySet(damageKey.registryKey()))
                                                                            .horizontalBlockingAngle(angle)
                                                                            .build());

                                                                    Utils.tell(context, """
                                                                            <primary>Added the following damage reduction to your shield:
                                                                            <secondary>Damage Types:</secondary> <value>{damage}</value>
                                                                            """
                                                                            .replace("{damage}", damageKey.asString())
                                                                            .replace("{angle}", angle + "°")
                                                                            .replace("{base}", String.valueOf(base))
                                                                            .replace("{factor}", String.valueOf(factor)));
                                                                    return builder;
                                                                }))
                                                        )
                                                        .executes(context -> updateBlocksAttacks(context, builder -> {
                                                            final TypedKey<DamageType> damageKey = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.DAMAGE_TYPE, "damage");
                                                            final float base = FloatArgumentType.getFloat(context, "base");
                                                            final float factor = FloatArgumentType.getFloat(context, "factor");

                                                            builder.addDamageReduction(DamageReduction.damageReduction()
                                                                    .base(base)
                                                                    .factor(factor)
                                                                    .type(RegistrySet.keySet(damageKey.registryKey()))
                                                                    .horizontalBlockingAngle(90f)
                                                                    .build());

                                                            Utils.tell(context, """
                                                                    <primary>Added the following damage reduction to your shield:
                                                                    <secondary>Damage Types:</secondary> <value>{damage}</value>
                                                                    """
                                                                    .replace("{damage}", damageKey.asString())
                                                                    .replace("{angle}", "90°")
                                                                    .replace("{base}", String.valueOf(base))
                                                                    .replace("{factor}", String.valueOf(factor)));
                                                            return builder;
                                                        }))
                                                )
                                        )
                                )
                        )
                        .then(literal("remove")
                                .then(argument("number", IntegerArgumentType.integer(0))
                                        .suggests((context, builder) -> {
                                            if (context.getSource().getSender() instanceof final Player user) {
                                                // Get the item stack in the user's main hand.
                                                final ItemStack stack = user.getInventory().getItemInMainHand();
                                                if (ItemValidator.isInvalid(stack)) {
                                                    return builder.buildFuture();
                                                }

                                                if (!stack.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
                                                    return builder.buildFuture();
                                                }

                                                final BlocksAttacks attacks = stack.getData(DataComponentTypes.BLOCKS_ATTACKS);
                                                if (!attacks.damageReductions().isEmpty()) {
                                                    for (int i = 0; i < attacks.damageReductions().size(); i++) {
                                                        final DamageReduction damageReduction = attacks.damageReductions().get(i);
                                                        if (damageReduction.type() == null) continue;
                                                        builder.suggest(i, Utils.message("""
                                                                <primary>Damage: <secondary>{damage}</secondary>
                                                                Angle: <secondary>{angle}</secondary>
                                                                Base: <secondary>{base}</secondary>
                                                                Factor: <secondary>{factor}</secondary>"""
                                                                .replace("{damage}", damageReduction.type().registryKey().key().asString())
                                                                .replace("{angle}", damageReduction.horizontalBlockingAngle() + "°")
                                                                .replace("{base}", String.valueOf(damageReduction.base()))
                                                                .replace("{factor}", String.valueOf(damageReduction.factor()))
                                                        ));
                                                    }

                                                    return builder.buildFuture();
                                                }
                                            }

                                            return builder.buildFuture();
                                        })
                                        .executes(context -> removeDamageReduction(context, IntegerArgumentType.getInteger(context, "number")))
                                )
                        )

                )
                .then(literal("item_damage")
                        .then(argument("threshold", FloatArgumentType.floatArg(0))
                                .then(argument("base", FloatArgumentType.floatArg(0))
                                        .then(argument("factor", FloatArgumentType.floatArg(0, 1))
                                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                                    final float threshold = FloatArgumentType.getFloat(context, "threshold");
                                                    final float base = FloatArgumentType.getFloat(context, "base");
                                                    final float factor = FloatArgumentType.getFloat(context, "factor");

                                                    builder.itemDamage(ItemDamageFunction.itemDamageFunction()
                                                            .threshold(threshold)
                                                            .base(base)
                                                            .factor(factor)
                                                            .build());

                                                    Utils.tell(context, """
                                                            <primary>Updated the function for which your shield will take damage:
                                                            <secondary>Threshold (Before Damage)</secondary>: <value>{threshold}</value>
                                                            <secondary>Base (Constant)</secondary>: <value>{base}</value>
                                                            <secondary>Factor (Fraction of Damage Dealt applied)</secondary>: <value>{factor}</value>"""
                                                            .replace("{threshold}", String.valueOf(threshold))
                                                            .replace("{base}", String.valueOf(base))
                                                            .replace("{factor}", String.valueOf(factor))
                                                    );
                                                    return builder;
                                                }))
                                        )
                                )
                        )
                )
                .then(literal("bypassed_by")
                        .then(argument("damage", ArgumentTypes.resourceKey(RegistryKey.DAMAGE_TYPE))
                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                    final Key key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.DAMAGE_TYPE, "damage");
                                    builder.bypassedBy(TagKey.create(RegistryKey.DAMAGE_TYPE, key));
                                    Utils.tell(context, "<primary><secondary>" + key.asString() + "</secondary> damage now bypasses your shield.");
                                    return builder;
                                }))
                        )
                )
                .then(literal("sound")
                        .then(argument("sound", ArgumentTypes.resourceKey(RegistryKey.SOUND_EVENT))
                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                    final Key key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.SOUND_EVENT, "sound");
                                    builder.blockSound(key);
                                    Utils.tell(context, "<primary>Your shields block sound has been set to <secondary>" + key.asString() + "</secondary>.");
                                    return builder;
                                }))
                        )
                )
                .then(literal("disable_sound")
                        .then(argument("sound", ArgumentTypes.resourceKey(RegistryKey.SOUND_EVENT))
                                .executes(context -> updateBlocksAttacks(context, builder -> {
                                    final Key key = RegistryArgumentExtractor.getTypedKey(context, RegistryKey.SOUND_EVENT, "sound");
                                    builder.disableSound(key);
                                    Utils.tell(context, "<primary>Your shields disable sound has been set to <secondary>" + key.asString() + "</secondary>.");
                                    return builder;
                                }))
                        )
                )
                ;
    }

    /**
     * Converts an already built {@link BlocksAttacks} into its builder.
     *
     * @param attacks The {@link BlocksAttacks} instance to convert back to a builder.
     * @return Returns the {@link BlocksAttacks.Builder} from the provided BlocksAttacks.
     */
    private BlocksAttacks.@NotNull Builder toBuilder(@NotNull BlocksAttacks attacks) {
        return BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(attacks.blockDelaySeconds())
                .disableCooldownScale(attacks.disableCooldownScale())
                .damageReductions(attacks.damageReductions())
                .itemDamage(attacks.itemDamage())
                .bypassedBy(attacks.bypassedBy())
                .blockSound(attacks.blockSound())
                .disableSound(attacks.disableSound());
    }

    private int removeDamageReduction(final @NotNull CommandContext<CommandSourceStack> context, final int location) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!stack.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
            Utils.tell(user, "<primary>The item you are holding cannot block attacks.");
            return FAILURE;
        }

        final BlocksAttacks attacks = stack.getData(DataComponentTypes.BLOCKS_ATTACKS);
        final List<DamageReduction> reductions = attacks.damageReductions();

        if (reductions.isEmpty()) {
            Utils.tell(user, "<red>The reductions list on your shield is empty, cannot remove nothing!");
            return FAILURE;
        }

        if (location < reductions.size()) {
            Utils.tell(user, "<red>Couldn't find the value in the list!");
            return FAILURE;
        }

        final DamageReduction reduction = reductions.remove(location);

        return updateBlocksAttacks(context, builder -> {
            Utils.tell(context, "<primary>Your shield no longer reduces <secondary>" + reduction.type().registryKey().key() + "</secondary>.");
            return builder.damageReductions(reductions);
        });
    }

    private int updateBlocksAttacks(final @NotNull CommandContext<CommandSourceStack> context, UnaryOperator<BlocksAttacks.Builder> operator) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.setData(DataComponentTypes.BLOCKS_ATTACKS, operator.apply(!stack.hasData(DataComponentTypes.BLOCKS_ATTACKS) ? BlocksAttacks.blocksAttacks() :
                toBuilder(stack.getData(DataComponentTypes.BLOCKS_ATTACKS))));
        return 1;
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context, int page) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!stack.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
            Utils.tell(user, "<primary>The item you are holding cannot block attacks.");
            return FAILURE;
        }

        final BlocksAttacks attacks = stack.getData(DataComponentTypes.BLOCKS_ATTACKS);

        if (page == 1) {
            final String damageFunction = """
                    <secondary>Threshold:</secondary> <value>{threshold}</value>
                    <secondary>Base:</secondary> <value>{base}</value>
                    <secondary>Factor:</secondary> <value>{factor}</value>"""
                    .replace("{threshold}", String.valueOf(attacks.itemDamage().threshold()))
                    .replace("{base}", String.valueOf(attacks.itemDamage().base()))
                    .replace("{factor}", String.valueOf(attacks.itemDamage().factor()));

            Utils.tell(user, """
                    <primary>Your shield values:
                    
                    <secondary>Block Sound:</secondary> <value>{sound}</value>
                    <secondary>Disable Sound:</secondary> <value>{disable}</value>
                    <secondary>Block Delay Seconds:</secondary> <value>{delay} seconds</value>
                    <secondary>Damage Function:</secondary> <value>{damage_function}</value>
                    <secondary>Bypassed By:</secondary> <value>{bypassed}</value>"""
                    .replace("{sound}", attacks.blockSound().asString())
                    .replace("{disable}", attacks.disableSound().asString())
                    .replace("{delay}", String.valueOf(attacks.blockDelaySeconds()))
                    .replace("{damage_function}", "<hover:show_text:'" + damageFunction + "'><info>[Hover the value]</info></hover>")
                    .replace("{bypassed}", attacks.bypassedBy().key().asString())
            );
        }

        final StringBuilder builder = new StringBuilder();
        attacks.damageReductions().forEach(reduction -> {
            if (reduction.type() == null) return;
            final String values = """
                    Angle: <secondary>{angle}</secondary>
                    Base: <secondary>{base}</secondary>
                    Factor: <secondary>{factor}</secondary>"""
                    .replace("{angle}", reduction.horizontalBlockingAngle() + "°")
                    .replace("{base}", String.valueOf(reduction.base()))
                    .replace("{factor}", String.valueOf(reduction.factor()));

            builder.append("<primary> ◼ <secondary><hover:show_text:'").append(values).append("'>").append(reduction.type() == null ? "null" : reduction.type().registryKey().key().asString()).append("</hover></secondary><br>");
        });
        final StringPaginator paginator = new StringPaginator(builder.toString(), "/ie shield -view");
        Utils.tell(user, paginator.page(page).asString());
        Utils.tell(user, paginator.navigation(page));
        return SUCCESS;
    }

}
