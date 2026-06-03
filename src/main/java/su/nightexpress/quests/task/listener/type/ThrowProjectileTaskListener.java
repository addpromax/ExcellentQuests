package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 投掷物任务监听器
 * 支持：雪球、鸡蛋、末影珍珠
 */
public class ThrowProjectileTaskListener extends TaskListener<String, AdapterFamily<String>> {

    public ThrowProjectileTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<String, AdapterFamily<String>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) return;

        Player player = (Player) projectile.getShooter();
        if (!this.manager.canDoTasks(player)) return;

        String projectileType = null;
        if (projectile instanceof Snowball) {
            projectileType = "snowball";
        } else if (projectile instanceof Egg) {
            projectileType = "egg";
        } else if (projectile instanceof EnderPearl) {
            projectileType = "enderpearl";
        }

        if (projectileType != null) {
            this.progressQuests(player, projectileType);
        }
    }
}

