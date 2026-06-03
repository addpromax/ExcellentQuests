package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.raid.RaidFinishEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 赢得突袭任务监听器
 */
public class WinRaidTaskListener extends TaskListener<String, AdapterFamily<String>> {

    public WinRaidTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<String, AdapterFamily<String>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidFinish(RaidFinishEvent event) {
        // 遍历附近的玩家，奖励参与突袭的玩家
        event.getRaid().getLocation().getNearbyEntities(64, 64, 64).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .filter(this.manager::canDoTasks)
            .forEach(player -> this.progressQuests(player, "raid_victory"));
    }
}

