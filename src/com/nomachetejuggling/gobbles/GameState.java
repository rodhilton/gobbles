package com.nomachetejuggling.gobbles;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

//TODO: game starts paused, first direction unpauses and moves in that direction
//TODO: dead pauses game, can restart
//TODO: counter for pellets
//TODO: load maps from text files
//TODO: obstacles collide
//TODO: getting 10 pellets moves to next world
//TODO: final world has no end, just more and more pellets
//TODO (later): special characters for things like teleporters, doors, etc.
//TODO (later): special characters that paint different colors, but are not obstacles?

public class GameState {
    private static final int SNAKE_START_LENGTH=10;
    private static final int SNAKE_MULTIPLIER=7;

    private int width;
    private int height;
    private boolean dead;
    private int snakeLength;
    ArrayList<Coordinate> snake;

    private Direction direction;

    private final ArrayList<Coordinate> validSquares;
    private Coordinate foodLocation;
    private int foodCount;

    private Random rng;

    public GameState(int width, int height) {

        this.rng = new Random();
        this.dead = false;
        this.width = width;
        this.height = height;
        this.snakeLength = SNAKE_START_LENGTH;
        this.snake = new ArrayList<Coordinate>();
        this.foodCount = 0;

        this.validSquares = new ArrayList<Coordinate>();
        //Init valid squares.  Remove obstacles
        for(int i=0;i<width;i++) {
            for(int j=0;j<height;j++) {
                validSquares.add(new Coordinate(i,j));
            }
        }

        Coordinate startPos = new Coordinate(rng.nextInt(width), rng.nextInt(height));
        snake.add(startPos);
        this.direction = Direction.RIGHT;
        foodLocation = placeFood();
    }

    private Coordinate placeFood() {
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
                foodLocation = placeFood();
                foodCount++;
                snakeLength=(SNAKE_MULTIPLIER*foodCount)+SNAKE_START_LENGTH;
            }

            while(snake.size()>snakeLength) {
                snake.remove(0);
            }
        } else {

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
        return dead;
    }

    private boolean hasCollided() {
        Coordinate head = snake.get(snake.size()-1);

        for(int i=0;i<snake.size()-1;i++) { //All except the head
            if(snake.get(i).equals(head)) return true;
        }

        return false;
    }

    public BufferedImage render(int width, int height) {
        BufferedImage buff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) buff.getGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.GRAY);
        g2.fillRect(0, 0, width, height);


        int offsetHeight = 0;
        int offsetWidth = 0;
        int scaleFactor;
        int smallestSide;

        if(width > height) {
            scaleFactor = (int)(height/((double)this.height));
            offsetWidth = (width - height)/2;
            smallestSide = this.height;
        } else {
            scaleFactor = (int)(width/((double)this.width));
            offsetHeight = (height - width)/2;
            smallestSide = this.width;
        }
        g2.setColor(Color.BLACK);
        g2.fillRect(offsetWidth, offsetHeight, scaleFactor*smallestSide, scaleFactor*smallestSide);

        g2.setColor(Color.WHITE);
        System.out.println(scaleFactor);

        for(Coordinate snakeCoord: this.snake) {
            g2.fillRect((snakeCoord.getX()*scaleFactor)+offsetWidth, (snakeCoord.getY()*scaleFactor)+offsetHeight, scaleFactor, scaleFactor);
        }

        g2.setColor(Color.YELLOW);
        g2.fillOval((foodLocation.getX()*scaleFactor)+offsetWidth, (foodLocation.getY()*scaleFactor)+offsetHeight, scaleFactor, scaleFactor);

        if(dead) {
            g2.setColor(Color.RED);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
            FontMetrics fm = g2.getFontMetrics(g2.getFont());
            Coordinate head = this.snake.get(this.snake.size() - 1);
            g2.drawString("Dead",head.getX()*scaleFactor, head.getY()*scaleFactor+fm.getHeight()/2);
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

    public boolean equals(Coordinate other) {
        return this.x == other.x && this.y == other.y;
    }
}

enum Direction {
    UP, LEFT, DOWN, RIGHT, NONE
}