package su.nightexpress.quests.data;

import com.google.common.reflect.TypeToken;
import su.nightexpress.nightcore.db.sql.query.impl.InsertQuery;
import su.nightexpress.nightcore.db.sql.query.impl.UpdateQuery;
import su.nightexpress.nightcore.db.sql.util.WhereOperator;
import su.nightexpress.quests.battlepass.definition.BattlePassSeason;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.battlepass.data.BattlePassData;
import su.nightexpress.quests.user.QuestUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class DataQueries {

    public static final Function<ResultSet, QuestUser> USER_LOADER = resultSet -> {
        try {
            UUID uuid = UUID.fromString(resultSet.getString(DataHandler.COLUMN_USER_ID.getName()));
            String name = resultSet.getString(DataHandler.COLUMN_USER_NAME.getName());
            long dateCreated = resultSet.getLong(DataHandler.COLUMN_USER_DATE_CREATED.getName());
            long lastOnline = resultSet.getLong(DataHandler.COLUMN_USER_LAST_ONLINE.getName());
            long newQuestsDate = resultSet.getLong(DataHandler.COLUMN_NEW_QUESTS_DATE.getName());

            Map<UUID, BattlePassData> battlePassData = DataHandler.GSON.fromJson(resultSet.getString(DataHandler.COLUMN_BATTLE_PASS_DATA.getName()), new TypeToken<Map<UUID, BattlePassData>>(){}.getType());
            Map<UUID, QuestData> questData = DataHandler.GSON.fromJson(resultSet.getString(DataHandler.COLUMN_QUEST_DATA.getName()), new TypeToken<Map<UUID, QuestData>>(){}.getType());
            Map<String, MilestoneData> milestoneData = DataHandler.GSON.fromJson(resultSet.getString(DataHandler.COLUMN_MILESTONE_DATA.getName()), new TypeToken<Map<String, MilestoneData>>(){}.getType());

            // Remove battle pass datas for expired battle pass seasons.
            battlePassData.values().removeIf(BattlePassData::isExpired);

            QuestUser user = new QuestUser(uuid, name, dateCreated, lastOnline, newQuestsDate, battlePassData, questData, milestoneData);
            
            // 加载周期任务重置时间
            try {
                long newDailyQuestsDate = resultSet.getLong(DataHandler.COLUMN_NEW_DAILY_QUESTS_DATE.getName());
                long newWeeklyQuestsDate = resultSet.getLong(DataHandler.COLUMN_NEW_WEEKLY_QUESTS_DATE.getName());
                long newMonthlyQuestsDate = resultSet.getLong(DataHandler.COLUMN_NEW_MONTHLY_QUESTS_DATE.getName());
                
                user.setNewDailyQuestsDate(newDailyQuestsDate);
                user.setNewWeeklyQuestsDate(newWeeklyQuestsDate);
                user.setNewMonthlyQuestsDate(newMonthlyQuestsDate);
            } catch (SQLException e) {
                // 可能是旧数据，没有这些字段，使用默认值0
            }
            
            // 加载上次登录日期
            try {
                String lastLoginDate = resultSet.getString(DataHandler.COLUMN_LAST_LOGIN_DATE.getName());
                if (lastLoginDate != null) {
                    user.setLastLoginDate(lastLoginDate);
                }
            } catch (SQLException e) {
                // 可能是旧数据，没有这个字段，使用默认值空字符串
            }
            
            return user;
        }
        catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final Function<ResultSet, BattlePassSeason> SEASON_LOADER = resultSet -> {
        try {
            UUID uuid = UUID.fromString(resultSet.getString(DataHandler.COLUMN_BP_ID.getName()));
            String name = resultSet.getString(DataHandler.COLUMN_BP_NAME.getName());
            long startDate = resultSet.getLong(DataHandler.COLUMN_BP_START_DATE.getName());
            long endDate = resultSet.getLong(DataHandler.COLUMN_BP_END_DATE.getName());
            long expireDate = resultSet.getLong(DataHandler.COLUMN_BP_EXPIRE_DATE.getName());
            boolean active = resultSet.getBoolean(DataHandler.COLUMN_BP_ACTIVE.getName());

            return new BattlePassSeason(uuid, name, startDate, endDate, expireDate, active);
        }
        catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final InsertQuery<BattlePassSeason> SEASON_INSERT = new InsertQuery<BattlePassSeason>()
        .setValue(DataHandler.COLUMN_BP_ID, season -> season.getId().toString())
        .setValue(DataHandler.COLUMN_BP_NAME, BattlePassSeason::getName)
        .setValue(DataHandler.COLUMN_BP_START_DATE, season -> String.valueOf(season.getStartDate()))
        .setValue(DataHandler.COLUMN_BP_END_DATE, season -> String.valueOf(season.getEndDate()))
        .setValue(DataHandler.COLUMN_BP_EXPIRE_DATE, season -> String.valueOf(season.getExpireDate()))
        .setValue(DataHandler.COLUMN_BP_ACTIVE, season -> String.valueOf(season.isLaunched() ? 1 : 0));

    public static final UpdateQuery<BattlePassSeason> SEASON_UPDATE = new UpdateQuery<BattlePassSeason>()
        .setValue(DataHandler.COLUMN_BP_ACTIVE, season -> String.valueOf(season.isLaunched() ? 1 : 0));

    // 全局任务查询
    public static final Function<ResultSet, su.nightexpress.quests.quest.data.GlobalQuestData> GLOBAL_QUEST_LOADER = resultSet -> {
        try {
            String dataJson = resultSet.getString(DataHandler.COLUMN_GQ_DATA.getName());
            return DataHandler.GSON.fromJson(dataJson, su.nightexpress.quests.quest.data.GlobalQuestData.class);
        }
        catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final InsertQuery<su.nightexpress.quests.quest.data.GlobalQuestData> GLOBAL_QUEST_INSERT = 
        new InsertQuery<su.nightexpress.quests.quest.data.GlobalQuestData>()
            .setValue(DataHandler.COLUMN_GQ_ID, quest -> quest.getId().toString())
            .setValue(DataHandler.COLUMN_GQ_QUEST_ID, su.nightexpress.quests.quest.data.GlobalQuestData::getQuestId)
            .setValue(DataHandler.COLUMN_GQ_QUEST_TYPE, quest -> quest.getQuestType().name())
            .setValue(DataHandler.COLUMN_GQ_DATA, quest -> DataHandler.GSON.toJson(quest))
            .setValue(DataHandler.COLUMN_GQ_EXPIRE_DATE, quest -> String.valueOf(quest.getExpireDate()))
            .setValue(DataHandler.COLUMN_GQ_CREATE_DATE, quest -> String.valueOf(quest.getCreateDate()))
            .setValue(DataHandler.COLUMN_GQ_ACTIVE, quest -> String.valueOf(quest.isActive() ? 1 : 0));

    public static final UpdateQuery<su.nightexpress.quests.quest.data.GlobalQuestData> GLOBAL_QUEST_UPDATE = 
        new UpdateQuery<su.nightexpress.quests.quest.data.GlobalQuestData>()
            .setValue(DataHandler.COLUMN_GQ_QUEST_ID, su.nightexpress.quests.quest.data.GlobalQuestData::getQuestId)
            .setValue(DataHandler.COLUMN_GQ_QUEST_TYPE, quest -> quest.getQuestType().name())
            .setValue(DataHandler.COLUMN_GQ_DATA, quest -> DataHandler.GSON.toJson(quest))
            .setValue(DataHandler.COLUMN_GQ_EXPIRE_DATE, quest -> String.valueOf(quest.getExpireDate()))
            .setValue(DataHandler.COLUMN_GQ_CREATE_DATE, quest -> String.valueOf(quest.getCreateDate()))
            .setValue(DataHandler.COLUMN_GQ_ACTIVE, quest -> String.valueOf(quest.isActive() ? 1 : 0))
            .where(DataHandler.COLUMN_GQ_ID, WhereOperator.EQUAL, quest -> quest.getId().toString());
}
