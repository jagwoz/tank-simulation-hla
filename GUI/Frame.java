package GUI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

public class Frame {
    /*x* PUBLIC *x*/
    public ArrayList<Integer> terrain;
    public float avg;
    public boolean needUpdate;
    public int targetPosition;
    public int temperature;
    public int wind;
    public int shots;
    public int misses;
    public int hits;
    public int vInit;
    public int angle;
    public int lastShotPosition;
    public int bulletSize;
    public int targetSize;

    private BufferedImage temperatureImage;
    private BufferedImage windImage;
    private BufferedImage targetImage;
    private BufferedImage bulletIn;
    private BufferedImage bulletMiss;
    private ArrayList<BufferedImage> tanksImages;
    private ArrayList<BufferedImage> timeImages;
    private BufferedImage crossImage;
    private int lastHits;
    private int timer;

    public Frame(){
        terrain = new ArrayList<>(Collections.nCopies(100, 50));
        tanksImages = new ArrayList<>();
        timeImages = new ArrayList<>();
        temperature = 0;
        misses = 0;
        wind = 0;
        shots = 0;
        hits = 0;
        targetSize = 2;
        bulletSize = 2;
        lastHits = 0;
        avg = 0.0f;
        angle = 20;
        vInit = 59;
        targetPosition = 49;
        lastShotPosition = 999;
        needUpdate = false;
        timer = 0;
        try{
            windImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/wind.png"
                    )
            );
            bulletIn = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/bulletg2.png"
                    )
            );
            bulletMiss = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/bulletg.png"
                    )
            );
            targetImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/target.png"
                    )
            );
            temperatureImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/temperature.png"
                    )
            );
            crossImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/hits.png"
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            BufferedImage tanksImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/tank.png"
                    )
            );
            for(int i=0; i<13; i++){
                tanksImages.add(tanksImage.getSubimage(i*100, 0, 100, 100));
            }
            BufferedImage timeImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Images/time.png"
                    )
            );
            for(int i=0; i<8; i++){
                timeImages.add(timeImage.getSubimage(i*100, 0, 100, 100));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(){

        needUpdate = false;
    }

    public void draw(Graphics2D g) {
        /*x* PANEL *x*/
        g.setColor(new Color(255, 255,255));
        g.fillRect(0, 0, Panel.WIDTH, Panel.HEIGHT);
        g.setFont(new Font("TimesRoman", Font.PLAIN, 30));


        g.setColor(new Color(222, 199, 104));
        if(temperature < 0)
            g.setColor(new Color(255, 255,255));
        g.fillRect(0, 350, Panel.WIDTH, Panel.HEIGHT - 350);

        g.setColor(new Color(42, 117, 158));
        g.fillRect(0, 0, Panel.WIDTH, 350);
        g.fillRect(350, 150, 800, 400);

        g.setColor(new Color(222, 199, 104));
        if(temperature < 0)
            g.setColor(new Color(255, 255,255));
        for(int i = 0; i < terrain.size(); i++){
            g.fillRect(350 + i * 8, 150 + (100 - terrain.get(i)) * 4, 8, terrain.get(i) * 4);
        }
        g.setColor(new Color(0, 0, 0));
        g.drawRect(350, 150, 800, 400);

        g.drawImage(targetImage, 350 + targetPosition * 8 - 8, 150 + (100 - terrain.get(targetPosition)) * 4 - 35, 16, 35, null);
        g.setColor(new Color(231, 0, 0));
        g.drawLine(350 + vInit * 8, 150, 350 + vInit * 8, 550);


        if(lastShotPosition < 999){
            if(lastHits == hits){
                g.drawImage(bulletMiss, 350 + lastShotPosition * 8 - 8, 150 + (100 - terrain.get(lastShotPosition)) * 4 - 35, 16, 35, null);
            } else {
                g.drawImage(bulletIn, 350 + lastShotPosition * 8 - 8, 150 + (100 - terrain.get(lastShotPosition)) * 4 - 35, 16, 35, null);
                lastHits = hits;
            }

        }

        /*x* WEATHER *x*/
        g.setColor(new Color(255, 255, 255));
        g.fillRect(150, 50, 150, 150);
        if(temperature >= 0){
            g.setColor(new Color(231, 0, 0));
        } else {
            g.setColor(new Color(42, 149, 173));
        }
        g.fillRect(150, 113 - temperature * 10, 150, 77 + temperature * 10);
        g.drawImage(temperatureImage, 150, 50, 150, 150, null);

        g.setColor(new Color(255, 255, 255));
        g.fillRect(50, 50, 150, 150);
        g.setColor(new Color(80, 77, 77));

        if(wind > 0)
            g.fillRect(127, 50, wind * 10, 150);
        if(wind < 0)
            g.fillRect(127 + wind * 10, 50, -wind * 10, 150);

        g.drawImage(windImage, 50, 50, 150, 150, null);

        /*x* STATISTICS *x*/
        g.setColor(new Color(231, 0, 0));
        g.fillRect(400, 25, 100, 100);
        g.setColor(new Color(0, 231, 0));

        g.fillRect(400, 40 + (70 - (int)((float)70 * ((float)hits/(float)shots))), 100, (int)((float)70 * ((float)hits/(float)shots)));
        g.drawImage(crossImage, 400, 25, 100, 100, null);

        g.setColor(new Color(255, 255, 255));
        g.fillRect(700, 25, 100, 100);
        g.drawImage(timeImages.get(timer%8), 700, 25, 100, 100, null);
        timer++;

        /*x* TANK *x*/
        g.drawImage(tanksImages.get((angle-20)/5), 150, 275, 100, 100, null);

        /*x* TEXT INFORMATION *x*/
        g.setColor(new Color(0, 0, 0));
        String shotsString = "Misses: " + misses;
        g.drawString(shotsString,510, 68);
        String hitsString = "Hits: " + hits;
        g.drawString(hitsString,510, 103);
        String avgString = "Avg time: " + avg;
        g.drawString(avgString,811, 84);
    }
}