package com.nomachetejuggling.gobbles;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class GameInput {

    private List<PressedKey> keys = new ArrayList<PressedKey>();

    private int key;

    public void queueKey(int key) {
        if(key != KeyEvent.VK_UP && key != KeyEvent.VK_DOWN && key != KeyEvent.VK_LEFT && key != KeyEvent.VK_RIGHT && key != KeyEvent.VK_SPACE && key != KeyEvent.VK_ENTER) return;

        PressedKey newKey = new PressedKey();
        newKey.setTimeStamp(System.currentTimeMillis());
        newKey.setKey(key);

        keys.add(newKey);
    }

    public int dequeueKey() {
        if(keys.size() == 0) return 0;
        PressedKey key = keys.get(0);
        keys.remove(0);
//        while(System.currentTimeMillis() - key.timeStamp > 1000) {
//            if(keys.size() == 0) return 0;
//            key = keys.first()
//            keys.remove(0)
//        }
        if(key == null) return 0;
        return key.key;
    }

    public void clear() {
        this.key = 0;
    }

    public boolean keyPressed() {
        return this.key != 0;
    }
}