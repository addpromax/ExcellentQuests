package su.nightexpress.quests.milestone.definition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 目标组：将多个目标组合在一起，它们的进度会累加计算
 */
public class ObjectiveGroup {
    
    private final String groupName;
    private final List<String> members;  // 组内成员的完整名称列表
    private final MilestoneObjective requirement;  // 组的要求值
    
    public ObjectiveGroup(@NotNull String groupName, @NotNull List<String> members, @NotNull MilestoneObjective requirement) {
        this.groupName = groupName;
        this.members = new ArrayList<>(members);
        this.requirement = requirement;
    }
    
    @NotNull
    public String getGroupName() {
        return groupName;
    }
    
    @NotNull
    public List<String> getMembers() {
        return members;
    }
    
    @NotNull
    public MilestoneObjective getRequirement() {
        return requirement;
    }
    
    /**
     * 获取指定等级的要求值
     */
    public int getRequiredAmount(int level) {
        return requirement.getAmount(level);
    }
    
    /**
     * 检查指定的目标是否属于这个组
     */
    public boolean contains(@NotNull String fullName) {
        return members.contains(fullName);
    }
}

