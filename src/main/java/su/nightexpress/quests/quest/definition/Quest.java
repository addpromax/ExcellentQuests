package su.nightexpress.quests.quest.definition;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.random.Rnd;
import su.nightexpress.nightcore.util.wrapper.UniInt;
import su.nightexpress.quests.QuestsAPI;
import su.nightexpress.quests.QuestsPlaceholders;
import su.nightexpress.quests.api.IQuest;
import su.nightexpress.quests.api.exception.QuestLoadException;
import su.nightexpress.quests.quest.data.QuestCounter;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.task.TaskType;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

public class Quest implements IQuest {

    private final File   file;
    private final String id;

    private TaskType<?, ?>      type;
    private String              name;
    private List<String>        description;
    private NightItem           icon;
    private UniInt              objectivesAmount;
    private QuestObjectiveTable objectiveTable;
    private List<String>  rewards;  // 奖励命令列表（直接配置）
    private List<String>  rewardLore;  // 奖励描述（用于显示）
    private Map<Integer, List<String>> rankingRewards; // 排名奖励：排名 -> 命令列表
    private Map<Integer, Integer> rankingBattlePassXP; // 排名通行证经验奖励：排名 -> 经验值
    private Map<Integer, List<String>> rankingRewardLore; // 排名奖励描述：排名 -> 描述列表
    private QuestXPReward battlePassXPReward;

    private long completionTime;
    
    // 新增：任务类型相关字段
    private QuestType questType;
    private String cooperativeFormula; // 合作任务：计算目标数量的公式，如 "%maxplayer%*10"
    private int competitiveMaxCompletions; // 竞争任务：最大完成人数（固定数值）
    private double competitiveMaxCompletionPercent; // 竞争任务：最大完成百分比（0-1之间）
    private boolean competitiveUsePercent; // 竞争任务：是否使用百分比模式
    
    // 新增：任务周期相关字段
    private QuestPeriod questPeriod; // 任务周期：每日、每周、每月、赛季
    
    // 新增：任务筛选器
    private Map<String, List<String>> filters; // 筛选器：用于限制任务进度计算条件

    public Quest(@NotNull File file, @NotNull String id) {
        this.file = file;
        this.id = id;
        this.rewards = new ArrayList<>();
        this.rewardLore = new ArrayList<>();
        this.rankingRewards = new HashMap<>();
        this.rankingBattlePassXP = new HashMap<>();
        this.rankingRewardLore = new HashMap<>();
        // 默认为独立任务
        this.questType = QuestType.INDEPENDENT;
        this.cooperativeFormula = "%maxplayer%*10";
        this.competitiveMaxCompletions = 10;
        this.competitiveMaxCompletionPercent = 0.1;
        this.competitiveUsePercent = false;
        // 默认为每日任务
        this.questPeriod = QuestPeriod.DAILY;
        // 默认无筛选器
        this.filters = new HashMap<>();
    }

    @Override
    public void load() throws QuestLoadException {
        FileConfig config = this.loadConfig();
        String path = "";

        String typeName = ConfigValue.create(path + ".Type", "null").read(config);
        this.type = QuestsAPI.plugin().getTaskTypeRegistry().getTypeById(typeName);
        if (this.type == null) {
            throw new QuestLoadException("Invalid quest type '" + typeName + "'!");
        }

        this.name = ConfigValue.create(path + ".Name", StringUtil.capitalizeUnderscored(this.id)).read(config);
        this.description = ConfigValue.create(path + ".Description", Collections.emptyList()).read(config);
        this.icon = ConfigValue.create(path + ".Icon", NightItem.fromType(Material.CHEST_MINECART)).read(config);
        
        // 清除图标的 DisplayName 和 Lore，因为这些由 Lore 模板和任务名称控制
        // 只保留材质、Model 等功能性配置
        this.icon.setDisplayName(null);
        this.icon.setLore(Collections.emptyList());

        this.objectivesAmount = ConfigValue.create(path + ".Objectives.Amount", UniInt::read, UniInt.of(1, -1)).read(config);
        
        try {
            this.objectiveTable = QuestObjectiveTable.read(config, path + ".Objectives.List");
        } catch (Exception e) {
            throw new QuestLoadException(
                "任务 '" + this.getId() + "' 加载失败: 无法读取目标列表 (Objectives.List). " +
                "配置文件: " + config.getFile().getName() + ". " +
                "错误: " + e.getMessage() + 
                (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "")
            );
        }

        // 读取奖励配置（直接配置方式）
        this.rewards = ConfigValue.create(path + ".Rewards.Commands", Collections.emptyList()).read(config);
        this.rewardLore = ConfigValue.create(path + ".Rewards.Lore", Collections.emptyList()).read(config);
        
        // 兼容旧配置：如果 Rewards.Commands 不存在但 Rewards.Custom 存在，则使用旧配置
        if (this.rewards.isEmpty() && config.contains(path + ".Rewards.Custom")) {
            this.rewards = ConfigValue.create(path + ".Rewards.Custom", Collections.emptyList()).read(config);
        }
        
        // 读取排名奖励配置
        this.rankingRewards = new HashMap<>();
        this.rankingBattlePassXP = new HashMap<>();
        this.rankingRewardLore = new HashMap<>();
        if (config.contains(path + ".Ranking_Rewards")) {
            for (String rankKey : config.getSection(path + ".Ranking_Rewards")) {
                try {
                    int rank = Integer.parseInt(rankKey);
                    List<String> commands = config.getStringList(path + ".Ranking_Rewards." + rankKey + ".commands");
                    if (!commands.isEmpty()) {
                        this.rankingRewards.put(rank, new ArrayList<>(commands));
                    }
                    
                    // 读取排名通行证经验奖励
                    int battlePassXP = config.getInt(path + ".Ranking_Rewards." + rankKey + ".battlepass_xp", 0);
                    if (battlePassXP > 0) {
                        this.rankingBattlePassXP.put(rank, battlePassXP);
                    }
                    
                    // 读取排名奖励描述
                    List<String> lore = config.getStringList(path + ".Ranking_Rewards." + rankKey + ".lore");
                    if (!lore.isEmpty()) {
                        this.rankingRewardLore.put(rank, new ArrayList<>(lore));
                    }
                    
                    // 兼容旧配置：command 改为 commands
                    if (commands.isEmpty()) {
                        List<String> oldCommands = config.getStringList(path + ".Ranking_Rewards." + rankKey + ".command");
                        if (!oldCommands.isEmpty()) {
                            this.rankingRewards.put(rank, new ArrayList<>(oldCommands));
                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略非数字的key
                }
            }
        }

        this.setBattlePassXPReward(QuestXPReward.read(config, path + ".Rewards.BattlePassXP"));

        this.completionTime = ConfigValue.create(path + ".CompletionTime", 7200).read(config);
        
        // 读取任务类型相关配置
        String questTypeName = ConfigValue.create(path + ".QuestType", QuestType.INDEPENDENT.name()).read(config);
        this.questType = QuestType.fromString(questTypeName);
        
        this.cooperativeFormula = ConfigValue.create(path + ".CooperativeFormula", "%maxplayer%*10",
            "合作任务的目标数量计算公式。可用变量: %maxplayer% (当日最高在线人数)").read(config);
        
        this.competitiveMaxCompletions = ConfigValue.create(path + ".CompetitiveMaxCompletions", 10,
            "竞争任务的最大完成人数（固定数值模式）").read(config);
        
        this.competitiveMaxCompletionPercent = ConfigValue.create(path + ".CompetitiveMaxCompletionPercent", 0.1,
            "竞争任务的最大完成百分比（百分比模式，0-1之间）").read(config);
        
        this.competitiveUsePercent = ConfigValue.create(path + ".CompetitiveUsePercent", false,
            "竞争任务是否使用百分比模式。true=使用百分比，false=使用固定数值").read(config);
        
        // 读取任务周期配置
        String questPeriodName = ConfigValue.create(path + ".QuestPeriod", QuestPeriod.DAILY.name(),
            "任务周期。DAILY=每日, WEEKLY=每周, MONTHLY=每月, SEASONAL=赛季").read(config);
        this.questPeriod = QuestPeriod.fromString(questPeriodName);
        
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
        FileConfig config = this.loadConfig();
        String path = "";

        config.set(path + ".Type", this.type.getId());
        config.set(path + ".Name", this.name);
        config.set(path + ".Description", this.description);
        config.set(path + ".Icon", this.icon);
        config.set(path + ".Objectives.Amount", this.objectivesAmount);
        config.set(path + ".Objectives.List", this.objectiveTable);
        config.set(path + ".Rewards.BattlePassXP", this.battlePassXPReward);
        config.set(path + ".Rewards.Commands", this.rewards);
        config.set(path + ".Rewards.Lore", this.rewardLore);
        
        // 保存排名奖励配置
        if (!this.rankingRewards.isEmpty() || !this.rankingBattlePassXP.isEmpty() || !this.rankingRewardLore.isEmpty()) {
            // 获取所有排名的并集
            Set<Integer> allRanks = new HashSet<>();
            allRanks.addAll(this.rankingRewards.keySet());
            allRanks.addAll(this.rankingBattlePassXP.keySet());
            allRanks.addAll(this.rankingRewardLore.keySet());
            
            for (Integer rank : allRanks) {
                if (this.rankingRewards.containsKey(rank)) {
                    config.set(path + ".Ranking_Rewards." + rank + ".commands", this.rankingRewards.get(rank));
                }
                if (this.rankingBattlePassXP.containsKey(rank)) {
                    config.set(path + ".Ranking_Rewards." + rank + ".battlepass_xp", this.rankingBattlePassXP.get(rank));
                }
                if (this.rankingRewardLore.containsKey(rank)) {
                    config.set(path + ".Ranking_Rewards." + rank + ".lore", this.rankingRewardLore.get(rank));
                }
            }
        }
        
        config.set(path + ".CompletionTime", this.completionTime);
        
        // 保存任务类型相关配置
        config.set(path + ".QuestType", this.questType.name());
        config.set(path + ".CooperativeFormula", this.cooperativeFormula);
        config.set(path + ".CompetitiveMaxCompletions", this.competitiveMaxCompletions);
        config.set(path + ".CompetitiveMaxCompletionPercent", this.competitiveMaxCompletionPercent);
        config.set(path + ".CompetitiveUsePercent", this.competitiveUsePercent);
        
        // 保存任务周期配置
        config.set(path + ".QuestPeriod", this.questPeriod.name());

        config.save();
    }

    @NotNull
    public UnaryOperator<String> replacePlaceholders() {
        return QuestsPlaceholders.QUEST.replacer(this);
    }

    @Nullable
    public QuestData createQuestData() {
        UUID uuid = UUID.randomUUID();
        Map<String, QuestCounter> objectives = new LinkedHashMap<>();
        Set<String> rewardIds = new HashSet<>(this.rewards);

        double scale = 1D; // TODO

        Map<String, Double> objectiveByWeight = new HashMap<>();
        this.objectiveTable.getEntryMap().forEach((fullName, objective) -> {
            objectiveByWeight.put(fullName, objective.weight());
        });

        double unitsWorth = 0;
        int objectivesAmount = this.objectivesAmount.roll();
        while (objectivesAmount > 0 && !objectiveByWeight.isEmpty()) {
            String fullName = Rnd.getByWeight(objectiveByWeight);
            objectiveByWeight.remove(fullName);

            QuestObjective objective = this.objectiveTable.getEntry(fullName);
            if (objective == null) continue;

            int amount = objective.rollAmount(scale);
            if (amount <= 0) continue;

            double unitWorth = objective.unitWorth();

            objectives.put(/*LowerCase.INTERNAL.apply(*/fullName, QuestCounter.create(amount, unitWorth));
            objectivesAmount--;
            unitsWorth += (amount * unitWorth);
        }
        if (objectives.isEmpty()) return null;

        int xpReward = this.battlePassXPReward.getXP(unitsWorth);

        // 独立任务：不设置过期时间，由周期控制（每日/每周/每月/赛季）
        // 全局任务：在创建全局任务时设置过期时间
        boolean active = false;
        long expireDate = -1L; // -1 表示不过期，由周期控制

        return new QuestData(uuid, this.id, objectives, rewardIds, scale, xpReward, active, expireDate);
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
    public UniInt getObjectivesAmount() {
        return this.objectivesAmount;
    }

    public void setObjectivesAmount(@NotNull UniInt objectivesAmount) {
        this.objectivesAmount = objectivesAmount;
    }

    @NotNull
    public QuestObjectiveTable getObjectiveTable() {
        return this.objectiveTable;
    }

    public void setObjectiveTable(@NotNull QuestObjectiveTable objectiveTable) {
        this.objectiveTable = objectiveTable;
    }

    public long getCompletionTime() {
        return this.completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    @NotNull
    public QuestXPReward getBattlePassXPReward() {
        return this.battlePassXPReward;
    }

    public void setBattlePassXPReward(double base, double unitBonus) {
        this.setBattlePassXPReward(new QuestXPReward(base, unitBonus));
    }

    public void setBattlePassXPReward(@NotNull QuestXPReward xpReward) {
        this.battlePassXPReward = xpReward;
    }

    @NotNull
    public List<String> getRewards() {
        return this.rewards;
    }

    public void addReward(@NotNull String rewardId) {
        this.rewards.add(rewardId);
    }
    
    @NotNull
    public List<String> getRewardLore() {
        return this.rewardLore;
    }
    
    public void setRewardLore(@NotNull List<String> rewardLore) {
        this.rewardLore = rewardLore;
    }
    
    @NotNull
    public Map<Integer, List<String>> getRankingRewards() {
        return this.rankingRewards;
    }
    
    public void setRankingRewards(@NotNull Map<Integer, List<String>> rankingRewards) {
        this.rankingRewards = rankingRewards;
    }
    
    public void addRankingReward(int rank, @NotNull List<String> commands) {
        this.rankingRewards.put(rank, new ArrayList<>(commands));
    }
    
    @Nullable
    public List<String> getRankingRewardForRank(int rank) {
        return this.rankingRewards.get(rank);
    }
    
    @NotNull
    public Map<Integer, Integer> getRankingBattlePassXP() {
        return this.rankingBattlePassXP;
    }
    
    public void setRankingBattlePassXP(@NotNull Map<Integer, Integer> rankingBattlePassXP) {
        this.rankingBattlePassXP = rankingBattlePassXP;
    }
    
    public int getRankingBattlePassXPForRank(int rank) {
        return this.rankingBattlePassXP.getOrDefault(rank, 0);
    }
    
    @NotNull
    public Map<Integer, List<String>> getRankingRewardLore() {
        return this.rankingRewardLore;
    }
    
    public void setRankingRewardLore(@NotNull Map<Integer, List<String>> rankingRewardLore) {
        this.rankingRewardLore = rankingRewardLore;
    }
    
    @Nullable
    public List<String> getRankingRewardLoreForRank(int rank) {
        return this.rankingRewardLore.get(rank);
    }
    
    // 新增：任务类型相关getter和setter
    @NotNull
    public QuestType getQuestType() {
        return this.questType;
    }
    
    public void setQuestType(@NotNull QuestType questType) {
        this.questType = questType;
    }
    
    @NotNull
    public String getCooperativeFormula() {
        return this.cooperativeFormula;
    }
    
    public void setCooperativeFormula(@NotNull String cooperativeFormula) {
        this.cooperativeFormula = cooperativeFormula;
    }
    
    /**
     * 计算合作任务的目标数量倍数
     * @param maxOnlinePlayers 当日最高在线人数
     * @return 计算后的倍数
     */
    public double calculateCooperativeScale(int maxOnlinePlayers) {
        if (this.questType != QuestType.COOPERATIVE) {
            return 1.0;
        }
        
        try {
            String formula = this.cooperativeFormula.replace("%maxplayer%", String.valueOf(maxOnlinePlayers));
            // 简单的公式解析：支持基本的算术运算
            return evaluateFormula(formula);
        } catch (Exception e) {
            return 1.0;
        }
    }
    
    /**
     * 简单的公式计算器，支持 +, -, *, / 运算
     */
    private double evaluateFormula(String formula) {
        formula = formula.trim().replaceAll("\\s+", "");
        
        // 处理乘法和除法
        while (formula.contains("*") || formula.contains("/")) {
            int multIndex = formula.indexOf("*");
            int divIndex = formula.indexOf("/");
            
            int opIndex;
            char operator;
            if (multIndex >= 0 && (divIndex < 0 || multIndex < divIndex)) {
                opIndex = multIndex;
                operator = '*';
            } else {
                opIndex = divIndex;
                operator = '/';
            }
            
            // 提取操作数
            int leftStart = opIndex - 1;
            while (leftStart > 0 && (Character.isDigit(formula.charAt(leftStart - 1)) || formula.charAt(leftStart - 1) == '.')) {
                leftStart--;
            }
            
            int rightEnd = opIndex + 2;
            while (rightEnd < formula.length() && (Character.isDigit(formula.charAt(rightEnd)) || formula.charAt(rightEnd) == '.')) {
                rightEnd++;
            }
            
            double left = Double.parseDouble(formula.substring(leftStart, opIndex));
            double right = Double.parseDouble(formula.substring(opIndex + 1, rightEnd));
            double result = operator == '*' ? left * right : left / right;
            
            formula = formula.substring(0, leftStart) + result + formula.substring(rightEnd);
        }
        
        // 处理加法和减法
        double result = 0;
        String[] parts = formula.split("(?=[+-])");
        for (String part : parts) {
            if (!part.isEmpty()) {
                result += Double.parseDouble(part);
            }
        }
        
        return result;
    }
    
    public int getCompetitiveMaxCompletions() {
        return this.competitiveMaxCompletions;
    }
    
    public void setCompetitiveMaxCompletions(int competitiveMaxCompletions) {
        this.competitiveMaxCompletions = competitiveMaxCompletions;
    }
    
    public double getCompetitiveMaxCompletionPercent() {
        return this.competitiveMaxCompletionPercent;
    }
    
    public void setCompetitiveMaxCompletionPercent(double competitiveMaxCompletionPercent) {
        this.competitiveMaxCompletionPercent = Math.max(0.0, Math.min(1.0, competitiveMaxCompletionPercent));
    }
    
    public boolean isCompetitiveUsePercent() {
        return this.competitiveUsePercent;
    }
    
    public void setCompetitiveUsePercent(boolean competitiveUsePercent) {
        this.competitiveUsePercent = competitiveUsePercent;
    }
    
    /**
     * 计算竞争任务的最大完成人数
     * @param totalOnlinePlayers 当前总在线人数（用于百分比模式）
     * @return 最大完成人数
     */
    public int calculateCompetitiveMaxCompletions(int totalOnlinePlayers) {
        if (this.questType != QuestType.COMPETITIVE) {
            return Integer.MAX_VALUE;
        }
        
        if (this.competitiveUsePercent) {
            return Math.max(1, (int) Math.ceil(totalOnlinePlayers * this.competitiveMaxCompletionPercent));
        } else {
            return Math.max(1, this.competitiveMaxCompletions);
        }
    }
    
    @NotNull
    public QuestPeriod getQuestPeriod() {
        return this.questPeriod;
    }
    
    public void setQuestPeriod(@NotNull QuestPeriod questPeriod) {
        this.questPeriod = questPeriod;
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
