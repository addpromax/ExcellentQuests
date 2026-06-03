package su.nightexpress.quests.quest;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.random.Rnd;
import su.nightexpress.nightcore.util.wrapper.UniInt;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.data.QuestCounter;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestObjective;
import su.nightexpress.quests.quest.definition.QuestObjectiveTable;
import su.nightexpress.quests.quest.definition.QuestType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局任务管理器
 * 管理合作任务和竞争任务的全局数据
 */
public class GlobalQuestManager {
    
    private final QuestsPlugin plugin;
    private final Map<UUID, GlobalQuestData> globalQuestData;
    private final Map<String, Integer> dailyMaxOnlineCount; // 每日最高在线人数记录
    private long lastResetDate;
    
    public GlobalQuestManager(@NotNull QuestsPlugin plugin) {
        this.plugin = plugin;
        this.globalQuestData = new ConcurrentHashMap<>();
        this.dailyMaxOnlineCount = new ConcurrentHashMap<>();
        this.lastResetDate = System.currentTimeMillis();
    }
    
    /**
     * 创建全局任务数据
     */
    @Nullable
    public GlobalQuestData createGlobalQuest(@NotNull Quest quest, long expireDate) {
        plugin.debug("GlobalQuestManager.createGlobalQuest() 开始");
        plugin.debug("  - 任务ID: " + quest.getId());
        
        QuestType questType = quest.getQuestType();
        plugin.debug("  - 任务类型: " + questType);
        
        if (questType == QuestType.INDEPENDENT) {
            plugin.debug("  - 独立任务不需要全局数据，返回 null");
            return null; // 独立任务不需要全局数据
        }
        
        UUID id = UUID.randomUUID();
        Map<String, QuestCounter> objectives = new LinkedHashMap<>();
        
        // 获取当前最高在线人数
        int maxOnlinePlayers = getCurrentMaxOnlinePlayers();
        updateDailyMaxOnline(maxOnlinePlayers);
        plugin.debug("  - 当前在线人数: " + maxOnlinePlayers);
        
        final double scale;
        boolean skipObjectives = false;  // 是否跳过目标生成
        
        if (questType == QuestType.COOPERATIVE) {
            if (maxOnlinePlayers == 0) {
                // 合作任务：如果没有玩家在线，创建任务但不生成目标
                // 等待第一个玩家上线时再初始化目标
                skipObjectives = true;
                scale = 1.0;
                plugin.debug("  - 合作任务且无玩家在线，跳过目标生成（等待玩家上线初始化）");
            } else {
                // 合作任务：根据公式计算倍数
                scale = quest.calculateCooperativeScale(maxOnlinePlayers);
                plugin.debug("  - 合作任务计算 scale: " + scale + " (基于在线人数: " + maxOnlinePlayers + ")");
            }
        } else {
            scale = 1.0;
        }
        
        // 创建目标
        if (!skipObjectives) {
            plugin.debug("  - 开始生成任务目标...");
            
            Map<String, Double> objectiveByWeight = new HashMap<>();
            quest.getObjectiveTable().getEntryMap().forEach((fullName, objective) -> {
                objectiveByWeight.put(fullName, objective.weight());
            });
            
            int objectivesAmount = quest.getObjectivesAmount().roll();
            int totalObjectivesInTable = quest.getObjectiveTable().getEntryMap().size();
            
            plugin.debug("  - 目标表总数: " + totalObjectivesInTable);
            plugin.debug("  - 需要生成目标数: " + objectivesAmount);
            plugin.debug("  - Scale倍数: " + scale);
            
            int generatedCount = 0;
            int skippedZeroAmount = 0;
            int skippedNull = 0;
            
            while (objectivesAmount > 0 && !objectiveByWeight.isEmpty()) {
                String fullName = su.nightexpress.nightcore.util.random.Rnd.getByWeight(objectiveByWeight);
                objectiveByWeight.remove(fullName);
                
                plugin.debug("  - 尝试生成目标: " + fullName);
                
                QuestObjective objective = quest.getObjectiveTable().getEntry(fullName);
                if (objective == null) {
                    plugin.debug("    × 目标为 null (跳过)");
                    skippedNull++;
                    continue;
                }
                
                int amount = objective.rollAmount(scale);
                plugin.debug("    - rollAmount结果: " + amount + " (min=" + objective.min() + ", max=" + objective.max() + ", scale=" + scale + ")");
                
                if (amount <= 0) {
                    plugin.debug("    × 数量 <= 0 (跳过)");
                    skippedZeroAmount++;
                    continue;
                }
                
                double unitWorth = objective.unitWorth();
                objectives.put(LowerCase.INTERNAL.apply(fullName), QuestCounter.create(amount, unitWorth));
                generatedCount++;
                plugin.debug("    ✓ 成功添加目标: " + fullName + " = " + amount + " (unitWorth=" + unitWorth + ")");
                objectivesAmount--;
            }
            
            plugin.debug("  - 目标生成完成统计:");
            plugin.debug("    * 成功: " + generatedCount);
            plugin.debug("    * 跳过(数量<=0): " + skippedZeroAmount);
            plugin.debug("    * 跳过(null): " + skippedNull);
            plugin.debug("    * 最终目标数: " + objectives.size());
            
            if (objectives.isEmpty()) {
                plugin.error("=====================================");
                plugin.error("任务 '" + quest.getId() + "' 创建失败: 没有有效的目标 (objectives)!");
                plugin.error("  - 目标表大小: " + totalObjectivesInTable);
                plugin.error("  - 需要生成目标数: " + quest.getObjectivesAmount().roll());
                plugin.error("  - scale: " + scale);
                plugin.error("  - 任务类型: " + questType);
                
                // 详细输出目标表内容
                plugin.error("  - 目标表详情:");
                quest.getObjectiveTable().getEntryMap().forEach((fullName, objective) -> {
                    plugin.error("    * " + fullName + ":");
                    plugin.error("      - min: " + objective.min() + ", max: " + objective.max());
                    plugin.error("      - weight: " + objective.weight());
                    plugin.error("      - rollAmount(scale=" + scale + "): " + objective.rollAmount(scale));
                });
                
                plugin.error("  - 可能的原因:");
                plugin.error("    1. Objectives.List 为空 (目标表大小=" + totalObjectivesInTable + ")");
                plugin.error("    2. 所有目标的 amount 都为 0 或负数");
                plugin.error("    3. 目标数据格式不正确（应为字符串格式，如 'coal_ore: \"64;64 1.0 1.0\"'）");
                plugin.error("  - 请检查配置文件: quests/" + quest.getQuestType().name().toLowerCase() + "/" + quest.getId() + ".yml");
                plugin.error("=====================================");
                return null;
            }
        }
        
        GlobalQuestData globalData;
        if (questType == QuestType.COOPERATIVE) {
            plugin.debug("  - 创建合作任务全局数据");
            plugin.debug("    * 在线人数基准: " + maxOnlinePlayers);
            globalData = GlobalQuestData.createCooperative(id, quest.getId(), objectives, maxOnlinePlayers, expireDate);
        } else { // COMPETITIVE
            int maxCompletions = quest.calculateCompetitiveMaxCompletions(maxOnlinePlayers);
            plugin.debug("  - 创建竞争任务全局数据");
            plugin.debug("    * 最大完成人数: " + maxCompletions);
            globalData = GlobalQuestData.createCompetitive(id, quest.getId(), objectives, maxCompletions, expireDate);
        }
        
        this.globalQuestData.put(id, globalData);
        plugin.debug("GlobalQuestManager.createGlobalQuest() 完成 - 全局ID: " + id);
        return globalData;
    }
    
    /**
     * 获取全局任务数据
     */
    @Nullable
    public GlobalQuestData getGlobalQuestData(@NotNull UUID id) {
        return this.globalQuestData.get(id);
    }
    
    /**
     * 添加全局任务进度
     */
    public synchronized void addProgress(@NotNull UUID globalQuestId, @NotNull String fullName, int amount) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        if (data != null && data.isActive() && !data.isExpired()) {
            data.addCompleted(fullName, amount);
        }
    }
    
    /**
     * 检查玩家是否可以完成竞争任务
     */
    public boolean canPlayerCompleteCompetitive(@NotNull UUID globalQuestId, @NotNull UUID playerId) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        if (data == null) return false;
        
        if (data.getQuestType() != QuestType.COMPETITIVE) return true;
        
        return data.canPlayerComplete(playerId) && !data.hasPlayerCompleted(playerId);
    }
    
    /**
     * 标记玩家完成竞争任务
     */
    public void markPlayerCompleted(@NotNull UUID globalQuestId, @NotNull UUID playerId) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        if (data != null && data.getQuestType() == QuestType.COMPETITIVE) {
            data.addCompletedPlayer(playerId);
        }
    }
    
    /**
     * 检查玩家是否已完成竞争任务
     */
    public boolean hasPlayerCompleted(@NotNull UUID globalQuestId, @NotNull UUID playerId) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        return data != null && data.hasPlayerCompleted(playerId);
    }
    
    /**
     * 清理过期的全局任务
     */
    public void cleanExpiredQuests() {
        List<GlobalQuestData> expiredQuests = this.globalQuestData.values().stream()
            .filter(GlobalQuestData::isExpired)
            .toList();
        
        // 从内存和数据库中移除过期任务
        for (GlobalQuestData expiredQuest : expiredQuests) {
            this.globalQuestData.remove(expiredQuest.getId());
            this.plugin.getDataHandler().removeGlobalQuest(expiredQuest);
            this.plugin.debug("[清理过期任务] 已删除过期的全局任务: " + expiredQuest.getQuestId() + " (全局ID: " + expiredQuest.getId() + ")");
        }
    }
    
    /**
     * 清空所有全局任务
     */
    public void clearAllQuests() {
        this.globalQuestData.clear();
    }
    
    /**
     * 获取当前在线人数
     */
    private int getCurrentMaxOnlinePlayers() {
        return Players.getOnline().size();
    }
    
    /**
     * 更新每日最高在线人数
     */
    public void updateDailyMaxOnline(int currentOnline) {
        String dateKey = getCurrentDateKey();
        dailyMaxOnlineCount.merge(dateKey, currentOnline, Math::max);
    }
    
    /**
     * 获取今日最高在线人数
     */
    public int getTodayMaxOnlinePlayers() {
        String dateKey = getCurrentDateKey();
        return dailyMaxOnlineCount.getOrDefault(dateKey, getCurrentMaxOnlinePlayers());
    }
    
    /**
     * 重置每日数据
     */
    public void resetDailyData() {
        String dateKey = getCurrentDateKey();
        dailyMaxOnlineCount.entrySet().removeIf(entry -> !entry.getKey().equals(dateKey));
        this.lastResetDate = System.currentTimeMillis();
    }
    
    /**
     * 获取当前日期的key
     */
    private String getCurrentDateKey() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%d-%02d-%02d", 
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * 为合作任务初始化目标
     * @param quest 任务配置
     * @param globalData 全局任务数据
     * @param maxOnlinePlayers 当日最高在线人数
     * @return 是否成功初始化
     */
    public boolean initializeCooperativeQuestObjectives(@NotNull Quest quest, @NotNull GlobalQuestData globalData, int maxOnlinePlayers) {
        if (quest.getQuestType() != QuestType.COOPERATIVE) {
            return false;
        }
        
        // 计算合作任务的目标数量倍数
        double scale = quest.calculateCooperativeScale(maxOnlinePlayers);
        
        this.plugin.info("[合作任务初始化]   - 在线人数基准: " + maxOnlinePlayers);
        this.plugin.info("[合作任务初始化]   - 计算倍数: " + scale);
        
        // 为任务生成目标
        Map<String, QuestCounter> objectives = new LinkedHashMap<>();
        QuestObjectiveTable objectiveTable = quest.getObjectiveTable();
        UniInt objectivesAmount = quest.getObjectivesAmount();
        
        Map<String, Double> objectiveByWeight = new HashMap<>();
        objectiveTable.getEntryMap().forEach((fullName, objective) -> {
            objectiveByWeight.put(fullName, objective.weight());
        });
        
        int objectivesToGenerate = objectivesAmount.roll();
        this.plugin.info("[合作任务初始化]   - 需要生成目标数: " + objectivesToGenerate);
        
        while (objectivesToGenerate > 0 && !objectiveByWeight.isEmpty()) {
            String fullName = Rnd.getByWeight(objectiveByWeight);
            objectiveByWeight.remove(fullName);
            
            QuestObjective objective = objectiveTable.getEntry(fullName);
            if (objective == null) continue;
            
            int amount = objective.rollAmount(scale);
            if (amount <= 0) continue;
            
            double unitWorth = objective.unitWorth();
            objectives.put(LowerCase.INTERNAL.apply(fullName), QuestCounter.create(amount, unitWorth));
            objectivesToGenerate--;
            
            this.plugin.info("[合作任务初始化]     * 添加目标: " + fullName + " = " + amount);
        }
        
        if (objectives.isEmpty()) {
            return false;
        }
        
        // 更新全局任务的目标
        globalData.getObjectiveCounterMap().clear();
        globalData.getObjectiveCounterMap().putAll(objectives);
        globalData.setNeedsObjectiveInitialization(false);
        globalData.setMaxOnlinePlayerCount(maxOnlinePlayers);
        
        return true;
    }
    
    /**
     * 获取所有全局任务数据
     */
    @NotNull
    public Collection<GlobalQuestData> getAllGlobalQuests() {
        return new ArrayList<>(this.globalQuestData.values());
    }
    
    /**
     * 按类型获取全局任务数据
     */
    @NotNull
    public List<GlobalQuestData> getGlobalQuestsByType(@NotNull QuestType questType) {
        return this.globalQuestData.values().stream()
            .filter(data -> data.getQuestType() == questType)
            .toList();
    }
    
    /**
     * 添加全局任务数据（用于数据加载）
     */
    public void addGlobalQuestData(@NotNull GlobalQuestData data) {
        this.globalQuestData.put(data.getId(), data);
    }
    
    /**
     * 移除全局任务数据
     * @return 是否成功移除
     */
    public boolean removeGlobalQuestData(@NotNull UUID id) {
        GlobalQuestData removed = this.globalQuestData.remove(id);
        // 同时从数据库删除
        if (removed != null) {
            this.plugin.getDataHandler().removeGlobalQuest(removed);
            return true;
        }
        return false;
    }
    
    /**
     * 清空所有全局任务数据（用于reload时清理）
     */
    public void clearAllGlobalQuests() {
        this.globalQuestData.clear();
        this.plugin.debug("已清空所有全局任务数据");
    }
    
    /**
     * 获取全局任务的当前进度
     */
    public int getGlobalProgress(@NotNull UUID globalQuestId, @NotNull String fullName) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        return data != null ? data.getCurrent(fullName) : 0;
    }
    
    /**
     * 获取全局任务的需求数量
     */
    public int getGlobalRequired(@NotNull UUID globalQuestId, @NotNull String fullName) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        return data != null ? data.getRequired(fullName) : 0;
    }
    
    /**
     * 获取全局任务的完成状态
     */
    public boolean isGlobalQuestCompleted(@NotNull UUID globalQuestId) {
        GlobalQuestData data = this.globalQuestData.get(globalQuestId);
        return data != null && data.isCompleted();
    }
    
    /**
     * 初始化合作任务的目标（当第一个玩家上线时调用）
     */
    public void initializeCooperativeQuestObjectives(@NotNull Quest quest, @NotNull GlobalQuestData globalData) {
        plugin.debug("initializeCooperativeQuestObjectives() 被调用");
        plugin.debug("  - 任务ID: " + quest.getId());
        plugin.debug("  - 全局任务ID: " + globalData.getId());
        
        if (quest.getQuestType() != QuestType.COOPERATIVE) {
            plugin.debug("  - 不是合作任务，跳过初始化");
            return;
        }
        
        if (!globalData.needsObjectiveInitialization()) {
            plugin.debug("  - 目标已经初始化过了，跳过");
            return;
        }
        
        plugin.debug("  - 开始初始化合作任务目标...");
        
        // 获取当前在线人数
        int maxOnlinePlayers = Math.max(1, getCurrentMaxOnlinePlayers());
        updateDailyMaxOnline(maxOnlinePlayers);
        plugin.debug("  - 当前在线人数: " + maxOnlinePlayers);
        
        // 计算 scale
        double scale = quest.calculateCooperativeScale(maxOnlinePlayers);
        plugin.debug("  - 计算得到的 scale: " + scale);
        
        // 生成目标
        Map<String, Double> objectiveByWeight = new HashMap<>();
        quest.getObjectiveTable().getEntryMap().forEach((fullName, objective) -> {
            objectiveByWeight.put(fullName, objective.weight());
        });
        
        int objectivesAmount = quest.getObjectivesAmount().roll();
        int totalObjectivesInTable = quest.getObjectiveTable().getEntryMap().size();
        
        plugin.debug("  - 目标表总数: " + totalObjectivesInTable);
        plugin.debug("  - 需要生成目标数: " + objectivesAmount);
        
        int generatedCount = 0;
        int skippedZeroAmount = 0;
        int skippedNull = 0;
        
        while (objectivesAmount > 0 && !objectiveByWeight.isEmpty()) {
            String fullName = su.nightexpress.nightcore.util.random.Rnd.getByWeight(objectiveByWeight);
            objectiveByWeight.remove(fullName);
            
            plugin.debug("  - 尝试生成目标: " + fullName);
            
            QuestObjective objective = quest.getObjectiveTable().getEntry(fullName);
            if (objective == null) {
                plugin.debug("    × 目标为 null (跳过)");
                skippedNull++;
                continue;
            }
            
            int amount = objective.rollAmount(scale);
            plugin.debug("    - rollAmount结果: " + amount + " (min=" + objective.min() + ", max=" + objective.max() + ", scale=" + scale + ")");
            
            if (amount <= 0) {
                plugin.debug("    × 数量 <= 0 (跳过)");
                skippedZeroAmount++;
                continue;
            }
            
            double unitWorth = objective.unitWorth();
            globalData.getObjectiveCounterMap().put(LowerCase.INTERNAL.apply(fullName), QuestCounter.create(amount, unitWorth));
            generatedCount++;
            plugin.debug("    ✓ 成功添加目标: " + fullName + " = " + amount + " (unitWorth=" + unitWorth + ")");
            objectivesAmount--;
        }
        
        plugin.debug("  - 目标初始化完成统计:");
        plugin.debug("    * 成功: " + generatedCount);
        plugin.debug("    * 跳过(数量<=0): " + skippedZeroAmount);
        plugin.debug("    * 跳过(null): " + skippedNull);
        plugin.debug("    * 最终目标数: " + globalData.getObjectiveCounterMap().size());
        
        if (globalData.getObjectiveCounterMap().isEmpty()) {
            plugin.error("[DEBUG] !!!警告!!! 合作任务目标初始化后仍然为空！");
            plugin.error("[DEBUG]   - 这会导致任务显示 NaN");
            plugin.error("[DEBUG]   - 请检查任务配置文件: quests/cooperative/" + quest.getId() + ".yml");
        }
        
        // 标记目标已初始化
        globalData.setNeedsObjectiveInitialization(false);
        plugin.debug("initializeCooperativeQuestObjectives() 完成");
    }
}
