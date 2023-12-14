package ru.leymooo.botfilter.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.SneakyThrows;
import net.md_5.bungee.BungeeCord;
import ru.leymooo.botfilter.BotFilter;
import ru.leymooo.botfilter.BotFilterUser;
import ru.leymooo.botfilter.config.Settings;
import ru.leymooo.botfilter.config.Settings.SQL;

/**
 * @author Leymooo
 */
public class Sql
{

    private final BotFilter botFilter;
    private Connection connection;
    private boolean connecting = false;
    private long lastSync = System.currentTimeMillis();

    private final ScheduledExecutorService executor
        = Executors.newSingleThreadScheduledExecutor( new ThreadFactoryBuilder().setNameFormat( "BotFilter-SQL-thread" ).build() );
    private final Logger logger = BungeeCord.getInstance().getLogger();

    public Sql(BotFilter botFilter)
    {
        this.botFilter = botFilter;
        setupConnect();
    }

    @SneakyThrows
    private void setupConnect()
    {

        try
        {
            if ( executor.isShutdown() || connecting )
            {
                return;
            }
            connecting = true;
            if ( connection != null && connection.isValid( 1 ) )
            {
                return;
            }
            this.connection = null;
            logger.info( "[BotFilter] 正在连接到据库..." );
            long start = System.currentTimeMillis();
            if ( Settings.IMP.SQL.STORAGE_TYPE.equalsIgnoreCase( "mysql" ) )
            {
                SQL s = Settings.IMP.SQL;
                connectToDatabase( String.format( "JDBC:mysql://%s:%s/%s?useSSL=false&useUnicode=true&characterEncoding=utf-8", s.HOSTNAME, s.PORT, s.DATABASE ), s.USER, s.PASSWORD );
            } else
            {
                Class.forName( "org.sqlite.JDBC" );
                connectToDatabase( "JDBC:sqlite:BotFilter/database.db", null, null );
            }
            logger.log( Level.INFO, "[BotFilter] 数据库已连接 ({0} ms)", System.currentTimeMillis() - start );
            createTable();
            alterLastJoinColumn();
            clearOldUsers();
            loadUsers();
            startTasks();
        } catch ( SQLException | ClassNotFoundException e )
        {
            executor.schedule( this::setupConnect, 5, TimeUnit.SECONDS );
            logger.log( Level.WARNING, "无法连接到数据库或执行sql: ", e );
            if ( connection != null )
            {
                Connection conn = connection;
                this.connection = null;
                conn.close();
            }
        } finally
        {
            connecting = false;
        }
    }


    private void connectToDatabase(String url, String user, String password) throws SQLException
    {
        this.connection = DriverManager.getConnection( url, user, password );
    }

    private void createTable() throws SQLException
    {
        String sql = "CREATE TABLE IF NOT EXISTS `Users` ("
            + "`Name` VARCHAR(16) NOT NULL PRIMARY KEY UNIQUE,"
            + "`Ip` VARCHAR(16) NOT NULL,"
            + "`LastCheck` BIGINT NOT NULL,"
            + "`LastJoin` BIGINT NOT NULL);";

        try ( PreparedStatement statement = connection.prepareStatement( sql ) )
        {
            statement.executeUpdate();
        }
    }

    private void alterLastJoinColumn()
    {
        try ( ResultSet rs = connection.getMetaData().getColumns( null, null, "Users", "LastJoin" ) )
        {
            if ( !rs.next() )
            {
                try ( Statement st = connection.createStatement() )
                {
                    st.executeUpdate( "ALTER TABLE `Users` ADD COLUMN `LastJoin` BIGINT NOT NULL DEFAULT 0;" );
                    st.executeUpdate( "UPDATE `Users` SET LastJoin = LastCheck" );
                }
            }
        } catch ( Exception e )
        {
            logger.log( Level.WARNING, "[BotFilter] 向数据库中添加列时出错", e );
        }
    }

    private void clearOldUsers() throws SQLException
    {
        if ( Settings.IMP.SQL.PURGE_TIME <= 0 )
        {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.DATE, -Settings.IMP.SQL.PURGE_TIME );
        long until = calendar.getTimeInMillis();
        int before = botFilter.getUsersCount();
        botFilter.getUserCache().entrySet().removeIf( (entry) -> entry.getValue().getLastJoin() < until );
        if ( ( before - botFilter.getUsersCount() ) > 0 )
        {
            logger.log( Level.INFO, "[BotFilter] 从内存中删除了 {0} 个账号", before - botFilter.getUsersCount() );
        }
        if ( this.connection != null )
        {
            try ( PreparedStatement statement = connection.prepareStatement( "DELETE FROM `Users` WHERE `LastJoin` < " + until + ";" ) )
            {
                int removed = statement.executeUpdate();
                if ( removed > 0 )
                {
                    logger.log( Level.INFO, "[BotFilter] 从数据库中删除了 {0} 个账号", removed );
                }
            }
        }
    }


    private void startTasks()
    {

        if ( Settings.IMP.SQL.PURGE_TIME > 0 )
        {
            executor.scheduleAtFixedRate( this::tryCleanUP, 1, 1, TimeUnit.MINUTES );
        }
        if ( Settings.IMP.SQL.SYNC_INTERVAL > 0 )
        {
            executor.scheduleAtFixedRate( this::syncUsers, Settings.IMP.SQL.SYNC_INTERVAL, Settings.IMP.SQL.SYNC_INTERVAL, TimeUnit.MILLISECONDS );
        }
    }

    private void syncUsers()
    {
        if ( connecting || connection == null )
        {
            return;
        }
        long curr = System.currentTimeMillis();
        try ( PreparedStatement statament = connection.prepareStatement( "SELECT * FROM `Users` WHERE `LastJoin` > " + lastSync + ";" );
              ResultSet set = statament.executeQuery() )
        {
            int synced = 0;
            while ( set.next() )
            {
                String name = set.getString( "Name" );
                String ip = set.getString( "Ip" );
                long lastCheck = set.getLong( "LastCheck" );
                long lastJoin = set.getLong( "LastJoin" );

                botFilter.removeUser( name );
                BotFilterUser botFilterUser = new BotFilterUser( name, ip, lastCheck, lastJoin );
                botFilter.addUserToCache( botFilterUser );
                synced++;
            }
            if ( synced > 0 )
            {
                logger.log( Level.INFO, "[BotFilter] 已同步 ({0}) 个检查", synced );
            }
            lastSync = curr;
        } catch ( Exception e )
        {
            logger.log( Level.WARNING, "[BotFilter] 无法同步检查", e );
            setupConnect();
        }
    }

    private void loadUsers() throws SQLException
    {
        try ( PreparedStatement statament = connection.prepareStatement( "SELECT * FROM `Users`;" );
              ResultSet set = statament.executeQuery() )
        {
            int i = 0;
            while ( set.next() )
            {
                String name = set.getString( "Name" );
                String ip = set.getString( "Ip" );
                if ( isInvalidName( name ) )
                {
                    removeUser( "REMOVE FROM `Users` WHERE `Ip` = '" + ip + "' AND `LastCheck` = '" + set.getLong( "LastCheck" ) + "';" );
                    continue;
                }
                long lastCheck = set.getLong( "LastCheck" );
                long lastJoin = set.getLong( "LastJoin" );
                BotFilterUser botFilterUser = new BotFilterUser( name, ip, lastCheck, lastJoin );
                botFilter.addUserToCache( botFilterUser );
                i++;
            }
            logger.log( Level.INFO, "[BotFilter] 白名单已加载 ({0})", i );
        }
    }

    private boolean isInvalidName(String name)
    {
        return name.contains( "'" ) || name.contains( "\"" );
    }

    private void removeUser(String sql)
    {
        if ( connection != null )
        {
            this.executor.execute( () ->
            {
                try ( PreparedStatement statament = connection.prepareStatement( sql ) )
                {
                    statament.execute();
                } catch ( SQLException ignored )
                {
                }
            } );
        }
    }

    public void saveUser(BotFilterUser botFilterUser)
    {
        if ( connecting || isInvalidName( botFilterUser.getName() ) )
        {
            return;
        }
        if ( connection != null )
        {
            this.executor.execute( () ->
            {
                final long timestamp = System.currentTimeMillis();
                String sql = "SELECT `Name` FROM `Users` where `Name` = '" + botFilterUser.getName() + "' LIMIT 1;";
                try ( Statement statament = connection.createStatement();
                      ResultSet set = statament.executeQuery( sql ) )
                {
                    if ( !set.next() )
                    {
                        sql = "INSERT INTO `Users` (`Name`, `Ip`, `LastCheck`, `LastJoin`) VALUES "
                            + "('" + botFilterUser.getName() + "','" + botFilterUser.getIp() + "',"
                            + "'" + botFilterUser.getLastCheck() + "','" + botFilterUser.getLastJoin() + "');";
                        statament.executeUpdate( sql );
                    } else
                    {
                        sql = "UPDATE `Users` SET `Ip` = '" + botFilterUser.getIp() + "', `LastCheck` = '" + botFilterUser.getLastCheck() + "',"
                            + " `LastJoin` = '" + botFilterUser.getLastJoin() + "' where `Name` = '" + botFilterUser.getName() + "';";
                        statament.executeUpdate( sql );
                    }
                } catch ( SQLException ex )
                {
                    logger.log( Level.WARNING, "[BotFilter] 无法执行数据库查询", ex );
                    logger.log( Level.WARNING, sql );
                    setupConnect();
                }
            } );
        }
    }

    public void tryCleanUP()
    {
        if ( Settings.IMP.SQL.PURGE_TIME > 0 )
        {
            try
            {
                clearOldUsers();
            } catch ( SQLException ex )
            {
                setupConnect();
                logger.log( Level.WARNING, "[BotFilter] 无法清理玩家列表", ex );
            }

        }
    }

    public void close()
    {
        this.executor.shutdownNow();
        try
        {
            if ( connection != null )
            {
                this.connection.close();
            }
        } catch ( SQLException ignore )
        {
        }
        this.connection = null;
    }
}
