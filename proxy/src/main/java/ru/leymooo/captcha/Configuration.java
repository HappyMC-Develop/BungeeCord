package ru.leymooo.captcha;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.nashorn.internal.ir.Symbol;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 *
 * @author Leymooo
 */
public class Configuration
{

    private static Configuration conf;
    @Getter
    private String wrongCaptchaKick = "wrong-captcha-kick";
    @Getter
    private String timeOutKick = "timeout-kick";
    @Getter
    private String botKick = "bot-kick";
    @Getter
    private String wrongCaptcha = "wrong-captcha";
    @Getter
    private String enterCaptcha = "enter-captcha";
    @Getter
    private boolean ripple = true;
    @Getter
    private boolean blur = true;
    @Getter
    private boolean outline = false;
    @Getter
    private boolean rotate = true;
    @Getter
    @Setter
    private boolean underAttack = false;
    @Getter
    private boolean capthcaAfterReJoin = false;
    private boolean mySqlEnabled = false;
    @Getter
    private int worldType;
    @Getter
    private int mode = 0;
    @Getter
    private int threads = 4;
    @Getter
    private int timeout = 15;
    @Getter
    private int maxCaptchas = 1500;
    @Getter
    private int underAttackTime = 120000;
    private MySql mysql = null;
    private final HashMap<String, String> users = new HashMap<>();
    @Getter
    private final Set<PacketReciever> connectedUsersSet = Sets.newConcurrentHashSet();
    //=========Такая реализация скорее всего лучше, чем использование thread======//
    private double attactStartTime = 0;
    private double lastBotAttackCheck = System.currentTimeMillis();
    private AtomicInteger botCounter = new AtomicInteger();

    public Configuration()
    {
        conf = this;
        net.md_5.bungee.config.Configuration config = null;
        try
        {
            config = checkFileAndGiveConfig();
            this.load( config );
        } catch ( IOException e )
        {
            e.printStackTrace();
            System.out.println( "vk.com/leymooo_s" );
            try
            {
                Thread.sleep( 3000L );
            } catch ( InterruptedException ex )
            {
            }
            System.exit( 0 );
        }
        if ( mySqlEnabled && !capthcaAfterReJoin )
        {
            this.mysql = new MySql( config.getString( "mysql.host" ), config.getString( "mysql.username" ), config.getString( "mysql.password" ), config.getString( "mysql.database" ) );
        }
        this.startThread();
    }

    public static Configuration getInstance()
    {
        return conf;
    }

    public void addUserToMap(String name, String ip)
    {
        users.put( name.toLowerCase(), ip );
    }

    public void saveIp(String name, String ip)
    {
        if ( capthcaAfterReJoin )
        {
            return;
        }
        if ( mySqlEnabled )
        {
            this.mysql.addAddress( name.toLowerCase(), ip );
        }
        this.addUserToMap( name, ip );
    }

    public boolean needCapthca(String name, String ip)
    {
        if ( this.capthcaAfterReJoin )
        {
            return true;
        }
        //Проверяем включён ли режим 'под атакой'
        if ( System.currentTimeMillis() - attactStartTime < underAttackTime )
        {
            return true;
        }
        //Проверяем что не прошло 5 секунд после последней проверки на бот атаку и проверяем есть ли бот атака.
        if ( ( System.currentTimeMillis() - lastBotAttackCheck <= 5000 ) && botCounter.incrementAndGet() >= 130 )
        {
            attactStartTime = System.currentTimeMillis();
            lastBotAttackCheck = System.currentTimeMillis();
            return true;
        }
        botCounter.incrementAndGet();
        if ( System.currentTimeMillis() - lastBotAttackCheck >= 5000 )
        {
            lastBotAttackCheck = System.currentTimeMillis();
            botCounter.set( 0 );
        }
        if ( !this.users.containsKey( name.toLowerCase() ) )
        {
            return true;
        }
        return !this.users.get( name.toLowerCase() ).equalsIgnoreCase( ip );
    }

    private void load(net.md_5.bungee.config.Configuration config)
    {
        this.wrongCaptchaKick = ChatColor.translateAlternateColorCodes( '&', config.getString( wrongCaptchaKick ) );
        this.timeOutKick = ChatColor.translateAlternateColorCodes( '&', config.getString( timeOutKick ) );
        this.botKick = ChatColor.translateAlternateColorCodes( '&', config.getString( botKick ) );
        this.wrongCaptcha = ChatColor.translateAlternateColorCodes( '&', config.getString( wrongCaptcha ) );
        this.enterCaptcha = ChatColor.translateAlternateColorCodes( '&', config.getString( enterCaptcha ) );
        this.mySqlEnabled = config.getBoolean( "mysql.enabled" );
        this.capthcaAfterReJoin = config.getBoolean( "always-captcha-enter" );
        this.ripple = config.getBoolean( "captcha-generator-settings.outline" );
        this.blur = config.getBoolean( "captcha-generator-settings.blur" );
        this.outline = config.getBoolean( "captcha-generator-settings.outline" );
        this.rotate = config.getBoolean( "captcha-generator-settings.rotate" );
        this.maxCaptchas = config.getInt( "max-captchas" );
        this.mode = config.getInt( "captcha-generator" );
        this.timeout = config.getInt( "max-enter-time" ) * 1000;
        this.underAttackTime = config.getInt( "under-attack-time" ) * 1000;
        this.threads = config.getInt( "image-generation-threads" );
        this.worldType = config.getInt( "world-type" );
    }

    private net.md_5.bungee.config.Configuration checkFileAndGiveConfig() throws IOException
    {
        File file = new File( "captcha.yml" );
        if ( file.exists() )
        {
            return ConfigurationProvider.getProvider( YamlConfiguration.class ).load( file );
        }
        InputStream in = getClass().getClassLoader().getResourceAsStream( ( "captcha.yml" ) );
        Files.copy( in, file.toPath() );
        return ConfigurationProvider.getProvider( YamlConfiguration.class ).load( file );
    }

    private void startThread()
    {
        ( new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                while ( true )
                {
                    try
                    {
                        Thread.sleep( 1000L );
                    } catch ( InterruptedException ex )
                    {
                    }
                    for ( PacketReciever user : getConnectedUsersSet() )
                    {
                        if (user.getPt() == null) {
                            getConnectedUsersSet().remove( user );
                            continue;
                        }
                        if ( user.getPt().isBot() )
                        {
                            user.getUser().kick( Configuration.getInstance().getBotKick() );
                            continue;
                        }
                        if ( System.currentTimeMillis() - user.getJoinTime() >= Configuration.getInstance().getTimeout() )
                        {
                            user.getUser().kick( Configuration.getInstance().getTimeOutKick() );
                            continue;
                        }
                        user.getUser().enterCapthca();
                    }
                }
            }
        }, "Captcha Thread" ) ).start();
    }
}
