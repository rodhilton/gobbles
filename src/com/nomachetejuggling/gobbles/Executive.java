package com.nomachetejuggling.gobbles;

public class Executive {

    public static void main(String[] args) {
        final GameState state = new GameState(40,40);

        final MainWindow mainWindow = new MainWindow(state);



        Runnable gameLoop = new Runnable() {

            private long lastMillis = System.currentTimeMillis();

            @Override
            public void run() {
                while(true) {
                    try {
                        long currentMillis = System.currentTimeMillis();
                        if(currentMillis - lastMillis > 40) {
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
