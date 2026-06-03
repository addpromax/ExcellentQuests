package su.nightexpress.quests.task.listener.type;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 放置桶任务监听器（岩浆桶、水桶）
 */
public class PlaceBucketTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public PlaceBucketTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        Material bucketType = event.getBucket();
        ItemStack bucket = new ItemStack(bucketType);
        
        this.progressQuests(player, bucket);
    }
}

