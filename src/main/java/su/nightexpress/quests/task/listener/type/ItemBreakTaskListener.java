package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 物品损坏任务监听器
 */
public class ItemBreakTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public ItemBreakTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack item = event.getBrokenItem();
        this.progressQuests(player, item);
    }
}

