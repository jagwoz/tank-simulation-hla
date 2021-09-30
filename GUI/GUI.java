package GUI;

import javax.swing.*;
import java.util.Random;

public class GUI {
    private int shots;
    private int hits;
    private int misses;
    private float avg;
    private static GUI instance = null;
    private Random random;
    private boolean isWindow = false;
    public JFrame window;
    public Panel panel = new Panel();

    private GUI() {
        this.random = new Random();
        this.shots = 0;
        this.avg = 0.0f;
        this.hits = 0;
        this.misses = 0;
    }

    public void addShot(){
        shots++;
    }

    public void addPanel(){
        if(!isWindow){
            window = new JFrame("TankSimulation");
            window.setContentPane(panel);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.pack();
            window.setVisible(true);
            isWindow = true;
        }
    }

    public void addHit(int wasHit, double time){
        if(wasHit == 1) hits++;
        else misses++;
        if(hits != 0)
        avg = (float)((float)time/(float)hits);
        else avg = 0;
    }

    static public GUI getInstance()
    {
        if(instance==null) instance = new GUI();
        return instance;
    }
}
