package com.nomachetejuggling.gobbles;

import java.awt.event.KeyEvent;

public class GameInput {
    private int key;

    public void setKey(int key) {
        if(key != KeyEvent.VK_UP && key != KeyEvent.VK_DOWN && key != KeyEvent.VK_LEFT && key != KeyEvent.VK_RIGHT && key != KeyEvent.VK_SPACE) return;

        this.key = key;
    }

    public int getKey() {
        return key;
    }

    public void clear() {
        this.key = 0;
    }
}
