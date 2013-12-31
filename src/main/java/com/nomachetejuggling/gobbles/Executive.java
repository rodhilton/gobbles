package com.nomachetejuggling.gobbles;

import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;


import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
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
                        if(currentMillis - lastMillis > 80) {
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

    private static Map<Character, GameElement> charMapping = new HashMap<Character, GameElement>() {{
        put('W', new GameElement(ElementType.WALL, new Color(152, 93, 0))); //Dark Brown
        put('X', new GameElement(ElementType.WALL, new Color(0, 7, 152))); //Dark Blue
        put('P', new GameElement(ElementType.WALL, new Color(136, 0, 152))); //Dark Purple
        put('G', new GameElement(ElementType.WALL, new Color(4, 152, 0))); //Dark Green
        put('1', new GameElement(ElementType.TELEPORTER, new Color(255, 162, 0))); //Orange
        put('2', new GameElement(ElementType.TELEPORTER, new Color(0, 255, 0))); //Green
        put('3', new GameElement(ElementType.TELEPORTER, new Color(48, 255, 255))); //Cyan
        put('4', new GameElement(ElementType.TELEPORTER, new Color(255, 63, 63))); //Red
        put('5', new GameElement(ElementType.TELEPORTER, new Color(255, 63, 255))); //Red
        put('a', new GameElement(ElementType.DOOR_OPEN, new Color(255, 162, 0)));
        put('A', new GameElement(ElementType.DOOR_CLOSED, new Color(255, 162, 0)));
        put('ä', new GameElement(ElementType.DOOR_SWITCH, new Color(255, 162, 0)));
    }};

    public MapReader() {
    }

    public Level buildLevel(String mapContents) {
        String[] lines = mapContents.split("\n");
        int height = lines.length;
        int width = 0;

        Map<Coordinate, GameElement> gameElements = new HashMap<Coordinate, GameElement>();

        for(int y=0;y<lines.length;y++) {
            String line = lines[y];
            char[] c = line.toCharArray();
            if(c.length > width) width = c.length;
            for(int x=0;x<c.length;x++) {
                char theChar = c[x];
                Coordinate coord = new Coordinate(x, y);
                if(charMapping.containsKey(theChar)) {
                    gameElements.put(coord, charMapping.get(theChar));
                }
            }
        }

        return new Level(width, height, gameElements);
    }
}
