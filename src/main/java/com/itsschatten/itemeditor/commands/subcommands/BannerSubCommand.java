package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.menus.BannerMenu;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Subcommand responsible for handling banners.
 */
public class BannerSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public BannerSubCommand(@NotNull CommandBase owningCommand) {
        super("banner", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><add|remove|-clear|-view> [pattern] [color]").hoverEvent(StringUtil.color("""
                <primary>Add or remove an attribute from an item.
                \s
                ◼ <secondary><add><required> <pattern><required> <color><optional> </secondary> Add a banner patten to your banner, defaults to white if no color is provided.
                ◼ <secondary><menu><optional></secondary> Open's the banner creator menu.
                ◼ <secondary><remove><required></secondary> Remove a pattern from the item.
                ◼ <secondary><-clear><required></secondary> Clear all patterns on the item.
                ◼ <secondary><-view><optional></secondary> View all patterns on the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
        if (!(stack.getItemMeta() instanceof final BannerMeta meta)) {
            returnTell("<red>You aren't holding a banner!");
            return;
        }

        // We need something to do to the banner.
        if (args.length == 0) {
            new BannerMenu(stack, meta).displayTo(Utils.getManager().getMenuHolder(user));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                // We need a pattern.
                if (args.length == 1) {
                    returnTell("<red>Please provide a pattern to add to your banner.");
                    return;
                }

                // Get the banner type from the registry.
                final NamespacedKey patternKey = args[1].contains(":") ? NamespacedKey.fromString(args[1]) : NamespacedKey.minecraft(args[1]);
                if (patternKey == null) {
                    returnTell("<red>Could not find any enchantment by the name <yellow>" + args[1] + "</yellow>.");
                    return;
                }
                final PatternType patternType = Registry.BANNER_PATTERN.get(patternKey);
                if (patternType == null) {
                    returnTell("<red>Could not find any enchantment by the name <yellow>" + args[1] + "</yellow>.");
                    return;
                }

                // Build the pattern, default to white color if none are provided.
                final Pattern pattern = new Pattern(args.length == 2 ? DyeColor.WHITE : DyeColor.valueOf(args[2].toUpperCase()), patternType);
                meta.addPattern(pattern);

                tell("<primary>Add the pattern <secondary>" + patternType.key().asString() + "</secondary> with the color " +
                        "<c:#" + Integer.toHexString(pattern.getColor().getColor().asRGB()) + ">" + pattern.getColor().name().toLowerCase().replace("_", " ") + "</c>.");
            }

            case "remove" -> {
                // We need the location of the pattern to remove.
                if (args.length == 1) {
                    returnTell("<red>Please provide the pattern to remove from your banner!");
                    return;
                }

                // Get the number, and make sure it doesn't exceed the maximum amount.
                // Also bind it to 0 or higher.
                final int number = Math.max(getNumber(1, "<yellow>" + args[1] + " <red>is not a valid integer."), 0);
                if (number > meta.getPatterns().size()) {
                    returnTell("<red>That pattern doesn't exist on your item!");
                    return;
                }

                // Get the pattern, mainly for the message.
                final Pattern pattern = meta.getPattern(number);
                meta.removePattern(number);
                tell("<primary>Removed the pattern <secondary>" + pattern.getPattern().key().asString() + "</secondary> with color " +
                        "<c:#" + Integer.toHexString(pattern.getColor().getColor().asRGB()) + ">" + pattern.getColor().name().toLowerCase().replace("_", " ") + "</c> from your item.");
            }

            case "menu" -> {
                new BannerMenu(stack, meta).displayTo(Utils.getManager().getMenuHolder(user));
                return;
            }

            case "-clear" -> {
                meta.setPatterns(new ArrayList<>());
                tell("<primary>Cleared all patterns from your banner.");
            }

            case "-view" -> {
                // If we have no patterns, say so.
                if (meta.getPatterns().isEmpty()) {
                    tell("<primary>Your banner doesn't have any patterns.");
                    return;
                }

                tell("<primary>Your banner has the following patterns:");
                tell("<primary>Your banner's base color is: <secondary>" + stack.getType().getKey().getKey().toLowerCase().replace("_banner", ""));
                meta.getPatterns().forEach((pattern) -> tell("<primary>◼ <c:#" + Integer.toHexString(pattern.getColor().getColor().asRGB()) + ">" + pattern.getColor().name().toLowerCase().replace("_", " ") + "</c> " +
                        "<secondary>" + pattern.getPattern().key().asString() + "</secondary>"));
                return;
            }
        }

        stack.setItemMeta(meta);
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("add", "remove", "menu", "-view", "-clear").filter((name) -> name.contains(args[0].toLowerCase())).toList();
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add")) {
                    return Registry.BANNER_PATTERN.stream().map((pattern) -> pattern.key().asString()).filter((name) -> name.contains(args[1].toLowerCase())).toList();
                }

                if (args[0].equalsIgnoreCase("remove") &&
                        sender instanceof final Player player &&
                        player.getInventory().getItemInMainHand().getItemMeta() instanceof final BannerMeta meta) {

                    // Ignore if we have no patterns.
                    if (meta.getPatterns().isEmpty()) {
                        return super.getTabComplete(sender, args);
                    }

                    return (meta.getPatterns().size() == 1 ? Stream.of("0") : IntStream.rangeClosed(0, meta.getPatterns().size() - 1).mapToObj(Integer::toString)).filter((name) -> name.contains(args[1].toLowerCase())).toList();
                }
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
                return Arrays.stream(DyeColor.values()).map((color) -> color.name().toLowerCase()).filter((name) -> name.contains(args[2].toLowerCase())).toList();
            }
        }

        return super.getTabComplete(sender, args);
    }
}
