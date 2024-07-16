package com.itsschatten.itemeditor;

import com.itsschatten.itemeditor.commands.ItemEditorCommand;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemEditorPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Utils.setInstance(this, true, false);

        StringUtil.addResolvers(TagResolver.builder()
                // Prefix for the plugin.
                .tag("prefix", Tag.selfClosingInserting(StringUtil.color("<gradient:#D8D8F6:#978897>ItemEditor</gradient>")))
                // A required argument.
                .tag("required", Tag.selfClosingInserting(StringUtil.color("<red>*</red>")))
                // A first argument in a branching command.
                .tag("first", Tag.selfClosingInserting(StringUtil.color("<aqua>*</aqua>")))
                // Optional command.
                .tag("optional", Tag.selfClosingInserting(StringUtil.color("<gold>*</gold>")))
                // Main color.
                .tag("primary", Tag.styling((b) -> b.color(TextColor.fromHexString("#F5D491"))))
                // Secondary color.
                .tag("secondary", Tag.styling((b) -> b.color(TextColor.fromHexString("#D8D8F6"))))
                // Information color and styling.
                .tag("info", Tag.styling((b) -> b.color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)))
                .build());
    }

    @Override
    public void onDisable() {
        Utils.setInstance(null);
    }
}
