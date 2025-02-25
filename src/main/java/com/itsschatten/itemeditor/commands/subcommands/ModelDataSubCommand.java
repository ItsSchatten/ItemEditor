package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.*;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModelDataSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie modeldata <secondary><data></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the custom model data of the item.
                <gray><i>Pass 'null' to the model.</i></gray>
                \s
                ◼ <secondary><data><required></secondary> The number to assign as the custom model data.
                ◼ <secondary>[-view]<optional></secondary> View the item's current custom model data.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie modeldata "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("modeldata")
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
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            if (meta.hasCustomModelData()) {
                                Utils.tell(user, "<primary>Your item's custom model data is <secondary>" + meta.getCustomModelData() + "</secondary>!");
                            } else {
                                Utils.tell(user, "<primary>Your item doesn't have any custom model data!");
                            }

                            return 1;
                        })
                )
                .then(literal("null")
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
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            meta.setCustomModelDataComponent(null);
                            stack.setItemMeta(meta);
                            Utils.tell(user, "<primary>Cleared your item's custom model data!");
                            return 1;
                        })
                )
                // We opt to use a compound tag (pretty much JSON) because it's easier that way,
                // AND it allows the user to provide MULTIPLE points of data at the same time.
                .then(argument("data", CompoundTagArgument.compoundTag())
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            final ItemMeta meta = stack.getItemMeta();
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            final CompoundTag tag = CompoundTagArgument.getCompoundTag(context, "data");

                            // Old used for comparisons to get a nicer looking information return.
                            final CustomModelDataComponent old = meta.getCustomModelDataComponent();
                            // Data is the actual updated component.
                            final CustomModelDataComponent data = meta.getCustomModelDataComponent();

                            tag.getAllKeys().forEach(key -> {
                                switch (key) {
                                    // Assume a single or a list of floats.
                                    // And don't parse other data.
                                    case "floats" -> {
                                        if (Objects.requireNonNull(tag.get(key)).getId() == Tag.TAG_FLOAT) {
                                            data.setFloats(List.of(((FloatTag) Objects.requireNonNull(tag.get(key))).getAsFloat()));
                                        } else {
                                            data.setFloats(tag.getList(key, Tag.TAG_FLOAT).stream().map(tag1 -> tag1.getId() == Tag.TAG_FLOAT ? ((FloatTag) tag1).getAsFloat() : null).toList());
                                        }
                                    }

                                    case "flags" -> {
                                        // Assume a single or a list of booleans.
                                        // And don't parse other data.
                                        if (Objects.requireNonNull(tag.get(key)).getId() == Tag.TAG_BYTE) {
                                            data.setFlags(List.of(((ByteTag) Objects.requireNonNull(tag.get(key))).getAsByte() == (byte) 1));
                                        } else {
                                            data.setFlags(tag.getList(key, Tag.TAG_BYTE).stream().map(tag1 -> tag1.getId() == Tag.TAG_BYTE ? ((ByteTag) tag1).getAsByte() == (byte) 1 : null).toList());
                                        }
                                    }

                                    case "colors" -> {
                                        try {
                                            final int type = ((ListTag) Objects.requireNonNull(tag.get(key))).getElementType();
                                            final List<Color> colors = new ArrayList<>();

                                            // Assume a single or a list of colors, in any of their forms.
                                            // And don't parse other data.
                                            // The Tag calls here are simply for readability.
                                            // It could be a magic number, if needed.
                                            if (Objects.requireNonNull(tag.get(key)).getId() == Tag.TAG_INT_ARRAY) {
                                                final IntArrayTag rgbTag = (IntArrayTag) Objects.requireNonNull(tag.get(key));
                                                if (rgbTag.isEmpty() || rgbTag.size() < 3) {
                                                    Utils.tell(context, "<red>Failed to build a color from " + rgbTag + ".");
                                                    return;
                                                }

                                                colors.add(Color.fromRGB(rgbTag.getFirst().getAsInt(), rgbTag.get(1).getAsInt(), rgbTag.get(2).getAsInt()));
                                            } else if (Objects.requireNonNull(tag.get(key)).getId() == Tag.TAG_STRING) {
                                                final DyeColor color = DyeColor.valueOf(Objects.requireNonNull(tag.get(key)).getAsString().toUpperCase().replace(" ", "_"));
                                                colors.add(color.getColor());
                                            } else if (Objects.requireNonNull(tag.get(key)).getId() == Tag.TAG_INT) {
                                                colors.add(Color.fromARGB(((IntTag) Objects.requireNonNull(tag.get(key))).getAsInt()));
                                            } else {
                                                tag.getList(key, type).forEach(tag1 -> {
                                                    if (tag1.getId() == Tag.TAG_INT_ARRAY) {
                                                        final IntArrayTag rgbTag = (IntArrayTag) tag1;
                                                        if (rgbTag.isEmpty() || rgbTag.size() < 3) {
                                                            Utils.tell(context, "<red>Failed to build a color from " + rgbTag + ".");
                                                            return;
                                                        }

                                                        colors.add(Color.fromRGB(rgbTag.getFirst().getAsInt(), rgbTag.get(1).getAsInt(), rgbTag.get(2).getAsInt()));
                                                    } else if (tag1.getId() == Tag.TAG_STRING) {
                                                        final DyeColor color = DyeColor.valueOf(tag1.getAsString().toUpperCase().replace(" ", "_"));
                                                        colors.add(color.getColor());
                                                    } else if (tag1.getId() == Tag.TAG_INT) {
                                                        colors.add(Color.fromARGB(((IntTag) tag1).getAsInt()));
                                                    }
                                                });
                                            }

                                            data.setColors(colors);
                                        } catch (ClassCastException ex) {
                                            // We fail gracefully so we can continue parsing other data.
                                            // We do, however, send a message to the player to say we failed
                                            // to parse a color.
                                            Utils.tell(context, "<red>Your 'colors' field is not a LIST of integers, strings, or list of integers!");
                                        } catch (IllegalArgumentException ex) {
                                            Utils.tell(context, "<red>Couldn't find a color by the provided name!");
                                        }
                                    }

                                    case "strings" -> {
                                        // Assume a single or a list of strings.
                                        // And don't parse other data.
                                        if (Objects.requireNonNull(tag.get(key)).getId() == Tag.TAG_STRING) {
                                            data.setStrings(List.of(Objects.requireNonNull(tag.get(key)).getAsString()));
                                        } else {
                                            data.setStrings(tag.getList(key, Tag.TAG_STRING).stream().map(tag1 -> tag1.getId() == Tag.TAG_STRING ? tag1.getAsString() : null).toList());
                                        }
                                    }

                                    default -> Utils.tell(context, "<dark_gray><i>Skipping unknown key: " + key);
                                }
                            });

                            meta.setCustomModelDataComponent(data);
                            stack.setItemMeta(meta);

                            Utils.tell(user, "<primary>Updated your item's custom model data to <secondary>" + compareToString(old, data) + "</secondary>!");
                            return 1;
                        })
                );
    }

    private String compareToString(final @NotNull CustomModelDataComponent old, final @NotNull CustomModelDataComponent updated) {
        final StringBuilder sb = new StringBuilder();

        if (!updated.getColors().isEmpty() && updated.getColors() != old.getColors()) {
            final StringBuilder colors = new StringBuilder();
            updated.getColors().forEach(color -> colors.append("<hover:show_text:'<").append(TextColor.color(color.asRGB())).append(">Looks like this!'").append(">").append(color.asRGB()).append("</hover><gray>,</gray>"));
            sb.append("colors=[").append(colors, 0, colors.lastIndexOf("<gray>,</gray>")).append("]<gray>,</gray> ");
        }

        if (!updated.getFlags().isEmpty() && updated.getFlags() != old.getFlags()) {
            final StringBuilder flags = new StringBuilder();
            updated.getFlags().forEach(flag -> flags.append(flag).append("<gray>,</gray>"));

            sb.append("flags=[").append(flags, 0, flags.lastIndexOf("<gray>,</gray>")).append("]<gray>,</gray> ");
        }

        if (!updated.getFloats().isEmpty() && updated.getFloats() != old.getFloats()) {
            final StringBuilder floats = new StringBuilder();
            updated.getFloats().forEach(aFloat -> floats.append(aFloat).append("f<gray>,</gray>"));

            sb.append("floats=[").append(floats, 0, floats.lastIndexOf("<gray>,</gray>")).append("]<gray>,</gray> ");
        }

        if (!updated.getStrings().isEmpty() && updated.getStrings() != old.getStrings()) {
            final StringBuilder strings = new StringBuilder();
            updated.getStrings().forEach(string -> strings.append("\"").append(string).append("\"<gray>,</gray>"));

            sb.append("strings=[").append(strings, 0, strings.lastIndexOf("<gray>,</gray>")).append("]");
        }

        if (sb.toString().endsWith("<gray>,</gray>") || sb.toString().endsWith("<gray>,</gray> ")) {
            return sb.substring(0, sb.lastIndexOf("<gray>,</gray>"));
        } else {
            return sb.toString();
        }
    }

}
