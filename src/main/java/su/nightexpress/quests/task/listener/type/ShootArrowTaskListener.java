package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 射箭任务监听器
 */
public class ShootArrowTaskListener extends TaskListener<String, AdapterFamily<String>> {

    public ShootArrowTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<String, AdapterFamily<String>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        Player player = (Player) event.getEntity();
        if (!this.manager.canDoTasks(player)) return;

        // 使用箭矢类型作为任务对象
        this.progressQuests(player, "arrow");
    }
}

