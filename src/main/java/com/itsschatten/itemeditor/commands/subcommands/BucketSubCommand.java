package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.DyeColor;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public final class BucketSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie bucket <secondary><variant|pattern|body|body color|pattern color></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the fish variant within a bucket.
                \s
                <gray><i>If holding a tropical fish bucket.</i></gray>
                ◼ <secondary><pattern|tropical pattern><required> [color]<optional></secondary> Set the pattern and, if provided, the color.
                ◼ <secondary><body|bodycolor|patterncolor><required> <color><required></secondary> Set's the color of a body or a pattern.
                ◼ <secondary>[-view]<optional></secondary> View the current pattern, pattern color, and body color.
                <gray><i>If holding an axolotl bucket.</i></gray>
                ◼ <secondary><variant><required></secondary> The variant to set for the axolotl in the bucket.
                ◼ <secondary>[-view]<optional></secondary> View the current variant.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie bucket "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("bucket")
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
                            final ItemMeta meta = stack.getItemMeta();

                            if (meta instanceof TropicalFishBucketMeta tropicalFishBucketMeta) {
                                if (tropicalFishBucketMeta.hasVariant()) {
                                    Utils.tell(user, """
                                            <primary>Body Color: {body}
                                            Pattern: <secondary>{pattern}</secondary>
                                            Pattern Color: {color}"""
                                            .replace("{body}", "<c:" + Integer.toHexString(tropicalFishBucketMeta.getBodyColor().getColor().asRGB()) + ">" + tropicalFishBucketMeta.getBodyColor().name().toLowerCase().replace("_", " ") + "</c>")
                                            .replace("{color}", "<c:" + Integer.toHexString(tropicalFishBucketMeta.getPatternColor().getColor().asRGB()) + ">" + tropicalFishBucketMeta.getPatternColor().name().toLowerCase().replace("_", " ") + "</c>")
                                            .replace("{pattern}", tropicalFishBucketMeta.getPattern().name().toLowerCase().replace("_", " ")));
                                } else {
                                    Utils.tell(user, "<primary>Your tropical fish bucket doesn't have a variant!");
                                }
                            } else if (meta instanceof AxolotlBucketMeta axolotlBucketMeta) {
                                if (axolotlBucketMeta.hasVariant()) {
                                    Utils.tell(user, "<primary>Your axolotl's variant is currently <secondary>" + axolotlBucketMeta.getVariant().name().toLowerCase().replace("_", " ") + "</secondary>");
                                } else {
                                    Utils.tell(user, "<primary>Your bucket doesn't have a variant.");
                                }
                            } else {
                                Utils.tell(user, "<red>Your item is not a valid bucket!");
                            }

                            return 1;
                        })
                )
                .then(argument("variant", GenericEnumArgument.generic(Axolotl.Variant.class))
                        .executes(context -> handleAxolotl((Player) context.getSource().getSender(), (meta) -> {
                            final Axolotl.Variant variant = context.getArgument("variant", Axolotl.Variant.class);
                            meta.setVariant(variant);
                            Utils.tell(context.getSource(), "<primary>Set your axolotl variant to <secondary>" + variant.name().toLowerCase().replace("_", " ") + "</secondary>.");
                            return meta;
                        }))
                )
                .then(literal("body")
                        .then(argument("color", GenericEnumArgument.generic(DyeColor.class))
                                .executes(context -> handleTropicalFish((Player) context.getSource().getSender(), (meta) -> {
                                    meta.setBodyColor(context.getArgument("color", DyeColor.class));
                                    return meta;
                                }))
                        )
                )
                .then(literal("pattern")
                        .then(argument("pattern", GenericEnumArgument.generic(TropicalFish.Pattern.class))
                                .then(argument("color", GenericEnumArgument.generic(DyeColor.class))
                                        .executes(context -> handleTropicalFish((Player) context.getSource().getSender(), (meta) -> {
                                            meta.setPattern(context.getArgument("pattern", TropicalFish.Pattern.class));
                                            meta.setPatternColor(context.getArgument("color", DyeColor.class));
                                            return meta;
                                        }))
                                )
                        )
                        .executes(context -> handleTropicalFish((Player) context.getSource().getSender(), (meta) -> {
                            meta.setPattern(context.getArgument("pattern", TropicalFish.Pattern.class));
                            return meta;
                        }))
                )
                ;
    }

    private int handleTropicalFish(final @NotNull Player user, final UnaryOperator<TropicalFishBucketMeta> function) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof TropicalFishBucketMeta meta)) {
            Utils.tell(user, "<red>You are not holding a tropical fish bucket!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));
        return 1;
    }

    private int handleAxolotl(final @NotNull Player user, final UnaryOperator<AxolotlBucketMeta> function) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!(stack.getItemMeta() instanceof AxolotlBucketMeta meta)) {
            Utils.tell(user, "<red>You are not holding an axolotl bucket!");
            return 0;
        }

        stack.setItemMeta(function.apply(meta));
        return 1;
    }
}
