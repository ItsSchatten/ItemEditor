package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ColorSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public ColorSubCommand(final CommandBase base) {
        super("color", List.of("dye"), base);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><red|color name|#hexclr|clear|-view> \\<green> \\<blue></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the dye color of armor and the color of a potion.
                \s
                ◼ <secondary>\\<red><required> \\<green><required> \\<blue><required></secondary> Red, green, and blue values.
                ◼ <secondary><color name><required></secondary> Dye color name to apply to your item.
                ◼ <secondary><#hexclr><required></secondary> Hex color string to apply to your item.
                ◼ <secondary><clear><required></secondary> Clear the color of your item.
                ◼ <secondary><-view><optional></secondary> View the current color on your item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        switch (stack.getItemMeta()) {
            case final LeatherArmorMeta meta -> {
                if (args.length >= 1) {
                    if (args[0].equalsIgnoreCase("-view")) {
                        // Check if the color is equal to the default.
                        if (meta.getColor().equals(Bukkit.getItemFactory().getDefaultLeatherColor())) {
                            tell("<primary>Your item's color is currently <secondary>default</secondary>.");
                        } else {
                            // Check if we have a DyeColor used.
                            final DyeColor color = DyeColor.getByColor(meta.getColor());
                            if (color == null) {
                                tell("<primary>Your item's color is currently <c:#" + Integer.toHexString(meta.getColor().asRGB()) + ">" + Integer.toHexString(meta.getColor().asRGB()) + "</c>.");
                            } else {
                                tell("<primary>Your item's color is currently <c:#" + Integer.toHexString(meta.getColor().asRGB()) + ">" + color.name().toLowerCase().replace("_", " ") + "</c>.");
                            }
                        }
                        return;
                    }

                    // Clears the color.
                    if (args[0].equalsIgnoreCase("clear")) {
                        meta.setColor(null);
                        stack.setItemMeta(meta);

                        tell("<primary>Your item's color has been reset to <secondary>default</secondary>.");
                        return;
                    }

                }

                // Get the color from the arguments.
                final Color color = getColor(args);
                if (color == null) {
                    returnTell("<red>The provided arguments cannot be converted to a color!\nPlease supply a color (name, rgb, or hex), 'clear' to clear the color, or '-view' to view the color.");
                    return;
                }

                // Set the color and update the meta.
                meta.setColor(color);
                stack.setItemMeta(meta);

                // Get the dye color if applicable.
                final DyeColor dyeColor = DyeColor.getByColor(color);
                if (dyeColor == null) {
                    tell("<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + Integer.toHexString(color.asRGB()) + "</c>.");
                } else {
                    tell("<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + dyeColor.name().toLowerCase().replace("_", " ") + "</c>.");
                }
            }

            case final PotionMeta meta -> {
                if (args.length >= 1) {
                    if (args[0].equalsIgnoreCase("-view")) {
                        // Check if the color is equal to the default.
                        if (!meta.hasColor()) {
                            tell("<primary>Your item's color is currently <secondary>default</secondary>.");
                        } else {
                            // Check if we have a DyeColor used.
                            final DyeColor color = DyeColor.getByColor(Objects.requireNonNull(meta.getColor()));
                            if (color == null) {
                                tell("<primary>Your item's color is currently <c:#" + Integer.toHexString(meta.getColor().asRGB()) + ">" + Integer.toHexString(meta.getColor().asRGB()) + "</c>.");
                            } else {
                                tell("<primary>Your item's color is currently <c:#" + Integer.toHexString(meta.getColor().asRGB()) + ">" + color.name().toLowerCase().replace("_", " ") + "</c>.");
                            }
                        }
                        return;
                    }

                    // Clears the color.
                    if (args[0].equalsIgnoreCase("clear")) {
                        meta.setColor(null);
                        stack.setItemMeta(meta);

                        tell("<primary>Your item's color has been reset to <secondary>default</secondary>.");
                        return;
                    }

                }

                // Get the color from the arguments.
                final Color color = getColor(args);
                if (color == null) {
                    returnTell("<red>The provided arguments cannot be converted to a color!\nPlease supply a color (name, rgb, or hex), 'clear' to clear the color, or '-view' to view the color.");
                    return;
                }

                // Set the color and update the meta.
                meta.setColor(color);
                stack.setItemMeta(meta);

                // Get the dye color if applicable.
                final DyeColor dyeColor = DyeColor.getByColor(color);
                if (dyeColor == null) {
                    tell("<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + Integer.toHexString(color.asRGB()) + "</c>.");
                } else {
                    tell("<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + dyeColor.name().toLowerCase().replace("_", " ") + "</c>.");
                }
            }

            case null, default -> returnTell("<red>Your item cannot be dyed.");
        }
    }

    private @Nullable Color getColor(final String @NotNull [] args) {
        // Make sure we have args.
        if (args.length == 0) {
            returnTell("<red>Please supply a color (name, rgb, or hex), 'clear' to clear the color, or '-view' to view the color.");
            return null;
        }

        // Check if we have three numbers.
        if ((args.length == 1 && StringUtils.isNumeric(args[0])) || (args.length == 2 && StringUtils.isNumeric(args[1]))) {
            returnTell("<red>You must supply 3 numbers to use RGB formatting.\n<gray>I.E: /ie color 200 50 0");
            return null;
        }

        // Get the three values to convert to RGB.
        if (args.length == 3) {
            final int red = Utils.range(getNumber(0, "<yellow>" + args[0] + " <red>is not a valid integer."), 0, 255);
            final int green = Utils.range(getNumber(1, "<yellow>" + args[1] + " <red>is not a valid integer."), 0, 255);
            final int blue = Utils.range(getNumber(2, "<yellow>" + args[2] + " <red>is not a valid integer."), 0, 255);
            return Color.fromRGB(red, green, blue);
        }

        if (args[0].startsWith("#")) {
            final int rgb = Integer.parseInt(args[0].substring(1), 16);
            return Color.fromRGB(rgb);
        } else {
            final DyeColor color = DyeColor.valueOf(args[0].toUpperCase());
            return color.getColor();
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                final List<String> strings = new ArrayList<>(Arrays.stream(DyeColor.values()).map(DyeColor::name).map(String::toLowerCase).toList());
                strings.add("clear");
                strings.add("-view");

                return strings.stream().filter((name) -> name.contains(args[0].toLowerCase())).toList();
            }
        }
        return super.getTabComplete(sender, args);
    }
}
