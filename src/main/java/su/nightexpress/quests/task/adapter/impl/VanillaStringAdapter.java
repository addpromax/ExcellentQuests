package su.nightexpress.quests.task.adapter.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.quests.task.adapter.type.AbstractAdapter;

/**
 * 简单字符串适配器
 * 用于不需要复杂对象处理的任务类型（如死亡、登录等）
 */
public class VanillaStringAdapter extends AbstractAdapter<String, String> {

    public VanillaStringAdapter(@NotNull String name) {
        super(name);
    }

    @Override
    public boolean canHandle(@NotNull String entity) {
        return true;  // 接受所有字符串
    }

    @Override
    @Nullable
    public String getTypeByName(@NotNull String name) {
        return name;
    }

    @Override
    @Nullable
    public String getType(@NotNull String entity) {
        return entity;
    }

    @Override
    @NotNull
    public String getTypeName(@NotNull String type) {
        return type;
    }

    @Override
    @Nullable
    public String getLocalizedName(@NotNull String type) {
        return type;
    }

    @Override
    @Nullable
    public String toFullNameOfEntity(@NotNull String entity) {
        return this.toFullNameOfType(entity);
    }

    @Override
    @NotNull
    public String toFullNameOfType(@NotNull String type) {
        return this.toFullName(type);
    }

    @Override
    @NotNull
    public String toFullName(@NotNull String name) {
        return this.getName() + ":" + name;
    }
}

