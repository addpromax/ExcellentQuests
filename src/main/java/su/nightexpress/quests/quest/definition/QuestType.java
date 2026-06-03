package su.nightexpress.quests.quest.definition;

import org.jetbrains.annotations.NotNull;

/**
 * 任务类型枚举
 * INDEPENDENT - 独立任务：每个玩家的进度独立
 * COOPERATIVE - 合作任务：全服玩家共享进度，目标数量基于当日最高在线人数
 * COMPETITIVE - 竞争任务：限制完成人数，先到先得
 */
public enum QuestType {
    
    INDEPENDENT("独立任务", "每个玩家的进度是独立的"),
    COOPERATIVE("合作任务", "所有玩家共享进度"),
    COMPETITIVE("竞争任务", "只有部分玩家可以完成");
    
    private final String displayName;
    private final String description;
    
    QuestType(@NotNull String displayName, @NotNull String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    @NotNull
    public String getDisplayName() {
        return this.displayName;
    }
    
    @NotNull
    public String getDescription() {
        return this.description;
    }
    
    @NotNull
    public static QuestType fromString(@NotNull String name) {
        for (QuestType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return INDEPENDENT; // 默认返回独立任务
    }
}
