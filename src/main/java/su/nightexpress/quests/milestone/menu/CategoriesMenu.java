package su.nightexpress.quests.milestone.menu;

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
import su.nightexpress.nightcore.ui.menu.type.NormalMenu;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.milestone.MilestoneManager;
import su.nightexpress.quests.milestone.definition.Milestone;
import su.nightexpress.quests.milestone.definition.MilestoneCategory;
import su.nightexpress.quests.user.QuestUser;

import java.util.*;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.BLACK;
import static su.nightexpress.quests.QuestsPlaceholders.*;

public class CategoriesMenu extends NormalMenu<QuestsPlugin> implements ConfigBased {

    private final MilestoneManager manager;

    private final TreeMap<Integer, int[]> slotsByCategoryCount;

    public CategoriesMenu(@NotNull QuestsPlugin plugin, @NotNull MilestoneManager manager) {
        super(plugin, MenuType.GENERIC_9X5, BLACK.wrap("Milestone • Categories"));
        this.manager = manager;
        this.slotsByCategoryCount = new TreeMap<>();
        this.setApplyPlaceholderAPI(true);
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        List<MilestoneCategory> categories = this.manager.getCategories().stream().sorted(Comparator.comparing(category -> NightMessage.stripTags(category.getName()))).toList();

        int categoryCount = categories.size();
        int[] categorySlots = Optional.ofNullable(this.slotsByCategoryCount.ceilingEntry(categoryCount)).map(Map.Entry::getValue).orElse(new int[0]);

        // 确保不会越界：使用实际分类数量和槽位数量的最小值
        int itemsToDisplay = Math.min(categorySlots.length, categories.size());
        
        for (int index = 0; index < itemsToDisplay; index++) {
            int slot = categorySlots[index];
            MilestoneCategory category = categories.get(index);
            Set<Milestone> milestones = this.manager.getMilestonesByCategory(category);

            viewer.addItem(category.getIcon()
                .hideAllComponents()
                .localized(Lang.UI_MILESTONES_CATEGORY_INFO)
                .replacement(replacer -> replacer
                    .replace(category.replacePlaceholders())
                    .replace(GENERIC_TOTAL, () -> String.valueOf(milestones.size()))
                    .replace(GENERIC_COMPLETED, () -> String.valueOf(milestones.stream().filter(user::isCompleted).count()))
                    .replace(GENERIC_LEVELS, () -> String.valueOf(milestones.stream().mapToInt(user::getMilestoneCompletedLevels).sum()))
                )
                .toMenuItem()
                .setPriority(Integer.MAX_VALUE)
                .setSlots(slot)
                .setHandler((viewer1, event) -> this.runNextTick(() -> this.manager.openMilestones(viewer1.getPlayer(), category)))
                .build()
            );
        }
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        // 支持最多 30 个分类
        for (int count = 0; count < 30; count++) {
            int amount = count + 1;
            int[] defSlots = getDefaultSlots(amount);
            int[] skillSlots = ConfigValue.create("Category.SlotsByCount." + amount, defSlots).read(config);

            this.slotsByCategoryCount.put(amount, skillSlots);
        }

        loader.addDefaultItem(NightItem.fromType(Material.BLACK_STAINED_GLASS_PANE)
            .setHideTooltip(true)
            .toMenuItem()
            .setPriority(-1)
            .setSlots(0,1,2,3,4,5,6,7,8,36,37,38,39,40,41,42,43,44)
        );

        loader.addDefaultItem(NightItem.fromType(Material.GRAY_STAINED_GLASS_PANE)
            .setHideTooltip(true)
            .toMenuItem()
            .setPriority(-1)
            .setSlots(IntStream.range(9, 36).toArray())
        );
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
            case 11 -> new int[]{20, 21, 22, 23, 24, 28, 29, 30, 31, 32, 33};
            case 12 -> new int[]{19, 20, 21, 22, 23, 24, 28, 29, 30, 31, 32, 33};
            case 13 -> new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33};
            case 14 -> new int[]{19, 20, 21, 22, 23, 24, 25, 27, 28, 29, 30, 31, 32, 33};
            case 15 -> new int[]{19, 20, 21, 22, 23, 24, 25, 27, 28, 29, 30, 31, 32, 33, 34};
            case 16 -> new int[]{18, 19, 20, 21, 22, 23, 24, 25, 27, 28, 29, 30, 31, 32, 33, 34};
            case 17 -> new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34};
            case 18 -> new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
            // 19-27: 从槽位9开始填满整个中间3行
            case 19 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
            case 20 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 24, 25, 26, 27, 28, 29};
            case 21 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
            case 22 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
            case 23 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
            case 24 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
            case 25 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33};
            case 26 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34};
            case 27 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
            default -> new int[]{}; // 超过27个分类，返回空数组
        };
    }
}
