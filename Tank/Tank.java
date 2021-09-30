package Tank;

public class Tank {
    private static Tank instance = null;
    private int angle;
    private int viewFinder;
    private int range;
    private boolean canFirstShot;
    private boolean canShot;

    private Tank() {
        this.canFirstShot = false;
        this.canShot = true;
        this.viewFinder = 0;
        this.angle = 45;
        this.range = 10;
    }

    public void calculateAngle(int targetPosition) {
        this.angle = 20 + (int)((((float)targetPosition + 1.0f) / 100) * 60);
        if(!canFirstShot) {
            System.out.println(targetPosition);
            viewFinder = targetPosition + range;
            canFirstShot = true;
        }
    }

    public void calculateMoreShoots(int targetPosition, int targetDirection, int bulletPositionLast, int wasHitLast) {
        if(!canShot) {
            if(0 == Integer.parseInt(String.valueOf(wasHitLast).substring(1,2))){
                if(Integer.valueOf(String.valueOf(wasHitLast).substring(2, 3)).equals(Integer.valueOf(String.valueOf(wasHitLast).substring(3, 4)))){
                    range = 10 + Math.abs(targetPosition - bulletPositionLast);
                }
            }

            if(targetDirection == 1){
                viewFinder = targetPosition + range;
                if(viewFinder > 99) viewFinder = 99 - (viewFinder - 99);
            } else {
                viewFinder = targetPosition - range;
                viewFinder = Math.abs(viewFinder);
            }
            canShot = true;
        }
    }

    public boolean canTankShot(){
        if(canFirstShot){
            if(canShot){
                canShot = false;
                return true;
            }
            return false;
        }
        return false;
    }

    public int getViewFinder() {
        return viewFinder;
    }
    public int getAngle() {
        return angle;
    }
    static public Tank getInstance()
    {
        if(instance==null) instance = new Tank();
        return instance;
    }
}
