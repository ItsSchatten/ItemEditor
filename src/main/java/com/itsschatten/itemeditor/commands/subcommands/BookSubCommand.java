package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BookSubCommand extends PlayerSubCommand {

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public BookSubCommand(CommandBase base) {
        super("book", Collections.emptyList(), base);
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><enchant|author|title|gen.> ...</secondary>").hoverEvent(StringUtil.color("""
                <primary>Alter written book title, author, and generation or the stored enchantments.
                \s
                <info>While holding a book or enchanted book.</info>
                ◼ <secondary><enchantment><required> [level]<optional></secondary> Store an enchantment on a book or an enchanted book.
                <info>While holding a written.</info>
                ◼ <secondary><author><required> <author string|-clear><required></secondary> Set the author of the written book.
                ◼ <secondary><title><required> <title string|-clear><required></secondary> Set the title of the written book.
                ◼ <secondary><generation><required> <generation|-clear><required></secondary> Set the generation of the written book.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
    }

    @Override
    protected void run(@NotNull Player user, String[] args) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            returnTell("<red>You need to be holding an item in your hand.");
            return;
        }

        ItemStack finalStack = stack;

        if (stack.getType() == Material.BOOK) {
            finalStack = stack.withType(Material.ENCHANTED_BOOK);
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        switch (finalStack.getItemMeta()) {
            case final BookMeta meta -> {
                if (stack.getType() != Material.WRITTEN_BOOK) {
                    returnTell("<red>You must be holding a written book to alter the author, title, or generation.");
                    return;
                }

                switch (args[0].toLowerCase()) {
                    case "author" -> {
                        // Make sure we have arguments.
                        if (args.length == 1) {
                            returnTell("<red>Please provide an author or '-clear' to remove.");
                            return;
                        }

                        // Check if we wish to remove the author.
                        if (args[1].equalsIgnoreCase("-clear")) {
                            meta.setAuthor(null);
                            tell("<primary>Cleared the author from your book.");
                        } else {
                            meta.author(StringUtil.color(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
                            tell("<primary>Set the author of your book to <reset>'" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + "<reset>'<primary>.");
                        }
                    }

                    case "title" -> {
                        // Make sure we have arguments.
                        if (args.length == 1) {
                            returnTell("<red>Please provide a title or '-clear' to remove.");
                            return;
                        }

                        // Check if we wish to remove the author.
                        if (args[1].equalsIgnoreCase("-clear")) {
                            meta.setTitle(null);
                            tell("<primary>Cleared the title from your book.");
                        } else {
                            String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                            if (title.length() > 32) {
                                title = title.substring(0, 32);
                                tell("<gray>Your title has been limited to 32 characters.");
                            }

                            meta.title(StringUtil.color(title));
                            tell("<primary>Set the author of your book to <reset>'" + title + "<reset>'<primary>.");
                        }
                    }

                    case "generation" -> {
                        // Make sure we have arguments.
                        if (args.length == 1) {
                            returnTell("<red>Please provide a generation or '-clear' to remove.");
                            return;
                        }

                        // Remove the generation if '-clear', otherwise set the generation if valid.
                        if (args[1].equalsIgnoreCase("-clear")) {
                            meta.setGeneration(null);
                            tell("<primary>Removed the generation from your book.");
                        } else {
                            // Try to get the generation if invalid sends a message.
                            try {
                                final BookMeta.Generation generation = BookMeta.Generation.valueOf(args[1].toUpperCase());
                                meta.setGeneration(generation);
                                tell("<primary>Set the generation of your book to <secondary>" + generation.name().toLowerCase().replace("_", " ") + "</secondary>.");
                            } catch (IllegalArgumentException ignored) {
                                returnTell("<yellow>" + args[1] + "<red> is not a valid generation.");
                                return;
                            }
                        }
                    }

                    default ->
                            returnTell("<red>Unknown written book operation: <yellow>" + args[0] + "</yellow>\nValid operations are: <yellow>author, title, or generation</yellow>");
                }

                stack.setItemMeta(meta);
            }

            case final EnchantmentStorageMeta meta -> {
                if (stack.getType() != Material.BOOK && stack.getType() != Material.ENCHANTED_BOOK) {
                    returnTell("<red>You must be holding a book or enchanted book to store an enchantment on it.");
                    return;
                }

                if (args.length == 0 || args[0].equalsIgnoreCase("-view")) {
                    if (!meta.hasStoredEnchants()) {
                        tell("<primary>Your item is not storing enchantments.");
                        return;
                    }

                    tell("<primary>Your item currently storing the following enchantments: <#D8D8F6>" + String.join("<gray>,</gray> ", meta.getStoredEnchants().entrySet().stream()
                            .map((entry) -> "<click:suggest_command:'/ie book " + entry.getKey().getKey().getKey() + " 0'><hover:show_text:'<gray><i>Click to suggest the command to remove this enchantment!'>" + entry.getKey().getKey().getKey().replace("_", " ") + " " + entry.getValue() + "</hover></click>").toList()));
                    tell("<gray><i>Click an enchantment above to suggest the command to remove it!");
                    return;
                }

                // Get the enchantment from the registry.
                final NamespacedKey enchantmentKey = args[0].contains(":") ? NamespacedKey.fromString(args[0]) : NamespacedKey.minecraft(args[0]);
                if (enchantmentKey == null) {
                    returnTell("<red>Could not find any enchantment by the name <yellow>" + args[0] + "</yellow>.");
                    return;
                }
                final Enchantment enchantment = Registry.ENCHANTMENT.get(enchantmentKey);
                if (enchantment == null) {
                    returnTell("<red>Could not find any enchantment by the name <yellow>" + args[0] + "</yellow>.");
                    return;
                }

                if (args.length >= 2) {
                    // Get a level for the enchantment.
                    final int level = getNumber(1, "<yellow>" + args[1] + "</yellow><red> is not a valid number!");
                    // Check if we want to remove an enchantment.
                    if (level <= 0) {
                        // Remove our enchantment.
                        meta.removeStoredEnchant(enchantment);
                        tell("<primary>Removed the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> from your item.");
                    } else {
                        // Add our enchantment with the provided level.
                        meta.addStoredEnchant(enchantment, level, true);
                        tell("<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> at level <secondary>" + level + "</secondary> to your item.");
                    }
                } else {
                    // Add our enchantment at level 1.
                    meta.addStoredEnchant(enchantment, 1, true);
                    tell("<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> to your item.");
                }

                // Finally, update the item meta.
                finalStack.setItemMeta(meta);
                user.getInventory().setItemInMainHand(finalStack);
            }

            case null, default -> returnTell("<red>You are not holding a book!");
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermission(sender)) {
            if (sender instanceof final Player player) {
                if (args.length == 1) {
                    // Check if we have a book or enchanted book, that way we can store enchants on it.
                    if (player.getInventory().getItemInMainHand().getType() == Material.BOOK || player.getInventory().getItemInMainHand().getType() == Material.ENCHANTED_BOOK) {
                        final List<String> strings = new ArrayList<>(Registry.ENCHANTMENT.stream().map((enchant) -> enchant.getKey().getKey()).toList());
                        strings.add("-view");
                        return strings.stream().filter((name) -> name.contains(args[0].toLowerCase())).toList();
                    } else {
                        // Probably a written book, go ahead and have that tab complete.
                        return Stream.of("author", "title", "generation").filter((name) -> name.contains(args[0].toLowerCase())).toList();
                    }
                }

                if (args.length == 2) {
                    if (player.getInventory().getItemInMainHand().getItemMeta() instanceof EnchantmentStorageMeta) {
                        // Get the enchantment.
                        final NamespacedKey enchantmentKey = args[0].contains(":") ? NamespacedKey.fromString(args[0]) : NamespacedKey.minecraft(args[0]);
                        if (enchantmentKey == null) {
                            return super.getTabComplete(sender, args);
                        }
                        final Enchantment enchantment = Registry.ENCHANTMENT.get(enchantmentKey);
                        if (enchantment == null) {
                            return super.getTabComplete(sender, args);
                        }

                        return IntStream.rangeClosed(0, enchantment.getMaxLevel()).mapToObj(Integer::toString).filter((name) -> name.contains(args[1].toLowerCase())).toList();
                    }

                    final List<String> strings = new ArrayList<>();
                    strings.add("-clear");
                    if (args[0].equalsIgnoreCase("generation")) {
                        strings.addAll(Arrays.stream(BookMeta.Generation.values()).map((gen) -> gen.name().toLowerCase()).toList());
                    }

                    return strings.stream().filter((name) -> name.contains(args[1].toLowerCase())).toList();
                }
            }
        }

        return super.getTabComplete(sender, args);
    }
}
