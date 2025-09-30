package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.commands.arguments.GenericEnumArgument;
import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class BookSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie book <secondary><enchant|author|title|gen.> ...</secondary>").hoverEvent(StringUtil.color("""
                <primary>Alter written book title, author, and generation or the stored enchantments.
                \s
                <info>While holding a book or enchanted book.</info>
                ◼ <secondary><enchantment><required> [level]<optional></secondary> Store an enchantment on a book or an enchanted book.
                <info>While holding a written.</info>
                ◼ <secondary><author><required> <author string|-clear><required></secondary> Set the author of the written book.
                ◼ <secondary><title><required> <title string|-clear><required></secondary> Set the title of the written book.
                ◼ <secondary><generation><required> <generation|-clear><required></secondary> Set the generation of the written book.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie book"));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("book")
                .then(literal("author")
                        .then(literal("-clear")
                                .executes(context -> handleBookUpdate((Player) context.getSource().getSender(), (meta) -> {
                                            meta.setAuthor(null);
                                            Utils.tell(context.getSource(), "<primary>Cleared the author from your book.");
                                            return meta;
                                        })
                                )
                        )
                        .then(argument("author", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    // Make sure we have ItemMeta, it shouldn't ever be null.
                                    // But still better to be safe than sorry.
                                    // Also, it should be a book.
                                    if (!(stack.getItemMeta() instanceof BookMeta bookMeta)) {
                                        return builder.buildFuture();
                                    }

                                    if (bookMeta.author() == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(List.of(MiniMessage.miniMessage().serialize(Objects.requireNonNull(bookMeta.author()))), builder);
                                })
                                .executes(context -> handleBookUpdate((Player) context.getSource().getSender(), (meta) -> {
                                    final String author = StringArgumentType.getString(context, "author");
                                    meta.author(StringUtil.color(author));
                                    Utils.tell(context.getSource(), "<primary>Set the author of your book to <reset>'" + author + "<reset>'<primary>.");
                                    return meta;
                                }))
                        )
                )
                .then(literal("title")
                        .then(literal("-clear")
                                .executes(context -> handleBookUpdate((Player) context.getSource().getSender(), (meta) -> {
                                            meta.setTitle(null);
                                            Utils.tell(context.getSource(), "<primary>Cleared the title from your book.");
                                            return meta;
                                        })
                                )
                        )
                        .then(argument("title", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    final Player user = (Player) context.getSource().getSender();
                                    // Get the item stack in the user's main hand.
                                    final ItemStack stack = user.getInventory().getItemInMainHand();
                                    if (ItemValidator.isInvalid(stack)) {
                                        return builder.buildFuture();
                                    }

                                    // Make sure we have ItemMeta, it shouldn't ever be null.
                                    // But still better to be safe than sorry.
                                    // Also, it should be a book.
                                    if (!(stack.getItemMeta() instanceof BookMeta bookMeta)) {
                                        return builder.buildFuture();
                                    }

                                    if (bookMeta.title() == null) {
                                        return builder.buildFuture();
                                    }

                                    return SharedSuggestionProvider.suggest(List.of(MiniMessage.miniMessage().serialize(Objects.requireNonNull(bookMeta.title()))), builder);
                                })
                                .executes(context -> handleBookUpdate((Player) context.getSource().getSender(), (meta) -> {
                                    String title = StringArgumentType.getString(context, "title");
                                    if (title.length() > 32) {
                                        title = title.substring(0, 32);
                                        Utils.tell(context.getSource(), "<gray>Your title has been limited to 32 characters.");
                                    }

                                    meta.title(StringUtil.color(title));
                                    Utils.tell(context.getSource(), "<primary>Set the title of your book to <reset>'" + title + "<reset>'<primary>.");

                                    return meta;
                                }))
                        )
                )
                .then(literal("generation")
                        .then(literal("-clear")
                                .executes(context -> handleBookUpdate((Player) context.getSource().getSender(), (meta) -> {
                                            meta.setGeneration(null);
                                            Utils.tell(context.getSource(), "<primary>Removed the generation from your book.");
                                            return meta;
                                        })
                                )
                        )
                        .then(argument("generation", GenericEnumArgument.generic(BookMeta.Generation.class))
                                .executes(context -> handleBookUpdate((Player) context.getSource().getSender(), (meta) -> {
                                    final BookMeta.Generation generation = context.getArgument("generation", BookMeta.Generation.class);
                                    meta.setGeneration(generation);
                                    Utils.tell(context.getSource(), "<primary>Set the generation of your book to <secondary>" + generation.name().toLowerCase().replace("_", " ") + "</secondary>.");
                                    return meta;
                                }))
                        )
                )
                .then(literal("-view")
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();
                            final ItemStack stack = user.getInventory().getItemInMainHand();
                            if (ItemValidator.isInvalid(stack)) {
                                Utils.tell(user, "<red>You need to be holding an item in your hand.");
                                return 0;
                            }

                            if (stack.getType() != Material.BOOK && stack.getType() != Material.ENCHANTED_BOOK) {
                                Utils.tell(user, "<red>You must be holding a book or enchanted book to store an enchantment on it.");
                                return 0;
                            }

                            ItemStack finalStack = stack;

                            if (stack.getType() == Material.BOOK) {
                                finalStack = stack.withType(Material.ENCHANTED_BOOK);
                            }

                            if (!(finalStack.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
                                Utils.tell(user, "<red>You are not holding a book that can store enchantments.");
                                return 0;
                            }

                            if (!meta.hasStoredEnchants()) {
                                Utils.tell(user, "<primary>Your item is not storing enchantments.");
                                return 1;
                            }

                            Utils.tell(user, "<primary>Your item is currently storing the following enchantments: <#D8D8F6>" + String.join("<gray>,</gray> ", meta.getStoredEnchants().entrySet().stream()
                                    .map((entry) -> "<click:suggest_command:'/ie book " + entry.getKey().getKey().getKey() + " 0'><hover:show_text:'<gray><i>Click to suggest the command to remove this enchantment!'>" + entry.getKey().getKey().getKey().replace("_", " ") + " " + entry.getValue() + "</hover></click>").toList()));
                            Utils.tell(user, "<gray><i>Click an enchantment above to suggest the command to remove it!");
                            return 1;
                        })
                )
                .then(argument("enchantment", ArgumentTypes.resource(RegistryKey.ENCHANTMENT))
                        .then(argument("level", IntegerArgumentType.integer(0))
                                .executes(context -> handleEnchantmentStore((Player) context.getSource().getSender(), (meta) -> {
                                    final Enchantment enchantment = context.getArgument("enchantment", Enchantment.class);
                                    if (enchantment == null) {
                                        Utils.tell(context.getSource(), "<red>Failed to find the enchantment!");
                                        return null;
                                    }

                                    final int level = IntegerArgumentType.getInteger(context, "level");

                                    if (level <= 0) {
                                        // Remove our enchantment.
                                        meta.removeStoredEnchant(enchantment);
                                        Utils.tell(context.getSource(), "<primary>Removed the stored enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> from your item.");
                                    } else {
                                        // Add our enchantment with the provided level.
                                        meta.addStoredEnchant(enchantment, level, true);
                                        Utils.tell(context.getSource(), "<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> at level <secondary>" + level + "</secondary> to your item enchantment storage.");
                                    }

                                    return meta;
                                }))
                        )
                )
                ;
    }

    private int handleBookUpdate(@NotNull Player user, UnaryOperator<BookMeta> function) {
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (stack.getType() != Material.WRITTEN_BOOK) {
            Utils.tell(user, "<red>You must be holding a written book to alter the author, title, or generation.");
            return 0;
        }

        if (!(stack.getItemMeta() instanceof BookMeta bookMeta)) {
            Utils.tell(user, "<red>You are not holding a book that can have it's author, title, or generation authored.");
            return 0;
        }

        stack.setItemMeta(function.apply(bookMeta));
        return 1;
    }

    private int handleEnchantmentStore(@NotNull Player user, UnaryOperator<EnchantmentStorageMeta> function) {
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        if (stack.getType() != Material.BOOK && stack.getType() != Material.ENCHANTED_BOOK) {
            Utils.tell(user, "<red>You must be holding a book or enchanted book to store an enchantment on it.");
            return 0;
        }

        ItemStack finalStack = stack;

        if (stack.getType() == Material.BOOK) {
            finalStack = stack.withType(Material.ENCHANTED_BOOK);
        }

        if (!(finalStack.getItemMeta() instanceof EnchantmentStorageMeta storageMeta)) {
            Utils.tell(user, "<red>You are not holding a book that can store enchantments.");
            return 0;
        }

        finalStack.setItemMeta(function.apply(storageMeta));
        user.getInventory().setItemInMainHand(finalStack);
        return 1;
    }

}
