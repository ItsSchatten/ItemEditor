package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public final class EnchantSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie enchant <secondary><enchantment></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Add and remove enchantments.
                        \s
                        ◼ <secondary><enchantment><required></secondary> The enchantment to add.
                        ◼ <secondary><level><optional></secondary> The level of the enchantment, set to 0 to remove the enchantment.
                        ◼ <secondary>[-view]<optional></secondary> View all enchantments and their levels.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie enchant "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("enchant")
                .then(argument("enchantment", ArgumentTypes.resource(RegistryKey.ENCHANTMENT))
                        .then(argument("level", IntegerArgumentType.integer(0))
                                .executes(context -> updateEnchantment(context, meta -> {
                                    final Enchantment enchantment = context.getArgument("enchantment", Enchantment.class);
                                    final int level = IntegerArgumentType.getInteger(context, "level");

                                    if (meta instanceof EnchantmentStorageMeta storageMeta) {
                                        if (level <= 0) {
                                            storageMeta.removeStoredEnchant(enchantment);
                                            Utils.tell(context.getSource(), "<primary>Removed the stored enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> from your item.");
                                        } else {
                                            storageMeta.addStoredEnchant(enchantment, level, true);
                                            Utils.tell(context.getSource(), "<primary>Stored the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> at level <secondary>" + level + "</secondary> to your item.");
                                        }
                                    } else {
                                        if (level <= 0) {
                                            meta.removeEnchant(enchantment);
                                            Utils.tell(context.getSource(), "<primary>Removed the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> from your item.");
                                        } else {
                                            meta.addEnchant(enchantment, level, true);
                                            Utils.tell(context.getSource(), "<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> at level <secondary>" + level + "</secondary> to your item.");
                                        }
                                    }


                                    return meta;
                                }))
                        )
                        .executes(context -> updateEnchantment(context, meta -> {
                            final Enchantment enchantment = context.getArgument("enchantment", Enchantment.class);
                            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                                storageMeta.addStoredEnchant(enchantment, 1, true);
                                Utils.tell(context.getSource(), "<primary>Stored the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> to your item.");
                            } else {
                                meta.addEnchant(enchantment, 1, true);
                                Utils.tell(context.getSource(), "<primary>Added the enchantment <secondary>" + enchantment.getKey().getKey() + "</secondary> to your item.");
                            }
                            return meta;
                        }))
                )
                .then(literal("-view")
                        .executes(this::handleView)
                );
    }

    private int updateEnchantment(@NotNull CommandContext<CommandSourceStack> context, final UnaryOperator<ItemMeta> function) {
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

        stack.setItemMeta(function.apply(meta));
        return 1;
    }

    private int handleView(@NotNull CommandContext<CommandSourceStack> context) {
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

        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            if (!storageMeta.hasStoredEnchants()) {
                Utils.tell(user, "<primary>Your item has no stored enchantments.");
                return 0;
            }
            Utils.tell(user, "<primary>Your item is currently storing the following enchantments: <secondary>" + String.join("<gray>,</gray> ", meta.getEnchants().entrySet().stream()
                    .map((entry) -> "<click:suggest_command:'/ie enchant " + entry.getKey().getKey().getKey() + " 0'><hover:show_text:'<gray><i>Click to suggest the command to remove this enchantment!'>" + entry.getKey().getKey().getKey().replace("_", " ") + " " + entry.getValue() + "</hover></click>").toList()));
            Utils.tell(user, "<gray><i>Click an enchantment above to suggest the command to remove it!");
            return 1;
        }

        if (!meta.hasEnchants()) {
            Utils.tell(user, "<primary>Your item is not enchanted.");
            return 0;
        }

        Utils.tell(user, "<primary>Your item currently has the following enchantments: <secondary>" + String.join("<gray>,</gray> ", meta.getEnchants().entrySet().stream()
                .map((entry) -> "<click:suggest_command:'/ie enchant " + entry.getKey().getKey().getKey() + " 0'><hover:show_text:'<gray><i>Click to suggest the command to remove this enchantment!'>" + entry.getKey().getKey().getKey().replace("_", " ") + " " + entry.getValue() + "</hover></click>").toList()));
        Utils.tell(user, "<gray><i>Click an enchantment above to suggest the command to remove it!");

        return 1;
    }
}
