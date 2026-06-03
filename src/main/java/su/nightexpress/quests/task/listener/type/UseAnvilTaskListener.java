package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 使用铁砧任务监听器
 */
public class UseAnvilTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public UseAnvilTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (event.getRawSlot() != 2) return;

        Player player = (Player) event.getWhoClicked();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null) return;

        this.progressQuests(player, result);
    }
}

