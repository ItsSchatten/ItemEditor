package com.itsschatten.itemeditor.menus;

import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.AnimatedButton;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.premade.NavigationButton;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.types.interfaces.Animated;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.PaperRegistryAccess;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Menu that handles the editing of a banner pattern.
 */
public final class BannerPatternMenu extends PaginatedMenu<MenuHolder, PatternType> implements Animated {

    // The "parent" menu.
    final BannerMenu menu;
    // The previous pattern, used when editing a pattern.
    final Pattern previous;

    // The selected banner pattern.
    PatternType selected;
    // The color of the pattern. If null, defaults to black.
    DyeColor dyeColor;

    /**
     * Constructs a new Menu to be opened for a player.
     *
     * @param menu   The {@link BannerMenu} instance to use as the parent, after this menu is closed return to this menu.
     * @param record The pattern we are editing.
     */
    public BannerPatternMenu(final BannerMenu menu, final @Nullable Pattern record) {
        super(menu, "Banner Pattern Selection", 45, PaperRegistryAccess.instance().getRegistry(RegistryKey.BANNER_PATTERN).stream().toList());

        this.menu = menu;
        this.previous = record;

        this.selected = record == null ? null : record.getPattern();
        this.dyeColor = record == null ? DyeColor.BLACK : record.getColor();

        setHideNav(true);
    }

    // We don't want to add a close button, just the return one.
    // Closing this menu will also just return the player to the BannerMenu instance.
    @Override
    public boolean addClose() {
        return false;
    }

    // Sets the menu position of the return button to where a close button will be by default.
    @Override
    public @Nullable ReturnButton.ReturnButtonBuilder<MenuHolder> getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).position(Objects.requireNonNull(getCloseButton()).build().getPosition());
    }

    // Utility method to quickly create a DyeColor button from an integer value, usually the ordinal from the DyeColor enum.
    private @NotNull Button<MenuHolder> makeButton(final int i) {
        final DyeColor color = DyeColor.values()[i];

        // This is an animated button, this button updates every second.
        return new AnimatedButton<>() {
            @Override
            public void onClicked(MenuHolder user, Menu<MenuHolder> menu, ClickType type) {
                // Set our selected dye color to the color found based on the integer.
                BannerPatternMenu.this.dyeColor = color;
            }

            @Contract(pure = true)
            @Override
            public ItemCreator createItem() {
                // Creates the item based on the color name.
                return ItemCreator.of(Material.matchMaterial(color.name() + "_DYE"))
                        .name("<#" + Integer.toHexString(color.getColor().asRGB()) + ">" + WordUtils.capitalizeFully(color.name().replace("_", " ")))
                        .lore(List.of("<yellow>Left-Click<gray> to use this color."))
                        .options(ItemOptions.builder().glow(color.equals(dyeColor)).build()).build();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                // Set the slot with the provided integer and then add 27 to it.
                return InventoryPosition.fromSlot(i + (9 * 3));
            }
        };
    }

    // Makes and registers the dye color buttons.
    @Override
    public @NotNull List<Button<MenuHolder>> makeButtons() {
        final List<Button<MenuHolder>> buttons = new ArrayList<>();
        for (int i = 0; i < DyeColor.values().length; i++) {
            final Button<MenuHolder> colorButton = makeButton(i);
            buttons.add(colorButton);
        }
        return buttons;
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(2, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    // Method that converts the PatternType to an item.
    @Override
    public ItemCreator convertToStack(PatternType pattern) {
        // Make an ItemStack based on the pattern.
        // If the pattern is selected, we make the banner purple or pink, depending on the dye color.
        // If not selected, we make the banner black or white, depending on the dye color.
        final ItemStack convertBanner = new ItemStack(pattern == selected ?
                (dyeColor.equals(DyeColor.PURPLE) ? Material.PINK_BANNER : Material.PURPLE_BANNER) :
                (dyeColor.equals(DyeColor.WHITE) ? Material.BLACK_BANNER : Material.WHITE_BANNER));
        final BannerMeta bannerMeta = (BannerMeta) convertBanner.getItemMeta();
        // Add the pattern so we can visualize it.
        bannerMeta.addPattern(new Pattern(dyeColor, pattern));
        convertBanner.setItemMeta(bannerMeta);

        // Return the pattern's key as it's string representation.
        final String bannerText = Objects.requireNonNull(RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKey(pattern)).asString();

        // Default lore, give description of what to do and the full namespaced key.
        final List<String> lore = new ArrayList<>(List.of("<info>Full key: " + bannerText, "",
                "<yellow>Click <gray>to select this pattern."));

        // If our pattern is selected, we want to add additional lore explaining why things are colored.
        if (pattern == selected) {
            lore.add("");
            lore.add("<dark_purple>This item is colored because it's your");
            lore.add("<dark_purple>current pattern selection!");
        }

        // Finally, creates the item.
        return ItemCreator.of(convertBanner)
                .name("<primary>" + WordUtils.capitalizeFully(bannerText.substring(bannerText.indexOf(":") + 1).replace("_", " ")))
                .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.BANNER_PATTERNS).build())
                .lore(lore).build();
    }

    // If we click a page item (a banner pattern), set our selected pattern to it.
    @Override
    public void onClickPageItem(MenuHolder user, PatternType pattern, ClickType click) {
        this.selected = pattern;
    }

    @Override
    public void onSwitch(MenuHolder user) {
        // We have no selected, ignore it and send a message to the player, so they know no new pattern was added.
        if (selected == null) {
            user.tell("<red>Failed to add a new pattern. You never selected a pattern type!");
            return;
        }

        // Update the list based on the previous PatternRecord. If we have one, update the list location with our new pattern.
        // Otherwise, add the pattern to the end of the list.
        if (previous != null) {
            this.menu.patterns.set(this.menu.patterns.indexOf(previous), new Pattern(dyeColor, selected));
        } else {
            this.menu.patterns.add(new Pattern(dyeColor, selected));
        }
    }

    // Handles things on menu close.
    @Override
    public void onClose(MenuHolder user) {
        // We aren't opening a new menu, so open our parent.
        Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> menu.switchMenu(user, this), 1L);

        // We have no selected, ignore it and send a message to the player, so they know no new pattern was added.
        if (selected == null) {
            user.tell("<red>Failed to add a new pattern. You never selected a pattern type!");
            return;
        }

        // Update the list based on the previous PatternRecord. If we have one, update the list location with our new pattern.
        // Otherwise, add the pattern to the end of the list.
        if (previous != null) {
            this.menu.patterns.set(this.menu.patterns.indexOf(previous), new Pattern(dyeColor, selected));
        } else {
            this.menu.patterns.add(new Pattern(dyeColor, selected));
        }

    }

    @Override
    public NavigationButton.NavigationButtonBuilder<MenuHolder> getCounterButton() {
        return Objects.requireNonNull(super.getCounterButton()).position(InventoryPosition.of(2, 4));
    }

    @Override
    public NavigationButton.NavigationButtonBuilder<MenuHolder> getPreviousButton() {
        return Objects.requireNonNull(super.getPreviousButton()).position(InventoryPosition.of(2, 3));
    }

    @Override
    public NavigationButton.NavigationButtonBuilder<MenuHolder> getNextButton() {
        return Objects.requireNonNull(super.getNextButton()).position(InventoryPosition.of(2, 5));
    }

    // Set a list of placeable positions in the menu, in this case the first 2 rows in the menu.
    @Override
    public @NotNull List<InventoryPosition> getPlaceablePositions() {
        final List<InventoryPosition> placeablePositions = new ArrayList<>();
        placeablePositions.addAll(InventoryPosition.ofRow(0));
        placeablePositions.addAll(InventoryPosition.ofRow(1));

        return placeablePositions;
    }

    // Refresh this page ever second, this only updates the page items and does not touch the other buttons.
    @Override
    public void animate() {
        refresh();
    }
}
