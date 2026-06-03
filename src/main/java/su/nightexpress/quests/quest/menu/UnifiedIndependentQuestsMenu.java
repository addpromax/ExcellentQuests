package su.nightexpress.quests.quest.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.ConfigBased;
import su.nightexpress.nightcore.ui.menu.data.MenuLoader;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.NormalMenu;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestType;
import su.nightexpress.quests.quest.definition.QuestPeriod;
import su.nightexpress.quests.user.QuestUser;

import java.util.*;
import java.util.stream.Collectors;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.quests.QuestsPlaceholders.*;

/**
 * 统一的个人任务菜单
 * 在一个菜单中显示每日、每周、每月任务
 */
public class UnifiedIndependentQuestsMenu extends NormalMenu<QuestsPlugin> implements ConfigBased {

    private final QuestManager manager;
    
    // 每种周期任务的显示槽位（根据任务数量）
    private int maxDailyQuests;
    private int maxWeeklyQuests;
    private int maxMonthlyQuests;
    
    // 固定槽位配置
    private int[] dailyQuestSlots;    // 第一行
    private int[] weeklyQuestSlots;   // 第二行
    private int[] monthlyQuestSlots;  // 第三行

    public UnifiedIndependentQuestsMenu(@NotNull QuestsPlugin plugin, @NotNull QuestManager manager) {
        super(plugin, MenuType.GENERIC_9X5, BLACK.wrap("个人任务"));
        this.manager = manager;
        this.setAutoRefreshInterval(1);
        
        // 默认值
        this.maxDailyQuests = 6;
        this.maxWeeklyQuests = 6;
        this.maxMonthlyQuests = 6;
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        su.nightexpress.quests.util.QuestItemBuilder itemBuilder = this.manager.getQuestItemBuilder();
        
        // 获取三种周期的任务(显示所有独立任务)
        Map<QuestPeriod, List<QuestData>> questsByPeriod = user.getQuestDatas().stream()
            .filter(questData -> {
                // 独立任务不会过期，由周期控制
                Quest quest = this.manager.getQuestById(questData.getQuestId());
                return quest != null && quest.getQuestType() == QuestType.INDEPENDENT;
            })
            .collect(Collectors.groupingBy(questData -> {
                Quest quest = this.manager.getQuestById(questData.getQuestId());
                return quest.getQuestPeriod();
            }));
        
        // 每日任务 - 第一行
        List<QuestData> dailyQuests = questsByPeriod.getOrDefault(QuestPeriod.DAILY, new ArrayList<>());
        dailyQuests.sort(Comparator.comparing(QuestData::getQuestId));
        addQuestItems(viewer, itemBuilder, dailyQuests, dailyQuestSlots);
        
        // 每周任务 - 第二行
        List<QuestData> weeklyQuests = questsByPeriod.getOrDefault(QuestPeriod.WEEKLY, new ArrayList<>());
        weeklyQuests.sort(Comparator.comparing(QuestData::getQuestId));
        addQuestItems(viewer, itemBuilder, weeklyQuests, weeklyQuestSlots);
        
        // 每月任务 - 第三行
        List<QuestData> monthlyQuests = questsByPeriod.getOrDefault(QuestPeriod.MONTHLY, new ArrayList<>());
        monthlyQuests.sort(Comparator.comparing(QuestData::getQuestId));
        addQuestItems(viewer, itemBuilder, monthlyQuests, monthlyQuestSlots);
    }
    
    private void addQuestItems(@NotNull MenuViewer viewer, 
                               @NotNull su.nightexpress.quests.util.QuestItemBuilder itemBuilder,
                               @NotNull List<QuestData> questDatas,
                               @NotNull int[] slots) {
        Player player = viewer.getPlayer();
        
        for (int index = 0; index < Math.min(questDatas.size(), slots.length); index++) {
            int slot = slots[index];
            QuestData questData = questDatas.get(index);

            Quest quest = this.manager.getQuestById(questData.getQuestId());
            if (quest == null) {
                this.plugin.error("Invalid quest data: '" + questData.getId() + "'!");
                continue;
            }

            // 使用统一的ItemBuilder构建任务显示项
            NightItem icon = itemBuilder.buildIndependentQuestItem(quest, questData, player);
            
            viewer.addItem(icon
                .toMenuItem()
                .setPriority(Integer.MAX_VALUE)
                .setSlots(slot)
                .setHandler((viewer1, event) -> {
                    if (questData.isActive()) return;

                    questData.setActive(true);
                    // 独立任务不设置过期时间，由周期控制（每日/每周/每月/赛季）

                    // 修复：激活任务后立即保存，确保激活状态不会丢失
                    QuestUser user = this.plugin.getUserManager().getOrFetch(player);
                    this.plugin.getUserManager().save(user);
                    this.plugin.debug("玩家 " + player.getName() + " 激活任务 " + quest.getId() + "，已保存数据");

                    this.runNextTick(() -> this.flush(viewer));
                    Lang.QUESTS_QUEST_ACCEPTED.message().send(player, replacer -> replacer.replace(quest.replacePlaceholders()));
                })
                .build());
        }
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @Override
    protected void onItemPrepare(@NotNull MenuViewer viewer, @NotNull MenuItem menuItem, @NotNull NightItem item) {
        super.onItemPrepare(viewer, menuItem, item);

        Player player = viewer.getPlayer();
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);

        // 为所有物品添加占位符替换，包括配置文件中的计时器
        item.replacement(replacer -> {
            // 如果时间未设置（为0），则使用PeriodUtil计算下次重置时间
            long dailyResetTime = user.getNewDailyQuestsDate();
            if (dailyResetTime <= 0) {
                dailyResetTime = su.nightexpress.quests.util.PeriodUtil.getNextResetTime(su.nightexpress.quests.quest.definition.QuestPeriod.DAILY);
            }
            
            long weeklyResetTime = user.getNewWeeklyQuestsDate();
            if (weeklyResetTime <= 0) {
                weeklyResetTime = su.nightexpress.quests.util.PeriodUtil.getNextResetTime(su.nightexpress.quests.quest.definition.QuestPeriod.WEEKLY);
            }
            
            long monthlyResetTime = user.getNewMonthlyQuestsDate();
            if (monthlyResetTime <= 0) {
                monthlyResetTime = su.nightexpress.quests.util.PeriodUtil.getNextResetTime(su.nightexpress.quests.quest.definition.QuestPeriod.MONTHLY);
            }
            
            long finalDailyResetTime = dailyResetTime;
            long finalWeeklyResetTime = weeklyResetTime;
            long finalMonthlyResetTime = monthlyResetTime;
            
            // 修复：正确的占位符格式应该是 %refresh_time_DAILY% 而不是 %refresh_time%_DAILY%
            replacer.replace("%refresh_time_DAILY%", () -> TimeFormats.formatDuration(finalDailyResetTime, TimeFormatType.LITERAL));
            replacer.replace("%refresh_time_WEEKLY%", () -> TimeFormats.formatDuration(finalWeeklyResetTime, TimeFormatType.LITERAL));
            replacer.replace("%refresh_time_MONTHLY%", () -> TimeFormats.formatDuration(finalMonthlyResetTime, TimeFormatType.LITERAL));
        });
    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        // 从配置读取任务上限
        this.maxDailyQuests = ConfigValue.create("Quest.MaxDailyQuests", 6,
            "每日任务显示的最大数量").read(config);
        this.maxWeeklyQuests = ConfigValue.create("Quest.MaxWeeklyQuests", 6,
            "每周任务显示的最大数量").read(config);
        this.maxMonthlyQuests = ConfigValue.create("Quest.MaxMonthlyQuests", 6,
            "每月任务显示的最大数量").read(config);
        
        // 配置槽位 - 第2行（每日任务）槽位10-15
        int[] defaultDailySlots = new int[]{10, 11, 12, 13, 14, 15};
        int[] rawDailySlots = ConfigValue.create("Quest.DailyQuestSlots", defaultDailySlots,
                "每日任务的显示槽位").read(config);
        this.dailyQuestSlots = Arrays.copyOf(rawDailySlots, Math.min(maxDailyQuests, 6));
        
        // 第3行（每周任务）槽位19-24
        int[] defaultWeeklySlots = new int[]{19, 20, 21, 22, 23, 24};
        this.weeklyQuestSlots = Arrays.copyOf(
            ConfigValue.create("Quest.WeeklyQuestSlots", defaultWeeklySlots,
                "每周任务的显示槽位").read(config),
            Math.min(maxWeeklyQuests, 6)
        );
        
        // 第4行（每月任务）槽位28-33
        int[] defaultMonthlySlots = new int[]{28, 29, 30, 31, 32, 33};
        this.monthlyQuestSlots = Arrays.copyOf(
            ConfigValue.create("Quest.MonthlyQuestSlots", defaultMonthlySlots,
                "每月任务的显示槽位").read(config),
            Math.min(maxMonthlyQuests, 6)
        );

        // 修复：加载配置文件中定义的所有菜单项（装饰物品、计时器等）
        loader.loadItems();
        
        config.saveChanges();
    }
}

