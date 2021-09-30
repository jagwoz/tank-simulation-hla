package Bullet;

import java.util.ArrayList;

public class Bullet {
    private static Bullet instance = null;
    private final int maxRunTime;
    private final int size;

    private int runTime;
    private boolean isLaunch;
    private ArrayList<Integer> params;
    private boolean needRemoveBullet;

    private Bullet() {
        this.maxRunTime = 9;
        this.size = 5;

        this.isLaunch = false;
        this.runTime = 0;
        this.params = new ArrayList<>();
        this.needRemoveBullet = false;
    }

    public boolean isRemoveBulletNeed(){
        if(needRemoveBullet){
            needRemoveBullet = false;
            isLaunch = false;
            return true;
        }
        return false;
    }

    public int getPositionParam(){
        int sum = 0;
        for(int a: params) sum += a;
        return (int)(sum/params.size());
    }

    public void updatePosition(int temperature, int wind){
        if(!isLaunch) return;
        params.add(temperature);
        params.add(wind);
        runTime ++;
        if(runTime >= maxRunTime) needRemoveBullet = true;
    }

    public void launchBullet(){
        isLaunch = true;
        params = new ArrayList<>();
        runTime = 0;
    }

    public int getSize() {
        return size;
    }

    static public Bullet getInstance()
    {
        if(instance==null) instance = new Bullet();
        return instance;
    }
}
