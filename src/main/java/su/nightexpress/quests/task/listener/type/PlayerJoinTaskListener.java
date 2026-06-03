package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;
import su.nightexpress.quests.user.QuestUser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 玩家登录任务监听器
 * 每个玩家每天只计算一次登录
 */
public class PlayerJoinTaskListener extends TaskListener<String, AdapterFamily<String>> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PlayerJoinTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<String, AdapterFamily<String>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延迟处理，确保玩家数据已加载
        // 使用 NightCore 的调度器以支持 Folia
        this.plugin.runTaskLater(task -> {
            if (!player.isOnline() || !this.manager.canDoTasks(player)) {
                return;
            }
            
            // 获取玩家数据
            QuestUser user = this.plugin.getUserManager().getOrFetch(player);
            
            // 获取今天的日期字符串
            String today = LocalDate.now().format(DATE_FORMATTER);
            
            // 检查是否是今天首次登录
            if (user.isFirstLoginToday(today)) {
                // 更新玩家的登录日期
                user.setLastLoginDate(today);
                
                // 保存玩家数据
                this.plugin.getUserManager().save(user);
                
                // 使用 "join" 作为标识符，会被转换为 vanilla_string:join
                this.progressQuests(player, "join");
            }
        }, 20L);
    }
}

