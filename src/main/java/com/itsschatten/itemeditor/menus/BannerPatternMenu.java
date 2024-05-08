package com.itsschatten.itemeditor.menus;

import com.itsschatten.itemeditor.utils.PatternRecord;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.AnimatedButton;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.types.PagedMenu;
import com.itsschatten.yggdrasil.menus.types.interfaces.Animated;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.ItemCreator;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Registry;
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

/**
 * Menu that handles the editing of a banner pattern.
 */
public class BannerPatternMenu extends PagedMenu<PatternType> implements Animated {

    // The "parent" menu.
    final BannerMenu menu;
    // The previous pattern, used when editing a pattern.
    final PatternRecord previous;

    // The selected banner pattern.
    PatternType selected;
    // The color of the pattern. If null, defaults to black.
    DyeColor dyeColor;

    /**
     * Constructs a new Menu to be opened for a player.
     *
     * @param menu   The {@link BannerMenu} instance to use as the parent, after this menu is closed return to this menu.
     * @param record A {@link PatternRecord} if we are editing an already "assigned" pattern.
     */
    public BannerPatternMenu(final BannerMenu menu, final @Nullable PatternRecord record) {
        // We filter GUSTER and FLOW as they are experimental banner patterns.
        super(menu, Registry.BANNER_PATTERN.stream().filter(pattern -> pattern != PatternType.GUSTER && pattern != PatternType.FLOW).toList());

        this.menu = menu;
        this.previous = record;

        this.selected = record == null ? null : record.fullPattern().getPattern();
        this.dyeColor = record == null ? DyeColor.BLACK : record.fullPattern().getColor();

        setSize(45);
        setTitle("Banner Pattern Selection");
        setRemoveNavIfCantGo(true);
    }

    // We don't want to add a close button, just the return one.
    // Closing this menu will also just return the player to the BannerMenu instance.
    @Override
    public boolean addClose() {
        return false;
    }

    // Sets the menu position of the return button to where a close button will be by default.
    @Override
    protected InventoryPosition getReturnButtonPosition() {
        return getCloseButtonPosition();
    }

    // Utility method to quickly create a DyeColor button from an integer value, usually the ordinal from the DyeColor enum.
    private @NotNull Button makeButton(int i) {
        final DyeColor color = DyeColor.values()[i];
        final int finalI = i;

        // This is an animated button, this button updates every second.
        return new AnimatedButton() {
            @Override
            public long getUpdateTime() {
                return 20L;
            }

            @Override
            public void onClicked(IMenuHolder user, Menu menu, ClickType type) {
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
                        .glow(color.equals(dyeColor)).build();
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                // Set the slot with the provided integer and then add 27 to it.
                return InventoryPosition.fromSlot(finalI + (9 * 3));
            }
        };
    }

    // Makes and registers the dye color buttons.
    @Override
    public void makeButtons() {
        for (int i = 0; i < DyeColor.values().length; i++) {
            final Button colorButton = makeButton(i);
            registerButtons(colorButton);
        }
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(2, ItemCreator.makeFillerItem(Material.GRAY_STAINED_GLASS_PANE));
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
        final String bannerText = pattern.key().asString();

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
                .lore(lore).build();
    }

    // If we click a page item (a banner pattern), set our selected pattern to it.
    @Override
    public void onClickPageItem(IMenuHolder user, PatternType pattern, ClickType click) {
        this.selected = pattern;
    }

    // Handles things on menu close.
    @Override
    public void onClose(IMenuHolder user) {
        // We aren't opening a new menu, so open our parent.
        if (!isOpeningNew()) {
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> menu.switchMenu(user, this), 1L);
        }

        // We have no selected, ignore it and send a message to the player, so they know no new pattern was added.
        if (selected == null) {
            user.tell("<red>Failed to add a new pattern. You never selected a pattern type!");
            return;
        }

        // Update the list based on the previous PatternRecord. If we have one, update the list location with our new pattern.
        // Otherwise, add the pattern to the end of the list.
        if (previous != null) {
            this.menu.patterns.set(previous.location(), new PatternRecord(previous.location(), new Pattern(dyeColor, selected)));
        } else {
            this.menu.patterns.add(new PatternRecord(this.menu.patterns.size(), new Pattern(dyeColor, selected)));
        }

    }

    // Positions our page counter item.
    @Override
    public InventoryPosition getCounterPosition() {
        return InventoryPosition.of(2, 4);
    }

    // Positions our page "last page" item.
    @Override
    public InventoryPosition getBackPosition() {
        return InventoryPosition.of(2, 3);
    }

    // Positions our page "next page" item.
    @Override
    public InventoryPosition getNextPosition() {
        return InventoryPosition.of(2, 5);
    }

    // Set a list of placeable positions in the menu, in this case the first 2 rows in the menu.
    @Override
    public List<InventoryPosition> getPlaceablePositions() {
        final List<InventoryPosition> placeablePositions = new ArrayList<>();
        placeablePositions.addAll(InventoryPosition.ofRow(0));
        placeablePositions.addAll(InventoryPosition.ofRow(1));

        return placeablePositions;
    }

    // Refresh this page ever second, this only updates the page items and does not touch the other buttons.
    @Override
    public void animate() {
        refreshPage();
    }
}
