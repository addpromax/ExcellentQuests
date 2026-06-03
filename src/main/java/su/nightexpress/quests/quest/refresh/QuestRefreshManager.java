package su.nightexpress.quests.quest.refresh;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.random.Rnd;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.GlobalQuestManager;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestType;
import su.nightexpress.quests.user.QuestUser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务刷新管理器
 * 负责按照配置定时刷新全局任务（合作和竞争）
 * 注意：独立任务由 QuestManager.updatePeriodQuests 根据周期（每日/每周/每月）自动刷新
 */
public class QuestRefreshManager {
    
    private final QuestsPlugin plugin;
    private final QuestManager questManager;
    private final QuestRefreshConfig config;
    private final GlobalQuestManager globalQuestManager;
    
    public QuestRefreshManager(@NotNull QuestsPlugin plugin,
                              @NotNull QuestManager questManager,
                              @NotNull QuestRefreshConfig config,
                              @NotNull GlobalQuestManager globalQuestManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.config = config;
        this.globalQuestManager = globalQuestManager;
    }
    
    /**
     * 检查并刷新所有类型的任务
     * 注意：独立任务由 QuestManager.updatePeriodQuests 根据周期自动刷新，这里只刷新全局任务
     */
    public void checkAndRefreshAll() {
        // 只刷新全局任务（合作和竞争）
        // 独立任务不在这里刷新，由 updatePeriodQuests 根据周期自动处理
        if (config.shouldRefresh(QuestType.COOPERATIVE)) {
            refreshQuestType(QuestType.COOPERATIVE);
        }
        if (config.shouldRefresh(QuestType.COMPETITIVE)) {
            refreshQuestType(QuestType.COMPETITIVE);
        }
    }
    
    /**
     * 仅检查并刷新全局任务（合作和竞争），用于服务器启动时
     */
    public void checkAndRefreshGlobalOnly() {
        plugin.info("服务器启动，仅检查全局任务（合作和竞争）...");
        
        // 清理过期的全局任务
        globalQuestManager.cleanExpiredQuests();
        
        // 检查并刷新合作任务
        if (config.shouldRefresh(QuestType.COOPERATIVE)) {
            refreshQuestType(QuestType.COOPERATIVE);
        }
        
        // 检查并刷新竞争任务
        if (config.shouldRefresh(QuestType.COMPETITIVE)) {
            refreshQuestType(QuestType.COMPETITIVE);
        }
        
        plugin.info("全局任务检查完成（独立任务将在玩家登录时根据周期自动刷新）");
    }
    
    /**
     * 刷新指定类型的任务
     * 注意：独立任务不应该调用此方法，它们由 updatePeriodQuests 根据周期自动刷新
     */
    public void refreshQuestType(@NotNull QuestType questType) {
        plugin.info("正在刷新 " + questType.getDisplayName() + "...");
        
        // 清理过期的全局任务
        globalQuestManager.cleanExpiredQuests();
        
        // 根据任务类型选择不同的刷新策略
        if (questType == QuestType.COOPERATIVE) {
            // 合作任务：使用全局刷新方法（无需在线玩家）
            questManager.refreshCooperativeQuestsGlobal();
        } else if (questType == QuestType.COMPETITIVE) {
            // 竞争任务：使用全局刷新方法（无需在线玩家）
            questManager.refreshCompetitiveQuestsGlobal();
        } else if (questType == QuestType.INDEPENDENT) {
            // 独立任务：不应该在这里刷新，由 updatePeriodQuests 根据周期自动处理
            plugin.warn("独立任务应该由 updatePeriodQuests 根据周期（每日/每周/每月）自动刷新，跳过。");
            return;
        }
        
        // 标记已刷新
        config.markRefreshed(questType);
    }
    
    @NotNull
    public String getTimeUntilRefreshString(@NotNull QuestType questType) {
        return config.getNextRefreshTimeString(questType);
    }
    
    @NotNull
    public QuestRefreshConfig getConfig() {
        return config;
    }
}

