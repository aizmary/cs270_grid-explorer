package grid_explorer;

import java.lang.System;

import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.TextLCD;

public class grid_explorer {
    public static final int ROW_NUM = 4;
    public static final int COL_NUM = 6;

    public static int[] grid = new int[ROW_NUM * COL_NUM + 5];
    public static int cnt = 0; // count total number of moves + turns

    public static int cur_pos = 0; // position: 0 ~ 23
    // direction: [0, 1, 2, 3] = ["E", "W", "S", "N"]
    public static int cur_dir = 0; 
    public static int old_dir = 0;

    public static int[] red = {9, 16};
    public static int[] obstacles = {12, 22};

    // save positions of red tiles and boxes that are found
    public static int[] found_red = {-1, -1};
    public static int[] found_obstacles = {-1, -1};

    public static boolean robot_mode = false; // toggle between modes to test with/without robot hardware
    public static robot robot = null;

    public static boolean start_from_0 = true; // true => start from (0, 0), false => start from (5, 3)
    public static EV3 ev3 = (EV3) BrickFinder.getLocal();
    public static TextLCD lcd = ev3.getTextLCD();
    public static Keys keys = ev3.getKeys();

    public static boolean debug = true; // toggle between debugging mode (print on console)

    // coordinate(row, col) => index(0~23)
    public static int compute_index(int row, int col) {
        return row * COL_NUM + col;
    }

    // index(0~23) => coordinate row
    public static int compute_row(int index) {
        return index / COL_NUM;
    }

    // index(0~23) => coordinate col
    public static int compute_col(int index) {
        return index % COL_NUM;
    }

    // all red and obstacles are found (saved in found_red and found_obstacles)
    public static boolean is_done() {
        return (found_red[0] != -1 && found_red[1] != -1) &&
            (found_obstacles[0] != -1 && found_obstacles[1] != -1);
    }

    // check whether cell with index(0~23) is red
    public static boolean is_red(int index) {
        return red[0] == index || red[1] == index;
    }

    // check whether cell with index(0~23) is obstacle
    public static boolean is_obstacle(int index) {
        return obstacles[0] == index || obstacles[1] == index;
    }

    // initialize grid state, robot state, etc
    public static void init_grid() {
        for(int r = 0; r < ROW_NUM; r++) {
            for(int c = 0; c < COL_NUM; c++) {
                int index = compute_index(r, c);
                grid[index] = -1;
            }
        }
        cnt = 0;
        cur_pos = 0;
        cur_dir = 0;
        old_dir = 0;
        found_red[0] = -1;
        found_red[1] = -1;
        found_obstacles[0] = -1;
        found_obstacles[1] = -1;
    }

    public static void print_grid() {
        for(int r = ROW_NUM - 1; r >= 0; r--) {
            for(int c = 0; c < COL_NUM; c++) {
                int index = compute_index(r, c);
                System.out.printf("%3d ", grid[index]);
            }
            System.out.printf("\n");
        }
        System.out.printf("\n");
        return;
    }

    // print results of red tiles and box positions
    public static void print_results() {
       if(!start_from_0) {
          found_red[0] = ROW_NUM * COL_NUM - 1 - found_red[0];
          found_red[1] = ROW_NUM * COL_NUM - 1 - found_red[1];
          found_obstacles[0] = ROW_NUM * COL_NUM - 1 - found_obstacles[0];
          found_obstacles[1] = ROW_NUM * COL_NUM - 1 - found_obstacles[1];          
       }
        System.out.printf("(%d, %d, R)\n", compute_col(found_red[0]), compute_row(found_red[0]));
        System.out.printf("(%d, %d, R)\n", compute_col(found_red[1]), compute_row(found_red[1]));
        System.out.printf("(%d, %d, B)\n", compute_col(found_obstacles[0]), compute_row(found_obstacles[0]));
        System.out.printf("(%d, %d, B)\n", compute_col(found_obstacles[1]), compute_row(found_obstacles[1]));
    }

    // calculate which index(0~23) to move based on current index(0~23) and direction("E", "W", "S", "N")
    public static int calculate_next_pos(int cur_pos, int cur_dir) {
        int[] row_diff = {0, 0, -1, +1};
        int[] col_diff = {+1, -1, 0, 0};
    
        int cur_row = compute_row(cur_pos);
        int cur_col = compute_col(cur_pos);
        int next_row = cur_row + row_diff[cur_dir];
        int next_col = cur_col + col_diff[cur_dir];
        if(next_row < 0 || next_row >= ROW_NUM || next_col < 0 || next_col >= COL_NUM) return -1;
    
        int next_pos = compute_index(next_row, next_col);
        return next_pos;
    }

    // calculate next direction to face in order to move around the detected box clockwise
    public static int rotate_clockwise(int pos, int dir, int obstacle) {
        int coor_row = compute_row(pos);
        int coor_col = compute_col(pos);
        int obst_row = compute_row(obstacle);
        int obst_col = compute_col(obstacle);
        int row_diff = obst_row - coor_row;
        int col_diff = obst_col - coor_col;
        if(row_diff == 0 && col_diff == 1) return 3; // "N"
        else if(row_diff == -1 && col_diff == 1) return 0; // "E"
        else if(row_diff == -1 && col_diff == 0) return 0; // "E"
        else if(row_diff == -1 && col_diff == -1) return 2; // "S"
        else if(row_diff == 0 && col_diff == -1) return 2; // "S"
        else if(row_diff == 1 && col_diff == -1) return 1; // "W"
        else if(row_diff == 1 && col_diff == 0) return 1; // "W"
        else if(row_diff == 1 && col_diff == 1) return 3; // "N"
        return -1;
    }
    
    // calculate next direction to face in order to move around the detected box counterclockwise
    public static int rotate_counter_clockwise(int pos, int dir, int obstacle) {
        int coor_row = compute_row(pos);
        int coor_col = compute_col(pos);
        int obst_row = compute_row(obstacle);
        int obst_col = compute_col(obstacle);
        int row_diff = obst_row - coor_row;
        int col_diff = obst_col - coor_col;
        if(row_diff == 0 && col_diff == 1) return 2; // "S"
        else if(row_diff == 1 && col_diff == 1) return 0; // "E"
        else if(row_diff == 1 && col_diff == 0) return 0; // "E"
        else if(row_diff == 1 && col_diff == -1) return 3; // "N"
        else if(row_diff == 0 && col_diff == -1) return 3; // "N"
        else if(row_diff == -1 && col_diff == -1) return 1; // "W"
        else if(row_diff == -1 && col_diff == 0) return 1; // "W"
        else if(row_diff == -1 && col_diff == 1) return 2; // "S"
        return -1;
    }

    // algorithm to move the robot to find red tiles and boxes
    public static int algorithm() {
        int cur_row = 0;
        int cur_col = 0;
        int next_pos;
        int cur_obstacle = -1;
        int default_pos = -1;
        int row_dir = -1;
        cnt = 0;

        while(true) {
            grid[cur_pos] = cnt;
            cur_row = compute_row(cur_pos);
            cur_col = compute_col(cur_pos);

            if(cur_obstacle == -1) { // default mode: following default route( (0, 0) => (5, 0) => (5, 1) => (0, 1) => (0, 2) => ... ) 
                if(cur_row % 2 == 0) {
                    if(cur_col == COL_NUM - 1) cur_dir = 3; // "N"
                    else cur_dir = 0; // "E"
                } else {
                    if(cur_col == 0) cur_dir = 3; // "N"
                    else cur_dir = 1; // "W"
                }
            } else { // obstacle mode: avoiding box (position of box saved in cur_obstacle)
                if(row_dir == 0) cur_dir = rotate_clockwise(cur_pos, cur_dir, cur_obstacle);
                else cur_dir = rotate_counter_clockwise(cur_pos, cur_dir, cur_obstacle);
            }

            // check if current position is red tile
            if(robot_mode ? robot.red() : is_red(cur_pos)) {
                if(found_red[0] == -1) found_red[0] = cur_pos;
                else if(found_red[0] != cur_pos) found_red[1] = cur_pos;
                grid[cur_pos] = -88;
            }
            if(is_done()) break;

            // calculate next position and check if it is possible
            next_pos = calculate_next_pos(cur_pos, cur_dir);
            if(next_pos == -1) {
                if(cur_obstacle != -1) row_dir = (row_dir == 0) ? 1 : 0;

                // handle extreme cases (the robot cannot come back to the default path. position saved in default_pos)                
                if(found_obstacles[0] == 4 && found_obstacles[1] == 11 && default_pos == 5) {
                    grid[default_pos] = 99;
                    default_pos = 9;
                    continue;
                }
                if(found_obstacles[0] == 12 && found_obstacles[1] == 19 && default_pos == 18) {
                    grid[default_pos] = 99;
                    default_pos = 14;
                    continue;
                }
                if(found_obstacles[0] == 17 && found_obstacles[1] == 22 && default_pos == 23) {
                    grid[default_pos] = 99;
                    default_pos = 21;
                    continue;
                }
                continue;
            }
            
            if(old_dir != cur_dir) {
               if(robot_mode) robot.turn(old_dir, cur_dir);
               cnt++;
            }
            old_dir = cur_dir;

            if(debug) {
                System.out.printf("(%d, %d)\n", cur_row, cur_col);
                System.out.printf("direction: %c\n", cur_dir == 0 ? 'E' : cur_dir == 1 ? 'W' : cur_dir == 2 ? 'S' : 'N');    
                print_grid();
            }

            // check if next position is box
            if(robot_mode ? robot.obstacle() : is_obstacle(next_pos)) {
                if(debug) System.out.printf("obstacle!\n");
                cur_obstacle = next_pos;

                if(row_dir == -1) {
                    if(cur_row % 2 == 0) row_dir = 0;
                    else row_dir = 1;
                }

                int obstacle_row = compute_row(cur_obstacle);
                if(default_pos == -1 || next_pos == default_pos) {
                    default_pos = calculate_next_pos(cur_obstacle, (obstacle_row % 2 == 0) ? 0 : 1);
                    if(default_pos == -1) default_pos = calculate_next_pos(cur_obstacle, 3); // "N"
                }

                if(found_obstacles[0] == -1) found_obstacles[0] = next_pos;
                else if (found_obstacles[0] != next_pos) found_obstacles[1] = next_pos;
                grid[next_pos] = -99;
                continue;
            }
            if(is_done()) break;

            // when to alter back to default mode from obstacle mode
            if(next_pos == default_pos) {
                cur_obstacle = -1;
                row_dir = -1; 
                default_pos = -1;  
            }

            // move robot to next position
            cur_pos = next_pos;
            if(robot_mode) robot.forward();
            cnt++;
        }
        return 0;
    }

    // algorithm to move the robot back to the starting position
    public static void return_to_start() {
        int cur_row = 0;
        int cur_col = 0;
        int next_pos;
        int cur_obstacle = -1;
        int default_pos = -1;
        int row_dir = -1;
        
        while(cur_pos != 0) {
            if(grid[cur_pos] >= -1) grid[cur_pos] = cnt;
            cur_row = compute_row(cur_pos);
            cur_col = compute_col(cur_pos);
            
            if(cur_obstacle == -1) { // default mode: following default route (go straight west, go straight down)
                cur_dir = 1; // "W"
                if(cur_col == 0) cur_dir = 2; // "S"
            } else { // obstacle mode: avoiding box (position of box saved in cur_obstacle)
                if(row_dir == 0) cur_dir = rotate_clockwise(cur_pos, cur_dir, cur_obstacle);
                else cur_dir = rotate_counter_clockwise(cur_pos, cur_dir, cur_obstacle);
            }

            // calculate next position and check if it is possible
            next_pos = calculate_next_pos(cur_pos, cur_dir);
            if(next_pos == -1) {
                if(cur_obstacle != -1) row_dir = (row_dir == 0) ? 1 : 0;
                continue;
            }
            
            if(old_dir != cur_dir) {
               if(robot_mode) robot.turn(old_dir, cur_dir);
               cnt++;
            }
            old_dir = cur_dir;

            if(debug) {
                System.out.printf("(%d, %d)\n", cur_row, cur_col);
                System.out.printf("direction: %c\n", cur_dir == 0 ? 'E' : cur_dir == 1 ? 'W' : cur_dir == 2 ? 'S' : 'N');    
                print_grid();
            }

            // check if next position is box
            if(next_pos == found_obstacles[0] || next_pos == found_obstacles[1]) {
                if(debug) System.out.printf("obstacle!\n");
                cur_obstacle = next_pos;

                if(row_dir == -1) row_dir = 0;

                if(default_pos == -1 || next_pos == default_pos) {
                    default_pos = calculate_next_pos(cur_obstacle, 1); // "W"
                    if(default_pos == -1) default_pos = calculate_next_pos(cur_obstacle, 2); // "S"
                }
                continue;
            }

            // when to alter back to default mode from obstacle mode
            if(next_pos == default_pos) {
                cur_obstacle = -1;
                row_dir = -1;
                default_pos = -1;  
            }

            // move robot to next position
            cur_pos = next_pos;
            if(robot_mode) robot.forward();
            cnt++;
        }
    }

    // receive user input to init settings for start position
    public static void initialize() {
        // get button input for start position
        while(true) {
            String print_str = "";
            keys.waitForAnyPress();
            lcd.clear();

            // be able to change start position when ENTER key is pressed
            if(keys.getButtons() == Keys.ID_ENTER) {
                if(start_from_0) {
                    start_from_0 = false;
                    print_str = "Start from (5,3)";
                } else {
                    start_from_0 = true;
                    print_str = "Start from (0,0)";
                }
                lcd.drawString(print_str, 1, 4);
            }

            // select start position and break while loop when ESCAPE key is pressed
            if(keys.getButtons() == Keys.ID_ESCAPE) break;
        }
    }

    public static void main(String[] args) {
        // debugging settings
        debug = false;
        robot_mode = true;
        if(robot_mode) robot = new robot();

        // initial preparation
        System.out.println("START");
        initialize();
        init_grid();
        if(!robot_mode) {
            obstacles[0] = 15;
            obstacles[1] = 20;
            red[0] = 4;
            red[1] = 18;
        }

        // main program
        int result = algorithm();
        return_to_start();
        print_results();

        // end program
        while(true) {
            if(keys.getButtons() == Keys.ID_ESCAPE) break;
        }
    }
}