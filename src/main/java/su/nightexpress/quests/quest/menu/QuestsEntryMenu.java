package su.nightexpress.quests.quest.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.ConfigBased;
import su.nightexpress.nightcore.ui.menu.data.MenuLoader;
import su.nightexpress.nightcore.ui.menu.type.NormalMenu;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.QuestManager;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * 任务入口菜单 - 完全配置驱动
 * 玩家可以选择打开：独立任务、合作任务或竞争任务
 */
public class QuestsEntryMenu extends NormalMenu<QuestsPlugin> implements ConfigBased {

    private final QuestManager manager;

    public QuestsEntryMenu(@NotNull QuestsPlugin plugin, @NotNull QuestManager manager) {
        super(plugin, MenuType.GENERIC_9X5, BLACK.wrap("任务中心"));
        this.manager = manager;
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        // 所有菜单项都在配置文件中定义
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        // 所有菜单项都通过配置文件定义
        // 配置文件位置: /menu/quests_entry.yml
        // 
        // 菜单项需要通过配置文件的 Items 部分定义
        // 点击处理器通过配置文件的 Click_Commands 定义
        // 
        // 示例配置:
        // Items:
        //   independent_quests:
        //     Priority: 100
        //     Slots: [10]
        //     Material: DIAMOND_SWORD
        //     Name: '&e&l独立任务'
        //     Lore:
        //       - '&7查看和完成个人任务获取奖励'
        //     Click_Commands:
        //       Left:
        //         - '[MENU] independent_quests'
    }
    
    public QuestManager getManager() {
        return manager;
    }
}

