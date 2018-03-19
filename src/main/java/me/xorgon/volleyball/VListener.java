package me.xorgon.volleyball;

import com.supaham.commons.bukkit.text.FancyMessage;
import me.xorgon.volleyball.events.BallLandEvent;
import me.xorgon.volleyball.objects.Court;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class VListener implements Listener {

    VolleyballPlugin plugin = VolleyballPlugin.getInstance();
    VManager manager = plugin.getManager();

    @EventHandler
    public void onSlimeHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && manager.isVolleyball(event.getEntity())) {
            event.setCancelled(true);
            Slime ball = (Slime) event.getEntity();
            Player player = (Player) event.getDamager();
            Court court = manager.getCourt(ball);
            if (court.getTeam(player) != Court.Team.NONE) {
                if (court.canHit()) {
                    Court.Team ballSide = court.getSide(ball.getLocation());
                    if (ballSide != court.getTeam(player) && ballSide != Court.Team.NONE) {
                        player.sendMessage(manager.messages.getWrongSideMessage());
                        return;
                    }
                    if (court.getLastHitBy() == court.getTeam(player)) {
                        if (court.getHitCount() >= Court.MAX_HITS) {
                            player.sendMessage(manager.messages.getTooManyHitsMessage());
                            return;
                        }
                    } else {
                        court.resetHitCount();
                    }
                    Vector dir = player.getLocation().getDirection();
                    if (!ball.hasGravity()) {
                        ball.setGravity(true);
                    }
                    double factor = 0.5;
                    if (player.isSprinting()) {
                        factor += 0.5;
                    }
                    if (!player.isOnGround()) {
                        factor += 0.5;
                    }
                    factor *= court.getPower();
                    ball.setVelocity(dir.clone().multiply(2.7).setY(dir.getY() + 1.1).multiply(factor));
                    court.setLastHitBy(court.getTeam(player));
                    court.incHitCount();
                }
            }
        }
    }

    @EventHandler
    public void onSlimeDamage(EntityDamageEvent event) {
        if (manager.isVolleyball(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBallLand(BallLandEvent event) {
        event.getCourt().score(event.getScoringTeam());
    }


    private List<Player> teamFullSent = new ArrayList<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (manager.isInCourt(player)) {
            Court court = manager.getCourt(player);
            if (court.isStarted() && court.getTeam(player) == Court.Team.NONE
                    || !player.hasPermission("vb.user")) {
                Vector centerToPlayer = player.getLocation().toVector().subtract(court.getCenter().toVector()).clone();
                player.setVelocity(centerToPlayer.setY(0).normalize().setY(1));

                if (!manager.isBouncedPlayer(player)) {
                    if (!player.hasPermission("vb.user")) {
                        player.sendMessage(manager.messages.getNoPermissionsMessage());
                    } else {
                        player.sendMessage(manager.messages.getMatchStartedMessage());
                    }

                    manager.addBouncedPlayer(player);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> manager.removeBouncedPlayer(player), 100);
                }
                return;
            }
            if (court.isStarted()) {
                return;
            }
            FancyMessage helpMsg = new FancyMessage();
            String help = manager.messages.getClickForHelpMessage();
            helpMsg.text(help)
                    .command("/vb help")
                    .tooltip(help)
                    .then();
            if (court.getSide(player.getLocation()) == Court.Team.RED) {
                if (court.getRedPlayers().size() < court.getMaxTeamSize()) {
                    if (!court.getRedPlayers().contains(player)) {
                        if (court.getBluePlayers().contains(player)) {
                            court.removePlayer(player);
                        }
                        court.addPlayer(player, Court.Team.RED);
                        FancyMessage msg = new FancyMessage(manager.messages.getJoinedTeamMessage(Court.Team.RED));
                        msg.send(player);
                        helpMsg.send(player);
                    }
                } else {
                    if (!teamFullSent.contains(player)) {
                        player.sendMessage(manager.messages.getFullTeamMessage(Court.Team.RED));
                        teamFullSent.add(player);
                    }
                }
            } else {
                if (court.getBluePlayers().size() < court.getMaxTeamSize()) {
                    if (!court.getBluePlayers().contains(player)) {
                        if (court.getRedPlayers().contains(player)) {
                            court.removePlayer(player);
                        }
                        court.addPlayer(player, Court.Team.BLUE);
                        FancyMessage msg = new FancyMessage(manager.messages.getJoinedTeamMessage(Court.Team.BLUE));
                        msg.send(player);
                        helpMsg.send(player);
                    }
                } else {
                    if (!teamFullSent.contains(player)) {
                        player.sendMessage(manager.messages.getFullTeamMessage(Court.Team.BLUE));
                        teamFullSent.add(player);
                    }
                }
            }
            if (!court.isStarting()) {

                String alertMsg;
                if (court.getDisplayName() != null) {
                    alertMsg = manager.messages.getMatchStartingWithNameMessage(court);
                } else {
                    alertMsg = manager.messages.getMatchStartingWithoutNameMessage();
                }
                Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("vb.user"))
                        .filter(p -> !manager.isPlaying(p) && !manager.getCourt(p).isStarted() && manager.getCourt(p) != court)
                        .forEach(p -> p.sendMessage(alertMsg));

                FancyMessage joinMsg = new FancyMessage()
                        .text(manager.messages.getClickToJoinMessage())
                        .command("/vb join " + court.getName())
                        .tooltip(manager.messages.getClickToJoinMessage());
                Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("vb.tp"))
                        .filter(p -> !manager.isPlaying(p) && manager.getCourt(p).isStarted() && manager.getCourt(p) != court)
                        .forEach(p -> joinMsg.send(p));
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> court.startGame(false), Court.START_DELAY_SECS * 20);
                court.setStarting(true);
            }
        } else if (manager.isPlaying(player)) {
            Court court = manager.getPlayingIn(player);
            if (!court.isStarted()) {
                player.sendMessage(manager.messages.getLeftGameMessage());
                court.removePlayer(player);
            }
        } else if (teamFullSent.contains(player)) {
            teamFullSent.remove(player);
        }
    }

    @EventHandler
    public void onPlayerDamagedByBall(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Slime) {
            if (manager.isVolleyball(event.getDamager())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        // Cancel the event if player is playing.
        if (manager.isPlaying(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (manager.isPlaying(event.getPlayer())) {
            manager.getPlayingIn(event.getPlayer()).removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onServerShutdown(WorldUnloadEvent event) {
        manager.getCourts().values().forEach(Court::resetCourt);
    }
}
