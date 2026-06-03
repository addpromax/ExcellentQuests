package su.nightexpress.quests.config;

import org.bukkit.Sound;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.EnumLocale;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.locale.message.MessageData;
import su.nightexpress.quests.battlepass.definition.BattlePassType;
import su.nightexpress.quests.quest.command.QuestsCommands;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.quests.QuestsPlaceholders.*;

public class Lang implements LangContainer {

    public static final EnumLocale<BattlePassType> BATTLE_PASS_MODE = LangEntry.builder("BattlePassMode").enumeration(BattlePassType.class);

    public static final TextLocale COMMAND_ARGUMENT_NAME_MILESTONE = LangEntry.builder("Command.ArgumentName.Milestone").text("milestone");
    public static final TextLocale COMMAND_ARGUMENT_NAME_QUEST     = LangEntry.builder("Command.ArgumentName.Quest").text("quest");
    public static final TextLocale COMMAND_ARGUMENT_NAME_DURATION  = LangEntry.builder("Command.ArgumentName.Duration").text("duration");

    public static final TextLocale COMMAND_BATTLE_PASS_NAME        = LangEntry.builder("Command.BattlePass.Name").text("Battle Pass");
    public static final TextLocale COMMAND_BATTLE_PASS_DESC        = LangEntry.builder("Command.BattlePass.Desc").text("View Battle Pass.");
    public static final TextLocale COMMAND_BATTLE_PASS_START_DESC  = LangEntry.builder("Command.BattlePass.Start.Desc").text("Start a new season.");
    public static final TextLocale COMMAND_BATTLE_PASS_CANCEL_DESC = LangEntry.builder("Command.BattlePass.Start.Desc").text("Cancel current or scheduled season.");

    public static final TextLocale COMMAND_BATTLE_PASS_ADD_LEVEL_DESC     = LangEntry.builder("Command.BattlePass.AddLevel.Desc").text("Add BattlePass levels.");
    public static final TextLocale COMMAND_BATTLE_PASS_REMOVE_LEVEL_DESC  = LangEntry.builder("Command.BattlePass.RemoveLevel.Desc").text("Remove BattlePass levels.");
    public static final TextLocale COMMAND_BATTLE_PASS_SET_LEVEL_DESC     = LangEntry.builder("Command.BattlePass.SetLevel.Desc").text("Set BattlePass levels.");
    public static final TextLocale COMMAND_BATTLE_PASS_ADD_XP_DESC        = LangEntry.builder("Command.BattlePass.AddXP.Desc").text("Add BattlePass XP.");
    public static final TextLocale COMMAND_BATTLE_PASS_REMOVE_XP_DESC     = LangEntry.builder("Command.BattlePass.RemoveXP.Desc").text("Remove BattlePass XP.");
    public static final TextLocale COMMAND_BATTLE_PASS_SET_XP_DESC        = LangEntry.builder("Command.BattlePass.SetXP.Desc").text("Set BattlePass XP.");
    
    public static final TextLocale COMMAND_QUESTS_NAME         = LangEntry.builder("Command.Quests.Name").text("任务");
    public static final TextLocale COMMAND_QUESTS_DESC         = LangEntry.builder("Command.Quests.Desc").text("查看任务列表");
    public static final TextLocale COMMAND_QUESTS_REFRESH_DESC = LangEntry.builder("Command.Quests.Refresh.Desc").text("刷新玩家的任务");
    public static final TextLocale COMMAND_QUESTS_REFRESH_ALL_DESC = LangEntry.builder("Command.Quests.Refresh.All.Desc").text("刷新所有类型的任务");
    public static final TextLocale COMMAND_QUESTS_REFRESH_INDEPENDENT_DESC = LangEntry.builder("Command.Quests.Refresh.Independent.Desc").text("刷新个人任务");
    public static final TextLocale COMMAND_QUESTS_REFRESH_COOPERATIVE_DESC = LangEntry.builder("Command.Quests.Refresh.Cooperative.Desc").text("刷新合作任务");
    public static final TextLocale COMMAND_QUESTS_REFRESH_COMPETITIVE_DESC = LangEntry.builder("Command.Quests.Refresh.Competitive.Desc").text("刷新竞争任务");
    public static final TextLocale COMMAND_QUESTS_INDEPENDENT_DESC = LangEntry.builder("Command.Quests.Independent.Desc").text("查看个人任务");
    public static final TextLocale COMMAND_QUESTS_COOPERATIVE_DESC = LangEntry.builder("Command.Quests.Cooperative.Desc").text("查看合作任务");
    public static final TextLocale COMMAND_QUESTS_COMPETITIVE_DESC = LangEntry.builder("Command.Quests.Competitive.Desc").text("查看竞争任务");
    public static final TextLocale COMMAND_QUESTS_DELETE_DESC = LangEntry.builder("Command.Quests.Delete.Desc").text("删除任务");
    public static final TextLocale COMMAND_QUESTS_DELETE_INDEPENDENT_DESC = LangEntry.builder("Command.Quests.Delete.Independent.Desc").text("删除指定玩家的独立任务");
    public static final TextLocale COMMAND_QUESTS_DELETE_COOPERATIVE_DESC = LangEntry.builder("Command.Quests.Delete.Cooperative.Desc").text("删除全服的合作任务");
    public static final TextLocale COMMAND_QUESTS_DELETE_COMPETITIVE_DESC = LangEntry.builder("Command.Quests.Delete.Competitive.Desc").text("删除全服的竞争任务");
    public static final TextLocale COMMAND_QUESTS_FORCE_COMPLETE_DESC = LangEntry.builder("Command.Quests.ForceComplete.Desc").text("强制完成指定玩家的指定任务");

    public static final TextLocale COMMAND_MILESTONES_NAME       = LangEntry.builder("Command.Milestones.Name").text("Milestones");
    public static final TextLocale COMMAND_MILESTONES_DESC       = LangEntry.builder("Command.Milestones.Desc").text("View milestones.");
    public static final TextLocale COMMAND_MILESTONES_RESET_DESC = LangEntry.builder("Command.Milestones.Reset.Desc").text("Reset a player's milestone.");

    public static final MessageLocale COMMAND_SYNTAX_INVALID_MILESTONE = LangEntry.builder("Command.Syntax.InvalidMilestone").chatMessage(
        GRAY.wrap(SOFT_RED.wrap(GENERIC_INPUT) + " is not a valid milestone!")
    );

    public static final MessageLocale COMMAND_SYNTAX_INVALID_QUEST = LangEntry.builder("Command.Syntax.InvalidQuest").chatMessage(
        GRAY.wrap(SOFT_RED.wrap(GENERIC_INPUT) + " is not a valid quest!")
    );

    public static final MessageLocale BATTLE_PASS_SEASON_CANCEL_NOTHING = LangEntry.builder("BattlePass.Season.Cancel.Nothing").chatMessage(
        GRAY.wrap("There is no active or scheduled season to cancel.")
    );

    public static final MessageLocale BATTLE_PASS_SEASON_CANCELLED = LangEntry.builder("BattlePass.Season.Cancelled").chatMessage(
        GRAY.wrap("Successfully cancelled the " + YELLOW.wrap(SEASON_NAME) + " season.")
    );

    public static final MessageLocale BATTLE_PASS_SEASON_SCHEDULE_ALREADY = LangEntry.builder("BattlePass.Season.Schedule.Already").chatMessage(
        GRAY.wrap("There is already an active or scheduled season. You need to cancel it first.")
    );

    /*public static final MessageLocale BATTLE_PASS_SEASON_SCHEDULED = LangEntry.builder("BattlePass.Season.Scheduled").message(
        MessageData.CHAT_NO_PREFIX,
        " ",
        YELLOW.and(BOLD).wrap("NEW SEASON IS COMING!"),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("Season: ") + YELLOW.wrap(SEASON_NAME),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("Launch Date: ") + YELLOW.wrap(SEASON_START_DATE),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("End Date: ") + YELLOW.wrap(SEASON_END_DATE),
        " "
    );*/

    public static final MessageLocale BATTLE_PASS_SEASON_LAUNCHED = LangEntry.builder("BattlePass.Season.Launched").message(
        MessageData.CHAT_NO_PREFIX,
        " ",
        GREEN.and(BOLD).wrap("SEASON " + WHITE.wrap(SEASON_NAME) + " JUST LAUNCHED!"),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("Duration: ") + GREEN.wrap(SEASON_TIME_LEFT),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("End Date: ") + GREEN.wrap(SEASON_END_DATE),
        //" ",
        //GRAY.wrap("Click " + RUN_COMMAND.with("/" + QuestsCommands.DEFAULT_ALIAS).wrap(YELLOW.and(BOLD).wrap("[HERE]")) + " to view your quests!"),
        " "
    );

    public static final MessageLocale BATTLE_PASS_SEASON_FINISHED = LangEntry.builder("BattlePass.Season.Finished").message(
        MessageData.CHAT_NO_PREFIX,
        " ",
        RED.and(BOLD).wrap("SEASON " + WHITE.wrap(SEASON_NAME) + " IS OVER!"),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("Launch Date: ") + RED.wrap(SEASON_START_DATE),
        DARK_GRAY.wrap("»") + " " + GRAY.wrap("End Date: ") + RED.wrap(SEASON_END_DATE),
        //" ",
        //GRAY.wrap("Click " + RUN_COMMAND.with("/" + BattlePassCommands.DEFAULT_ALIAS).wrap(YELLOW.and(BOLD).wrap("[HERE]")) + " to claim your rewards!"),
        " "
    );

    public static final MessageLocale BATTLE_PASS_LEVEL_ADDED = LangEntry.builder("BattlePass.Level.Added").chatMessage(
        GRAY.wrap("Added " + SOFT_GREEN.wrap(GENERIC_AMOUNT) + " level(s) to " + SOFT_GREEN.wrap(PLAYER_NAME) + "'s Battle Pass.")
    );

    public static final MessageLocale BATTLE_PASS_LEVEL_REMOVED = LangEntry.builder("BattlePass.Level.Removed").chatMessage(
        GRAY.wrap("Removed " + SOFT_RED.wrap(GENERIC_AMOUNT) + " level(s) from " + SOFT_RED.wrap(PLAYER_NAME) + "'s Battle Pass.")
    );

    public static final MessageLocale BATTLE_PASS_LEVEL_SET = LangEntry.builder("BattlePass.Level.Set").chatMessage(
        GRAY.wrap("Set " + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + " level for " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s Battle Pass.")
    );

    public static final MessageLocale BATTLE_PASS_XP_ADDED = LangEntry.builder("BattlePass.XP.Added").chatMessage(
        GRAY.wrap("Added " + SOFT_GREEN.wrap(GENERIC_AMOUNT) + " XP to " + SOFT_GREEN.wrap(PLAYER_NAME) + "'s Battle Pass.")
    );

    public static final MessageLocale BATTLE_PASS_XP_REMOVED = LangEntry.builder("BattlePass.XP.Removed").chatMessage(
        GRAY.wrap("Removed " + SOFT_RED.wrap(GENERIC_AMOUNT) + " XP from " + SOFT_RED.wrap(PLAYER_NAME) + "'s Battle Pass.")
    );

    public static final MessageLocale BATTLE_PASS_XP_SET = LangEntry.builder("BattlePass.XP.Set").chatMessage(
        GRAY.wrap("Set " + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + " XP for " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s Battle Pass.")
    );

    public static final MessageLocale BATTLE_PASS_LEVEL_UP = LangEntry.builder("BattlePass.Level.Up").titleMessage(
        GREEN.wrap("↑ " + BOLD.and(UNDERLINED).wrap("LEVEL UP") + " ↑"),
        GRAY.wrap("Your Battle Pass level raised to " + GREEN.wrap(BATTLE_PASS_LEVEL) + "!"),
        Sound.ENTITY_PLAYER_LEVELUP
    );

    public static final MessageLocale BATTLE_PASS_LEVEL_DOWN = LangEntry.builder("BattlePass.Level.Downgrade").titleMessage(
        RED.wrap("↓ " + BOLD.and(UNDERLINED).wrap("LEVEL DOWNGRADE") + " ↓"),
        GRAY.wrap("Your Battle Pass level decreased to " + RED.wrap(BATTLE_PASS_LEVEL) + "!"),
        Sound.ENTITY_IRON_GOLEM_DEATH
    );

    /*public static final MessageLocale BATTLE_PASS_LEVEL_REWARDS_NOTIFY = LangEntry.builder("BattlePass.LevelRewards.Notify").message(MessageData.CHAT_NO_PREFIX,
        " ",
        GRAY.wrap(SOFT_YELLOW.and(BOLD).wrap("REWARDS:") + " You have " + SOFT_YELLOW.and(BOLD.and(UNDERLINED)).wrap(GENERIC_AMOUNT) + " Battle Pass rewards available! " +
            RUN_COMMAND.with("/" + CharacterCommands.DEFAULT_ALIAS + " " + CharacterCommands.REWARDS_ALIAS).wrap(SHOW_TEXT.with(GRAY.wrap("Click to view your rewards!")).wrap(SOFT_YELLOW.wrap("[Click to Claim]")))),
        " "
    );

    public static final MessageLocale BATTLE_PASS_LEVEL_REWARDS_RECEIVE = LangEntry.builder("BattlePass.LevelRewards.Receive").message(MessageData.CHAT_NO_PREFIX,
        " ",
        GRAY.wrap(SOFT_YELLOW.and(BOLD).wrap("REWARDS:") + " You received " + SOFT_YELLOW.and(BOLD.and(UNDERLINED)).wrap(GENERIC_AMOUNT) + " character rewards! " +
            SHOW_TEXT.with(GENERIC_REWARDS).wrap(SOFT_YELLOW.wrap("[Hover to View]"))),
        " "
    );

    public static final TextLocale REWARDS_ENTRY = LangEntry.builder("Other.Rewards.Entry").text(
        GRAY.wrap(REWARD_NAME)
    );*/

    public static final MessageLocale BATTLE_PASS_NO_ACTIVE_SEASON = LangEntry.builder("BattlePass.NoActiveSeason").chatMessage(
        GRAY.wrap("There is no active season.")
    );

    public static final MessageLocale QUESTS_LOCKED = LangEntry.builder("Quests.Locked").chatMessage(
        GRAY.wrap("Quests are not available currently.")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_FOR = LangEntry.builder("Quests.RefreshedFor").chatMessage(
        GRAY.wrap("已为 " + YELLOW.wrap(PLAYER_NAME) + " 刷新任务。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_ALL_FOR = LangEntry.builder("Quests.RefreshedAllFor").chatMessage(
        GRAY.wrap("已为 " + YELLOW.wrap(PLAYER_NAME) + " 刷新所有类型的任务。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_INDEPENDENT_FOR = LangEntry.builder("Quests.RefreshedIndependentFor").chatMessage(
        GRAY.wrap("已为 " + YELLOW.wrap(PLAYER_NAME) + " 刷新 " + SOFT_GREEN.wrap("个人任务") + "。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_COOPERATIVE_FOR = LangEntry.builder("Quests.RefreshedCooperativeFor").chatMessage(
        GRAY.wrap("已为 " + YELLOW.wrap(PLAYER_NAME) + " 刷新 " + SOFT_YELLOW.wrap("合作任务") + "。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_COMPETITIVE_FOR = LangEntry.builder("Quests.RefreshedCompetitiveFor").chatMessage(
        GRAY.wrap("已为 " + YELLOW.wrap(PLAYER_NAME) + " 刷新 " + SOFT_RED.wrap("竞争任务") + "。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_COOPERATIVE_GLOBAL = LangEntry.builder("Quests.RefreshedCooperativeGlobal").chatMessage(
        GRAY.wrap("已刷新全服的 " + SOFT_YELLOW.wrap("合作任务") + "，所有在线玩家已收到新的合作任务。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_COMPETITIVE_GLOBAL = LangEntry.builder("Quests.RefreshedCompetitiveGlobal").chatMessage(
        GRAY.wrap("已刷新全服的 " + SOFT_RED.wrap("竞争任务") + "，所有在线玩家已收到新的竞争任务。")
    );
    
    public static final MessageLocale QUESTS_REFRESHED_COOPERATIVE_NOTIFY = LangEntry.builder("Quests.RefreshedCooperativeNotify")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GOLD.and(BOLD).wrap("合作任务已刷新！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("新的合作任务已生成，与全服玩家一起完成吧！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("点击 " + RUN_COMMAND.with("/quests cooperative").wrap(GOLD.and(BOLD).wrap("[这里]")) + " 查看新任务！"),
            " "
        );
    
    public static final MessageLocale QUESTS_REFRESHED_COOPERATIVE_NOTIFY_WITH_COUNT = LangEntry.builder("Quests.RefreshedCooperativeNotifyWithCount")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GOLD.and(BOLD).wrap("合作任务已刷新！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("当日最高在线人数: ") + YELLOW.wrap("%max_online%") + GRAY.wrap(" 人"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("新增任务数量: ") + GREEN.wrap("%quest_count%") + GRAY.wrap(" 个"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("当前总任务数: ") + GOLD.wrap("%total_quests%") + GRAY.wrap(" 个"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("点击 " + RUN_COMMAND.with("/quests cooperative").wrap(GOLD.and(BOLD).wrap("[这里]")) + " 查看新任务！"),
            " "
        );
    
    public static final MessageLocale QUESTS_REFRESHED_COMPETITIVE_NOTIFY = LangEntry.builder("Quests.RefreshedCompetitiveNotify")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            RED.and(BOLD).wrap("竞争任务已刷新！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("新的竞争任务已生成，名额有限先到先得！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("点击 " + RUN_COMMAND.with("/quests competitive").wrap(RED.and(BOLD).wrap("[这里]")) + " 查看新任务！"),
            " "
        );
    
    public static final MessageLocale QUESTS_REFRESHED_COMPETITIVE_NOTIFY_WITH_COUNT = LangEntry.builder("Quests.RefreshedCompetitiveNotifyWithCount")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            RED.and(BOLD).wrap("竞争任务已刷新！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("当日最高在线人数: ") + YELLOW.wrap("%max_online%") + GRAY.wrap(" 人"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("新增任务数量: ") + GREEN.wrap("%quest_count%") + GRAY.wrap(" 个"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("当前总任务数: ") + RED.wrap("%total_quests%") + GRAY.wrap(" 个"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("点击 " + RUN_COMMAND.with("/quests competitive").wrap(RED.and(BOLD).wrap("[这里]")) + " 查看新任务！"),
            " "
        );
    
    public static final MessageLocale QUESTS_DELETED_INDEPENDENT = LangEntry.builder("Quests.DeletedIndependent").chatMessage(
        GRAY.wrap("已删除 " + YELLOW.wrap(PLAYER_NAME) + " 的 " + SOFT_GREEN.wrap("个人任务") + "（ID: " + SOFT_YELLOW.wrap(QUEST_DATA_ID) + "）。")
    );
    
    public static final MessageLocale QUESTS_DELETED_COOPERATIVE = LangEntry.builder("Quests.DeletedCooperative").chatMessage(
        GRAY.wrap("已删除全服的 " + SOFT_YELLOW.wrap("合作任务") + "（ID: " + SOFT_YELLOW.wrap(QUEST_DATA_ID) + "），所有在线玩家已收到通知。")
    );
    
    public static final MessageLocale QUESTS_DELETED_COMPETITIVE = LangEntry.builder("Quests.DeletedCompetitive").chatMessage(
        GRAY.wrap("已删除全服的 " + SOFT_RED.wrap("竞争任务") + "（ID: " + SOFT_YELLOW.wrap(QUEST_DATA_ID) + "），所有在线玩家已收到通知。")
    );
    
    public static final MessageLocale QUESTS_DELETED_COOPERATIVE_NOTIFY = LangEntry.builder("Quests.DeletedCooperativeNotify")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GOLD.and(BOLD).wrap("合作任务已被删除！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("一个合作任务已被管理员删除。"),
            " "
        );
    
    public static final MessageLocale QUESTS_DELETED_COMPETITIVE_NOTIFY = LangEntry.builder("Quests.DeletedCompetitiveNotify")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            RED.and(BOLD).wrap("竞争任务已被删除！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("一个竞争任务已被管理员删除。"),
            " "
        );
    
    public static final MessageLocale QUESTS_DELETE_FAILED = LangEntry.builder("Quests.DeleteFailed").chatMessage(
        SOFT_RED.wrap("删除任务失败！请检查任务ID是否正确。")
    );
    
    public static final MessageLocale QUESTS_FORCE_COMPLETED = LangEntry.builder("Quests.ForceCompleted").chatMessage(
        GRAY.wrap("已强制完成 " + YELLOW.wrap(PLAYER_NAME) + " 的任务（ID: " + SOFT_YELLOW.wrap(QUEST_DATA_ID) + "），任务奖励已发放。")
    );
    
    public static final MessageLocale QUESTS_FORCE_COMPLETE_FAILED = LangEntry.builder("Quests.ForceCompleteFailed").chatMessage(
        SOFT_RED.wrap("强制完成任务失败！请检查玩家是否在线以及任务ID是否正确。")
    );
    
    public static final MessageLocale QUESTS_FORCE_COMPLETE_NOT_FOUND = LangEntry.builder("Quests.ForceCompleteNotFound").chatMessage(
        SOFT_RED.wrap("未找到该任务！玩家可能没有这个任务数据。")
    );

    public static final MessageLocale QUESTS_REFRESHED = LangEntry.builder("Quests.Refreshed")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            YELLOW.and(BOLD).wrap("NEW QUESTS AVAILABLE!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("You have " + YELLOW.wrap(GENERIC_AMOUNT) + " new daily quests available."),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Click " + RUN_COMMAND.with("/" + QuestsCommands.DEFAULT_ALIAS).wrap(YELLOW.and(BOLD).wrap("HERE")) + " to view them!"),
            " "
        );
    
    public static final MessageLocale QUESTS_LOGIN_SUMMARY = LangEntry.builder("Quests.LoginSummary")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GOLD.and(BOLD).wrap("任务中心"),
            " ",
            DARK_GRAY.wrap("» ") + GRAY.wrap("个人任务: ") + YELLOW.wrap("%independent_count%") + GRAY.wrap(" 个 ") + RUN_COMMAND.with("/quests independent").wrap(YELLOW.wrap("[查看]")),
            DARK_GRAY.wrap("» ") + GRAY.wrap("合作任务: ") + GOLD.wrap("%cooperative_count%") + GRAY.wrap(" 个 ") + RUN_COMMAND.with("/quests cooperative").wrap(GOLD.wrap("[查看]")),
            DARK_GRAY.wrap("» ") + GRAY.wrap("竞争任务: ") + RED.wrap("%competitive_count%") + GRAY.wrap(" 个 ") + RUN_COMMAND.with("/quests competitive").wrap(RED.wrap("[查看]")),
            " "
        );

    public static final MessageLocale QUESTS_QUEST_TIME_OUT = LangEntry.builder("Quests.Quest.TimeOut")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            SOFT_RED.and(BOLD).wrap("QUEST EXPIRED!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("The " + SOFT_RED.wrap(QUEST_NAME) + " quest has expired!"),
            " "
        );

    public static final MessageLocale QUESTS_QUEST_ACCEPTED = LangEntry.builder("Quests.Quest.Accepted")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            YELLOW.and(BOLD).wrap("QUEST ACCEPTED!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Complete the " + YELLOW.wrap(QUEST_NAME) + " quest to get rewards!"),
            " "
        );

    public static final MessageLocale QUESTS_QUEST_COMPLETED_XP_ONLY = LangEntry.builder("Quests.Quest.Completed.XPOnly")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GREEN.and(BOLD).wrap("QUEST COMPLETED!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("You completed the " + GREEN.wrap(QUEST_NAME) + " quest!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Battle Pass XP: " + GREEN.wrap(GENERIC_XP)),
            " "
        );

    public static final MessageLocale QUESTS_QUEST_COMPLETED_XP_REWARDS = LangEntry.builder("Quests.Quest.Completed.XPRewards")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GREEN.and(BOLD).wrap("QUEST COMPLETED!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("You completed the " + GREEN.wrap(QUEST_NAME) + " quest!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Battle Pass XP: " + GREEN.wrap(GENERIC_XP)),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Reward(s): " + GREEN.wrap(GENERIC_REWARDS)),
            " "
        );

    public static final MessageLocale QUESTS_COMPETITIVE_NO_SLOTS = LangEntry.builder("Quests.Competitive.NoSlots")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            SOFT_RED.and(BOLD).wrap("竞争任务已满!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("任务 " + SOFT_RED.wrap(QUEST_NAME) + " 的名额已被其他玩家占满！"),
            " "
        );
    
    public static final MessageLocale QUESTS_COOPERATIVE_COMPLETED = LangEntry.builder("Quests.Cooperative.Completed")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GREEN.and(BOLD).wrap("合作任务完成!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("全服玩家共同完成了 " + GREEN.wrap(QUEST_NAME) + " 任务！"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Battle Pass XP: " + GREEN.wrap(GENERIC_XP)),
            " "
        );

    public static final MessageLocale MILESTONES_RESET_FOR = LangEntry.builder("Milestones.ResetFor").chatMessage(
        GRAY.wrap("Successfully reset " + YELLOW.wrap(MILESTONE_NAME) + " milestone progress for " + YELLOW.wrap(PLAYER_NAME) + "!")
    );

    public static final MessageLocale MILESTONES_MILESTONE_COMPLETED = LangEntry.builder("Milestones.Milestone.Completed")
        .message(MessageData.CHAT_NO_PREFIX,
            " ",
            GREEN.and(BOLD).wrap("MILESTONE COMPLETED!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("You completed " + WHITE.wrap("Level " + GENERIC_LEVEL) + " of the " + GREEN.wrap(MILESTONE_NAME) + " milestone!"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Reward(s): " + GREEN.wrap(GENERIC_REWARDS)),
            " "
        );

    public static final IconLocale UI_MILESTONES_CATEGORY_INFO = LangEntry.iconBuilder("UI.Milestones.CategoryInfo")
        .rawName(YELLOW.wrap(MILESTONE_CATEGORY_NAME) + DARK_GRAY.wrap(" ┃ ") + GOLD.wrap("Milestone"))
        .rawLore(
            MILESTONE_CATEGORY_DESCRIPTION,
            EMPTY_IF_ABOVE,
            DARK_GRAY.wrap("•") + WHITE.wrap(" Total Milestones: ") + YELLOW.wrap(GENERIC_TOTAL),
            DARK_GRAY.wrap("•") + WHITE.wrap(" Levels Completed: ") + YELLOW.wrap(GENERIC_LEVELS),
            DARK_GRAY.wrap("•") + WHITE.wrap(" Milestones Completed: ") + YELLOW.wrap(GENERIC_COMPLETED),
            "",
            YELLOW.wrap("→ " + UNDERLINED.wrap("Click to view!"))
        )
        .build();

    public static final IconLocale UI_MILESTONES_MILESTONE_INFO = LangEntry.iconBuilder("UI.Milestones.MilestoneInfo")
        .rawName(GOLD.wrap(MILESTONE_NAME))
        .rawLore(
            MILESTONE_DESCRIPTION,
            EMPTY_IF_ABOVE,
            GOLD.wrap("Progress:"),
            DARK_GRAY.wrap("»") + " " + GRAY.wrap("Level: " + WHITE.wrap(GENERIC_LEVEL) + "/" + WHITE.wrap(MILESTONE_LEVELS)),
            DARK_GRAY.wrap("»" + " " + GENERIC_PROGRESS_BAR + " (" + WHITE.wrap(GENERIC_PROGRESS + "%") + ")"),
            GENERIC_OBJECTIVES,
            "",
            GOLD.wrap("Rewards:"),
            GENERIC_REWARDS,
            "",
            GOLD.wrap("→ " + UNDERLINED.wrap("Click for details!"))
        )
        .build();

    public static final IconLocale UI_QUEST_AVAILABLE = LangEntry.iconBuilder("UI.Quest.Available")
        .rawName(GOLD.wrap(QUEST_NAME) + GRAY.wrap(" • ") + WHITE.wrap("Available"))
        .rawLore(
            QUEST_DESCRIPTION,
            EMPTY_IF_ABOVE,
            "",
            GOLD.wrap("Objectives:"),
            GENERIC_OBJECTIVES,
            "",
            GOLD.wrap("Rewards:"),
            GENERIC_BATTLE_PASS_REWARDS,
            GENERIC_REWARDS,
            "",
            GOLD.wrap("→ " + UNDERLINED.wrap("Click to accept!"))
        )
        .build();

    public static final IconLocale UI_QUEST_IN_PROGRESS = LangEntry.iconBuilder("UI.Quest.InProgress")
        .rawName(GOLD.wrap(QUEST_NAME) + GRAY.wrap(" • ") + YELLOW.wrap("In Progress"))
        .rawLore(
            QUEST_DESCRIPTION,
            EMPTY_IF_ABOVE,
            RED.wrap("⏳ Timeleft:") + " " + WHITE.wrap(GENERIC_TIMELEFT),
            "",
            GOLD.wrap("Progress:"),
            DARK_GRAY.wrap("»" + " " + GENERIC_PROGRESS_BAR + " (" + WHITE.wrap(GENERIC_PROGRESS + "%") + ")"),
            GENERIC_OBJECTIVES,
            "",
            GOLD.wrap("Rewards:"),
            GENERIC_BATTLE_PASS_REWARDS,
            GENERIC_REWARDS
        )
        .build();

    public static final IconLocale UI_QUEST_COMPLETED = LangEntry.iconBuilder("UI.Quest.Completed")
        .rawName(GOLD.wrap(QUEST_NAME) + GRAY.wrap(" • ") + GREEN.wrap("Completed"))
        .rawLore(
            QUEST_DESCRIPTION,
            EMPTY_IF_ABOVE,
            GOLD.wrap("Progress:"),
            DARK_GRAY.wrap("»" + " " + GENERIC_PROGRESS_BAR + " (" + WHITE.wrap(GENERIC_PROGRESS + "%") + ")"),
            GENERIC_OBJECTIVES,
            "",
            GOLD.wrap("Rewards:"),
            GENERIC_BATTLE_PASS_REWARDS,
            GENERIC_REWARDS
        )
        .build();

    public static final IconLocale UI_QUEST_FAILED = LangEntry.iconBuilder("UI.Quest.Failed")
        .rawName(GOLD.wrap(QUEST_NAME) + GRAY.wrap(" • ") + RED.wrap("Failed"))
        .rawLore(
            QUEST_DESCRIPTION,
            EMPTY_IF_ABOVE,
            GOLD.wrap("Progress:"),
            DARK_GRAY.wrap("»" + " " + GENERIC_PROGRESS_BAR + " (" + WHITE.wrap(GENERIC_PROGRESS + "%") + ")"),
            GENERIC_OBJECTIVES,
            "",
            GOLD.wrap("Rewards:"),
            GENERIC_BATTLE_PASS_REWARDS,
            GENERIC_REWARDS
        )
        .build();

    public static final TextLocale UI_MILESTONES_MILESTONE_OBJECTIVE = LangEntry.builder("UI.Milestones.MilestoneObjective")
        .text(DARK_GRAY.wrap("» " + WHITE.wrap(GENERIC_NAME + ":") + " " + SOFT_YELLOW.wrap(GENERIC_CURRENT) + "/" + SOFT_YELLOW.wrap(GENERIC_REQUIRED)));

    public static final TextLocale UI_ENTRY_REWARD = LangEntry.builder("UI.Entry.Reward.Custom")
        .text(DARK_GRAY.wrap("┃ " + WHITE.wrap(GENERIC_NAME)));

    public static final TextLocale UI_ENTRY_REWARD_BATTLE_PASS_XP = LangEntry.builder("UI.Entry.Reward.BattlePass.XP")
        .text(DARK_GRAY.wrap("┃ " + WHITE.wrap("Battle Pass XP: ") + YELLOW.wrap(GENERIC_XP)));

    // 通配符目标显示名称
    public static final TextLocale WILDCARD_ITEM_PICKUP = LangEntry.builder("Wildcard.ItemPickup").text("Any Items");
    public static final TextLocale WILDCARD_ITEM_DROP = LangEntry.builder("Wildcard.ItemDrop").text("Any Items");
    public static final TextLocale WILDCARD_ITEM_BREAK = LangEntry.builder("Wildcard.ItemBreak").text("Any Tools");
    public static final TextLocale WILDCARD_BLOCK_BREAK = LangEntry.builder("Wildcard.BlockBreak").text("Any Blocks");
    public static final TextLocale WILDCARD_BLOCK_PLACE = LangEntry.builder("Wildcard.BlockPlace").text("Any Blocks");
    public static final TextLocale WILDCARD_ENTITY_KILL = LangEntry.builder("Wildcard.EntityKill").text("Any Entities");
    public static final TextLocale WILDCARD_ENTITY_BREED = LangEntry.builder("Wildcard.EntityBreed").text("Any Entities");
    public static final TextLocale WILDCARD_ENTITY_TAME = LangEntry.builder("Wildcard.EntityTame").text("Any Entities");
    public static final TextLocale WILDCARD_ENTITY_SHEAR = LangEntry.builder("Wildcard.EntityShear").text("Any Entities");
    public static final TextLocale WILDCARD_FISHING = LangEntry.builder("Wildcard.Fishing").text("Any Fish");
    public static final TextLocale WILDCARD_EAT_ITEM = LangEntry.builder("Wildcard.EatItem").text("Any Food");
    public static final TextLocale WILDCARD_DRINK_POTION = LangEntry.builder("Wildcard.DrinkPotion").text("Any Potions");
    public static final TextLocale WILDCARD_ENCHANT_ITEM = LangEntry.builder("Wildcard.EnchantItem").text("Any Items");
    public static final TextLocale WILDCARD_VILLAGER_TRADE = LangEntry.builder("Wildcard.VillagerTrade").text("Any Items");
    public static final TextLocale WILDCARD_DEFAULT = LangEntry.builder("Wildcard.Default").text("Any Targets");

    // Vanilla String 目标显示名称
    public static final TextLocale VANILLA_STRING_JOIN = LangEntry.builder("VanillaString.Join").text("登录");
    public static final TextLocale VANILLA_STRING_DEATH = LangEntry.builder("VanillaString.Death").text("死亡");
    public static final TextLocale VANILLA_STRING_ADVANCEMENT = LangEntry.builder("VanillaString.Advancement").text("进度");
    public static final TextLocale VANILLA_STRING_ARROW = LangEntry.builder("VanillaString.Arrow").text("射箭");
    public static final TextLocale VANILLA_STRING_SNOWBALL = LangEntry.builder("VanillaString.Snowball").text("投掷雪球");
    public static final TextLocale VANILLA_STRING_EGG = LangEntry.builder("VanillaString.Egg").text("投掷鸡蛋");
    public static final TextLocale VANILLA_STRING_ENDERPEARL = LangEntry.builder("VanillaString.Enderpearl").text("投掷末影珍珠");
    public static final TextLocale VANILLA_STRING_RAID_VICTORY = LangEntry.builder("VanillaString.RaidVictory").text("突袭胜利");
    public static final TextLocale VANILLA_STRING_TARGET = LangEntry.builder("VanillaString.Target").text("射中靶心");
    public static final TextLocale VANILLA_STRING_ANVIL = LangEntry.builder("VanillaString.Anvil").text("使用铁砧");
    public static final TextLocale VANILLA_STRING_BED = LangEntry.builder("VanillaString.Bed").text("使用床");
    
    // 特殊物品/方块显示名称（用于附魔、酿造等）
    public static final TextLocale SPECIAL_ENCHANTING = LangEntry.builder("Special.Enchanting").text("附魔");
    public static final TextLocale SPECIAL_BREWING = LangEntry.builder("Special.Brewing").text("酿造");
    public static final TextLocale SPECIAL_FIREWORK = LangEntry.builder("Special.Firework").text("烟花");
    public static final TextLocale SPECIAL_MUSIC_DISC = LangEntry.builder("Special.MusicDisc").text("音乐唱片");
    public static final TextLocale SPECIAL_LAVA_BUCKET = LangEntry.builder("Special.LavaBucket").text("岩浆桶");
    public static final TextLocale SPECIAL_WATER_BUCKET = LangEntry.builder("Special.WaterBucket").text("水桶");
    public static final TextLocale SPECIAL_TRIDENT = LangEntry.builder("Special.Trident").text("三叉戟");
    public static final TextLocale SPECIAL_BOOK = LangEntry.builder("Special.Book").text("书籍");
    
    // Minecraft 标签翻译 - 方块/物品标签
    public static final TextLocale TAG_LOGS = LangEntry.builder("Tag.Logs").text("任意原木");
    public static final TextLocale TAG_PLANKS = LangEntry.builder("Tag.Planks").text("任意木板");
    public static final TextLocale TAG_WOOL = LangEntry.builder("Tag.Wool").text("任意羊毛");
    public static final TextLocale TAG_STONE_BRICKS = LangEntry.builder("Tag.StoneBricks").text("任意石砖");
    public static final TextLocale TAG_FLOWERS = LangEntry.builder("Tag.Flowers").text("任意花");
    public static final TextLocale TAG_SAPLINGS = LangEntry.builder("Tag.Saplings").text("任意树苗");
    public static final TextLocale TAG_LEAVES = LangEntry.builder("Tag.Leaves").text("任意树叶");
    public static final TextLocale TAG_WOODEN_DOORS = LangEntry.builder("Tag.WoodenDoors").text("任意木门");
    public static final TextLocale TAG_WOODEN_STAIRS = LangEntry.builder("Tag.WoodenStairs").text("任意木楼梯");
    public static final TextLocale TAG_WOODEN_SLABS = LangEntry.builder("Tag.WoodenSlabs").text("任意木台阶");
    public static final TextLocale TAG_WOODEN_FENCES = LangEntry.builder("Tag.WoodenFences").text("任意木栅栏");
    public static final TextLocale TAG_WOODEN_PRESSURE_PLATES = LangEntry.builder("Tag.WoodenPressurePlates").text("任意木压力板");
    public static final TextLocale TAG_WOODEN_BUTTONS = LangEntry.builder("Tag.WoodenButtons").text("任意木按钮");
    public static final TextLocale TAG_WOODEN_TRAPDOORS = LangEntry.builder("Tag.WoodenTrapdoors").text("任意木活板门");
    public static final TextLocale TAG_SAND = LangEntry.builder("Tag.Sand").text("任意沙子");
    public static final TextLocale TAG_ANVIL = LangEntry.builder("Tag.Anvil").text("任意铁砧");
    public static final TextLocale TAG_RAILS = LangEntry.builder("Tag.Rails").text("任意铁轨");
    public static final TextLocale TAG_BEDS = LangEntry.builder("Tag.Beds").text("任意床");
    public static final TextLocale TAG_BANNERS = LangEntry.builder("Tag.Banners").text("任意旗帜");
    public static final TextLocale TAG_BOATS = LangEntry.builder("Tag.Boats").text("任意船");
    public static final TextLocale TAG_FISHES = LangEntry.builder("Tag.Fishes").text("任意鱼");
    public static final TextLocale TAG_SIGNS = LangEntry.builder("Tag.Signs").text("任意告示牌");
    public static final TextLocale TAG_MUSIC_DISCS = LangEntry.builder("Tag.MusicDiscs").text("任意音乐唱片");
    public static final TextLocale TAG_COALS = LangEntry.builder("Tag.Coals").text("任意煤炭");
    public static final TextLocale TAG_ARROWS = LangEntry.builder("Tag.Arrows").text("任意箭");
    
    // Minecraft 标签翻译 - 实体标签
    public static final TextLocale TAG_RAIDERS = LangEntry.builder("Tag.Raiders").text("任意袭击者");
    public static final TextLocale TAG_SKELETONS = LangEntry.builder("Tag.Skeletons").text("任意骷髅");
    public static final TextLocale TAG_ZOMBIES = LangEntry.builder("Tag.Zombies").text("任意僵尸");
    public static final TextLocale TAG_IMPACT_PROJECTILES = LangEntry.builder("Tag.ImpactProjectiles").text("任意投射物");
    public static final TextLocale TAG_BEEHIVE_INHABITORS = LangEntry.builder("Tag.BeehiveInhabitors").text("任意蜂巢居民");
    public static final TextLocale TAG_AXOLOTL_ALWAYS_HOSTILES = LangEntry.builder("Tag.AxolotlAlwaysHostiles").text("任意美西螈敌对生物");
    public static final TextLocale TAG_AXOLOTL_HUNT_TARGETS = LangEntry.builder("Tag.AxolotlHuntTargets").text("任意美西螈狩猎目标");
    public static final TextLocale TAG_FREEZE_IMMUNE_ENTITY_TYPES = LangEntry.builder("Tag.FreezeImmuneEntityTypes").text("任意冰冻免疫生物");
    public static final TextLocale TAG_FREEZE_HURTS_EXTRA_TYPES = LangEntry.builder("Tag.FreezeHurtsExtraTypes").text("任意冰冻易伤生物");
    public static final TextLocale TAG_FROG_FOOD = LangEntry.builder("Tag.FrogFood").text("任意青蛙食物");
    public static final TextLocale TAG_POWDER_SNOW_WALKABLE_MOBS = LangEntry.builder("Tag.PowderSnowWalkableMobs").text("任意细雪行走生物");

}
