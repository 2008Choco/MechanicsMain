package me.deecaad.core.events.triggers;

import com.google.common.collect.ImmutableList;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.events.EntityEquipmentEvent;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

/**
 * This class triggers the {@link EntityEquipmentEvent}. This
 * event occurs when an {@link org.bukkit.inventory.EntityEquipment} is
 * changed.
 */
public class EquipListener implements Listener {

    // * ----- REFLECTIONS ----- * //
    private static final Field playerInventoryField;
    private static final Class<?> playerInventoryClass;
    private static final Field inventoryField;
    private static final Field armorField;
    private static final Field offHandField;
    private static final Field hotBarSlotField;
    private static final Field combinedField;

    static {
        Class<?> humanClass = ReflectionUtil.getNMSClass("world.entity.player", "EntityHuman");
        playerInventoryClass = ReflectionUtil.getNMSClass("world.entity.player", "PlayerInventory");
        playerInventoryField = ReflectionUtil.getField(humanClass, playerInventoryClass);

        // Used to get player inventory fields
        Class<?> nonNullListClass = ReflectionUtil.getNMSClass("core", "NonNullList");

        inventoryField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass, 0);
        armorField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass, 1);
        offHandField = ReflectionUtil.getField(playerInventoryClass, nonNullListClass, 2);
        combinedField = ReflectionUtil.getField(playerInventoryClass, List.class, 3); // index 3 since nonNonList is a List

        hotBarSlotField = ReflectionUtil.getField(playerInventoryClass, int.class, 0, true);
    }

    // * ----- END OF REFLECTIONS ----- * //

    public static final EquipListener SINGLETON = new EquipListener();

    private final Set<Player> dropCancelledPlayers;
    private final Set<Player> ignoreGiveDropPlayers;

    private EquipListener() {
        dropCancelledPlayers = new HashSet<>();
        ignoreGiveDropPlayers = new HashSet<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSwap(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        Inventory inv = player.getInventory();

        // Swapping items doesn't cause any changes in the inventory, so it is
        // not covered by the player injection system.
        if (!(isEmpty(inv.getItem(e.getNewSlot())) && isEmpty(inv.getItem(e.getPreviousSlot())))) {
            ItemStack old = inv.getItem(e.getPreviousSlot());
            ItemStack current = inv.getItem(e.getNewSlot());
            Bukkit.getPluginManager().callEvent(new EntityEquipmentEvent(player, EquipmentSlot.HAND, old, current));
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {

        // The command line may look like "/give CJCrafter iron_ingot"
        String commandLine = event.getMessage().toLowerCase(Locale.ROOT);
        Player player = event.getPlayer();

        // The VANILLA give command has the unfortunate side effect of causing
        // a PlayerDropItemEvent (EssentialsX overrides it and fixes it). This
        // will cause a de-equip event (seemingly randomly depending on hotbar
        // slot), then an equip event (If the item given to the player goes
        // into their hand).
        if (commandLine.startsWith("/give") || commandLine.startsWith("/minecraft:give")) {
            Listener listener = new Listener() {
                @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
                public void onDrop(PlayerDropItemEvent event) {
                    if (player.equals(event.getPlayer())) {
                        ignoreGiveDropPlayers.add(player);
                    }
                }
            };

            // Register, then unregister in 1 tick
            Bukkit.getPluginManager().registerEvents(listener, MechanicsCore.getPlugin());
            new BukkitRunnable() {
                @Override
                public void run() {
                    HandlerList.unregisterAll(listener);
                }
            }.runTask(MechanicsCore.getPlugin());
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryDrop(InventoryClickEvent event) {
        if (event.getSlot() == -999 && !isEmpty(event.getCursor()) && event.getWhoClicked() instanceof Player) {
            ignoreGiveDropPlayers.add((Player) event.getWhoClicked());
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // When a drop event is cancelled, the item is still removed from the
        // player's inventory, but it is reset later. This causes invalid equip
        // events. By adding cancelled events into the set, we can then filter
        // the "false" equip events.
        if (event.isCancelled()) {
            dropCancelledPlayers.add(player);
            return;
        } else if (ignoreGiveDropPlayers.remove(player)) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getItemInMainHand();

        // Only call event if the stack was emptied by dropping the last item
        // in a stack, or dropping an un-stackable item.
        if (isEmpty(item)) {
            Bukkit.getPluginManager().callEvent(new EntityEquipmentEvent(player, EquipmentSlot.HAND, event.getItemDrop().getItemStack(), null));
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getItemInHand().getAmount() - 1 == 0) {
            Bukkit.getPluginManager().callEvent(new EntityEquipmentEvent(event.getPlayer(), event.getHand(), new ItemStack(event.getBlockPlaced().getType()), null));
        }
    }

    @SuppressWarnings("unchecked")
    public void inject(Player player) {
        Object handle = CompatibilityAPI.getCompatibility().getEntityPlayer(player);
        Object playerInventory = ReflectionUtil.invokeField(playerInventoryField, handle);

        List<Object> inventory = CompatibilityAPI.getEntityCompatibility().generateNonNullList(36, (old, current, index) -> {
            if (isIllegalModification())
                return;

            // Filters out cancelled PlayerDropItemEvent
            if (dropCancelledPlayers.remove(player)) {
                return;
            }

            int hotBar = (int) ReflectionUtil.invokeField(hotBarSlotField, playerInventory);

            // Not sure how important this check is, but the MC code does it.
            // I assume that means hot bar can mean something else.
            // TAKE NOTE that this code does not call an event when the hotBar
            // var is changed... We must use an event for that.
            if (hotBar >= 0 && hotBar < 9) {
                if (hotBar == index) {
                    Bukkit.getPluginManager().callEvent(new EntityEquipmentEvent(player, EquipmentSlot.HAND, old, current));
                }
            }
        });
        List<Object> armor = CompatibilityAPI.getEntityCompatibility().generateNonNullList(4, (old, current, index) -> {
            if (isIllegalModification())
                return;

            EquipmentSlot slot = switch (index) {
                case 0 -> EquipmentSlot.FEET;
                case 1 -> EquipmentSlot.LEGS;
                case 2 -> EquipmentSlot.CHEST;
                case 3 -> EquipmentSlot.HEAD;
                default -> throw new IndexOutOfBoundsException("Index out of bounds: " + index + ", for list " + this);
            };

            Bukkit.getPluginManager().callEvent(new EntityEquipmentEvent(player, slot, old, current));
        });
        List<Object> offhand = CompatibilityAPI.getEntityCompatibility().generateNonNullList(1, (old, current, index) -> {
            if (isIllegalModification())
                return;

            Bukkit.getPluginManager().callEvent(new EntityEquipmentEvent(player, EquipmentSlot.OFF_HAND, old, current));
        });

        List<Object> oldItems = (List<Object>) ReflectionUtil.invokeField(inventoryField, playerInventory);
        for (int i = 0; i < oldItems.size(); i++) {
            inventory.set(i, oldItems.get(i));
        }
        List<Object> oldArmor = (List<Object>) ReflectionUtil.invokeField(armorField, playerInventory);
        for (int i = 0; i < oldArmor.size(); i++) {
            armor.set(i, oldArmor.get(i));
        }
        offhand.set(0, ((List<Object>) ReflectionUtil.invokeField(offHandField, playerInventory)).get(0));

        ReflectionUtil.setField(inventoryField, playerInventory, inventory);
        ReflectionUtil.setField(armorField, playerInventory, armor);
        ReflectionUtil.setField(offHandField, playerInventory, offhand);
        ReflectionUtil.setField(combinedField, playerInventory, ImmutableList.of(inventory, armor, offhand));
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /**
     * Returns <code>true</code> when the inventory is modified asynchronously.
     * Developers -- DO NOT MODIFY THE INVENTORY OF ANY ENTITY ASYNCHRONOUSLY.
     * You obviously didn't look into spigot source code, or you just don't
     * care about safety. This is an unsafe operation.
     *
     * @return true if the inventory was modified async.
     */
    private static boolean isIllegalModification() {
        if (Bukkit.isPrimaryThread())
            return false;

        // Walk backwards in the stack trace to find the offending plugin
        final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        Optional<StackWalker.StackFrame> optional = STACK_WALKER.walk(stream -> stream.filter(frame -> isPlugin(frame.getDeclaringClass())).findFirst());
        if (optional.isEmpty()) {
            MechanicsCore.debug.log(LogLevel.WARN, "Unknown async call to add inventory", new Throwable());
            return true;
        }

        // Gather information about the plugin
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(optional.get().getDeclaringClass());
        PluginDescriptionFile desc = plugin.getDescription();
        String location = optional.get().getDeclaringClass().getName() + "." + optional.get().getMethodName() + "(" + optional.get().getFileName() + ":" + optional.get().getLineNumber() + ")";

        // Spam console with the error using the offending plugin's logger.
        // This way, server admins will go to the offending plugin's support
        // channel INSTEAD OF the WeaponMechanics support channel. It is not
        // our job to fix other people's bad code.
        plugin.getLogger().log(Level.SEVERE, String.format("Nag author(s) %s of %s-%s about their async inventory modification at %s",
                desc.getAuthors(), desc.getName(), desc.getVersion(), location));
        MechanicsCore.debug.error("Found a bad plugin '" + desc.getName() + "' for modifying inventory async.");
        return true;
    }

    private static boolean isPlugin(Class<?> clazz) {
        try {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(clazz);
            return !"MechanicsCore".equals(plugin.getName());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
