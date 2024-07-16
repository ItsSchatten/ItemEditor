package com.itsschatten.itemeditor.menus;

import com.itsschatten.itemeditor.utils.PatternRecord;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.AnimatedButton;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.MenuTriggerButton;
import com.itsschatten.yggdrasil.menus.types.PagedMenu;
import com.itsschatten.yggdrasil.menus.types.interfaces.Animated;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.ItemCreator;
import org.apache.commons.lang.WordUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menu responsible for adding, removing, and editing of Banner's patterns.
 */
public class BannerMenu extends PagedMenu<PatternRecord> implements Animated {

    // The list of patterns for the banner.
    final List<PatternRecord> patterns;

    // The actual banner item, we can always assume this item is a banner item due to how this menu is accessed.
    // See the BannerSubCommand.java class for how that works.
    final ItemStack banner;
    // The meta of the 'banner' ItemStack.
    final BannerMeta meta;

    /**
     * Constructs a new Menu to be opened by a player.
     *
     * @param banner The {@link ItemStack banner item}.
     * @param meta   The {@link BannerMeta item meta} of the banner item.
     */
    public BannerMenu(final ItemStack banner, final @NotNull BannerMeta meta) {
        super(null, PatternRecord.convertToList(meta.getPatterns()));

        this.patterns = PatternRecord.convertToList(meta.getPatterns());

        this.banner = banner;
        this.meta = meta;

        setSize(54);
        setTitle("Banner Editor");
        setRemoveNavIfCantGo(true);
    }

    // Updates the list of items to page, and refreshes the items.
    @Override
    public void animate() {
        cleanUpdatePages(patterns);
    }

    // Creates the buttons for the menu.
    @Override
    public void makeButtons() {
        // Button that will open the BannerPatternMenu.
        // http://textures.minecraft.net/texture/b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f
        final Button addPatternButton = new MenuTriggerButton() {
            @Override
            public @NotNull ItemCreator createItem() {
                return ItemCreator.of(Material.PLAYER_HEAD)
                        .name("<primary>Add Pattern")
                        .lore(List.of("Adds a new banner pattern to your banner."))
                        .build()
                        // UUID is set here to prevent the head flashing if the menu was animated.
                        // Flashing is likely caused by the client caching a brand-new texture file every time because of a new UUID/name.
                        .setSkull(UUID.fromString("934efd8c-1026-4ced-b56b-d9ba91221bbb"), "", "https://textures.minecraft.net/texture/b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f", PlayerTextures.SkinModel.CLASSIC);
            }

            @Contract("_, _ -> new")
            @Override
            public @NotNull Menu getMenu(IMenuHolder user, ClickType type) {
                return new BannerPatternMenu(BannerMenu.this, null);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 0);
            }
        };

        // The Banner Button, clicking this will reward the player with a copy of the banner they are creating.
        final Button bannerButton = new AnimatedButton() {
            @Override
            public long getUpdateTime() {
                return 20L;
            }

            @Override
            public ItemCreator createItem() {
                final ItemStack bannerInner = new ItemStack(banner.getType());
                final BannerMeta bannerMeta = (BannerMeta) bannerInner.getItemMeta();
                bannerMeta.setPatterns(patterns.stream().map(PatternRecord::fullPattern).toList());
                bannerInner.setItemMeta(bannerMeta);

                return ItemCreator.of(bannerInner).name("<primary>Created Banner").lore(List.of("<yellow>Click <gray>to get a copy of this banner.")).build();
            }

            @Override
            public void onClicked(@NotNull IMenuHolder user, Menu menu, ClickType click) {
                final ItemStack bannerInner = new ItemStack(banner.getType());
                final BannerMeta bannerMeta = (BannerMeta) bannerInner.getItemMeta();
                bannerMeta.setPatterns(patterns.stream().map(PatternRecord::fullPattern).toList());
                bannerInner.setItemMeta(bannerMeta);

                user.getBase().getInventory().addItem(bannerInner);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 2);
            }
        };

        // Registers the buttons to this menu.
        registerButtons(bannerButton, addPatternButton);
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(5, ItemCreator.makeFillerItem(Material.GRAY_STAINED_GLASS_PANE));
    }

    // Handles this when closing this menu.
    @Override
    public void onClose(IMenuHolder user) {
        // Detect if we are switching menus.
        // If we are not switching, go ahead and update the banner's meta.
        if (!this.isOpeningNew()) {
            meta.setPatterns(patterns.stream().map(PatternRecord::fullPattern).toList());
            banner.setItemMeta(meta);
        }
    }

    // Get the placeable positions for the page items.
    // In this menu, we allow all but the bottom row.
    @Override
    public List<InventoryPosition> getPlaceablePositions() {
        final List<InventoryPosition> positions = new ArrayList<>();
        positions.addAll(InventoryPosition.ofRow(0));
        positions.addAll(InventoryPosition.ofRow(1));
        positions.addAll(InventoryPosition.ofRow(2));
        positions.addAll(InventoryPosition.ofRow(3));
        positions.addAll(InventoryPosition.ofRow(4));
        return positions;
    }

    // Converts a PatternRecord to an ItemStack for the menu.
    @Override
    public ItemCreator convertToStack(@NotNull PatternRecord object) {
        // Depending on the pattern's dye color, use a black or white banner.
        final ItemStack convertBanner = new ItemStack(object.fullPattern().getColor().equals(DyeColor.WHITE) ? Material.BLACK_BANNER : Material.WHITE_BANNER);
        final BannerMeta bannerMeta = (BannerMeta) convertBanner.getItemMeta();
        bannerMeta.addPattern(object.fullPattern());
        convertBanner.setItemMeta(bannerMeta);

        final String bannerText = object.fullPattern().getPattern().key().asString();

        return ItemCreator.of(convertBanner).name("<primary>" + WordUtils.capitalizeFully(bannerText.substring(bannerText.indexOf(":") + 1).replace("_", " ")))
                .lore(List.of("<info>Full key: " + bannerText, "",
                        "<yellow>Left-Click <gray>to edit this pattern.",
                        "<yellow>Right-Click <gray> to remove this pattern.")).build();
    }

    // Handles clicking a page item.
    @Override
    public void onClickPageItem(IMenuHolder user, @NotNull PatternRecord pattern, @NotNull ClickType click) {
        // If we are right-clicking, remove the pattern from the list.
        if (click.isRightClick()) {
            patterns.remove(pattern.location());
        } else {
            // Not right-clicking, we want to edit the pattern.
            // We can safely assume that a record, click, and user is not null.
            // This is due to how the library handles it, it should never reach this method any of those values are null.
            new BannerPatternMenu(this, pattern).switchMenu(user, this);
        }
    }
}
