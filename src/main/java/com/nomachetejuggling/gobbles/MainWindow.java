package com.nomachetejuggling.gobbles;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

public class MainWindow extends JFrame implements KeyListener {

    private HudPanel hudPanel;
    private GamePanel panel;
    private GameInput gameInput;

    public MainWindow(GameState state) {
        super("Gobbles");


        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize(dim);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setTitle("Gobbles");
        this.setBackground(Color.black);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);

        panel = new GamePanel(state);

        this.getContentPane().add(panel, BorderLayout.CENTER);

        hudPanel = new HudPanel(state);
        hudPanel.setPreferredSize(new Dimension(40, 40));

        this.getContentPane().add(hudPanel, BorderLayout.NORTH);

        this.setSize(452, 522);

        this.setVisible(true);
        this.addKeyListener(this);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);

        gameInput = new GameInput();
    }

    public void reRender() {
        panel.repaint();
        hudPanel.repaint();
    }

    public GameInput getGameInput() {
        return gameInput;
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        gameInput.queueKey(keyEvent.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }
}

class GamePanel extends JPanel {
    private GameState grid;

    public GamePanel(GameState grid) {
        this.grid = grid;
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = this.getWidth();
        int height = this.getHeight();

        BufferedImage buff = grid.renderGame(width, height);

        g.drawImage(buff, 0, 0, null);
    }

}

class HudPanel extends JPanel {
    private GameState grid;

    public HudPanel(GameState grid) {
        this.grid = grid;
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = this.getWidth();
        int height = this.getHeight();

        BufferedImage buff = grid.renderHud(width, height);

        g.drawImage(buff, 0, 0, null);
    }

}
