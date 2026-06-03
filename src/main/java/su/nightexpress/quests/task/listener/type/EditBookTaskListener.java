package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 编辑书籍任务监听器
 */
public class EditBookTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public EditBookTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (!event.isSigning()) return;  // 只在签名时计数

        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack book = new ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        this.progressQuests(player, book);
    }
}

