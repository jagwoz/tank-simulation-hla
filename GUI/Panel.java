package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class Panel extends JPanel
        implements Runnable{

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 600;

    private Thread thread;
    private boolean running;
    private int FPS = 10;
    private long targetTime = 1000 / FPS;
    private BufferedImage image;
    private Graphics2D g;
    public Frame frame;

    public Panel() {
        super();
        setPreferredSize(
                new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        requestFocus();
    }

    public void addNotify() {
        super.addNotify();
        if(thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    private void init() throws ExecutionException, InterruptedException {
        image = new BufferedImage(
                WIDTH, HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );
        g = (Graphics2D) image.getGraphics();
        running = true;
        frame = new Frame();
    }

    public void run() {
        try {
            init();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long start;
        long elapsed;
        long wait;

        while(running) {
            start = System.nanoTime();
            update();
            elapsed = System.nanoTime() - start;
            wait = targetTime - elapsed / 1000000;
            if(wait < 0) wait = 5;
            try {
                Thread.sleep(wait);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void update(){
        if(frame.needUpdate){
            frame.update();
            draw();
            drawToScreen();
        }
    }

    private void draw(){
        frame.draw(g);
    }

    private void drawToScreen() {
        Graphics g2 = getGraphics();
        g2.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
        g2.dispose();
    }
}

















