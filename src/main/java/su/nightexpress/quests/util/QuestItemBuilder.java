package su.nightexpress.quests.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.quests.config.Perms;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestXPReward;
import su.nightexpress.quests.quest.menu.LoreTemplateManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static su.nightexpress.quests.QuestsPlaceholders.*;

/**
 * 统一的任务物品构建器
 * 使用LoreTemplateManager生成Lore
 */
public class QuestItemBuilder {
    
    private final QuestManager questManager;
    private final LoreTemplateManager loreTemplateManager;
    
    public QuestItemBuilder(@NotNull QuestManager questManager) {
        this.questManager = questManager;
        this.loreTemplateManager = questManager.getLoreTemplateManager();
    }
    
    /**
     * 为全服任务（合作/竞争）构建显示项
     */
    @NotNull
    public NightItem buildGlobalQuestItem(@NotNull Quest quest, @Nullable GlobalQuestData globalData, @NotNull Player player) {
        if (globalData == null) {
            return buildLockedQuestItem(quest);
        }
        
        // 从模板生成Lore
        List<String> templateLore = new ArrayList<>(loreTemplateManager.generateQuestLore(quest.getQuestType()));
        
        // 使用完整的Icon配置（包含HideComponents等属性）
        NightItem item = quest.getIcon();
        
        // 设置名称（使用Quest的Name）
        item.setDisplayName(quest.getName());
        
        // 如果玩家有权限，在lore底部添加任务ID
        if (player.hasPermission(Perms.QUESTS_VIEW_ID)) {
            templateLore.add("");
            templateLore.add("<dark_gray>任务ID: <gray>" + globalData.getId().toString());
        }
        
        // 设置Lore
        item.setLore(templateLore);
        
        // 应用占位符替换
        return applyGlobalQuestReplacements(item, quest, globalData);
    }
    
    /**
     * 为独立任务构建显示项
     */
    @NotNull
    public NightItem buildIndependentQuestItem(@NotNull Quest quest, @NotNull su.nightexpress.quests.quest.data.QuestData questData, @NotNull Player player) {
        // 从模板生成Lore
        List<String> templateLore = new ArrayList<>(loreTemplateManager.generateQuestLore(quest.getQuestType()));
        
        // 使用完整的Icon配置（包含HideComponents等属性）
        NightItem item = quest.getIcon();
        
        // 设置名称
        item.setDisplayName(quest.getName());
        
        // 如果玩家有权限，在lore底部添加任务ID
        if (player.hasPermission(Perms.QUESTS_VIEW_ID)) {
            templateLore.add("");
            templateLore.add("<dark_gray>任务ID: <gray>" + questData.getId().toString());
        }
        
        // 设置Lore
        item.setLore(templateLore);
        
        // 应用占位符替换
        return applyIndependentQuestReplacements(item, quest, questData);
    }
    
    /**
     * 构建锁定状态的任务
     */
    @NotNull
    public NightItem buildLockedQuestItem(@NotNull Quest quest) {
        // 使用完整的Icon配置（包含HideComponents等属性）
        NightItem item = quest.getIcon();
        item.setDisplayName("§7" + quest.getName() + " §8[已锁定]");
        item.setLore(Arrays.asList("§7该任务尚未开启"));
        return item;
    }
    
    /**
     * 应用全服任务的占位符替换
     */
    @NotNull
    private NightItem applyGlobalQuestReplacements(@NotNull NightItem item, @NotNull Quest quest, @NotNull GlobalQuestData globalData) {
        double progress = globalData.getProgressValue();
        int maxOnlinePlayers = questManager.getGlobalQuestManager().getTodayMaxOnlinePlayers();
        
        // 处理Description中的变量
        String questDescription = processDescription(quest, maxOnlinePlayers);
        
        // 获取奖励相关信息
        List<String> rewardLore = quest.getRewardLore();
        QuestXPReward xpReward = quest.getBattlePassXPReward();
        int unitsWorth = globalData.countUnitsWorth();
        int xpAmount = xpReward != null ? xpReward.getXP(unitsWorth) : 0;
        boolean hasBattleSeason = questManager.plugin().getBattlePassManager() != null && 
                                 questManager.plugin().getBattlePassManager().getSeason() != null;
        
        // 构建排名信息
        Map<String, String> rankReplacers = buildRankingReplacers(quest, globalData);
        
        // 基础替换
        item.replacement(replacer -> replacer
            .replace(QUEST_DATA_ID, globalData.getId().toString().substring(0, 8))
            .replace(GENERIC_TIMELEFT, () -> TimeFormats.formatDuration(globalData.getExpireDate(), TimeFormatType.LITERAL))
            .replace(GENERIC_PROGRESS_BAR, () -> MenuUtils.buildProgressBar(progress))
            .replace(GENERIC_PROGRESS, () -> NumberUtil.format(progress * 100D))
            .replace(GENERIC_OBJECTIVES, formatGlobalObjectives(quest, globalData))
            .replace(GENERIC_REWARDS, rewardLore)
            .replace(GENERIC_BATTLE_PASS_REWARDS, hasBattleSeason ? MenuUtils.formatBattlePassRewards(xpReward, unitsWorth) : Collections.emptyList())
            .replace(GENERIC_XP, NumberUtil.format(xpAmount))
            .replace("%maxplayer%", String.valueOf(maxOnlinePlayers))
            .replace("%completed_players%", String.valueOf(globalData.getCompletedPlayerCount()))
            .replace("%max_players%", String.valueOf(globalData.getMaxCompletionCount()))
            .replace("%rewards_inline%", String.join(", ", rewardLore))
            .replace("%quest_description%", questDescription)
            .replace(quest.replacePlaceholders())
        );
        
        // 动态排名占位符替换
        for (Map.Entry<String, String> entry : rankReplacers.entrySet()) {
            item.replacement(replacer -> replacer.replace(entry.getKey(), entry.getValue()));
        }
        
        return item;
    }
    
    /**
     * 应用独立任务的占位符替换
     */
    @NotNull
    private NightItem applyIndependentQuestReplacements(@NotNull NightItem item, @NotNull Quest quest, @NotNull su.nightexpress.quests.quest.data.QuestData questData) {
        double progress = questData.getProgressValue();
        int maxOnlinePlayers = questManager.getGlobalQuestManager().getTodayMaxOnlinePlayers();
        String questDescription = processDescription(quest, maxOnlinePlayers);
        
        // 获取奖励描述，如果为空则提供默认值
        List<String> questRewardLore = quest.getRewardLore();
        final List<String> rewardLore = questRewardLore.isEmpty() ? 
            Collections.singletonList("查看配置文件") : questRewardLore;
        
        // 确定任务状态
        String status = getQuestStatus(questData);
        
        item.replacement(replacer -> replacer
            .replace(QUEST_DATA_ID, questData.getId().toString().substring(0, 8))
            .replace(GENERIC_PROGRESS_BAR, () -> MenuUtils.buildProgressBar(progress))
            .replace(GENERIC_PROGRESS, () -> NumberUtil.format(progress * 100D))
            .replace(GENERIC_OBJECTIVES, MenuUtils.formatObjectives(quest, questData))
            .replace(GENERIC_REWARDS, rewardLore)
            .replace("%status%", status)
            .replace("%rewards_inline%", String.join(", ", rewardLore))
            .replace("%quest_description%", questDescription)
            .replace(quest.replacePlaceholders())
        );
        
        return item;
    }
    
    /**
     * 获取任务状态文本
     */
    @NotNull
    private String getQuestStatus(@NotNull su.nightexpress.quests.quest.data.QuestData questData) {
        if (!questData.isActive()) {
            return "<gray>等待领取";
        }
        
        if (questData.isExpired()) {
            return "<red>已过期";
        }
        
        if (questData.isCompleted()) {
            return "<green>已完成";
        }
        
        return "<yellow>进行中";
    }
    
    /**
     * 处理Description中的变量并计算数学表达式
     */
    @NotNull
    private String processDescription(@NotNull Quest quest, int maxOnlinePlayers) {
        List<String> processedDescription = new ArrayList<>();
        for (String descLine : quest.getDescription()) {
            String processed = descLine.replace("%maxplayer%", String.valueOf(maxOnlinePlayers));
            processed = evaluateMathExpressions(processed);
            processedDescription.add(processed);
        }
        return String.join("\n", processedDescription);
    }
    
    /**
     * 构建排名占位符替换表
     */
    @NotNull
    private Map<String, String> buildRankingReplacers(@NotNull Quest quest, @NotNull GlobalQuestData globalData) {
        Map<String, String> replacers = new HashMap<>();
        
        List<Map.Entry<UUID, Integer>> topPlayers = globalData.getSortedPlayerContributions();
        Map<Integer, List<String>> allRankingRewards = quest.getRankingRewards();
        int maxRank = allRankingRewards.isEmpty() ? 0 : allRankingRewards.keySet().stream().max(Integer::compareTo).orElse(0);
        
        for (int rank = 1; rank <= maxRank; rank++) {
            String playerName = "暂无";
            String contribution = "0";
            String rewards = "暂无";
            
            // 获取该排名的玩家信息
            if (rank <= topPlayers.size()) {
                Map.Entry<UUID, Integer> entry = topPlayers.get(rank - 1);
                UUID playerId = entry.getKey();
                String name = questManager.plugin().getServer().getOfflinePlayer(playerId).getName();
                playerName = name != null ? name : "暂无";
                contribution = String.valueOf(entry.getValue());
            }
            
            // 获取该排名的奖励描述
            List<String> rewardLore = quest.getRankingRewardLoreForRank(rank);
            if (rewardLore != null && !rewardLore.isEmpty()) {
                rewards = String.join(", ", rewardLore);
            }
            
            // 存储占位符
            replacers.put("%rank_" + rank + "_player%", playerName);
            replacers.put("%rank_" + rank + "_contribution%", contribution);
            replacers.put("%rank_" + rank + "_rewards%", rewards);
        }
        
        return replacers;
    }
    
    /**
     * 格式化全服任务的目标显示
     */
    @NotNull
    private List<String> formatGlobalObjectives(@NotNull Quest quest, @NotNull GlobalQuestData globalData) {
        List<String> result = new ArrayList<>();
        
        globalData.getObjectiveCounterMap().forEach((objectId, counter) -> {
            int required = counter.getRequired();
            int current = counter.getCompleted();
            
            String displayName = getObjectiveDisplayName(quest.getType(), objectId);
            result.add("  §7▸ §e" + displayName + " §8(" + current + "/" + required + ")");
        });
        
        return result;
    }
    
    /**
     * 获取目标的显示名称
     */
    @NotNull
    private String getObjectiveDisplayName(@NotNull su.nightexpress.quests.task.TaskType<?, ?> type, @NotNull String fullName) {
        if (fullName.endsWith(":*") || fullName.endsWith(":all")) {
            return "任意" + type.getId();
        }
        
        su.nightexpress.quests.task.adapter.Adapter<?, ?> adapter = type.getAdapterFamily().getAdapterForName(fullName);
        if (adapter == null) {
            return fullName;
        }
        
        String localizedName = adapter.getLocalizedName(fullName);
        return localizedName != null ? localizedName : fullName;
    }
    
    /**
     * 计算字符串中的数学表达式
     */
    @NotNull
    private String evaluateMathExpressions(@NotNull String text) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*([+\\-*/])\\s*(\\d+)");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            try {
                int num1 = Integer.parseInt(matcher.group(1));
                String operator = matcher.group(2);
                int num2 = Integer.parseInt(matcher.group(3));
                
                int calculated = switch (operator) {
                    case "+" -> num1 + num2;
                    case "-" -> num1 - num2;
                    case "*" -> num1 * num2;
                    case "/" -> num2 != 0 ? num1 / num2 : num1;
                    default -> num1;
                };
                
                matcher.appendReplacement(result, String.valueOf(calculated));
            } catch (NumberFormatException e) {
                // 如果解析失败，保持原样
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}


