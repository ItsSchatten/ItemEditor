package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.menus.FireworkMenu;
import com.itsschatten.itemeditor.menus.FireworkStarMenu;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.WrapUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.itsschatten.yggdrasil.menus.MenuUtils;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class FireworkSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie firework <secondary><menu|power|-clear|add|remove></secondary>").hoverEvent(StringUtil.color("""
                <primary>Manipulate a firework or firework star via a GUI or command.
                <gray><i>Supply '""' if you wish to not have an argument in that slot, this does not work for colors.</i></gray>
                \s
                ◼ <secondary>[menu]<optional></secondary> Opens the firework edit menu, this is the default if no args are provided.
                ◼ <secondary><power><first></secondary> Set the power of a firework, fails if given a firework star.
                ◼ <secondary><remove><first></secondary> Remove a specific effect on a firework or clears the effect on a firework star.
                ◼ <secondary><-clear><first></secondary> Clear all firework effects from a firework or firework star.
                ◼ <secondary><add><first> <colors><required> <fades><optional> <shape><optional> <flicker><optional> <trail><optional></secondary> Add a firework effect.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie firework "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("firework")
                .then(literal("power")
                        .then(argument("power", IntegerArgumentType.integer(0, 4))
                                .executes(context -> updateFireworkPower(context, IntegerArgumentType.getInteger(context, "power")))
                        )
                )
                .then(literal("-clear")
                        .executes(this::clearEffects)
                )
                .then(literal("remove")
                        .then(argument("id", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getEffectIds(context).stream().map(integer -> integer + "").toList(), builder))
                                .executes(context -> removeEffect(context, IntegerArgumentType.getInteger(context, "id")))
                        )
                )
                .then(literal("add")
                        .then(argument("colors", StringArgumentType.string())
                                .then(argument("fades", StringArgumentType.string())
                                        .then(argument("shape", GenericEnumArgument.generic(FireworkEffect.Type.class))
                                                .then(argument("flicker", BoolArgumentType.bool())
                                                        .then(argument("trail", BoolArgumentType.bool())
                                                                .executes(context -> {
                                                                    final String colorString = StringArgumentType.getString(context, "colors");
                                                                    final String fadesString = StringArgumentType.getString(context, "fades");
                                                                    final FireworkEffect.Type type = context.getArgument("shape", FireworkEffect.Type.class);

                                                                    final boolean flicker = BoolArgumentType.getBool(context, "flicker");
                                                                    final boolean trail = BoolArgumentType.getBool(context, "trail");

                                                                    final List<Color> colors = parseColors(context, colorString);
                                                                    final List<Color> fades = parseColors(context, fadesString);

                                                                    if (colors.isEmpty()) {
                                                                        Utils.tell(context.getSource(), "<red>You must provide one color.");
                                                                        return 0;
                                                                    }

                                                                    return addFireWorkEffect(context, colors, fades, type, flicker, trail);
                                                                })
                                                        )
                                                        .executes(context -> {
                                                            final String colorString = StringArgumentType.getString(context, "colors");
                                                            final String fadesString = StringArgumentType.getString(context, "fades");
                                                            final FireworkEffect.Type type = context.getArgument("shape", FireworkEffect.Type.class);

                                                            final boolean flicker = BoolArgumentType.getBool(context, "flicker");

                                                            final List<Color> colors = parseColors(context, colorString);
                                                            final List<Color> fades = parseColors(context, fadesString);

                                                            if (colors.isEmpty()) {
                                                                Utils.tell(context.getSource(), "<red>You must provide one color.");
                                                                return 0;
                                                            }

                                                            return addFireWorkEffect(context, colors, fades, type, flicker, false);
                                                        })
                                                )
                                                .executes(context -> {
                                                    final String colorString = StringArgumentType.getString(context, "colors");
                                                    final String fadesString = StringArgumentType.getString(context, "fades");
                                                    final FireworkEffect.Type type = context.getArgument("shape", FireworkEffect.Type.class);

                                                    final List<Color> colors = parseColors(context, colorString);
                                                    final List<Color> fades = parseColors(context, fadesString);

                                                    if (colors.isEmpty()) {
                                                        Utils.tell(context.getSource(), "<red>You must provide one color.");
                                                        return 0;
                                                    }

                                                    return addFireWorkEffect(context, colors, fades, type, false, false);
                                                })
                                        )
                                        .suggests((context, builder) -> {
                                            final List<String> options = new ArrayList<>();
                                            final String prefix = builder.getRemainingLowerCase();

                                            for (final DyeColor color : DyeColor.values()) {
                                                options.add(prefix + color.name().toLowerCase() + ",");
                                            }

                                            return SharedSuggestionProvider.suggest(options, builder);
                                        })
                                        .executes(context -> {
                                            final String colorString = StringArgumentType.getString(context, "colors");
                                            final String fadesString = StringArgumentType.getString(context, "fades");

                                            final List<Color> colors = parseColors(context, colorString);
                                            final List<Color> fades = parseColors(context, fadesString);

                                            if (colors.isEmpty()) {
                                                Utils.tell(context.getSource(), "<red>You must provide one color.");
                                                return 0;
                                            }

                                            return addFireWorkEffect(context, colors, fades, FireworkEffect.Type.BALL, false, false);
                                        })
                                )
                                .suggests((context, builder) -> {
                                    final List<String> options = new ArrayList<>();
                                    final String prefix = builder.getRemainingLowerCase();

                                    for (final DyeColor color : DyeColor.values()) {
                                        options.add(prefix + color.name().toLowerCase() + ",");
                                    }

                                    return SharedSuggestionProvider.suggest(options, builder);
                                })
                                .executes(context -> {
                                    final String colorString = StringArgumentType.getString(context, "colors");
                                    final List<Color> colors = parseColors(context, colorString);

                                    if (colors.isEmpty()) {
                                        Utils.tell(context.getSource(), "<red>You must provide one color.");
                                        return 0;
                                    }

                                    return addFireWorkEffect(context, colors, Collections.emptyList(), FireworkEffect.Type.BALL, false, false);
                                })
                        )
                )
                .then(literal("menu")
                        .executes(this::openMenu)
                )
                .executes(this::openMenu);
    }

    private @NotNull List<Color> parseColors(final CommandContext<CommandSourceStack> context, final @NotNull String colorString) {
        final String[] split = colorString.replace("\"", "").replace(" ", "").split(",");
        final List<Color> colors = new ArrayList<>();

        // Loop the colors and add them to the list.
        for (final String clr : split) {
            try {
                // Get the firework color from the dye.
                colors.add(DyeColor.valueOf(clr.toUpperCase()).getFireworkColor());
            } catch (IllegalArgumentException ignored) {
                if (clr.startsWith("#") && clr.length() == 7 || clr.length() == 6) {
                    // Get RGB.
                    final Color color = Color.fromRGB(Integer.parseInt(clr.substring(1), 16));

                    colors.add(color);
                    continue;
                }

                Utils.tell(context.getSource(), "<gray>'" + clr + "' is not a valid color, must be a color name or a hex color string (#hexclr). It has been skipped.");
            }
        }

        return colors;
    }

    private List<Integer> getEffectIds(@NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            return Collections.emptyList();
        }

        if (!(stack.getItemMeta() instanceof final FireworkMeta meta)) {
            if (stack.getItemMeta() instanceof FireworkEffectMeta) {
                return List.of(0);
            }

            Utils.tell(user, "<red>You item is not a firework or firework star!");
            return Collections.emptyList();
        }

        return IntStream.rangeClosed(0, meta.getEffectsSize() - 1).boxed().toList();
    }

    private int clearEffects(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!(stack.getItemMeta() instanceof final FireworkMeta meta)) {
            if (stack.getItemMeta() instanceof final FireworkEffectMeta fireworkEffectMeta) {
                fireworkEffectMeta.setEffect(null);
                stack.setItemMeta(fireworkEffectMeta);
                Utils.tell(user, "<primary>Removed the firework effect from your firework star!");
                return 1;
            }

            Utils.tell(user, "<red>You item is not a firework or firework star!");
            return 0;
        }

        meta.clearEffects();
        stack.setItemMeta(meta);

        Utils.tell(user, "<primary>Cleared all effects from your firework!");
        return 1;
    }

    private int removeEffect(final @NotNull CommandContext<CommandSourceStack> context, int effectLocation) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }
        if (!(stack.getItemMeta() instanceof final FireworkMeta meta)) {
            if (stack.getItemMeta() instanceof FireworkEffectMeta) {
                return clearEffects(context);
            }

            Utils.tell(user, "<red>You item is not a firework!");
            return 0;
        }
        final FireworkEffect effect = meta.getEffects().get(effectLocation);
        meta.removeEffect(effectLocation);
        stack.setItemMeta(meta);
        Utils.tell(user, "<primary>Removed the <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>firework effect</hover></secondary> from your firework!\n<gray><i>Hover over firework effect to see the effect you removed!");

        return 1;
    }

    private int addFireWorkEffect(final @NotNull CommandContext<CommandSourceStack> context,
                                  final List<Color> colors, final List<Color> fades,
                                  final FireworkEffect.Type shape, final boolean flicker, final boolean trail) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final FireworkEffect effect = FireworkEffect.builder().withColor(colors).withFade(fades).with(shape).flicker(flicker).trail(trail).build();

        if (!(stack.getItemMeta() instanceof final FireworkMeta meta)) {
            if (stack.getItemMeta() instanceof final FireworkEffectMeta fireworkEffectMeta) {
                fireworkEffectMeta.setEffect(effect);
                stack.setItemMeta(fireworkEffectMeta);

                Utils.tell(user, "<primary>Set the firework effect to: <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + getShortEffect(effect) + "</hover></secondary>.");
                return 1;
            }

            Utils.tell(user, "<red>You item is not a firework or firework star!");
            return 0;
        }

        // Set the effect and update the meta.
        meta.addEffect(effect);
        stack.setItemMeta(meta);
        Utils.tell(user, "<primary>Add the firework effect: <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + getShortEffect(effect) + "</hover></secondary>.");

        return 1;
    }

    private int updateFireworkPower(final @NotNull CommandContext<CommandSourceStack> context, final int power) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (stack.getItemMeta() instanceof final FireworkMeta meta) {
            meta.setPower(power);
            stack.setItemMeta(meta);
            Utils.tell(user, "<primary>Set the power of your firework to <secondary>" + power + "</secondary>.");
            return 1;
        } else {
            Utils.tell(user, "<red>You item is not a firework!");
            return 0;
        }
    }


    private int openMenu(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (!(stack.getItemMeta() instanceof final FireworkMeta meta)) {
            if (stack.getItemMeta() instanceof final FireworkEffectMeta fireworkEffectMeta) {
                new FireworkStarMenu(stack, fireworkEffectMeta).displayTo(MenuUtils.getManager().getMenuHolder(user));
                return 1;
            }

            Utils.tell(user, "<red>You item is not a firework or firework star!");
            return 0;
        }

        new FireworkMenu(stack, meta).displayTo(MenuUtils.getManager().getMenuHolder(user));
        return 1;
    }

    // Gets a short effect string.
    private @NotNull String getShortEffect(final @NotNull FireworkEffect effect) {
        // Start the builder with the colors and shape, these are defaults.
        final StringBuilder builder = new StringBuilder("Colors: " + effect.getColors().size() + ", Shape: " + effect.getType().name().toLowerCase().replace("_", " "));

        // Check if we have fades and if so append them.
        if (!effect.getFadeColors().isEmpty()) {
            builder.append(", ").append("Fade Colors: ").append(effect.getFadeColors().size());
        }

        // Check if we have a flicker, we always show this but say if it has it or not.
        if (effect.hasFlicker()) {
            builder.append(", ").append("Flicker: <green>yes</green>");
        } else {
            builder.append(", ").append("Flicker: <red>no</red>");
        }

        // Check if we have a trail, we always show this but say if it has it or not.
        if (effect.hasTrail()) {
            builder.append(", ").append("Trail: <green>yes</green>");
        } else {
            builder.append(", ").append("Trail: <red>no</red>");
        }

        return builder.toString();
    }

    // Get the hover effect from the effect.
    @Contract(pure = true)
    private @NotNull String getHoverFromEffect(final @NotNull FireworkEffect effect) {
        // Get our colors and fades and build a comma seperated string.
        final List<String> colorList = new ArrayList<>();
        effect.getColors().forEach((color) -> {
            // Check if it's a valid dye color.
            if (DyeColor.getByFireworkColor(color) != null) {
                colorList.add(Objects.requireNonNull(DyeColor.getByFireworkColor(color)).name().toLowerCase());
            } else {
                // Not a valid dye color, go ahead and get the hex.
                final String forColor = "#" + Integer.toHexString(color.asRGB());
                colorList.add("<c:" + forColor + ">#" + Integer.toHexString(color.asRGB()) + "</c>");
            }
        });

        final List<String> fadesList = new ArrayList<>();
        effect.getFadeColors().forEach((color) -> {
            // Check if it's a valid dye color.
            if (DyeColor.getByFireworkColor(color) != null) {
                fadesList.add(Objects.requireNonNull(DyeColor.getByFireworkColor(color)).name().toLowerCase());
            } else {
                // Not a valid dye color, go ahead and get the hex.
                final String forColor = "#" + Integer.toHexString(color.asRGB());
                fadesList.add("<c:" + forColor + ">#" + Integer.toHexString(color.asRGB()) + "</c>");
            }
        });

        final String colors = String.join(", ", colorList);
        final String fades = String.join(", ", fadesList);

        return """
                <primary>Shape: <secondary>{shape}</secondary>
                Effects: <secondary>{effects}</secondary>
                Colors: <secondary>{colors}</secondary>
                Fades: <secondary>{fades}</secondary>"""
                .replace("{shape}", effect.getType().name().toLowerCase().replace("_", " "))
                .replace("{effects}", (effect.hasFlicker() ? "flicker" + (effect.hasTrail() ? ", " : "") : "") + (effect.hasTrail() ? "trail" : ""))
                .replace("{colors}", WrapUtils.wrap(colors, 35, "|"))
                .replace("{fades}", WrapUtils.wrap(fades, 35, "|"))
                ;
    }
}
