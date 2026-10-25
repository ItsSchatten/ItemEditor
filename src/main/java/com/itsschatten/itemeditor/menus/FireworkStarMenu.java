package com.itsschatten.itemeditor.menus;

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
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Menu responsible for editing a single FireworkEffect on a firework star.
 */
public final class FireworkStarMenu extends StandardMenu<MenuHolder> {

    // A large part of this class (if not most of it) is a copy of FireWorkEffectMenu.

    // The firework star we are editing, it is safe to assume this is always a FIREWORK_STAR.
    final ItemStack fireworkStar;
    // The meta of fireworkStar.
    final FireworkEffectMeta meta;

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

    public FireworkStarMenu(final ItemStack fireworkStar, final @NotNull FireworkEffectMeta meta) {
        super(null, "Firework Star Effect Menu", 45);

        this.fireworkStar = fireworkStar;
        this.meta = meta;

        // If we don't have an effect, set defaults.
        if (!meta.hasEffect()) {
            this.colors = new HashSet<>();
            this.fades = new HashSet<>();

            this.type = FireworkEffect.Type.BALL;

            this.trail = false;
            this.flicker = false;
        } else {
            // We can safely assume this is not null due to the meta.hasEffect call above.
            assert meta.getEffect() != null;

            this.colors = new HashSet<>(meta.getEffect().getColors());
            this.fades = new HashSet<>(meta.getEffect().getFadeColors());

            this.type = meta.getEffect().getType();

            this.trail = meta.getEffect().hasTrail();
            this.flicker = meta.getEffect().hasFlicker();
        }
    }

    // Handles things on menu close.
    @Override
    public void onClose(MenuHolder user) {
        // Sets the firework effect.
        meta.setEffect(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
        fireworkStar.setItemMeta(meta);
    }

    // Set the information for the Information item.
    @Contract(value = " -> new", pure = true)
    @Override
    public List<String> getInfo() {
        return List.of(
                "<red>To add custom hex colors use the command line!",
                "<gray>#abcdef"
        );
    }

    @Override
    public @Nullable ButtonBuilder<MenuHolder, ?, ?> getInfoButton() {
        return Objects.requireNonNull(super.getInfoButton()).position(InventoryPosition.of(1, 8));
    }

    // Registers buttons for this menu.
    @Override
    public List<IButton<MenuHolder>> makeButtons() {
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

        // This is an animated button, it updates after 1 second.
        // Clicking this button rewards the player with the created firework star.
        final IButton<MenuHolder> starButton = Buttons.animated()
                .item(() -> {
                    final ItemStack star = new ItemStack(Material.FIREWORK_STAR);
                    final FireworkEffectMeta fwMeta = (FireworkEffectMeta) star.getItemMeta();
                    if (!colors.isEmpty())
                        fwMeta.setEffect(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
                    star.setItemMeta(fwMeta);

                    return ItemCreator.of(star).name("<primary>Created Firework Star").lore(List.of("<yellow>Click <gray>to get a copy of this firework star."));
                })
                .onClick((holder, click) -> {
                    // We need colors to create the firework star, say so.
                    if (colors.isEmpty()) {
                        holder.tell("<red>You need to add at least one color to get this firework star!");
                        return;
                    }

                    final ItemStack star = new ItemStack(Material.FIREWORK_STAR);
                    final FireworkEffectMeta fwMeta = (FireworkEffectMeta) star.getItemMeta();
                    fwMeta.setEffect(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
                    star.setItemMeta(fwMeta);

                    holder.player().getInventory().addItem(star);
                })
                .position(4, 4)
                .build();

        // Register our buttons to the menu, so they function and are automatically set in the menu.
        return List.of(starButton, typeButton, twinkleButton, trailButton);
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
        setRow(4, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }
}
