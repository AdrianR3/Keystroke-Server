package me.awesome.robot;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class ActivationController implements FocusListener {

    private static Robot robot = null;


    public static boolean create() {

        if (robot == null) {
            try {
                robot = new Robot();
                robot.setAutoDelay(100);

//                BufferedImage image = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else {
            return false;
        }


    }

    private static boolean isCreated() {
        return robot != null;
    }

    public static void go() {
        if (!isCreated()) {
            create();
            robot.delay(1000 * 3);
        }

        int x = robot.getAutoDelay();
        robot.setAutoDelay(0);
        sendKeys(robot, "GO");
        robot.setAutoDelay(x);

//        robot.delay(1000*5);
//        robot.keyPress(KeyEvent.VK_E);
//        robot.keyRelease(KeyEvent.VK_E);

//        robot.setAutoDelay(100);
//        robot.keyPress(KeyEvent.VK_META); //press command
//        robot.keyPress(KeyEvent.VK_T); //press l
//        robot.keyRelease(KeyEvent.VK_META); //release command
//        robot.keyRelease(KeyEvent.VK_T); //release l
//
//        robot.setAutoDelay(10);
//        sendKeys(robot, "Hello, world!");
    }

    public void focusGained(FocusEvent e) {
        System.out.println("Focus gained");
    }

    public void focusLost(FocusEvent e) {
        System.out.println("Focus lost");
    }

    static void sendKeys(Robot robot, String keys) {
        for (char c : keys.toCharArray()) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (KeyEvent.CHAR_UNDEFINED == keyCode) {
                throw new RuntimeException(
                        "Key code not found for character '" + c + "'");
            }
            robot.keyPress(keyCode);
//            robot.delay(100);
            robot.keyRelease(keyCode);
//            robot.delay(100);
        }
    }
}
