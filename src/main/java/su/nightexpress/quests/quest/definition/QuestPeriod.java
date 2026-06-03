package su.nightexpress.quests.quest.definition;

import org.jetbrains.annotations.NotNull;

/**
 * 任务周期枚举
 * DAILY - 每日任务：每天刷新
 * WEEKLY - 每周任务：每周刷新
 * MONTHLY - 每月任务：每月刷新
 * SEASONAL - 赛季任务：跟随战斗通行证赛季
 */
public enum QuestPeriod {
    
    DAILY("每日任务", "每天刷新"),
    WEEKLY("每周任务", "每周刷新"),
    MONTHLY("每月任务", "每月刷新"),
    SEASONAL("赛季任务", "跟随战斗通行证赛季");
    
    private final String displayName;
    private final String description;
    
    QuestPeriod(@NotNull String displayName, @NotNull String description) {
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
    public static QuestPeriod fromString(@NotNull String name) {
        for (QuestPeriod period : values()) {
            if (period.name().equalsIgnoreCase(name)) {
                return period;
            }
        }
        return DAILY; // 默认返回每日任务
    }
}

