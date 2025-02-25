package com.itsschatten.itemeditor;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.itsschatten.itemeditor.utils.TagType;
import com.itsschatten.yggdrasil.StringPaginator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.menus.MenuUtils;
import io.papermc.paper.inventory.PaperInventoryCustomHolderContainer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public final class ItemEditorPlugin extends JavaPlugin {

    static Supplier<StringPaginator> PAGES;

    public static @NotNull String listResolvers(final @NotNull JavaPlugin plugin, final int page) {
        final FileConfiguration config = plugin.getConfig();
        if (config.getConfigurationSection("tags") != null) {
            final StringPaginator paginator = PAGES.get();

            return (page <= 1 ? """
                    <gradient:#D8D8F6:#978897><st>             </st> Built-in Tags <st>             </st></gradient>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<prefix>'>\\<prefix></click></hover>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<required>'>\\<required></click></hover>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<first>'>\\<first></click></hover>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<optional>'>\\<optional></click></hover>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<primary>'>\\<primary></click></hover>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<secondary>'>\\<secondary></click></hover>
                    <hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<info>'>\\<info></click></hover>
                    """ : "") +
                    """
                            <gradient:#D8D8F6:#978897><st>             </st> Custom Tags <st>             </st></gradient>
                            <info>These tags may be reloaded by executing '<hover:show_text:'<gray>Click me to suggest the command!</gray>'><click:suggest_command:'/updateresolvers'>/updateresolvers</click></hover>'!</info>
                            <primary><page></primary><dark_gray>/</dark_gray><secondary><total></secondary>
                            """.replace("<page>", String.valueOf(page)).replace("<total>", String.valueOf(paginator.totalPages())) +
                    paginator.page(page).asString() + "\n\n" +
                    paginator.navigation(page);
        }

        return "<red>There is no 'tags' key in the configuration file! Thus, there are no tag resolvers.";
    }

    @Contract(" -> new")
    private static @NotNull StringPaginator getPaginator() {
        final FileConfiguration config = JavaPlugin.getPlugin(ItemEditorPlugin.class).getConfig();

        final StringBuilder builder = new StringBuilder();
        for (final String key : Objects.requireNonNull(config.getConfigurationSection("tags")).getKeys(false)) {
            final boolean argsSupported = config.getBoolean("tags." + key + ".accepts-args", false);
            final TagType type = TagType.valueOf(Objects.requireNonNull(config.getString("tags." + key + ".type")).toUpperCase());
            switch (type) {
                case PRE_PARSE -> {
                    builder.append("<hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<").append(key).append(">'>\\<")
                            .append(key).append(">")
                            .append(" <info>(Pre Parse)</info> ");

                    if (argsSupported) {
                        builder.append("<info>(Args Supported)</info>");
                    }

                    // Always last.
                    builder.append("</click></hover>");
                }
                case INSERT, INSERT_CLOSED -> {
                    builder.append("<hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<").append(key).append(">'>\\<")
                            .append(key).append(">");

                    builder.append(" <info>(Insert Tag").append(type == TagType.INSERT_CLOSED ? " Self Closing" : "").append(")</info> ");
                    if (argsSupported) {
                        builder.append("<info>(Args Supported)</info>");
                    }

                    // Always last.
                    builder.append("</click></hover>");
                }
                case STYLE ->
                        builder.append("<hover:show_text:'<gray>Click to copy the tag to your clipboard!'><click:copy_to_clipboard:'<").append(key).append(">'>\\<")
                                .append(key).append(">")
                                .append(" <info>(Style Tag)</info></click></hover>");
            }

            // New line.
            builder.append("<br>");
        }

        return new StringPaginator(builder.toString(), "/listresolvers");
    }

    public static void configureResolvers(final @NotNull JavaPlugin plugin, @Nullable final CommandSender sender) {
        if (sender != null) {
            plugin.reloadConfig();
        } else {
            plugin.saveDefaultConfig();
        }

        final FileConfiguration config = plugin.getConfig();
        if (config.contains("tags")) {
            final List<TagResolver> resolvers = new ArrayList<>();
            for (@Subst("invalid_key") final String key : Objects.requireNonNull(config.getConfigurationSection("tags")).getKeys(false)) {
                final TagType type = TagType.valueOf(Objects.requireNonNull(config.getString("tags." + key + ".type")).toUpperCase());
                final TagResolver.Builder builder = TagResolver.builder();

                switch (type) {
                    case PRE_PARSE, INSERT, INSERT_CLOSED ->
                            builder.tag(key, handleTag(type, Objects.requireNonNull(config.getString("tags." + key + ".value")), config.getString("tags." + key + ".value-no-args", ""),
                                    config.getBoolean("tags." + key + ".accepts-args", false)));
                    case STYLE -> {
                        final Style.Builder styleBuilder = Style.style();

                        // Handle color.
                        if (config.getString("tags." + key + ".color") != null && !Objects.requireNonNull(config.getString("tags." + key + ".color")).isBlank()) {
                            final String color = Objects.requireNonNull(config.getString("tags." + key + ".color"), "Found a color String but it was actually null?").toLowerCase();
                            final NamedTextColor namedTextColor = NamedTextColor.NAMES.value(color);

                            if (namedTextColor != null) {
                                styleBuilder.color(namedTextColor);
                            } else if (color.startsWith("#") || color.length() == 6) {
                                styleBuilder.color(TextColor.fromHexString((color.startsWith("#") ? "" : "#") + color));
                            } else {
                                if (sender != null) {
                                    sender.sendRichMessage("<gray>Skipping '" + color + "' as it couldn't be found in the text colors and it can't be converted to a hex color.");
                                }

                                Utils.log("Skipping '" + color + "' as it couldn't be found in the text colors and it can't be converted to a hex color.");
                            }
                        }

                        // Handle text decorations.
                        if (config.isString("tags." + key + ".decorations") && config.getString("tags." + key + ".decorations") != null && !Objects.requireNonNull(config.getString("tags." + key + ".decorations")).isBlank()) {
                            try {
                                final TextDecoration decoration = TextDecoration.valueOf(config.getString("tags." + key + ".decorations"));
                                styleBuilder.decorate(decoration);
                            } catch (IllegalArgumentException e) {
                                if (sender != null) {
                                    sender.sendRichMessage("<gray>Skipping " + config.getString("tags." + key + ".decorations") + " as a text decoration because it can't be found in the enum.");
                                }

                                Utils.log("Skipping " + config.getString("tags." + key + ".decorations") + " as a text decoration because it can't be found in the enum.");
                            }
                        } else if (config.isList("tags." + key + ".decorations")) {
                            config.getStringList("tags." + key + ".decorations").forEach((decoration) -> {
                                try {
                                    final TextDecoration actualDecoration = TextDecoration.valueOf(decoration.toUpperCase());
                                    styleBuilder.decorate(actualDecoration);
                                } catch (IllegalArgumentException e) {
                                    if (sender != null) {
                                        sender.sendRichMessage("<gray>(loop) Skipping '" + decoration + "' as a text decoration because it can't be found in the enum.");
                                    }

                                    Utils.log("(loop) Skipping '" + decoration + "' as a text decoration because it can't be found in the enum.");
                                }
                            });
                        }

                        // Handle font.
                        if (config.getString("tags." + key + ".font") != null && !Objects.requireNonNull(config.getString("tags." + key + ".font")).isBlank()) {
                            @Subst("default") String string = Objects.requireNonNull(config.getString("tags." + key + ".font"));
                            styleBuilder.font(Key.key(string));
                        }

                        builder.tag(key, Tag.styling((b) -> b.merge(styleBuilder.build(), Style.Merge.all())));
                    }
                }

                if (sender != null && !(sender instanceof ConsoleCommandSender)) {
                    sender.sendRichMessage("<gray>Adding '" + key + "' as a '" + type.name() + "' tag" + "!");
                }
                Utils.log("Adding '" + key + "' as a '" + type.name() + "' tag" + "!");
                resolvers.add(builder.build());
            }

            if (sender != null && !(sender instanceof ConsoleCommandSender)) {
                sender.sendRichMessage("<green>Loaded <yellow>" + resolvers.size() + "</yellow> tag resolvers.");
            }
            Utils.log("Loaded " + resolvers.size() + " tag resolvers.");
            StringUtil.addResolvers(resolvers);
        }

        final TagResolver resolver = StringUtil.miniMessage().build().tags();

        if (sender != null) {
            Bukkit.getServer().getPluginManager().callEvent(new UpdateResolversEvent(resolver));
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(ItemEditorPlugin.getProvidingPlugin(ItemEditorPlugin.class), () -> Bukkit.getServer().getPluginManager().callEvent(new UpdateResolversEvent(resolver)));
        }

        StringUtil.addResolvers(TagResolver.builder()
                // Prefix for the plugin.
                .tag("prefix", Tag.selfClosingInserting(StringUtil.color("<gradient:#D8D8F6:#978897>ItemEditor</gradient>")))
                // A required argument.
                .tag("required", Tag.selfClosingInserting(StringUtil.color("<red>*</red>")))
                // A first argument in a branching command.
                .tag("first", Tag.selfClosingInserting(StringUtil.color("<aqua>*</aqua>")))
                // Optional command.
                .tag("optional", Tag.selfClosingInserting(StringUtil.color("<gold>*</gold>")))
                .tag("arrow", Tag.selfClosingInserting(StringUtil.color("<dark_gray>»</dark_gray>")))
                // Main color.
                .tag("primary", Tag.styling((b) -> b.color(TextColor.fromHexString("#F5D491"))))
                // Secondary color.
                .tag("secondary", Tag.styling((b) -> b.color(TextColor.fromHexString("#D8D8F6"))))
                // Information color and styling.
                .tag("info", Tag.styling((b) -> b.color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)))
                .build());

        // Memoize the paginator.
        PAGES = Suppliers.memoizeWithExpiration(ItemEditorPlugin::getPaginator, 5, TimeUnit.MINUTES);
    }

    @Contract(pure = true)
    private static @NotNull BiFunction<ArgumentQueue, Context, Tag> handleTag(final TagType type, final String value, final String noArgsValue, final boolean supportsArguments) {
        if (supportsArguments) {
            return (args, context) -> {
                if (args.hasNext()) {
                    return replaceArguments(type, value, args);
                }

                if (!noArgsValue.isBlank()) {
                    return getTag(type, noArgsValue);
                } else {
                    return getTag(type, value);
                }
            };
        } else {
            return (args, context) -> getTag(type, value);
        }
    }

    @NotNull
    private static Tag getTag(@NotNull TagType type, String value) {
        return switch (type) {
            case PRE_PARSE -> Tag.preProcessParsed(value);
            case INSERT -> Tag.inserting(StringUtil.color(value));
            case INSERT_CLOSED -> Tag.selfClosingInserting(StringUtil.color(value));
            case STYLE -> throw new UnsupportedOperationException("Style does not support the 'handleTag' method.");
        };
    }

    private static Tag replaceArguments(final TagType type, String string, @NotNull ArgumentQueue args) {
        int i = 1;
        while (args.hasNext()) {
            final String arg = args.pop().value();

            if (i == 1) string = string.replace("<arg>", arg);
            string = string.replace("<arg" + i + ">", arg);
            i++;
        }

        return switch (type) {
            case PRE_PARSE -> Tag.preProcessParsed(string);
            case INSERT -> Tag.inserting(StringUtil.color(string));
            case INSERT_CLOSED -> Tag.selfClosingInserting(StringUtil.color(string));
            case STYLE -> throw new UnsupportedOperationException("Style tags do not support arguments.");
        };
    }

    @Override
    public void onEnable() {
        Utils.setInstance(this);
        MenuUtils.initialize(this);

        configureResolvers(this, null);
    }

    @Override
    public void onDisable() {
        Utils.setInstance(null);
    }

}
