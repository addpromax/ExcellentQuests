package su.nightexpress.quests.task;

public class TaskTypeId {

    // 原有的任务类型
    public static final String PLACE_BLOCK     = "place_block";
    public static final String BREAK_BLOCK     = "break_block";
    public static final String BREED_MOB       = "breed_mob";
    public static final String BREWING         = "brewing";
    public static final String COOK_ITEM       = "cook_item";
    public static final String CRAFT_ITEM      = "craft_item";
    public static final String ENCHANTING      = "enchanting";
    public static final String FERTILIZING     = "fertilize_block";
    public static final String FISH_ITEM       = "fish_item";
    public static final String FORGE_ITEM      = "forge_item";
    public static final String BLOCK_LOOT      = "block_loot";
    public static final String MOB_LOOT        = "mob_loot";
    public static final String GRINDSTONE_ITEM = "grindstone_item";
    public static final String KILL_MOB        = "kill_mob";
    public static final String MILK_MOB        = "milk_mob";
    public static final String SHEAR_MOB       = "shear_mob";
    public static final String TAME_MOB        = "tame_mob";
    
    // 新增：射击相关
    public static final String SHOOT_ARROW     = "shoot_arrow";      // 射箭
    public static final String SHOOT_TARGET    = "shoot_target";     // 射中靶心
    
    // 新增：投掷物相关
    public static final String THROW_SNOWBALL  = "throw_snowball";   // 投掷雪球
    public static final String THROW_EGG       = "throw_egg";        // 投掷鸡蛋
    public static final String THROW_ENDERPEARL= "throw_enderpearl"; // 投掷末影珍珠
    
    // 新增：物品相关
    public static final String ITEM_PICKUP     = "item_pickup";      // 拾取物品
    public static final String ITEM_DROP       = "item_drop";        // 丢弃物品
    public static final String ITEM_BREAK      = "item_break";       // 物品损坏
    public static final String EAT_ITEM        = "eat_item";         // 吃东西
    public static final String DRINK_POTION    = "drink_potion";     // 喝药水
    
    // 新增：交互相关
    public static final String VILLAGER_TRADE  = "villager_trade";   // 村民交易
    public static final String USE_ANVIL       = "use_anvil";        // 使用铁砧
    public static final String USE_BED         = "use_bed";          // 使用床
    public static final String USE_HOE         = "use_hoe";          // 使用锄头
    
    // 新增：桶类相关
    public static final String PLACE_LAVA      = "place_lava_bucket";  // 放置岩浆桶
    public static final String PLACE_WATER     = "place_water_bucket"; // 放置水桶
    
    // 新增：其他
    public static final String LAUNCH_FIREWORK = "launch_firework";  // 发射烟花
    public static final String PLAY_MUSIC_DISC = "play_music_disc";  // 播放音乐唱片
    public static final String PLAYER_DEATH    = "player_death";     // 死亡次数
    public static final String PLAYER_JOIN     = "player_join";      // 登录次数
    public static final String USE_RIPTIDE     = "use_riptide";      // 使用激流
    public static final String WIN_RAID        = "win_raid";         // 赢得突袭
    public static final String EDIT_BOOK       = "edit_book";        // 编辑书籍
    public static final String COMPLETE_ADVANCEMENT = "complete_advancement"; // 完成进度
}
