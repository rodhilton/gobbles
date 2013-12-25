package com.nomachetejuggling.gobbles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Executive {

    public static void main(String[] args) {
        int width = 40;
        int height = 40;
        final List<Level> levels = new ArrayList<Level>();

        levels.add(generateLevelWithObstacles(0, width, height));
        levels.add(generateLevelWithObstacles(5, width, height));
        levels.add(generateLevelWithObstacles(10, width, height));
        levels.add(generateLevelWithObstacles(20, width, height));
        levels.add(generateLevelWithObstacles(40, width, height));

        final GameState state = new GameState(width,height, levels);

        final MainWindow mainWindow = new MainWindow(state);



        Runnable gameLoop = new Runnable() {

            private long lastMillis = System.currentTimeMillis();

            @Override
            public void run() {
                while(true) {
                    try {
                        long currentMillis = System.currentTimeMillis();
                        if(currentMillis - lastMillis > 100) {
                            state.tick(mainWindow.getGameInput());
                            lastMillis = currentMillis;
                            mainWindow.reRender();
                        }
                        Thread.sleep(10);
                    } catch (InterruptedException e) { }
                }
            }
        };

        gameLoop.run();

    }

    private static Level generateLevelWithObstacles(int numObstacles, int width, int height) {
        List<Coordinate> obstacles = new ArrayList<Coordinate>();
        Random rng = new Random();
        for(int i=0;i<numObstacles;i++) {
            obstacles.add(new Coordinate(rng.nextInt(width), rng.nextInt(height)));
        }
        Level level = new Level(obstacles);
        return level;
    }
}
