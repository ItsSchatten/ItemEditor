package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public final class TooltipSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie tooltip <secondary><tooltip></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Adjust the view of your item's tooltip.
                        
                        ◼ <secondary>hide|show <type><required></secondary> Hide or show a data component.
                        ◼ <secondary>hide|show -all<optional></secondary> Hide or show all data components.
                        ◼ <secondary>style <key><required></secondary> Change the tooltip style of the item.
                        ◼ <secondary>style -clear<required></secondary> Reset the tooltip style of the item.
                        ◼ <secondary>hidden [true|false]<optional></secondary> Hide or show the entire tooltip.
                        ◼ <secondary>[-view]<optional></secondary> View all your item's tooltip configuration.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie tooltip "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("tooltip")
                .then(literal("hidden")
                        .then(argument("value", BoolArgumentType.bool())
                                .executes(context -> updateTooltip(context, builder -> {
                                    final boolean value = BoolArgumentType.getBool(context, "value");

                                    // Set it the value.
                                    builder.hideTooltip(value);
                                    Utils.tell(context.getSource(), "<primary>Your item's tooltip is <secondary>" + (builder.build().hideTooltip() ? "now" : "no longer") + "</secondary> hidden!");
                                    return builder;
                                }))
                        )
                        .executes(context -> updateTooltip(context, builder -> {
                            builder.hideTooltip(!builder.build().hideTooltip());
                            Utils.tell(context.getSource(), "<primary>Your item's tooltip is <secondary>" + (builder.build().hideTooltip() ? "now" : "no longer") + "</secondary> hidden!");
                            return builder;
                        }))
                )

                .then(literal("hide")
                        .then(literal("-all")
                                .executes(context -> updateTooltip(context, builder -> {
                                    RegistryAccess.registryAccess().getRegistry(RegistryKey.DATA_COMPONENT_TYPE).forEach(builder::addHiddenComponents);
                                    Utils.tell(context.getSource(), "<primary>Your item is now hiding <secondary>all</secondary> components!");
                                    return builder;
                                }))
                        )
                        .then(argument("component", ArgumentTypes.resource(RegistryKey.DATA_COMPONENT_TYPE))
                                .executes(context -> updateTooltip(context, builder -> {
                                    final DataComponentType type = context.getArgument("component", DataComponentType.class);
                                    Utils.tell(context.getSource(), "<primary>Your item's tooltip now hides <secondary>" + type.key().asString() + "</secondary>!");
                                    builder.addHiddenComponents(type);
                                    return builder;
                                }))
                        )
                )

                .then(literal("-view")
                        .executes(this::handleView)
                )

                .then(literal("show")
                        .then(literal("-all")
                                .executes(context -> updateHiddenComponents(context, (builder, types) -> {
                                    builder.hiddenComponents(new HashSet<>());
                                    Utils.tell(context.getSource(), "<primary>Your item is no longer hiding <secondary>all</secondary> components!");
                                    return builder;
                                }))
                        )
                        .then(argument("component", ArgumentTypes.resource(RegistryKey.DATA_COMPONENT_TYPE))
                                .executes(context -> updateHiddenComponents(context, (builder, types) -> {
                                    final DataComponentType type = context.getArgument("component", DataComponentType.class);

                                    types.remove(type);
                                    Utils.tell(context.getSource(), "<primary>Your item's tooltip no longer hides <secondary>" + type.key().asString() + "</secondary>!");
                                    builder.hiddenComponents(types);
                                    return builder;
                                }))
                        )
                )

                .then(literal("style")
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
                        )
                )
                ;
    }

    private TooltipDisplay.@NotNull Builder toBuilder(final @NotNull TooltipDisplay display) {
        return TooltipDisplay.tooltipDisplay().hideTooltip(display.hideTooltip()).hiddenComponents(display.hiddenComponents());
    }

    private TooltipDisplay.Builder toBuilderWithoutHidden(final TooltipDisplay display) {
        return TooltipDisplay.tooltipDisplay().hideTooltip(display.hideTooltip());

    }

    private int updateHiddenComponents(final @NotNull CommandContext<CommandSourceStack> context, final BiFunction<TooltipDisplay.Builder, Set<DataComponentType>, TooltipDisplay.Builder> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.resetData(DataComponentTypes.TOOLTIP_STYLE);
        stack.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                function.apply(!stack.hasData(DataComponentTypes.TOOLTIP_DISPLAY) ? TooltipDisplay.tooltipDisplay() :
                                toBuilderWithoutHidden(stack.getData(DataComponentTypes.TOOLTIP_DISPLAY)),
                        stack.hasData(DataComponentTypes.TOOLTIP_DISPLAY) ? stack.getData(DataComponentTypes.TOOLTIP_DISPLAY).hiddenComponents() : new HashSet<>()));
        return 1;
    }

    private int updateTooltip(final @NotNull CommandContext<CommandSourceStack> context, final UnaryOperator<TooltipDisplay.Builder> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                function.apply(!stack.hasData(DataComponentTypes.TOOLTIP_DISPLAY) ? TooltipDisplay.tooltipDisplay() :
                        toBuilder(stack.getData(DataComponentTypes.TOOLTIP_DISPLAY))));
        return 1;
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!stack.hasData(DataComponentTypes.TOOLTIP_DISPLAY)) {
            Utils.tell(user, "<primary>Your item's full tooltip is <secondary>currently not</secondary> hidden!");
            return 0;
        }

        final TooltipDisplay display = stack.getData(DataComponentTypes.TOOLTIP_DISPLAY);
        // Shouldn't be null because of stack#hasData
        assert display != null;
        Utils.tell(user, "<primary>Your item's full tooltip is <secondary>currently" + (display.hideTooltip() ? "" : " not") + "</secondary> hidden!");

        if (!display.hiddenComponents().isEmpty()) {
            Utils.tell(user, "<primary>Your item has the following hidden components: <#D8D8F6>" + String.join("<gray>,</gray> ", display.hiddenComponents().stream()
                    .map((flag) -> "<click:suggest_command:'/ie tooltip show " + flag.key().asString() + "'><hover:show_text:'<gray><i>Click to suggest the command to remove this flag!'>" + flag.key().asString().toLowerCase() + "</hover></click>").toList()));
            Utils.tell(user, "<gray><i>Click a flag above to suggest the command to remove it!");
        } else {
            Utils.tell(user, "<primary>Your item has no hidden components.");
        }

        if (stack.hasData(DataComponentTypes.TOOLTIP_STYLE)) {
            Utils.tell(user, "<primary>Your item's current tooltip style is <secondary>" + Objects.requireNonNull(stack.getData(DataComponentTypes.TOOLTIP_STYLE)).asMinimalString() + "</secondary>!");
        } else {
            Utils.tell(user, "<primary>Your item doesn't have a tooltip style!");
        }

        return 1;
    }

}
