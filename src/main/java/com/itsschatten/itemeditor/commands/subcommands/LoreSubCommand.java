package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.WrapUtils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.commands.SharedSuggestionProvider;
import org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public final class LoreSubCommand extends BrigadierCommand {

    /**
     * The clipboard map, used to store a list of components keyed to a player's UUID.
     */
    private final Map<UUID, List<Component>> clipboard;

    public LoreSubCommand() {
        this.clipboard = new HashMap<>();
    }

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie<secondary><add|set|remove|...> [values]</secondary>").hoverEvent(StringUtil.color("""
                <primary>Manipulates lore on the item.
                \s
                ◼ <secondary><add><first> <lore><required></secondary> Adds a new lore line.
                ◼ <secondary><insert><first> <line #><required> <lore><required></secondary> Insert a lore line at the provided number.
                ◼ <secondary><set><first> <line #><required> <lore><required></secondary> Set a specific lore line.
                ◼ <secondary><remove><first> <line #><required></secondary> Remove a lore line.
                ◼ <secondary>\\<reset><first></secondary> Resets the lore on the item.
                ◼ <secondary><replace><first> <replace><required> <replacement><required></secondary> Replace all occurrences of a string.
                ◼ <secondary><wrap><first> <line #><required></secondary> The line number to wrap.
                ◼ <secondary><paste><first> [-view|-insert|-add]<optional></secondary> Pastes the lore on the item.
                ◼ <secondary><copy><first> [file_name|-override|-wrap]<optional></secondary> Copy the lore of one item into your clipboard.
                  '<secondary>-override</secondary>' whilst copying a book will use the books lore instead of it's pages.
                ◼ <secondary><copybook><first> [-wrap]<optional></secondary> Copy the contents of a book to your clipboard.
                ◼ <secondary><copyfile><first> <name><required> [-wrap]<optional></secondary> Copy the contents of a file to your clipboard.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("lore")
                .then(literal("add")
                        .executes(context -> updateLore(context, lore -> {
                            lore.add(Component.empty());
                            Utils.tell(context.getSource(), "<primary>Added <reset>'' <primary>to your item's lore.");
                            return lore;
                        }))
                        .then(literal("-wrap")
                                .then(argument("lore", StringArgumentType.greedyString())
                                        .executes(context -> updateLore(context, lore -> {
                                            final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                            lore.addAll(WrapUtils.convertStringToComponentList("<!i>" + toAdd));
                                            Utils.tell(context.getSource(), "<primary>Added <reset>'<dark_purple>" + toAdd + "<reset>' <gray>(wrapped)</gray> <primary>to your item's lore.");
                                            return lore;
                                        }))
                                )
                        )
                        .then(argument("lore", StringArgumentType.greedyString())
                                .executes(context -> updateLore(context, lore -> {
                                    final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                    lore.add(StringUtil.color("<!i>" + toAdd));
                                    Utils.tell(context.getSource(), "<primary>Added <reset>'<dark_purple>" + toAdd + "<reset>' <primary>to your item's lore.");
                                    return lore;
                                }))
                        )
                )

                .then(literal("insert")
                        .then(literal("last")
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = lore.size() - 1;

                                    // Wrap the string from the third argument.
                                    lore.add(location, Component.empty());

                                    Utils.tell(context.getSource(), "<primary>Inserted <reset>'' <primary>to position <secondary>#" + (location + 1) + "</secondary> in your item's lore.");
                                    return lore;
                                }))
                                .then(literal("-wrap")
                                        .then(argument("lore", StringArgumentType.greedyString())
                                                .executes(context -> updateLore(context, lore -> {
                                                    final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                                    final int location = lore.size() - 1;

                                                    // Wrap the string from the third argument.
                                                    lore.addAll(location, WrapUtils.convertStringToComponentList("<!i>" + toAdd));

                                                    Utils.tell(context.getSource(), "<primary>Inserted <reset>'<dark_purple>" + toAdd + "<reset>' <gray>(wrapped)</gray> <primary>to position <secondary>#" + (location + 1) + "</secondary> in your item's lore.");
                                                    return lore;
                                                }))
                                        )
                                )
                                .then(argument("lore", StringArgumentType.greedyString())
                                        .executes(context -> updateLore(context, lore -> {
                                            final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                            final int location = lore.size() - 1;

                                            // Add the lore line.
                                            lore.add(location, StringUtil.color("<!i>" + toAdd));

                                            Utils.tell(context.getSource(), "<primary>Inserted <reset>'<dark_purple>" + toAdd + "<reset>'<primary> to position <secondary>#" + (location + 1) + "</secondary> in your item's lore.");
                                            return lore;
                                        }))
                                )
                        )
                        .then(argument("location", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                    if (meta == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(getLoreLines(meta), builder);
                                })
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                    // Wrap the string from the third argument.
                                    if (location == 0 && lore.isEmpty()) {
                                        lore.add(Component.empty());
                                    } else {
                                        lore.add(location, Component.empty());
                                    }

                                    Utils.tell(context.getSource(), "<primary>Inserted <reset>'' <primary>to position <secondary>#" + (location + 1) + "</secondary> in your item's lore.");
                                    return lore;
                                }))
                                .then(literal("-wrap")
                                        .then(argument("lore", StringArgumentType.greedyString())
                                                .executes(context -> updateLore(context, lore -> {
                                                    final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                                    final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                                    // Wrap the string from the third argument.
                                                    if (location == 0 && lore.isEmpty()) {
                                                        lore.addAll(WrapUtils.convertStringToComponentList("<!i>" + toAdd));
                                                    } else {
                                                        lore.addAll(location, WrapUtils.convertStringToComponentList("<!i>" + toAdd));
                                                    }

                                                    Utils.tell(context.getSource(), "<primary>Inserted <reset>'<dark_purple>" + toAdd + "<reset>' <gray>(wrapped)</gray> <primary>to position <secondary>#" + (location + 1) + "</secondary> in your item's lore.");
                                                    return lore;
                                                }))
                                        )
                                )
                                .then(argument("lore", StringArgumentType.greedyString())
                                        .executes(context -> updateLore(context, lore -> {
                                            final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                            final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                            // Add the lore line.
                                            if (location == 0 && lore.isEmpty()) {
                                                lore.add(StringUtil.color("<!i>" + toAdd));
                                            } else {
                                                lore.add(location, StringUtil.color("<!i>" + toAdd));
                                            }

                                            Utils.tell(context.getSource(), "<primary>Inserted <reset>'<dark_purple>" + toAdd + "<reset>'<primary> to position <secondary>#" + (location + 1) + "</secondary> in your item's lore.");
                                            return lore;
                                        }))
                                )
                        )
                )

                .then(literal("set")
                        .then(literal("last")
                                .executes(context -> updateLore(context, lore -> {
                                    if (lore.isEmpty()) {
                                        lore.add(Component.empty());
                                        Utils.tell(context.getSource(), "<primary>Set position <secondary>#1</secondary> in your item's lore to <reset>''<primary>.");
                                    } else {
                                        final int location = lore.size() - 1;

                                        // Set the lines.
                                        lore.set(location, Component.empty());
                                        Utils.tell(context.getSource(), "<primary>Set position <secondary>#" + (location + 1) + "</secondary> in your item's lore to <reset>''<primary>.");
                                    }

                                    return lore;
                                }))
                                .then(argument("lore", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            final Player user = (Player) context.getSource().getSender();
                                            // Get the item stack in the user's main hand.
                                            final ItemStack stack = user.getInventory().getItemInMainHand();
                                            if (ItemValidator.isInvalid(stack)) {
                                                return builder.buildFuture();
                                            }

                                            final ItemMeta meta = stack.getItemMeta();
                                            // Make sure we have ItemMeta, it shouldn't ever be null.
                                            // But still better to be safe than sorry.
                                            if (meta == null) {
                                                return builder.buildFuture();
                                            }

                                            return !meta.hasLore() ? builder.buildFuture() :
                                                    SharedSuggestionProvider.suggest(
                                                            List.of(MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.lore()).getLast())), builder);
                                        })
                                        .executes(context -> updateLore(context, lore -> {
                                            final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");

                                            if (lore.isEmpty()) {
                                                lore.add(StringUtil.color("<!i>" + toAdd));
                                                Utils.tell(context.getSource(), "<primary>Set position <secondary>#0</secondary> in your item's lore to <reset>'<dark_purple>" + toAdd + "<reset>'<primary>.");
                                            } else {
                                                final int location = lore.size() - 1;

                                                // Set the lines.
                                                lore.set(location, StringUtil.color("<!i>" + toAdd));
                                                Utils.tell(context.getSource(), "<primary>Set position <secondary>#" + (location + 1) + "</secondary> in your item's lore to <reset>'<dark_purple>" + toAdd + "<reset>'<primary>.");
                                            }

                                            return lore;
                                        }))
                                )
                        )
                        .then(argument("location", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                    if (meta == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(getLoreLines(meta), builder);
                                })
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                    // Set the lines.
                                    if (location == 0 && lore.isEmpty()) {
                                        lore.add(Component.empty());
                                    } else {
                                        if (location > lore.size()) {
                                            lore.add(Component.empty());
                                        } else {
                                            lore.set(location, Component.empty());
                                        }
                                    }
                                    return lore;
                                }))
                                .then(argument("lore", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            final Player user = (Player) context.getSource().getSender();
                                            // Get the item stack in the user's main hand.
                                            final ItemStack stack = user.getInventory().getItemInMainHand();
                                            if (ItemValidator.isInvalid(stack)) {
                                                return builder.buildFuture();
                                            }

                                            final ItemMeta meta = stack.getItemMeta();
                                            // Make sure we have ItemMeta, it shouldn't ever be null.
                                            // But still better to be safe than sorry.
                                            if (meta == null) {
                                                return builder.buildFuture();
                                            }

                                            try {
                                                return !meta.hasLore() ? builder.buildFuture() :
                                                        SharedSuggestionProvider.suggest(List.of(MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.lore()).get(IntegerArgumentType.getInteger(context.getLastChild(), "location") - 1))), builder);

                                            } catch (IndexOutOfBoundsException e) {
                                                return builder.buildFuture();
                                            }
                                        })
                                        .executes(context -> updateLore(context, lore -> {
                                            final String toAdd = StringArgumentType.getString(context, "lore").replace("\\s", " ");
                                            final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                            // Set the lines.
                                            if (location == 0 && lore.isEmpty()) {
                                                lore.add(StringUtil.color("<!i>" + toAdd));
                                            } else {
                                                if (location > lore.size()) {
                                                    lore.add(StringUtil.color("<!i>" + toAdd));
                                                } else {
                                                    lore.set(location, StringUtil.color("<!i>" + toAdd));
                                                }
                                            }

                                            Utils.tell(context.getSource(), "<primary>Set position <secondary>#" + (location + 1) + "</secondary> in your item's lore to <reset>'<dark_purple>" + toAdd + "<reset>'<primary>.");
                                            return lore;
                                        }))
                                )
                        )
                )

                .then(literal("remove")
                        .then(literal("last")
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = lore.size() - 1;

                                    // remove the lore line.
                                    final String removed = MiniMessage.miniMessage().serialize(lore.remove(location));
                                    Utils.tell(context.getSource(), "<primary>Removed line <secondary><hover:show_text:'" + removed.replace("'", "\\'") + "'>#" + (location + 1) + "</hover></secondary> from the item's lore.\n<gray><i>Tip: Hover over the number to see the lore line you removed!</i></gray>");
                                    return lore;
                                }))
                        )
                        .then(argument("location", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                    if (meta == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(getLoreLines(meta), builder);
                                })
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                    // remove the lore line.
                                    final String removed = MiniMessage.miniMessage().serialize(lore.remove(location));
                                    Utils.tell(context.getSource(), "<primary>Removed line <secondary><hover:show_text:'" + removed.replace("'", "\\'") + "'>#" + (location + 1) + "</hover></secondary> from the item's lore.\n<gray><i>Tip: Hover over the number to see the lore line you removed!</i></gray>");
                                    return lore;
                                }))
                        )
                )

                .then(literal("reset")
                        .executes(context -> updateLore(context, lore -> {
                            lore.clear();
                            Utils.tell(context.getSource(), "<primary>Reset your item's lore to nothing.");
                            return lore;
                        }))
                )

                .then(literal("wrap")
                        .then(literal("last")
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = lore.size() - 1;

                                    // remove the lore line.
                                    final String removed = MiniMessage.miniMessage().serialize(lore.remove(location));
                                    lore.addAll(location, WrapUtils.convertStringToComponentList("<!i>" + removed));
                                    Utils.tell(context.getSource(), "<primary>Wrapped line <secondary>#" + (location + 1) + "</secondary>.");
                                    return lore;
                                }))
                        )
                        .then(argument("location", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                    if (meta == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(getLoreLines(meta), builder);
                                })
                                .executes(context -> updateLore(context, lore -> {
                                    final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                    // remove the lore line.
                                    final String removed = MiniMessage.miniMessage().serialize(lore.remove(location));
                                    lore.addAll(location, WrapUtils.convertStringToComponentList("<!i>" + removed));
                                    Utils.tell(context.getSource(), "<primary>Wrapped line <secondary>#" + (location + 1) + "</secondary>.");
                                    return lore;
                                }))
                        )
                )

                .then(literal("replace")
                        .then(argument("target", StringArgumentType.string())
                                .then(argument("replacement", StringArgumentType.greedyString())
                                        .executes(context -> updateLore(context, lore -> {
                                            // What we are replacing.
                                            final String replace = StringArgumentType.getString(context, "target");
                                            // What to replace it with.
                                            final String replacement = StringArgumentType.getString(context, "replacement");

                                            // Replace all the lore.
                                            lore.replaceAll(component -> component.replaceText(TextReplacementConfig.builder().matchLiteral(replace).replacement(replacement).build()));
                                            Utils.tell(context.getSource(), "<primary>Replaced any iteration of <secondary>" + replace + "</secondary> with <reset>'" + replacement + "<reset>'<primary>.");
                                            return lore;
                                        }))
                                )
                        )
                )

                .then(literal("paste")
                        .executes(context -> updateLore(context, lore -> {
                            final UUID uuid = context.getSource().getSender().get(Identity.UUID).orElse(null);
                            if (uuid == null || (clipboard.get(uuid) != null && clipboard.get(uuid).isEmpty())) {
                                Utils.tell(context.getSource(), "<red>You don't have any lore copied to your clipboard!");
                                return lore;
                            }

                            lore.clear();
                            lore.addAll(clipboard.get(uuid));
                            Utils.tell(context.getSource(), "<primary>Pasted your clipboard to your item's lore!");
                            return lore;
                        }))
                        .then(literal("-view")
                                .executes(this::viewClipboard)
                        )
                        .then(literal("-add")
                                .executes(context -> updateLore(context, lore -> {
                                    final UUID uuid = context.getSource().getSender().get(Identity.UUID).orElse(null);

                                    if (uuid == null || (clipboard.get(uuid) != null || clipboard.get(uuid).isEmpty())) {
                                        Utils.tell(context.getSource(), "<red>You don't have any lore copied to your clipboard!");
                                        return lore;
                                    }

                                    lore.addAll(clipboard.get(uuid));
                                    Utils.tell(context.getSource(), "<primary>Added your copied lore to the end of your item's lore.");
                                    return lore;
                                }))
                        )
                        .then(literal("-insert")
                                .then(argument("location", IntegerArgumentType.integer(1))
                                        .suggests((context, builder) -> {
                                            final Player user = (Player) context.getSource().getSender();
                                            // Get the item stack in the user's main hand.
                                            final ItemStack stack = user.getInventory().getItemInMainHand();
                                            if (ItemValidator.isInvalid(stack)) {
                                                return builder.buildFuture();
                                            }

                                            final ItemMeta meta = stack.getItemMeta();
                                            // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                            if (meta == null) {
                                                return builder.buildFuture();
                                            }

                                            return SharedSuggestionProvider.suggest(getLoreLines(meta), builder);
                                        })
                                        .executes(context -> updateLore(context, lore -> {
                                            final UUID uuid = context.getSource().getSender().get(Identity.UUID).orElse(null);
                                            if (uuid == null || (clipboard.get(uuid) != null || clipboard.get(uuid).isEmpty())) {
                                                Utils.tell(context.getSource(), "<red>You don't have any lore copied to your clipboard!");
                                                return lore;
                                            }

                                            final int location = IntegerArgumentType.getInteger(context, "location") - 1;

                                            // Add the lore line.
                                            if (location == 0 && lore.isEmpty()) {
                                                lore.addAll(clipboard.get(uuid));
                                                Utils.tell(context.getSource(), "<primary>Pasted your clipboard to your item's lore!");
                                            } else {
                                                lore.addAll(location, clipboard.get(uuid));
                                                Utils.tell(context.getSource(), "<primary>Inserted your item's lore to position <secondary>#" + (location + 1) + "</secondary> to your copied lore.");
                                            }

                                            return lore;
                                        }))
                                )
                                .then(literal("last")
                                        .executes(context -> updateLore(context, lore -> {
                                            final UUID uuid = context.getSource().getSender().get(Identity.UUID).orElse(null);
                                            if (uuid == null || (clipboard.get(uuid) != null || clipboard.get(uuid).isEmpty())) {
                                                Utils.tell(context.getSource(), "<red>You don't have any lore copied to your clipboard!");
                                                return lore;
                                            }

                                            // Wrap the string from the third argument.
                                            if (lore.isEmpty()) {
                                                lore.addAll(clipboard.get(uuid));
                                                Utils.tell(context.getSource(), "<primary>Pasted your clipboard to your item's lore!");
                                            } else {
                                                final int location = lore.size() - 1;
                                                lore.addAll(location, clipboard.get(uuid));
                                                Utils.tell(context.getSource(), "<primary>Inserted your item's lore to position <secondary>#" + (location + 1) + "</secondary> to your copied lore.");
                                            }

                                            return lore;
                                        }))
                                )
                        )
                )
                .then(literal("copy")
                        .then(argument("file", StringArgumentType.string())
                                .then(literal("-wrap")
                                        .executes(context -> {
                                            final String path = StringArgumentType.getString(context, "file");

                                            // Get a file stored within the plugins/ItemEditor directory.
                                            final File file = new File(Utils.getInstance().getDataFolder(), path);
                                            if (!file.exists()) {
                                                Utils.tell(context.getSource(), "<red>Couldn't find a file with the path <yellow>" + path + "</yellow>!");
                                                return 0;
                                            }

                                            // Copy the file's contents to the user's clipboard.
                                            copyFileText((Player) context.getSource().getSender(), file, true);
                                            return 1;
                                        })
                                )
                                .executes(context -> {
                                    final String path = StringArgumentType.getString(context, "file");

                                    // Get a file stored within the plugins/ItemEditor directory.
                                    final File file = new File(Utils.getInstance().getDataFolder(), path);
                                    if (!file.exists()) {
                                        Utils.tell(context.getSource(), "<red>Couldn't find a file with the path <yellow>" + path + "</yellow>!");
                                        return 0;
                                    }

                                    // Copy the file's contents to the user's clipboard.
                                    copyFileText((Player) context.getSource().getSender(), file);
                                    return 1;
                                })
                        )
                        .then(literal("-override")
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                                    if (meta == null) {
                                        Utils.tell(user, "<red>For some reason the item's meta is null!");
                                        return 0;
                                    }

                                    clipboard.put(user.getUniqueId(), meta.lore());
                                    Utils.tell(user, """
                                            <primary>Copied your item's lore to your clipboard!
                                            To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                                            To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
                                    return 1;
                                })
                        )
                        .then(literal("-wrap")
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    final ItemMeta meta = stack.getItemMeta();
                                    // Make sure we have ItemMeta, it shouldn't ever be null.
                                    // But still better to be safe than sorry.
                                    if (meta == null) {
                                        Utils.tell(user, "<red>For some reason the item's meta is null!");
                                        return 0;
                                    }

                                    // Handle copying book pages to the player's clipboard.
                                    if (meta instanceof final BookMeta bookMeta) {
                                        // Place all the pages in the clipboard.
                                        copyBookPages(user, bookMeta, true);
                                        return 1; // We've copied all we wanted.
                                    }

                                    clipboard.put(user.getUniqueId(), meta.lore());
                                    Utils.tell(user, """
                                            <primary>Copied your item's lore to your clipboard!
                                            To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                                            To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            final ItemMeta meta = stack.getItemMeta();
                            // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
                            if (meta == null) {
                                Utils.tell(user, "<red>For some reason the item's meta is null!");
                                return 0;
                            }

                            // Handle copying book pages to the player's clipboard.
                            if (meta instanceof final BookMeta bookMeta) {
                                // Place all the pages in the clipboard.
                                copyBookPages(user, bookMeta);
                                return 1; // We've copied all we wanted.
                            }

                            clipboard.put(user.getUniqueId(), meta.lore());
                            Utils.tell(user, """
                                    <primary>Copied your item's lore to your clipboard!
                                    To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                                    To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
                            return 1;
                        })
                )
                .then(literal("copybook")
                        .then(literal("-wrap")
                                .executes(context -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                        return 0;
                                    }

                                    // Handle copying book pages to the player's clipboard.
                                    if (stack.getItemMeta() instanceof final BookMeta bookMeta) {
                                        // Place all the pages in the clipboard.
                                        copyBookPages(user, bookMeta, true);
                                        return 1; // We've copied all we wanted.
                                    }

                                    Utils.tell(user, "<yellow>" + stack.getType().getKey().getKey() + "</yellow><red> is not a book that contains pages!");
                                    return 0;
                                })
                        )
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            // Handle copying book pages to the player's clipboard.
                            if (stack.getItemMeta() instanceof final BookMeta bookMeta) {
                                // Place all the pages in the clipboard.
                                copyBookPages(user, bookMeta);
                                return 1; // We've copied all we wanted.
                            }

                            Utils.tell(user, "<yellow>" + stack.getType().getKey().getKey() + "</yellow><red> is not a book that contains pages!");
                            return 0;
                        })
                )
                .then(literal("copyfile")
                        .then(argument("file", StringArgumentType.string())
                                .then(literal("-wrap")
                                        .executes(context -> {
                                            final String path = StringArgumentType.getString(context, "file");

                                            // Get a file stored within the plugins/ItemEditor directory.
                                            final File file = new File(Utils.getInstance().getDataFolder(), path);
                                            if (!file.exists()) {
                                                Utils.tell(context.getSource(), "<red>Couldn't find a file with the path <yellow>" + path + "</yellow>!");
                                                return 0;
                                            }

                                            // Copy the file's contents to the user's clipboard.
                                            copyFileText((Player) context.getSource().getSender(), file, true);
                                            return 1;
                                        })
                                )
                                .executes(context -> {
                                    final String path = StringArgumentType.getString(context, "file");

                                    // Get a file stored within the plugins/ItemEditor directory.
                                    final File file = new File(Utils.getInstance().getDataFolder(), path);
                                    if (!file.exists()) {
                                        Utils.tell(context.getSource(), "<red>Couldn't find a file with the path <yellow>" + path + "</yellow>!");
                                        return 0;
                                    }

                                    // Copy the file's contents to the user's clipboard.
                                    copyFileText((Player) context.getSource().getSender(), file);
                                    return 1;
                                })
                        )
                );
    }

    private int viewClipboard(@NotNull CommandContext<CommandSourceStack> context) {
        Utils.tell(context.getSource(), StringUtil.color("<gradient:#D8D8F6:#978897>Your clipboard contents:</gradient>"));

        final UUID uuid = context.getSource().getSender().get(Identity.UUID).orElse(null);
        if (uuid == null || (clipboard.get(uuid) != null || clipboard.get(uuid).isEmpty())) {
            Utils.tell(context.getSource(), "<red>You don't have any lore copied.");
            return 0;
        }

        clipboard.get(uuid).forEach(message -> Utils.tell(context.getSource(), "<primary>◼</primary> " + MiniMessage.miniMessage().serialize(message)));
        return 1;
    }

    private int updateLore(final @NotNull CommandContext<CommandSourceStack> context, final UnaryOperator<List<Component>> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        final ItemMeta meta = stack.getItemMeta();
        // Make sure we have ItemMeta, it shouldn't ever be null. But still better to be safe than sorry.
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        meta.lore(function.apply(meta.hasLore() ? meta.lore() : new ArrayList<>()));
        stack.setItemMeta(meta);
        return 1;
    }

    /**
     * Utility method that copies a files contents to the user's clipboard.
     *
     * @param user The user to copy the file contents for.
     * @param file The file of who's contents to copy.
     */
    private void copyFileText(final Player user, final File file) {
        copyFileText(user, file, false);
    }

    /**
     * Utility method that copies a files contents to the user's clipboard.
     *
     * @param user The user to copy the file contents for.
     * @param file The file of who's contents to copy.
     * @param wrap If the lines should be wrapped.
     */
    private void copyFileText(final Player user, final File file, boolean wrap) {
        final List<Component> fileLore = new ArrayList<>();
        try {
            // Get the contents of the file and set it to UTF_8, then "read" each line and add it to the fileLore.
            final List<String> contents = FileUtils.readLines(file, StandardCharsets.UTF_8);
            contents.forEach((line) -> {
                if (wrap) {
                    final List<String> wrapped = WrapUtils.convertStringToList(line);
                    fileLore.addAll(wrapped.stream().map(string -> StringUtil.color("<!i>" + string)).toList());
                } else {
                    fileLore.add(StringUtil.color("<!i>" + line));
                }
            });
        } catch (IOException e) {
            Utils.sendDeveloperErrorMessage(user, e);
            Utils.logError(e);
            Utils.tell(user, "<red>An error occurred whilst reading the file's data, see the console for more information.");
        }

        // Set the file lore to our clipboard.
        clipboard.put(user.getUniqueId(), fileLore);
        Utils.tell(user, """
                <primary>Copied the contents of the provided file to your clipboard.
                To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
        if (wrap) {
            Utils.tell(user, "<gray><i>The lines have been wrapped if applicable!");
        }
    }

    /**
     * Copy a written or writable book's pages to the user's clipboard.
     *
     * @param user     The user to copy the pages for.
     * @param bookMeta The meat of the book to copy.
     * @param wrap     If the text should be wrapped.
     */
    private void copyBookPages(@NotNull Player user, @NotNull BookMeta bookMeta, boolean wrap) {
        final List<Component> bookPages = new ArrayList<>();
        bookMeta.pages().forEach((page) ->
                // Split on all new lines.
                Arrays.stream(PlainTextComponentSerializer.plainText().serialize(page).split("\n"))
                        // Then add the strings to the lore.
                        .forEach((string) -> {
                            if (wrap) {
                                final List<String> wrapped = WrapUtils.convertStringToList(string);
                                bookPages.addAll(wrapped.stream().map(line -> StringUtil.color("<!i>" + line)).toList());
                            } else {
                                bookPages.add(StringUtil.color("<!i>" + string));
                            }
                        }));

        clipboard.put(user.getUniqueId(), bookPages);
        Utils.tell(user, """
                <primary>Copied the book's pages to your clipboard!
                To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
        if (wrap) {
            Utils.tell(user, "<gray><i>The lines have been wrapped if applicable!");
        }
    }

    /**
     * Copy a written or writable book's pages to the user's clipboard.
     *
     * @param user     The user to copy the pages for.
     * @param bookMeta The meat of the book to copy.
     */
    private void copyBookPages(@NotNull Player user, @NotNull BookMeta bookMeta) {
        copyBookPages(user, bookMeta, false);
    }

    // Gets the lore lines off an item.
    private @NotNull List<String> getLoreLines(ItemMeta meta) {
        return new ArrayList<>(meta != null && meta.hasLore() ? IntStream.range(1, Objects.requireNonNull(meta.lore()).size() + 1).mapToObj(Integer::toString).toList() : List.of());
    }
}
