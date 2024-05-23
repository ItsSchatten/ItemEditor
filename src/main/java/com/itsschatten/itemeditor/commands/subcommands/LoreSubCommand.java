package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.StringWrapUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import com.itsschatten.yggdrasil.commands.PlayerSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LoreSubCommand extends PlayerSubCommand {

    /**
     * The clipboard map, used to store a list of components keyed to a player's UUID.
     */
    private final Map<UUID, List<Component>> clipboard;

    /**
     * Constructs the command.
     *
     * @param owningCommand The command that "owns" this sub command, used to register this sub command in tab complete.
     */
    public LoreSubCommand(@NotNull CommandBase owningCommand) {
        super("lore", Collections.emptyList(), owningCommand);

        this.clipboard = new HashMap<>();
    }

    // Description/Usage message for this sub command.
    @Override
    public Component descriptionComponent() {
        return StringUtil.color("<primary>" + commandString() + " <secondary><add|set|remove|...> [values]</secondary>").hoverEvent(StringUtil.color("""
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
                ◼ <secondary><copy><first></secondary> Copy the lore of one item into your clipboard.
                ◼ <secondary><copybook><first></secondary> Copy the contents of a book to your clipboard.
                ◼ <secondary><copyfile><first></secondary> Copy the contents of a file to your clipboard.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand(commandString() + " "));
    }

    @Override
    protected void run(@NotNull Player user, String[] args) {
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();

        // The lore of the item, if it doesn't have any lore strings, we make a new ArrayList for the lore.
        final List<Component> lore = meta != null && meta.hasLore() ? (meta.lore() == null ? new ArrayList<>() : meta.lore()) : new ArrayList<>();
        assert lore != null;

        final String loreLine = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String successString = "<red>Someone didn't update the success string!";
        // Switch the first argument.
        switch (args[0].toLowerCase()) {
            // Add a lore line.
            case "add" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);
                if (args.length > 2 && args[1].equalsIgnoreCase("-wrap")) {
                    // Wrap the string from the third argument
                    lore.addAll(StringWrapUtils.convertStringToComponentList("<!i>" + String.join(" ", Arrays.copyOfRange(args, 2, args.length))));

                    successString = "<primary>Added <reset>'<dark_purple>" + String.join(" ", Arrays.copyOfRange(args, 2, args.length)) + "<reset>' <gray>(wrapped)</gray> <primary>to your item's lore.";
                } else {
                    // Give a hint about how to wrap a lore line.
                    if (args.length == 2 && args[1].equalsIgnoreCase("-wrap")) {
                        tell("<gray><i>Hint: If you want to just wrap a lore line use '/ie wrap <number>'.");
                    }

                    lore.add(StringUtil.color("<!i>" + loreLine));
                    successString = "<primary>Added <reset>'<dark_purple>" + loreLine + "<reset>'<primary> to your item's lore.";
                }
            }

            // Insert a lore line.
            case "insert" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);
                // Get the lore line number to remove.
                final int number = getLoreLine(lore, 1, args);
                if (number == -1) {
                    return;
                }

                if (args.length > 3 && args[2].equalsIgnoreCase("-wrap")) {
                    // Wrap the string from the third argument.
                    if (number == 0 && lore.isEmpty()) {
                        lore.addAll(StringWrapUtils.convertStringToComponentList("<!i>" + String.join(" ", Arrays.copyOfRange(args, 3, args.length))));
                    } else {
                        lore.addAll(number, StringWrapUtils.convertStringToComponentList("<!i>" + String.join(" ", Arrays.copyOfRange(args, 3, args.length))));
                    }

                    successString = "<primary>Inserted <reset>'" + String.join(" ", Arrays.copyOfRange(args, 3, args.length)) + "<reset>'<gray>(wrapped)</gray> <primary>to position <secondary>#" + number + "</secondary> in your item's lore.";
                } else {
                    // Give a hint about how to wrap a lore line.
                    if (args.length == 3 && args[2].equalsIgnoreCase("-wrap")) {
                        tell("<gray><i>Hint: If you want to just wrap a lore line use '/ie wrap <number>'.");
                    }

                    final String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    // Add the lore line.
                    if (number == 0 && lore.isEmpty()) {
                        lore.add(StringUtil.color("<!i>" + line));
                    } else {
                        lore.add(number, StringUtil.color("<!i>" + line));
                    }
                    successString = "<primary>Inserted <reset>'<dark_purple>" + line + "<reset>'<primary> to position " + number + " in your item's lore.";
                }

            }

            // Set a lore line.
            case "set" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);

                if (args.length < 2) {
                    tell("<red>Please provide a lore line to set.");
                    return;
                }

                // Get the lore line number to remove.
                final int number = getLoreLine(lore, 1, args);
                if (number == -1) {
                    return;
                }

                final String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (number == 0 && lore.isEmpty()) {
                    lore.add(StringUtil.color("<!i>" + line));
                } else {
                    lore.set(number, StringUtil.color("<!i>" + line));
                }

                successString = "<primary>Set position <secondary>#" + number + "</secondary> in your item's lore to <reset>'<dark_purple>" + line + "<reset>'<primary>.";
            }

            // Remove a lore line.
            case "remove" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);

                if (args.length < 2) {
                    tell("<red>Please provide a lore line to remove.");
                    return;
                }

                // Get the lore line number to remove.
                final int number = getLoreLine(lore, 1, args);
                if (number == -1) {
                    return;
                }

                // remove the lore line.
                final String removed = MiniMessage.miniMessage().serialize(lore.remove(number));
                successString = "<primary>Removed line <secondary><hover:show_text:'" + removed + "'>#" + number + "</hover></secondary> from the item's lore.\n<gray><i>Tip: Hover over the number to see the lore line you removed!</i></gray>";
            }

            // Resets the lore to nothing.
            case "reset" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);
                lore.clear();
                successString = "<primary>Reset your item's lore to nothing.";
            }

            // Replace all string occurrences with the provided string.
            case "replace" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);

                // Make sure we have the appropriate number of arguments.
                if (args.length < 3) {
                    returnTell("<red>Please supply a text to replace and what to replace it with.\n<secondary>/ie lore replace <to replace> <replacement>");
                    return;
                }

                // What we are replacing.
                final String replace = args[1];
                // What to replace it with.
                final String replacement = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                // Replace all the lore.
                lore.replaceAll(component -> component.replaceText(TextReplacementConfig.builder().matchLiteral(replace).replacement(replacement).build()));
                successString = "<primary>Replaced any iteration of <secondary>" + replace + "</secondary> with <reset>'" + replacement + "<reset>'<primary>.";
            }

            // Wrap a lore line.
            case "wrap" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);

                // TODO: This.
                tell("<red>We no worky right now.");
                return;
            }


            // Clipboard shenanigans.

            // Paste the player's clipboard.
            case "paste" -> {
                // Make sure we have something on the clipboard.
                if (clipboard.get(user.getUniqueId()) == null) {
                    returnTell("<red>You don't have anything on your clipboard!");
                    return;
                }

                // Check if we have args, if we do, we might need to do some stuff!
                if (args.length >= 2) {
                    // View the player's current clipboard.
                    if (args[1].equalsIgnoreCase("-view")) {
                        tell(StringUtil.color("<gradient:#D8D8F6:#978897>Your clipboard contents</gradient>"));
                        clipboard.get(user.getUniqueId()).forEach(this::tell);
                        return;
                    }

                    // Check if we have a valid item and the meta is also valid.
                    checkValid(stack, meta);

                    // Add all the lore to the end of the current lore.
                    if (args[1].equalsIgnoreCase("-add")) {
                        lore.addAll(clipboard.get(user.getUniqueId()));
                        break;
                    }

                    // Insert all the lore to the end of the current lore.
                    if (args[1].equalsIgnoreCase("-insert")) {
                        final int number = getLoreLine(lore, 2, args);
                        if (number == 0 && lore.isEmpty()) {
                            lore.addAll(clipboard.get(user.getUniqueId()));
                        } else {
                            lore.addAll(number, clipboard.get(user.getUniqueId()));
                        }
                        break;
                    }
                }

                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);

                // No args or invalid, clear the lore and replace it with the clipboard.
                lore.clear();
                lore.addAll(clipboard.get(user.getUniqueId()));
                successString = "<primary>Pasted your clipboard to your item's lore!";
            }

            // Copy the current item's lore to the player's clipboard.
            case "copy" -> {
                // Check if we have a valid item and the meta is also valid.
                checkValid(stack, meta);

                // Put the lore on the clipboard.
                clipboard.put(user.getUniqueId(), lore);
                tell("""
                        <primary>Copied your item's lore to your clipboard!
                        To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                        To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
                return;
            }

            case "copybook" -> {
                checkValid(stack, meta);

                // Check if our meta is an instance of BookMeta.
                if (meta instanceof final BookMeta bookMeta) {
                    // Place all the pages in the clipboard.
                    final List<Component> bookPages = new ArrayList<>();
                    bookMeta.pages().forEach((page) -> bookPages.add(StringUtil.color(PlainTextComponentSerializer.plainText().serialize(page))));
                    clipboard.put(user.getUniqueId(), bookPages);

                    tell("""
                            <primary>Copied the book's pages to your clipboard!
                            To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                            To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
                    return;
                }

                tell("<yellow>" + stack.getType().getKey().getKey() + "</yellow><red> is not a book that contains pages!");
                return;
            }

            case "copyfile" -> {
                if (args.length < 2) {
                    tell("<red>Please provide a path for the file.\n<yellow>Tip: The file should be placed in the 'plugins/ItemEditor' file.");
                    return;
                }

                // Get a file stored within the plugins/ItemEditor directory.
                final File file = new File(Utils.getInstance().getDataFolder(), args[1]);
                if (!file.exists()) {
                    tell("<red>Couldn't find a file with the path <yellow>" + args[1] + "</yellow>!");
                    return;
                }

                final List<Component> fileLore = new ArrayList<>();
                try {
                    // Get the contents of the file and set it to UTF_8, then "read" each line and add it to the fileLore.
                    final List<String> contents = FileUtils.readLines(file, StandardCharsets.UTF_8);
                    contents.forEach((line) -> fileLore.add(StringUtil.color(line)));
                } catch (IOException e) {
                    Utils.sendDeveloperErrorMessage(user, e);
                    Utils.logError(e);
                    tell("<red>An error occurred whilst reading the file's data, see the console for more information.");
                }

                // Set the file lore to our clipboard.
                clipboard.put(user.getUniqueId(), fileLore);
                tell("""
                        <primary>Copied the contents of the provided file to your clipboard.
                        To paste it execute '<click:run_command:'/ie lore paste'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste</secondary></hover></click>'!
                        To view your clipboard execute '<click:run_command:'/ie lore paste -view'><hover:show_text:'<gray>Click me to execute the command!'><secondary>/ie lore paste -view</secondary></hover></click>'!""");
                return;
            }

        }

        // Check if we have meta.
        if (meta != null) {
            // Set the lore to the new one and update it on the item.
            meta.lore(lore.isEmpty() ? null : lore);
            stack.setItemMeta(meta);
        }

        // Send our success message.
        tell(successString);
    }

    private void checkValid(final @NotNull ItemStack stack, final ItemMeta meta) {
        if (stack.isEmpty()) {
            returnTell("<red>You need to be holding an item in your hand.");
            return;
        }

        if (meta == null) {
            returnTell("<red>For some reason the item's meta is null!");
        }
    }

    private int getLoreLine(final List<Component> lore, int index, final String @NotNull [] args) {
        if (args[index].equalsIgnoreCase("last")) {
            return lore.size() - 1;
        }

        // Get the lore line to remove.
        final int number = getNumber(index, "<yellow>" + args[index] + "</yellow><red> is not a number.");

        // Check our range.
        if (number < 0) {
            returnTell("<red>You cannot use negative lore lines.");
            return -1;
        }

        if (number > lore.size()) {
            returnTell("<red>The lore line you specified doesn't exist.");
            return -1;
        }

        return number;
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (testPermissionSilent(sender)) {
            if (args.length == 1) {
                return Stream.of("add", "insert", "set", "remove", "reset", "copy", "copybook", "copyfile", "paste", "replace", "wrap")
                        .filter((name) -> name.contains(args[0].toLowerCase(Locale.ROOT))).toList();
            }

            if (args.length == 2 && sender instanceof final Player player) {
                final ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();

                return switch (args[0].toLowerCase()) {
                    case "add" -> Stream.of("-wrap").toList();
                    case "paste" ->
                            Stream.of("-view", "-add", "-insert").filter((name) -> name.contains(args[1].toLowerCase(Locale.ROOT))).toList();
                    case "insert", "set", "remove", "wrap" ->
                            getLoreLines(meta).stream().filter((name) -> name.contains(args[1].toLowerCase(Locale.ROOT))).toList();

                    default -> super.getTabComplete(sender, args);
                };
            }

            if (args.length == 3 && sender instanceof final Player player) {
                final ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();

                return switch (args[0].toLowerCase()) {
                    case "insert" ->
                            Stream.of("-wrap").filter((name) -> name.contains(args[1].toLowerCase(Locale.ROOT))).toList();
                    case "set" -> {
                        try {
                            yield meta != null && meta.hasLore() ? List.of(MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.lore()).get(Integer.parseInt(args[1]))))
                                    : Collections.emptyList();
                        } catch (NumberFormatException ignored) {
                            yield super.getTabComplete(sender, args);
                        }
                    }
                    case "paste" ->
                            getLoreLines(meta).stream().filter((name) -> name.contains(args[2].toLowerCase(Locale.ROOT))).toList();
                    default -> super.getTabComplete(sender, args);
                };
            }
        }
        return super.getTabComplete(sender, args);
    }

    // Gets the lore lines off an item.
    private @NotNull List<String> getLoreLines(ItemMeta meta) {
        final List<String> tab = new ArrayList<>();
        tab.add("last");
        tab.addAll(meta != null && meta.hasLore() ? IntStream.range(0, Objects.requireNonNull(meta.lore()).size()).mapToObj(Integer::toString).toList() : List.of("0"));
        return tab;
    }
}
