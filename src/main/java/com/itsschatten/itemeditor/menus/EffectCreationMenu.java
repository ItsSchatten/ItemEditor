package com.itsschatten.itemeditor.menus;

import com.google.common.collect.Lists;
import com.itsschatten.yggdrasil.TimeUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.anvilgui.AnvilGUI;
import com.itsschatten.yggdrasil.anvilgui.interfaces.Response;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.manipulators.ColorManipulator;
import com.itsschatten.yggdrasil.menus.buttons.Button;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.StandardMenu;
import com.itsschatten.yggdrasil.menus.utils.MenuHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class EffectCreationMenu extends StandardMenu<MenuHolder> {

    final EffectListMenu parent;

    PotionEffectType effect = PotionEffectType.ABSORPTION;

    int amplifier = 1;
    int duration = TimeUtils.MinecraftTimeUnits.MINUTE_MS.getAsInt();

    boolean ambient = false;
    boolean particles = true;
    boolean icon = true;

    private boolean configuring = false;

    public EffectCreationMenu(EffectListMenu parent) {
        super(parent, "Creation Potion Effect", 36);
        this.parent = parent;
    }

    @Override
    public @Nullable ReturnButton.ReturnButtonBuilder<MenuHolder> getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).position(rows() - 1, 0);
    }

    @Override
    public void postDisplay(MenuHolder holder) {
        refresh();
    }

    @Contract(" -> new")
    @Override
    public @NotNull @Unmodifiable List<Button<MenuHolder>> makeButtons() {
        return List.of(
                Buttons.menuTrigger()
                        .item(ItemCreator.of(Material.POTION).display("<primary>Potion Type: <secondary>" + effect.key().asMinimalString())
                                .lore("<yellow>Click <gray>to change the effect type!")
                                .manipulator(new ColorManipulator(effect.getColor()))
                                .options(ItemOptions.builder().hiddenComponent(DataComponentTypes.POTION_CONTENTS))
                                .supplier())
                        .position(1, 2)
                        .menu((holder, clickType) -> new EffectTypeListMenu(this, Lists.newArrayList(Registry.EFFECT).stream().sorted(Comparator.comparing(Keyed::key)).toList()))
                        .build(),

                Buttons.dynamic()
                        .item(() -> ItemCreator.of(ambient ? Material.LIME_DYE : Material.GRAY_DYE).name((ambient ? "<green>" : "<red>") + "Ambient")
                                .lore(
                                        "Click to " + (ambient ? "<green>enable" : "<red>disable") + "<gray> this potion's ambience.",
                                        "",
                                        "Marks this potion as an ambient effect",
                                        "as though it's from a beacon."
                                )
                        )
                        .position(1, 4)
                        .onClick((holder, menu, click) -> {
                            ambient = !ambient;
                        })
                        .build(),
                Buttons.dynamic()
                        .item(() -> ItemCreator.of(particles ? Material.LIME_DYE : Material.GRAY_DYE).name((particles ? "<green>" : "<red>") + "Particles")
                                .lore(
                                        "Click to " + (particles ? "<green>enable" : "<red>disable") + "<gray> this potion's particles.",
                                        "",
                                        "Show or hide this potion's particles while active."
                                )
                        )
                        .position(1, 5)
                        .onClick((holder, menu, click) -> {
                            particles = !particles;
                        })
                        .build(),
                Buttons.dynamic()
                        .item(() -> ItemCreator.of(icon ? Material.LIME_DYE : Material.GRAY_DYE).name((icon ? "<green>" : "<red>") + "Icon")
                                .lore(
                                        "Click to " + (icon ? "<green>enable" : "<red>disable") + "<gray> this potion's effect icon.",
                                        "",
                                        "Show or hide this potion's effect icon while active."
                                )
                        )
                        .position(1, 6)
                        .onClick((holder, menu, click) -> {
                            icon = !icon;
                        })
                        .build(),

                Buttons.button()
                        .item(ItemCreator.of(Material.CLOCK).name("<primary>Duration").lore("Duration in ticks for this effect.", "", "<secondary>Current <arrow> <yellow>" + duration + " <dark_gray>[" + TimeUtils.getMinecraftTimeClock(duration) + "]").supplier())
                        .position(2, 4)
                        .onClick((holder, menu, click) -> {
                            configuring = true;
                            AnvilGUI.builder().holder(holder).plugin(Utils.getInstance())
                                    .title("Enter Duration in Ticks")
                                    .clickHandler((integer, snapshot) -> CompletableFuture.completedFuture(Collections.singletonList(Response.openMenu(menu, holder))))
                                    .onClose(snapshot -> {
                                        configuring = false;
                                        if (!snapshot.text().isBlank()) {
                                            if (snapshot.text().equalsIgnoreCase("permanent")) {
                                                duration = -1;
                                                return;
                                            }

                                            if (NumberUtils.isNumber(snapshot.text())) {
                                                duration = Math.max(NumberUtils.toInt(snapshot.text()), -1);
                                            }
                                        }
                                    })
                                    .response(Response.openMenu(menu, holder))
                                    .open(holder);
                        })
                        .build(),
                Buttons.button()
                        .item(ItemCreator.of(Material.GUNPOWDER).name("<primary>Amplifier").lore("The amplifier for this effect.", "", "<secondary>Current <arrow> <yellow>" + amplifier).supplier())
                        .position(2, 6)
                        .onClick((holder, menu, click) -> {
                            configuring = true;
                            AnvilGUI.builder().holder(holder).plugin(Utils.getInstance())
                                    .title("Enter Amplifier")
                                    .clickHandler((integer, snapshot) -> CompletableFuture.completedFuture(Collections.singletonList(Response.openMenu(menu, holder))))
                                    .onClose(snapshot -> {
                                        configuring = false;
                                        if (!snapshot.text().isBlank() && NumberUtils.isNumber(snapshot.text())) {
                                            amplifier = Math.min(NumberUtils.toInt(snapshot.text()), 255);
                                        }
                                    })
                                    .response(Response.openMenu(menu, holder))
                                    .open(holder);
                        })
                        .build()
        );
    }

    @Override
    public void onSwitch(MenuHolder user) {
        pass();
    }

    @Override
    public void onClose(MenuHolder user) {
        if (!configuring) {
            pass();
            // A tick must delay this. Otherwise, the close call will be called continuously.
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> parent.switchMenu(user, this), 1L);
        }
    }

    @Override
    public void beforeDispose(MenuHolder holder) {
        pass();
    }

    private void pass() {
        parent.effects.add(effect.createEffect(duration, amplifier).withIcon(icon).withAmbient(ambient).withParticles(particles));
    }

}
