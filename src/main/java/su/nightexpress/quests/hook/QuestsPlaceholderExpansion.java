package su.nightexpress.quests.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.quests.QuestsAPI;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestType;
import su.nightexpress.quests.user.QuestUser;

import java.util.UUID;

public class QuestsPlaceholderExpansion extends PlaceholderExpansion {

    private final QuestsPlugin plugin;

    public QuestsPlaceholderExpansion(@NotNull QuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "excellentquests";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NightExpress";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) return null;

        // %excellentquests_quests_total% - 玩家当前所有任务总数
        if (params.equalsIgnoreCase("quests_total")) {
            if (player == null) return "0";
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            return String.valueOf(user.getQuestDatas().size());
        }

        // %excellentquests_quests_independent% - 玩家当前个人任务数量
        if (params.equalsIgnoreCase("quests_independent")) {
            if (player == null) return "0";
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            long count = user.getQuestDatas().stream()
                .filter(questData -> {
                    Quest quest = questManager.getQuestById(questData.getQuestId());
                    return quest != null && quest.getQuestType() == QuestType.INDEPENDENT;
                })
                .count();
            return String.valueOf(count);
        }

        // %excellentquests_quests_cooperative% - 玩家当前合作任务数量
        if (params.equalsIgnoreCase("quests_cooperative")) {
            if (player == null) return "0";
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            long count = user.getQuestDatas().stream()
                .filter(questData -> {
                    Quest quest = questManager.getQuestById(questData.getQuestId());
                    return quest != null && quest.getQuestType() == QuestType.COOPERATIVE;
                })
                .count();
            return String.valueOf(count);
        }

        // %excellentquests_quests_competitive% - 玩家当前竞争任务数量
        if (params.equalsIgnoreCase("quests_competitive")) {
            if (player == null) return "0";
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            long count = user.getQuestDatas().stream()
                .filter(questData -> {
                    Quest quest = questManager.getQuestById(questData.getQuestId());
                    return quest != null && quest.getQuestType() == QuestType.COMPETITIVE;
                })
                .count();
            return String.valueOf(count);
        }

        // %excellentquests_quests_active% - 玩家当前活跃任务数量
        if (params.equalsIgnoreCase("quests_active")) {
            if (player == null) return "0";
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            long count = user.getQuestDatas().stream()
                .filter(QuestData::isActive)
                .count();
            return String.valueOf(count);
        }

        // %excellentquests_quests_completed% - 玩家已完成任务数量
        if (params.equalsIgnoreCase("quests_completed")) {
            if (player == null) return "0";
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            long count = user.getQuestDatas().stream()
                .filter(QuestData::isCompleted)
                .count();
            return String.valueOf(count);
        }

        // %excellentquests_quest_<任务ID>_progress% - 指定任务的进度百分比
        if (params.startsWith("quest_") && params.endsWith("_progress")) {
            if (player == null) return "0";
            String questId = params.substring(6, params.length() - 9);
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            
            QuestData questData = user.getQuestDatas().stream()
                .filter(qd -> qd.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
                
            if (questData == null) return "0";
            
            double progress = questData.getProgressValue();
            UUID globalQuestId = questData.getGlobalQuestId();
            if (globalQuestId != null) {
                GlobalQuestData globalData = questManager.getGlobalQuestManager().getGlobalQuestData(globalQuestId);
                if (globalData != null) {
                    progress = globalData.getProgressValue();
                }
            }
            
            return NumberUtil.format(progress * 100D);
        }

        // %excellentquests_quest_<任务ID>_completed% - 指定任务是否完成
        if (params.startsWith("quest_") && params.endsWith("_completed")) {
            if (player == null) return "false";
            String questId = params.substring(6, params.length() - 10);
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            
            QuestData questData = user.getQuestDatas().stream()
                .filter(qd -> qd.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
                
            return String.valueOf(questData != null && questData.isCompleted());
        }

        // %excellentquests_quest_<任务ID>_active% - 指定任务是否激活
        if (params.startsWith("quest_") && params.endsWith("_active")) {
            if (player == null) return "false";
            String questId = params.substring(6, params.length() - 7);
            QuestUser user = plugin.getUserManager().getOrFetch(player);
            
            QuestData questData = user.getQuestDatas().stream()
                .filter(qd -> qd.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
                
            return String.valueOf(questData != null && questData.isActive());
        }

        // %excellentquests_global_cooperative_<任务ID>_progress% - 全服合作任务进度
        if (params.startsWith("global_cooperative_") && params.endsWith("_progress")) {
            String questId = params.substring(19, params.length() - 9);
            Quest quest = questManager.getQuestById(questId);
            if (quest == null || quest.getQuestType() != QuestType.COOPERATIVE) return "0";
            
            GlobalQuestData globalData = questManager.getGlobalQuestManager().getAllGlobalQuests().stream()
                .filter(data -> data.getQuestId().equals(questId) && !data.isExpired())
                .findFirst()
                .orElse(null);
            
            if (globalData == null) return "0";
            return NumberUtil.format(globalData.getProgressValue() * 100D);
        }

        // %excellentquests_global_competitive_<任务ID>_slots% - 竞争任务剩余名额
        if (params.startsWith("global_competitive_") && params.endsWith("_slots")) {
            String questId = params.substring(19, params.length() - 6);
            Quest quest = questManager.getQuestById(questId);
            if (quest == null || quest.getQuestType() != QuestType.COMPETITIVE) return "0";
            
            GlobalQuestData globalData = questManager.getGlobalQuestManager().getAllGlobalQuests().stream()
                .filter(data -> data.getQuestId().equals(questId) && !data.isExpired())
                .findFirst()
                .orElse(null);
            
            if (globalData == null) return "0";
            int remaining = globalData.getMaxCompletionCount() - globalData.getCompletedPlayers().size();
            return String.valueOf(Math.max(0, remaining));
        }

        // %excellentquests_global_competitive_<任务ID>_completed_count% - 竞争任务已完成人数
        if (params.startsWith("global_competitive_") && params.endsWith("_completed_count")) {
            String questId = params.substring(19, params.length() - 16);
            Quest quest = questManager.getQuestById(questId);
            if (quest == null || quest.getQuestType() != QuestType.COMPETITIVE) return "0";
            
            GlobalQuestData globalData = questManager.getGlobalQuestManager().getAllGlobalQuests().stream()
                .filter(data -> data.getQuestId().equals(questId) && !data.isExpired())
                .findFirst()
                .orElse(null);
            
            if (globalData == null) return "0";
            return String.valueOf(globalData.getCompletedPlayers().size());
        }

        return null;
    }
}

