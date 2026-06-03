package su.nightexpress.quests.task.listener.type;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 完成进度任务监听器
 */
public class CompleteAdvancementTaskListener extends TaskListener<String, AdapterFamily<String>> {

    public CompleteAdvancementTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<String, AdapterFamily<String>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        Advancement advancement = event.getAdvancement();
        // 过滤掉根进度和配方进度
        if (advancement.getKey().getKey().startsWith("recipes/")) return;

        // 使用固定标识符 "advancement" 表示完成任意进度
        // 这样所有进度都会计入同一个目标
        this.progressQuests(player, "advancement");
    }
}

