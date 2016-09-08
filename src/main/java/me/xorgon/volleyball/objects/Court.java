package me.xorgon.volleyball.objects;

import de.slikey.effectlib.EffectManager;
import me.xorgon.volleyball.VolleyballPlugin;
import me.xorgon.volleyball.effects.BallLandEffect;
import me.xorgon.volleyball.effects.BallTrailEffect;
import me.xorgon.volleyball.effects.RomanCandleEffect;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Court {

    private String name;
    private String displayName;

    private Vector blueMin;
    private Vector blueMax;

    private Vector redMin;
    private Vector redMax;

    private double y;

    private World world;

    private Slime ball;
    private int ballSize = 3;
    private Team lastHitBy;
    private int hitCount;
    public static int MAX_HITS = 3;
    private long lastHitMS;
    public static int HIT_PERIOD_MS = 250;

    private List<Player> redPlayers = new ArrayList<>();
    private List<Player> bluePlayers = new ArrayList<>();

    private int minTeamSize = 1;
    private int maxTeamSize = 6;

    private int redScore = 0;
    private int blueScore = 0;
    public static int MAX_SCORE = 21;

    private Team turn;
    private boolean started;

    public static int START_DELAY_SECS = 15;
    private boolean starting;

    private boolean initialized;

    private BallTrailEffect trailEffect;

    public Court(String name) {
        this.name = name;
        started = false;
        initialized = false;
    }

    public boolean isInCourt(Location location) {
        if (!initialized) {
            return false;
        }
        if (location.getWorld() == world && location.getY() >= y) {
            location.setY(y);
            if (location.toVector().isInAABB(redMin, redMax) || location.toVector().isInAABB(blueMin, blueMax)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public Team getSide(Location location) {
        if (location.toVector().setY(y).isInAABB(redMin, redMax)) {
            return Team.RED;
        } else if (location.toVector().setY(y).isInAABB(blueMin, blueMax)) {
            return Team.BLUE;
        } else {
            return Team.NONE;
        }
    }

    public Team getTeam(Player player) {
        if (redPlayers.contains(player)) {
            return Team.RED;
        } else if (bluePlayers.contains(player)) {
            return Team.BLUE;
        } else {
            return Team.NONE;
        }
    }

    public double getY() {
        return y;
    }

    public void setRed(Vector point1, Vector point2) {
        redMin = Vector.getMinimum(point1, point2);
        redMax = Vector.getMaximum(point1, point2);
        y = redMax.getY();
        isInitialized();
    }

    public void setRed(Location point1, Location point2) {
        redMin = Vector.getMinimum(point1.toVector(), point2.toVector());
        redMax = Vector.getMaximum(point1.toVector(), point2.toVector());
        y = redMax.getY();
        setWorld(point1.getWorld());
        isInitialized();
    }

    public void setBlue(Vector point1, Vector point2) {
        blueMin = Vector.getMinimum(point1, point2);
        blueMax = Vector.getMaximum(point1, point2);
        isInitialized();
    }

    public void setBlue(Location point1, Location point2) {
        blueMin = Vector.getMinimum(point1.toVector(), point2.toVector());
        blueMax = Vector.getMaximum(point1.toVector(), point2.toVector());
        setWorld(point1.getWorld());
        isInitialized();
    }

    public void setWorld(World world) {
        this.world = world;
        isInitialized();
    }

    public Slime getBall() {
        return ball;
    }

    public double getPower() {
        double length;

        double first = blueMin.distance(redMax);
        double second = redMin.distance(blueMax);
        if (first > second) {
            double dx = Math.abs(redMax.getX() - blueMin.getX());
            double dz = Math.abs(redMax.getZ() - blueMin.getZ());
            if (dx > dz) {
                length = dx;
            } else {
                length = dz;
            }
        } else {
            double dx = Math.abs(blueMax.getX() - redMin.getX());
            double dz = Math.abs(blueMax.getZ() - redMin.getZ());
            if (dx > dz) {
                length = dx;
            } else {
                length = dz;
            }
        }
        return Math.pow(length, 0.75) / 16.792;
    }

    public void spawnBall(Location loc) {
        if (this.ball != null) {
            removeBall();
        }
        Slime ball = (Slime) loc.getWorld().spawnEntity(loc, EntityType.SLIME);
        ball.setSize(ballSize);
        ball.setAI(false);
        ball.setGravity(false);
        this.ball = ball;
        trailEffect = new BallTrailEffect(VolleyballPlugin.getInstance().getEffectManager(), this);
        trailEffect.start();
    }

    public void removeBall() {
        if (ball != null) {
            this.ball.remove();
            this.ball = null;
            trailEffect.cancel();
        }
    }

    public boolean isBall(Entity entity) {
        if (entity instanceof Slime) {
            return entity == ball;
        } else {
            return false;
        }
    }

    public List<Player> getRedPlayers() {
        return redPlayers;
    }

    public List<Player> getInRedBox() {
        List<Player> players = new ArrayList<>();
        world.getPlayers().forEach(p -> {
            if (getSide(p.getLocation()) == Team.RED) {
                players.add(p);
            }
        });
        return players;
    }

    public List<Player> getBluePlayers() {
        return bluePlayers;
    }

    public List<Player> getInBlueBox() {
        List<Player> players = new ArrayList<>();
        world.getPlayers().forEach(p -> {
            if (getSide(p.getLocation()) == Team.BLUE) {
                players.add(p);
            }
        });
        return players;
    }

    public void addPoint(Team team) {
        if (team == Team.RED) {
            redScore += 1;
        } else if (team == Team.BLUE) {
            blueScore += 1;
        }
    }

    public Team getLastHitBy() {
        return lastHitBy;
    }

    public void setLastHitBy(Team lastHitBy) {
        this.lastHitBy = lastHitBy;
    }

    public int getRedScore() {
        return redScore;
    }

    public void setRedScore(int redScore) {
        this.redScore = redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public void setBlueScore(int blueScore) {
        this.blueScore = blueScore;
    }

    public void serve() {
        Location servePoint;
        if (turn == Team.RED) {
            servePoint = redMin.getMidpoint(redMax).add(new Vector(0, 2.25, 0)).toLocation(world);
            turn = Team.BLUE;
        } else {
            servePoint = blueMin.getMidpoint(blueMax).add(new Vector(0, 2.25, 0)).toLocation(world);
            turn = Team.RED;
        }
        spawnBall(servePoint);
    }

    public boolean isFinished() {
        return (redScore >= MAX_SCORE || blueScore >= MAX_SCORE);
    }

    public Team getWinning() {
        if (redScore > blueScore) {
            return Team.RED;
        } else if (blueScore > redScore) {
            return Team.BLUE;
        } else {
            return Team.NONE;
        }
    }

    public void endGame() {
        removeBall();
        String message;
        if (getWinning() == Team.RED) {
            message = ChatColor.RED + "Red team wins! Congratulations.";
        } else if (getWinning() == Team.BLUE) {
            message = ChatColor.BLUE + "Blue team wins! Congratulations.";
        } else {
            message = ChatColor.YELLOW + "It's a draw!";
        }
        sendAllPlayersMessage(message);
        fireworks(getWinning());
        redPlayers = new ArrayList<>();
        bluePlayers = new ArrayList<>();
        if (started) {
            started = false;
        }
    }

    // Only use when messages and fireworks are otherwise handled (e.g. forfeits).
    public void endGame(Team winner) {
        removeBall();
        fireworks(winner);
        redPlayers = new ArrayList<>();
        bluePlayers = new ArrayList<>();
        if (started) {
            started = false;
        }
    }

    public void ballLanded() {
        if (isFinished()) {
            endGame();
        } else {
            serve();
        }
    }

    public void sendRedPlayersMessage(String message) {
        redPlayers.forEach(player -> player.sendMessage(message));
    }

    public void sendBluePlayersMessage(String message) {
        bluePlayers.forEach(player -> player.sendMessage(message));
    }

    public void sendAllPlayersMessage(String message) {
        List<Player> players = getAllPlayers();
        players.forEach(p -> p.sendMessage(message));
    }

    public void startGame(boolean force) {
        if (!force && started) {
            return;
        }

        getAllPlayers().stream().filter(p -> !isInCourt(p.getLocation())).forEach(p -> {
            removePlayer(p);
            p.sendMessage(ChatColor.YELLOW + "You left the court before the game started.");
        });

        if (!hasEnoughPlayers() && !force) {
            sendAllPlayersMessage(ChatColor.YELLOW + "Not enough players to start.");
            starting = false;
            return;
        }

        sendRedPlayersMessage(ChatColor.YELLOW + "Game started, you're on " + ChatColor.RED + "red" + ChatColor.YELLOW + " team.");
        sendBluePlayersMessage(ChatColor.YELLOW + "Game started, you're on " + ChatColor.BLUE + "blue" + ChatColor.YELLOW + " team.");
        sendAllPlayersMessage(ChatColor.YELLOW + "Playing to " + ChatColor.LIGHT_PURPLE + MAX_SCORE);

        turn = Team.RED;
        redScore = 0;
        blueScore = 0;
        started = true;
        starting = false;

        serve();
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isInitialized() {
        boolean b = redMin != null && redMax != null && blueMin != null && blueMax != null && world != null;
        initialized = b;
        return b;
    }

    public Vector getBlueMin() {
        return blueMin;
    }

    public Vector getBlueMax() {
        return blueMax;
    }

    public Vector getRedMin() {
        return redMin;
    }

    public Vector getRedMax() {
        return redMax;
    }

    public World getWorld() {
        return world;
    }

    public int getMinTeamSize() {
        return minTeamSize;
    }

    public void setMinTeamSize(int minTeamSize) {
        this.minTeamSize = minTeamSize;
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isStarting() {
        return starting;
    }

    public void setStarting(boolean starting) {
        this.starting = starting;
    }

    public void incHitCount() {
        hitCount++;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void resetHitCount() {
        hitCount = 0;
    }

    public boolean canHit() {
        long now = new Date().getTime();
        if (now - lastHitMS < HIT_PERIOD_MS) {
            return false;
        } else {
            lastHitMS = now;
            return true;
        }
    }

    public void score(Team scoringTeam) {

        addPoint(scoringTeam);

        hitCount = 0;

        String message = (scoringTeam == Court.Team.RED ? ChatColor.RED + "Red " : ChatColor.BLUE + "Blue ")
                + ChatColor.YELLOW + "team scored!";
        String score = ChatColor.RED + "Red " + getRedScore() + ChatColor.YELLOW
                + " - " + ChatColor.BLUE + getBlueScore() + " Blue";

        sendAllPlayersMessage(message);
        sendAllPlayersMessage(score);

        boolean redMP = getRedScore() == Court.MAX_SCORE - 1 && getBlueScore() < MAX_SCORE;
        boolean blueMP = getBlueScore() == Court.MAX_SCORE - 1 && getRedScore() < MAX_SCORE;
        if (redMP && blueMP) {
            sendAllPlayersMessage(ChatColor.YELLOW + "Double match point!");
        } else if (redMP) {
            sendAllPlayersMessage(ChatColor.RED + "Red " + ChatColor.YELLOW + "match point!");
        } else if (blueMP) {
            sendAllPlayersMessage(ChatColor.BLUE + "Blue " + ChatColor.YELLOW + "match point!");
        }

        BallLandEffect effect = new BallLandEffect(VolleyballPlugin.getInstance().getEffectManager(), this, scoringTeam);
        effect.callback = this::ballLanded;
        effect.start();

        removeBall();
    }

    public boolean hasEnoughPlayers() {
        int redSize = getInRedBox().size();
        int blueSize = getInBlueBox().size();
        return (redSize >= minTeamSize && redSize <= maxTeamSize && blueSize >= minTeamSize && blueSize <= maxTeamSize);
    }

    public int getBallSize() {
        return ballSize;
    }

    public void setBallSize(int ballSize) {
        this.ballSize = ballSize;
    }

    public String getName() {
        return name;
    }

    public void fireworks(Team team) {
        Vector c1;
        Vector c2;
        Vector c3;
        Vector c4;
        Color color;
        if (team == Team.RED) {
            c1 = redMax;
            c2 = new Vector(redMax.getX(), y, redMin.getZ());
            c3 = redMin;
            c4 = new Vector(redMin.getX(), y, redMax.getZ());
            color = Color.RED;
        } else if (team == Team.BLUE) {
            c1 = blueMax;
            c2 = new Vector(blueMax.getX(), y, blueMin.getZ());
            c3 = blueMin;
            c4 = new Vector(blueMin.getX(), y, blueMax.getZ());
            color = Color.BLUE;
        } else {
            if (redMax.distance(blueMin) > blueMax.distance(redMin)) {
                c1 = redMax;
                c2 = new Vector(redMax.getX(), y, blueMin.getZ());
                c3 = blueMin;
                c4 = new Vector(blueMin.getX(), y, redMax.getZ());
            } else {
                c1 = blueMax;
                c2 = new Vector(blueMax.getX(), y, redMin.getZ());
                c3 = redMin;
                c4 = new Vector(redMin.getX(), y, blueMax.getZ());
            }
            color = Color.PURPLE;
        }

        EffectManager eM = VolleyballPlugin.getInstance().getEffectManager();

        int fPerSide = 5;
        double height = 8.0;
        Location loc;
        for (int i = 0; i < fPerSide; i++) {
            double dist = ((double) i) / (fPerSide - 1); // Distance along side
            loc = c1.clone().add(c2.clone().subtract(c1).multiply(dist)).toLocation(world);
            new RomanCandleEffect(eM, loc, color, height).start();
            loc = c2.clone().add(c3.clone().subtract(c2).multiply(dist)).toLocation(world);
            new RomanCandleEffect(eM, loc, color, height).start();
            loc = c3.clone().add(c4.clone().subtract(c3).multiply(dist)).toLocation(world);
            new RomanCandleEffect(eM, loc, color, height).start();
            loc = c4.clone().add(c1.clone().subtract(c4).multiply(dist)).toLocation(world);
            new RomanCandleEffect(eM, loc, color, height).start();
        }
    }

    public void removePlayer(Player player) {
        if (redPlayers.contains(player)) {
            redPlayers.remove(player);
        } else if (bluePlayers.contains(player)) {
            bluePlayers.remove(player);
        }
    }

    public void addPlayer(Player player, Team team) {
        if (team == Team.RED) {
            redPlayers.add(player);
        } else if (team == Team.BLUE) {
            bluePlayers.add(player);
        }
    }

    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        players.addAll(redPlayers);
        players.addAll(bluePlayers);
        return players;
    }

    public enum Team {
        RED, BLUE, NONE
    }

}