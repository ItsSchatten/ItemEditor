package com.itsschatten.itemeditor.commands;

import com.itsschatten.itemeditor.commands.subcommands.*;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.CommandBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.List;

/**
 * The main command of the plugin.
 */
public class ItemEditorCommand extends CommandBase {

    /**
     * Constructs the command.
     */
    public ItemEditorCommand() {
        super("itemeditor");

        setAliases(List.of("itemedit", "ie", "iteme", "editor"));
        setDescription("The main command for ItemEditor.");

        registerSubCommands(
                new AmountSubCommand(this),
                new ArmorTrimSubCommand(this),
                new AttributeSubCommand(this),
                new BannerSubCommand(this),
                new BookSubCommand(this),
                new BucketSubCommand(this),
                new ColorSubCommand(this),
                new CompassSubCommand(this),
                new CustomModelDataSubCommand(this),
                new DurabilitySubCommand(this),
                new EnchantmentGlintSubCommand(this),
                new EnchantSubCommand(this),
                new FireResistantSubCommand(this),
                new FireworkSubCommand(this),
                new GoatHornSubCommand(this),
                new HideAllSubCommand(this),
                new HideSubCommand(this),
                new HideTooltipSubCommand(this),
                new ItemNameSubCommand(this),
                new LoreSubCommand(this),
                new MaxDurabilitySubCommand(this),
                new MaxStackSizeSubCommand(this),
                new PotionSubCommand(this),
                new RenameSubCommand(this),
                new RepairCostSubCommand(this),
                new ShowAllSubCommand(this),
                new ShowSubCommand(this),
                new SkinSubCommand(this),
                new TypeSubCommand(this),
                new UnbreakableSubCommand(this));
    }

    // Handles the execution of this command.
    @Override
    public void runCommandSender(CommandSender sender, String[] args) {
        final PluginDescriptionFile pdf = Utils.getInstance().getDescription();

        final Component component = StringUtil.color("<gradient:#D8D8F6:#978897><st>     </st> ItemEditor v" + pdf.getVersion() + " <st>     </st></gradient>").colorIfAbsent(TextColor.fromHexString("#F5D491")).appendNewline()
                .append(StringUtil.color("<gray><i>Developed by ItsSchatten</i></gray>")).appendNewline().appendNewline()
                .append(StringUtil.color("<gray><i>Hover over a command for additional information! Click it to suggest the command.</i><reset>\n<red>* Required argument. <aqua>* First argument. <gold>* Optional argument.")).appendNewline().appendNewline();

        tell(component, getSubCommandDescriptionComponents());
    }
}
