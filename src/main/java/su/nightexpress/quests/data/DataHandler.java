package su.nightexpress.quests.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.db.AbstractUserDataManager;
import su.nightexpress.nightcore.db.sql.column.Column;
import su.nightexpress.nightcore.db.sql.column.ColumnType;
import su.nightexpress.nightcore.db.sql.query.impl.DeleteQuery;
import su.nightexpress.nightcore.db.sql.query.impl.SelectQuery;
import su.nightexpress.nightcore.db.sql.query.type.ValuedQuery;
import su.nightexpress.nightcore.db.sql.util.WhereOperator;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.battlepass.definition.BattlePassSeason;
import su.nightexpress.quests.data.serialize.MilestoneDataSerializer;
import su.nightexpress.quests.data.serialize.QuestCounterSerializer;
import su.nightexpress.quests.data.serialize.QuestDataSerializer;
import su.nightexpress.quests.data.serialize.BattlePassDataSerializer;
import su.nightexpress.quests.data.serialize.GlobalQuestDataSerializer;
import su.nightexpress.quests.quest.data.QuestCounter;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.battlepass.data.BattlePassData;
import su.nightexpress.quests.user.QuestUser;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

public class DataHandler extends AbstractUserDataManager<QuestsPlugin, QuestUser> {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(BattlePassData.class, new BattlePassDataSerializer())
        .registerTypeAdapter(QuestCounter.class, new QuestCounterSerializer())
        .registerTypeAdapter(QuestData.class, new QuestDataSerializer())
        .registerTypeAdapter(MilestoneData.class, new MilestoneDataSerializer())
        .registerTypeAdapter(GlobalQuestData.class, new GlobalQuestDataSerializer())
        .create();

    static final Column COLUMN_BATTLE_PASS_DATA = Column.of("battlePassData", ColumnType.STRING);
    static final Column COLUMN_QUEST_DATA       = Column.of("questData", ColumnType.STRING);
    static final Column COLUMN_MILESTONE_DATA   = Column.of("milestoneData", ColumnType.STRING);
    static final Column COLUMN_NEW_QUESTS_DATE  = Column.of("newQuestsDate", ColumnType.LONG);
    static final Column COLUMN_GLOBAL_QUEST_DATA = Column.of("globalQuestData", ColumnType.STRING);
    
    // 新增：周期任务重置时间列
    static final Column COLUMN_NEW_DAILY_QUESTS_DATE = Column.of("newDailyQuestsDate", ColumnType.LONG);
    static final Column COLUMN_NEW_WEEKLY_QUESTS_DATE = Column.of("newWeeklyQuestsDate", ColumnType.LONG);
    static final Column COLUMN_NEW_MONTHLY_QUESTS_DATE = Column.of("newMonthlyQuestsDate", ColumnType.LONG);
    
    // 新增：玩家上次登录日期列
    static final Column COLUMN_LAST_LOGIN_DATE = Column.of("lastLoginDate", ColumnType.STRING);

    static final Column COLUMN_BP_ID         = Column.of("seasonId", ColumnType.STRING);
    static final Column COLUMN_BP_NAME       = Column.of("name", ColumnType.STRING);
    static final Column COLUMN_BP_START_DATE = Column.of("startDate", ColumnType.LONG);
    static final Column COLUMN_BP_END_DATE   = Column.of("endDate", ColumnType.LONG);
    static final Column COLUMN_BP_EXPIRE_DATE   = Column.of("expireDate", ColumnType.LONG);
    static final Column COLUMN_BP_ACTIVE     = Column.of("active", ColumnType.BOOLEAN);

    // 全局任务表列定义
    // 注意: 数据库会自动创建一个名为 'id' 的 INTEGER 主键，所以我们的UUID列命名为 'globalId'
    static final Column COLUMN_GQ_ID          = Column.of("globalId", ColumnType.STRING);
    static final Column COLUMN_GQ_QUEST_ID    = Column.of("questId", ColumnType.STRING);
    static final Column COLUMN_GQ_QUEST_TYPE  = Column.of("questType", ColumnType.STRING);
    static final Column COLUMN_GQ_DATA        = Column.of("questData", ColumnType.STRING);
    static final Column COLUMN_GQ_EXPIRE_DATE = Column.of("expireDate", ColumnType.LONG);
    static final Column COLUMN_GQ_CREATE_DATE = Column.of("createDate", ColumnType.LONG);
    static final Column COLUMN_GQ_ACTIVE      = Column.of("active", ColumnType.BOOLEAN);

    static final String BP_TABLE = "bp_season";
    static final String GLOBAL_QUEST_TABLE = "global_quests";

    public DataHandler(@NotNull QuestsPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        this.plugin.info("[数据库] 创建战斗通行证表: " + BP_TABLE);
        this.createTable(BP_TABLE, Lists.newList(
            COLUMN_BP_ID,
            COLUMN_BP_NAME,
            COLUMN_BP_START_DATE,
            COLUMN_BP_END_DATE,
            COLUMN_BP_EXPIRE_DATE,
            COLUMN_BP_ACTIVE
        ));
        
        // 创建全局任务数据表
        this.plugin.info("[数据库] 创建全局任务表: " + GLOBAL_QUEST_TABLE);
        this.createTable(GLOBAL_QUEST_TABLE, Lists.newList(
            COLUMN_GQ_ID,
            COLUMN_GQ_QUEST_ID,
            COLUMN_GQ_QUEST_TYPE,
            COLUMN_GQ_DATA,
            COLUMN_GQ_EXPIRE_DATE,
            COLUMN_GQ_CREATE_DATE,
            COLUMN_GQ_ACTIVE
        ));
        
        this.plugin.info("[数据库] 数据表初始化完成");
    }

    @Override
    @NotNull
    protected Function<ResultSet, QuestUser> createUserFunction() {
        return DataQueries.USER_LOADER;
    }

    @Override
    protected void addUpsertQueryData(@NotNull ValuedQuery<?, QuestUser> query) {
        query.setValue(COLUMN_NEW_QUESTS_DATE, user -> String.valueOf(user.getNewQuestsDate()));
        query.setValue(COLUMN_BATTLE_PASS_DATA, user -> GSON.toJson(user.getBattlePassData()));
        query.setValue(COLUMN_QUEST_DATA, user -> GSON.toJson(user.getQuestData()));
        query.setValue(COLUMN_MILESTONE_DATA, user -> GSON.toJson(user.getMilestoneDataMap()));
        
        // 保存周期任务重置时间
        query.setValue(COLUMN_NEW_DAILY_QUESTS_DATE, user -> String.valueOf(user.getNewDailyQuestsDate()));
        query.setValue(COLUMN_NEW_WEEKLY_QUESTS_DATE, user -> String.valueOf(user.getNewWeeklyQuestsDate()));
        query.setValue(COLUMN_NEW_MONTHLY_QUESTS_DATE, user -> String.valueOf(user.getNewMonthlyQuestsDate()));
        
        // 保存上次登录日期
        query.setValue(COLUMN_LAST_LOGIN_DATE, QuestUser::getLastLoginDate);
    }

    @Override
    protected void addSelectQueryData(@NotNull SelectQuery<QuestUser> query) {
        query.column(COLUMN_NEW_QUESTS_DATE);
        query.column(COLUMN_BATTLE_PASS_DATA);
        query.column(COLUMN_QUEST_DATA);
        query.column(COLUMN_MILESTONE_DATA);
        
        // 查询周期任务重置时间
        query.column(COLUMN_NEW_DAILY_QUESTS_DATE);
        query.column(COLUMN_NEW_WEEKLY_QUESTS_DATE);
        query.column(COLUMN_NEW_MONTHLY_QUESTS_DATE);
        
        // 查询上次登录日期
        query.column(COLUMN_LAST_LOGIN_DATE);
    }

    @Override
    protected void addTableColumns(@NotNull List<Column> columns) {
        columns.add(COLUMN_NEW_QUESTS_DATE);
        columns.add(COLUMN_BATTLE_PASS_DATA);
        columns.add(COLUMN_QUEST_DATA);
        columns.add(COLUMN_MILESTONE_DATA);
        
        // 添加周期任务重置时间列
        columns.add(COLUMN_NEW_DAILY_QUESTS_DATE);
        columns.add(COLUMN_NEW_WEEKLY_QUESTS_DATE);
        columns.add(COLUMN_NEW_MONTHLY_QUESTS_DATE);
        
        // 添加上次登录日期列
        columns.add(COLUMN_LAST_LOGIN_DATE);
    }

    @NotNull
    public List<BattlePassSeason> loadBattlePassSeasons() {
        return this.select(BP_TABLE, DataQueries.SEASON_LOADER, SelectQuery::all);
    }

    public void insertBattlePassSeason(@NotNull BattlePassSeason season) {
        this.insert(BP_TABLE, DataQueries.SEASON_INSERT, season);
    }

    public void saveBattlePassSeason(@NotNull BattlePassSeason season) {
        this.update(BP_TABLE, DataQueries.SEASON_UPDATE, season);
    }

    public void removeBattlePassSeason(@NotNull BattlePassSeason season) {
        this.delete(BP_TABLE, new DeleteQuery<BattlePassSeason>().where(COLUMN_BP_ID, WhereOperator.EQUAL, passSeason -> passSeason.getId().toString()), season);
    }

    @Override
    @NotNull
    protected GsonBuilder registerAdapters(@NotNull GsonBuilder builder) {
        return builder;
    }

    // 全局任务数据操作（数据库版本）
    
    /**
     * 加载所有全局任务
     */
    @NotNull
    public List<GlobalQuestData> loadGlobalQuests() {
        try {
            return this.select(GLOBAL_QUEST_TABLE, DataQueries.GLOBAL_QUEST_LOADER, SelectQuery::all);
        } catch (Exception e) {
            // 表可能还不存在，返回空列表
            this.plugin.warn("无法加载全局任务数据（表可能不存在）: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * 插入新的全局任务
     */
    public void insertGlobalQuest(@NotNull GlobalQuestData globalQuest) {
        try {
            this.insert(GLOBAL_QUEST_TABLE, DataQueries.GLOBAL_QUEST_INSERT, globalQuest);
        } catch (Exception e) {
            this.plugin.error("插入全局任务失败: " + e.getMessage());
            // 不抛出异常，避免中断任务刷新流程
        }
    }
    
    /**
     * 更新全局任务数据
     */
    public void updateGlobalQuest(@NotNull GlobalQuestData globalQuest) {
        try {
            this.update(GLOBAL_QUEST_TABLE, DataQueries.GLOBAL_QUEST_UPDATE, globalQuest);
        } catch (Exception e) {
            this.plugin.error("更新全局任务失败: " + e.getMessage());
            // 不抛出异常，避免中断任务刷新流程
        }
    }
    
    /**
     * 删除全局任务
     */
    public void removeGlobalQuest(@NotNull GlobalQuestData globalQuest) {
        this.delete(GLOBAL_QUEST_TABLE, 
            new DeleteQuery<GlobalQuestData>().where(COLUMN_GQ_ID, WhereOperator.EQUAL, 
                quest -> quest.getId().toString()), 
            globalQuest);
    }
    
    /**
     * 删除所有过期的全局任务
     */
    public void removeExpiredGlobalQuests() {
        // 先加载所有任务，然后删除过期的
        List<GlobalQuestData> allQuests = this.loadGlobalQuests();
        for (GlobalQuestData quest : allQuests) {
            if (quest.isExpired()) {
                this.removeGlobalQuest(quest);
            }
        }
    }

    @Override
    public void onSynchronize() {
        // TODO: 实现多服务器同步逻辑
    }
}
