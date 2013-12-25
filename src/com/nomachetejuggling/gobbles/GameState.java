package com.nomachetejuggling.gobbles;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//TODO: dead pauses game, can restart
//TODO: lives per level
//TODO: load maps from text files
//TODO: random powerups that reverse snake if about to die (maybe they move)
//TODO: powerup that auto pauses if about to die
//TODO: pause should blank the screen to avoid slowmo effect
//TODO: HUD: counter num
//TODO: HUD: global point system (counts down as you move maybe, up big when you get food)
//TODO: HUD: current level
//TODO: game modes:  EASY (infinite lives per level, lower speed), NORMAL (5 lives per level, normal speed), HARD (3 lifes total, slightly faster speed), INSANE (1 life total, same speed as hard)
//TODO: high scores
//TODO (later): special characters for things like teleporters, doors, etc.
//TODO (later): special characters that paint different colors, but are not obstacles?
//TODO (later): lets split some stuff apart at some point.  Move renderer out at least, it's not part of state

public class GameState {
    private static final int SNAKE_START_LENGTH=10;
    private static final int SNAKE_MULTIPLIER=7;
    private static final int FOOD_GOAL = 10;

    private int currentLevelIndex;
    private Level currentLevel;

    private int width;
    private int height;
    private boolean dead;
    private int snakeLength;
    ArrayList<Coordinate> snake;
    private boolean paused;
    int points;

    private Direction direction;

    private ArrayList<Coordinate> validSquares;
    private Coordinate foodLocation;
    private int foodCount;

    private Random rng;

    private List<Level> levels = new ArrayList<Level>();

    public GameState(int width, int height, List<Level> levels) {

        this.rng = new Random();
        this.dead = false;
        this.width = width;
        this.height = height;
        this.levels = levels;
        this.currentLevelIndex = 0;
        this.points = 0;

        initLevel(currentLevelIndex);
    }

    private void initLevel(int index) {
        this.snakeLength = SNAKE_START_LENGTH;
        this.snake = new ArrayList<Coordinate>();

        this.validSquares = new ArrayList<Coordinate>();
        //Init valid squares.  Remove obstacles
        for(int i=0;i<width;i++) {
            for(int j=0;j<height;j++) {
                validSquares.add(new Coordinate(i,j));
            }
        }
        this.currentLevel = levels.get(index);

        this.validSquares.removeAll(currentLevel.getObstacles());

        Coordinate startPos = placeObject();
        snake.add(startPos);
        this.direction = Direction.NONE;
        this.paused = true;
        foodLocation = placeObject();

        this.foodCount = 0;
    }

    private Coordinate placeObject() {
        ArrayList<Coordinate> trulyValid = new ArrayList<Coordinate>();
        trulyValid.addAll(validSquares);
        trulyValid.removeAll(snake);
        return trulyValid.get(rng.nextInt(trulyValid.size()));
    }

    private int scaleX(int x) {
        if(x < 0) return width + x;
        else return x % width;
    }

    private int scaleY(int y) {
        if(y < 0) return height + y;
        else return y % height;
    }

    public void tick(GameInput gameInput) {
        if(!isPaused()) {
            if(gameInput.getKey() == KeyEvent.VK_SPACE) {
                paused = true;
                return;
            }

            if(safeDirectionChange(gameInput)) {
                direction = getDirection(gameInput);
            }

            Coordinate head = snake.get(snake.size()-1);

            switch( direction ) {
                case UP:
                    snake.add(new Coordinate(head.getX(), scaleY(head.getY() - 1)));
                    break;
                case DOWN:
                    snake.add(new Coordinate(head.getX(), scaleY(head.getY()+1)));
                    break;
                case LEFT:
                    snake.add(new Coordinate(scaleX(head.getX()-1), head.getY()));
                    break;
                case RIGHT :
                    snake.add(new Coordinate(scaleX(head.getX()+1), head.getY()));
                    break;
            }

            //check for collisions with snake, world, or tokens
            boolean hasCollided = hasCollided();

            if(hasCollided) {
                dead = true;
            }

            if(head.equals(foodLocation)) {
                foodLocation = placeObject();
                foodCount++;
                snakeLength=(SNAKE_MULTIPLIER*foodCount)+SNAKE_START_LENGTH;
                points = points + 10000;
                System.out.println("Score: "+foodCount);
                if(foodCount>=FOOD_GOAL && currentLevelIndex != levels.size()-1) {
                    //If accomplished goal and not on last level (which goes on forever):
                    this.paused = true;
                    this.currentLevelIndex++;
                    gameInput.clear();
                    initLevel(currentLevelIndex);
                }
            } else {
                this.points = Math.max(points - 10, 0);
            }

            System.out.println("Points: "+points);

            while(snake.size()>snakeLength) {
                snake.remove(0);
            }
        } else {
            direction = getDirection(gameInput);
            if(direction!= Direction.NONE) paused = false;
        }

    }

    private Direction getDirection(GameInput gameInput) {
        switch(gameInput.getKey()) {
            case KeyEvent.VK_UP: return Direction.UP;
            case KeyEvent.VK_DOWN: return Direction.DOWN;
            case KeyEvent.VK_RIGHT: return Direction.RIGHT;
            case KeyEvent.VK_LEFT: return Direction.LEFT;
        }
        return Direction.NONE;
    }

    private boolean safeDirectionChange(GameInput gameInput) {
        if(direction==Direction.DOWN && gameInput.getKey()==KeyEvent.VK_UP) return false;
        if(direction==Direction.UP && gameInput.getKey()==KeyEvent.VK_DOWN) return false;
        if(direction==Direction.RIGHT && gameInput.getKey()==KeyEvent.VK_LEFT) return false;
        if(direction==Direction.LEFT && gameInput.getKey()==KeyEvent.VK_RIGHT) return false;

        return true;
    }

    private boolean isPaused() {
        return dead || paused;
    }

    private boolean hasCollided() {
        Coordinate head = snake.get(snake.size()-1);

        for(int i=0;i<snake.size()-1;i++) { //All except the head
            if(snake.get(i).equals(head)) return true;
        }

        for(int i=0;i<currentLevel.getObstacles().size();i++) { //All except the head
            if(currentLevel.getObstacles().get(i).equals(head)) {
                return true;
            }
        }

        return false;
    }

    public BufferedImage render(int width, int height) {
        final Color SNAKE_COLOR = new Color(192, 109, 209, 255);

        BufferedImage buff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) buff.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //Background
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, width, height);

        int hudSize = 40;
        height = height - hudSize;

        g2.setColor(Color.GRAY);
        g2.fillRect(0,0,width,hudSize);

        int offsetHeight = hudSize;
        int offsetWidth = 0;
        int scaleFactor;
        int smallestSide;
        int smallestRealSide;

        if(width > height) {
            scaleFactor = (int)(height/((double)this.height));
            offsetWidth+= (width - height)/2;
            smallestSide = this.height;
            smallestRealSide = height;
        } else {
            scaleFactor = (int)(width/((double)this.width));
            offsetHeight+= (height - width)/2;
            smallestSide = this.width;
            smallestRealSide = width;
        }

        //int gameSize = smallestSide*scaleFactor;
        //Add in offsets for the imperfectness of the the board size, which must be a multiple of the game grid size
        int fudgeFactor = (smallestRealSide - (smallestSide*scaleFactor)) / 2;

        offsetHeight = offsetHeight + fudgeFactor;
        offsetWidth = offsetWidth + fudgeFactor;

        g2.setColor(Color.BLACK);
        g2.fillRect(offsetWidth, offsetHeight, scaleFactor*smallestSide, scaleFactor*smallestSide);
        g2.setColor(SNAKE_COLOR);

        Coordinate head = snake.get(snake.size()-1);
        for(Coordinate snakeCoord: this.snake) {
            if(snakeCoord.equals(head)) {
                g2.fillRoundRect((snakeCoord.getX()*scaleFactor)+offsetWidth, (snakeCoord.getY()*scaleFactor)+offsetHeight, scaleFactor, scaleFactor, scaleFactor/2, scaleFactor/2);
            } else {
                g2.fillRect((snakeCoord.getX() * scaleFactor) + offsetWidth, (snakeCoord.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);
            }
        }

        g2.setColor(Color.BLUE);
        for(Coordinate obstacle: this.currentLevel.getObstacles()) {
                g2.fillRect((obstacle.getX() * scaleFactor) + offsetWidth, (obstacle.getY() * scaleFactor) + offsetHeight, scaleFactor, scaleFactor);
        }

        g2.setColor(Color.YELLOW);
        g2.fillOval((foodLocation.getX()*scaleFactor)+offsetWidth, (foodLocation.getY()*scaleFactor)+offsetHeight, scaleFactor, scaleFactor);

        if(paused) {
            g2.setColor(Color.WHITE);
//            g2.fillRect((head.getX()*scaleFactor)+offsetWidth, (head.getY()*scaleFactor)+offsetHeight, scaleFactor, scaleFactor);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (hudSize*2)/3));
            Rectangle2D bounds = g2.getFontMetrics().getStringBounds("Paused", g2);
            g2.drawString("Paused", (int)(offsetWidth+(smallestRealSide-bounds.getWidth())/2), (int)((hudSize-bounds.getHeight())/2+g2.getFontMetrics().getHeight()-g2.getFontMetrics().getDescent()));
        }

        if(dead) {
            g2.setColor(Color.RED);
            g2.fillRect((head.getX()*scaleFactor)+offsetWidth, (head.getY()*scaleFactor)+offsetHeight, scaleFactor, scaleFactor);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, hudSize));
            Rectangle2D bounds = g2.getFontMetrics().getStringBounds("Dead!", g2);
            g2.drawString("Dead!", (int)(offsetWidth+(smallestRealSide-bounds.getWidth())/2), (int)((hudSize-bounds.getHeight())/2+g2.getFontMetrics().getHeight()-g2.getFontMetrics().getDescent()));
        }

        return buff;
    }
}

class Coordinate {
    private final int x;
    private final int y;

    Coordinate(int x, int y) {
        this.x=x;
        this.y=y;
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
    private List<Coordinate> obstacles;

    public Level(List<Coordinate> obstacles) {
        this.obstacles = obstacles;
    }

    public List<Coordinate> getObstacles() {
        return obstacles;
    }
}