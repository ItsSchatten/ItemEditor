package com.itsschatten.itemeditor.menus;

import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.AnimatedButton;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.DynamicButton;
import com.itsschatten.yggdrasil.menus.types.StandardMenu;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.ItemCreator;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Menu responsible for editing a single FireworkEffect on a firework star.
 */
public class FireworkStarMenu extends StandardMenu {

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
        super(null);

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


        setTitle("Firework Star Effect Menu");
        setSize(45);
    }

    // Handles things on menu close.
    @Override
    public void onClose(IMenuHolder user) {
        // Sets the firework effect.
        meta.setEffect(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
        fireworkStar.setItemMeta(meta);
    }

    // Set the information for the Information item.
    @Contract(value = " -> new", pure = true)
    @Override
    public final String @NotNull [] getInfo() {
        return new String[]{
                "<red>To add custom hex colors use the command line!",
                "<gray>colors:#abcdef"
        };
    }

    // Updates the information button position to after the dye colors.
    @Override
    protected InventoryPosition getInfoButtonPosition() {
        return InventoryPosition.of(1, 8);
    }

    // Registers buttons for this menu.
    @Override
    public void makeButtons() {
        // Loops all DyeColor values and registers their buttons.
        for (int i = 0; i < DyeColor.values().length; i++) {
            final Button colorButton = makeButton(i);

            registerButtons(colorButton);
        }

        // This is an animated button, it updates after 1 second.
        // Clicking this button rewards the player with the created firework star.
        final Button starButton = new AnimatedButton() {
            @Override
            public long getUpdateTime() {
                return 20L;
            }

            @Override
            public ItemCreator createItem() {
                final ItemStack star = new ItemStack(Material.FIREWORK_STAR);
                final FireworkEffectMeta fwMeta = (FireworkEffectMeta) star.getItemMeta();
                if (!colors.isEmpty())
                    fwMeta.setEffect(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
                star.setItemMeta(fwMeta);

                return ItemCreator.of(star).name("<primary>Created Firework Star").lore(List.of("<yellow>Click <gray>to get a copy of this firework star.")).build();
            }

            @Override
            public void onClicked(@NotNull IMenuHolder user, Menu menu, ClickType click) {
                // We need colors to create the firework star, say so.
                if (colors.isEmpty()) {
                    user.tell("<red>You need to add at least one color to get this firework star!");
                    return;
                }

                final ItemStack star = new ItemStack(Material.FIREWORK_STAR);
                final FireworkEffectMeta fwMeta = (FireworkEffectMeta) star.getItemMeta();
                fwMeta.setEffect(FireworkEffect.builder().flicker(flicker).trail(trail).with(type).withColor(colors).withFade(fades).build());
                star.setItemMeta(fwMeta);

                user.getBase().getInventory().addItem(star);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(4, 4);
            }
        };

        // The shape of the firework effect.
        // This is a dynamic button, this button. It will be updated 1 second after clicking it.
        final Button typeButton = new DynamicButton() {
            // Different method name, but effectively does what onClick does.
            @Override
            public void whenClicked(IMenuHolder user, Menu menu, @NotNull ClickType click) {
                if (click.isRightClick()) {
                    // Go down the enum until 0.
                    type = FireworkEffect.Type.values()[Math.max(0, type.ordinal() - 1)];
                } else {
                    // Go up the enum until maxed.
                    type = FireworkEffect.Type.values()[Math.min(FireworkEffect.Type.values().length - 1, type.ordinal() + 1)];
                }
            }

            // Effectively createItem but used to update the ItemStack internally.
            @Override
            public ItemCreator updateStack() {
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
                        .lore(lore).build();
            }

            // This is a dynamic button, so we just use the updateStack method to create the item
            @Override
            public ItemCreator createItem() {
                return updateStack();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(3, 4);
            }
        };

        // The twinkle effect of the firework effect.
        // This is a dynamic button, this button. It will be updated 1 second after clicking it.
        final Button twinkleButton = new DynamicButton() {
            @Override
            public void whenClicked(IMenuHolder user, Menu menu, @NotNull ClickType click) {
                // Toggle the flicker value.
                flicker = !flicker;
            }

            @Override
            public ItemCreator updateStack() {
                return ItemCreator.of(Material.GLOWSTONE_DUST).name("<primary>Twinkle: <secondary>" + flicker).glow(flicker).build();
            }

            @Override
            public ItemCreator createItem() {
                return updateStack();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(3, 2);
            }
        };

        // The trail of the firework effect.
        // This is a dynamic button, this button. It will be updated 1 second after clicking it.
        final Button trailButton = new DynamicButton() {
            @Override
            public void whenClicked(IMenuHolder user, Menu menu, @NotNull ClickType click) {
                // Toggle the trail value.
                trail = !trail;
            }

            @Override
            public ItemCreator updateStack() {
                return ItemCreator.of(Material.DIAMOND).name("<primary>Trail: <secondary>" + trail).glow(trail).build();
            }

            @Override
            public ItemCreator createItem() {
                return updateStack();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(3, 6);
            }
        };

        registerButtons(typeButton, twinkleButton, trailButton, starButton);
    }

    // Utility method to quickly create a DyeColor button from an integer value, usually the ordinal from the DyeColor enum.
    private @NotNull Button makeButton(int i) {
        final DyeColor color = DyeColor.values()[i];
        final int finalI = i;

        return new DynamicButton() {
            @Contract(pure = true)
            @Override
            public ItemCreator createItem() {
                return updateStack();
            }

            @Override
            public void whenClicked(IMenuHolder user, Menu menu, @NotNull ClickType click) {
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
            }

            @Override
            public ItemCreator updateStack() {
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
                return ItemCreator.of(Material.matchMaterial(color.name() + "_DYE"))
                        .name("<#" + Integer.toHexString(color.getColor().asRGB()) + ">" + WordUtils.capitalizeFully(color.name().replace("_", " ")))
                        .lore(lore)
                        .glow(colors.contains(color.getFireworkColor())).build();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                // Set the position of the provided integer.
                return InventoryPosition.fromSlot(finalI);
            }
        };
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(2, ItemCreator.makeFillerItem(Material.GRAY_STAINED_GLASS_PANE));
        setRow(4, ItemCreator.makeFillerItem(Material.GRAY_STAINED_GLASS_PANE));
    }
}
