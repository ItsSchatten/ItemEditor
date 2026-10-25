package com.itsschatten.itemeditor.menus;

import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.impl.Button.ButtonBuilder;
import com.itsschatten.yggdrasil.menus.buttons.interfaces.IButton;
import com.itsschatten.yggdrasil.menus.types.StandardMenu;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import org.apache.commons.text.WordUtils;
import org.bukkit.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Menu responsible for editing a firework effect.
 */
public final class FireworkEffectMenu extends StandardMenu<MenuHolder> {

    // The parent menu.
    final FireworkMenu parentMenu;

    // The main colors for the effect.
    final Set<Color> colors;
    // The fade colors for the effect.
    final Set<Color> fades;

    // The shape of the effect.
    // If not editing, defaults to ball.
    FireworkEffect.Type type;

    // The trail value for this effect.
    // If not editing, defaults to false.
    boolean trail;
    // The twinkle value for this effect.
    // If not editing, defaults to false.
    boolean flicker;

    /**
     * Constructs a menu to be opened for a Player.
     *
     * @param menu   The {@link FireworkMenu} to return to.
     * @param toEdit The effect to edit, can be null.
     */
    public FireworkEffectMenu(final FireworkMenu menu, final FireworkEffect toEdit) {
        super(menu, "Firework Effect Editor", 45);

        // Set default values if 'toEdit' is 'null.'
        if (toEdit == null) {
            this.colors = new HashSet<>();
            this.fades = new HashSet<>();

            this.type = FireworkEffect.Type.BALL;

            this.trail = false;
            this.flicker = false;
        } else {
            this.colors = new HashSet<>(toEdit.getColors());
            this.fades = new HashSet<>(toEdit.getFadeColors());

            this.type = toEdit.getType();

            this.trail = toEdit.hasTrail();
            this.flicker = toEdit.hasFlicker();
        }

        this.parentMenu = menu;
    }

    // Set the information for the Information item.
    @Contract(value = " -> new", pure = true)
    @Override
    public @NotNull @Unmodifiable List<String> getInfo() {
        return List.of(
                "<red>To add custom hex colors use the command line!",
                "<secondary>/ie firework #abcdef ..."
        );
    }


    @Override
    public @Nullable ButtonBuilder<MenuHolder, ?, ?> getInfoButton() {
        return Objects.requireNonNull(super.getInfoButton()).position(InventoryPosition.of(1, 8));
    }

    // Registers buttons for this menu.
    @Override
    public @NotNull @Unmodifiable List<IButton<MenuHolder>> makeButtons() {
        // Loops all DyeColor values and registers their buttons.
        for (int i = 0; i < DyeColor.values().length; i++) {
            final IButton<MenuHolder> colorButton = makeButton(i);

            registerButtons(colorButton);
        }

        // The shape of the firework effect.
        // This is a dynamic button, this button. It will be updated 1 second after clicking it.
        final IButton<MenuHolder> typeButton = Buttons.dynamic()
                .item(() -> {
                    final List<String> lore = new ArrayList<>();

                    // If we are not at Creeper, the last in the enum, add the forward selection lore line.
                    if (type != FireworkEffect.Type.CREEPER) {
                        lore.add("<yellow>Left-Click <gray>to select: <secondary>" + FireworkEffect.Type.values()[Math.min(FireworkEffect.Type.values().length - 1, type.ordinal() + 1)]);
                    }

                    // If we are not at Ball, the first in the enum, add the backward selection lore line.
                    if (type != FireworkEffect.Type.BALL) {
                        lore.add("<yellow>Right-Click <gray>to select: <secondary>" + FireworkEffect.Type.values()[Math.max(0, type.ordinal() - 1)]);
                    }

                    return ItemCreator.of(Material.FIREWORK_STAR).name("<primary>Shape: <secondary>" + WordUtils.capitalizeFully(type.name().replace("_", " ")))
                            .lore(lore);
                })
                .onClick((holder, click) -> {
                    if (click.isRightClick()) {
                        // Go down the enum until 0.
                        type = FireworkEffect.Type.values()[Math.max(0, type.ordinal() - 1)];
                    } else {
                        // Go up the enum until maxed.
                        type = FireworkEffect.Type.values()[Math.min(FireworkEffect.Type.values().length - 1, type.ordinal() + 1)];
                    }
                })
                .position(3, 4)
                .build();
        // The twinkle effect of the firework effect.
        // This is a dynamic button, this button. It will be updated 1 second after clicking it.
        final IButton<MenuHolder> twinkleButton = Buttons.dynamic()
                .item(() -> ItemCreator.of(Material.GLOWSTONE_DUST).name("<primary>Twinkle: <secondary>" + flicker)
                        .options(ItemOptions.builder().glow(flicker).build()))
                .onClick((holder, click) -> {
                    // Toggle the flicker value.
                    flicker = !flicker;
                })
                .position(3, 2)
                .build();

        // The trail of the firework effect.
        // This is a dynamic button. It will be updated 1 second after clicking it.
        final IButton<MenuHolder> trailButton = Buttons.dynamic()
                .item(() -> ItemCreator.of(Material.DIAMOND).name("<primary>Trail: <secondary>" + trail).options(ItemOptions.builder().glow(trail).build()))
                .onClick((holder, click) -> {
                    // Toggle the flicker value.
                    trail = !trail;
                })
                .position(3, 6)
                .build();

        // Register our buttons to the menu, so they function and are automatically set in the menu.
        return List.of(typeButton, twinkleButton, trailButton);
    }

    // Utility method to quickly create a DyeColor button from an integer value, usually the ordinal from the DyeColor enum.
    private @NotNull IButton<MenuHolder> makeButton(int i) {
        final DyeColor color = DyeColor.values()[i];
        return Buttons.dynamic()
                .item(() -> {
                    // Default descriptive lore.
                    final List<String> lore = new ArrayList<>(List.of("<yellow>Left-Click<gray> to add this color.", "<yellow>Right-Click <gray>to remove this color.", "<yellow>Shift click<gray> to add the colors to the fade.", ""));

                    // Check if the 'fades' Set contains the color.
                    if (fades.contains(color.getFireworkColor())) {
                        // We do, add a lore line to this color to say it's a fade color.
                        lore.add("<dark_purple><i>Fade Color");
                    }

                    // Check if the main colors Set contains the color.
                    if (colors.contains(color.getFireworkColor())) {
                        // Add some additional information to the item.
                        lore.add("<info>This item is enchanted because it's a main effect color.</info>");
                    }

                    // Creates the dye color
                    return ItemCreator.of(Objects.requireNonNull(Material.matchMaterial(color.name() + "_DYE")))
                            .name("<#" + Integer.toHexString(color.getColor().asRGB()) + ">" + WordUtils.capitalizeFully(color.name().replace("_", " ")))
                            .lore(lore)
                            .options(ItemOptions.builder().glow(colors.contains(color.getFireworkColor())).build());
                })
                .onClick((holder, click) -> {
                    // Check if our click is a right click.
                    if (click.isRightClick()) {
                        // Check if we are modifying it with a shift.
                        if (click.isShiftClick()) {
                            // We are shifting, remove the color from our fade colors.
                            fades.remove(color.getFireworkColor());
                        } else {
                            // We are not shifting, remove the color from the main colors.
                            colors.remove(color.getFireworkColor());
                        }
                    } else {
                        // Check if we modifying it with a shift.
                        if (click.isShiftClick()) {
                            // Add the color to our fade colors.
                            fades.add(color.getFireworkColor());
                        } else {
                            // Add the color to our main colors.
                            colors.add(color.getFireworkColor());
                        }
                    }
                })
                .position(InventoryPosition.fromSlot(i)).build();
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(2, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    // We don't want to add a close button to this menu.
    @Override
    public boolean addClose() {
        return false;
    }

    @Override
    public @Nullable ButtonBuilder<MenuHolder, ?, ?> getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).position(Objects.requireNonNull(getCloseButton()).build().position());
    }

    @Override
    public void onSwitch(MenuHolder user) {
        // Check if we have colors if we don't send them a failure message.
        if (colors.isEmpty()) {
            user.tell("<red>Failed to build firework effect, didn't have any colors.");
            return;
        }

        // Adds the effect to the list of effects.
        this.parentMenu.effects.add(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
    }

    // Handles things when the menu is closed.
    @Override
    public void onClose(MenuHolder user) {
        // Open the parent menu regardless of how this menu is closed.
        Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> parentMenu.switchTo(user), 1L);

        // Check if we have colors if we don't send them a failure message.
        if (colors.isEmpty()) {
            user.tell("<red>Failed to build firework effect, didn't have any colors.");
            return;
        }

        // Adds the effect to the list of effects.
        this.parentMenu.effects.add(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
    }
}
