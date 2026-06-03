package su.nightexpress.quests.task;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.blocktracker.PlayerBlockTracker;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.task.adapter.Adapter;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskGlobalListener;
import su.nightexpress.quests.task.listener.type.*;
import su.nightexpress.quests.task.workstation.WorkstationMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class TaskManager extends AbstractManager<QuestsPlugin> {

    private static final Predicate<Block> BLOCK_FILTER = block -> true;
    private static final String PLAYER_BLOCK_MARKER = "player_block_marker";

    private final TaskTypeRegistry taskTypeRegistry;

    private final Set<SpawnReason> artificalMobSpawns;

    private final NamespacedKey stationOwnerKey;
    private final NamespacedKey stationModeKey;
    private final NamespacedKey mobSpawnerKey;

    public TaskManager(@NotNull QuestsPlugin plugin, @NotNull TaskTypeRegistry taskTypeRegistry) {
        super(plugin);
        this.taskTypeRegistry = taskTypeRegistry;
        this.artificalMobSpawns = new HashSet<>();
        this.stationOwnerKey = new NamespacedKey(plugin, "workstation.owner_id");
        this.stationModeKey = new NamespacedKey(plugin, "workstation.craft_mode");
        this.mobSpawnerKey = new NamespacedKey(plugin, "spawner_mob");
    }

    @Override
    protected void onLoad() {
        this.plugin.info("TaskManager 开始加载...");
        
        if (!Config.ANTI_ABUSE_COUNT_PLAYER_BLOCKS.get()) {
            PlayerBlockTracker.initialize();
            PlayerBlockTracker.BLOCK_FILTERS.add(BLOCK_FILTER);
        }

        if (!Config.ANTI_ABUSE_COUNT_ARTIFICAL_MOBS.get()) {
            this.artificalMobSpawns.addAll(Lists.modify(Config.ANTI_ABUSE_ARTIFICAL_MOB_SPAWNS.get(), str -> Enums.get(str, SpawnReason.class)));
        }

        this.registerTaskTypes();
        this.registerListeners();
        this.addListener(new TaskGlobalListener(this.plugin, this));
        
        this.plugin.info("TaskManager 加载完成");
    }

    @Override
    protected void onShutdown() {

    }

    private void registerListeners() {
        // 从注册表获取任务类型并注册监听器
        // 这样在 reload 时也能正确注册监听器（即使注册表已冻结）
        TaskType<?, ?> placeBlockType = this.taskTypeRegistry.getTypeById(TaskTypeId.PLACE_BLOCK);
        if (placeBlockType != null) {
            this.addListener(new BlockPlaceTaskListener(this.plugin, this, (TaskType) placeBlockType));
        }
        
        TaskType<?, ?> breakBlockType = this.taskTypeRegistry.getTypeById(TaskTypeId.BREAK_BLOCK);
        if (breakBlockType != null) {
            this.addListener(new BlockBreakTaskListener(this.plugin, this, (TaskType) breakBlockType));
        }
        
        TaskType<?, ?> breedMobType = this.taskTypeRegistry.getTypeById(TaskTypeId.BREED_MOB);
        if (breedMobType != null) {
            this.addListener(new BreedingTaskListener(this.plugin, this, (TaskType) breedMobType));
        }
        
        TaskType<?, ?> brewingType = this.taskTypeRegistry.getTypeById(TaskTypeId.BREWING);
        if (brewingType != null) {
            this.addListener(new BrewingTaskListener(this.plugin, this, (TaskType) brewingType));
        }
        
        TaskType<?, ?> cookItemType = this.taskTypeRegistry.getTypeById(TaskTypeId.COOK_ITEM);
        if (cookItemType != null) {
            this.addListener(new CookingTaskListener(this.plugin, this, (TaskType) cookItemType));
        }
        
        TaskType<?, ?> craftItemType = this.taskTypeRegistry.getTypeById(TaskTypeId.CRAFT_ITEM);
        if (craftItemType != null) {
            this.addListener(new CraftingTaskListener(this.plugin, this, (TaskType) craftItemType));
        }
        
        TaskType<?, ?> enchantingType = this.taskTypeRegistry.getTypeById(TaskTypeId.ENCHANTING);
        if (enchantingType != null) {
            this.addListener(new EnchantingTaskListener(this.plugin, this, (TaskType) enchantingType));
        }
        
        TaskType<?, ?> fertilizingType = this.taskTypeRegistry.getTypeById(TaskTypeId.FERTILIZING);
        if (fertilizingType != null) {
            this.addListener(new FertilizingTaskListener(this.plugin, this, (TaskType) fertilizingType));
        }
        
        TaskType<?, ?> fishItemType = this.taskTypeRegistry.getTypeById(TaskTypeId.FISH_ITEM);
        if (fishItemType != null) {
            this.addListener(new FishingTaskListener(this.plugin, this, (TaskType) fishItemType));
        }
        
        TaskType<?, ?> forgeItemType = this.taskTypeRegistry.getTypeById(TaskTypeId.FORGE_ITEM);
        if (forgeItemType != null) {
            this.addListener(new ForgingTaskListener(this.plugin, this, (TaskType) forgeItemType));
        }
        
        TaskType<?, ?> blockLootType = this.taskTypeRegistry.getTypeById(TaskTypeId.BLOCK_LOOT);
        if (blockLootType != null) {
            this.addListener(new BlockDropTaskListener(this.plugin, this, (TaskType) blockLootType));
        }
        
        TaskType<?, ?> mobLootType = this.taskTypeRegistry.getTypeById(TaskTypeId.MOB_LOOT);
        if (mobLootType != null) {
            this.addListener(new MobDropTaskListener(this.plugin, this, (TaskType) mobLootType));
        }
        
        TaskType<?, ?> grindstoneType = this.taskTypeRegistry.getTypeById(TaskTypeId.GRINDSTONE_ITEM);
        if (grindstoneType != null) {
            this.addListener(new GrindstoneTaskListener(this.plugin, this, (TaskType) grindstoneType));
        }
        
        TaskType<?, ?> killMobType = this.taskTypeRegistry.getTypeById(TaskTypeId.KILL_MOB);
        if (killMobType != null) {
            this.addListener(new KillingTaskListener(this.plugin, this, (TaskType) killMobType));
        }
        
        TaskType<?, ?> milkMobType = this.taskTypeRegistry.getTypeById(TaskTypeId.MILK_MOB);
        if (milkMobType != null) {
            this.addListener(new MilkingTaskListener(this.plugin, this, (TaskType) milkMobType));
        }
        
        TaskType<?, ?> shearMobType = this.taskTypeRegistry.getTypeById(TaskTypeId.SHEAR_MOB);
        if (shearMobType != null) {
            this.addListener(new ShearingTaskListener(this.plugin, this, (TaskType) shearMobType));
        }
        
        TaskType<?, ?> tameMobType = this.taskTypeRegistry.getTypeById(TaskTypeId.TAME_MOB);
        if (tameMobType != null) {
            this.addListener(new TamingTaskListener(this.plugin, this, (TaskType) tameMobType));
        }
        
        // 新增的任务类型
        TaskType<?, ?> shootArrowType = this.taskTypeRegistry.getTypeById(TaskTypeId.SHOOT_ARROW);
        if (shootArrowType != null) {
            this.addListener(new ShootArrowTaskListener(this.plugin, this, (TaskType) shootArrowType));
        }
        
        TaskType<?, ?> shootTargetType = this.taskTypeRegistry.getTypeById(TaskTypeId.SHOOT_TARGET);
        if (shootTargetType != null) {
            this.addListener(new ShootTargetTaskListener(this.plugin, this, (TaskType) shootTargetType));
        }
        
        TaskType<?, ?> throwSnowballType = this.taskTypeRegistry.getTypeById(TaskTypeId.THROW_SNOWBALL);
        if (throwSnowballType != null) {
            this.addListener(new ThrowProjectileTaskListener(this.plugin, this, (TaskType) throwSnowballType));
        }
        
        TaskType<?, ?> throwEggType = this.taskTypeRegistry.getTypeById(TaskTypeId.THROW_EGG);
        if (throwEggType != null) {
            this.addListener(new ThrowProjectileTaskListener(this.plugin, this, (TaskType) throwEggType));
        }
        
        TaskType<?, ?> throwEnderpearlType = this.taskTypeRegistry.getTypeById(TaskTypeId.THROW_ENDERPEARL);
        if (throwEnderpearlType != null) {
            this.addListener(new ThrowProjectileTaskListener(this.plugin, this, (TaskType) throwEnderpearlType));
        }
        
        TaskType<?, ?> itemPickupType = this.taskTypeRegistry.getTypeById(TaskTypeId.ITEM_PICKUP);
        if (itemPickupType != null) {
            this.addListener(new ItemPickupTaskListener(this.plugin, this, (TaskType) itemPickupType));
        }
        
        TaskType<?, ?> itemDropType = this.taskTypeRegistry.getTypeById(TaskTypeId.ITEM_DROP);
        if (itemDropType != null) {
            this.addListener(new ItemDropTaskListener(this.plugin, this, (TaskType) itemDropType));
        }
        
        TaskType<?, ?> itemBreakType = this.taskTypeRegistry.getTypeById(TaskTypeId.ITEM_BREAK);
        if (itemBreakType != null) {
            this.addListener(new ItemBreakTaskListener(this.plugin, this, (TaskType) itemBreakType));
        }
        
        TaskType<?, ?> eatItemType = this.taskTypeRegistry.getTypeById(TaskTypeId.EAT_ITEM);
        if (eatItemType != null) {
            this.addListener(new EatItemTaskListener(this.plugin, this, (TaskType) eatItemType));
        }
        
        TaskType<?, ?> drinkPotionType = this.taskTypeRegistry.getTypeById(TaskTypeId.DRINK_POTION);
        if (drinkPotionType != null) {
            this.addListener(new EatItemTaskListener(this.plugin, this, (TaskType) drinkPotionType));
        }
        
        TaskType<?, ?> villagerTradeType = this.taskTypeRegistry.getTypeById(TaskTypeId.VILLAGER_TRADE);
        if (villagerTradeType != null) {
            this.addListener(new VillagerTradeTaskListener(this.plugin, this, (TaskType) villagerTradeType));
        }
        
        TaskType<?, ?> useAnvilType = this.taskTypeRegistry.getTypeById(TaskTypeId.USE_ANVIL);
        if (useAnvilType != null) {
            this.addListener(new UseAnvilTaskListener(this.plugin, this, (TaskType) useAnvilType));
        }
        
        TaskType<?, ?> useBedType = this.taskTypeRegistry.getTypeById(TaskTypeId.USE_BED);
        if (useBedType != null) {
            this.addListener(new UseBedTaskListener(this.plugin, this, (TaskType) useBedType));
        }
        
        TaskType<?, ?> useHoeType = this.taskTypeRegistry.getTypeById(TaskTypeId.USE_HOE);
        if (useHoeType != null) {
            this.addListener(new UseHoeTaskListener(this.plugin, this, (TaskType) useHoeType));
        }
        
        TaskType<?, ?> placeLavaType = this.taskTypeRegistry.getTypeById(TaskTypeId.PLACE_LAVA);
        if (placeLavaType != null) {
            this.addListener(new PlaceBucketTaskListener(this.plugin, this, (TaskType) placeLavaType));
        }
        
        TaskType<?, ?> placeWaterType = this.taskTypeRegistry.getTypeById(TaskTypeId.PLACE_WATER);
        if (placeWaterType != null) {
            this.addListener(new PlaceBucketTaskListener(this.plugin, this, (TaskType) placeWaterType));
        }
        
        TaskType<?, ?> launchFireworkType = this.taskTypeRegistry.getTypeById(TaskTypeId.LAUNCH_FIREWORK);
        if (launchFireworkType != null) {
            this.addListener(new LaunchFireworkTaskListener(this.plugin, this, (TaskType) launchFireworkType));
        }
        
        TaskType<?, ?> playMusicDiscType = this.taskTypeRegistry.getTypeById(TaskTypeId.PLAY_MUSIC_DISC);
        if (playMusicDiscType != null) {
            this.addListener(new PlayMusicDiscTaskListener(this.plugin, this, (TaskType) playMusicDiscType));
        }
        
        TaskType<?, ?> playerDeathType = this.taskTypeRegistry.getTypeById(TaskTypeId.PLAYER_DEATH);
        if (playerDeathType != null) {
            this.addListener(new PlayerDeathTaskListener(this.plugin, this, (TaskType) playerDeathType));
        }
        
        TaskType<?, ?> playerJoinType = this.taskTypeRegistry.getTypeById(TaskTypeId.PLAYER_JOIN);
        if (playerJoinType != null) {
            this.addListener(new PlayerJoinTaskListener(this.plugin, this, (TaskType) playerJoinType));
        }
        
        TaskType<?, ?> useRiptideType = this.taskTypeRegistry.getTypeById(TaskTypeId.USE_RIPTIDE);
        if (useRiptideType != null) {
            this.addListener(new UseRiptideTaskListener(this.plugin, this, (TaskType) useRiptideType));
        }
        
        TaskType<?, ?> winRaidType = this.taskTypeRegistry.getTypeById(TaskTypeId.WIN_RAID);
        if (winRaidType != null) {
            this.addListener(new WinRaidTaskListener(this.plugin, this, (TaskType) winRaidType));
        }
        
        TaskType<?, ?> editBookType = this.taskTypeRegistry.getTypeById(TaskTypeId.EDIT_BOOK);
        if (editBookType != null) {
            this.addListener(new EditBookTaskListener(this.plugin, this, (TaskType) editBookType));
        }
        
        TaskType<?, ?> completeAdvancementType = this.taskTypeRegistry.getTypeById(TaskTypeId.COMPLETE_ADVANCEMENT);
        if (completeAdvancementType != null) {
            this.addListener(new CompleteAdvancementTaskListener(this.plugin, this, (TaskType) completeAdvancementType));
        }
    }

    private void registerTaskTypes() {
        // 原有的任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.PLACE_BLOCK, AdapterFamily.BLOCK, taskType -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.BREAK_BLOCK, AdapterFamily.BLOCK, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.BREED_MOB, AdapterFamily.ENTITY, taskType -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.BREWING, AdapterFamily.ITEM, taskType -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.COOK_ITEM, AdapterFamily.ITEM, taskType -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.CRAFT_ITEM, AdapterFamily.ITEM, taskType -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.ENCHANTING, AdapterFamily.ENCHANTMENT, taskType -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.FERTILIZING, AdapterFamily.BLOCK_STATE, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.FISH_ITEM, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.FORGE_ITEM, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.BLOCK_LOOT, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.MOB_LOOT, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.GRINDSTONE_ITEM, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.KILL_MOB, AdapterFamily.ENTITY, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.MILK_MOB, AdapterFamily.ENTITY, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.SHEAR_MOB, AdapterFamily.ENTITY, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.TAME_MOB, AdapterFamily.ENTITY, type -> {});
        
        // 新增：射击相关任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.SHOOT_ARROW, AdapterFamily.CUSTOM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.SHOOT_TARGET, AdapterFamily.BLOCK, type -> {});
        
        // 新增：投掷物相关任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.THROW_SNOWBALL, AdapterFamily.CUSTOM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.THROW_EGG, AdapterFamily.CUSTOM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.THROW_ENDERPEARL, AdapterFamily.CUSTOM, type -> {});
        
        // 新增：物品相关任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.ITEM_PICKUP, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.ITEM_DROP, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.ITEM_BREAK, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.EAT_ITEM, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.DRINK_POTION, AdapterFamily.ITEM, type -> {});
        
        // 新增：交互相关任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.VILLAGER_TRADE, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.USE_ANVIL, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.USE_BED, AdapterFamily.BLOCK, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.USE_HOE, AdapterFamily.BLOCK, type -> {});
        
        // 新增：桶类相关任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.PLACE_LAVA, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.PLACE_WATER, AdapterFamily.ITEM, type -> {});
        
        // 新增：其他任务类型
        this.taskTypeRegistry.registerType(TaskTypeId.LAUNCH_FIREWORK, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.PLAY_MUSIC_DISC, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.PLAYER_DEATH, AdapterFamily.CUSTOM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.PLAYER_JOIN, AdapterFamily.CUSTOM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.USE_RIPTIDE, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.WIN_RAID, AdapterFamily.CUSTOM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.EDIT_BOOK, AdapterFamily.ITEM, type -> {});
        this.taskTypeRegistry.registerType(TaskTypeId.COMPLETE_ADVANCEMENT, AdapterFamily.CUSTOM, type -> {});
    }

    public boolean canDoTasks(@NotNull Player player) {
        return player.getGameMode() != GameMode.CREATIVE && canDoTasksInVehicle(player);
    }

    public static boolean canDoTasksInVehicle(@NotNull Player player) {
        if (Config.ANTI_ABUSE_COUNT_IN_VEHICLES.get()) return true;

        Entity vehicle = player.getVehicle();
        return vehicle == null || vehicle instanceof LivingEntity;
    }

    public <O, A extends AdapterFamily<O>> void progressQuests(@NotNull Player player, @NotNull TaskType<O, A> taskType, @NotNull O entity, int amount) {
        Adapter<?, O> adapter = taskType.getAdapterFamily().getAdapterFor(entity);
        if (adapter == null) {
            return;
        }

        String fullName = adapter.toFullNameOfEntity(entity);
        if (fullName == null) {
            return;
        }

        this.plugin.milestoneManager().ifPresent(milestoneManager -> milestoneManager.progressMilestones(player, taskType, fullName, amount));
        this.plugin.questManager().ifPresent(questManager -> questManager.progressQuests(player, taskType, fullName, amount));
    }

    public boolean isArtificalSpawn(@NotNull SpawnReason reason) {
        return this.artificalMobSpawns.contains(reason);
    }

    public boolean isSpawnerMob(@NotNull Entity entity) {
        return PDCUtil.getBoolean(entity, this.mobSpawnerKey).isPresent();
    }

    public void markSpawnerMob(@NotNull Entity entity, boolean flag) {
        PDCUtil.set(entity, this.mobSpawnerKey, flag);
    }

    public boolean isPlayerBlock(@NotNull Block block) {
        return block.hasMetadata(PLAYER_BLOCK_MARKER) || PlayerBlockTracker.isTracked(block);
    }

    public void markPlayerBlock(@NotNull Block block, boolean flag) {
        if (flag) {
            block.setMetadata(PLAYER_BLOCK_MARKER, new FixedMetadataValue(this.plugin, true));
        }
        else {
            block.removeMetadata(PLAYER_BLOCK_MARKER, this.plugin);
        }
    }

    public void setWorkstationOwnerId(@NotNull TileState station, @NotNull UUID uuid) {
        PDCUtil.set(station, this.stationOwnerKey, uuid);
    }

    @Nullable
    public UUID getWorkstationOwnerId(@NotNull TileState station) {
        return PDCUtil.getUUID(station, this.stationOwnerKey).orElse(null);
    }

    @Nullable
    public Player getWorkstationOwner(@NotNull TileState station) {
        UUID uuid = getWorkstationOwnerId(station);
        return uuid == null ? null : Players.getPlayer(uuid);
    }

    public void setWorkstationMode(@NotNull TileState station, @NotNull WorkstationMode mode) {
        PDCUtil.set(station, this.stationModeKey, mode.getId());
    }

    @Nullable
    public WorkstationMode getWorkstationMode(@NotNull TileState station) {
        return PDCUtil.getInt(station, this.stationModeKey).map(WorkstationMode::byId).orElse(null);
    }
}
