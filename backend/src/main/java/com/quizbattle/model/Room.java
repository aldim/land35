package com.quizbattle.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

public class Room {
    private String code;
    private String hostSessionId;
    private List<Player> players;
    private GameState gameState;
    private String winnerId;
    private Long roundStartTime;
    private List<ButtonPress> buttonPresses;
    private ScheduledFuture<?> winnerDeterminationTask; // Для буферизации
    private long firstPressServerTime; // Время получения первого нажатия на сервере
    private Integer currentChapter; // Текущая глава викторины
    private Integer currentPart; // Текущая часть главы
    
    public static final int MAX_PLAYERS = 20;
    private static final long MIN_BUFFER_WINDOW = 100; // Минимальное окно 100мс
    private static final long MAX_BUFFER_WINDOW = 500; // Максимальное окно 500мс
    
    public Room() {}
    
    public Room(String code, String hostSessionId) {
        this.code = code;
        this.hostSessionId = hostSessionId;
        this.players = new CopyOnWriteArrayList<>();
        this.gameState = GameState.WAITING;
        this.buttonPresses = new CopyOnWriteArrayList<>();
    }
    
    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getHostSessionId() { return hostSessionId; }
    public void setHostSessionId(String hostSessionId) { this.hostSessionId = hostSessionId; }
    
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
    
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    
    public Long getRoundStartTime() { return roundStartTime; }
    public void setRoundStartTime(Long roundStartTime) { this.roundStartTime = roundStartTime; }
    
    public List<ButtonPress> getButtonPresses() { return buttonPresses; }
    public void setButtonPresses(List<ButtonPress> buttonPresses) { this.buttonPresses = buttonPresses; }
    
    public ScheduledFuture<?> getWinnerDeterminationTask() {
        return winnerDeterminationTask;
    }
    
    public void setWinnerDeterminationTask(ScheduledFuture<?> winnerDeterminationTask) {
        this.winnerDeterminationTask = winnerDeterminationTask;
    }
    
    public long getFirstPressServerTime() {
        return firstPressServerTime;
    }
    
    public void setFirstPressServerTime(long firstPressServerTime) {
        this.firstPressServerTime = firstPressServerTime;
    }
    
    public Integer getCurrentChapter() { return currentChapter; }
    public void setCurrentChapter(Integer currentChapter) { this.currentChapter = currentChapter; }
    
    public Integer getCurrentPart() { return currentPart; }
    public void setCurrentPart(Integer currentPart) { this.currentPart = currentPart; }
    
    // Business methods
    public boolean addPlayer(Player player) {
        if (players.size() >= MAX_PLAYERS) {
            return false;
        }
        players.add(player);
        return true;
    }
    
    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
    }
    
    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
    
    public Player getPlayerBySessionId(String sessionId) {
        return players.stream()
                .filter(p -> sessionId.equals(p.getSessionId()))
                .findFirst()
                .orElse(null);
    }
    
    public void startRound() {
        this.gameState = GameState.ACTIVE;
        this.winnerId = null;
        this.roundStartTime = System.currentTimeMillis();
        this.buttonPresses.clear();
        this.firstPressServerTime = 0;
        // Отменяем предыдущую задачу, если она есть
        if (this.winnerDeterminationTask != null && !this.winnerDeterminationTask.isDone()) {
            this.winnerDeterminationTask.cancel(false);
        }
        this.winnerDeterminationTask = null;
    }
    
    public void endRound() {
        this.gameState = GameState.ROUND_ENDED;
    }
    
    public void resetRound() {
        this.gameState = GameState.WAITING;
        this.winnerId = null;
        this.roundStartTime = null;
        this.buttonPresses.clear();
        this.firstPressServerTime = 0;
        // Снимаем оглушение со всех игроков после завершения раунда
        for (Player player : players) {
            player.setStunned(false);
        }
        // Отменяем задачу определения победителя
        if (this.winnerDeterminationTask != null && !this.winnerDeterminationTask.isDone()) {
            this.winnerDeterminationTask.cancel(false);
        }
        this.winnerDeterminationTask = null;
    }
    
    /**
     * Регистрация нажатия кнопки с clientTimestamp
     * Возвращает true, если это первое нажатие (для запуска таймера)
     */
    public synchronized boolean registerButtonPress(String playerId, long clientTimestamp) {
        // Проверяем, не нажимал ли уже этот игрок
        boolean alreadyPressed = buttonPresses.stream()
                .anyMatch(bp -> bp.getPlayerId().equals(playerId));
        
        if (alreadyPressed || gameState != GameState.ACTIVE) {
            return false;
        }
        
        long serverReceiveTime = System.currentTimeMillis();
        int position = buttonPresses.size() + 1;
        
        ButtonPress press = new ButtonPress(playerId, clientTimestamp, position);
        press.setServerReceiveTime(serverReceiveTime); // Сохраняем время получения на сервере
        
        buttonPresses.add(press);
        
        // Если это первое нажатие, запоминаем время получения на сервере
        if (position == 1) {
            this.firstPressServerTime = serverReceiveTime;
            return true; // Возвращаем true для запуска таймера
        }
        
        return false;
    }
    
    /**
     * Определить победителя на основе clientTimestamp
     */
    public synchronized void determineWinner() {
        if (buttonPresses.isEmpty() || gameState != GameState.ACTIVE) {
            return;
        }
        
        // Находим нажатие с минимальным clientTimestamp
        ButtonPress winnerPress = buttonPresses.stream()
                .min((bp1, bp2) -> Long.compare(bp1.getTimestamp(), bp2.getTimestamp()))
                .orElse(null);
        
        if (winnerPress != null) {
            this.winnerId = winnerPress.getPlayerId();
            this.gameState = GameState.ROUND_ENDED;
        }
    }
    
    /**
     * Вычислить адаптивное окно буферизации на основе задержек
     */
    public long calculateBufferWindow() {
        if (buttonPresses.isEmpty()) {
            return MIN_BUFFER_WINDOW;
        }
        
        // Вычисляем максимальную задержку среди всех нажатий
        // Задержка = время получения на сервере - время нажатия на клиенте
        long maxLatency = 0;
        for (ButtonPress press : buttonPresses) {
            // Если clientTimestamp больше serverReceiveTime (расхождение часов),
            // считаем задержку как 0 (или минимальную)
            long latency = Math.max(0, press.getServerReceiveTime() - press.getTimestamp());
            if (latency > maxLatency) {
                maxLatency = latency;
            }
        }
        
        // Окно = max(100ms, maxLatency * 2), но не больше 500ms
        // Умножаем на 2, чтобы дать время для нажатий с большей задержкой
        long window = Math.max(MIN_BUFFER_WINDOW, maxLatency * 2);
        return Math.min(window, MAX_BUFFER_WINDOW);
    }
}
