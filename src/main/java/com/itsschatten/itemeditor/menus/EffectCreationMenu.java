package com.itsschatten.itemeditor.menus;

import com.google.common.collect.Lists;
import com.itsschatten.yggdrasil.TimeUtils;
import com.itsschatten.yggdrasil.Utils;
import com.itsschatten.yggdrasil.anvilgui.AnvilGUI;
import com.itsschatten.yggdrasil.anvilgui.interfaces.Response;
import com.itsschatten.yggdrasil.items.ItemCreator;
import com.itsschatten.yggdrasil.items.ItemOptions;
import com.itsschatten.yggdrasil.items.manipulators.ColorManipulator;
import com.itsschatten.yggdrasil.menus.buttons.Buttons;
import com.itsschatten.yggdrasil.menus.buttons.premade.ReturnButton;
import com.itsschatten.yggdrasil.menus.types.StandardMenu;
import com.itsschatten.yggdrasil.menus.utils.IMenuHolder;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class EffectCreationMenu extends StandardMenu {

    final EffectListMenu parent;

    PotionEffectType effect = PotionEffectType.ABSORPTION;

    int amplifier = 1;
    int duration = TimeUtils.MinecraftTimeUnits.MINUTE_MS.getAsInt();

    boolean ambient = false;
    boolean particles = true;
    boolean icon = true;

    private boolean configuring = false;

    public EffectCreationMenu(EffectListMenu parent) {
        super(parent);
        this.parent = parent;

        setTitle("Create Potion Effect");
        setSize(36);
    }

    @Override
    public @Nullable ReturnButton getReturnButton() {
        return Objects.requireNonNull(super.getReturnButton()).toBuilder().position(getInventory().getRows() - 1, 0).build();
    }

    @Override
    public void postDisplay() {
        refresh();
    }

    @Override
    public void makeButtons() {
        registerButtons(
                Buttons.menuTrigger()
                        .item(ItemCreator.of(Material.POTION).display("<primary>Potion Type: <secondary>" + effect.key().asMinimalString())
                                .lore("<yellow>Click <gray>to change the effect type!")
                                .manipulator(new ColorManipulator(effect.getColor()))
                                .options(ItemOptions.HIDE_ALL_FLAGS)
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
    public void onClose(IMenuHolder user) {
        if (!isOpeningNew() && !configuring) {
            pass();
            // A tick must delay this. Otherwise, the close call will be called continuously.
            Bukkit.getScheduler().runTaskLater(Utils.getInstance(), () -> parent.switchMenu(user, this), 1L);
        }
    }

    @Override
    public void beforeReturn() {
        pass();
    }

    private void pass() {
        parent.effects.add(effect.createEffect(duration, amplifier).withIcon(icon).withAmbient(ambient).withParticles(particles));
    }

}
