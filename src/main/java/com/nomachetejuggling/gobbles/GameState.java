package com.nomachetejuggling.gobbles;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

//TODO: BUGS:
//      * weirdness with teleporters.  On current 0_basic test map, going into cyan from right side, the collision is in the wrong place (outside the box)
//      * teleporter handling is weird.  removing the head of the snake and making a new one?  seems too late, like an ordering issue

//TODO: Map enhancements:
//      * Switches/Doors
//      * Full GUI editor for text files
//      * Map selection argument or cheat key

//TODO: Powerup System:
//      * Points in HUD
//      * Random coins in game (not food, they disappear after a few cycles, maybe 50) earn points
//      * X points puts a powerup on board, disappears after a few cycles, maybe moves around)
//      * Powerups:
//           extra life
//           auto reverse if about to die
//           auto pause if about to die
//           auto turn if about to crash
//           can walk through self (but not obstacles) once
//           shorten snake

//TODO: Graphical enhancements:
//      * pause should blank the screen to avoid slowmo effect (but not on start)
//      * better prompting ("press enter to continue" etc)
//      * glyphs or icons for squares
//      * is it possible to improve frame rate?
//      * statuses (paused, dead) appear centered on grid, not in HUD

//TODO: HUD:
//      * food eaten
//      * current level

//TODO: Later enhancements:
//      * game modes:  EASY (infinite lives per level, lower speed), NORMAL (5 lives per level, normal speed), HARD (3 lifes total, slightly faster speed), INSANE (1 life total, same speed as hard)
//      * high scores and initial keeping
//      * web start for sam
//      * port all of Dave's Nibbles levels

//TODO: Professionalism:
//      * Store types, maybe an enum, for possible spaces.  Map to chars?  Need to flatten data structure, bunch of arrays isn't working. (random piece generation slow)
//      * Optimizations - need to store entire grid indexable by location coord.  logic in Level class.
//      * everything except the rendering should theoretically be unit testable
//      * rendering in separate class


public class GameState {
    private static final int SNAKE_START_LENGTH = 10;
    private static final int SNAKE_MULTIPLIER = 7;
    private static final int FOOD_GOAL = 10;
    private static final int DEFAULT_LIVES = 3;

    private int currentLevelIndex;
    private Level currentLevel;

    private ArrayList<Coordinate> snake;
    private Direction direction;
    private int snakeLength;

    private int foodCount;

    private Coordinate foodLocation;
    private boolean dead;

    private boolean paused;
    private int points;

    private int lives;
    private List<Level> levels = new ArrayList<Level>();

    private ArrayList<Coordinate> validSquares;
    private Random rng;
    ArrayList<Listener<GameState>> listeners;

    public GameState(List<Level> levels) {
        this.rng = new Random();
        this.levels = levels;
        this.listeners = new ArrayList<Listener<GameState>>();

        initGame();
    }

    private void initGame() {
        this.currentLevelIndex = 0;
        this.points = 0;
        this.lives = DEFAULT_LIVES;
        this.dead = false;
        initLevel(currentLevelIndex);
    }

    private void initLevel(int index) {
        this.currentLevel = levels.get(index);

        this.snakeLength = SNAKE_START_LENGTH;
        this.snake = new ArrayList<Coordinate>();


        this.validSquares = new ArrayList<Coordinate>();
        //Init valid squares.  Remove obstacles
        for (int i = 0; i < currentLevel.getWidth(); i++) {
            for (int j = 0; j < currentLevel.getHeight(); j++) {
                validSquares.add(new Coordinate(i, j));
            }
        }

        this.validSquares.removeAll(currentLevel.getObstructionCoordinates());

        Coordinate startPos = placeObject();
        this.snake.add(startPos);
        this.direction = Direction.NONE;
        this.paused = true;
        this.foodLocation = placeObject();

        this.foodCount = 0;
    }

    private Coordinate placeObject() {
        ArrayList<Coordinate> trulyValid = new ArrayList<Coordinate>();
        trulyValid.addAll(validSquares);
        trulyValid.removeAll(snake);
        return trulyValid.get(rng.nextInt(trulyValid.size()));
    }

    private int scaleX(int x) {
        if (x < 0) return currentLevel.getWidth() + x;
        else return x % currentLevel.getWidth();
    }

    private int scaleY(int y) {
        if (y < 0) return currentLevel.getHeight() + y;
        else return y % currentLevel.getHeight();
    }

    public void tick(GameInput gameInput) {
        boolean shouldContinue = handleInputAndContinue(gameInput);
        if (shouldContinue) updateState(gameInput);
        updateListeners();
    }

    private void updateListeners() {
        for (Listener<GameState> listener : listeners) {
            listener.update();
        }
    }

    private void updateState(GameInput gameInput) {
        if (!isPaused()) {
            Coordinate head = snake.get(snake.size() - 1);

            moveSnakeRelativeTo(head);

            //check for collisions with snake, world, or tokens
            boolean hasCollided = hasCollided();

            if(hasCollided) {
                dead = true;
            }

            GameElement collidingElement = currentLevel.getElementAt(head);

            if(collidingElement != null) {

                switch(collidingElement.getType()) {
                    case WALL:
                        dead = true;
                        break;
                    case TELEPORTER:
                        List<Coordinate> teleporters = currentLevel.getMatchingElements(collidingElement);
                        snake.remove(snake.size()-1);
                        teleporters.remove(head);
                        Coordinate teleportLocation = teleporters.get(rng.nextInt(teleporters.size()));
                        moveSnakeRelativeTo(teleportLocation);
                        //Need to recheck for collisions here.  Code smell.
                        break;
                    default:
                        break;
                }
            }

            if (head.equals(foodLocation)) {
                foodLocation = placeObject();
                foodCount++;
                snakeLength = (SNAKE_MULTIPLIER * foodCount) + SNAKE_START_LENGTH;
                points = points + 10000;
                System.out.println("Score: " + foodCount);
                if (foodCount >= FOOD_GOAL && currentLevelIndex != levels.size() - 1) {
                    //If accomplished goal and not on last level (which goes on forever):
                    this.paused = true;
                    this.currentLevelIndex++;
                    gameInput.clear();
                    initLevel(currentLevelIndex);
                }
            } else {
//                this.points = Math.max(points - 10, 0);
            }

            while (snake.size() > snakeLength) {
                snake.remove(0);
            }
        }
    }

    private void moveSnakeRelativeTo(Coordinate head) {
        switch (direction) {
            case UP:
                snake.add(new Coordinate(head.getX(), scaleY(head.getY() - 1)));
                break;
            case DOWN:
                snake.add(new Coordinate(head.getX(), scaleY(head.getY() + 1)));
                break;
            case LEFT:
                snake.add(new Coordinate(scaleX(head.getX() - 1), head.getY()));
                break;
            case RIGHT:
                snake.add(new Coordinate(scaleX(head.getX() + 1), head.getY()));
                break;
        }
    }

    private boolean handleInputAndContinue(GameInput gameInput) {
        //Handle Input
        int inputKey = gameInput.dequeueKey();
        if (!isPaused()) {
            if (inputKey == KeyEvent.VK_SPACE) {
                paused = true;
                direction = Direction.NONE;
                return false;
            }

            if (safeDirectionChange(inputKey)) {
                direction = getDirection(inputKey);
            }
            return true;
        } else if (dead) {
            if (inputKey == KeyEvent.VK_ENTER) {
                dead = false;
                lives = lives - 1;
                if (lives >= 0) {
                    initLevel(currentLevelIndex);
                } else {
                    initGame();
                }
                return false;
            }
        } else { //Resuming
            direction = getDirection(inputKey);
            if (direction != Direction.NONE) paused = false;
            return false;
        }
        return true;
    }

    private Direction getDirection(int key) {
        switch (key) {
            case KeyEvent.VK_UP:
                return Direction.UP;
            case KeyEvent.VK_DOWN:
                return Direction.DOWN;
            case KeyEvent.VK_RIGHT:
                return Direction.RIGHT;
            case KeyEvent.VK_LEFT:
                return Direction.LEFT;
        }
        return direction; //no change
    }

    private boolean safeDirectionChange(int key) {
        if (direction == Direction.DOWN && key == KeyEvent.VK_UP) return false;
        if (direction == Direction.UP && key == KeyEvent.VK_DOWN) return false;
        if (direction == Direction.RIGHT && key == KeyEvent.VK_LEFT)
            return false;
        if (direction == Direction.LEFT && key == KeyEvent.VK_RIGHT)
            return false;

        return true;
    }

    private boolean isPaused() {
        return dead || paused;
    }

    private boolean hasCollided() {
        Coordinate head = snake.get(snake.size() - 1);

        for (int i = 0; i < snake.size() - 1; i++) { //All except the head
            if (snake.get(i).equals(head)) return true;
        }

       return false;
    }

    public BufferedImage renderHud(int width, int height) {
        BufferedImage buff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) buff.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.GRAY);
        g2.fillRect(0, 0, width, height);

        if (paused) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (height * 2) / 3));
            Rectangle2D bounds = g2.getFontMetrics().getStringBounds("Paused", g2);
            g2.drawString("Paused", (int) ((width - bounds.getWidth()) / 2), (int) ((height - bounds.getHeight()) / 2 + g2.getFontMetrics().getHeight() - g2.getFontMetrics().getDescent()));
        }

        if (dead) {
            g2.setColor(Color.RED);
            if (lives > 0) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, height));
                Rectangle2D bounds = g2.getFontMetrics().getStringBounds("Dead!", g2);
                g2.drawString("Dead!", (int) ((width - bounds.getWidth()) / 2), (int) ((height - bounds.getHeight()) / 2 + g2.getFontMetrics().getHeight() - g2.getFontMetrics().getDescent()));
            } else {
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, height));
                Rectangle2D bounds = g2.getFontMetrics().getStringBounds("Game Over", g2);
                g2.drawString("Game Over", (int) ((width - bounds.getWidth()) / 2), (int) ((height - bounds.getHeight()) / 2 + g2.getFontMetrics().getHeight() - g2.getFontMetrics().getDescent()));
            }
        }

        return buff;
    }

    public BufferedImage renderGame(int width, int height) {
        final Color SNAKE_COLOR = Color.WHITE;

        BufferedImage buff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) buff.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //TODO: maybe these offsets should be calculated, then call a renderView or something that makes an image that is
        //painted offset just one time, so all other calls can ignore the offset.
        //Background
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, width, height);

        int scaleFactor;

        if (width / this.currentLevel.getWidth() > height / this.currentLevel.getHeight()) {
            scaleFactor = (int) (height / ((double) this.currentLevel.getHeight()));
        } else {
            scaleFactor = (int) (width / ((double) this.currentLevel.getWidth()));
        }

        int realWidth = scaleFactor * this.currentLevel.getWidth();
        int realHeight = scaleFactor * this.currentLevel.getHeight();

        int offsetWidth = (width - realWidth) / 2;
        int offsetHeight = (height - realHeight) / 2;

        g2.setColor(Color.BLACK);
        g2.fillRect(offsetWidth, offsetHeight, scaleFactor * this.currentLevel.getWidth(), scaleFactor * this.currentLevel.getHeight());
        g2.setColor(SNAKE_COLOR);

        Coordinate head = snake.get(snake.size() - 1);
        for (Coordinate snakeCoord : this.snake) {
            if (snakeCoord.equals(head)) {
                g2.fillRoundRect((snakeCoord.getX() * scaleFactor) + offsetWidth, (snakeCoord.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor, scaleFactor / 2, scaleFactor / 2);
            } else {
                g2.fillRoundRect((snakeCoord.getX() * scaleFactor) + offsetWidth, (snakeCoord.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor, scaleFactor / 4, scaleFactor / 4);
            }
        }


        for (Map.Entry<Coordinate, GameElement> entry : this.currentLevel.getElements().entrySet()) {
            Coordinate coordinate = entry.getKey();
            GameElement obstacle = entry.getValue();
            g2.setColor(obstacle.getColor());
            g2.setStroke(new BasicStroke(1F));
            switch(obstacle.getType()) {
                case WALL:
                    g2.fillRect((coordinate.getX() * scaleFactor) + offsetWidth, (coordinate.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);
                    break;
                case TELEPORTER:
                    g2.setStroke(new BasicStroke(scaleFactor/5F));
                    g2.drawOval((coordinate.getX() * scaleFactor) + offsetWidth, (coordinate.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);
                    break;

            }
        }

//        g2.setColor(new Color(255, 255-(((int)(128*(foodCount/(double)FOOD_GOAL)))), 0, 255)); //Gotta be careful here, at last level theres more than goal counts
        g2.setColor(Color.YELLOW);
        g2.fillOval((foodLocation.getX() * scaleFactor) + offsetWidth, (foodLocation.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);

        if (dead) {
            g2.setColor(Color.RED);
            g2.fillRect((head.getX() * scaleFactor) + offsetWidth, (head.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);
        }

        return buff;
    }


    public void registerListener(Listener<GameState> listener) {
        listeners.add(listener);
    }
}

class Coordinate {
    private final int x;
    private final int y;

    Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate)) return false;

        Coordinate that = (Coordinate) o;

        if (x != that.x) return false;
        if (y != that.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}

enum Direction {
    UP, LEFT, DOWN, RIGHT, NONE
}

class Level {
    private int width;
    private int height;
    private Map<Coordinate, GameElement> elements;

    List<Wall> walls;

    public Level(int width, int height, Map<Coordinate, GameElement> elements) {
        this.width = width;
        this.height = height;
        this.elements = elements;

        walls = new ArrayList<Wall>();
        for (Coordinate coord : elements.keySet()) {
            GameElement thing = elements.get(coord);
            if (thing.getType() == ElementType.WALL) {
                walls.add(new Wall(coord.getX(), coord.getY(), thing.getColor()));
            }
        }
    }

    public GameElement getElementAt(Coordinate coordinate) {
        if(elements.containsKey(coordinate)) {
            return elements.get(coordinate);
        } else {
            return null;
        }
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Map<Coordinate, GameElement> getElements() {
        return elements;
    }

    public Collection<Coordinate> getObstructionCoordinates() {
        return elements.keySet();
    }

    public List<Coordinate> getMatchingElements(GameElement collidingElement) {
        ArrayList<Coordinate> matchingElements = new ArrayList<Coordinate>();
        for(Map.Entry<Coordinate, GameElement> entry: elements.entrySet()) {
            GameElement element = entry.getValue();
            if(element.getType() == collidingElement.getType() && element.getColor().equals(collidingElement.getColor())) {
                matchingElements.add(entry.getKey());
            }
        }
        return matchingElements;
    }
}

enum ElementType {
    WALL,
    TELEPORTER,
    DOOR_OPEN,
    DOOR_CLOSED,
    DOOR_SWITCH
}

class Wall {
    private int x;
    private int y;
    private Color color;

    public Wall(int x, int y, Color color) {

        this.x = x;
        this.y = y;
        this.color = color;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }
}