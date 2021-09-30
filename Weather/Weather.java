package Weather;

import java.util.Random;

public class Weather {
    private static Weather instance = null;
    private Random random;

    private int temperature;
    private int wind;

    private double windChangeChance = 0.25d;
    private double temperatureChangeChance = 0.1d;

    private Weather() {
        random = new Random();
        temperature = 0;
        wind = 0;
    }

    public void updateWeather(){
        temperature = updateParam(temperature, temperatureChangeChance);
        wind = updateParam(wind, windChangeChance);
    }

    public int updateParam(int param, double paramChangeChance){
        if(random.nextDouble() > paramChangeChance) return param;
        if(param == 0){
            switch (random.nextInt(2)){
                case 0: param++; break;
                default: param--; break;
            }
        } else if(param > 0){
            if(random.nextInt(param + 1) == 0)
                param++;
            else param--;
        } else {
            if(random.nextInt(Math.abs(param - 1)) == 0)
                param--;
            else param++;
        }
        if(param > 5) param = 5;
        else if(param < -5) param = -5;
        return param;
    }

    static public Weather getInstance()
    {
        if(instance==null) instance = new Weather();
        return instance;
    }
    public int getTemperature() {
        return temperature;
    }
    public int getWind() {
        return wind;
    }
}
