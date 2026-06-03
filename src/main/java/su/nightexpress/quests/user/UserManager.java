package su.nightexpress.quests.user;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.db.AbstractUserManager;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.battlepass.data.BattlePassData;
import su.nightexpress.quests.data.DataHandler;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.quest.data.QuestData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager extends AbstractUserManager<QuestsPlugin, QuestUser> {

    public UserManager(@NotNull QuestsPlugin plugin, @NotNull DataHandler dataHandler) {
        super(plugin, dataHandler);
    }

    @Override
    @NotNull
    public QuestUser create(@NotNull UUID uuid, @NotNull String name) {
        long dateCreated = System.currentTimeMillis();
        long newQuestsDate = 0L;
        Map<UUID, BattlePassData> battlePassData = new HashMap<>();
        Map<UUID, QuestData> questData = new HashMap<>();
        Map<String, MilestoneData> milestoneData = new HashMap<>();

        return new QuestUser(uuid, name, dateCreated, dateCreated, newQuestsDate, battlePassData, questData, milestoneData);
    }

    @Override
    protected void onShutdown() {
        // 修复：在关闭时保存所有玩家的完整数据（包括任务进度和激活状态）
        this.plugin.info("正在保存所有玩家数据...");
        this.getLoaded().forEach(user -> this.dataManager.saveUser(user));
        this.plugin.info("玩家数据保存完成，共 " + this.getLoaded().size() + " 个玩家");
        
        // 调用父类的清理逻辑
        super.onShutdown();
    }
}
