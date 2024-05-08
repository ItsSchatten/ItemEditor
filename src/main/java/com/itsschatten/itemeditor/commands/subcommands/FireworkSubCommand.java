package com.itsschatten.itemeditor.commands.subcommands;

import com.google.common.collect.Lists;
import com.itsschatten.itemeditor.menus.FireworkMenu;
import com.itsschatten.itemeditor.menus.FireworkStarMenu;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.StringWrapUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

public class FireworkSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public FireworkSubCommand(@NotNull CommandBase owningCommand) {
        super("firework", List.of("fw"), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><menu|power|clear|remove></secondary>").hoverEvent(StringUtil.color("""
                <primary>Manipulate a firework or firework star via a GUI or command.
                <gray><i>Tab complete is heavily recommended for messing with colors, shapes, and effects when using the command.</i></gray>
                \s
                ◼ <secondary>[menu]<optional></secondary> Opens the firework edit menu, this is the default if no args are provided.
                ◼ <secondary><power><first></secondary> Set the power of a firework, fails if given a firework star.
                ◼ <secondary><remove><first></secondary> Remove a specific effect on a firework or clears the effect on a firework star.
                ◼ <secondary><clear><first></secondary> Clear all firework effects from a firework or firework star.
                ◼ <secondary><color:><first></secondary> Set the primary colors of the firework effect.
                ◼ <secondary><fade:><first></secondary> Set the fade colors of the firework effect.
                ◼ <secondary><shape:><first></secondary> Set the shape of the firework effect.
                ◼ <secondary><effects:><first></secondary> Set the effects of the firework effect.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
    }

    @Override
    protected void run(@NotNull Player user, String[] args) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            returnTell("<red>You need to be holding an item in your hand.");
            return;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final FireworkMeta meta)) {

            if (stack.getItemMeta() instanceof final FireworkEffectMeta fireworkEffectMeta) {
                // Default to opening the menu.
                if (args.length == 0) {
                    new FireworkStarMenu(stack, fireworkEffectMeta).displayTo(Utils.getManager().getMenuHolder(user));
                    return;
                }

                switch (args[0].toLowerCase()) {
                    // Opens the menu.
                    case "menu" -> {
                        new FireworkStarMenu(stack, fireworkEffectMeta).displayTo(Utils.getManager().getMenuHolder(user));
                        return;
                    }

                    // Handle the power, we cannot set the power on a Firework star, so we ignore it.
                    case "power" -> {
                        tell("<red>You cannot set power on a firework star!");
                        return;
                    }

                    // Removes the firework effect from the item.
                    case "remove", "clear" -> {
                        fireworkEffectMeta.setEffect(null);
                        stack.setItemMeta(fireworkEffectMeta);
                        tell("<primary>Removed the firework effect from your firework star!");
                        return;
                    }

                    default -> {
                        // Build a firework effect based on the args.
                        final FireworkEffect effect = getEffect(args);
                        // Return if null, error message(s) should've been sent.
                        if (effect == null) return;

                        // Set the effect and update the meta.
                        fireworkEffectMeta.setEffect(effect);
                        stack.setItemMeta(fireworkEffectMeta);
                        tell("<primary>Set the firework effect to: <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + getShortEffect(effect) + "</hover></secondary>.");
                    }
                }
                return;
            }

            returnTell("<red>Your item is not a firework!");
            return;
        }

        // We need arguments.
        if (args.length == 0) {
            new FireworkMenu(stack, meta).displayTo(Utils.getManager().getMenuHolder(user));
            return;
        }

        switch (args[0].toLowerCase()) {
            // Opens the menu.
            case "menu" -> {
                new FireworkMenu(stack, meta).displayTo(Utils.getManager().getMenuHolder(user));
                return;
            }

            // Handle the power, we cannot set the power on a Firework star, so we ignore it.
            case "power" -> {
                if (args.length < 2) {
                    returnTell("<red>Please provide the power you wish to apply.");
                    return;
                }
                final int power = getNumber(1, "<yellow>" + args[1] + "</yellow><red> is not a valid number.");

                meta.setPower(Math.max(0, Math.min(4, power)));
                tell("<primary>Set the power of your firework to <secondary>" + power + "</secondary>.");
            }

            // Removes the firework effect from the item.
            case "remove" -> {
                // Ignore if we have no effects.
                if (meta.getEffectsSize() == 0) {
                    tell("<red>Your firework contains no effects!");
                    return;
                }

                // Make sure we have an argument.
                if (args.length < 2) {
                    returnTell("<red>Please provide the location of the effect you wish to remove.");
                    return;
                }
                final int effectId = getNumber(1, "<yellow>" + args[1] + "</yellow><red> is not a valid number.");

                // Get the effect before we remove it.
                final FireworkEffect effect = meta.getEffects().get(effectId);
                meta.removeEffect(effectId);
                tell("<primary>Removed the <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>firework effect</hover></secondary> from your firework!\n<gray><i>Hover over firework effect to see the effect you removed!");
            }

            // Remove all firework effects.
            case "clear" -> {
                // Clear the fireworks.
                meta.clearEffects();

                tell("<primary>Cleared all effects from your firework!");
            }

            default -> {
                // Build a firework effect based on the args.
                final FireworkEffect effect = getEffect(args);
                // Return if null, error message(s) should've been sent.
                if (effect == null) return;

                // Set the effect and update the meta.
                meta.addEffect(effect);
                tell("<primary>Add the firework effect: <secondary><hover:show_text:'" + getHoverFromEffect(effect) + "'>" + getShortEffect(effect) + "</hover></secondary>.");
            }
        }

        stack.setItemMeta(meta);
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
                .replace("{colors}", StringWrapUtils.wrap(colors, 35, "|"))
                .replace("{fades}", StringWrapUtils.wrap(fades, 35, "|"))
                ;
    }

    // Builds a firework effect from the provided arguments.
    private @Nullable FireworkEffect getEffect(final @NotNull String[] args) {
        // Build a list from the argument array.
        final List<String> arguments = new ArrayList<>(Arrays.stream(args).toList());

        // Storage list of main colors.
        final List<Color> colorList = new ArrayList<>();
        // Make sure we have the "color:" argument.
        if (containsPartial(arguments, "color:")) {
            final int index = getIndex(arguments, "color:");

            // Shouldn't ever return true.
            if (index == -1) {
                returnTell("<red>Detected 'color:' but couldn't find it.");
                return null;
            }

            // The full string, then split by commas.
            final String colors = arguments.get(index).substring(6);
            final String[] split = colors.split(",");

            // Loop the colors and add them to the list.
            for (final String clr : split) {
                try {
                    // Get the firework color from the dye.
                    colorList.add(DyeColor.valueOf(clr.toUpperCase()).getFireworkColor());
                } catch (IllegalArgumentException ignored) {
                    if (clr.startsWith("#") && clr.length() == 7) {
                        // Get RGB.
                        final int red = Integer.valueOf(clr.substring(1, 3), 16);
                        final int green = Integer.valueOf(clr.substring(3, 4), 16);
                        final int blue = Integer.valueOf(clr.substring(5, 7), 16);
                        final Color color = Color.fromRGB(red, green, blue);

                        colorList.add(color);
                        continue;
                    }

                    tell("<gray>'" + clr + "' is not a valid color, must be a color name or a hex color string (#hexclr).");
                }
            }
        } else {
            // We have to return because we require a color for firework effects.
            returnTell("<red>You must set at least one color for your firework.");
            return null;
        }

        // Make sure we have colors.
        if (colorList.isEmpty()) {
            returnTell("<red>Couldn't find any valid colors!");
            return null;
        }

        // Storage list of fade colors.
        final List<Color> fadeList = new ArrayList<>();
        // Check if we have the "fade:" argument.
        if (containsPartial(arguments, "fade:")) {
            final int index = getIndex(arguments, "fade:");
            // Shouldn't ever return true.
            if (index == -1) {
                returnTell("<red>Detected 'fade:' but couldn't find it.");
                return null;
            }

            // The full string, then split by commas.
            final String colors = arguments.get(index).substring(5);
            final String[] split = colors.split(",");

            // Loop the colors and add them to the list.
            for (final String clr : split) {
                try {
                    // Get the firework color from the dye.
                    fadeList.add(DyeColor.valueOf(clr.toUpperCase()).getFireworkColor());
                } catch (IllegalArgumentException ignored) {
                    if (clr.startsWith("#") && clr.length() == 7) {
                        // Get RGB.
                        final int red = Integer.valueOf(clr.substring(1, 3), 16);
                        final int green = Integer.valueOf(clr.substring(3, 4), 16);
                        final int blue = Integer.valueOf(clr.substring(5, 7), 16);
                        final Color color = Color.fromRGB(red, green, blue);

                        fadeList.add(color);
                        continue;
                    }

                    tell("<gray>'" + clr + "' is not a valid color, must be a color name or a hex color string (#hexclr).");
                }
            }
        }

        // Defaults to ball.
        FireworkEffect.Type shape = FireworkEffect.Type.BALL;
        if (containsPartial(arguments, "shape:")) {
            final int index = getIndex(arguments, "shape:");
            // Shouldn't ever return true.
            if (index == -1) {
                returnTell("<red>Detected 'shape:' but couldn't find it.");
                return null;
            }

            // Get the shape and set it.
            final String shapeArg = arguments.get(index).substring(6);
            try {
                shape = FireworkEffect.Type.valueOf(shapeArg.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                tell("<gray>'" + shapeArg + "' is not a valid shape, it has been defaulted back to small ball.");
            }
        } else {
            tell("<gray>Because you didn't provide a shape it has been defaulted to ball.");
        }

        boolean flicker = false;
        boolean trail = false;
        if (containsPartial(arguments, "effect:")) {
            final int index = getIndex(arguments, "effect:");
            // Shouldn't ever return true.
            if (index == -1) {
                returnTell("<red>Detected 'effect:' but couldn't find it.");
                return null;
            }

            // Get the shape and set it.
            final String effects = arguments.get(index).substring(7).toLowerCase();
            flicker = effects.contains("twinkle") || effects.contains("flicker");
            trail = effects.contains("trail");
        }

        // The builder to use.
        final FireworkEffect.Builder effect = FireworkEffect.builder();

        // Set the colors, this is required.
        effect.withColor(colorList);
        // Set the shape, this is required.
        effect.with(shape);

        // Check if we have fade colors if we do add them.
        if (!fadeList.isEmpty()) {
            effect.withFade(fadeList);
        }

        // Set flicker and trail.
        effect.flicker(flicker);
        effect.trail(trail);
        return effect.build();
    }

    // Utility method to check if a list contains a starts with a string.
    private boolean containsPartial(final @NotNull List<String> strings, final String partial) {
        return strings.stream().anyMatch(s -> s.startsWith(partial));
    }

    // Utility method to find an index based on a partial, this will only ever return the first iteration.
    private int getIndex(final @NotNull List<String> list, final String search) {
        return IntStream.range(0, list.size())
                .filter(i -> list.get(i).contains(search))
                .findFirst()
                .orElse(-1);
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {

            // TODO: Turn this into my variants.

            // Taken from Essential's Commandfirework.
            // https://github.com/EssentialsX/Essentials/blob/2.x/Essentials/src/main/java/com/earth2me/essentials/commands/Commandfirework.java
            // Note: this enforces an order of color fade shape effect, which the actual command doesn't have.  But that's fine.
            if (args.length == 1) {
                final List<String> options = Lists.newArrayList();
                if (args[0].startsWith("color:")) {
                    final String prefix;
                    if (args[0].contains(",")) {
                        prefix = args[0].substring(0, args[0].lastIndexOf(',') + 1);
                    } else {
                        prefix = "color:";
                    }
                    for (final DyeColor color : DyeColor.values()) {
                        options.add(prefix + color.name().toLowerCase() + ",");
                    }
                    return options;
                }
                options.add("clear");
                options.add("power");
                options.add("color:");
                options.add("remove");
                options.add("menu");
                return options;
            } else if (args.length == 2) {
                if (args[0].equals("power") && (sender instanceof Player player) && player.getInventory().getItemInMainHand().getItemMeta() instanceof FireworkMeta) {
                    return Lists.newArrayList("0", "1", "2", "3", "4");
                } else if (args[0].equalsIgnoreCase("remove") && (sender instanceof Player player) && player.getInventory().getItemInMainHand().getItemMeta() instanceof FireworkMeta meta) {
                    if (meta.getEffects().size() > 1) {
                        return IntStream.rangeClosed(0, meta.getEffectsSize() - 1).mapToObj(Integer::toString).toList();
                    } else {
                        return List.of("0");
                    }
                } else if (args[0].startsWith("color:")) {
                    final List<String> options = Lists.newArrayList();
                    if (!args[1].startsWith("fade:")) {
                        args[1] = "fade:";
                    }
                    final String prefix;
                    if (args[1].contains(",")) {
                        prefix = args[1].substring(0, args[1].lastIndexOf(',') + 1);
                    } else {
                        prefix = "fade:";
                    }
                    for (final DyeColor color : DyeColor.values()) {
                        options.add(prefix + color.name().toLowerCase() + ",");
                    }
                    return options;
                } else {
                    return Collections.emptyList();
                }
            } else if (args.length == 3 && args[0].startsWith("color:")) {
                return Lists.newArrayList("shape:star", "shape:ball", "shape:large", "shape:creeper", "shape:burst");
            } else if (args.length == 4 && args[0].startsWith("color:")) {
                return Lists.newArrayList("effect:trail", "effect:twinkle", "effect:trail,twinkle", "effect:twinkle,trail");
            } else {
                return Collections.emptyList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
