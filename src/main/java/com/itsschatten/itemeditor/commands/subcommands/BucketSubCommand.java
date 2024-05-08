package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.DyeColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BucketSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public BucketSubCommand(@NotNull CommandBase owningCommand) {
        super("bucket", Collections.emptyList(), owningCommand);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><variant|pattern|body|bodycolor|patterncolor></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the fish variant within a bucket.
                \s
                <gray><i>If holding a tropical fish bucket.</i></gray>
                ◼ <secondary><pattern|tropical pattern><required> [color]<optional></secondary> Set the pattern and, if provided, the color.
                ◼ <secondary><body|bodycolor|patterncolor><required> <color><required></secondary> Set's the color of a body or a pattern.
                ◼ <secondary>[-view]<optional></secondary> View the current pattern, pattern color, and body color.
                <gray><i>If holding an axolotl bucket.</i></gray>
                ◼ <secondary><variant><required></secondary> The variant to set for the axolotl in the bucket.
                ◼ <secondary>[-view]<optional></secondary> View the current variant.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
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
            case final TropicalFishBucketMeta meta -> {
                if (args.length == 0) {
                    returnTell("<red>Please provide an operation to perform on your tropical fish.");
                    return;
                }

                // Switch our first argument, defaults to setting the pattern.
                switch (args[0].toLowerCase()) {
                    // View the variables for the tropical fish.
                    case "-view" -> {
                        if (meta.hasVariant()) {
                            tell("""
                                    <primary>Body Color: {body}
                                    Pattern: <secondary>{pattern}</secondary>
                                    Pattern Color: {color}"""
                                    .replace("{body}", "<c:" + Integer.toHexString(meta.getBodyColor().getColor().asRGB()) + ">" + meta.getBodyColor().name().toLowerCase().replace("_", " ") + "</c>")
                                    .replace("{color}", "<c:" + Integer.toHexString(meta.getPatternColor().getColor().asRGB()) + ">" + meta.getPatternColor().name().toLowerCase().replace("_", " ") + "</c>")
                                    .replace("{pattern}", meta.getPattern().name().toLowerCase().replace("_", " ")));
                        } else {
                            tell("<primary>Your bucket doesn't have a variant.");
                        }
                    }

                    // Defaults to setting the pattern.
                    default -> {
                        boolean setColor = false;
                        DyeColor dye = null;
                        // Get the dye color.
                        if (args.length > 1) {
                            try {
                                dye = DyeColor.valueOf(args[1].toUpperCase());

                                // Set the pattern color to the provided dye.
                                meta.setPatternColor(dye);
                                setColor = true;
                            } catch (IllegalArgumentException ignored) {
                                returnTell("<yellow>" + args[1].toLowerCase() + " <red>is an invalid dye color!");
                                return;
                            }
                        }

                        // Get the variant.
                        try {
                            final TropicalFish.Pattern variant = TropicalFish.Pattern.valueOf(args[0].toUpperCase());

                            // Set the variant.
                            meta.setPattern(variant);
                            stack.setItemMeta(meta);
                            tell("<primary>Set your tropical fish's pattern to <secondary>" + variant.name().toLowerCase() + "</secondary>" +
                                    (setColor ? " with the color <c:#" + Integer.toHexString(dye.getColor().asRGB()) + ">" + dye.name().toLowerCase().replace("_", " ") + "</c>" : "."));
                        } catch (IllegalArgumentException ignored) {
                            returnTell("<yellow>" + args[0] + "</yellow> <red>is an invalid tropical fish pattern.");
                        }
                    }

                    case "body", "bodycolor" -> {
                        // Get the dye color.
                        try {
                            final DyeColor dye = DyeColor.valueOf(args[0].toUpperCase());

                            // Set the variant.
                            meta.setBodyColor(dye);
                            stack.setItemMeta(meta);
                            tell("<primary>Set your tropical fish's body color to <c:#" + Integer.toHexString(dye.getColor().asRGB()) + ">" + dye.name().toLowerCase().replace("_", " ") + "</c>.");
                        } catch (IllegalArgumentException ignored) {
                            returnTell("<yellow>" + args[0] + "</yellow> is an invalid dye color.");
                        }
                    }

                    case "patterncolor" -> {
                        // Get the dye color.
                        try {
                            final DyeColor dye = DyeColor.valueOf(args[0].toUpperCase());

                            // Set the pattern color.
                            meta.setPatternColor(dye);
                            stack.setItemMeta(meta);
                            tell("<primary>Set your tropical fish's pattern color to <c:#" + Integer.toHexString(dye.getColor().asRGB()) + ">" + dye.name().toLowerCase().replace("_", " ") + "</c>.");
                        } catch (IllegalArgumentException ignored) {
                            returnTell("<yellow>" + args[0] + "</yellow> <red>is an invalid dye color.");
                        }
                    }
                }
            }

            // We have a bucket o' Axolotl.
            case final AxolotlBucketMeta meta -> {
                if (args.length == 0) {
                    returnTell("<red>Please provide axolotl variant.");
                    return;
                }

                // View the axolotl's variant.
                if (args[0].equalsIgnoreCase("-view")) {
                    if (meta.hasVariant()) {
                        tell("<primary>Your axolotl's variant is currently <secondary>" + meta.getVariant().name().toLowerCase().replace("_", " ") + "</secondary>");
                    } else {
                        tell("<primary>Your bucket doesn't have a variant.");
                    }
                    return;
                }

                // Get the variant.
                try {
                    final Axolotl.Variant variant = Axolotl.Variant.valueOf(args[0].toUpperCase());

                    // Set the variant.
                    meta.setVariant(variant);
                    stack.setItemMeta(meta);
                    tell("<primary>Set your axolotl variant to <secondary>" + variant.name().toLowerCase().replace("_", " ") + "</secondary>.");
                } catch (IllegalArgumentException ignored) {
                    returnTell("<yellow>" + args[0] + "</yellow> <red>is an invalid axolotl variant.");
                }
            }

            case null, default -> returnTell("<red>You are not holding a tropical fish bucket or an axolotl bucket!");
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (sender instanceof final Player player) {
                if (args.length == 1) {
                    // Get the item meta of the item in the main hand.
                    return switch (player.getInventory().getItemInMainHand().getItemMeta()) {
                        // Send the tropical fish patterns.
                        case TropicalFishBucketMeta ignored -> {
                            final List<String> strings = new ArrayList<>(Arrays.stream(TropicalFish.Pattern.values()).map(TropicalFish.Pattern::name).toList());
                            strings.add("patterncolor");
                            strings.add("pattern");
                            strings.add("body");
                            strings.add("bodycolor");

                            yield strings.stream().map(String::toLowerCase).filter((name) -> name.contains(args[0].toLowerCase())).toList();
                        }
                        // Send the axolotl variants.
                        case AxolotlBucketMeta ignored ->
                                Arrays.stream(Axolotl.Variant.values()).map(Axolotl.Variant::name).map(String::toLowerCase).filter((name) -> name.contains(args[0].toLowerCase())).toList();

                        case null, default -> super.getTabComplete(sender, args);
                    };
                }

                if (args.length == 2) {
                    if (player.getInventory().getItemInMainHand().getItemMeta() instanceof AxolotlBucketMeta) {
                        return super.getTabComplete(sender, args);
                    }

                    if (args[0].equalsIgnoreCase("pattern")) {
                        return Arrays.stream(TropicalFish.Pattern.values()).map(pattern -> pattern.name().toLowerCase()).filter(name -> name.contains(args[1].toLowerCase())).toList();
                    } else {
                        return Arrays.stream(DyeColor.values()).map(dye -> dye.name().toLowerCase()).filter((name) -> name.contains(args[1].toLowerCase())).toList();
                    }
                }

            }
        }

        return super.getTabComplete(sender, args);
    }
}
