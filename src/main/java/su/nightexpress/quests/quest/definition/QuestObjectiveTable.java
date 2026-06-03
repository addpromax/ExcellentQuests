package su.nightexpress.quests.quest.definition;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.quests.api.AbstractObjectiveTable;
import su.nightexpress.quests.task.adapter.Adapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuestObjectiveTable extends AbstractObjectiveTable<QuestObjective> {

    public static final QuestObjectiveTable EMPTY = new QuestObjectiveTable(Collections.emptyMap());

    public QuestObjectiveTable() {
        super();
    }

    public QuestObjectiveTable(@NotNull Map<String, QuestObjective> entires) {
        super(entires);
    }
    
    @Override
    public void write(@NotNull FileConfig config, @NotNull String path) {
        // 使用新的结构化格式保存
        this.getEntryMap().forEach((name, objective) -> {
            String fullPath = path + "." + name;
            config.set(fullPath + ".Weight", objective.weight());
            config.set(fullPath + ".Amount.Min", objective.min());
            config.set(fullPath + ".Amount.Max", objective.max());
            config.set(fullPath + ".UnitWorth", objective.unitWorth());
        });
    }

    @NotNull
    public static QuestObjectiveTable read(@NotNull FileConfig config, @NotNull String path) {
        Map<String, QuestObjective> entires = new HashMap<>();

        config.getSection(path).forEach(name -> {
            String fullPath = path + "." + name;

            // 检查是否是新的结构化格式
            if (config.contains(fullPath + ".Weight")) {
                // 新格式：使用 Weight, Amount.Min/Max, UnitWorth
                try {
                    double weight = config.getDouble(fullPath + ".Weight", 0.0);
                    int min = config.getInt(fullPath + ".Amount.Min", 0);
                    int max = config.getInt(fullPath + ".Amount.Max", 0);
                    double unitWorth = config.getDouble(fullPath + ".UnitWorth", 0.0);
                    
                    QuestObjective objective = new QuestObjective(min, max, weight, unitWorth);
                    entires.put(name, objective);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "无法解析目标 '" + name + "' (结构化格式). " +
                        "路径: " + fullPath + ". " +
                        "错误: " + e.getMessage(), e
                    );
                }
            } else {
                // 旧格式：字符串格式 'MIN;MAX WEIGHT UNIT_WORTH'
                String data = config.getString(fullPath);
                if (data == null) {
                    Object rawValue = config.get(fullPath);
                    String valueType = rawValue == null ? "null" : rawValue.getClass().getSimpleName();
                    throw new IllegalArgumentException(
                        "目标 '" + name + "' 的值不是字符串! " +
                        "路径: " + fullPath + ", " +
                        "实际类型: " + valueType + ", " +
                        "实际值: " + rawValue + ". " +
                        "请使用结构化格式或字符串格式"
                    );
                }

                try {
                    QuestObjective objective = QuestObjective.deserialize(data);
                    entires.put(name, objective);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "无法解析目标 '" + name + "' (字符串格式). " +
                        "路径: " + fullPath + ", " +
                        "值: '" + data + "'. " +
                        "错误: " + e.getMessage(), e
                    );
                }
            }
        });

        return new QuestObjectiveTable(entires);
    }

    @NotNull
    public static QuestObjectiveTable.Builder builder() {
        return new QuestObjectiveTable.Builder();
    }

    public static class Builder {

        private final Map<String, QuestObjective> entires;

        private double scale;

        Builder() {
            this.scale = 1D;
            this.entires = new LinkedHashMap<>();
        }

        @NotNull
        public QuestObjectiveTable build() {
            return new QuestObjectiveTable(this.entires);
        }

        @NotNull
        public Builder withScale(double scale) {
            this.scale = scale;
            return this;
        }

        public <I, O, E extends Adapter<I, O>> Builder addEntry(@NotNull String name, @NotNull E adapter, int min, int max, double weight, double unitWorth) {
            I type = adapter.getTypeByName(name);
            if (type == null) return this;

            return this.addType(type, adapter, min, max, weight, unitWorth);
        }

        public <I, O, E extends Adapter<I, O>> Builder addType(@NotNull I type, @NotNull E adapter, int min, int max, double weight, double unitWorth) {
            return this.addEntry(adapter.toFullNameOfType(type), min, max, weight, unitWorth);
        }

        public <I, O, E extends Adapter<I, O>> Builder addEntity(@NotNull O entity, @NotNull E adapter, int min, int max, double weight, double unitWorth) {
            String fullName = adapter.toFullNameOfEntity(entity);
            if (fullName == null) return this;

            return this.addEntry(fullName, min, max, weight, unitWorth);
        }

        @NotNull
        public Builder addEntry(@NotNull String fullName, int min, int max, double weight, double unitWorth) {
            int minScaled = (int) Math.ceil(min * this.scale);
            int maxScaled = (int) Math.ceil(max * this.scale);

            this.entires.put(fullName, new QuestObjective(minScaled, maxScaled, weight, unitWorth));
            return this;
        }
    }
}
