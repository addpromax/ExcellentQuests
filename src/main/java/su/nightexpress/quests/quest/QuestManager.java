package su.nightexpress.quests.quest;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.random.Rnd;
import su.nightexpress.quests.QuestsPlaceholders;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.api.exception.QuestLoadException;
import su.nightexpress.quests.battlepass.BattlePassManager;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.quest.command.QuestsCommands;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.data.QuestCounter;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestXPReward;
import su.nightexpress.quests.quest.listener.QuestGenericListener;
import su.nightexpress.quests.quest.menu.QuestsEntryMenu;
import su.nightexpress.quests.quest.menu.CooperativeQuestsMenu;
import su.nightexpress.quests.quest.menu.CompetitiveQuestsMenu;
import su.nightexpress.quests.quest.menu.UnifiedIndependentQuestsMenu;
import su.nightexpress.quests.quest.menu.LoreTemplateManager;
import su.nightexpress.quests.quest.refresh.QuestRefreshConfig;
import su.nightexpress.quests.quest.refresh.QuestRefreshManager;
import su.nightexpress.quests.reward.Reward;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.user.QuestUser;
import su.nightexpress.quests.util.QuestUtils;
import su.nightexpress.quests.util.TagMatcher;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class QuestManager extends AbstractManager<QuestsPlugin> {

    private final Map<String, Quest> questById;
    private final String dirPath;

    private QuestsEntryMenu questsEntryMenu;
    private UnifiedIndependentQuestsMenu unifiedIndependentQuestsMenu;
    private CooperativeQuestsMenu cooperativeQuestsMenu;
    private CompetitiveQuestsMenu competitiveQuestsMenu;
    private GlobalQuestManager globalQuestManager;
    private QuestRefreshManager questRefreshManager;
    private LoreTemplateManager loreTemplateManager;
    private su.nightexpress.quests.util.QuestItemBuilder questItemBuilder;

    public QuestManager(@NotNull QuestsPlugin plugin) {
        super(plugin);
        this.questById = new HashMap<>();
        this.dirPath = this.plugin.getDataFolder() + Config.DIR_QUESTS;
        this.globalQuestManager = new GlobalQuestManager(this.plugin);
        this.loreTemplateManager = new LoreTemplateManager(this.plugin);
        this.questItemBuilder = new su.nightexpress.quests.util.QuestItemBuilder(this);
    }

    @Override
    protected void onLoad() {
        this.loadQuests();
        this.loadUI();
        
        // 初始化任务刷新管理器
        FileConfig refreshConfigFile = FileConfig.loadOrExtract(this.plugin, "quest_refresh_config.yml");
        QuestRefreshConfig refreshConfig = QuestRefreshConfig.load(refreshConfigFile);
        this.questRefreshManager = new QuestRefreshManager(this.plugin, this, refreshConfig, this.globalQuestManager);
        this.plugin.info("任务刷新管理器已初始化");

        this.addListener(new QuestGenericListener(this.plugin, this));
        
        // 定期更新在线人数统计
        this.addAsyncTask(() -> {
            int currentOnline = Players.getOnline().size();
            this.globalQuestManager.updateDailyMaxOnline(currentOnline);
        }, 20L * 60L); // 每分钟更新一次
        
        // 定期保存全局任务数据
        this.addAsyncTask(this::saveGlobalQuests, 20L * 60L * 5L); // 每5分钟保存一次
        
        // 定时任务：每分钟检查是否到了刷新时间点
        this.addAsyncTask(this::checkAndRefreshAllOnlinePlayers, 20L * 60L);
        
        // 立即加载全局任务（在reload时需要立即加载，而不是延迟）
        this.plugin.info("正在从数据库加载全局任务...");
        this.loadGlobalQuests();
        this.plugin.info("全局任务加载完成");
        
        // 启动时清理过期的全局任务
        this.plugin.info("清理过期的全局任务...");
        this.globalQuestManager.cleanExpiredQuests();
        
        // 检查并修复从数据库加载的合作任务
        this.fixLoadedCooperativeQuests();
        
        // 启动时只刷新全局任务（合作和竞争），独立任务在玩家登录时刷新
        this.questRefreshManager.checkAndRefreshGlobalOnly();
        this.plugin.info("任务刷新管理器已启动");

        QuestsCommands.load(this.plugin, this);
    }

    @Override
    protected void onShutdown() {
        this.saveGlobalQuests();
        this.questById.clear();
        
        // 清空全局任务数据和所有玩家的全局任务引用
        this.globalQuestManager.clearAllGlobalQuests();
        QuestsCommands.shutdown();
    }
    
    
    private void loadGlobalQuests() {
        try {
            // 从数据库加载全局任务
            java.util.List<su.nightexpress.quests.quest.data.GlobalQuestData> globalQuests = 
                this.plugin.getDataHandler().loadGlobalQuests();
            
            if (globalQuests != null && !globalQuests.isEmpty()) {
                int validCount = 0;
                int invalidCount = 0;
                
                for (su.nightexpress.quests.quest.data.GlobalQuestData globalData : globalQuests) {
                    // 验证全局任务数据的有效性
                    if (isGlobalQuestDataValid(globalData)) {
                        this.globalQuestManager.addGlobalQuestData(globalData);
                        validCount++;
                    } else {
                        this.plugin.warn("跳过无效的全局任务数据: " + globalData.getQuestId() + " (ID: " + globalData.getId() + ")");
                        this.plugin.warn("  原因: 目标为空或任务已过期");
                        // 从数据库中删除无效任务
                        this.plugin.getDataHandler().removeGlobalQuest(globalData);
                        invalidCount++;
                    }
                }
                
                this.plugin.info("加载了 " + validCount + " 个有效的全局任务" + 
                    (invalidCount > 0 ? " (清理了 " + invalidCount + " 个无效任务)" : ""));
            }
        } catch (Exception e) {
            this.plugin.error("Failed to load global quests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 验证全局任务数据是否有效
     */
    private boolean isGlobalQuestDataValid(su.nightexpress.quests.quest.data.GlobalQuestData globalData) {
        // 检查任务是否过期
        if (globalData.isExpired()) {
            return false;
        }
        
        // 合作任务：如果需要初始化目标，则认为有效（等待玩家上线初始化）
        if (globalData.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE && 
            globalData.needsObjectiveInitialization()) {
            return true;
        }
        
        // 其他情况：目标不能为空
        if (globalData.getObjectiveCounterMap().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    private void saveGlobalQuests() {
        try {
            // 更新所有全局任务到数据库
            java.util.Collection<su.nightexpress.quests.quest.data.GlobalQuestData> globalQuests = 
                this.globalQuestManager.getAllGlobalQuests();
            
            for (su.nightexpress.quests.quest.data.GlobalQuestData globalQuest : globalQuests) {
                this.plugin.getDataHandler().updateGlobalQuest(globalQuest);
            }
        } catch (Exception e) {
            this.plugin.error("Failed to save global quests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 修复从数据库加载的合作任务
     * 确保合作任务的目标数量和当前在线人数相匹配
     */
    private void fixLoadedCooperativeQuests() {
        List<GlobalQuestData> cooperativeQuests = this.globalQuestManager.getGlobalQuestsByType(
            su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE
        );
        
        if (cooperativeQuests.isEmpty()) {
            return;
        }
        
        this.plugin.debug("[合作任务修复] 检查 " + cooperativeQuests.size() + " 个合作任务");
        
        for (GlobalQuestData globalData : cooperativeQuests) {
            Quest quest = this.getQuestById(globalData.getQuestId());
            if (quest == null) continue;
            
            // 如果目标为空，标记需要初始化
            if (globalData.getObjectiveCounterMap().isEmpty()) {
                this.plugin.debug("[合作任务修复] 任务 " + quest.getId() + " 目标为空，标记为需要初始化");
                globalData.setNeedsObjectiveInitialization(true);
            }
        }
        
        this.saveGlobalQuests();
    }
    
    
    /**
     * 初始化待初始化的合作任务（目标为空的任务）
     */
    private void initializePendingCooperativeQuests() {
        List<GlobalQuestData> pendingQuests = this.globalQuestManager.getGlobalQuestsByType(
            su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE
        ).stream()
            .filter(GlobalQuestData::needsObjectiveInitialization)
            .toList();
        
        if (pendingQuests.isEmpty()) {
            return;
        }
        
        int currentOnline = Players.getOnline().size();
        // 先更新当前在线人数，确保getTodayMaxOnlinePlayers()能获取到正确的值
        this.globalQuestManager.updateDailyMaxOnline(currentOnline);
        int maxOnline = this.globalQuestManager.getTodayMaxOnlinePlayers();
        
        this.plugin.debug("[合作任务初始化] 检测到 " + pendingQuests.size() + " 个待初始化任务");
        this.plugin.debug("[合作任务初始化] 当前在线: " + currentOnline + ", 今日最高: " + maxOnline);
        
        for (GlobalQuestData globalData : pendingQuests) {
            Quest quest = this.getQuestById(globalData.getQuestId());
            if (quest == null) continue;
            
            this.plugin.debug("[合作任务初始化] 正在初始化任务: " + quest.getId() + " (全局ID: " + globalData.getId() + ")");
            
            // 为合作任务生成目标
            boolean success = this.globalQuestManager.initializeCooperativeQuestObjectives(quest, globalData, maxOnline);
            
            if (success) {
                this.plugin.debug("[合作任务初始化] ✓ 成功初始化任务: " + quest.getId());
                this.plugin.debug("[合作任务初始化]   - 目标数量: " + globalData.getObjectiveCounterMap().size());
                globalData.getObjectiveCounterMap().forEach((name, counter) -> {
                    this.plugin.debug("[合作任务初始化]     * " + name + ": 0/" + counter.getRequired());
                });
            } else {
                this.plugin.warn("[合作任务初始化] ✗ 初始化失败: " + quest.getId());
            }
        }
        
        // 保存更新后的全局任务数据
        if (!pendingQuests.isEmpty()) {
            this.saveGlobalQuests();
        }
    }

    private void loadQuests() {
        File dir = new File(this.dirPath);
        if (!dir.exists() && dir.mkdirs()) {
            QuestDefaults.createQuests(this);
        }

        int successCount = 0;
        int failCount = 0;
        
        // 递归扫描所有子目录中的配置文件
        try {
            java.util.List<File> questFiles = new java.util.ArrayList<>();
            java.nio.file.Files.walk(dir.toPath())
                .filter(path -> path.toString().endsWith(".yml"))
                .map(java.nio.file.Path::toFile)
                .forEach(questFiles::add);
            
            this.plugin.info("开始加载任务配置文件...");
            
            for (File file : questFiles) {
                String id = Strings.filterForVariable(FileConfig.getName(file));
                Quest quest = new Quest(file, id);
                
                try {
                    quest.load();
                    this.questById.put(quest.getId(), quest);
                    successCount++;
                    this.plugin.info("  ✓ 成功加载: " + quest.getName() + " (" + quest.getId() + ")");
                }
                catch (QuestLoadException exception) {
                    failCount++;
                    this.plugin.error("  ✗ 加载失败: " + file.getName() + " - " + exception.getMessage());
                }
            }
        } catch (java.io.IOException e) {
            this.plugin.error("扫描任务目录失败: " + e.getMessage());
        }

        // 输出加载统计
        if (failCount > 0) {
            this.plugin.warn("任务加载完成: 成功 " + successCount + " 个, 失败 " + failCount + " 个");
        } else {
            this.plugin.info("任务加载完成: 成功加载 " + successCount + " 个任务");
        }
        
        // 按类型统计
        long independentCount = this.questById.values().stream()
            .filter(q -> q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT)
            .count();
        long cooperativeCount = this.questById.values().stream()
            .filter(q -> q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE)
            .count();
        long competitiveCount = this.questById.values().stream()
            .filter(q -> q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE)
            .count();
        
        this.plugin.info("  - 独立任务: " + independentCount + " 个");
        this.plugin.info("  - 合作任务: " + cooperativeCount + " 个");
        this.plugin.info("  - 竞争任务: " + competitiveCount + " 个");
    }

    private void loadUI() {
        this.questsEntryMenu = this.addMenu(new QuestsEntryMenu(this.plugin, this), Config.DIR_MENU, "quests_entry.yml");
        this.unifiedIndependentQuestsMenu = this.addMenu(new UnifiedIndependentQuestsMenu(this.plugin, this), Config.DIR_MENU, "independent_quests.yml");
        this.cooperativeQuestsMenu = this.addMenu(new CooperativeQuestsMenu(this.plugin, this), Config.DIR_MENU, "cooperative_quests.yml");
        this.competitiveQuestsMenu = this.addMenu(new CompetitiveQuestsMenu(this.plugin, this), Config.DIR_MENU, "competitive_quests.yml");
    }

    public boolean isQuestsAvailable() {
        if (Config.isQuestsForBattlePass()) {
            return this.plugin.battlePassManager().map(BattlePassManager::isSeasonActive).orElse(false);
        }

        return true;
    }

    public void createQuest(@NotNull String name, @NotNull Consumer<Quest> consumer) {
        String id = Strings.filterForVariable(name);
        if (this.getQuestById(id) != null) return;

        File file = new File(this.dirPath, FileConfig.withExtension(id));
        Quest quest = new Quest(file, id);
        consumer.accept(quest);
        quest.save();
    }

    public void openQuests(@NotNull Player player) {
        if (!this.isQuestsAvailable()) {
            Lang.QUESTS_LOCKED.message().send(player);
            return;
        }
        // 打开任务入口菜单
        this.questsEntryMenu.open(player);
    }

    public void openIndependentQuests(@NotNull Player player) {
        if (!this.isQuestsAvailable()) {
            Lang.QUESTS_LOCKED.message().send(player);
            return;
        }
        this.unifiedIndependentQuestsMenu.open(player);
    }

    public void openCooperativeQuests(@NotNull Player player) {
        if (!this.isQuestsAvailable()) {
            Lang.QUESTS_LOCKED.message().send(player);
            return;
        }
        this.cooperativeQuestsMenu.open(player);
    }

    public void openCompetitiveQuests(@NotNull Player player) {
        if (!this.isQuestsAvailable()) {
            Lang.QUESTS_LOCKED.message().send(player);
            return;
        }
        this.competitiveQuestsMenu.open(player);
    }

    public void openUnifiedIndependentQuests(@NotNull Player player) {
        if (!this.isQuestsAvailable()) {
            Lang.QUESTS_LOCKED.message().send(player);
            return;
        }
        this.unifiedIndependentQuestsMenu.open(player);
    }

    public void refreshAllQuests(@NotNull Player player) {
        this.plugin.debug("[刷新所有任务] 玩家: " + player.getName());
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        
        // 先清理无效任务（配置文件已删除的任务）
        int removedInvalid = 0;
        List<UUID> invalidIds = new java.util.ArrayList<>();
        for (Map.Entry<UUID, QuestData> entry : user.getQuestData().entrySet()) {
            Quest quest = this.getQuestById(entry.getValue().getQuestId());
            if (quest == null) {
                invalidIds.add(entry.getKey());
                removedInvalid++;
                this.plugin.debug("[刷新所有任务] 清理无效任务: " + entry.getValue().getQuestId());
            }
        }
        invalidIds.forEach(user.getQuestData()::remove);
        
        if (removedInvalid > 0) {
            this.plugin.debug("[刷新所有任务] 清理了 " + removedInvalid + " 个无效任务");
        }
        
        // 统计当前任务类型
        int totalBefore = user.getQuestData().size();
        int independentBefore = 0;
        int cooperativeBefore = 0;
        int competitiveBefore = 0;
        
        for (QuestData questData : user.getQuestData().values()) {
            Quest quest = this.getQuestById(questData.getQuestId());
            if (quest != null) {
                switch (quest.getQuestType()) {
                    case INDEPENDENT:
                        independentBefore++;
                        break;
                    case COOPERATIVE:
                        cooperativeBefore++;
                        break;
                    case COMPETITIVE:
                        competitiveBefore++;
                        break;
                }
            }
        }
        
        this.plugin.debug("[刷新所有任务] 刷新前统计 - 总数: " + totalBefore + 
            ", 独立: " + independentBefore + 
            ", 合作: " + cooperativeBefore + 
            ", 竞争: " + competitiveBefore);
        
        // 移除所有个人任务（独立任务）
        int removedIndependent = 0;
        List<UUID> independentIds = new java.util.ArrayList<>();
        for (Map.Entry<UUID, QuestData> entry : user.getQuestData().entrySet()) {
            Quest quest = this.getQuestById(entry.getValue().getQuestId());
            if (quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT) {
                independentIds.add(entry.getKey());
                removedIndependent++;
                this.plugin.debug("[刷新所有任务] 移除独立任务: " + quest.getId());
            }
        }
        independentIds.forEach(user.getQuestData()::remove);
        this.plugin.debug("[刷新所有任务] 移除独立任务: " + removedIndependent + " 个");
        
        // 重新生成个人任务
        this.generateQuestsByType(player, user, su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT);
        
        // 刷新全局任务（合作和竞争）
        this.plugin.debug("[刷新所有任务] 开始刷新全局任务");
        this.forceRefreshCooperativeQuestsGlobal();
        this.forceRefreshCompetitiveQuestsGlobal();
        
        // 同步全局任务到玩家
        this.plugin.debug("[刷新所有任务] 同步全局任务到玩家");
        this.generateQuestsByType(player, user, su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE);
        this.generateQuestsByType(player, user, su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE);
        
        this.plugin.getUserManager().save(user);
        
        // 立即保存到数据库，避免玩家重新登录时数据被覆盖
        this.plugin.getUserManager().saveInDatabase(user);
        
        // 统计最终结果
        int totalAfter = user.getQuestData().size();
        int independentAfter = 0;
        int cooperativeAfter = 0;
        int competitiveAfter = 0;
        
        for (QuestData questData : user.getQuestData().values()) {
            Quest quest = this.getQuestById(questData.getQuestId());
            if (quest != null) {
                switch (quest.getQuestType()) {
                    case INDEPENDENT:
                        independentAfter++;
                        break;
                    case COOPERATIVE:
                        cooperativeAfter++;
                        break;
                    case COMPETITIVE:
                        competitiveAfter++;
                        break;
                }
            }
        }
        
        this.plugin.debug("[刷新所有任务] 完成 - 总数: " + totalAfter + 
            ", 独立: " + independentAfter + 
            ", 合作: " + cooperativeAfter + 
            ", 竞争: " + competitiveAfter);
        
        // 刷新所有打开的界面
        if (this.unifiedIndependentQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.unifiedIndependentQuestsMenu.flush(player));
        }
        if (this.cooperativeQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.cooperativeQuestsMenu.flush(player));
        }
        if (this.competitiveQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.competitiveQuestsMenu.flush(player));
        }
    }
    
    public void refreshIndependentQuests(@NotNull Player player) {
        this.plugin.debug("[刷新独立任务] 玩家: " + player.getName());
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        
        // 移除所有个人任务
        int removedCount = 0;
        List<UUID> toRemove = new java.util.ArrayList<>();
        for (Map.Entry<UUID, QuestData> entry : user.getQuestData().entrySet()) {
            Quest quest = this.getQuestById(entry.getValue().getQuestId());
            if (quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT) {
                toRemove.add(entry.getKey());
                removedCount++;
            }
        }
        toRemove.forEach(user.getQuestData()::remove);
        this.plugin.debug("[刷新独立任务] 移除旧任务: " + removedCount + " 个");
        
        // 重新生成个人任务
        this.generateQuestsByType(player, user, su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT);
        
        this.plugin.getUserManager().save(user);
        
        long independentCount = user.getQuestData().values().stream()
            .filter(data -> {
                Quest q = this.getQuestById(data.getQuestId());
                return q != null && q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT;
            })
            .count();
        this.plugin.debug("[刷新独立任务] 完成，当前独立任务数: " + independentCount);
        
        // 刷新界面
        if (this.unifiedIndependentQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.unifiedIndependentQuestsMenu.flush(player));
        }
    }
    
    public void refreshCooperativeQuests(@NotNull Player player) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        
        // 移除所有合作任务
        user.getQuestDatas().removeIf(questData -> {
            Quest quest = this.getQuestById(questData.getQuestId());
            return quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE;
        });
        
        // 重新生成合作任务（从全局任务中同步）
        this.generateQuestsByType(player, user, su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE);
        
        this.plugin.getUserManager().save(user);
        
        // 刷新界面
        if (this.cooperativeQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.cooperativeQuestsMenu.flush(player));
        }
    }
    
    public void refreshCompetitiveQuests(@NotNull Player player) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        
        // 移除所有竞争任务
        user.getQuestDatas().removeIf(questData -> {
            Quest quest = this.getQuestById(questData.getQuestId());
            return quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE;
        });
        
        // 重新生成竞争任务（从全局任务中同步）
        this.generateQuestsByType(player, user, su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE);
        
        this.plugin.getUserManager().save(user);
        
        // 刷新界面
        if (this.competitiveQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.competitiveQuestsMenu.flush(player));
        }
    }
    
    /**
     * 刷新全服的合作任务
     * 只添加新任务，不删除旧任务，任务通过过期时间自然消失
     */
    public void refreshCooperativeQuestsGlobal(int maxOnlinePlayers) {
        refreshCooperativeQuestsGlobal(maxOnlinePlayers, false);
    }
    
    /**
     * 刷新全服的合作任务
     * @param maxOnlinePlayers 最大在线玩家数
     * @param force 是否强制刷新（清空旧任务）
     */
    public void refreshCooperativeQuestsGlobal(int maxOnlinePlayers, boolean force) {
        this.plugin.debug("[刷新合作任务] 开始刷新，强制模式: " + force);
        
        // 第一步：清理已过期的任务
        this.globalQuestManager.cleanExpiredQuests();
        this.plugin.debug("[刷新合作任务] 已清理过期任务");
        
        // 第二步：检查配置的任务数量上限（根据在线人数动态计算）
        int maxQuests;
        if (this.questRefreshManager != null) {
            maxQuests = this.questRefreshManager.getConfig().getQuestCountByOnline(
                su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE, 
                maxOnlinePlayers
            );
        } else {
            maxQuests = 2; // 默认值
        }
        
        this.plugin.debug("当日最高在线人数: " + maxOnlinePlayers);
        this.plugin.debug("配置的合作任务上限: " + maxQuests);
        
        // 第三步：统计当前有效的合作任务数量
        long currentCount = this.globalQuestManager.getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE)
            .filter(data -> !data.isExpired())
            .count();
        
        this.plugin.debug("当前有效合作任务数量: " + currentCount);
        
        // 第四步：如果当前任务数量超过上限，或者是强制刷新，则删除所有旧任务
        if (force || currentCount > maxQuests) {
            this.plugin.debug("[刷新合作任务] 需要清空旧任务 (强制刷新: " + force + ", 超过上限: " + (currentCount > maxQuests) + ")");
            
            // 获取所有合作任务的ID
            List<UUID> cooperativeQuestIds = this.globalQuestManager.getAllGlobalQuests().stream()
                .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE)
                .map(GlobalQuestData::getId)
                .collect(java.util.stream.Collectors.toList());
            
            // 删除所有合作任务
            cooperativeQuestIds.forEach(this.globalQuestManager::removeGlobalQuestData);
            
            // 从所有在线玩家中删除合作任务引用
            Players.getOnline().forEach(player -> {
                QuestUser user = this.plugin.getUserManager().getOrFetch(player);
                user.getQuestDatas().removeIf(questData -> {
                    Quest quest = this.getQuestById(questData.getQuestId());
                    return quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE;
                });
                this.plugin.getUserManager().save(user);
            });
            
            this.plugin.debug("[刷新合作任务] 已清空 " + cooperativeQuestIds.size() + " 个合作任务");
            currentCount = 0; // 重置计数
        }
        
        // 第五步：如果已达上限且不需要清空，跳过刷新
        if (!force && currentCount >= maxQuests) {
            this.plugin.debug("[刷新合作任务] 合作任务已达到上限 (" + currentCount + "/" + maxQuests + ")，跳过刷新");
            this.plugin.debug("[刷新合作任务] 刷新完成（无需添加）");
            return;
        }
        
        // 第六步：计算需要生成的数量
        int toGenerate = maxQuests - (int)currentCount;
        this.plugin.debug("需要生成的合作任务数量: " + toGenerate);
        
        // 获取所有合作类型的任务定义
        List<Quest> cooperativeQuests = this.questById.values().stream()
            .filter(quest -> quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE)
            .collect(java.util.stream.Collectors.toList());
        
        this.plugin.debug("可用的合作任务配置数量: " + cooperativeQuests.size());
        
        if (cooperativeQuests.isEmpty()) {
            this.plugin.warn("[刷新合作任务] 没有可用的合作任务配置");
            this.plugin.debug("[刷新合作任务] 刷新完成（无可用配置）");
            return;
        }
        
        // 第六步：生成新的全局任务数据
        this.plugin.debug("开始生成全局任务...");
        List<su.nightexpress.quests.quest.data.GlobalQuestData> generatedGlobalQuests = new java.util.ArrayList<>();
        int generated = 0;
        int attempts = 0;
        while (generated < toGenerate && !cooperativeQuests.isEmpty()) {
            Quest quest = cooperativeQuests.remove(Rnd.nextInt(cooperativeQuests.size()));
            attempts++;
            
            this.plugin.debug("尝试 #" + attempts + " - 生成任务: " + quest.getId());
            this.plugin.debug("  - 目标表大小: " + quest.getObjectiveTable().getEntryMap().size());
            this.plugin.debug("  - 需要生成目标数: " + quest.getObjectivesAmount().roll());
            
            long expireDate = TimeUtil.toEpochMillis(QuestUtils.getNewDayMidnight());
            su.nightexpress.quests.quest.data.GlobalQuestData globalData = this.globalQuestManager.createGlobalQuest(quest, expireDate);
            
            if (globalData != null) {
                // 插入到数据库
                this.plugin.getDataHandler().insertGlobalQuest(globalData);
                
                generatedGlobalQuests.add(globalData);
                generated++;
                this.plugin.debug("  ✓ 成功 - 全局ID: " + globalData.getId());
                this.plugin.debug("  - 目标数量: " + globalData.getObjectiveCounterMap().size());
                this.plugin.debug("  - 是否需要初始化: " + globalData.needsObjectiveInitialization());
                if (!globalData.getObjectiveCounterMap().isEmpty()) {
                    globalData.getObjectiveCounterMap().forEach((name, counter) -> {
                        this.plugin.debug("    * " + name + ": " + counter.getCompleted() + "/" + counter.getRequired());
                    });
                }
            } else {
                this.plugin.error("[DEBUG]   ✗ 失败 - 任务创建返回 null (详见上方错误信息)");
            }
        }
        
        this.plugin.debug("任务生成完成 - 成功: " + generated + "/" + toGenerate + ", 尝试次数: " + attempts);
        
        // 第八步：通知所有在线玩家并刷新界面（仅当生成了新任务时）
        if (generated > 0) {
            List<org.bukkit.entity.Player> onlinePlayers = new java.util.ArrayList<>(Players.getOnline());
            this.plugin.debug("在线玩家数: " + onlinePlayers.size());
            
            final int finalGenerated = generated;
            final int finalMaxOnlinePlayers = maxOnlinePlayers;
            final int finalMaxQuests = maxQuests;
            
            for (org.bukkit.entity.Player player : onlinePlayers) {
                // 刷新界面
                if (this.cooperativeQuestsMenu.isViewer(player)) {
                    this.plugin.runTask(() -> this.cooperativeQuestsMenu.flush(player));
                }
                
                // 发送通知（带有在线人数和任务数量信息）
                Lang.QUESTS_REFRESHED_COOPERATIVE_NOTIFY_WITH_COUNT.message().send(player, replacer -> replacer
                    .replace("%quest_count%", String.valueOf(finalGenerated))
                    .replace("%max_online%", String.valueOf(finalMaxOnlinePlayers))
                    .replace("%total_quests%", String.valueOf(finalMaxQuests))
                );
            }
        }
        
        // 保存全局任务数据
        this.plugin.debug("保存全局任务数据...");
        this.saveGlobalQuests();
        
        this.plugin.debug("[刷新合作任务] 当前全局任务总数: " + this.globalQuestManager.getAllGlobalQuests().size());
        this.plugin.debug("[刷新合作任务] 刷新完成");
    }
    
    /**
     * 刷新全服的竞争任务
     * 只添加新任务，不删除旧任务，任务通过过期时间自然消失
     */
    public void refreshCompetitiveQuestsGlobal(int maxOnlinePlayers) {
        refreshCompetitiveQuestsGlobal(maxOnlinePlayers, false);
    }
    
    /**
     * 刷新全服的竞争任务
     * @param maxOnlinePlayers 最大在线玩家数
     * @param force 是否强制刷新（清空旧任务）
     */
    public void refreshCompetitiveQuestsGlobal(int maxOnlinePlayers, boolean force) {
        this.plugin.debug("[刷新竞争任务] 开始刷新，强制模式: " + force);
        
        // 第一步：清理已过期的任务
        this.globalQuestManager.cleanExpiredQuests();
        this.plugin.debug("[刷新竞争任务] 已清理过期任务");
        
        // 第二步：检查配置的任务数量上限（根据在线人数动态计算）
        int maxQuests;
        if (this.questRefreshManager != null) {
            maxQuests = this.questRefreshManager.getConfig().getQuestCountByOnline(
                su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE, 
                maxOnlinePlayers
            );
        } else {
            maxQuests = 2; // 默认值
        }
        
        this.plugin.debug("[刷新竞争任务] 当日最高在线人数: " + maxOnlinePlayers);
        this.plugin.debug("[刷新竞争任务] 配置的竞争任务上限: " + maxQuests);
        
        // 第三步：统计当前有效的竞争任务数量
        long currentCount = this.globalQuestManager.getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE)
            .filter(data -> !data.isExpired())
            .count();
        
        this.plugin.debug("[刷新竞争任务] 当前有效竞争任务数量: " + currentCount);
        
        // 第四步：如果当前任务数量超过上限，或者是强制刷新，则删除所有旧任务
        if (force || currentCount > maxQuests) {
            this.plugin.debug("[刷新竞争任务] 需要清空旧任务 (强制刷新: " + force + ", 超过上限: " + (currentCount > maxQuests) + ")");
            
            // 获取所有竞争任务的ID
            List<UUID> competitiveQuestIds = this.globalQuestManager.getAllGlobalQuests().stream()
                .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE)
                .map(GlobalQuestData::getId)
                .collect(java.util.stream.Collectors.toList());
            
            // 删除所有竞争任务
            competitiveQuestIds.forEach(this.globalQuestManager::removeGlobalQuestData);
            
            // 从所有在线玩家中删除竞争任务引用
            Players.getOnline().forEach(player -> {
                QuestUser user = this.plugin.getUserManager().getOrFetch(player);
                user.getQuestDatas().removeIf(questData -> {
                    Quest quest = this.getQuestById(questData.getQuestId());
                    return quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE;
                });
                this.plugin.getUserManager().save(user);
            });
            
            this.plugin.debug("[刷新竞争任务] 已清空 " + competitiveQuestIds.size() + " 个竞争任务");
            currentCount = 0; // 重置计数
        }
        
        // 第五步：如果已达上限且不需要清空，跳过刷新
        if (!force && currentCount >= maxQuests) {
            this.plugin.debug("[刷新竞争任务] 竞争任务已达到上限 (" + currentCount + "/" + maxQuests + ")，跳过刷新");
            this.plugin.debug("[刷新竞争任务] 刷新完成（无需添加）");
            return;
        }
        
        // 第六步：计算需要生成的数量
        int toGenerate = maxQuests - (int)currentCount;
        this.plugin.debug("[刷新竞争任务] 需要生成的竞争任务数量: " + toGenerate);
        
        // 获取所有竞争类型的任务定义
        List<Quest> competitiveQuests = this.questById.values().stream()
            .filter(quest -> quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE)
            .collect(java.util.stream.Collectors.toList());
        
        this.plugin.debug("[刷新竞争任务] 可用的竞争任务配置数量: " + competitiveQuests.size());
        
        if (competitiveQuests.isEmpty()) {
            this.plugin.warn("[刷新竞争任务] 没有可用的竞争任务配置");
            this.plugin.debug("[刷新竞争任务] 刷新完成（无可用配置）");
            return;
        }
        
        // 第六步：生成新的全局任务数据
        this.plugin.debug("开始生成全局任务...");
        List<su.nightexpress.quests.quest.data.GlobalQuestData> generatedGlobalQuests = new java.util.ArrayList<>();
        int generated = 0;
        int attempts = 0;
        while (generated < toGenerate && !competitiveQuests.isEmpty()) {
            Quest quest = competitiveQuests.remove(Rnd.nextInt(competitiveQuests.size()));
            attempts++;
            
            this.plugin.debug("尝试 #" + attempts + " - 生成任务: " + quest.getId());
            this.plugin.debug("  - 目标表大小: " + quest.getObjectiveTable().getEntryMap().size());
            this.plugin.debug("  - 需要生成目标数: " + quest.getObjectivesAmount().roll());
            
            long expireDate = TimeUtil.toEpochMillis(QuestUtils.getNewDayMidnight());
            su.nightexpress.quests.quest.data.GlobalQuestData globalData = this.globalQuestManager.createGlobalQuest(quest, expireDate);
            
            if (globalData != null) {
                // 插入到数据库
                this.plugin.getDataHandler().insertGlobalQuest(globalData);
                
                generatedGlobalQuests.add(globalData);
                generated++;
                this.plugin.debug("  ✓ 成功 - 全局ID: " + globalData.getId());
                this.plugin.debug("  - 目标数量: " + globalData.getObjectiveCounterMap().size());
                globalData.getObjectiveCounterMap().forEach((name, counter) -> {
                    this.plugin.debug("    * " + name + ": " + counter.getCompleted() + "/" + counter.getRequired());
                });
            } else {
                this.plugin.error("[DEBUG]   ✗ 失败 - 任务创建返回 null (详见上方错误信息)");
            }
        }
        
        this.plugin.debug("任务生成完成 - 成功: " + generated + "/" + toGenerate + ", 尝试次数: " + attempts);
        
        // 第八步：通知所有在线玩家并刷新界面（仅当生成了新任务时）
        if (generated > 0) {
            List<org.bukkit.entity.Player> onlinePlayers = new java.util.ArrayList<>(Players.getOnline());
            this.plugin.debug("在线玩家数: " + onlinePlayers.size());
            
            final int finalGenerated = generated;
            final int finalMaxOnlinePlayers = maxOnlinePlayers;
            final int finalMaxQuests = maxQuests;
            
            for (org.bukkit.entity.Player player : onlinePlayers) {
                // 刷新界面
                if (this.competitiveQuestsMenu.isViewer(player)) {
                    this.plugin.runTask(() -> this.competitiveQuestsMenu.flush(player));
                }
                
                // 发送通知（带有在线人数和任务数量信息）
                Lang.QUESTS_REFRESHED_COMPETITIVE_NOTIFY_WITH_COUNT.message().send(player, replacer -> replacer
                    .replace("%quest_count%", String.valueOf(finalGenerated))
                    .replace("%max_online%", String.valueOf(finalMaxOnlinePlayers))
                    .replace("%total_quests%", String.valueOf(finalMaxQuests))
                );
            }
        }
        
        // 保存全局任务数据
        this.plugin.debug("保存全局任务数据...");
        this.saveGlobalQuests();
        
        this.plugin.debug("[刷新竞争任务] 当前全局任务总数: " + this.globalQuestManager.getAllGlobalQuests().size());
        this.plugin.debug("[刷新竞争任务] 刷新完成");
    }
    
    /**
     * 刷新全服的合作任务（重载方法，用于向后兼容）
     */
    public void refreshCooperativeQuestsGlobal() {
        int maxOnlinePlayers = this.globalQuestManager.getTodayMaxOnlinePlayers();
        refreshCooperativeQuestsGlobal(maxOnlinePlayers, false);
    }
    
    /**
     * 强制刷新全服的合作任务（用于命令调用）
     */
    public void forceRefreshCooperativeQuestsGlobal() {
        int maxOnlinePlayers = this.globalQuestManager.getTodayMaxOnlinePlayers();
        refreshCooperativeQuestsGlobal(maxOnlinePlayers, true);
    }
    
    /**
     * 刷新全服的竞争任务（重载方法，用于向后兼容）
     */
    public void refreshCompetitiveQuestsGlobal() {
        int maxOnlinePlayers = this.globalQuestManager.getTodayMaxOnlinePlayers();
        refreshCompetitiveQuestsGlobal(maxOnlinePlayers, false);
    }
    
    /**
     * 强制刷新全服的竞争任务（用于命令调用）
     */
    public void forceRefreshCompetitiveQuestsGlobal() {
        int maxOnlinePlayers = this.globalQuestManager.getTodayMaxOnlinePlayers();
        refreshCompetitiveQuestsGlobal(maxOnlinePlayers, true);
    }
    
    private void generateQuestsByType(@NotNull Player player, @NotNull QuestUser user, @NotNull su.nightexpress.quests.quest.definition.QuestType questType) {
        this.plugin.debug("[生成任务] 玩家: " + player.getName() + ", 类型: " + questType);
        
        // 对于独立任务，按周期分别生成
        if (questType == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT) {
            this.plugin.debug("[生成任务] 独立任务 - 按周期生成");
            // 生成每日任务
            generateIndependentQuestsByPeriod(player, user, su.nightexpress.quests.quest.definition.QuestPeriod.DAILY);
            // 生成每周任务
            generateIndependentQuestsByPeriod(player, user, su.nightexpress.quests.quest.definition.QuestPeriod.WEEKLY);
            // 生成每月任务
            generateIndependentQuestsByPeriod(player, user, su.nightexpress.quests.quest.definition.QuestPeriod.MONTHLY);
            return;
        }
        
        // 全局任务（合作和竞争）不需要在玩家数据中创建引用
        // 它们直接从 GlobalQuestManager 中读取
        if (questType == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE || 
            questType == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE) {
            this.plugin.debug("[生成任务] 全局任务 - 无需创建引用，直接从 GlobalQuestManager 读取");
            return;
        }
    }
    
    private void generateIndependentQuestsByPeriod(@NotNull Player player, @NotNull QuestUser user, 
                                                    @NotNull su.nightexpress.quests.quest.definition.QuestPeriod period) {
        // 获取该周期的独立任务
        List<Quest> availableQuests = this.questById.values().stream()
            .filter(quest -> quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT)
            .filter(quest -> quest.getQuestPeriod() == period)
            .collect(Collectors.toList());
        
        this.plugin.debug("[生成独立任务] 周期: " + period + ", 可用任务数: " + availableQuests.size());
        
        if (availableQuests.isEmpty()) {
            this.plugin.debug("[生成独立任务] 没有可用的 " + period + " 任务");
            return;
        }
        
        // 获取该周期的最大任务数量
        int maxQuests = this.getMaxQuestsForPeriod(player, period);
        this.plugin.debug("[生成独立任务] 最大任务数: " + maxQuests);
        
        if (maxQuests <= 0) {
            this.plugin.debug("[生成独立任务] 最大任务数为0，跳过生成");
            return;
        }
        
        // 统计该周期当前的任务数量
        long currentCount = user.getQuestData().values().stream()
            .filter(data -> {
                Quest q = this.getQuestById(data.getQuestId());
                return q != null 
                    && q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT
                    && q.getQuestPeriod() == period;
            })
            .count();
        
        this.plugin.debug("[生成独立任务] 当前该周期任务数: " + currentCount + ", 上限: " + maxQuests);
        
        // 无论当前任务数量是多少，都先清空该周期的所有旧任务
        // 这样可以确保任务数量始终等于上限，不会累积超出
        if (currentCount > 0) {
            this.plugin.debug("[生成独立任务] 清空该周期的所有旧任务");
            List<UUID> toRemove = new java.util.ArrayList<>();
            for (Map.Entry<UUID, QuestData> entry : user.getQuestData().entrySet()) {
                Quest q = this.getQuestById(entry.getValue().getQuestId());
                if (q != null 
                    && q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT
                    && q.getQuestPeriod() == period) {
                    toRemove.add(entry.getKey());
                }
            }
            toRemove.forEach(user.getQuestData()::remove);
            this.plugin.debug("[生成独立任务] 已清空 " + toRemove.size() + " 个旧任务");
        }
        
        // 生成新任务（严格按照上限数量）
        int generatedCount = 0;
        while (generatedCount < maxQuests && !availableQuests.isEmpty()) {
            Quest quest = availableQuests.remove(Rnd.nextInt(availableQuests.size()));
            
            QuestData questData = this.createQuestDataForPlayer(quest, user);
            if (questData == null) {
                this.plugin.debug("[生成独立任务] 创建任务数据失败: " + quest.getId());
                continue;
            }
            
            this.plugin.debug("[生成独立任务] 准备添加任务数据: " + quest.getId() + " (UUID: " + questData.getId() + ")");
            this.plugin.debug("[生成独立任务]   - 任务类型: " + quest.getQuestType());
            this.plugin.debug("[生成独立任务]   - 任务周期: " + quest.getQuestPeriod());
            this.plugin.debug("[生成独立任务]   - 是否激活: " + questData.isActive());
            this.plugin.debug("[生成独立任务]   - 目标数量: " + questData.getObjectiveCounterMap().size());
            
            user.addQuestData(questData);
            generatedCount++;
            
            // 验证是否真的添加成功
            boolean exists = user.getQuestData().containsKey(questData.getId());
            this.plugin.debug("[生成独立任务] 添加后验证 - 存在于Map中: " + exists);
            
            this.plugin.debug("[生成独立任务] 成功生成: " + quest.getId() + " (" + generatedCount + "/" + maxQuests + ")");
        }
        
        // 生成后统计
        long afterCount = user.getQuestData().values().stream()
            .filter(data -> {
                Quest q = this.getQuestById(data.getQuestId());
                return q != null 
                    && q.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT
                    && q.getQuestPeriod() == period;
            })
            .count();
        this.plugin.debug("[生成独立任务] 生成后该周期任务数: " + afterCount);
        this.plugin.debug("[生成独立任务] " + period + " 任务生成完成，共生成: " + generatedCount + " 个");
    }

    /**
     * 定时检查所有在线玩家，如果到了刷新时间则刷新任务
     * 每分钟执行一次
     */
    private void checkAndRefreshAllOnlinePlayers() {
        if (Players.getOnline().isEmpty()) {
            return;
        }
        
        // 检查是否到了刷新时间点
        java.time.LocalDateTime now = TimeUtil.getLocalDateTimeOf(System.currentTimeMillis());
        java.time.LocalTime currentTime = now.toLocalTime();
        
        // 只在接近0点的时候检查（避免错过刷新时间）
        // 检查是否在 00:00 到 00:01 之间
        if (currentTime.getHour() == 0 && currentTime.getMinute() == 0) {
            this.plugin.info("[定时刷新] 检测到刷新时间点，开始刷新所有在线玩家的任务");
            
            // 先刷新全局任务
            java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
            int dayOfMonth = now.getDayOfMonth();
            
            // 每天都刷新每日任务相关的全局任务
            this.globalQuestManager.cleanExpiredQuests();
            this.globalQuestManager.resetDailyData();
            
            // 刷新所有在线玩家的独立任务
            for (Player player : Players.getOnline()) {
                this.refreshPlayerPeriodQuests(player, su.nightexpress.quests.quest.definition.QuestPeriod.DAILY);
                
                // 周一刷新每周任务
                if (dayOfWeek == java.time.DayOfWeek.MONDAY) {
                    this.refreshPlayerPeriodQuests(player, su.nightexpress.quests.quest.definition.QuestPeriod.WEEKLY);
                }
                
                // 每月1日刷新每月任务
                if (dayOfMonth == 1) {
                    this.refreshPlayerPeriodQuests(player, su.nightexpress.quests.quest.definition.QuestPeriod.MONTHLY);
                }
            }
            
            this.plugin.info("[定时刷新] 完成，已刷新 " + Players.getOnline().size() + " 个在线玩家的任务");
        }
    }
    
    /**
     * 玩家登录时检查并刷新任务
     * 只在玩家登录时调用一次
     */
    public void checkAndRefreshPlayerQuests(@NotNull Player player) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        boolean needSave = false;
        
        // 检查并初始化需要初始化的合作任务
        this.initializePendingCooperativeQuests();
        
        // 1. 清理无效任务（配置文件已删除的任务）
        if (!user.getQuestData().isEmpty()) {
            List<UUID> toRemove = new java.util.ArrayList<>();
            
            for (Map.Entry<UUID, QuestData> entry : user.getQuestData().entrySet()) {
                QuestData questData = entry.getValue();
                Quest quest = this.getQuestById(questData.getQuestId());
                
                // 移除配置不存在的任务，或者是全局任务的引用（旧数据）
                if (quest == null || questData.isGlobalQuest()) {
                    toRemove.add(entry.getKey());
                    if (quest == null) {
                        this.plugin.debug("[玩家登录] 清理无效任务: " + questData.getQuestId());
                    } else {
                        this.plugin.debug("[玩家登录] 清理全局任务引用（旧数据）: " + questData.getQuestId());
                    }
                }
            }
            
            if (!toRemove.isEmpty()) {
                toRemove.forEach(user.getQuestData()::remove);
                this.plugin.info("[玩家登录] 为 " + player.getName() + " 清理了 " + toRemove.size() + " 个无效任务");
                needSave = true;
            }
        }
        
        if (!this.isQuestsAvailable()) {
            if (needSave) {
                this.plugin.getUserManager().save(user);
            }
            return;
        }
        
        // 2. 检查并刷新各个周期的独立任务（会自动检测是否需要刷新）
        boolean anyRefreshed = false;
        anyRefreshed |= this.updatePeriodQuests(player, user, su.nightexpress.quests.quest.definition.QuestPeriod.DAILY);
        anyRefreshed |= this.updatePeriodQuests(player, user, su.nightexpress.quests.quest.definition.QuestPeriod.WEEKLY);
        anyRefreshed |= this.updatePeriodQuests(player, user, su.nightexpress.quests.quest.definition.QuestPeriod.MONTHLY);
        
        // 3. 检查并清理超过上限的任务（即使没有到刷新时间）
        boolean cleaned = this.cleanExcessQuests(player, user);
        if (cleaned) {
            needSave = true;
        }
        
        // 4. 显示任务汇总
        if (anyRefreshed) {
            sendQuestSummary(player, user);
            needSave = true;
        }
        
        if (needSave) {
            this.plugin.getUserManager().save(user);
        }
    }
    
    /**
     * 清理超过上限的任务
     * 保留最新的任务，删除旧的任务
     */
    private boolean cleanExcessQuests(@NotNull Player player, @NotNull QuestUser user) {
        boolean cleaned = false;
        
        for (su.nightexpress.quests.quest.definition.QuestPeriod period : 
            new su.nightexpress.quests.quest.definition.QuestPeriod[]{
                su.nightexpress.quests.quest.definition.QuestPeriod.DAILY,
                su.nightexpress.quests.quest.definition.QuestPeriod.WEEKLY,
                su.nightexpress.quests.quest.definition.QuestPeriod.MONTHLY
            }) {
            
            // 获取该周期的最大任务数量
            int maxQuests = this.getMaxQuestsForPeriod(player, period);
            if (maxQuests <= 0) continue;
            
            // 获取该周期的所有任务（按创建时间排序，最新的在前）
            List<Map.Entry<UUID, QuestData>> periodQuests = user.getQuestData().entrySet().stream()
                .filter(entry -> {
                    Quest quest = this.getQuestById(entry.getValue().getQuestId());
                    return quest != null 
                        && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT
                        && quest.getQuestPeriod() == period;
                })
                .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey())) // UUID 越大越新
                .collect(Collectors.toList());
            
            // 如果超过上限，删除旧的任务
            if (periodQuests.size() > maxQuests) {
                int toRemoveCount = periodQuests.size() - maxQuests;
                this.plugin.info("[清理超限任务] 玩家 " + player.getName() + " 的 " + period + 
                    " 任务超过上限 (" + periodQuests.size() + "/" + maxQuests + ")，删除 " + toRemoveCount + " 个旧任务");
                
                // 保留前 maxQuests 个（最新的），删除其余的
                for (int i = maxQuests; i < periodQuests.size(); i++) {
                    user.getQuestData().remove(periodQuests.get(i).getKey());
                    cleaned = true;
                }
            }
        }
        
        return cleaned;
    }
    
    /**
     * 刷新玩家指定周期的独立任务（用于定时刷新）
     */
    private void refreshPlayerPeriodQuests(@NotNull Player player, @NotNull su.nightexpress.quests.quest.definition.QuestPeriod period) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        
        boolean refreshed = this.updatePeriodQuests(player, user, period);
        
        if (refreshed) {
            this.plugin.getUserManager().save(user);
            // 定时刷新时不发送消息，避免打扰玩家
        }
    }
    
    private void sendQuestSummary(@NotNull Player player, @NotNull QuestUser user) {
        // 统计独立任务数量
        int independentCount = 0;
        for (QuestData questData : user.getQuestDatas()) {
            Quest quest = this.getQuestById(questData.getQuestId());
            if (quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT) {
                independentCount++;
            }
        }
        
        // 统计全局任务数量（直接从 GlobalQuestManager 获取）
        int cooperativeCount = (int) this.globalQuestManager.getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE)
            .filter(data -> !data.isExpired())
            .count();
        
        int competitiveCount = (int) this.globalQuestManager.getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE)
            .filter(data -> !data.isExpired())
            .count();
        
        final int finalIndependentCount = independentCount;
        final int finalCooperativeCount = cooperativeCount;
        final int finalCompetitiveCount = competitiveCount;
        
        Lang.QUESTS_LOGIN_SUMMARY.message().send(player, replacer -> replacer
            .replace("%independent_count%", String.valueOf(finalIndependentCount))
            .replace("%cooperative_count%", String.valueOf(finalCooperativeCount))
            .replace("%competitive_count%", String.valueOf(finalCompetitiveCount))
        );
    }
    
    private boolean updatePeriodQuests(@NotNull Player player, @NotNull QuestUser user, @NotNull su.nightexpress.quests.quest.definition.QuestPeriod period) {
        // 检查是否到了刷新时间
        if (!user.isNewPeriodQuestsTime(period)) {
            return false;
        }
        
        // 清理过期的全局任务
        if (period == su.nightexpress.quests.quest.definition.QuestPeriod.DAILY) {
            this.globalQuestManager.cleanExpiredQuests();
            this.globalQuestManager.resetDailyData();
        }
        
        // 获取该周期的最大任务数量
        int maxQuests = this.getMaxQuestsForPeriod(player, period);
        if (maxQuests <= 0) {
            // 即使没有任务，也要更新重置时间，避免一直检查
            updateNextResetTime(user, period);
            return false;
        }
        
        // 筛选该周期的独立任务
        List<Quest> availableQuests = this.questById.values().stream()
            .filter(quest -> quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT
                && quest.getQuestPeriod() == period)
            .collect(Collectors.toList());
        
        if (availableQuests.isEmpty()) {
            // 即使没有任务配置，也要更新重置时间，避免一直检查
            updateNextResetTime(user, period);
            return false;
        }
        
        // 移除该周期的所有旧任务（无论是否完成、是否激活）
        List<UUID> toRemove = new java.util.ArrayList<>();
        for (Map.Entry<UUID, QuestData> entry : user.getQuestData().entrySet()) {
            QuestData questData = entry.getValue();
            Quest quest = this.getQuestById(questData.getQuestId());
            if (quest != null 
                && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT
                && quest.getQuestPeriod() == period) {
                toRemove.add(entry.getKey());
            }
        }
        
        // 批量删除
        int removedCount = toRemove.size();
        toRemove.forEach(user.getQuestData()::remove);
        
        if (removedCount > 0) {
            this.plugin.debug("[刷新" + period + "任务] 移除了 " + removedCount + " 个旧任务");
        }
        
        // 生成新任务（严格按照上限数量生成）
        int generatedCount = 0;
        while (generatedCount < maxQuests && !availableQuests.isEmpty()) {
            Quest quest = availableQuests.remove(Rnd.nextInt(availableQuests.size()));
            
            QuestData questData = this.createQuestDataForPlayer(quest, user);
            if (questData == null) continue;
            
            user.addQuestData(questData);
            generatedCount++;
        }
        
        // 更新下次重置时间
        updateNextResetTime(user, period);
        
        // 刷新界面
        if (this.unifiedIndependentQuestsMenu.isViewer(player)) {
            this.plugin.runTask(() -> this.unifiedIndependentQuestsMenu.flush(player));
        }
        
        return generatedCount > 0;
    }
    
    /**
     * 更新玩家的下次任务重置时间
     */
    private void updateNextResetTime(@NotNull QuestUser user, @NotNull su.nightexpress.quests.quest.definition.QuestPeriod period) {
        long nextResetTime;
        
        switch (period) {
            case DAILY:
                // 下一个午夜（明天00:00）
                nextResetTime = TimeUtil.toEpochMillis(QuestUtils.getNewDayMidnight());
                user.setNewDailyQuestsDate(nextResetTime);
                break;
            case WEEKLY:
                // 下周一00:00
                java.time.LocalDate today = TimeUtil.getCurrentDate();
                java.time.DayOfWeek currentDay = today.getDayOfWeek();
                int daysUntilNextMonday = (8 - currentDay.getValue()) % 7;
                if (daysUntilNextMonday == 0) {
                    daysUntilNextMonday = 7; // 如果今天是周一，则下次是下周一
                }
                java.time.LocalDateTime nextMonday = java.time.LocalDateTime.of(
                    today.plusDays(daysUntilNextMonday), 
                    java.time.LocalTime.MIDNIGHT
                );
                nextResetTime = TimeUtil.toEpochMillis(nextMonday);
                user.setNewWeeklyQuestsDate(nextResetTime);
                break;
            case MONTHLY:
                // 下个月1日00:00
                java.time.LocalDate currentDate = TimeUtil.getCurrentDate();
                java.time.LocalDate nextMonth = currentDate.plusMonths(1).withDayOfMonth(1);
                java.time.LocalDateTime nextMonthStart = java.time.LocalDateTime.of(
                    nextMonth, 
                    java.time.LocalTime.MIDNIGHT
                );
                nextResetTime = TimeUtil.toEpochMillis(nextMonthStart);
                user.setNewMonthlyQuestsDate(nextResetTime);
                break;
            case SEASONAL:
                // 赛季任务不自动重置，不需要更新时间
                break;
            default:
                break;
        }
    }
    
    private int getMaxQuestsForPeriod(@NotNull Player player, @NotNull su.nightexpress.quests.quest.definition.QuestPeriod period) {
        switch (period) {
            case DAILY:
                return Config.QUESTS_DAILY_AMOUNT_PER_RANK.get().getGreatest(player).intValue();
            case WEEKLY:
                return Config.QUESTS_WEEKLY_AMOUNT_PER_RANK.get().getGreatest(player).intValue();
            case MONTHLY:
                return Config.QUESTS_MONTHLY_AMOUNT_PER_RANK.get().getGreatest(player).intValue();
            case SEASONAL:
                return Config.QUESTS_SEASONAL_AMOUNT_PER_RANK.get().getGreatest(player).intValue();
            default:
                return Config.QUESTS_AMOUT_PER_RANK.get().getGreatest(player).intValue();
        }
    }
    
    @Nullable
    private QuestData createQuestDataForPlayer(@NotNull Quest quest, @NotNull QuestUser user) {
        // 只处理独立任务，全局任务由刷新机制统一创建
        if (quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT) {
            // 添加详细的调试信息
            su.nightexpress.nightcore.util.wrapper.UniInt amount = quest.getObjectivesAmount();
            this.plugin.debug("[创建任务数据] 任务: " + quest.getId());
            this.plugin.debug("[创建任务数据]   - UniInt对象: " + amount);
            this.plugin.debug("[创建任务数据]   - 目标表大小: " + quest.getObjectiveTable().getEntryMap().size());
            
            // 尝试多次 roll 看看是否总是返回 0
            int roll1 = amount.roll();
            int roll2 = amount.roll();
            int roll3 = amount.roll();
            this.plugin.debug("[创建任务数据]   - Roll测试: " + roll1 + ", " + roll2 + ", " + roll3);
            
            // 打印目标表内容
            this.plugin.debug("[创建任务数据]   - 目标表内容:");
            quest.getObjectiveTable().getEntryMap().forEach((name, objective) -> {
                this.plugin.debug("[创建任务数据]     * " + name + 
                    " - 权重: " + objective.weight() + 
                    ", 单位价值: " + objective.unitWorth());
            });
            
            QuestData data = quest.createQuestData();
            if (data == null) {
                this.plugin.warn("[创建任务数据] 失败 - 任务: " + quest.getId() + " 无法生成有效目标");
                this.plugin.warn("[创建任务数据]   可能原因：");
                this.plugin.warn("[创建任务数据]   1. 目标的 Amount 配置错误（需要 Min/Max 格式）");
                this.plugin.warn("[创建任务数据]   2. rollAmount() 返回了 0 或负数");
                this.plugin.warn("[创建任务数据]   3. 所有目标都被跳过了");
            } else {
                this.plugin.debug("[创建任务数据] 成功 - 任务: " + quest.getId() + 
                    ", 生成目标数: " + data.getObjectiveCounterMap().size());
            }
            return data;
        }
        return null;
    }
    
    

    public <O, A extends AdapterFamily<O>> void progressQuests(@NotNull Player player, @NotNull TaskType<O, A> taskType, @NotNull String fullName, int amount) {
        if (!this.isQuestsAvailable()) {
            return;
        }

        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        boolean needSave = false;
        
        // 处理独立任务：从玩家数据遍历
        for (QuestData questData : user.getQuestDatas()) {
            // 快速过滤：跳过全局任务引用、非激活任务
            // 独立任务不检查过期（由周期控制）
            if (questData.isGlobalQuest() || !questData.isActive()) {
                continue;
            }

            Quest quest = this.getQuestById(questData.getQuestId());
            if (quest == null || quest.getType() != taskType) {
                continue;
            }
            
            this.progressIndependentQuest(player, user, questData, quest, fullName, amount);
            needSave = true;
        }
        
        // 处理全局任务：从 GlobalQuestManager 遍历
        Collection<GlobalQuestData> allGlobalQuests = this.globalQuestManager.getAllGlobalQuests();
        
        for (GlobalQuestData globalData : allGlobalQuests) {
            if (globalData.isExpired()) {
                continue;
            }
            
            Quest quest = this.getQuestById(globalData.getQuestId());
            if (quest == null || quest.getType() != taskType) {
                continue;
            }
            
            this.progressGlobalQuest(player, user, globalData, quest, fullName, amount);
            needSave = true;
        }
        
        // 修复：任务进度更新后立即标记需要保存，确保数据不会丢失
        if (needSave) {
            this.plugin.getUserManager().save(user);
        }
    }
    
    /**
     * 查找通配符键（用于 QuestData）
     * 如果任务定义中使用了通配符，返回通配符键
     */
    @NotNull
    private String findWildcardKey(@NotNull QuestData questData, @NotNull String fullName) {
        // 尝试通配符 "any" (最常用的通配符)
        if (questData.hasObjective("any")) {
            return "any";
        }
        // 尝试通配符 "all"
        if (questData.hasObjective("all")) {
            return "all";
        }
        // 尝试通配符 "*"
        if (questData.hasObjective("*")) {
            return "*";
        }
        // 尝试通配符 "minecraft:*"
        if (questData.hasObjective("minecraft:*")) {
            return "minecraft:*";
        }
        // 尝试通配符 "minecraft:all"
        if (questData.hasObjective("minecraft:all")) {
            return "minecraft:all";
        }
        // 尝试通配符 "vanilla_string:*"
        if (questData.hasObjective("vanilla_string:*")) {
            return "vanilla_string:*";
        }
        // 尝试通配符 "vanilla_string:all"
        if (questData.hasObjective("vanilla_string:all")) {
            return "vanilla_string:all";
        }
        // 没有通配符，返回原始键
        return fullName;
    }
    
    /**
     * 查找通配符键（用于 GlobalQuestData）
     * 如果任务定义中使用了通配符，返回通配符键
     */
    @NotNull
    private String findWildcardKeyInGlobalData(@NotNull su.nightexpress.quests.quest.data.GlobalQuestData globalData, @NotNull String fullName) {
        // 尝试通配符 "any" (最常用的通配符)
        if (globalData.getRequired("any") > 0) {
            return "any";
        }
        // 尝试通配符 "all"
        if (globalData.getRequired("all") > 0) {
            return "all";
        }
        // 尝试通配符 "*"
        if (globalData.getRequired("*") > 0) {
            return "*";
        }
        // 尝试通配符 "minecraft:*"
        if (globalData.getRequired("minecraft:*") > 0) {
            return "minecraft:*";
        }
        // 尝试通配符 "minecraft:all"
        if (globalData.getRequired("minecraft:all") > 0) {
            return "minecraft:all";
        }
        // 尝试通配符 "vanilla_string:*"
        if (globalData.getRequired("vanilla_string:*") > 0) {
            return "vanilla_string:*";
        }
        // 尝试通配符 "vanilla_string:all"
        if (globalData.getRequired("vanilla_string:all") > 0) {
            return "vanilla_string:all";
        }
        // 没有通配符，返回原始键
        return fullName;
    }
    
    /**
     * 查找匹配的目标键（支持精确匹配、标签匹配、通配符匹配）
     * 
     * @param objectiveKeys 任务中定义的所有目标键
     * @param fullName 要匹配的完整名称（如 minecraft:stone）
     * @param taskType 任务类型（用于判断匹配方式）
     * @return 匹配的目标键，如果没有匹配则返回原始名称
     */
    @NotNull
    private String findMatchingObjectiveKey(@NotNull Set<String> objectiveKeys, @NotNull String fullName, @NotNull TaskType<?, ?> taskType) {
        this.plugin.debug("findMatchingObjectiveKey:");
        this.plugin.debug("  fullName: " + fullName);
        this.plugin.debug("  objectiveKeys: " + objectiveKeys);
        
        // 1. 精确匹配（带命名空间）
        if (objectiveKeys.contains(fullName)) {
            this.plugin.debug("  -> 精确匹配: " + fullName);
            return fullName;
        }
        
        // 2. 精确匹配（不带命名空间）
        if (fullName.contains(":")) {
            String valueOnly = fullName.substring(fullName.indexOf(':') + 1);
            if (objectiveKeys.contains(valueOnly)) {
                this.plugin.debug("  -> 不带命名空间匹配: " + valueOnly);
                return valueOnly;
            }
        }
        
        // 3. 标签匹配 - 检查目标中是否有标签定义
        for (String objKey : objectiveKeys) {
            if (TagMatcher.isTag(objKey)) {
                // 根据任务类型选择匹配方法
                boolean matches = matchesObjectiveWithTag(fullName, objKey, taskType);
                if (matches) {
                    this.plugin.debug("  -> 标签匹配: " + objKey + " 匹配 " + fullName);
                    return objKey;
                }
            }
        }
        
        // 4. 通配符匹配
        for (String wildcardKey : Arrays.asList("any", "all", "*", "minecraft:*", "minecraft:all", "vanilla_string:*", "vanilla_string:all")) {
            if (objectiveKeys.contains(wildcardKey)) {
                this.plugin.debug("  -> 通配符匹配: " + wildcardKey);
                return wildcardKey;
            }
        }
        
        this.plugin.debug("  -> 无匹配，返回原始值: " + fullName);
        return fullName;
    }
    
    /**
     * 根据任务类型判断目标是否与标签匹配
     * 
     * @param fullName 完整的目标名称（如 minecraft:stone）
     * @param tagKey 标签键（如 #minecraft:logs）
     * @param taskType 任务类型
     * @return true 如果匹配
     */
    private boolean matchesObjectiveWithTag(@NotNull String fullName, @NotNull String tagKey, @NotNull TaskType<?, ?> taskType) {
        // 根据适配器家族类型选择匹配方式
        AdapterFamily<?> family = taskType.getAdapterFamily();
        
        // 方块/物品相关任务 - 使用材料匹配
        if (family == AdapterFamily.BLOCK || family == AdapterFamily.BLOCK_STATE || family == AdapterFamily.ITEM) {
            org.bukkit.Material material = parseMaterial(fullName);
            if (material != null) {
                return TagMatcher.matchesMaterial(tagKey, material);
            }
        }
        
        // 实体相关任务 - 使用实体类型匹配
        if (family == AdapterFamily.ENTITY) {
            org.bukkit.entity.EntityType entityType = parseEntityType(fullName);
            if (entityType != null) {
                // 创建一个临时实体进行匹配（这里简化处理，直接比较类型）
                // 实际上 TagMatcher 需要 Entity 对象，但我们只有 EntityType
                // 所以这里需要特殊处理
                return matchesEntityTag(tagKey, entityType);
            }
        }
        
        return false;
    }
    
    /**
     * 从字符串解析 Material
     */
    @Nullable
    private org.bukkit.Material parseMaterial(@NotNull String fullName) {
        try {
            String materialName;
            if (fullName.contains(":")) {
                String[] parts = fullName.split(":", 2);
                materialName = parts[1];
            } else {
                materialName = fullName;
            }
            
            // 先尝试精确匹配（大写）
            org.bukkit.Material material = org.bukkit.Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                // 再尝试模糊匹配
                material = org.bukkit.Material.matchMaterial(materialName);
            }
            
            if (material == null) {
                this.plugin.debug("无法解析材料: " + fullName);
            }
            
            return material;
        } catch (Exception e) {
            this.plugin.debug("解析材料时出错: " + fullName + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从字符串解析 EntityType
     */
    @Nullable
    private org.bukkit.entity.EntityType parseEntityType(@NotNull String fullName) {
        try {
            String entityName;
            if (fullName.contains(":")) {
                String[] parts = fullName.split(":", 2);
                entityName = parts[1];
            } else {
                entityName = fullName;
            }
            
            // EntityType.valueOf 需要完全匹配大写名称
            org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(entityName.toUpperCase());
            return entityType;
        } catch (IllegalArgumentException e) {
            this.plugin.debug("无法解析实体类型: " + fullName);
            return null;
        } catch (Exception e) {
            this.plugin.debug("解析实体类型时出错: " + fullName + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查实体类型是否匹配标签
     * 注意：TagMatcher.matchesEntity 需要 Entity 对象，这里我们只有 EntityType
     * 所以需要特殊处理或扩展 TagMatcher
     */
    private boolean matchesEntityTag(@NotNull String tagKey, @NotNull org.bukkit.entity.EntityType entityType) {
        // 移除标签前缀
        String tagName = TagMatcher.removeTagPrefix(tagKey);
        
        try {
            // 尝试获取实体标签
            org.bukkit.NamespacedKey key;
            if (tagName.contains(":")) {
                String[] parts = tagName.split(":", 2);
                key = new org.bukkit.NamespacedKey(parts[0], parts[1]);
            } else {
                key = org.bukkit.NamespacedKey.minecraft(tagName);
            }
            
            // 使用反射获取 Bukkit 的实体类型标签
            for (java.lang.reflect.Field field : org.bukkit.Tag.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                    
                    Object value = field.get(null);
                    if (value instanceof org.bukkit.Tag) {
                        org.bukkit.Tag<?> tag = (org.bukkit.Tag<?>) value;
                        if (value instanceof org.bukkit.Keyed) {
                            org.bukkit.Keyed keyedTag = (org.bukkit.Keyed) value;
                            if (keyedTag.getKey().equals(key)) {
                                // 检查是否是实体标签
                                try {
                                    @SuppressWarnings("unchecked")
                                    org.bukkit.Tag<org.bukkit.entity.EntityType> entityTag = 
                                        (org.bukkit.Tag<org.bukkit.entity.EntityType>) tag;
                                    return entityTag.isTagged(entityType);
                                } catch (ClassCastException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.plugin.debug("标签匹配失败: " + e.getMessage());
        }
        
        return false;
    }
    
    private void progressIndependentQuest(@NotNull Player player, @NotNull QuestUser user, 
                                         @NotNull QuestData questData, @NotNull Quest quest,
                                         @NotNull String fullName, int amount) {
        if (questData.isCompleted()) {
            return;
        }
        
        // 查找匹配的目标键（支持精确匹配、标签匹配、通配符匹配）
        String actualKey = this.findMatchingObjectiveKey(questData.getObjectiveCounterMap().keySet(), fullName, quest.getType());
        int required = questData.getRequired(actualKey);
        
        if (required <= 0) {
            return;
        }

        int currentProgress = questData.getCurrent(actualKey);
        int count = Math.min(required, amount);
        questData.addCompleted(actualKey, count);

        if (questData.isCompleted()) {
            this.completeQuest(player, questData, quest);
        }
    }
    
    private void progressGlobalQuest(@NotNull Player player, @NotNull QuestUser user,
                                    @NotNull GlobalQuestData globalData, @NotNull Quest quest,
                                    @NotNull String fullName, int amount) {
        UUID globalQuestId = globalData.getId();
        
        // 检查竞争任务是否还有名额
        if (quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE) {
            if (this.globalQuestManager.hasPlayerCompleted(globalQuestId, player.getUniqueId())) {
                return;
            }
            
            if (!this.globalQuestManager.canPlayerCompleteCompetitive(globalQuestId, player.getUniqueId())) {
                Lang.QUESTS_COMPETITIVE_NO_SLOTS.message().send(player, replacer -> replacer.replace(quest.replacePlaceholders()));
                return;
            }
        }
        
        // 查找匹配的目标键（支持精确匹配、标签匹配、通配符匹配）
        String actualKey = this.findMatchingObjectiveKey(globalData.getObjectiveCounterMap().keySet(), fullName, quest.getType());
        int required = globalData.getRequired(actualKey);
        
        // 保留原有的兼容逻辑作为后备
        if (required <= 0 && fullName.contains(":")) {
            String valueOnly = fullName.substring(fullName.indexOf(':') + 1);
            required = globalData.getRequired(valueOnly);
            if (required > 0) {
                actualKey = valueOnly;
            }
        }
        
        // 如果还是没有匹配，尝试通配符
        if (required <= 0) {
            actualKey = this.findWildcardKeyInGlobalData(globalData, fullName);
            required = globalData.getRequired(actualKey);
        }
        
        if (required <= 0) {
            return;
        }

        int currentProgress = globalData.getCurrent(actualKey);
        int count = Math.min(required - currentProgress, amount);
        
        // 更新全局进度
        this.globalQuestManager.addProgress(globalQuestId, actualKey, count);
        
        // 更新玩家贡献度（用于合作任务和竞争任务排名）
        if (quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE ||
            quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE) {
            globalData.addPlayerContribution(player.getUniqueId(), count);
        }
        
        // 检查全局任务是否完成
        if (globalData.isCompleted()) {
            if (quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE) {
                // 合作任务完成：通知所有在线玩家并发奖
                Players.getOnline().forEach(onlinePlayer -> {
                    this.completeGlobalQuest(onlinePlayer, globalData, quest);
                });
            } else {
                // 竞争任务完成：只奖励当前玩家
                this.globalQuestManager.markPlayerCompleted(globalQuestId, player.getUniqueId());
                this.completeGlobalQuest(player, globalData, quest);
            }
        }
    }
    
    /**
     * 完成全局任务（合作或竞争）
     */
    private void completeGlobalQuest(@NotNull Player player, @NotNull GlobalQuestData globalData, @NotNull Quest quest) {
        // 执行任务基础奖励
        List<String> rewardCommands = quest.getRewards();
        if (!rewardCommands.isEmpty()) {
            for (String command : rewardCommands) {
                String cmd = command
                    .replace("%player%", player.getName())
                    .replace("%player_name%", player.getName())
                    .replace("%player_uuid%", player.getUniqueId().toString());
                this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), cmd);
            }
        }
        
        // 发放排名奖励
        if (!quest.getRankingRewards().isEmpty() || !quest.getRankingBattlePassXP().isEmpty()) {
            UUID globalQuestId = globalData.getId();
            int playerRank = globalData.getPlayerRank(player.getUniqueId());
            
            if (playerRank > 0) {
                // 发放排名命令奖励
                List<String> rankingCommands = quest.getRankingRewardForRank(playerRank);
                if (rankingCommands != null && !rankingCommands.isEmpty()) {
                    for (String command : rankingCommands) {
                        String cmd = command
                            .replace("%player%", player.getName())
                            .replace("%player_name%", player.getName())
                            .replace("%player_uuid%", player.getUniqueId().toString())
                            .replace("%rank%", String.valueOf(playerRank));
                        this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), cmd);
                    }
                    this.plugin.info("已为玩家 " + player.getName() + " 发放排名 " + playerRank + " 的奖励");
                }
                
                // 发放排名通行证经验
                int rankXP = quest.getRankingBattlePassXPForRank(playerRank);
                if (rankXP > 0 && this.plugin.getBattlePassManager() != null) {
                    this.plugin.getBattlePassManager().addXP(player, rankXP);
                    this.plugin.info("已为玩家 " + player.getName() + " 发放排名 " + playerRank + " 的通行证经验: " + rankXP);
                }
            }
        }
        
        // 发放通行证经验
        QuestXPReward xpReward = quest.getBattlePassXPReward();
        int unitsWorth = globalData.countUnitsWorth();
        int xpAmount = xpReward != null ? xpReward.getXP(unitsWorth) : 0;
        
        if (xpAmount > 0 && this.plugin.getBattlePassManager() != null) {
            this.plugin.getBattlePassManager().addXP(player, xpAmount);
        }
        
        // 获取奖励描述
        List<String> rewardLore = quest.getRewardLore();
        String rewardsDisplay = rewardLore.isEmpty() ? "无奖励" : String.join(", ", rewardLore);
        boolean hasRewards = !rewardCommands.isEmpty();
        
        // 通知玩家
        int finalXpAmount = xpAmount;
        String finalRewardsDisplay = rewardsDisplay;
        (hasRewards ? Lang.QUESTS_QUEST_COMPLETED_XP_REWARDS : Lang.QUESTS_QUEST_COMPLETED_XP_ONLY).message().send(player, replacer -> replacer
            .replace(quest.replacePlaceholders())
            .replace(QuestsPlaceholders.GENERIC_XP, NumberUtil.format(finalXpAmount))
            .replace(QuestsPlaceholders.GENERIC_REWARDS, finalRewardsDisplay)
        );
    }
    
    private void completeQuest(@NotNull Player player, @NotNull QuestData questData, @NotNull Quest quest) {
        double scale = questData.getScale();
        int units = questData.countUnitsWorth();
        int xpReward = questData.getXPReward();
        
        // 执行任务基础奖励（直接执行命令）
        List<String> rewardCommands = quest.getRewards();
        boolean hasRewards = !rewardCommands.isEmpty();
        if (!rewardCommands.isEmpty()) {
            for (String command : rewardCommands) {
                String cmd = command
                    .replace("%player%", player.getName())
                    .replace("%player_name%", player.getName())
                    .replace("%player_uuid%", player.getUniqueId().toString());
                this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), cmd);
            }
        }
        
        // 排名奖励已在 completeGlobalQuest() 中处理，这里不再需要
        int rankingBattlePassXP = 0;
        
        // 获取奖励描述用于显示
        List<String> rewardLore = quest.getRewardLore();
        String rewardsDisplay = rewardLore.isEmpty() ? "无奖励" : String.join(", ", rewardLore);

        int finalXpReward = xpReward;
        int finalRankingBattlePassXP = rankingBattlePassXP;
        String finalRewardsDisplay = rewardsDisplay;
        (hasRewards ? Lang.QUESTS_QUEST_COMPLETED_XP_REWARDS : Lang.QUESTS_QUEST_COMPLETED_XP_ONLY).message().send(player, replacer -> replacer
            .replace(quest.replacePlaceholders())
            .replace(QuestsPlaceholders.GENERIC_XP, NumberUtil.format(finalXpReward + finalRankingBattlePassXP))
            .replace(QuestsPlaceholders.GENERIC_REWARDS, finalRewardsDisplay)
        );

        // 发放通行证经验（基础 + 排名奖励）
        int totalBattlePassXP = xpReward + rankingBattlePassXP;
        this.plugin.battlePassManager().ifPresent(bp -> bp.addXP(player, totalBattlePassXP));
    }
    
    /**
     * 强制完成指定玩家的指定任务（用于测试）
     * @param player 玩家
     * @param questDataId 任务数据ID
     * @return 是否成功
     */
    public boolean forceCompleteQuest(@NotNull Player player, @NotNull String questDataId) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        if (user == null) {
            return false;
        }
        
        try {
            UUID questDataUUID = UUID.fromString(questDataId);
            
            // 首先尝试查找独立任务
            QuestData questData = user.getQuestDatas().stream()
                .filter(data -> data.getId().equals(questDataUUID))
                .findFirst()
                .orElse(null);
            
            if (questData != null) {
                // 找到了独立任务
                String questId = questData.getQuestId();
                Quest quest = this.getQuestById(questId);
                
                if (quest == null) {
                    this.plugin.warn("强制完成任务失败：未找到任务配置 " + questId);
                    return false;
                }
                
                // 手动将所有目标设置为完成
                questData.getObjectiveCounterMap().forEach((key, counter) -> {
                    int remaining = counter.getRequired() - counter.getCompleted();
                    if (remaining > 0) {
                        counter.addCompleted(remaining);
                    }
                });
                
                // 调用完成逻辑
                this.completeQuest(player, questData, quest);
                
                // 从玩家数据中移除
                user.getQuestDatas().remove(questData);
                this.plugin.getUserManager().save(user);
                
                this.plugin.info("已强制完成玩家 " + player.getName() + " 的独立任务 " + questId + " (ID: " + questDataId + ")");
                return true;
            }
            
            // 尝试查找全局任务（合作或竞争）
            GlobalQuestData globalData = this.globalQuestManager.getGlobalQuestData(questDataUUID);
            
            if (globalData != null) {
                String questId = globalData.getQuestId();
                Quest quest = this.getQuestById(questId);
                
                if (quest == null) {
                    this.plugin.warn("强制完成任务失败：未找到任务配置 " + questId);
                    return false;
                }
                
                // 手动将所有目标设置为完成
                globalData.getObjectiveCounterMap().forEach((key, counter) -> {
                    int remaining = counter.getRequired() - counter.getCompleted();
                    if (remaining > 0) {
                        counter.addCompleted(remaining);
                    }
                });
                
                // 发放奖励
                if (quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE) {
                    // 竞争任务：只奖励指定玩家
                    this.globalQuestManager.markPlayerCompleted(questDataUUID, player.getUniqueId());
                    this.completeGlobalQuest(player, globalData, quest);
                } else {
                    // 合作任务：奖励所有在线玩家
                    this.completeGlobalQuest(player, globalData, quest);
                }
                
                this.plugin.info("已强制完成全局任务 " + questId + " (ID: " + questDataId + ")");
                return true;
            }
            
            // 没有找到任何任务
            this.plugin.warn("强制完成任务失败：未找到任务数据 " + questDataId);
            return false;
            
        } catch (IllegalArgumentException e) {
            this.plugin.warn("强制完成任务失败：无效的UUID格式 " + questDataId);
            return false;
        }
    }
    
    @Nullable
    public Quest getQuestById(@NotNull String id) {
        return this.questById.get(id);
    }
    
    @NotNull
    public Collection<Quest> getAllQuests() {
        return new ArrayList<>(this.questById.values());
    }
    
    @NotNull
    public GlobalQuestManager getGlobalQuestManager() {
        return this.globalQuestManager;
    }
    
    @Nullable
    public QuestRefreshManager getQuestRefreshManager() {
        return this.questRefreshManager;
    }
    
    @NotNull
    public LoreTemplateManager getLoreTemplateManager() {
        return this.loreTemplateManager;
    }
    
    @NotNull
    public su.nightexpress.quests.util.QuestItemBuilder getQuestItemBuilder() {
        return this.questItemBuilder;
    }
    
    /**
     * 删除玩家的独立任务
     * @param player 玩家
     * @param questDataId 任务数据ID
     * @return 是否成功删除
     */
    public boolean deleteIndependentQuest(@NotNull Player player, @NotNull String questDataId) {
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        UUID uuid;
        try {
            uuid = UUID.fromString(questDataId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        
        boolean removed = user.getQuestDatas().removeIf(questData -> {
            if (!questData.getId().equals(uuid)) return false;
            
            Quest quest = this.getQuestById(questData.getQuestId());
            return quest != null && quest.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.INDEPENDENT;
        });
        
        if (removed) {
            this.plugin.getUserManager().save(user);
            
            // 刷新界面
            if (this.unifiedIndependentQuestsMenu.isViewer(player)) {
                this.plugin.runTask(() -> this.unifiedIndependentQuestsMenu.flush(player));
            }
        }
        
        return removed;
    }
    
    /**
     * 删除全服的合作任务
     * @param globalQuestId 全局任务ID（UUID字符串）
     * @return 是否成功删除
     */
    public boolean deleteCooperativeQuestGlobal(@NotNull String globalQuestId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(globalQuestId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        
        // 删除全局任务数据
        boolean removed = this.globalQuestManager.removeGlobalQuestData(uuid);
        
        if (!removed) {
            return false;
        }
        
        // 通知所有在线玩家并刷新界面
        Players.getOnline().forEach(player -> {
            // 刷新界面
            if (this.cooperativeQuestsMenu.isViewer(player)) {
                this.plugin.runTask(() -> this.cooperativeQuestsMenu.flush(player));
            }
            
            // 发送通知
            Lang.QUESTS_DELETED_COOPERATIVE_NOTIFY.message().send(player);
        });
        
        // 保存全局任务数据
        this.saveGlobalQuests();
        
        return true;
    }
    
    /**
     * 删除全服的竞争任务
     * @param globalQuestId 全局任务ID（UUID字符串）
     * @return 是否成功删除
     */
    public boolean deleteCompetitiveQuestGlobal(@NotNull String globalQuestId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(globalQuestId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        
        // 删除全局任务数据
        boolean removed = this.globalQuestManager.removeGlobalQuestData(uuid);
        
        if (!removed) {
            return false;
        }
        
        // 通知所有在线玩家并刷新界面
        Players.getOnline().forEach(player -> {
            // 刷新界面
            if (this.competitiveQuestsMenu.isViewer(player)) {
                this.plugin.runTask(() -> this.competitiveQuestsMenu.flush(player));
            }
            
            // 发送通知
            Lang.QUESTS_DELETED_COMPETITIVE_NOTIFY.message().send(player);
        });
        
        // 保存全局任务数据
        this.saveGlobalQuests();
        
        return true;
    }
}
