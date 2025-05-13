package com.itsschatten.itemeditor.menus;

import com.itsschatten.itemeditor.utils.ConsumeEffectOptions;
import com.itsschatten.itemeditor.utils.StringHelper;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.anvilgui.AnvilGUI;
import com.itsschatten.yggdrasil.anvilgui.interfaces.Response;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.SkinTexture;
import com.itsschatten.yggdrasil.items.UtilityItems;
import com.itsschatten.yggdrasil.items.manipulators.ColorManipulator;
import com.itsschatten.yggdrasil.items.manipulators.SkullManipulator;
import com.itsschatten.yggdrasil.menus.Menu;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.utils.InventorySize;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class EffectListMenu extends PaginatedMenu<MenuHolder, PotionEffect> {

    final List<PotionEffect> effects;

    float probability = 1;

    private boolean configuring = false;

    public EffectListMenu(Menu<MenuHolder> parent, List<PotionEffect> pages) {
        super(parent, "Effect List", InventorySize.FULL, pages);

        effects = new ArrayList<>(pages);
        setHideNav(true);
    }

    @Override
    public void postDisplay(MenuHolder holder) {
        refresh();
    }

    @Override
    public void onOpen(MenuHolder holder) {
        cleanUpdatePages(effects);
    }

    @Override
    public void drawExtra() {
        setRow(rows() - 1, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    @Contract(" -> new")
    @Override
    public @NotNull @Unmodifiable List<Button<MenuHolder>> makeButtons() {
        return List.of(Buttons.menuTrigger()
                        .item(ItemCreator.of(Material.PLAYER_HEAD).name("<green>New Effect").lore("Add a new effect.").manipulator(new SkullManipulator(SkinTexture.of(UUID.fromString("934efd8c-1026-4ced-b56b-d9ba91221bbb"), "b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f"))).supplier())
                        .menu((holder, clickType) -> new EffectCreationMenu(this))
                        .position(5, 1)
                        .build(),
                Buttons.button()
                        .item(ItemCreator.of(Material.COMPARATOR).name("<primary>Probability").lore("The probability for the effects to apply.", "", "<secondary>Current <arrow> <yellow>" + (probability * 100) + "%").supplier())
                        .position(5, 2)
                        .onClick((holder, menu, click) -> {
                            configuring = true;
                            AnvilGUI.builder().holder(holder).plugin(Utils.getInstance())
                                    .title("Enter Amplifier")
                                    .clickHandler((integer, snapshot) -> CompletableFuture.completedFuture(Collections.singletonList(Response.openMenu(menu, holder))))
                                    .onClose(snapshot -> {
                                        configuring = false;
                                        if (!snapshot.text().isBlank() && NumberUtils.isNumber(snapshot.text())) {
                                            probability = Math.max(Math.min(NumberUtils.toFloat(snapshot.text()) / 100F, 1), 0);
                                        }
                                    })
                                    .response(Response.openMenu(menu, holder))
                                    .open(holder);
                        })
                        .build());
    }

    @Override
    public @Nullable ReturnButton.ReturnButtonBuilder<MenuHolder> getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).position(rows() - 1, 0);
    }

    @Override
    public ItemCreator convertToStack(@NotNull PotionEffect object) {
        return ItemCreator.of(Material.POTION).display("<info>Info Legend: Has Particles, Has Icon, Is Ambient")
                .lore(
                        "",
                        "<secondary>" + StringHelper.potionEffectToString(object)
                )
                .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.POTION_CONTENTS))
                .manipulator(new ColorManipulator(object.getType().getColor())).build();
    }

    @Override
    public void onClickPageItem(MenuHolder user, PotionEffect object, ClickType click) {
        effects.remove(object);
        removeValue(object);
    }

    @Override
    public void onSwitch(MenuHolder user) {
        if ((!configuring) && getParent() instanceof ConsumableEffectCreateMenu menu) {
            menu.options = new ConsumeEffectOptions.ApplyStatusEffectsOptions(effects, probability);
        }
    }

    @Override
    public void onClose(MenuHolder user) {
        if (!configuring && getParent() != null) {
            // A tick must delay this. Otherwise, the close call will be called continuously.
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> getParent().switchMenu(user, this), 1L);
        }

        if ((!configuring) && getParent() instanceof ConsumableEffectCreateMenu menu) {
            menu.options = new ConsumeEffectOptions.ApplyStatusEffectsOptions(effects, probability);
        }
    }
}
