package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.ColorArgument;
import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ColorSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie color<secondary><red|color name|#hexclr|clear|-view> \\<green> \\<blue></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the dye color of armor and the color of a potion.
                \s
                ◼ <secondary>\\<red><required> \\<green><required> \\<blue><required></secondary> Red, green, and blue values.
                ◼ <secondary><color name><required></secondary> Dye color name to apply to your item.
                ◼ <secondary><rbg><required></secondary> Dye color name to apply to your item.
                ◼ <secondary><#hexclr|hexclr><required></secondary> Hex color string to apply to your item.
                ◼ <secondary><clear><required></secondary> Clear the color of your item.
                ◼ <secondary><-view><optional></secondary> View the current color on your item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie color "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("color")
                .then(Commands.argument("red", IntegerArgumentType.integer(0))
                        .then(Commands.argument("green", IntegerArgumentType.integer(0, 255))
                                .then(Commands.argument("blue", IntegerArgumentType.integer(0, 255))
                                        .executes(context -> handleColor(context, Color.fromRGB(Math.min(IntegerArgumentType.getInteger(context, "red"), 255), IntegerArgumentType.getInteger(context, "green"), IntegerArgumentType.getInteger(context, "blue"))))
                                )
                        )
                        .executes(context -> handleColor(context, Color.fromRGB(IntegerArgumentType.getInteger(context, "red"))))
                )
                .then(Commands.argument("hex", ColorArgument.color())
                        .executes(context -> handleColor(context, context.getArgument("hex", Color.class)))
                )
                .then(Commands.argument("color", GenericEnumArgument.generic(DyeColor.class))
                        .executes(context -> handleColor(context, context.getArgument("color", DyeColor.class).getColor()))
                )
                .then(Commands.literal("-view")
                        .executes(this::handleView)
                )
                .then(Commands.literal("-clear")
                        .executes(this::handleReset)
                );
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        switch (stack.getItemMeta()) {
            case final LeatherArmorMeta meta -> {
                // Check if the color is equal to the default.
                if (meta.getColor().equals(Bukkit.getItemFactory().getDefaultLeatherColor())) {
                    Utils.tell(user, "<primary>Your item's color is currently <secondary>default</secondary>.");
                } else {
                    // Check if we have a DyeColor used.
                    final DyeColor color = DyeColor.getByColor(meta.getColor());
                    sendViewMessage(user, color, meta.getColor());
                }

                return 1;
            }

            case final PotionMeta meta -> {
                // Check if the color is equal to the default.
                if (!meta.hasColor()) {
                    Utils.tell(user, "<primary>Your item's color is currently <secondary>default</secondary>.");
                } else {
                    // Check if we have a DyeColor used.
                    final DyeColor color = DyeColor.getByColor(Objects.requireNonNull(meta.getColor()));
                    sendViewMessage(user, color, meta.getColor());
                }
                return 1;
            }

            default -> {
                Utils.tell(user, "<red>Your item cannot be dyed.");
                return 0;
            }
        }
    }

    private int handleReset(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        switch (stack.getItemMeta()) {
            case final LeatherArmorMeta meta -> {
                meta.setColor(null);
                stack.setItemMeta(meta);

                Utils.tell(user, "<primary>Your item's color has been reset to <secondary>default</secondary>.");
                return 1;
            }

            case final PotionMeta meta -> {
                meta.setColor(null);
                stack.setItemMeta(meta);

                Utils.tell(user, "<primary>Your item's color has been reset to <secondary>default</secondary>.");
                return 1;
            }

            default -> {
                Utils.tell(user, "<red>Your item cannot be dyed.");
                return 0;
            }
        }
    }

    private int handleColor(final @NotNull CommandContext<CommandSourceStack> context, final Color color) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        switch (stack.getItemMeta()) {
            case final LeatherArmorMeta meta -> {
                // Set the color and update the meta.
                meta.setColor(color);
                stack.setItemMeta(meta);

                // Get the dye color if applicable.
                final DyeColor dyeColor = DyeColor.getByColor(color);
                if (dyeColor == null) {
                    Utils.tell(user, "<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + Integer.toHexString(color.asRGB()) + "</c>.");
                } else {
                    Utils.tell(user, "<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + dyeColor.name().toLowerCase().replace("_", " ") + "</c>.");
                }
                return 1;
            }

            case final PotionMeta meta -> {
                // Set the color and update the meta.
                meta.setColor(color);
                stack.setItemMeta(meta);

                // Get the dye color if applicable.
                final DyeColor dyeColor = DyeColor.getByColor(color);
                if (dyeColor == null) {
                    Utils.tell(user, "<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + Integer.toHexString(color.asRGB()) + "</c>.");
                } else {
                    Utils.tell(user, "<primary>Your item's color is now <c:#" + Integer.toHexString(color.asRGB()) + ">" + dyeColor.name().toLowerCase().replace("_", " ") + "</c>.");
                }
                return 1;
            }

            default -> {
                Utils.tell(user, "<red>Your item cannot be dyed.");
                return 0;
            }
        }
    }

    private void sendViewMessage(Player user, DyeColor possibleDye, Color actualColor) {
        if (possibleDye == null) {
            Utils.tell(user, "<primary>Your item's color is currently <c:#" + Integer.toHexString(actualColor.asRGB()) + ">" + Integer.toHexString(actualColor.asRGB()) + "</c>.");
        } else {
            Utils.tell(user, "<primary>Your item's color is currently <c:#" + Integer.toHexString(actualColor.asRGB()) + ">" + possibleDye.name().toLowerCase().replace("_", " ") + "</c>.");
        }
    }
}
