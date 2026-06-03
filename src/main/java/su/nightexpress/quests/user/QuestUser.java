package su.nightexpress.quests.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.db.AbstractUser;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.quests.battlepass.definition.BattlePassSeason;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.milestone.definition.Milestone;
import su.nightexpress.quests.battlepass.data.BattlePassData;

import java.util.*;

public class QuestUser extends AbstractUser {

    private final Map<UUID, BattlePassData>  battlePassData;
    private final Map<UUID, QuestData>       questData;
    private final Map<String, MilestoneData> milestoneData;

    private long newQuestsDate;
    
    // 新增：不同周期的任务重置时间
    private long newDailyQuestsDate;   // 每日任务重置时间
    private long newWeeklyQuestsDate;  // 每周任务重置时间
    private long newMonthlyQuestsDate; // 每月任务重置时间
    
    // 新增：玩家上次登录日期（用于登录任务，格式：yyyyMMdd）
    private String lastLoginDate;

    public QuestUser(@NotNull UUID uuid,
                     @NotNull String name,
                     long dateCreated,
                     long lastOnline,
                     long newQuestsDate,
                     @NotNull Map<UUID, BattlePassData> battlePassData,
                     @NotNull Map<UUID, QuestData> questData,
                     @NotNull Map<String, MilestoneData> milestoneData) {
        super(uuid, name, dateCreated, lastOnline);
        this.setNewQuestsDate(newQuestsDate);
        this.battlePassData = battlePassData;
        this.questData = questData;
        this.milestoneData = milestoneData;
        // 初始化周期任务时间
        this.newDailyQuestsDate = 0L;
        this.newWeeklyQuestsDate = 0L;
        this.newMonthlyQuestsDate = 0L;
        // 初始化上次登录日期
        this.lastLoginDate = "";
    }

    public int countQuestsAmount() {
        return this.questData.size();
    }

    public boolean isNewQuestsTime() {
        return TimeUtil.isPassed(this.newQuestsDate);
    }

    public long getNewQuestsDate() {
        return this.newQuestsDate;
    }

    public void setNewQuestsDate(long newQuestsDate) {
        this.newQuestsDate = newQuestsDate;
    }

    public boolean hasMilestone(@NotNull Milestone milestone) {
        return this.hasMilestone(milestone.getId());
    }

    public boolean hasMilestone(@NotNull String id) {
        return this.milestoneData.containsKey(id);
    }

    public boolean addMilestone(@NotNull Milestone milestone) {
        if (this.hasMilestone(milestone)) return false;

        this.milestoneData.put(milestone.getId(), MilestoneData.create(milestone));
        return true;
    }

    public int getMilestoneCompletedLevels(@NotNull Milestone milestone) {
        MilestoneData data = this.getMilestoneData(milestone);
        return data.countCompletedLevels();
    }

    public boolean isCompleted(@NotNull Milestone milestone) {
        return this.getMilestoneCompletedLevels(milestone) >= milestone.getLevels();
    }

    public void clearQuestData() {
        this.questData.clear();
    }

    public void clearMilestoneData() {
        this.milestoneData.clear();
    }

    @NotNull
    public Map<UUID, BattlePassData> getBattlePassData() {
        return this.battlePassData;
    }

    @NotNull
    public BattlePassData getBattlePassData(@NotNull BattlePassSeason season) {
        return this.battlePassData.computeIfAbsent(season.getId(), k -> BattlePassData.create(season.getExpireDate()));
    }

    @Nullable
    public BattlePassData getBattlePassData(@NotNull UUID id) {
        return this.battlePassData.get(id);
    }

    public void addQuestData(@NotNull QuestData questData) {
        this.questData.put(questData.getId(), questData);
    }

    @NotNull
    public Collection<QuestData> getQuestDatas() {
        return this.questData.values();
    }

    @NotNull
    public Map<UUID, QuestData> getQuestData() {
        return this.questData;
    }

    @NotNull
    public Map<String, MilestoneData> getMilestoneDataMap() {
        return this.milestoneData;
    }

    @NotNull
    public Set<MilestoneData> getMilestoneDatas() {
        return new HashSet<>(this.milestoneData.values());
    }

    @NotNull
    public MilestoneData getMilestoneData(@NotNull Milestone milestone) {
        if (!this.hasMilestone(milestone)) {
            this.addMilestone(milestone);
        }
        return this.milestoneData.get(milestone.getId());
    }

    @Nullable
    public MilestoneData getMilestoneData(@NotNull String id) {
        return this.milestoneData.get(id);
    }
    
    // 新增：周期任务时间管理方法
    public long getNewDailyQuestsDate() {
        return this.newDailyQuestsDate;
    }
    
    public void setNewDailyQuestsDate(long newDailyQuestsDate) {
        this.newDailyQuestsDate = newDailyQuestsDate;
    }
    
    public long getNewWeeklyQuestsDate() {
        return this.newWeeklyQuestsDate;
    }
    
    public void setNewWeeklyQuestsDate(long newWeeklyQuestsDate) {
        this.newWeeklyQuestsDate = newWeeklyQuestsDate;
    }
    
    public long getNewMonthlyQuestsDate() {
        return this.newMonthlyQuestsDate;
    }
    
    public void setNewMonthlyQuestsDate(long newMonthlyQuestsDate) {
        this.newMonthlyQuestsDate = newMonthlyQuestsDate;
    }
    
    public boolean isNewPeriodQuestsTime(@NotNull su.nightexpress.quests.quest.definition.QuestPeriod period) {
        long resetTime;
        switch (period) {
            case DAILY:
                resetTime = this.newDailyQuestsDate;
                break;
            case WEEKLY:
                resetTime = this.newWeeklyQuestsDate;
                break;
            case MONTHLY:
                resetTime = this.newMonthlyQuestsDate;
                break;
            case SEASONAL:
                // 赛季任务不自动重置
                return false;
            default:
                resetTime = this.newQuestsDate;
                break;
        }
        return su.nightexpress.nightcore.util.TimeUtil.isPassed(resetTime);
    }
    
    // 新增：登录日期管理方法
    @NotNull
    public String getLastLoginDate() {
        return this.lastLoginDate;
    }
    
    public void setLastLoginDate(@NotNull String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    
    /**
     * 检查今天是否已经登录过
     * @param today 今天的日期字符串（格式：yyyyMMdd）
     * @return true 如果今天还没登录过
     */
    public boolean isFirstLoginToday(@NotNull String today) {
        return !today.equals(this.lastLoginDate);
    }
}
