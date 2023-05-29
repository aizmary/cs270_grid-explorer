package grid_explorer;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.Color;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class robot {
    private static final RegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.A);
    private static final RegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.D);

    private static final EV3ColorSensor color_sensor_l = new EV3ColorSensor(SensorPort.S1);
    private static final EV3ColorSensor color_sensor_r = new EV3ColorSensor(SensorPort.S4);

    private final SensorModes ir_sensor = new EV3IRSensor(SensorPort.S3);
    
    private static final int ROTATE_ANGLE = 255;
    private static final int ROTATE_DELAY = 1000;

    private static final int MOVE_SPEED = 500;
    private static final int MOVE_DELAY = 1200;
    private static final int SLIGHT_MOVE_DELAY = 300;
    private static final int FIX_SPEED = 200;
    private static final int FIX_DELAY = 20;
    
    private static int cell_color = Color.WHITE;

    // turn robot 90 degrees to face left
    public static void turn_left() {
        leftMotor.rotate(-ROTATE_ANGLE, true);
        rightMotor.rotate(ROTATE_ANGLE, true);
        Delay.msDelay(ROTATE_DELAY);
    }
    
    // turn robot 90 degrees to face right
    public static void turn_right() {
        leftMotor.rotate(ROTATE_ANGLE, true);
       rightMotor.rotate(-ROTATE_ANGLE, true);
       Delay.msDelay(ROTATE_DELAY);
   }
    
    // turn robot 180 degrees to face back
    public static void turn_back() {
       turn_left();
       turn_left();
   }

    // decide which direction to turn based on previous and next direction
    void turn(int from, int to) {
       if((from == 0 && to == 3) || 
         (from == 2 && to == 0) ||
         (from == 1 && to == 2) ||
         (from == 3 && to == 1)) turn_left();
       else if((from == 0 && to == 2) ||
             (from == 2 && to == 1) ||
             (from == 1 && to == 3) ||
             (from == 3 && to == 0)) turn_right();
       else turn_back();
    }

    // move robot forward one cell
    void forward() {
        // move forward roughly one cell
        leftMotor.setSpeed(FIX_SPEED);
        rightMotor.setSpeed(FIX_SPEED);
        leftMotor.forward();
        rightMotor.forward();

        while((color_sensor_r.getColorID() != Color.BLACK) && (color_sensor_l.getColorID() != Color.BLACK)) {
            Delay.msDelay(FIX_DELAY);
        } 

        leftMotor.stop(true);
        rightMotor.stop(true);
        Delay.msDelay(SLIGHT_MOVE_DELAY);

        // calibrate by slightly changing directions (until the robot aligns to the black line)
        while((color_sensor_r.getColorID() != Color.BLACK)) {
            rightMotor.rotate(10);
            leftMotor.rotate(-5);
        }
        while((color_sensor_l.getColorID() != Color.BLACK)) {
            rightMotor.rotate(-5);
            leftMotor.rotate(10);
        }
                
        leftMotor.setSpeed(MOVE_SPEED);
        rightMotor.setSpeed(MOVE_SPEED);
        leftMotor.forward();
        rightMotor.forward();
        Delay.msDelay(MOVE_DELAY);
        
        // save color of current cell (before aligning to the black line)
        int left_color = color_sensor_l.getColorID();
        int right_color = color_sensor_r.getColorID();
        if(left_color == Color.RED || right_color == Color.RED){
            cell_color = Color.RED;
        } else {
           cell_color = Color.WHITE;
        }
          
        leftMotor.stop(true);
        rightMotor.stop(true);
        Delay.msDelay(SLIGHT_MOVE_DELAY);
    }
    
    // check whether current cell is red
    boolean red() {
       return cell_color == Color.RED;
    }

    // check whether next cell has box
    boolean obstacle() {
        SampleProvider distanceMode = ir_sensor.getMode("Distance");
        float[] value = new float[distanceMode.sampleSize()];
        distanceMode.fetchSample(value, 0);
        return value[0] < 15;
    }
}