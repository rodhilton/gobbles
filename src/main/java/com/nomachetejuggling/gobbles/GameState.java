package com.nomachetejuggling.gobbles;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import sun.security.util.BigInt;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.*;
import java.util.List;

//TODO: BUGS:
//      * one tick adds two snake points when going through teleporter.  first should be in, next tick out.  or should it? what happens if you press a direction?  seems like you should come out of the teleporter in that dir

//TODO: Map enhancements:
//      * Switches/Doors
//         * do we need to check for collisions on every part of snake, not just head?  because if you are ON an open door and close it, shouldnt it kill you?  maybe it should cut you in half but leave you alive, but leave the remains of your body there forever? wormesque.
//      * Full GUI editor for text files
//      * Map selection argument or cheat key

//TODO: Powerup System:
//      * Points in HUD
//      * Powerups:
//           ----extra life
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
    private static final int SNAKE_MULTIPLIER = 10;
    private static final int FOOD_GOAL = 10;
    private static final int DEFAULT_LIVES = 4;
    private static final int COIN_REWARD = 10;
    private static final int MAX_LIVES = 10;
    private static final double COIN_CHANCE = 0.50; //Chance of eating creating a coin

    private int currentLevelIndex;
    private Level currentLevel;

    private ArrayList<Coordinate> snake;
    private Direction direction;
    private int snakeLength;

    private int foodCount;
    private int coinCount;

    private Coordinate foodLocation;
    private Coordinate coinLocation=null;
    private Coordinate extraLifeLocation=null;

    private boolean dead;
    private boolean paused;

    private int points;
    private int lives;

    private List<Level> levels = new ArrayList<Level>();
    private ArrayList<Coordinate> validSquares;
    private Random rng;
    ArrayList<Listener<GameState>> listeners;

    private long frameCounter;

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
        this.coinCount = 0;
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
        this.frameCounter = 0L;
        this.foodCount = 0;
    }

    private Coordinate placeObject() {
        ArrayList<Coordinate> trulyValid = new ArrayList<Coordinate>();
        trulyValid.addAll(validSquares);
        trulyValid.removeAll(snake);
        trulyValid.remove(foodLocation);
        trulyValid.remove(coinLocation);
        trulyValid.remove(extraLifeLocation);
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
            frameCounter++;

            Coordinate head = snake.get(snake.size() - 1);

            GameElement currentCollidingElement = currentLevel.getElementAt(head);

            //We have to do a check for 'current' to see if it affects the move, and then later do a check to see if its a reaction to the new move itself
            //Seems like we should only have to check the once

            if(currentCollidingElement != null && currentCollidingElement.getType() == ElementType.TELEPORTER) {
                List<Coordinate> teleporters = currentLevel.getMatchingElements(currentCollidingElement);
                teleporters.removeAll(snake);
                if(teleporters.size()==0) {
                    moveSnakeRelativeTo(head);
                } else {
                    Coordinate teleportLocation = teleporters.get(rng.nextInt(teleporters.size()));
                    snake.add(teleportLocation);
                }
            } else {
                moveSnakeRelativeTo(head);
            }

            head = snake.get(snake.size() - 1);

            //check for collisions with snake, world, or tokens
            boolean hasCollided = hasCollided();

            if(hasCollided) {
                dead = true;
            }

            GameElement newCollidingElement = currentLevel.getElementAt(head);

            if(newCollidingElement != null) {

                if(newCollidingElement.getType() == ElementType.WALL) {
                    dead = true;
                }
//                switch(newCollidingElement.getType()) {
//                    case WALL:
//                        dead = true;
//                        break;
////                    case TELEPORTER:
////                        List<Coordinate> teleporters = currentLevel.getMatchingElements(collidingElement);
////                        teleporters.remove(head);
////                        Coordinate teleportLocation = teleporters.get(rng.nextInt(teleporters.size()));
////                        snake.add(teleportLocation);
////                        head = snake.get(snake.size() - 1);
////                        break;
//                    default:
//                        break;
//                }
            }

//            //check for collisions with snake, world, or tokens
//            boolean hasCollided = hasCollided();
//
//            if(hasCollided) {
//                dead = true;
//            }

            if (head.equals(coinLocation)) {
                coinCount++;
                coinLocation = null;
                if(coinCount == COIN_REWARD) {
                    coinCount = 0;
                    extraLifeLocation = placeObject();
                }
            }

            if(head.equals(extraLifeLocation)) {
                lives = Math.max(lives+1, MAX_LIVES);
                extraLifeLocation = null;
            }

            if (head.equals(foodLocation)) {
                foodLocation = placeObject();
                foodCount++;
                snakeLength = (SNAKE_MULTIPLIER * foodCount) + SNAKE_START_LENGTH;
                points = points + 10000;
                if (foodCount >= FOOD_GOAL && !lastLevel()) {
                    //If accomplished goal and not on last level (which goes on forever):
                    this.paused = true;
                    this.currentLevelIndex++;
                    gameInput.clear();
                    initLevel(currentLevelIndex);
                } else {
                    if(!hasCoin()) {
                        if(rng.nextDouble() < COIN_CHANCE) {
                            addCoin(placeObject());
                        }
                    }
                }
            } else {
//                this.points = Math.max(points - 10, 0);
            }

            while (snake.size() > snakeLength) {
                snake.remove(0);
            }
        }
    }

    public boolean hasCoin() {
        return coinLocation != null;
    }

    public void addCoin(Coordinate coordinate) {
        coinLocation = coordinate;
    }

    private boolean lastLevel() {
        return currentLevelIndex == levels.size() - 1;
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

        if(coinLocation != null) {
            g2.setColor(new Color(255, 205, 64));
            int borderSize = scaleFactor / 4;
            int animationFrames = 8;
            int coinWidth = scaleFactor - borderSize;

            int animationCount = (int)(frameCounter % animationFrames);
//            int frontBackCount = (int)(frameCounter % (animationFrames * 2));
            int halfFrame = animationFrames / 2;

            int spinAmt = 0;

            if(animationCount >= halfFrame) { //back, expanding
                spinAmt = animationFrames - animationCount;
            } else {
                spinAmt = animationCount;
            }

//            if(Math.abs(frontBackCount - animationCount) > animationFrames) {
//                //g2.setColor(new Color(178, 124, 40));
//                g2.setColor(Color.PINK);
//            } else {
//                g2.setColor(new Color(255, 205, 64));
//            }

            double percent = spinAmt / (double)halfFrame;
            int sideBorder = Math.min(coinWidth/2 - 4, (int)(percent * (coinWidth/2)));
            //System.out.println(sideBorder);


            g2.fillOval((coinLocation.getX() * scaleFactor) + offsetWidth + sideBorder + borderSize, (coinLocation.getY() * scaleFactor) + offsetHeight + borderSize, scaleFactor - borderSize * 2 - sideBorder * 2, scaleFactor - borderSize * 2);
        }

        if(extraLifeLocation != null) {
            g2.setColor(new Color(64, 255, 64));
            int animationFrames = (scaleFactor / 4)*2;

            int animationCount = (int)(frameCounter % animationFrames);
            int halfFrame = animationFrames / 2;

            int borderSize = 0;

            if(animationCount > halfFrame) { //back, expanding
                borderSize = animationFrames - animationCount;
            } else {
                borderSize = animationCount;
            }

            g2.fillOval((extraLifeLocation.getX() * scaleFactor) + offsetWidth + borderSize, (extraLifeLocation.getY() * scaleFactor) + offsetHeight + borderSize, scaleFactor - borderSize * 2, scaleFactor - borderSize * 2);
        }

//        g2.setColor(new Color(255, 255-(((int)(128*(foodCount/(double)FOOD_GOAL)))), 0, 255)); //Gotta be careful here, at last level theres more than goal counts
        g2.setColor(Color.YELLOW);
        g2.fillOval((foodLocation.getX() * scaleFactor) + offsetWidth, (foodLocation.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);
        //if(!lastLevel()) {
        if(foodCount + 1 < 100) {
            String label = ""+(foodCount+1);
            if(foodCount + 1 == FOOD_GOAL && !lastLevel()) {
                label = "!";
            }
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, (int)(scaleFactor*.7)));
            FontMetrics fm = g2.getFontMetrics(g2.getFont());
            int x = ((scaleFactor - fm.stringWidth(label)) / 2);
            int y = ((scaleFactor - fm.getHeight()) / 2) + fm.getAscent();

            g2.drawString(label, (foodLocation.getX() * scaleFactor) + offsetWidth + x, (foodLocation.getY() * scaleFactor) + offsetHeight + y);
        }

        if (paused) {
            String pausedLabel="Paused";
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaleFactor * 2));
            FontMetrics fm = g2.getFontMetrics(g2.getFont());
            int x = ((width - fm.stringWidth(pausedLabel)) / 2);
            int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(pausedLabel, x, y);
        }

        if (dead) {
            g2.setColor(Color.RED);

            String deadLabel = "Dead!";
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaleFactor * 3));
            if (lives == 0) {
                deadLabel = "GAME OVER";
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaleFactor * 4));
            }
            FontMetrics fm = g2.getFontMetrics(g2.getFont());
            int x = ((width - fm.stringWidth(deadLabel)) / 2);
            int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(deadLabel, x, y);

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
    DOOR_SWITCH,
    COIN
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