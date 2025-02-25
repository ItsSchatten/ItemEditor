package com.itsschatten.itemeditor.menus;

import com.google.common.collect.Lists;
import com.itsschatten.itemeditor.utils.ConsumeEffectOptions;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.SkinTexture;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.items.manipulators.ColorManipulator;
import com.itsschatten.yggdrasil.items.manipulators.SkullManipulator;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EffectTypeListMenu extends PaginatedMenu<PotionEffectType> {

    protected final Set<PotionEffectType> types;

    public EffectTypeListMenu(Menu parent, List<PotionEffectType> pages) {
        super(parent, pages);

        this.types = new HashSet<>(pages);

        setSize(54);
        setTitle("Effect Type List");
        setHideNav(true);
    }

    @Override
    public void drawExtra() {
        setRow(getInventory().getRows() - 1, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    @Override
    public void makeButtons() {
        if (getParent() instanceof ConsumableEffectCreateMenu) {
            registerButtons(Buttons.menuTrigger()
                    .item(ItemCreator.of(Material.PLAYER_HEAD).name("<green>New Effect Type").lore("Add a new effect type.").manipulator(new SkullManipulator(SkinTexture.of(UUID.fromString("934efd8c-1026-4ced-b56b-d9ba91221bbb"), "b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f"))).supplier())
                    .menu((holder, clickType) -> new EffectTypeListMenu(this, Lists.newArrayList(Registry.EFFECT).stream().sorted(Comparator.comparing(Keyed::key)).toList()))
                    .position(5, 1)
                    .build());
        }
    }

    @Override
    public @Nullable ReturnButton getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).toBuilder().position(getInventory().getRows() - 1, 0).build();
    }

    @Override
    public void onOpen(IMenuHolder user) {
        cleanUpdatePages(types);
    }

    @Override
    public ItemCreator convertToStack(@NotNull PotionEffectType object) {
        return ItemCreator.of(Material.POTION)
                .display("<primary>" + object.key().asMinimalString())
                .lore("<yellow>Click</yellow> to remove.")
                .options(ItemOptions.HIDE_ALL_FLAGS)
                .manipulator(new ColorManipulator(object.getColor())).build();
    }

    @Override
    public void onClose(IMenuHolder user) {
        if (!isOpeningNew()) {
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> Objects.requireNonNull(getParent()).displayTo(user), 1L);
        }

        if (getParent() instanceof final ConsumableEffectCreateMenu consumableEffectCreateMenu) {
            consumableEffectCreateMenu.options = new ConsumeEffectOptions.RemoveStatusEffectsOptions(new ArrayList<>(types));
        }
    }

    @Override
    public void onClickPageItem(IMenuHolder user, PotionEffectType object, ClickType click) {
        if (getParent() instanceof ConsumableEffectCreateMenu) {
            types.remove(object);
            updatePages(types);
            refreshPage();
        } else if (getParent() instanceof EffectTypeListMenu type) {
            type.types.add(object);
            getParent().switchMenu(user, this);
        } else if (getParent() instanceof EffectCreationMenu menu) {
            menu.effect = object;
            getParent().switchMenu(user, this);
        }
    }
}
