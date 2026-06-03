package su.nightexpress.quests.task.listener.type;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 使用床任务监听器
 */
public class UseBedTaskListener extends TaskListener<Block, AdapterFamily<Block>> {

    public UseBedTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<Block, AdapterFamily<Block>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        Block bed = event.getBed();
        this.progressQuests(player, bed);
    }
}

