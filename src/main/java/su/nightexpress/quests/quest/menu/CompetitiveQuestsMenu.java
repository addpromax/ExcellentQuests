package su.nightexpress.quests.quest.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.ConfigBased;
import su.nightexpress.nightcore.ui.menu.data.MenuLoader;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.NormalMenu;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.battlepass.BattlePassManager;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.quest.GlobalQuestManager;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestType;
import su.nightexpress.quests.quest.definition.QuestXPReward;
import su.nightexpress.quests.reward.Reward;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.user.QuestUser;
import su.nightexpress.quests.util.MenuUtils;

import java.util.*;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.quests.QuestsPlaceholders.*;

public class CompetitiveQuestsMenu extends NormalMenu<QuestsPlugin> implements ConfigBased {

    private final QuestManager manager;
    private final TreeMap<Integer, int[]> slotsByQuestCount;

    public CompetitiveQuestsMenu(@NotNull QuestsPlugin plugin, @NotNull QuestManager manager) {
        super(plugin, MenuType.GENERIC_9X5, BLACK.wrap("竞争任务"));
        this.manager = manager;
        this.slotsByQuestCount = new TreeMap<>();
        this.setAutoRefreshInterval(1);
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        GlobalQuestManager globalManager = this.manager.getGlobalQuestManager();
        su.nightexpress.quests.util.QuestItemBuilder itemBuilder = this.manager.getQuestItemBuilder();
        
        // 直接从全局任务管理器获取竞争任务
        List<GlobalQuestData> globalQuestDatas = globalManager.getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == QuestType.COMPETITIVE)
            .filter(data -> !data.isExpired())
            .sorted(Comparator.comparing(GlobalQuestData::getQuestId))
            .toList();

        int questCount = globalQuestDatas.size();
        int[] questSlots = Optional.ofNullable(this.slotsByQuestCount.ceilingEntry(questCount)).map(Map.Entry::getValue).orElse(new int[0]);

        int maxIndex = Math.min(questSlots.length, globalQuestDatas.size());
        for (int index = 0; index < maxIndex; index++) {
            int slot = questSlots[index];
            GlobalQuestData globalData = globalQuestDatas.get(index);

            Quest quest = this.manager.getQuestById(globalData.getQuestId());
            if (quest == null) {
                this.plugin.error("Invalid global quest: '" + globalData.getQuestId() + "'!");
                continue;
            }

            // 使用统一的ItemBuilder构建任务显示项
            NightItem icon = itemBuilder.buildGlobalQuestItem(quest, globalData, player);
            
            viewer.addItem(icon
                .toMenuItem()
                .setPriority(Integer.MAX_VALUE)
                .setSlots(slot)
                .build());
        }
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @Override
    protected void onItemPrepare(@NotNull MenuViewer viewer, @NotNull MenuItem menuItem, @NotNull NightItem item) {
        super.onItemPrepare(viewer, menuItem, item);
        // 竞争任务不需要刷新时间显示，每个任务已经在lore中显示剩余时间
    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        for (int count = 0; count < 10; count++) {
            int amount = count + 1;
            int[] defSlots = getDefaultSlots(amount);
            int[] skillSlots = ConfigValue.create("Quest.SlotsByCount." + amount, defSlots).read(config);

            this.slotsByQuestCount.put(amount, skillSlots);
        }

        // 所有装饰物品和其他菜单项都通过配置文件定义
        // 配置文件位置: /menu/competitive_quests.yml
        
        config.saveChanges();
    }

    private static int[] getDefaultSlots(int count) {
        return switch (count) {
            case 1 -> new int[]{22};
            case 2 -> new int[]{21, 23};
            case 3 -> new int[]{21, 22, 23};
            case 4 -> new int[]{21, 22, 24, 25};
            case 5 -> new int[]{20, 21, 22, 23, 24};
            case 6 -> new int[]{20, 21, 22, 23, 24, 31};
            case 7 -> new int[]{20, 21, 22, 23, 24, 30, 32};
            case 8 -> new int[]{20, 21, 22, 23, 24, 30, 31, 32};
            case 9 -> new int[]{20, 21, 22, 23, 24, 29, 30, 32, 33};
            case 10 -> new int[]{20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
            default -> new int[]{};
        };
    }

    @NotNull
    private List<String> formatGlobalObjectives(@NotNull Quest quest, @NotNull GlobalQuestData globalData) {
        List<String> list = new ArrayList<>();
        TaskType<?, ?> type = quest.getType();

        globalData.getObjectiveCounterMap().forEach((fullName, counter) -> {
            int required = counter.getRequired();
            int current = counter.getCompleted();

            String name = getObjectiveDisplayName(type, fullName);
            list.add(Lang.UI_MILESTONES_MILESTONE_OBJECTIVE.text()
                .replace(GENERIC_NAME, name)
                .replace(GENERIC_CURRENT, NumberUtil.format(current))
                .replace(GENERIC_REQUIRED, NumberUtil.format(required)));
        });

        return list;
    }

    @NotNull
    private String getObjectiveDisplayName(@NotNull TaskType<?, ?> type, @NotNull String fullName) {
        if (fullName.endsWith(":*") || fullName.endsWith(":all")) {
            return "任意" + type.getId();
        }
        
        su.nightexpress.quests.task.adapter.Adapter<?, ?> adapter = type.getAdapterFamily().getAdapterForName(fullName);
        if (adapter == null) {
            return fullName;
        }
        
        String localizedName = adapter.getLocalizedName(fullName);
        return localizedName != null ? localizedName : fullName;
    }
    
    /**
     * 格式化排名奖励显示
     */
    @NotNull
    private List<String> formatRankingRewards(@NotNull Quest quest) {
        List<String> result = new ArrayList<>();
        
        Map<Integer, List<String>> rankingRewards = quest.getRankingRewards();
        Map<Integer, Integer> rankingBattlePassXP = quest.getRankingBattlePassXP();
        
        // 获取所有排名（合并两个Map的键）
        Set<Integer> allRanks = new TreeSet<>();
        allRanks.addAll(rankingRewards.keySet());
        allRanks.addAll(rankingBattlePassXP.keySet());
        
        for (Integer rank : allRanks) {
            StringBuilder rewardText = new StringBuilder();
            rewardText.append("&6第").append(rank).append("名: ");
            
            List<String> rewards = new ArrayList<>();
            
            // 添加通行证经验奖励
            if (rankingBattlePassXP.containsKey(rank)) {
                int xp = rankingBattlePassXP.get(rank);
                rewards.add(xp + " 通行证经验");
            }
            
            // 添加命令奖励的描述（从命令中提取）
            if (rankingRewards.containsKey(rank)) {
                List<String> commands = rankingRewards.get(rank);
                for (String command : commands) {
                    // 尝试从命令中提取奖励描述
                    if (command.contains("addExp")) {
                        String[] parts = command.split("\\s+");
                        if (parts.length >= 2) {
                            rewards.add(parts[1] + " 经验");
                        }
                    }
                }
            }
            
            if (!rewards.isEmpty()) {
                rewardText.append(String.join(" + ", rewards));
                result.add(rewardText.toString());
            }
        }
        
        return result;
    }
    
    /**
     * 格式化排名奖励为字符串
     */
    @NotNull
    private String formatRankRewards(@NotNull Quest quest, int rank) {
        List<String> rewardLore = quest.getRankingRewardLoreForRank(rank);
        if (rewardLore == null || rewardLore.isEmpty()) {
            return "暂无";
        }
        return String.join(", ", rewardLore);
    }
    
    /**
     * 根据排名返回颜色代码
     */
    @NotNull
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "&6";  // 金色
            case 2 -> "&e";  // 黄色
            case 3 -> "&c";  // 红色
            default -> "&7"; // 灰色
        };
    }
    
    /**
     * 计算字符串中的数学表达式
     * 支持基本运算：+, -, *, /
     */
    @NotNull
    private String evaluateMathExpressions(@NotNull String text) {
        // 匹配数字运算表达式，如 "1*50", "10+20", "100/2"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*([+\\-*/])\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            try {
                int num1 = Integer.parseInt(matcher.group(1));
                String operator = matcher.group(2);
                int num2 = Integer.parseInt(matcher.group(3));
                
                int calculated = switch (operator) {
                    case "+" -> num1 + num2;
                    case "-" -> num1 - num2;
                    case "*" -> num1 * num2;
                    case "/" -> num2 != 0 ? num1 / num2 : num1;
                    default -> num1;
                };
                
                matcher.appendReplacement(result, String.valueOf(calculated));
            } catch (NumberFormatException e) {
                // 如果解析失败，保持原样
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}

