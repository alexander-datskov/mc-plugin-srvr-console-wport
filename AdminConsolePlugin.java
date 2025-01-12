package com.yourname.adminconsoleplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AdminConsolePlugin extends JavaPlugin {
    private Server webServer;
    private Set<Player> pendingRequests;

    @Override
    public void onEnable() {
        getLogger().info("AdminConsolePlugin has been enabled.");
        pendingRequests = new HashSet<>();
        
        // Start the web server
        startWebServer();
    }

    @Override
    public void onDisable() {
        getLogger().info("AdminConsolePlugin has been disabled.");
        
        // Stop the web server
        stopWebServer();
    }

    private void startWebServer() {
        webServer = new Server(9654);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        webServer.setHandler(context);

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/html");
                resp.setStatus(HttpServletResponse.SC_OK);
                StringBuilder response = new StringBuilder("<html><body><h1>Console Access Requests</h1><ul>");
                for (Player player : pendingRequests) {
                    response.append("<li>").append(player.getName())
                            .append(" <a href=\"/approve?player=").append(player.getName()).append("\">Approve</a>")
                            .append(" <a href=\"/deny?player=").append(player.getName()).append("\">Deny</a></li>");
                }
                response.append("</ul></body></html>");
                resp.getWriter().println(response.toString());
            }
        }), "/*");

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                String playerName = req.getParameter("player");
                if (playerName != null) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        pendingRequests.remove(player);
                        player.sendMessage("Your console access request has been approved.");
                    }
                }
                resp.sendRedirect("/");
            }
        }), "/approve");

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                String playerName = req.getParameter("player");
                if (playerName != null) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        pendingRequests.remove(player);
                        player.sendMessage("Your console access request has been denied.");
                    }
                }
                resp.sendRedirect("/");
            }
        }), "/deny");

        try {
            webServer.start();
        } catch (Exception e) {
            getLogger().severe("Failed to start web server: " + e.getMessage());
        }
    }

    private void stopWebServer() {
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception e) {
                getLogger().severe("Failed to stop web server: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("consolecmd")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (sender.hasPermission("adminconsole.request")) {
                    pendingRequests.add(player);
                    player.sendMessage("Your request for console access has been sent to the admins.");
                } else {
                    player.sendMessage("You don't have permission to use this command.");
                }
                return true;
            }
        }
        return false;
    }
}
