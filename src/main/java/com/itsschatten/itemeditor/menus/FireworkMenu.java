package com.itsschatten.itemeditor.menus;

import com.itsschatten.yggdrasil.WrapUtils;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.SkinTexture;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.items.manipulators.SkullManipulator;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.AnimatedButton;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.DynamicButton;
import com.itsschatten.yggdrasil.menus.buttons.MenuTriggerButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.InventorySize;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.apache.commons.text.WordUtils;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Menu responsible for editing, adding, and removing firework effects on a firework.
 * <br>
 * This menu does not allow custom hex colors.
 */
public final class FireworkMenu extends PaginatedMenu<MenuHolder, FireworkEffect> {

    // The actual firework item, it's meta is updated when closing this menu.
    final ItemStack firework;
    // The meta for the firework item, updated when closing and also set to the item.
    final FireworkMeta meta;

    // A list of all effects for this firework.
    final List<FireworkEffect> effects;

    /**
     * Constructs a Menu to be displayed for a Player.
     *
     * @param firework The firework {@link ItemStack}, we can assume this item is always a firework rocket.
     * @param meta     The meta of {@link #firework}.
     */
    public FireworkMenu(final ItemStack firework, final @NotNull FireworkMeta meta) {
        super(null, "Firework Effect Menu", InventorySize.FULL, new ArrayList<>(meta.getEffects()));

        this.firework = firework;
        this.meta = meta;
        this.effects = new ArrayList<>(meta.getEffects());

        setHideNav(true);
    }

    // Animates the menu.
    // In this case, we update the page items in the menu.
    @Override
    public void postDisplay(MenuHolder user) {
        updatePages(effects);
    }

    // Get a list of placeable InventoryPositions.
    // In this case, all but the last row.
    @Override
    public List<InventoryPosition> getPlaceablePositions() {
        final List<InventoryPosition> placeablePositions = new ArrayList<>();
        placeablePositions.addAll(InventoryPosition.ofRow(0));
        placeablePositions.addAll(InventoryPosition.ofRow(1));
        placeablePositions.addAll(InventoryPosition.ofRow(2));
        placeablePositions.addAll(InventoryPosition.ofRow(3));
        placeablePositions.addAll(InventoryPosition.ofRow(4));
        return placeablePositions;
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(5, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    // Makes and registers buttons for this menu.
    @Override
    public List<Button<MenuHolder>> makeButtons() {
        // The power button, updates the firework's power.
        // This button is dynamic, it will update itself after 1 second after being clicked.
        final Button<MenuHolder> powerButton = new DynamicButton<>() {
            @Override
            public void onClicked(@NotNull MenuHolder user, Menu menu, @NotNull ClickType type) {
                // If we are right-clicking, we want to decrease the power, to a minimum of 0,
                if (type.isRightClick()) {
                    meta.setPower(Math.max(meta.getPower() - 1, 0));
                } else {
                    // We aren't right-clicking, we want to increase the power to a maximum of 4.
                    meta.setPower(Math.min(4, meta.getPower() + 1));
                }
            }

            @Override
            public ItemCreator updateStack() {
                return ItemCreator.of(Material.GUNPOWDER).amount(Math.max(meta.getPower(), 1)).name("<primary>Firework Power")
                        .lore(List.of(
                                "<primary>Power: <secondary>" + meta.getPower(),
                                "",
                                "<yellow>Left-Click <gray>to increment power by one.",
                                "<yellow>Right-Click <gray>to decrement power by one."
                        )).build();
            }

            @Override
            public ItemCreator createItem() {
                return updateStack();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 1);
            }
        };

        // http://textures.minecraft.net/texture/b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f
        // Opens the FireworkEffectMenu.
        final Button<MenuHolder> addEffect = new MenuTriggerButton<>() {
            @Override
            public @NotNull ItemCreator createItem() {
                return ItemCreator.of(Material.PLAYER_HEAD)
                        .name("<primary>Add Effect")
                        .lore(List.of("Adds a new firework effect to your firework."))
                        .manipulator(new SkullManipulator(new SkinTexture(UUID.fromString("934efd8c-1026-4ced-b56b-d9ba91221bbb"), "b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f")))
                        .build();
            }

            @Contract("_, _ -> new")
            @Override
            public @NotNull Menu<MenuHolder> getMenu(MenuHolder user, ClickType type) {
                return new FireworkEffectMenu(FireworkMenu.this, null);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 0);
            }
        };

        // Creates an animated button that updates every second.
        // Clicking this button will reward the player with a firework with all the effects from this menu.
        final Button<MenuHolder> fireworkButton = new AnimatedButton<>() {

            @Override
            public ItemCreator createItem() {
                return ItemCreator.of(Material.FIREWORK_ROCKET).name("<primary>Created Firework").lore(List.of("<yellow>Click <gray>to get a copy of this firework.")).build();
            }

            @Override
            public void onClicked(@NotNull MenuHolder user, Menu menu, ClickType click) {
                final ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
                final FireworkMeta fwMeta = (FireworkMeta) firework.getItemMeta();
                fwMeta.setPower(meta.getPower());
                fwMeta.addEffects(effects);
                firework.setItemMeta(fwMeta);

                user.player().getInventory().addItem(firework);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 2);
            }
        };

        return List.of(addEffect, powerButton, fireworkButton);
    }

    // Converts a FireworkEffect to an ItemStack.
    @Override
    public ItemCreator convertToStack(FireworkEffect effect) {
        // Build a firework star.
        final ItemStack itemStack = new ItemStack(Material.FIREWORK_STAR);
        final FireworkEffectMeta meta = (FireworkEffectMeta) itemStack.getItemMeta();
        // Place nothing if meta is null.
        if (meta == null) return null;

        // Set the effect and update the meta.
        meta.setEffect(effect);
        itemStack.setItemMeta(meta);

        final List<String> lore = new ArrayList<>(Arrays.stream(getLoreFromEffect(effect).split("\n")).toList());
        lore.add("");
        lore.add("<yellow>Right-Click<gray> to remove this effect.");
        lore.add("<yellow>Left-Click<gray> to edit this effect.");

        // Return a copy of it.
        return ItemCreator.of(itemStack)
                .name("<primary>" + WordUtils.capitalizeFully(effect.getType().name().replace("_", " ")))
                .lore(lore)
                .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.FIREWORK_EXPLOSION))
                .build();
    }

    // Get the hover effect from the effect.
    @Contract(pure = true)
    private @NotNull String getLoreFromEffect(final @NotNull FireworkEffect effect) {
        // Get our colors and fades and build a comma seperated string.
        final List<String> colorList = new ArrayList<>();
        effect.getColors().forEach((color) -> {
            // Check if it's a valid dye color.
            if (DyeColor.getByFireworkColor(color) != null) {
                colorList.add(Objects.requireNonNull(DyeColor.getByFireworkColor(color)).name().toLowerCase());
            } else {
                // Not a valid dye color, go ahead and get the hex.
                final String forColor = "#" + Integer.toHexString(color.asRGB());
                colorList.add("<c:" + forColor + ">#" + Integer.toHexString(color.asRGB()) + "</c>");
            }
        });

        final List<String> fadesList = new ArrayList<>();
        effect.getFadeColors().forEach((color) -> {
            // Check if it's a valid dye color.
            if (DyeColor.getByFireworkColor(color) != null) {
                fadesList.add(Objects.requireNonNull(DyeColor.getByFireworkColor(color)).name().toLowerCase());
            } else {
                // Not a valid dye color, go ahead and get the hex.
                final String forColor = "#" + Integer.toHexString(color.asRGB());
                fadesList.add("<c:" + forColor + ">#" + Integer.toHexString(color.asRGB()) + "</c>");
            }
        });

        final String colors = "<secondary>" + String.join(", ", colorList) + "</secondary>";
        final String fades = String.join(", ", fadesList);

        return """
                <primary>Colors: {colors}
                <primary>Fades: {fades}
                <primary>Effects: <secondary>{effects}</secondary>"""
                .replace("{effects}", !effect.hasFlicker() && !effect.hasTrail() ? "<red>none" : (effect.hasFlicker() ? "flicker" + (effect.hasTrail() ? ", " : "") : "") + (effect.hasTrail() ? "trail" : ""))
                .replace("{colors}", WrapUtils.wrap(colors, 35, "|"))
                .replace("{fades}", fades.isEmpty() ? "<red>none" : WrapUtils.wrap("<secondary>" + fades + "</secondary>", 35, "|"));
    }

    // Called when clicking on a page item in the menu.
    @Override
    public void onClickPageItem(MenuHolder user, FireworkEffect effect, @NotNull ClickType click) {
        // Always removed.
        effects.remove(effect);
        removeValue(effect);

        // We are left-clicking, so we want to edit the effect.
        if (click.isLeftClick()) {
            // Creates the FireworkEffectMenu and switches to it.
            new FireworkEffectMenu(this, effect).switchMenu(user, this);
        }
    }

    // Handles things when the menu is closed.
    @Override
    public void onClose(MenuHolder user) {
        // Remove all effects from the firework.
        meta.clearEffects();

        // Re-add all effects.
        meta.addEffects(effects);
        firework.setItemMeta(meta);
    }
}
