package su.nightexpress.quests.task.listener.type;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 播放音乐唱片任务监听器
 */
public class PlayMusicDiscTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public PlayMusicDiscTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.JUKEBOX) return;

        ItemStack item = event.getItem();
        if (item == null || !item.getType().name().contains("MUSIC_DISC")) return;

        Player player = event.getPlayer();
        if (!this.manager.canDoTasks(player)) return;

        this.progressQuests(player, item);
    }
}

