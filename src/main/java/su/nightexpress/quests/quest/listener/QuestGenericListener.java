package su.nightexpress.quests.quest.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.QuestManager;

public class QuestGenericListener extends AbstractListener<QuestsPlugin> {

    private final QuestManager manager;

    public QuestGenericListener(@NotNull QuestsPlugin plugin, QuestManager manager) {
        super(plugin);
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        // 玩家登录时检查并刷新任务
        this.manager.checkAndRefreshPlayerQuests(event.getPlayer());
    }
}
