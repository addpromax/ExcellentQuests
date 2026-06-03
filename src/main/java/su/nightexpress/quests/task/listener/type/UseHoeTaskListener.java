package su.nightexpress.quests.task.listener.type;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 使用锄头任务监听器
 */
public class UseHoeTaskListener extends TaskListener<Block, AdapterFamily<Block>> {

    public UseHoeTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<Block, AdapterFamily<Block>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;

        ItemStack item = event.getItem();
        if (item == null || !item.getType().name().contains("HOE")) return;

        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        // 检查方块是否变成了耕地
        // 使用 NightCore 的调度器以支持 Folia
        this.plugin.runTaskLater(() -> {
            if (block.getType().name().contains("FARMLAND")) {
                this.progressQuests(player, block);
            }
        }, 1L);
    }
}

