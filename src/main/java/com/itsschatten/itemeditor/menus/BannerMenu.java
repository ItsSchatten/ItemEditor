package com.itsschatten.itemeditor.menus;

import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.SkinTexture;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.items.manipulators.SkullManipulator;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.AnimatedButton;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.MenuTriggerButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.types.interfaces.Animated;
import com.itsschatten.yggdrasil.menus.utils.InventoryPosition;
import com.itsschatten.yggdrasil.menus.utils.InventorySize;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.apache.commons.text.WordUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Menu responsible for adding, removing, and editing of Banner's patterns.
 */
public final class BannerMenu extends PaginatedMenu<MenuHolder, Pattern> implements Animated {

    // The list of patterns for the banner.
    final List<Pattern> patterns;

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
        super(null, "Banner Editor", InventorySize.FULL, meta.getPatterns());

        this.patterns = meta.getPatterns();

        this.banner = banner;
        this.meta = meta;

        setHideNav(true);
    }

    // Updates the list of items to page, and refreshes the items.
    @Override
    public void animate() {
        updatePages(patterns);
    }

    // Creates the buttons for the menu.
    @Override
    public @NotNull @Unmodifiable List<Button<MenuHolder>> makeButtons() {
        // Button that will open the BannerPatternMenu.
        // http://textures.minecraft.net/texture/b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f
        final Button<MenuHolder> addPatternButton = new MenuTriggerButton<>() {
            @Override
            public @NotNull ItemCreator createItem() {
                return ItemCreator.of(Material.PLAYER_HEAD)
                        .name("<primary>Add Pattern")
                        .manipulator(new SkullManipulator(new SkinTexture(UUID.fromString("934efd8c-1026-4ced-b56b-d9ba91221bbb"), "b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f")))
                        .lore("Adds a new banner pattern to your banner.")
                        .build();
            }

            @Contract("_, _ -> new")
            @Override
            public @NotNull Menu<MenuHolder> getMenu(MenuHolder user, ClickType type) {
                return new BannerPatternMenu(BannerMenu.this, null);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 0);
            }
        };

        // The Banner Button, clicking this will reward the player with a copy of the banner they are creating.
        final Button<MenuHolder> bannerButton = new AnimatedButton<>() {

            @Override
            public ItemCreator createItem() {
                final ItemStack bannerInner = new ItemStack(banner.getType());
                final BannerMeta bannerMeta = (BannerMeta) bannerInner.getItemMeta();
                bannerMeta.setPatterns(patterns);
                bannerInner.setItemMeta(bannerMeta);

                return ItemCreator.of(bannerInner).name("<primary>Created Banner")
                        .lore(List.of("<yellow>Click <gray>to get a copy of this banner."))
                        .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.BANNER_PATTERNS).build()).build();
            }

            @Override
            public void onClicked(@NotNull MenuHolder user, Menu menu, ClickType click) {
                final ItemStack bannerInner = new ItemStack(banner.getType());
                final BannerMeta bannerMeta = (BannerMeta) bannerInner.getItemMeta();
                bannerMeta.setPatterns(patterns);
                bannerInner.setItemMeta(bannerMeta);

                user.player().getInventory().addItem(bannerInner);
            }

            @Contract(" -> new")
            @Override
            public @NotNull InventoryPosition getPosition() {
                return InventoryPosition.of(5, 2);
            }
        };

        // Registers the buttons to this menu.
        return List.of(bannerButton, addPatternButton);
    }

    // Draws non-functional items to the menu.
    @Override
    public void drawExtra() {
        setRow(5, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    // Handles this when closing this menu.
    @Override
    public void onClose(MenuHolder user) {
        meta.setPatterns(patterns);
        banner.setItemMeta(meta);
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
    public ItemCreator convertToStack(@NotNull Pattern object) {
        // Depending on the pattern's dye color, use a black or white banner.
        final ItemStack convertBanner = new ItemStack(object.getColor().equals(DyeColor.WHITE) ? Material.BLACK_BANNER : Material.WHITE_BANNER);
        final BannerMeta bannerMeta = (BannerMeta) convertBanner.getItemMeta();
        bannerMeta.addPattern(object);
        convertBanner.setItemMeta(bannerMeta);

        final String bannerText = Objects.requireNonNull(RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKey(object.getPattern())).asString();

        return ItemCreator.of(convertBanner).name("<primary>" + WordUtils.capitalizeFully(bannerText.substring(bannerText.indexOf(":") + 1).replace("_", " ")))
                .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.BANNER_PATTERNS).build())
                .lore(List.of("<info>Full key: " + bannerText, "",
                        "<yellow>Left-Click <gray>to edit this pattern.",
                        "<yellow>Right-Click <gray> to remove this pattern.")).build();
    }

    // Handles clicking a page item.
    @Override
    public void onClickPageItem(MenuHolder user, @NotNull Pattern pattern, @NotNull ClickType click) {
        // If we are right-clicking, remove the pattern from the list.
        if (click.isRightClick()) {
            patterns.remove(pattern);
            removeValue(pattern);
        } else {
            // Not right-clicking, we want to edit the pattern.
            // We can safely assume that a record, click, and user is not null.
            // This is due to how the library handles it, it should never reach this method any of those values are null.
            new BannerPatternMenu(this, pattern).switchMenu(user, this);
        }
    }
}
