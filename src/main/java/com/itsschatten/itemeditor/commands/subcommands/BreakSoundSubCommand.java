package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class BreakSoundSubCommand extends BrigadierCommand {

    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie breaksound <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the sound to play when the item breaks.
                \s
                ◼ <secondary><name><required></secondary> The break sound for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current break sound.
                ◼ <secondary>[-clear]<optional></secondary> Clear the break sound.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie breaksound "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("breaksound")
                .then(literal("-view")
                        .executes(this::view)
                )
                .then(literal("-clear")
                        .executes(this::clear)
                )
                .then(argument("sound", ArgumentTypes.resource(RegistryKey.SOUND_EVENT))
                        .executes(context -> updateBreakSound(context, () -> {
                            final Sound sound = context.getArgument("sound", Sound.class);
                            final Key key = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).getKey(sound);
                            Utils.tell(context.getSource(), "<primary>Updated the playable song on your item to '<secondary>" + key.asString() + "</secondary>'!");
                            return key;
                        }))
                );
    }

    private int view(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        if (!stack.hasData(DataComponentTypes.BREAK_SOUND)) {
            Utils.tell(user, "<primary>Your item does not have a break sound!");
            return 0;
        }

        final Key key = stack.getData(DataComponentTypes.BREAK_SOUND);
        Utils.tell(user, "<primary>Break sound <dark_gray>» </dark_gray> <secondary>" + key.asString() + "</secondary>.");
        return 1;
    }

    private int clear(final CommandContext<CommandSourceStack> context) {
        return updateBreakSound(context, () -> {
            Utils.tell(context.getSource(), "<primary>Cleared the playable song from your item!");
            return null;
        });
    }

    private int updateBreakSound(final @NotNull CommandContext<CommandSourceStack> context, Supplier<Key> operator) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.setData(DataComponentTypes.BREAK_SOUND, operator.get());
        return 1;
    }

}
