package com.itsschatten.itemeditor.commands;

import com.itsschatten.itemeditor.commands.subcommands.*;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.List;

/**
 * The main command of the plugin.
 */
public class ItemEditorCommand extends BrigadierCommand {

    // TODO: Tool sub command.
    // TODO: Check and make sure that we've added a command for all things item manipulation.

    /**
     * Constructs the command.
     */
    public ItemEditorCommand() {
        super("The main command for ItemEditor.", List.of("itemedit", "ie"));

        addSubCommand(List.of(
                new AmountSubCommand(),
                new ArmorTrimSubCommand(),
                new AttributeSubCommand(),
                new BookSubCommand(),
                new BannerSubCommand(),
                new BucketSubCommand(),
                new ColorSubCommand(),
                new CompassSubCommand(),
                new CustomModelDataSubCommand(),
                new DurabilitySubCommand(),
                new EnchantmentGlintSubCommand(),
                new EnchantSubCommand(),
                new FireResistantSubCommand(),
                new FireworkSubCommand(),
                new FoodSubCommand(),
                new GoatHornSubCommand(),
                new HideSubCommand(),
                new HideTooltipSubCommand(),
                new ItemNameSubCommand(),
                new LoreSubCommand(),
                new PotionSubCommand(),
                new RaritySubCommand(),
                new DisplaySubCommand(),
                new RepairCostSubCommand(),
                new ShowSubCommand(),
                new SkinSubCommand(),
                new TypeSubCommand(),
                new UnbreakableSubCommand()
        ));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("itemeditor")
                .requires(context -> context.getSender() instanceof Player)
                .executes(context -> {
                    final PluginDescriptionFile pdf = Utils.getInstance().getDescription();

                    final Component component = StringUtil.color("<gradient:#D8D8F6:#978897><st>     </st> ItemEditor v" + pdf.getVersion() + " <st>     </st></gradient>").colorIfAbsent(TextColor.fromHexString("#F5D491")).appendNewline()
                            .append(StringUtil.color("<gray><i>Developed by ItsSchatten</i></gray>")).appendNewline().appendNewline()
                            .append(StringUtil.color("<gray><i>Hover over a command for additional information! Click it to suggest the command.</i><reset>\n<red>* Required argument. <aqua>* First argument. <gold>* Optional argument.")).appendNewline().appendNewline();

                    Utils.tell(context.getSource(), component, subcommandDescriptionComponents());
                    return Command.SINGLE_SUCCESS;
                });
    }
}
