package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.menus.BannerMenu;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.itsschatten.yggdrasil.menus.MenuUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.RegistryArgumentExtractor;
import io.papermc.paper.registry.PaperRegistryAccess;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

// FIXME: Use methods. or more of them.

/**
 * Subcommand responsible for handling banners.
 */
public final class BannerSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie banner <secondary><add|remove|-clear|-view> [pattern] [color]").hoverEvent(StringUtil.color("""
                <primary>Add or remove an attribute from an item.
                \s
                ◼ <secondary><add><required> <pattern><required> <color><optional> </secondary> Add a banner patten to your banner, defaults to white if no color is provided.
                ◼ <secondary><menu><optional></secondary> Open's the banner creator menu.
                ◼ <secondary><remove><required></secondary> Remove a pattern from the item.
                ◼ <secondary><-clear><required></secondary> Clear all patterns on the item.
                ◼ <secondary><-view><optional></secondary> View all patterns on the item.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie banner"));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("banner")
                .then(literal("menu")
                        .executes(context -> {
                            openMenu(context.getSource());
                            return 1;
                        })
                )
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            if (!(stack.getItemMeta() instanceof final BannerMeta meta)) {
                                Utils.tell(user, "<red>You aren't holding a banner!");
                                return 0;
                            }

                            // If we have no patterns, say so.
                            if (meta.getPatterns().isEmpty()) {
                                Utils.tell(user, "<primary>Your banner doesn't have any patterns.");
                                return 0;
                            }

                            Utils.tell(user, "<primary>Your banner has the following patterns:");
                            Utils.tell(user, "<primary>Your banner's base color is: <secondary>" + stack.getType().getKey().getKey().toLowerCase().replace("_banner", ""));
                            meta.getPatterns().forEach((pattern) -> Utils.tell(user, "<primary>◼ <c:#" + Integer.toHexString(pattern.getColor().getColor().asRGB()) + ">" + pattern.getColor().name().toLowerCase().replace("_", " ") + "</c> " +
                                    "<secondary>" + Objects.requireNonNull(RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKey(pattern.getPattern())).asString() + "</secondary>"));
                            return 1;
                        })
                )
                .then(literal("-clear")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            if (!(stack.getItemMeta() instanceof final BannerMeta meta)) {
                                Utils.tell(user, "<red>You aren't holding a banner!");
                                return 0;
                            }

                            // If we have no patterns, say so.
                            if (meta.getPatterns().isEmpty()) {
                                Utils.tell(user, "<primary>Your banner doesn't have any patterns.");
                                return 0;
                            }


                            meta.setPatterns(new ArrayList<>());
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Cleared all patterns from your banner.");
                            return 1;
                        })

                )
                .then(literal("add")
                        .then(argument("pattern", ArgumentTypes.resourceKey(RegistryKey.BANNER_PATTERN))
                                .then(argument("color", GenericEnumArgument.generic(DyeColor.class))
                                        .executes(context -> {
                                            final PatternType type = PaperRegistryAccess.instance().getRegistry(RegistryKey.BANNER_PATTERN).get(RegistryArgumentExtractor.getTypedKey(context, RegistryKey.BANNER_PATTERN, "pattern").key());
                                            final DyeColor color = context.getArgument("color", DyeColor.class);

                                            addPattern(context.getSource(), type, color);
                                            return 1;
                                        })
                                )
                                .executes(context -> {
                                    final PatternType type = PaperRegistryAccess.instance().getRegistry(RegistryKey.BANNER_PATTERN).get(RegistryArgumentExtractor.getTypedKey(context, RegistryKey.BANNER_PATTERN, "pattern").key());
                                    addPattern(context.getSource(), type, DyeColor.WHITE);
                                    return 1;
                                })
                        )
                )
                .then(literal("remove")
                        .then(argument("pattern", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    removePattern(context.getSource(), IntegerArgumentType.getInteger(context, "pattern"));
                                    return 1;
                                })
                        )
                )
                .executes(context -> {
                    openMenu(context.getSource());
                    return 1;
                });
    }

    private void openMenu(final @NotNull CommandSourceStack source) {
        final Player user = (Player) source.getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final BannerMeta meta)) {
            Utils.tell(user, "<red>You aren't holding a banner!");
            return;
        }

        new BannerMenu(stack, meta).displayTo(MenuUtils.getManager().getMenuHolder(user));
    }

    private void addPattern(final CommandSourceStack source, final PatternType type, final DyeColor color) {
        if (type == null) {
            Utils.tell(source, "<red>Failed to find a banner pattern!");
            return;
        }

        final Player user = (Player) source.getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final BannerMeta meta)) {
            Utils.tell(user, "<red>You aren't holding a banner!");
            return;
        }

        final Pattern pattern = new Pattern(color, type);
        meta.addPattern(pattern);
        stack.setItemMeta(meta);

        Utils.tell(user, "<primary>Added the pattern <secondary>" + Objects.requireNonNull(RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKey(type)).asString() + "</secondary> with the color " +
                "<c:#" + Integer.toHexString(pattern.getColor().getColor().asRGB()) + ">" + pattern.getColor().name().toLowerCase().replace("_", " ") + "</c>.");
    }

    private void removePattern(final @NotNull CommandSourceStack source, final int patternLocation) {
        final Player user = (Player) source.getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof final BannerMeta meta)) {
            Utils.tell(user, "<red>You aren't holding a banner!");
            return;
        }

        if (meta.getPatterns().isEmpty()) {
            Utils.tell(user, "<red>Your item has no patterns to remove!");
            return;
        }

        final int finalPattern = Math.min(patternLocation, meta.getPatterns().size() - 1);

        final Pattern pattern = meta.getPattern(finalPattern);

        meta.removePattern(finalPattern);
        stack.setItemMeta(meta);
        Utils.tell(user, "<primary>Removed the pattern <secondary>" + Objects.requireNonNull(RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKey(pattern.getPattern())).asString() + "</secondary> with color " +
                "<c:#" + Integer.toHexString(pattern.getColor().getColor().asRGB()) + ">" + pattern.getColor().name().toLowerCase().replace("_", " ") + "</c> from your item.");
    }
}
