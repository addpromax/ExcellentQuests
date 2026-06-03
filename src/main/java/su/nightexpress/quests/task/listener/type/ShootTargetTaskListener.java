package su.nightexpress.quests.task.listener.type;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 射中靶心任务监听器
 */
public class ShootTargetTaskListener extends TaskListener<Block, AdapterFamily<Block>> {

    public ShootTargetTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<Block, AdapterFamily<Block>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Block hitBlock = event.getHitBlock();
        if (hitBlock == null || hitBlock.getType() != Material.TARGET) return;

        Player player = (Player) event.getEntity().getShooter();
        if (!this.manager.canDoTasks(player)) return;

        this.progressQuests(player, hitBlock);
    }
}

