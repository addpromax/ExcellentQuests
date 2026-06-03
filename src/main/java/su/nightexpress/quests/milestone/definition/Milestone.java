package su.nightexpress.quests.milestone.definition;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.quests.QuestsAPI;
import su.nightexpress.quests.QuestsPlaceholders;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.api.IQuest;
import su.nightexpress.quests.api.exception.QuestLoadException;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.task.TaskType;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

public class Milestone implements IQuest {

    private final File   file;
    private final String id;

    private TaskType<?, ?>               type;
    private String                       category;
    private String                       name;
    private List<String>                 description;
    private NightItem                    icon;
    private int                          levels;
    private MilestoneObjectiveTable      objectiveTable;
    private List<ObjectiveGroup>         objectiveGroups;  // 目标组列表
    private List<String>                 rewards;
    private Map<String, List<String>>    filters;  // 筛选器

    public Milestone(@NotNull File file, @NotNull String id) {
        this.file = file;
        this.id = id;
        this.rewards = new ArrayList<>();
        this.objectiveGroups = new ArrayList<>();
        this.filters = new HashMap<>();
    }

    public void load() throws QuestLoadException {
        FileConfig config = this.getConfig();
        String path = "";

        String typeName = ConfigValue.create(path + ".Type", "null").read(config);
        this.type = QuestsAPI.plugin().getTaskTypeRegistry().getTypeById(typeName);
        if (this.type == null) {
            throw new QuestLoadException("Invalid milestone type '" + typeName + "'!");
        }

        this.name = ConfigValue.create(path + ".Name", StringUtil.capitalizeUnderscored(this.id)).read(config);
        this.description = ConfigValue.create(path + ".Description", Collections.emptyList()).read(config);
        this.icon = ConfigValue.create(path + ".Icon", NightItem.fromType(Material.CHEST_MINECART)).read(config);
        
        // 清除图标的 DisplayName 和 Lore，因为这些由 Lore 模板和里程碑名称控制
        // 只保留材质、Model 等功能性配置
        this.icon.setDisplayName(null);
        this.icon.setLore(Collections.emptyList());

        // 尝试读取新格式（分组格式）
        this.loadObjectives(config, path);

        this.rewards = ConfigValue.create(path + ".Rewards.Custom", Collections.emptyList()).read(config);

        this.category = ConfigValue.create(path + ".Category", "null").read(config);
        this.levels = ConfigValue.create(path + ".Levels", 1).read(config);
        
        // 读取筛选器配置
        if (config.contains(path + ".Filters")) {
            this.filters = new HashMap<>();
            var section = config.getConfigurationSection(path + ".Filters");
            if (section != null) {
                for (String filterKey : section.getKeys(false)) {
                    List<String> filterValues = config.getStringList(path + ".Filters." + filterKey);
                    if (!filterValues.isEmpty()) {
                        this.filters.put(filterKey, filterValues);
                    }
                }
            }
        }

        config.saveChanges();
    }

    public void save() {
        FileConfig config = this.getConfig();
        String path = "";

        config.set(path + ".Type", this.type.getId());
        config.set(path + ".Category", this.category);
        config.set(path + ".Name", this.name);
        config.set(path + ".Description", this.description);
        config.set(path + ".Icon", this.icon);
        config.set(path + ".Levels", this.levels);
        config.set(path + ".Objectives.List", this.objectiveTable);
        config.set(path + ".Rewards.Custom", this.rewards);

        config.save();
    }

    @NotNull
    public UnaryOperator<String> replacePlaceholders() {
        return QuestsPlaceholders.MILESTONE.replacer(this);
    }

    public boolean isCategory(@NotNull MilestoneCategory category) {
        return this.isCategory(category.getId());
    }

    public boolean isCategory(@NotNull String name) {
        return this.category.equalsIgnoreCase(name);
    }

    /**
     * 获取实际应该使用的目标键
     * 如果使用了通配符匹配，返回通配符键；否则返回原始键
     */
    @NotNull
    public String getActualObjectiveKey(@NotNull String fullName) {
        // 先尝试精确匹配
        if (this.objectiveTable.getEntry(fullName) != null) {
            return fullName;
        }
        
        // 如果没有精确匹配，尝试去掉命名空间再匹配
        if (fullName.contains(":")) {
            String valueOnly = fullName.substring(fullName.indexOf(':') + 1);
            if (this.objectiveTable.getEntry(valueOnly) != null) {
                return valueOnly;
            }
        }
        
        // 如果还是没有匹配，尝试通配符 "minecraft:*"
        if (this.objectiveTable.getEntry("minecraft:*") != null) {
            return "minecraft:*";
        }
        
        // 尝试通配符 "minecraft:all"
        if (this.objectiveTable.getEntry("minecraft:all") != null) {
            return "minecraft:all";
        }
        
        // 尝试通配符 "vanilla_string:*"
        if (this.objectiveTable.getEntry("vanilla_string:*") != null) {
            return "vanilla_string:*";
        }
        
        // 尝试通配符 "vanilla_string:all"
        if (this.objectiveTable.getEntry("vanilla_string:all") != null) {
            return "vanilla_string:all";
        }
        
        // 没有匹配，返回原始键
        return fullName;
    }
    
    public int getObjectiveRequirement(@NotNull String fullName, int level) {
        // 先尝试精确匹配
        MilestoneObjective objective = this.objectiveTable.getEntry(fullName);
        if (objective != null) {
            return objective.getAmount(level);
        }
        
        // 如果没有精确匹配，尝试去掉命名空间再匹配
        if (fullName.contains(":")) {
            String valueOnly = fullName.substring(fullName.indexOf(':') + 1);
            objective = this.objectiveTable.getEntry(valueOnly);
            if (objective != null) {
                return objective.getAmount(level);
            }
        }
        
        // 如果还是没有匹配，尝试通配符 "minecraft:*"
        objective = this.objectiveTable.getEntry("minecraft:*");
        if (objective != null) {
            return objective.getAmount(level);
        }
        
        // 尝试通配符 "minecraft:all"
        objective = this.objectiveTable.getEntry("minecraft:all");
        if (objective != null) {
            return objective.getAmount(level);
        }
        
        // 尝试通配符 "vanilla_string:*"
        objective = this.objectiveTable.getEntry("vanilla_string:*");
        if (objective != null) {
            return objective.getAmount(level);
        }
        
        // 尝试通配符 "vanilla_string:all"
        objective = this.objectiveTable.getEntry("vanilla_string:all");
        if (objective != null) {
            return objective.getAmount(level);
        }
        
        return -1;
    }

    public int countTotalRequirements() {
        int startLevel = Config.isMilestonesResetProgress() ? 1 : this.levels;

        int total = 0;
        for (int level = startLevel; level < this.levels + 1; level++) {
            total += countTotalRequirements(level);
        }
        return total;
    }

    public int countTotalRequirements(int level) {
        return this.objectiveTable.getEntryMap().values().stream().mapToInt(objective -> objective.getAmount(level)).sum();
    }

    @Override
    @NotNull
    public Path getPath() {
        return this.file.toPath();
    }

    @NotNull
    @Override
    public String getId() {
        return this.id;
    }

    @NotNull
    @Override
    public TaskType<?, ?> getType() {
        return this.type;
    }

    public void setType(@NotNull TaskType<?, ?> type) {
        this.type = type;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public List<String> getDescription() {
        return this.description;
    }

    public void setDescription(@NotNull List<String> description) {
        this.description = description;
    }

    @NotNull
    @Override
    public NightItem getIcon() {
        return this.icon.copy();
    }

    public void setIcon(@NotNull NightItem icon) {
        this.icon = icon.copy();
    }

    @NotNull
    public String getCategory() {
        return this.category;
    }

    public void setCategory(@NotNull String category) {
        this.category = category;
    }

    public int getLevels() {
        return this.levels;
    }

    public void setLevels(int levels) {
        this.levels = levels;
    }

    @NotNull
    public MilestoneObjectiveTable getObjectiveTable() {
        return this.objectiveTable;
    }

    public void setObjectiveTable(@NotNull MilestoneObjectiveTable objectiveTable) {
        this.objectiveTable = objectiveTable;
    }

    @NotNull
    public List<String> getRewards() {
        return this.rewards;
    }

    public void addReward(@NotNull String id) {
        this.rewards.add(id);
    }
    
    @NotNull
    public List<ObjectiveGroup> getObjectiveGroups() {
        return objectiveGroups;
    }
    
    /**
     * 检查目标是否属于某个组
     */
    public ObjectiveGroup getGroupForObjective(@NotNull String fullName) {
        for (ObjectiveGroup group : objectiveGroups) {
            if (group.contains(fullName)) {
                return group;
            }
        }
        return null;
    }
    
    /**
     * 加载目标配置（支持新旧两种格式）
     */
    private void loadObjectives(@NotNull FileConfig config, @NotNull String path) {
        String groupsPath = path + ".Objectives.Groups";
        String requirementsPath = path + ".Objectives.Requirements";
        String listPath = path + ".Objectives.List";
        
        // 检查是否使用新格式（分组格式）
        if (config.contains(groupsPath) && config.contains(requirementsPath)) {
            this.plugin.info("[DEBUG] 加载分组格式目标: " + this.id);
            this.loadGroupedObjectives(config, groupsPath, requirementsPath);
        } else if (config.contains(listPath)) {
            // 使用旧格式（列表格式）
            this.plugin.info("[DEBUG] 加载列表格式目标: " + this.id);
            this.objectiveTable = MilestoneObjectiveTable.read(config, listPath);
            this.objectiveGroups.clear();  // 清空分组
        } else {
            // 没有目标配置
            this.objectiveTable = MilestoneObjectiveTable.EMPTY;
            this.objectiveGroups.clear();
        }
    }
    
    /**
     * 加载分组格式的目标
     */
    private void loadGroupedObjectives(@NotNull FileConfig config, @NotNull String groupsPath, @NotNull String requirementsPath) {
        this.objectiveGroups.clear();
        Map<String, MilestoneObjective> allObjectives = new HashMap<>();
        
        // 读取每个分组
        config.getSection(groupsPath).forEach(groupName -> {
            List<String> members = config.getStringList(groupsPath + "." + groupName);
            if (members.isEmpty()) return;
            
            // 读取该组的要求值
            String requirementData = config.getString(requirementsPath + "." + groupName);
            if (requirementData == null) {
                this.plugin.warn("分组 '" + groupName + "' 没有配置要求值！");
                return;
            }
            
            MilestoneObjective requirement = MilestoneObjective.deserialize(requirementData);
            
            // 创建目标组
            ObjectiveGroup group = new ObjectiveGroup(groupName, members, requirement);
            this.objectiveGroups.add(group);
            
            this.plugin.info("[DEBUG]   组: " + groupName + ", 成员: " + members.size() + ", 要求: " + requirementData);
            
            // 将组内所有成员添加到目标表（使用组的要求值）
            for (String member : members) {
                allObjectives.put(member, requirement);
            }
        });
        
        this.objectiveTable = new MilestoneObjectiveTable(allObjectives);
    }
    
    private QuestsPlugin plugin;
    
    public void setPlugin(QuestsPlugin plugin) {
        this.plugin = plugin;
    }
    
    // 筛选器相关方法
    @NotNull
    public Map<String, List<String>> getFilters() {
        return this.filters;
    }
    
    public void setFilters(@NotNull Map<String, List<String>> filters) {
        this.filters = filters;
    }
}
