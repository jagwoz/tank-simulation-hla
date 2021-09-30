package Target;

import java.util.Random;

public class Target {
    private int position;
    private int size;
    private int direction;
    private boolean needTerrainUpdate;
    private static Target instance = null;
    private Random random;

    private Target() {
        this.random = new Random();
        this.position = 49;
        this.size = 4;
        this.direction = 1;
        this.needTerrainUpdate = false;
    }

    public void updatePosition(int temperature){
        int step = 2;
        if(Math.abs(temperature) >= 5){
            step = 1;
        }
        if(direction == 1){
            if(position + step >= 99){
                position = 99;
                direction = 0;
                needTerrainUpdate = true;
            } else {
                position += step;
            }
        } else {
            if(position - step <= 0){
                position = 0;
                direction = 1;
                needTerrainUpdate = true;
            } else {
                position -= step;
            }
        }
    }

    public boolean isNeedTerrainUpdate(){
        if(needTerrainUpdate){
            needTerrainUpdate = false;
            return true;
        }
        return false;
    }

    public int getPosition() {
        return position;
    }
    public int getSize() {
        return size;
    }
    public int getDirection() {
        return direction;
    }

    static public Target getInstance()
    {
        if(instance==null) instance = new Target();
        return instance;
    }
}
