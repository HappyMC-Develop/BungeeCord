package ru.leymooo.botfilter.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Settings extends Config
{

    @Ignore
    public static final Settings IMP = new Settings();

    @Comment(
        {
        "请在github上提交所有错误、bug、建议和其他请求 "
        })
    @Final
    public final String ISSUES = "https://github.com/Leymooo/BungeeCord/issues";
    @Final
    public final String HELP = "http://www.rubukkit.org/threads/137038/";
    @Final
    public String BOT_FILTER_VERSION = "3.8.13";

    @Create
    public MESSAGES MESSAGES;
    @Create
    public DIMENSIONS DIMENSIONS;
    @Create
    public GEO_IP GEO_IP;
    @Create
    public PING_CHECK PING_CHECK;
    @Create
    public SERVER_PING_CHECK SERVER_PING_CHECK;
    @Create
    public PROTECTION PROTECTION;
    @Create
    public SQL SQL;
    @Comment(
        {
        "当1分钟内有多少玩家/假人进入服务器时，自动启用保护",
        "无推广/活动时的推荐数值: ",
        "低于150名玩家 - 25, 低于250 - 30, 低于350 - 35, 低于550 - 40,45, 更多玩家 - 自行调整 ",
        "在推广/活动时期或刚设置保护的时候, 推荐将这些数值调高"
        })
    public int PROTECTION_THRESHOLD = 30;
    @Comment("自动保护的持续时间, 以毫秒为单位. 1秒=1000毫秒")
    public int PROTECTION_TIME = 120000;
    @Comment("在被假人攻击时, 进入服务器是否强制检查. 不论通过与否, 都会被强制检查")
    public boolean FORCE_CHECK_ON_ATTACK = true;
    @Comment("是否在在线人数上加上在过滤器中(验证中)的玩家")
    public boolean SHOW_ONLINE = true;
    @Comment("玩家必须在多少时间内完成验证, 以毫秒为单位. 1秒=1000毫秒")
    public int TIME_OUT = 12700;
    @Comment("是否启用客户端 'Team 'xxx' already exist in this scoreboard 报错的修复？'")
    public boolean FIX_SCOREBOARD_TEAMS = true;
    @Comment("是否将未通过验证的玩家/假人的IP记录到文件中??")
    public boolean SAVE_FAILED_IPS_TO_FILE = true;

    public void reload(File file)
    {
        load( file );
        save( file );
    }

    @Comment("如果要换行，请不要使用 '\\n', 应该使用 %nl%")
    public static class MESSAGES
    {

        public String PREFIX = "&b&lBot&d&lFilter";
        public String CHECKING = "%prefix%&7>> &a等待检查完成…";
        public String CHECKING_CAPTCHA = "%prefix%&7>> &a在聊天框中输入图片中的数字";
        public String CHECKING_CAPTCHA_WRONG = "%prefix%&7>> &c您输入错误的验证码，请再试一次.你还可以尝试 &a%s &c%s 次";
        public String SUCCESSFULLY = "%prefix%&7>> &a检查通过，祝你玩得开心";
        public String KICK_MANY_CHECKS = "%prefix%%nl%%nl%&c我们检测到了来自于你的IP地址的可疑活动%nl%%nl%&6请在10分钟后重试";
        public String KICK_NOT_PLAYER = "%prefix%%nl%%nl%&c你没有通过验证, 你是个假人吗?%nl%&7&o如果不是, 请重试";
        public String KICK_COUNTRY = "%prefix%%nl%%nl%&c你所在的国家被禁止进入服务器";
        public String KICK_BIG_PING = "%prefix%%nl%%nl%&c你的延迟过高,请切换网络后重试";
        @Comment(
            {
            "标题%nl%副标题", "您可以留空以禁用标题 (例如: CHECKING_TITLE = \"\") ",
            "禁用标题可能会略微提高性能"
            })
        public String CHECKING_TITLE = "&r&lBot&b&lFilter%nl%&a检查中...";
        public String CHECKING_TITLE_SUS = "&r验证通过%nl%&a祝您游戏愉快";
        public String CHECKING_TITLE_CAPTCHA = " %nl%&r请在聊天栏中输入验证码!";
    }

    @Comment("是否启用 GeoIp 检查")
    public static class GEO_IP
    {

        @Comment(
            {
            "在什么时候启用该检查?",
            "0 - 总是",
            "1 - 只在被假人攻击时",
            "2 - 禁用"
            })
        public int MODE = 1;
        @Comment(
            {
            "GeoIp的工作模式",
            "0 - 白名单模式(只有在ALLOWED_COUNTRIES列表中的国家才能进入)",
            "1 - 黑名单模式(只有不在ALLOWED_COUNTRIES列表中的国家才能进入)"
            })
        public int TYPE = 0;
        @Comment(
            {
            "从哪里下载GeoIp数据库?",
            "如果该链接因为某些原因无法下载, 请更改它",
            "文件必须以 .mmdb 结尾或打包成 .tar.gz"
            })
        public String NEW_GEOIP_DOWNLOAD_URL = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=%license_key%&suffix=tar.gz";
        @Comment(
            {
            "如果密钥失效, 你需要在 https://www.maxmind.com/ 注册一个账号",
            "并在 https://www.maxmind.com/en/accounts/current/license-key 生成一个新的密钥"
            })
        public String MAXMIND_LICENSE_KEY = "P5g0fVdAQIq8yQau";
        @Comment("国家代码列表, 请参考 https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2")
        public List<String> ALLOWED_COUNTRIES = Arrays.asList( "RU", "UA", "BY", "KZ", "EE", "MD", "KG", "AZ", "LT", "LV", "GE", "PL" );
    }

    @Comment("启用或禁用高延迟检查")
    public static class PING_CHECK
    {

        @Comment(
            {
            "在什么时候启用该检查?",
            "0 - 总是",
            "1 - 只在被假人攻击时",
            "2 - 禁用"
            })
        public int MODE = 1;
        @Comment("最大延迟")
        public int MAX_PING = 350;
    }

    @Comment("启用或禁用直接连接检查")
    public static class SERVER_PING_CHECK
    {

        @Comment(
            {
            "在什么时候启用该检查?",
            "0 - 总是",
            "1 - 只在被假人攻击时",
            "2 - 禁用",
            "由于在面对强力攻击时不稳定, 此检查默认禁用"
            })
        public int MODE = 2;
        @Comment("客户端接收到服务器MOTD后允许多少秒内进入服务器")
        public int CACHE_TIME = 12;
        public List<String> KICK_MESSAGE = new ArrayList()
        {
            {
                add( "%nl%" );
                add( "%nl%" );
                add( "&c你已被踢出! 请不要使用直接连接" );
                add( "%nl%" );
                add( "%nl%" );
                add( "&b要进入服务器:" );
                add( "%nl%" );
                add( "&71) &r将服务器添加到 &l服务器列表中." );
                add( "%nl%" );
                add( "&l我们的IP &8>> &b&lIP" );
                add( "%nl%" );
                add( "%nl%" );
                add( "&72) &r刷新服务器列表. " );
                add( "%nl%" );
                add( "&o要刷新服务器列表, 请点击 &c&l刷新&r 按钮" );
                add( "%nl%" );
                add( "%nl%" );
                add( "&73) &r等待 &c1-3&r 秒后进入服务器!" );

            }
        };
    }

    @Comment(
        {
        "设置反假人保护的操作模式",
        "0 - 验证码检查",
        "1 - 重力检查 + 验证码检查",
        "2 - 重力检查, 如果验证失败再进行验证码检查"
        })
    public static class PROTECTION
    {

        @Comment("无攻击时的操作模式")
        public int NORMAL = 2;
        @Comment("被攻击时的操作模式")
        public int ON_ATTACK = 1;
        @Comment(
            {
            "是否总是检查玩家?",
            "启用此功能时, 请不要忘记将 protection-threshold 的值设置得更高"
            })
        public boolean ALWAYS_CHECK = false;

        @Comment(
            {
            "检查 ip 为 127.0.0.1 的玩家?", "使用 Geyser 时可能会有用",
            "0 - 检查", "1 - 不检查", "2 - 总是检查"
            })
        public int CHECK_LOCALHOST = 0;

        @Comment("禁用 Geyser-standalone 在无攻击时的检查? auth-type必须设置为 floodgate")
        public boolean SKIP_GEYSER = false;
        /*
        @Comment(
                {
                    "Когда работают дополнительные проверки по протоколу",
                    "    (Пакеты на которые клиент должен всегда отвечать)",
                    "0 - Всегда",
                    "1 - Только во время бот атаки",
                    "2 - Отключить"
                })
        public int ADDITIONAL_CHECKS = 1;
         */
    }

    @Comment("数据库设置")
    public static class SQL
    {

        @Comment("数据库类型 sqlite 或 mysql")
        public String STORAGE_TYPE = "sqlite";
        @Comment("从数据库内删除多少天内通过验证且没有再登录过的玩家. 将该值设置为0或更小的值以禁用该功能")
        public int PURGE_TIME = 14;
        @Comment("mysql设置")
        public String HOSTNAME = "127.0.0.1";
        public int PORT = 3306;
        public String USER = "user";
        public String PASSWORD = "password";
        public String DATABASE = "database";
        @Comment("多久内同步一次数据库? 以毫秒为单位. 在使用多个BungeeCord时可能会有用")
        public int SYNC_INTERVAL = -1;
    }

    @Comment("虚拟世界设置")
    public static class DIMENSIONS
    {
        @Comment(
            {
            "使用哪个世界维度?",
            "0 - 主世界",
            "1 - 下界",
            "2 - 末地"
            })
        public int TYPE = 0;
    }
}
