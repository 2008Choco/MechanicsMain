package me.deecaad.weaponmechanics.weapon.info;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.placeholder.PlaceholderAPI;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.core.utils.ReflectionUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.MessageHelper;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;

import static me.deecaad.weaponmechanics.WeaponMechanics.*;

public class WeaponInfoDisplay implements Serializer<WeaponInfoDisplay> {

    private static Constructor<?> packetPlayOutExperienceConstructor;

    static {
        if (CompatibilityAPI.getVersion() < 1.15) {
            packetPlayOutExperienceConstructor = ReflectionUtil.getConstructor(ReflectionUtil.getPacketClass("PacketPlayOutExperience"), float.class, int.class, int.class);
        }
    }

    private String actionBar;

    private String bossBar;
    private BossBar.Color barColor;
    private BossBar.Overlay barStyle;

    private boolean showAmmoInBossBarProgress;
    private boolean showAmmoInExpLevel;
    private boolean showAmmoInExpProgress;

    private String dualWieldMainActionBar;
    private String dualWieldMainBossBar;
    private String dualWieldOffActionBar;
    private String dualWieldOffBossBar;

    /**
     * Default constructor for serializer
     */
    public WeaponInfoDisplay() {
    }

    public WeaponInfoDisplay(String actionBar, String bossBar, BossBar.Color barColor, BossBar.Overlay barStyle,
                             boolean showAmmoInBossBarProgress, boolean showAmmoInExpLevel, boolean showAmmoInExpProgress,
                             String dualWieldMainActionBar, String dualWieldMainBossBar, String dualWieldOffActionBar, String dualWieldOffBossBar) {
        this.actionBar = actionBar;
        this.bossBar = bossBar;
        this.barColor = barColor;
        this.barStyle = barStyle;
        this.showAmmoInBossBarProgress = showAmmoInBossBarProgress;
        this.showAmmoInExpLevel = showAmmoInExpLevel;
        this.showAmmoInExpProgress = showAmmoInExpProgress;
        this.dualWieldMainActionBar = dualWieldMainActionBar;
        this.dualWieldMainBossBar = dualWieldMainBossBar;
        this.dualWieldOffActionBar = dualWieldOffActionBar;
        this.dualWieldOffBossBar = dualWieldOffBossBar;
    }

    public void send(PlayerWrapper playerWrapper, EquipmentSlot slot) {
        send(playerWrapper, slot, null, null);
    }

    public void send(PlayerWrapper playerWrapper, EquipmentSlot slot, ItemStack knownNewMainStack, ItemStack knownNewOffStack) {
        Player player = playerWrapper.getPlayer();
        MessageHelper messageHelper = playerWrapper.getMessageHelper();

        String mainWeapon = playerWrapper.getMainHandData().getCurrentWeaponTitle();
        String offWeapon = playerWrapper.getOffHandData().getCurrentWeaponTitle();

        ItemStack mainStack = knownNewMainStack == null ? player.getEquipment().getItemInMainHand() : knownNewMainStack;
        ItemStack offStack = knownNewOffStack == null ? player.getEquipment().getItemInOffHand() : knownNewOffStack;

        InfoHandler infoHandler = WeaponMechanics.getWeaponHandler().getInfoHandler();
        String checkCorrectMain = infoHandler.getWeaponTitle(mainStack, false);
        if (checkCorrectMain == null) {
            // No mainhand weapon
            mainStack = null;
            mainWeapon = null;
            playerWrapper.getMainHandData().setCurrentWeaponTitle(null);
        } else if (!checkCorrectMain.equals(mainWeapon)) {
            // Ensure that the weapon is actually same
            mainWeapon = checkCorrectMain;
        }

        String checkCorrectOff = infoHandler.getWeaponTitle(offStack, false);
        if (checkCorrectOff == null) {
            // No offhand weapon
            offStack = null;
            offWeapon = null;
            playerWrapper.getOffHandData().setCurrentWeaponTitle(null);
        } else if (!checkCorrectOff.equals(offWeapon)) {
            // Ensure that the weapon is actually same
            offWeapon = checkCorrectOff;
        }

        if (mainWeapon == null && offWeapon == null) return;

        // Mostly this is RIGHT, but some players may have it LEFT
        boolean hasInvertedMainHand = player.getMainHand() == MainHand.LEFT;

        boolean mainhand = slot == EquipmentSlot.HAND;
        boolean isDualWielding = mainWeapon != null && offWeapon != null && mainStack != null && offStack != null;

        if (actionBar != null) {
            if (isDualWielding) {
                String offHand, mainHand;

                WeaponInfoDisplay mainDisplay;
                WeaponInfoDisplay offDisplay;

                if (mainWeapon.equals(offWeapon)) {
                    mainDisplay = this;
                    offDisplay = this;
                } else {
                    mainDisplay = mainhand ? this : getConfigurations().getObject(mainWeapon + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class);
                    offDisplay = mainhand ? getConfigurations().getObject(offWeapon + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class) : this;
                }

                // OFF HAND < dual wield split > MAIN HAND
                // IF inverted: MAIN HAND < dual wield split > OFF HAND
                offHand = getDualDisplay(offDisplay, player, offStack, offWeapon, EquipmentSlot.OFF_HAND, mainDisplay, false, hasInvertedMainHand);
                mainHand = getDualDisplay(mainDisplay, player, mainStack, mainWeapon, EquipmentSlot.HAND, offDisplay, false, hasInvertedMainHand);

                Audience audience = MechanicsCore.getPlugin().adventure.player(player);
                audience.sendActionBar(MechanicsCore.getPlugin().message.deserialize(buildDisplay(new StringBuilder(), hasInvertedMainHand, mainHand, offHand).toString()));
            } else {
                if (mainhand) {
                    if (mainStack != null && mainStack.hasItemMeta()) {
                        Audience audience = MechanicsCore.getPlugin().adventure.player(player);
                        audience.sendActionBar(MechanicsCore.getPlugin().message.deserialize(PlaceholderAPI.applyPlaceholders(actionBar, player, mainStack, mainWeapon, slot)));
                    }
                } else if (offStack != null && offStack.hasItemMeta()) {
                    Audience audience = MechanicsCore.getPlugin().adventure.player(player);
                    audience.sendActionBar(MechanicsCore.getPlugin().message.deserialize(PlaceholderAPI.applyPlaceholders(actionBar, player, offStack, offWeapon, slot)));
                }
            }
        }

        double magazineProgress = -1;

        if (bossBar != null) {
            StringBuilder builder = new StringBuilder();

            if (isDualWielding) {

                String offHand, mainHand;

                WeaponInfoDisplay mainDisplay;
                WeaponInfoDisplay offDisplay;

                if (mainWeapon.equals(offWeapon)) {
                    mainDisplay = this;
                    offDisplay = this;
                } else {
                    mainDisplay = mainhand ? this : getConfigurations().getObject(mainWeapon + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class);
                    offDisplay = mainhand ? getConfigurations().getObject(offWeapon + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class) : this;
                }

                // OFF HAND < dual wield split > MAIN HAND
                // IF inverted: MAIN HAND < dual wield split > OFF HAND
                offHand = getDualDisplay(offDisplay, player, offStack, offWeapon, EquipmentSlot.OFF_HAND, mainDisplay, true, hasInvertedMainHand);
                mainHand = getDualDisplay(mainDisplay, player, mainStack, mainWeapon, EquipmentSlot.HAND, offDisplay, true, hasInvertedMainHand);

                buildDisplay(builder, hasInvertedMainHand, mainHand, offHand);
            } else {
                if (mainhand) {
                    if (mainStack != null && mainStack.hasItemMeta()) {
                        builder.append(PlaceholderAPI.applyPlaceholders(bossBar, player, mainStack, mainWeapon, slot));
                    }
                } else if (offStack != null && offStack.hasItemMeta()) {
                    builder.append(PlaceholderAPI.applyPlaceholders(bossBar, player, offStack, offWeapon, slot));
                }
            }

            if (builder.length() != 0) {
                BossBar bossBar = messageHelper.getBossBar();
                if (bossBar == null) {
                    bossBar = BossBar.bossBar(MechanicsCore.getPlugin().message.deserialize(builder.toString()), 1.0f, barColor, barStyle);
                    messageHelper.setBossBar(bossBar);

                    Audience audience = MechanicsCore.getPlugin().adventure.player(player);
                    audience.showBossBar(bossBar);

                } else {
                    Bukkit.getScheduler().cancelTask(messageHelper.getBossBarTask());
                    bossBar.name(MechanicsCore.getPlugin().message.deserialize(builder.toString()));
                    bossBar.color(barColor);
                    bossBar.overlay(barStyle);
                }
                if (showAmmoInBossBarProgress) {
                    magazineProgress = mainhand ? getMagazineProgress(mainStack, mainWeapon) : getMagazineProgress(offStack, offWeapon);
                    bossBar.progress((float) magazineProgress);
                }
                messageHelper.setBossBarTask(new BukkitRunnable() {
                    @Override
                    public void run() {
                        Audience audience = MechanicsCore.getPlugin().adventure.player(player);
                        audience.hideBossBar(messageHelper.getBossBar());
                        messageHelper.setBossBar(null);
                        messageHelper.setBossBarTask(0);
                    }
                }.runTaskLater(WeaponMechanics.getPlugin(), 40).getTaskId());
            }
        }

        if (showAmmoInExpLevel || showAmmoInExpProgress) {
            ItemStack useStack = mainhand ? mainStack : offStack;
            String useWeapon = mainhand ? mainWeapon : offWeapon;

            if (useStack == null || !useStack.hasItemMeta() || useWeapon == null) return;

            if (magazineProgress == -1) {
                magazineProgress = getMagazineProgress(useStack, useWeapon);
            }

            int lastExpTask = messageHelper.getExpTask();
            if (lastExpTask != 0) Bukkit.getServer().getScheduler().cancelTask(lastExpTask);

            if (CompatibilityAPI.getVersion() < 1.15) {
                CompatibilityAPI.getCompatibility().sendPackets(player,
                        ReflectionUtil.newInstance(packetPlayOutExperienceConstructor, showAmmoInExpProgress
                                        ? (float) (magazineProgress != -1 ? magazineProgress : getMagazineProgress(useStack, useWeapon))
                                        : player.getExp(),
                                player.getTotalExperience(),
                                showAmmoInExpLevel ? getAmmoLeft(useStack, useWeapon) : player.getLevel()));
                messageHelper.setExpTask(new BukkitRunnable() {
                    public void run() {
                        CompatibilityAPI.getCompatibility().sendPackets(player,
                                ReflectionUtil.newInstance(packetPlayOutExperienceConstructor,
                                        player.getExp(),
                                        player.getTotalExperience(),
                                        player.getLevel()));
                        messageHelper.setExpTask(0);
                    }
                }.runTaskLater(WeaponMechanics.getPlugin(), 40).getTaskId());
            } else {
                player.sendExperienceChange(showAmmoInExpProgress ? (float) (magazineProgress != -1 ? magazineProgress : getMagazineProgress(useStack, useWeapon)) : player.getExp(),
                        showAmmoInExpLevel ? getAmmoLeft(useStack, useWeapon) : player.getLevel());
                messageHelper.setExpTask(new BukkitRunnable() {
                    public void run() {
                        player.sendExperienceChange(player.getExp(), player.getLevel());
                        messageHelper.setExpTask(0);
                    }
                }.runTaskLater(WeaponMechanics.getPlugin(), 40).getTaskId());
            }
        }
    }

    private String getDualDisplay(WeaponInfoDisplay display, Player player, ItemStack stack, String weapon, EquipmentSlot slot, WeaponInfoDisplay otherDisplay, boolean bossbar, boolean isInverted) {
        if (display == null) return null;

        String toApply;

        if (otherDisplay == null) {
            toApply = bossbar
                    ? PlaceholderAPI.applyPlaceholders(display.bossBar, player, stack, weapon, slot)
                    : PlaceholderAPI.applyPlaceholders(display.actionBar, player, stack, weapon, slot);
        } else {
            String mainHand = getBasicConfigurations().getString("Placeholder_Symbols.Dual_Wield.Main_Hand",
                    "<gold>%ammo-left%<gray>»<gold>%reload% <gold>%firearm-state%%weapon-title%");
            String offHand = getBasicConfigurations().getString("Placeholder_Symbols.Dual_Wield.Off_Hand",
                    "<gold>%weapon-title%%firearm-state% <gold>%reload%<gray>«<gold>%ammo-left%");

            if (isInverted) {
                if (slot == EquipmentSlot.HAND) {
                    toApply = bossbar
                            ? display.dualWieldOffBossBar != null ? display.dualWieldOffBossBar : offHand
                            : display.dualWieldOffActionBar != null ? display.dualWieldOffActionBar : offHand;
                } else {
                    toApply = bossbar
                            ? display.dualWieldMainBossBar != null ? display.dualWieldMainBossBar : mainHand
                            : display.dualWieldMainActionBar != null ? display.dualWieldMainActionBar : mainHand;
                }
            } else {
                if (slot == EquipmentSlot.HAND) {
                    toApply = bossbar
                            ? display.dualWieldMainBossBar != null ? display.dualWieldMainBossBar : mainHand
                            : display.dualWieldMainActionBar != null ? display.dualWieldMainActionBar : mainHand;
                } else {
                    toApply = bossbar
                            ? display.dualWieldOffBossBar != null ? display.dualWieldOffBossBar : offHand
                            : display.dualWieldOffActionBar != null ? display.dualWieldOffActionBar : offHand;
                }
            }
        }

        return PlaceholderAPI.applyPlaceholders(toApply, player, stack, weapon, slot);
    }

    private StringBuilder buildDisplay(StringBuilder builder, boolean hasInvertedMainHand, String mainHand, String offHand) {
        String dualWieldSplit = getBasicConfigurations().getString("Placeholder_Symbols.Dual_Wield.Split", " &7| ");
        if (hasInvertedMainHand) {
            if (mainHand != null) {
                builder.append(mainHand);
                if (offHand != null) {
                    builder.append(dualWieldSplit);
                }
            }
            if (offHand != null) {
                builder.append(offHand);
            }
        } else {
            if (offHand != null) {
                builder.append(offHand);
                if (mainHand != null) {
                    builder.append(dualWieldSplit);
                }
            }
            if (mainHand != null) {
                builder.append(mainHand);
            }
        }
        return builder;
    }

    private int getAmmoLeft(ItemStack weaponStack, String weaponTitle) {
        return getWeaponHandler().getReloadHandler().getAmmoLeft(weaponStack, weaponTitle);
    }

    private double getMagazineProgress(ItemStack weaponStack, String weaponTitle) {
        double progress = (double) getWeaponHandler().getReloadHandler().getAmmoLeft(weaponStack, weaponTitle) / (double) getConfigurations().getInt(weaponTitle + ".Reload.Magazine_Size");
        return NumberUtil.minMax(0.0, progress, 1.0);
    }

    @Override
    public String getKeyword() {
        return "Weapon_Info_Display";
    }

    @Override
    @Nonnull
    public WeaponInfoDisplay serialize(SerializeData data) throws SerializerException {

        // ACTION BAR
        String actionBarMessage = data.of("Action_Bar.Message").getAdventure(null);

        String bossBarMessage = data.of("Boss_Bar.Title").getAdventure(null);
        BossBar.Color barColor = null;
        BossBar.Overlay barStyle = null;
        if (bossBarMessage != null) {
            barColor = data.of("Boss_Bar.Bar_Color").getEnum(BossBar.Color.class, BossBar.Color.WHITE);
            barStyle = data.of("Boss_Bar.Bar_Style").getEnum(BossBar.Overlay.class, BossBar.Overlay.NOTCHED_20);
        }

        boolean expLevel = data.of("Show_Ammo_In.Exp_Level").getBool(false);
        boolean expProgress = data.of("Show_Ammo_In.Exp_Progress").getBool(false);
        boolean bossBarProgress = data.of("Show_Ammo_In.Boss_Bar_Progress").getBool(false);

        String dualWieldMainActionBar = data.of("Action_Bar.Dual_Wield.Main_Hand").getAdventure(null);
        String dualWieldMainBossBar = data.of("Boss_Bar.Dual_Wield.Main_Hand").getAdventure(null);

        String dualWieldOffActionBar = data.of("Action_Bar.Dual_Wield.Off_Hand").getAdventure(null);
        String dualWieldOffBossBar = data.of("Boss_Bar.Dual_Wield.Off_Hand").getAdventure(null);

        if (actionBarMessage == null && bossBarMessage == null && !expLevel && !expProgress) {
            throw data.exception(null, "Found an empty Weapon_Info_Display... Users won't be able to see any changes in their ammo!");
        }

        if (bossBarProgress && bossBarMessage == null) {
            throw data.exception(null, "In order for a boss bar to work properly, 'Show_Ammo_In.Boss_Bar_Progress: true' and the",
                    "boss bar needs to be defined in the message.");
        }

        return new WeaponInfoDisplay(actionBarMessage, bossBarMessage, barColor, barStyle,
                bossBarProgress, expLevel, expProgress,
                dualWieldMainActionBar, dualWieldMainBossBar, dualWieldOffActionBar, dualWieldOffBossBar);
    }
}