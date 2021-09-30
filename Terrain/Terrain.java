package Terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Terrain {
    private static Terrain instance = null;
    private Random random;

    private ArrayList<Integer> terrain;
    private int terrainSize;
    private boolean nextRight;
    private int staticTerrain;

    private Terrain() {
        this.random = new Random();
        this.nextRight = true;
        this.terrainSize = 100;
        this.staticTerrain = 10;
        this.terrain = new ArrayList<>();
        createTerrain();
    }

    private int newHeight(int height){
        switch (random.nextInt(4)){
            case 0: if(height + 2 <= 76) height += 2; break;
            case 1: if(height - 2 >= 24) height -= 2; break;
        }
        return height;
    }

    private void createTerrain() {
        int height = random.nextInt(26) * 2;
        for(int i = 0; i < this.terrainSize; i++){
            height = newHeight(height);
            this.terrain.add(height);
        }
    }

    public void terrainUpdate(){
        ArrayList<Integer> newTerrain = new ArrayList<>();
        if(this.nextRight){
            for(int i = this.terrainSize - 1; i >= this.terrainSize - this.staticTerrain; i--)
                newTerrain.add(this.terrain.get(i));
        } else {
            for(int i = 0; i < this.staticTerrain; i++)
                newTerrain.add(this.terrain.get(i));
        }
        int height = newTerrain.get(newTerrain.size() - 1);
        for(int i = newTerrain.size() - 1; i < this.terrainSize - 1; i++){
            height = newHeight(height);
            newTerrain.add(height);
        }

        if(this.nextRight){
            Collections.reverse(newTerrain);
            this.nextRight = false;
            this.terrain = newTerrain;
            return;
        }
        this.terrain = newTerrain;
        this.nextRight = true;
    }

    public ArrayList<Integer> getTerrain() {
        return this.terrain;
    }
    static public Terrain getInstance()
    {
        if(instance==null) instance = new Terrain();
        return instance;
    }
}
