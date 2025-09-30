package com.itsschatten.itemeditor.commands.subcommands;

import com.itsschatten.itemeditor.utils.ItemValidator;
import com.itsschatten.yggdrasil.StringUtil;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.commands.BrigadierCommand;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Weapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class WeaponSubCommand extends BrigadierCommand {

    // Description/Usage message for this sub command.
    @Override
    public @NotNull Component descriptionComponent() {
        return StringUtil.color("<primary>/ie weapon <secondary><amount></secondary>").hoverEvent(StringUtil.color("""
                        <primary>Adjusts values for the weapon component.
                        \s
                        ◼ <secondary><amount><required></secondary> The amount to update the stack.""").asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/ie amount "));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("weapon")
                .then(literal("-view").executes(this::handleView))
                .then(literal("damageperattack")
                        .then(literal("-clear")
                                .executes(context -> updateWeapon(context, (weapon) -> {
                                    Utils.tell(context, "<primary>Your weapon's damage per attack has been reset!");
                                    return weapon.itemDamagePerAttack(1).build();
                                }))
                        )
                        .then(argument("damage", IntegerArgumentType.integer(0))
                                .executes(context -> updateWeapon(context, (weapon) -> {
                                    final int damage = IntegerArgumentType.getInteger(context, "damage");

                                    Utils.tell(context, "<primary>Your weapon will now take <secondary>" + damage + " damage</secondary> per attack!");
                                    return weapon.itemDamagePerAttack(damage).build();
                                }))
                        )
                )
                .then(literal("disableblockfor")
                        .then(argument("seconds", FloatArgumentType.floatArg(0.0f))
                                .executes(context -> updateWeapon(context, (weapon) -> {
                                    final float seconds = FloatArgumentType.getFloat(context, "seconds");

                                    Utils.tell(context, "<primary>Your weapon will now disable blocking for <secondary>" + seconds + " seconds</secondary>!");
                                    return weapon.disableBlockingForSeconds(seconds).build();
                                }))
                        )
                );
    }

    private Weapon.@NotNull Builder toBuilder(final @NotNull Weapon weapon) {
        return Weapon.weapon().disableBlockingForSeconds(weapon.disableBlockingForSeconds()).itemDamagePerAttack(weapon.itemDamagePerAttack());
    }

    private int updateWeapon(final @NotNull CommandContext<CommandSourceStack> context, final Function<Weapon.Builder, Weapon> function) {
        final Player user = (Player) context.getSource().getSender();
        // Get the item stack in the user's main hand.
        final ItemStack stack = user.getInventory().getItemInMainHand();
        if (ItemValidator.isInvalid(stack)) {
            Utils.tell(user, "<red>You need to be holding an item in your hand.");
            return 0;
        }

        stack.setData(DataComponentTypes.WEAPON, function.apply(!stack.hasData(DataComponentTypes.WEAPON) ? Weapon.weapon() : toBuilder(stack.getData(DataComponentTypes.WEAPON))));
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

        if (!stack.hasData(DataComponentTypes.WEAPON)) {
            Utils.tell(context, "<primary>Your item is not a weapon!");
            return 1;
        }

        final Weapon weapon = stack.getData(DataComponentTypes.WEAPON);

        Utils.tell(context, """
                <primary>Your Weapon Values:
                <secondary>Damage Per Attack: <value>{damage}</value>
                <secondary>Disables Block For (seconds): <value>{seconds}</value> second(s)"""
                .replace("{damage}", String.valueOf(weapon.itemDamagePerAttack()))
                .replace("{seconds}", String.valueOf(weapon.disableBlockingForSeconds()))
        );
        return 1;
    }

}
