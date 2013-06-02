package net.md_5.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class CommandWhois extends Command
{

    public CommandWhois()
    {
        super( "whois", "bungeecord.command.whois" );
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if ( args.length != 1 )
        {
            sender.sendMessage( ChatColor.RED + "Please follow this command by a user name" );
        } else
        {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer( args[0] );
            if ( player == null || player.getServer() == null )
            {
                sender.sendMessage( ChatColor.RED + "That user is not online" );
            } else
            {
                sender.sendMessage( ChatColor.BLUE + args[0] + " is online at " + player.getServer().getInfo().getName() );
            }
        }
    }
}