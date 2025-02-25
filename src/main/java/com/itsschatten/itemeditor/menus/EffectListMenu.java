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
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.PaginatedMenu;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EffectListMenu extends PaginatedMenu<PotionEffect> {

    final List<PotionEffect> effects;

    float probability = 1;

    private boolean configuring = false;

    public EffectListMenu(Menu parent, List<PotionEffect> pages) {
        super(parent, pages);

        effects = new ArrayList<>(pages);

        setSize(54);
        setTitle("Effect List");
        setHideNav(true);
    }

    @Override
    public void postDisplay() {
        refresh();
    }

    @Override
    public void onOpen(IMenuHolder user) {
        cleanUpdatePages(effects);
    }

    @Override
    public void drawExtra() {
        setRow(getInventory().getRows() - 1, UtilityItems.makeFiller(Material.GRAY_STAINED_GLASS_PANE));
    }

    @Override
    public void makeButtons() {
        registerButtons(Buttons.menuTrigger()
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
    public @Nullable ReturnButton getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).toBuilder().position(getInventory().getRows() - 1, 0).build();
    }

    @Override
    public ItemCreator convertToStack(@NotNull PotionEffect object) {
        return ItemCreator.of(Material.POTION).display("<info>Info Legend: Has Particles, Has Icon, Is Ambient")
                .lore(
                        "",
                        "<secondary>" + StringHelper.potionEffectToString(object)
                )
                .options(ItemOptions.HIDE_ALL_FLAGS)
                .manipulator(new ColorManipulator(object.getType().getColor())).build();
    }

    @Override
    public void onClickPageItem(IMenuHolder user, PotionEffect object, ClickType click) {
        effects.remove(object);
        updatePages(effects);
        refreshPage();
    }

    @Override
    public void onClose(IMenuHolder user) {
        if ((!isOpeningNew() && !configuring) && getParent() != null) {
            // A tick must delay this. Otherwise, the close call will be called continuously.
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> getParent().switchMenu(user, this), 1L);
        }

        if ((!configuring) && getParent() instanceof ConsumableEffectCreateMenu menu) {
            menu.options = new ConsumeEffectOptions.ApplyStatusEffectsOptions(effects, probability);
        }
    }
}
