package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class ItemNameSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie name <secondary><name></secondary>").hoverEvent(StringUtil.color("""
                <primary>Set the item's default name.
                \s
                ◼ <secondary><name><required> </secondary> The name for the item.
                ◼ <secondary>[-view]<optional></secondary> View the item's current name.
                ◼ <secondary>[-clear]<optional></secondary> Clear the name.""").asHoverEvent()).clickEvent(ClickEvent.suggestCommand("/ie name "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("name")
                .then(argument("name", StringArgumentType.greedyString())
                        .executes(context -> updateName(context, (stack, meta) -> {
                            final Component component = StringUtil.color("<!i>" + StringArgumentType.getString(context, "name"));
                            meta.itemName(component);

                            Utils.tell(context.getSource(), StringUtil.color("<primary>Set your item's name to <reset>'").append(component).append(StringUtil.color("<reset>'<primary>.")));

                            if (meta.hasDisplayName()) {
                                Utils.tell(context.getSource(), "<i><gray>Your item has a set display name so your set item name will not appear.");
                                Utils.tell(context.getSource(), "<i><dark_gray><click:run_command:'/ie display -clear'><hover:show_text:'<gray>This will run '/ie display -clear'.'>[Click to remove the display name.]</hover></click>");
                            }

                            return meta;
                        }))
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of(getName(context)), builder))
                )
                .then(literal("-clear")
                        .executes(context -> updateName(context, (stack, meta) -> {
                            meta.itemName(null);

                            Utils.tell(context.getSource(), StringUtil.color("<primary>Your item's name has been reset to <yellow>" + stack.getType().getKey().getKey().toLowerCase().replace("_", " ") + "</yellow>."));
                            return meta;
                        }))
                )
                .then(literal("-view")
                        .executes(this::handleView)
                );
    }

    private @NotNull String getName(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            return "";
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return "";
        }

        return meta.hasItemName() ? MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.itemName())) : "";
    }

    private int updateName(final @NotNull CommandContext<CommandSourceStack> context, BiFunction<ItemStack, ItemMeta, ItemMeta> function) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        stack.setItemMeta(function.apply(stack, meta));
        return 1;
    }

    private int handleView(final @NotNull CommandContext<CommandSourceStack> context) {
        final Player user = (Player) context.getSource().getSender();

        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        // Get the item's meta and check if it's null, it really shouldn't be but safety.
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            Utils.tell(user, "<red>For some reason the item's meta is null!");
            return 0;
        }

        if (meta.hasItemName()) {
            Utils.tell(user, StringUtil.color("<primary>Your item's name is currently:").appendSpace().append(meta.itemName()).colorIfAbsent(meta.hasRarity() ? meta.getRarity().color() : NamedTextColor.WHITE).append(StringUtil.color("<primary>.")));
        } else {
            Utils.tell(user, "<primary>Your item doesn't currently have an item name.");
        }

        if (meta.hasDisplayName()) {
            Utils.tell(context.getSource(), "<i><gray>Your item has a set display name so your set item name will not appear.");
            Utils.tell(context.getSource(), "<i><dark_gray><click:run_command:'/ie display -clear'><hover:show_text:'<gray>This will run '/ie display -clear'.'>[Click to remove the display name.]</hover></click>");
        }

        return 1;
    }

}
