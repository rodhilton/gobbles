package com.nomachetejuggling.gobbles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.*;

public class Executive {

    public static void main(String[] args) {
        int width = 40;
        int height = 40;
        final List<Level> levels = new ArrayList<Level>();

        File mapDir = new File(args[0]);

        if(!mapDir.exists() || !mapDir.isDirectory() || !mapDir.canRead()) {
            throw new RuntimeException(args[0]+" is not a valid map dir");
        }

        File[] maps = mapDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".txt");
            }
        });

        Arrays.sort(maps);

        MapReader mapReader = new MapReader(width, height);

        for(File map: maps) {
            try {
                String text = new Scanner( map ).useDelimiter("\\Z").next();
                Level level = mapReader.buildLevel(text);
                levels.add(level);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                System.err.println("Map "+map+" not valid: "+e.getMessage());
            }
        }

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

}

class MapReader {
    private int width;
    private int height;

    public MapReader(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Level buildLevel(String mapContents) {
        String[] lines = mapContents.split("\n");
        if(lines.length != height) throw new RuntimeException("Number of lines != "+height);

        List<Coordinate> obstacles = new ArrayList<Coordinate>();
        for(int y=0;y<lines.length;y++) {
            String line = lines[y];
            char[] c = line.toCharArray();
            if(c.length != width) throw new RuntimeException("Number of columns != "+width);
            for(int x=0;x<c.length;x++) {
                char theChar = c[x];
                if(theChar!=' ') {
                    obstacles.add(new Coordinate(x, y));
                }
            }
        }

        return new Level(obstacles);
    }

    private Level generateLevelWithObstacles(int numObstacles) {
        List<Coordinate> obstacles = new ArrayList<Coordinate>();
        Random rng = new Random();
        for(int i=0;i<numObstacles;i++) {
            obstacles.add(new Coordinate(rng.nextInt(width), rng.nextInt(height)));
        }
        Level level = new Level(obstacles);
        return level;
    }
}
