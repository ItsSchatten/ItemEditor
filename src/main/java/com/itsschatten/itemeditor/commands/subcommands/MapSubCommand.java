package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class MapSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie map <secondary><id|deco> [...]</secondary>").hoverEvent(StringUtil.color("""
                <primary>Change the map's view and add decorations.
                
                ◼ <secondary>id <id><required></secondary> Change the map view of the map. The map must exist to be applied.
                ◼ <secondary>deco <add|remove|-clear><required> [...]</secondary> Add, remove, or clear decorations on a map.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie map "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("map")
                .then(literal("id")
                        .then(argument("id", IntegerArgumentType.integer(0))
                                .executes(context -> update(context, meta -> {
                                    final int map = IntegerArgumentType.getInteger(context, "id");
                                    final MapView view = Bukkit.getMap(map);
                                    if (view == null) {
                                        Utils.tell(context, "<red>There is no map found for the id: <yellow>" + map);
                                        return meta;
                                    }

                                    meta.setMapView(view);
                                    Utils.tell(context, "<primary>Updated your map to id <secondary>#" + map + "</secondary>!");
                                    return meta;
                                }))
                        )
                )
                .then(literal("deco")
                        .then(literal("remove")
                                .then(argument("key", ArgumentTypes.key())
                                        .suggests((context, builder) -> suggest(context, builder).buildFuture())
                                        .executes(context -> update(context, stack -> {
                                            final Key key = context.getArgument("key", Key.class);
                                            if (!stack.hasData(DataComponentTypes.MAP_DECORATIONS)) {
                                                Utils.tell(context, "<red>Your item doesn't have any map decorations!!");
                                                return;
                                            }

                                            final Map<String, MapDecorations.DecorationEntry> entries = new java.util.HashMap<>(stack.getData(DataComponentTypes.MAP_DECORATIONS).decorations());

                                            if (entries.isEmpty()) {
                                                Utils.tell(context, "<red>Your map doesn't have any decorations!");
                                                return;
                                            }

                                            if (!entries.containsKey(key.asMinimalString())) {
                                                Utils.tell(context, "<red>Your map doesn't have any decoration under the name '" + key.asMinimalString() + "'!");
                                                return;
                                            }

                                            final MapDecorations.DecorationEntry entry = entries.remove(key.asMinimalString());
                                            stack.setData(DataComponentTypes.MAP_DECORATIONS, MapDecorations.mapDecorations(entries));
                                            Utils.tell(context, "<primary>Removed the decoration under the name: <secondary><hover:show_text:'" + convert(entry) + "'>" + key.asMinimalString());
                                        }))
                                )
                        )
                        .then(literal("add")
                                .then(argument("key", ArgumentTypes.key())
                                        .then(argument("type", ArgumentTypes.resource(RegistryKey.MAP_DECORATION_TYPE))
                                                .then(argument("loc", ArgumentTypes.blockPosition())
                                                        .then(argument("rotation", FloatArgumentType.floatArg(0))
                                                                .executes(context -> update(context, stack -> {
                                                                    final Key key = context.getArgument("key", Key.class);
                                                                    final MapCursor.Type type = context.getArgument("type", MapCursor.Type.class);
                                                                    final float rotation = FloatArgumentType.getFloat(context, "rotation");
                                                                    final BlockPosition pos;
                                                                    try {
                                                                        pos = context.getArgument("loc", BlockPositionResolver.class).resolve(context.getSource());
                                                                    } catch (CommandSyntaxException e) {
                                                                        Utils.tell(context, "<red>An error occurred!");
                                                                        return;
                                                                    }

                                                                    final MapDecorations.Builder builder;
                                                                    if (stack.hasData(DataComponentTypes.MAP_DECORATIONS)) {
                                                                        builder = MapDecorations.mapDecorations().putAll(stack.getData(DataComponentTypes.MAP_DECORATIONS).decorations());
                                                                    } else {
                                                                        builder = MapDecorations.mapDecorations();
                                                                    }

                                                                    final MapDecorations.DecorationEntry entry = MapDecorations.decorationEntry(type, pos.x(), pos.z(), rotation);
                                                                    builder.put(key.asMinimalString(), entry);
                                                                    stack.setData(DataComponentTypes.MAP_DECORATIONS, builder.build());
                                                                    Utils.tell(context, "<primary>Added your text decoration under the name <secondary><hover:show_text:'" + convert(entry) + "'></secondary>");
                                                                }))
                                                        )
                                                        .executes(context -> update(context, stack -> {
                                                            final Key key = context.getArgument("key", Key.class);
                                                            final MapCursor.Type type = context.getArgument("type", MapCursor.Type.class);
                                                            final BlockPosition pos;
                                                            try {
                                                                pos = context.getArgument("loc", BlockPositionResolver.class).resolve(context.getSource());
                                                            } catch (CommandSyntaxException e) {
                                                                Utils.tell(context, "<red>An error occurred!");
                                                                return;
                                                            }

                                                            final MapDecorations.Builder builder;
                                                            if (stack.hasData(DataComponentTypes.MAP_DECORATIONS)) {
                                                                builder = MapDecorations.mapDecorations().putAll(stack.getData(DataComponentTypes.MAP_DECORATIONS).decorations());
                                                            } else {
                                                                builder = MapDecorations.mapDecorations();
                                                            }

                                                            final MapDecorations.DecorationEntry entry = MapDecorations.decorationEntry(type, pos.x(), pos.z(), 0f);
                                                            builder.put(key.asMinimalString(), entry);
                                                            stack.setData(DataComponentTypes.MAP_DECORATIONS, builder.build());
                                                            Utils.tell(context, "<primary>Added your text decoration under the name <secondary><hover:show_text:'" + convert(entry) + "'></secondary>");
                                                        }))
                                                )
                                        )
                                )
                        )

                        .then(literal("-clear")
                                .executes(context -> update(context, stack -> {
                                    if (!stack.hasData(DataComponentTypes.MAP_DECORATIONS)) {
                                        Utils.tell(context, "<red>Your item doesn't have any map decorations!!");
                                        return;
                                    }

                                    stack.setData(DataComponentTypes.MAP_DECORATIONS, MapDecorations.mapDecorations(new HashMap<>()));
                                    Utils.tell(context, "<primary>Cleared all of the maps decorations!");
                                }))
                        )
                );
    }

    @Contract(pure = true)
    private @NotNull String convert(final MapDecorations.@NotNull DecorationEntry entry) {
        return "<value>Type <arrow><secondary>" + entry.type() + "</secondary>" +
                "\nLocation <arrow><secondary>" + entry.x() + "<gray>,</gray> " + entry.z() + "</secondary>" +
                "\nRotation <arrow><secondary>" + entry.rotation() + "f";
    }

    private SuggestionsBuilder suggest(final @NotNull CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            return builder;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof MapMeta)) {
            return builder;
        }

        if (stack.hasData(DataComponentTypes.MAP_DECORATIONS)) {
            final MapDecorations decorations = stack.getData(DataComponentTypes.MAP_DECORATIONS);
            decorations.decorations().forEach((key, decoration) -> builder.suggest(key, MessageComponentSerializer.message().serialize(StringUtil.color("Location: " + decoration.x() + ", " + decoration.z() + " | Type: " + decoration.type().key()))));
            return builder;
        }

        return builder;
    }

    private int update(final @NotNull CommandContext<CommandSourceStack> context, UnaryOperator<MapMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!(stack.getItemMeta() instanceof final MapMeta meta)) {
            Utils.tell(user, "<red>You must be holding a map to change things about a map!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));
        return 1;
    }

    private int update(final @NotNull CommandContext<CommandSourceStack> context, Consumer<ItemStack> function) {
        final Player user = (Player) context.getSource().getSender();

        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!(stack.getItemMeta() instanceof MapMeta)) {
            Utils.tell(user, "<red>You must be holding a map to change things about a map!");
            return 0;
        }

        function.accept(stack);
        return 1;
    }


}
