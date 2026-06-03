package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 拾取物品任务监听器
 */
public class ItemPickupTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public ItemPickupTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack item = event.getItem().getItemStack();
        this.progressQuests(player, item, item.getAmount());
    }
}

