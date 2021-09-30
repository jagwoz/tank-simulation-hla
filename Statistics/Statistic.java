package Statistics;

import java.util.Random;

public class Statistic {
    private int shots;
    private int hits;
    private float avg;
    private static Statistic instance = null;

    private Statistic() {
        this.shots = 0;
        this.avg = 0.0f;
        this.hits = 0;
    }

    public void addShot(){
        shots++;
    }

    public void addHit(int wasHit, double time){
        if(1 == Integer.parseInt(String.valueOf(wasHit).substring(1,2))) hits++;
        if(hits != 0)
        avg = ((float)time/(float)hits);
        else avg = 0;
    }

    public void updateAvg(double time){
        if(hits != 0)
            avg = ((float)time/(float)hits);
        else avg = 0;
    }

    public int getShots() {
        return shots;
    }
    public int getHits() {
        return hits;
    }
    public float getAvgChance() {
        return avg;
    }
    static public Statistic getInstance()
    {
        if(instance==null) instance = new Statistic();
        return instance;
    }
}
