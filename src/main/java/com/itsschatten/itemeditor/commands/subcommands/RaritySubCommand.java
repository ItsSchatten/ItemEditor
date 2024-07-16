package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.commands.arguments.GenericEnumArgument;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class RaritySubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie rarity <secondary><rarity></secondary>").hoverEvent(StringUtil.color("""
                <primary>Updates the rarity of the item.
                \s
                ◼ <secondary>[rarity]<required></secondary> The rarity to set the item to.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie rarity "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("rarity")
                .then(Commands.argument("rarity", GenericEnumArgument.generic(ItemRarity.class))
                        .executes(context -> {
                            final Player user = (Player) context.getSource().getSender();

                            // Get the item stack in the user's main hand.
                            final ItemStack stack = user.getInventory().getItemInMainHand();

                            // Get the item's meta and check if it's null, it really shouldn't be but safety.
                            final ItemMeta meta = stack.getItemMeta();

                            // Get the rarity from the arguments.
                            final ItemRarity rarity = context.getArgument("rarity", ItemRarity.class);

                            // Update the rarity to the item.
                            meta.setRarity(rarity);
                            stack.setItemMeta(meta);

                            Utils.tell(user, "<primary>Updated your item's rarity to <" + rarity.color().asHexString() + ">" + rarity.name().toLowerCase() + "</" + rarity.color().asHexString() + ">!");
                            return 1;
                        })
                );
    }

}
