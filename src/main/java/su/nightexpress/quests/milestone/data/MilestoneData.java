package su.nightexpress.quests.milestone.data;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.milestone.definition.Milestone;
import su.nightexpress.quests.milestone.definition.MilestoneObjective;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MilestoneData {

    private final String               milestoneId;
    private final Map<String, Integer> progress;
    private final Set<Integer> completedLevels;

    public MilestoneData(@NotNull String milestoneId, @NotNull Map<String, Integer> progress, @NotNull Set<Integer> completedLevels) {
        this.milestoneId = milestoneId;
        this.progress = progress;
        this.completedLevels = completedLevels;
    }

    @NotNull
    public static MilestoneData create(@NotNull Milestone milestone) {
        return new MilestoneData(milestone.getId(), new HashMap<>(), new HashSet<>());
    }

    public void reset() {
        this.resetProgress();
        this.completedLevels.clear();
    }

    public void resetProgress() {
        this.progress.clear();
    }

    public boolean isReady(@NotNull Milestone milestone, int level) {
        // 如果有定义分组，使用分组逻辑
        if (!milestone.getObjectiveGroups().isEmpty()) {
            return this.isReadyWithGroups(milestone, level);
        }
        
        // 否则使用旧逻辑：每个目标独立计算
        return milestone.getObjectiveTable().getEntryMap().entrySet().stream().allMatch(entry -> {
            String fullName = entry.getKey();
            MilestoneObjective objective = entry.getValue();

            int required = objective.getAmount(level);
            if (required <= 0) return true;

            int objectiveProgress = this.getObjectiveProgress(fullName);
            return objectiveProgress >= required;
        });
    }
    
    /**
     * 使用分组逻辑检查是否完成
     */
    private boolean isReadyWithGroups(@NotNull Milestone milestone, int level) {
        // 检查每个分组
        for (su.nightexpress.quests.milestone.definition.ObjectiveGroup group : milestone.getObjectiveGroups()) {
            int required = group.getRequiredAmount(level);
            if (required <= 0) continue;
            
            // 累加组内所有成员的进度
            int totalProgress = 0;
            for (String member : group.getMembers()) {
                totalProgress += this.getObjectiveProgress(member);
            }
            
            // 如果有任何一个组未达到要求，返回 false
            if (totalProgress < required) {
                return false;
            }
        }
        
        return true;
    }

    public double getTotalProgressValue(@NotNull Milestone milestone) {
        return (double) this.countTotalProgress(milestone) / (double) milestone.countTotalRequirements();
    }

    public int countTotalProgress(@NotNull Milestone milestone) {
        int total = 0;

        if (Config.isMilestonesResetProgress()) {
            for (Integer completedLevel : this.completedLevels) {
                total += milestone.countTotalRequirements(completedLevel);
            }
        }

        for (String fullName : milestone.getObjectiveTable().getEntryMap().keySet()) {
            total += this.getObjectiveProgress(fullName);
        }

        return total;
    }

    public int getFirstIncompletedLevel(@NotNull Milestone milestone) {
        for (int level = 1; level < milestone.getLevels() + 1; level++) {
            if (!this.isLevelCompleted(level)) {
                return level;
            }
        }
        return -1;
    }

    public boolean isLevelCompleted(int level) {
        return this.completedLevels.contains(level);
    }

    public void addCompletedLevel(int level) {
        this.completedLevels.add(level);
    }

    public int countCompletedLevels() {
        return this.completedLevels.size();
    }

    @NotNull
    public String getMilestoneId() {
        return this.milestoneId;
    }

    @NotNull
    public Map<String, Integer> getProgress() {
        return this.progress;
    }

    public int getObjectiveProgress(@NotNull String fullName) {
        return this.progress.getOrDefault(LowerCase.INTERNAL.apply(fullName), 0);
    }

    public void addObjectiveProgress(@NotNull String fullName, int amount) {
        int has = this.getObjectiveProgress(fullName);
        this.setObjectiveProgress(fullName, amount + has);
    }

    public void setObjectiveProgress(@NotNull String fullName, int amount) {
        this.progress.put(LowerCase.INTERNAL.apply(fullName), amount);
    }

    @NotNull
    public Set<Integer> getCompletedLevels() {
        return this.completedLevels;
    }
}
