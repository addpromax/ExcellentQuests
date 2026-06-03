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
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.GlobalQuestManager;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.quest.definition.QuestType;

import java.util.*;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class CooperativeQuestsMenu extends NormalMenu<QuestsPlugin> implements ConfigBased {

    private final QuestManager manager;
    private final TreeMap<Integer, int[]> slotsByQuestCount;

    public CooperativeQuestsMenu(@NotNull QuestsPlugin plugin, @NotNull QuestManager manager) {
        super(plugin, MenuType.GENERIC_9X5, BLACK.wrap("合作任务"));
        this.manager = manager;
        this.slotsByQuestCount = new TreeMap<>();
        this.setAutoRefreshInterval(1);
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        GlobalQuestManager globalManager = this.manager.getGlobalQuestManager();
        su.nightexpress.quests.util.QuestItemBuilder itemBuilder = this.manager.getQuestItemBuilder();
        
        // 直接从全局任务管理器获取合作任务
        List<GlobalQuestData> globalQuestDatas = globalManager.getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == QuestType.COOPERATIVE)
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
        // 合作任务不需要刷新时间显示，每个任务已经在lore中显示剩余时间
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
        // 配置文件位置: /menu/cooperative_quests.yml
        
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

}

