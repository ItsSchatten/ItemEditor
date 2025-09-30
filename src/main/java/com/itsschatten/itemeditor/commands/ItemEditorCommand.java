package com.itsschatten.itemeditor.commands;

import com.itsschatten.itemeditor.commands.subcommands.*;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

/**
 * The main command of the plugin.
 */
public final class ItemEditorCommand extends BrigadierCommand {

    // TODO: can break, can place on

    /**
     * Constructs the command.
     */
    public ItemEditorCommand() {
        super("The main command for ItemEditor.", List.of("itemedit", "ie"));
        permission("itemeditor.command");

        addSubCommand(List.of(
                new AmountSubCommand(),
                new ArmorTrimSubCommand(),
                new AttributeSubCommand(),
                new BannerSubCommand(),
                new BlocksAttacksSubCommand(),
                new BookSubCommand(),
                new BreakSoundSubCommand(),
                new BucketSubCommand(),
                new ColorSubCommand(),
                new CompassSubCommand(),
                new ConsumableSubCommand(),
                new ConvertSubCommand(),
                new CooldownSubCommand(),
                new DeathProtectionSubCommand(),
                new DisplaySubCommand(),
                new DebugSubCommand(),
                new DurabilitySubCommand(),
                new EnchantableSubCommand(),
                new EnchantmentGlintSubCommand(),
                new EnchantSubCommand(),
                new EquipableSubCommand(),
                new FireworkSubCommand(),
                new FoodSubCommand(),
                new GliderSubCommand(),
                new GoatHornSubCommand(),
                new IntangibleSubCommand(),
                new ItemNameSubCommand(),
                new JukeboxSubCommand(),
                new LoreSubCommand(),
                new MapSubCommand(),
                new ModelDataSubCommand(),
                new ModelSubCommand(),
                new OminousSubCommand(),
                new PotSubCommand(),
                new PotionSubCommand(),
                new RaritySubCommand(),
                new ResistantSubCommand(),
                new RepairableSubCommand(),
                new RepairCostSubCommand(),
                new SkinSubCommand(),
                new StewSubCommand(),
                new TooltipSubCommand(),
                new ToolSubCommand(),
                new TypeSubCommand(),
                new UnbreakableSubCommand(),
                new WeaponSubCommand()
        ));
    }

    // Because of this requirement here, this command can never work for Command Blocks or Console.
    // As such it is ALWAYS safe to assume that the sender of this command is a player.
    @Contract(pure = true)
    @Override
    public @NotNull Predicate<CommandSourceStack> requirements() {
        return (source) -> source.getSender() instanceof Player;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("itemeditor")
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
