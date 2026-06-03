package su.nightexpress.quests.quest.data;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.quests.quest.definition.QuestType;

import java.util.*;

/**
 * 全局任务数据类
 * 用于存储合作任务和竞争任务的全局数据
 */
public class GlobalQuestData {
    
    private final UUID id;
    private final String questId;
    private final QuestType questType;
    private final Map<String, QuestCounter> objectiveCounter;
    private final int maxCompletionCount; // 竞争任务：最大完成人数
    private final Set<UUID> completedPlayers; // 竞争任务：已完成的玩家列表
    private int maxOnlinePlayerCount; // 合作任务：当日最高在线人数（非final，可以在初始化时更新）
    private final Map<UUID, Integer> playerContributions; // 合作任务：玩家贡献度追踪
    
    private boolean active;
    private long expireDate;
    private long createDate;
    private boolean needsObjectiveInitialization; // 合作任务：是否需要在第一个玩家上线时初始化目标
    
    public GlobalQuestData(@NotNull UUID id,
                          @NotNull String questId,
                          @NotNull QuestType questType,
                          @NotNull Map<String, QuestCounter> objectiveCounter,
                          int maxCompletionCount,
                          @NotNull Set<UUID> completedPlayers,
                          int maxOnlinePlayerCount,
                          @NotNull Map<UUID, Integer> playerContributions,
                          boolean active,
                          long expireDate,
                          long createDate) {
        this.id = id;
        this.questId = questId;
        this.questType = questType;
        this.objectiveCounter = objectiveCounter;
        this.maxCompletionCount = maxCompletionCount;
        this.completedPlayers = completedPlayers;
        this.maxOnlinePlayerCount = maxOnlinePlayerCount;
        this.playerContributions = playerContributions;
        this.active = active;
        this.expireDate = expireDate;
        this.createDate = createDate;
    }
    
    @NotNull
    public static GlobalQuestData createCooperative(@NotNull UUID id,
                                                    @NotNull String questId,
                                                    @NotNull Map<String, QuestCounter> objectives,
                                                    int maxOnlinePlayerCount,
                                                    long expireDate) {
        GlobalQuestData data = new GlobalQuestData(id, questId, QuestType.COOPERATIVE, objectives, 
            0, new HashSet<>(), maxOnlinePlayerCount, new HashMap<>(), true, expireDate, System.currentTimeMillis());
        // 如果目标为空，标记需要初始化
        data.needsObjectiveInitialization = objectives.isEmpty();
        return data;
    }
    
    @NotNull
    public static GlobalQuestData createCompetitive(@NotNull UUID id,
                                                    @NotNull String questId,
                                                    @NotNull Map<String, QuestCounter> objectives,
                                                    int maxCompletionCount,
                                                    long expireDate) {
        return new GlobalQuestData(id, questId, QuestType.COMPETITIVE, objectives,
            maxCompletionCount, new HashSet<>(), 0, new HashMap<>(), true, expireDate, System.currentTimeMillis());
    }
    
    public double getProgressValue() {
        int total = this.countTotalRequirement();
        if (total == 0) {
            return 0.0; // 避免除以0导致NaN
        }
        return (double) this.countTotalProgress() / (double) total;
    }
    
    public int countTotalRequirement() {
        return this.objectiveCounter.values().stream().mapToInt(QuestCounter::getRequired).sum();
    }
    
    public int countTotalProgress() {
        return this.objectiveCounter.values().stream().mapToInt(QuestCounter::getCompleted).sum();
    }
    
    public int countUnitsWorth() {
        return this.objectiveCounter.values().stream().mapToInt(counter -> (int) Math.ceil(counter.getRequired() * counter.getUnitWorth())).sum();
    }
    
    public boolean isCompleted() {
        // 如果目标为空或需要初始化，则任务不算完成
        if (this.objectiveCounter.isEmpty() || this.needsObjectiveInitialization) {
            return false;
        }
        return this.objectiveCounter.values().stream().allMatch(QuestCounter::isCompleted);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expireDate;
    }
    
    public boolean canPlayerComplete(@NotNull UUID playerId) {
        if (this.questType == QuestType.COMPETITIVE) {
            // 竞争任务：检查是否还有名额
            return this.completedPlayers.size() < this.maxCompletionCount;
        }
        return true;
    }
    
    public boolean hasPlayerCompleted(@NotNull UUID playerId) {
        return this.completedPlayers.contains(playerId);
    }
    
    public void addCompletedPlayer(@NotNull UUID playerId) {
        this.completedPlayers.add(playerId);
    }
    
    public int getRequired(@NotNull String fullName) {
        QuestCounter counter = this.getObjectiveCounter(fullName);
        return counter != null ? counter.getRequired() : 0;
    }
    
    public int getCurrent(@NotNull String fullName) {
        QuestCounter counter = this.getObjectiveCounter(fullName);
        return counter != null ? counter.getCompleted() : 0;
    }
    
    public synchronized void addCompleted(@NotNull String fullName, int amount) {
        QuestCounter counter = this.getObjectiveCounter(fullName);
        if (counter != null) {
            counter.addCompleted(amount);
        }
    }
    
    /**
     * 添加玩家贡献度
     * @param playerId 玩家UUID
     * @param contribution 贡献数量
     */
    public synchronized void addPlayerContribution(@NotNull UUID playerId, int contribution) {
        this.playerContributions.merge(playerId, contribution, Integer::sum);
    }
    
    /**
     * 获取玩家贡献度
     * @param playerId 玩家UUID
     * @return 贡献数量
     */
    public int getPlayerContribution(@NotNull UUID playerId) {
        return this.playerContributions.getOrDefault(playerId, 0);
    }
    
    /**
     * 获取所有玩家贡献度（按贡献从高到低排序）
     * @return 排序后的玩家贡献列表
     */
    @NotNull
    public List<Map.Entry<UUID, Integer>> getSortedPlayerContributions() {
        return this.playerContributions.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .toList();
    }
    
    /**
     * 获取玩家排名（从1开始）
     * @param playerId 玩家UUID
     * @return 排名，如果玩家没有贡献则返回-1
     */
    public int getPlayerRank(@NotNull UUID playerId) {
        List<Map.Entry<UUID, Integer>> sorted = getSortedPlayerContributions();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(playerId)) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * 获取所有玩家贡献度Map
     * @return 玩家贡献度Map
     */
    @NotNull
    public Map<UUID, Integer> getPlayerContributions() {
        return new HashMap<>(this.playerContributions);
    }
    
    public boolean hasObjective(@NotNull String fullName) {
        return this.getObjectiveCounter(fullName) != null;
    }
    
    public QuestCounter getObjectiveCounter(@NotNull String fullName) {
        return this.objectiveCounter.get(LowerCase.INTERNAL.apply(fullName));
    }
    
    @NotNull
    public UUID getId() {
        return this.id;
    }
    
    @NotNull
    public String getQuestId() {
        return this.questId;
    }
    
    @NotNull
    public QuestType getQuestType() {
        return this.questType;
    }
    
    @NotNull
    public Map<String, QuestCounter> getObjectiveCounterMap() {
        return this.objectiveCounter;
    }
    
    public int getMaxCompletionCount() {
        return this.maxCompletionCount;
    }
    
    @NotNull
    public Set<UUID> getCompletedPlayers() {
        return new HashSet<>(this.completedPlayers);
    }
    
    public int getCompletedPlayerCount() {
        return this.completedPlayers.size();
    }
    
    public int getMaxOnlinePlayerCount() {
        return this.maxOnlinePlayerCount;
    }
    
    public void setMaxOnlinePlayerCount(int count) {
        this.maxOnlinePlayerCount = count;
    }
    
    public boolean isActive() {
        return this.active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public long getExpireDate() {
        return this.expireDate;
    }
    
    public void setExpireDate(long expireDate) {
        this.expireDate = expireDate;
    }
    
    public long getCreateDate() {
        return this.createDate;
    }
    
    public boolean needsObjectiveInitialization() {
        return this.needsObjectiveInitialization;
    }
    
    public void setNeedsObjectiveInitialization(boolean needsObjectiveInitialization) {
        this.needsObjectiveInitialization = needsObjectiveInitialization;
    }
}
