package io.th0rgal.oraxen.mechanics.provided.farming.harvesting;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HarvestingMechanicManager implements Listener {

    private final MechanicFactory factory;

    public HarvestingMechanicManager(final MechanicFactory factory) {
        this.factory = factory;
    }

    private static List<Block> getNearbyBlocks(final Location location, final int radius, final int height) {
        final List<Block> blocks = new ArrayList<>();
        for (int x = location.getBlockX() - Math.floorDiv(radius, 2); x <= location.getBlockX()
                + Math.floorDiv(radius, 2); x++)
            for (int y = location.getBlockY() - Math.floorDiv(height, 2); y <= location.getBlockY()
                    + Math.floorDiv(height, 2); y++)
                for (int z = location.getBlockZ() - Math.floorDiv(radius, 2); z <= location.getBlockZ()
                        + Math.floorDiv(radius, 2); z++)
                    blocks.add(Objects.requireNonNull(location.getWorld()).getBlockAt(x, y, z));
        return blocks;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock() == null)
            return;

        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null)
            return;

        final String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        final Player player = event.getPlayer();

        final HarvestingMechanic mechanic = (HarvestingMechanic) factory.getMechanic(itemID);

        final Timer playerTimer = mechanic.getTimer(player);

        if (!playerTimer.isFinished()) {
            mechanic.getTimer(player).sendToPlayer(player);
            return;
        }

        playerTimer.reset();

        for (final Block block : getNearbyBlocks(event.getClickedBlock().getLocation(), mechanic.getRadius(),
                mechanic.getHeight()))
            if (block.getBlockData() instanceof Ageable ageable
                    && ageable.getAge() == ageable.getMaximumAge()
                    && ProtectionLib.canBreak(player, block.getLocation())
                    && ProtectionLib.canBuild(player, block.getLocation())) {
                ageable.setAge(0);
                block.setBlockData(ageable);
                final List<ItemStack> drops = new ArrayList<>();
                switch (block.getType()) {
                    case WHEAT -> {
                        drops.add(new ItemStack(Material.WHEAT));
                        drops.add(new ItemStack(Material.WHEAT_SEEDS));
                    }
                    case BEETROOTS -> {
                        drops.add(new ItemStack(Material.BEETROOT));
                        drops.add(new ItemStack(Material.BEETROOT_SEEDS));
                    }
                    default -> drops.addAll(block.getDrops());
                }
                for (final ItemStack itemStack : drops)
                    giveItem(player, itemStack);
            }

    }

    private void giveItem(final HumanEntity humanEntity, final ItemStack item) {
        if (humanEntity.getInventory().firstEmpty() != -1)
            humanEntity.getInventory().addItem(item);
        else
            humanEntity.getWorld().dropItem(humanEntity.getLocation(), item);
    }
}
