package com.nomachetejuggling.gobbles;

import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;


import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class Executive {

    public static void main(String[] args) {
        final List<Level> levels = new ArrayList<Level>();

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(new ResourcesScanner()));

        Set<String> textFiles = reflections.getResources(Pattern.compile("\\d+_.*\\.txt"));

        for (String file : textFiles){
            System.out.println(file);
        }

        List<String> maps = new ArrayList<String>(textFiles);

        Collections.sort(maps);

        System.out.println(maps);

        MapReader mapReader = new MapReader();

        for(String map: maps) {
            try {
                InputStream myInputStream = Executive.class.getClassLoader().getResourceAsStream(map);
                String myString = IOUtils.toString(myInputStream, "UTF-8");
                Level level = mapReader.buildLevel(myString);
                levels.add(level);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                System.err.println("Map "+map+" not valid: "+e.getMessage());
            } catch (IOException e) {
                System.err.println("Map " + map + " not valid: " + e.getMessage());
            }
        }

        final GameState state = new GameState(levels);

        final MainWindow mainWindow = new MainWindow(state);



        Runnable gameLoop = new Runnable() {

            private long lastMillis = System.currentTimeMillis();

            @Override
            public void run() {
                while(true) {
                    try {
                        long currentMillis = System.currentTimeMillis();
                        if(currentMillis - lastMillis > 90) {
                            state.tick(mainWindow.getGameInput());
                            lastMillis = currentMillis;
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

    public MapReader() {
    }

    public Level buildLevel(String mapContents) {
        String[] lines = mapContents.split("\n");
        int height = lines.length;
        int width = 0;

        List<Coordinate> obstacles = new ArrayList<Coordinate>();
        for(int y=0;y<lines.length;y++) {
            String line = lines[y];
            char[] c = line.toCharArray();
            if(c.length > width) width = c.length;
            for(int x=0;x<c.length;x++) {
                char theChar = c[x];
                if(theChar!=' ') {
                    obstacles.add(new Coordinate(x, y));
                }
            }
        }

        return new Level(width, height, obstacles);
    }
}
