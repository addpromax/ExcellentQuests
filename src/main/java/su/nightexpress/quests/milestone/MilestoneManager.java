package su.nightexpress.quests.milestone;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.Strings;
import su.nightexpress.quests.QuestsPlaceholders;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.api.exception.QuestLoadException;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.milestone.command.MilestoneCommands;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.milestone.definition.Milestone;
import su.nightexpress.quests.milestone.definition.MilestoneCategory;
import su.nightexpress.quests.milestone.listener.MilestoneGenericListener;
import su.nightexpress.quests.milestone.menu.CategoriesMenu;
import su.nightexpress.quests.milestone.menu.MilestonesMenu;
import su.nightexpress.quests.milestone.menu.ProgressionMenu;
import su.nightexpress.quests.reward.Reward;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.user.QuestUser;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MilestoneManager extends AbstractManager<QuestsPlugin> {

    private final Map<String, MilestoneCategory> categoryById;
    private final Map<String, Milestone>         milestoneById;
    private final Map<TaskType<?, ?>, Set<Milestone>> milestonesByType;  // 按任务类型索引里程碑，提升性能
    private final String dirPath;

    private CategoriesMenu categoriesMenu;
    private MilestonesMenu milestonesMenu;
    private ProgressionMenu progressionMenu;

    public MilestoneManager(@NotNull QuestsPlugin plugin) {
        super(plugin);
        this.dirPath = plugin.getDataFolder() + Config.DIR_MILESTONES;
        this.categoryById = new HashMap<>();
        this.milestoneById = new HashMap<>();
        this.milestonesByType = new HashMap<>();
    }

    @Override
    protected void onLoad() {
        FileConfig config = this.plugin.getConfig();
        this.loadCategories(config);
        this.loadMilestones();
        this.loadUI();
        this.loadCommands();

        this.addListener(new MilestoneGenericListener(this.plugin, this));

        // Add new milestones for online players after the config reload.
        // Folia: 不能在 onLoad() 中使用调度器，改为在监听器中处理
        // Players 上线时会自动通过 PlayerJoinEvent 更新里程碑
        // 已在线的玩家会在下次交互时更新
    }

    @Override
    protected void onShutdown() {
        this.categoryById.clear();
        this.milestoneById.clear();
        this.milestonesByType.clear();

        MilestoneCommands.shutdown();
    }

    private void loadCategories(@NotNull FileConfig config) {
        String path = "Milestones.Categories";
        if (!config.contains(path)) {
            MilestoneDefaults.createCategories().forEach(category -> config.set(path + "." + category.getId(), category));
        }

        config.getSection(path).forEach(sId -> {
            MilestoneCategory category = MilestoneCategory.read(config, path + "." + sId, sId);
            this.categoryById.put(category.getId(), category);
        });

        this.plugin.info("Loaded " + this.categoryById.size() + " milestone categories.");
    }

    private void loadMilestones() {
        File dir = new File(this.dirPath);
        if (!dir.exists() && dir.mkdirs()) {
            MilestoneDefaults.createMilestones(this);
        }

        FileUtil.getConfigFiles(this.dirPath).forEach(file -> {
            String id = Strings.filterForVariable(FileConfig.getName(file));

            Milestone milestone = new Milestone(file, id);
            try {
                milestone.setPlugin(this.plugin);  // 设置plugin引用以便在load时使用
                milestone.load();
            }
            catch (QuestLoadException exception) {
                this.plugin.error("Quest '" + file.getPath() + "' not loaded: " + exception.getMessage());
                return;
            }

            this.milestoneById.put(milestone.getId(), milestone);
            
            // 添加到任务类型索引，提升查询性能
            this.milestonesByType.computeIfAbsent(milestone.getType(), k -> new HashSet<>()).add(milestone);
        });

        this.plugin.info("Loaded " + this.milestoneById.size() + " milestones.");
    }

    private void loadUI() {
        this.categoriesMenu = this.addMenu(new CategoriesMenu(this.plugin, this), Config.DIR_MENU, "milestone_categories.yml");
        this.milestonesMenu = this.addMenu(new MilestonesMenu(this.plugin, this), Config.DIR_MENU, "milestones.yml");
        this.progressionMenu = this.addMenu(new ProgressionMenu(this.plugin, this), Config.DIR_MENU, "milestone_progression.yml");
    }

    private void loadCommands() {
        MilestoneCommands.load(this.plugin, this);
    }

    public void createMilestone(@NotNull String name, @NotNull Consumer<Milestone> consumer) {
        String id = Strings.filterForVariable(name);
        if (this.getMilestoneById(id) != null) return;

        File file = new File(this.dirPath, FileConfig.withExtension(id));
        Milestone milestone = new Milestone(file, id);
        consumer.accept(milestone);
        milestone.save();
    }

    public void openCategories(@NotNull Player player) {
        this.categoriesMenu.open(player);
    }

    public void openMilestones(@NotNull Player player, @NotNull MilestoneCategory category) {
        this.milestonesMenu.open(player, category);
    }

    public void openProgression(@NotNull Player player, @NotNull Milestone milestone) {
        this.progressionMenu.open(player, milestone);
    }

    public void updateMilestones(@NotNull Player player) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);

        boolean result = this.getMilestones().stream().anyMatch(user::addMilestone);
        if (result) {
            this.plugin.getUserManager().save(user);
        }
    }

    public <O, A extends AdapterFamily<O>> void progressMilestones(@NotNull Player player, @NotNull TaskType<O, A> taskType, @NotNull String fullName, int amount) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);

        // 使用索引直接获取匹配类型的里程碑，而不是遍历所有里程碑
        Set<Milestone> typedMilestones = this.milestonesByType.get(taskType);
        if (typedMilestones == null || typedMilestones.isEmpty()) {
            return;
        }

        AtomicBoolean progressed = new AtomicBoolean(false);

        // 只遍历匹配类型的里程碑
        typedMilestones.forEach(milestone -> {
            if (user.isCompleted(milestone)) {
                return;
            }

            MilestoneData data = user.getMilestoneData(milestone);

            int level = data.getFirstIncompletedLevel(milestone);
            if (level <= 0) {
                return;
            }

            int required = milestone.getObjectiveRequirement(fullName, level);
            if (required <= 0) {
                return;
            }
            
            // 获取实际应该使用的目标键（如果是通配符匹配，返回通配符键）
            String actualKey = milestone.getActualObjectiveKey(fullName);

            int progress = data.getObjectiveProgress(actualKey);
            int total = Math.min(required, progress + amount);

            data.setObjectiveProgress(actualKey, total);
            progressed.set(true);

            // 检查并完成所有满足条件的等级（而不是只检查当前等级）
            this.checkAndCompleteLevels(player, milestone, data, level);
        });

        if (progressed.get()) {
            this.plugin.getUserManager().save(user);
        }
    }
    
    /**
     * 检查并完成所有满足条件的等级
     * 从当前等级开始，逐级检查，只完成真正满足条件的等级
     */
    private void checkAndCompleteLevels(@NotNull Player player, @NotNull Milestone milestone, 
                                        @NotNull MilestoneData data, int startLevel) {
        int currentLevel = startLevel;
        
        // 从当前等级开始，逐级检查
        while (currentLevel > 0 && currentLevel <= milestone.getLevels()) {
            // 如果这个等级已经完成，跳到下一个
            if (data.isLevelCompleted(currentLevel)) {
                currentLevel++;
                continue;
            }
            
            // 检查这个等级是否满足条件
            if (data.isReady(milestone, currentLevel)) {
                data.addCompletedLevel(currentLevel);

                // 获取当前等级对应的奖励（而不是所有奖励）
                List<Reward> allRewards = this.plugin.getRewardManager().getMilestoneRewards(milestone);
                int units = data.countTotalProgress(milestone);
                
                // 使用final变量在lambda中引用
                final int completedLevel = currentLevel;
                
                // 奖励列表中，索引 0 对应等级 1，索引 1 对应等级 2，以此类推
                // 所以等级 N 的奖励索引是 N-1
                int rewardIndex = completedLevel - 1;
                
                if (rewardIndex >= 0 && rewardIndex < allRewards.size()) {
                    Reward levelReward = allRewards.get(rewardIndex);
                    
                    levelReward.runCommands(player, units, completedLevel, 1D);
                    
                    Lang.MILESTONES_MILESTONE_COMPLETED.message().send(player, replacer -> replacer
                        .replace(milestone.replacePlaceholders())
                        .replace(QuestsPlaceholders.GENERIC_LEVEL, String.valueOf(completedLevel))
                        .replace(QuestsPlaceholders.GENERIC_REWARDS, levelReward.getName(units, completedLevel, 1D))
                    );
                } else {
                    this.plugin.warn("等级 " + completedLevel + " 没有对应的奖励！(索引: " + rewardIndex + ", 总奖励数: " + allRewards.size() + ")");
                }
                
                // 继续检查下一个等级
                currentLevel++;
            } else {
                // 这个等级不满足条件，停止检查
                break;
            }
        }
    }

    @NotNull
    public Set<Milestone> getMilestonesByCategory(@NotNull MilestoneCategory category) {
        return this.getMilestones().stream().filter(milestone -> milestone.isCategory(category)).collect(Collectors.toSet());
    }

    @NotNull
    public Map<String, MilestoneCategory> getCategoryByIdMap() {
        return this.categoryById;
    }

    @NotNull
    public Set<MilestoneCategory> getCategories() {
        return new HashSet<>(this.categoryById.values());
    }

    @Nullable
    public MilestoneCategory getCategoryById(@NotNull String id) {
        return this.categoryById.get(id);
    }

    @NotNull
    public Map<String, Milestone> getMilestoneByIdMap() {
        return this.milestoneById;
    }

    @Nullable
    public Milestone getMilestoneById(@NotNull String id) {
        return this.milestoneById.get(id);
    }

    @NotNull
    public Set<Milestone> getMilestones() {
        return new HashSet<>(this.milestoneById.values());
    }
}
