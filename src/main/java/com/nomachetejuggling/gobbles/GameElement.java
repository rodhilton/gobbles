package com.nomachetejuggling.gobbles;

import java.awt.*;

public class GameElement {
    private ElementType type;
    private Color color;

    public GameElement(ElementType type, Color color) {
        this.type = type;
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public ElementType getType() {
        return type;
    }
}
