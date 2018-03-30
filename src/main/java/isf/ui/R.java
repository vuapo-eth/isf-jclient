package isf.ui;

import java.util.*;


public class R {
    public static final ResourceBundle STR;
    public static final ResourceBundle URL = ResourceBundle.getBundle("urls");

    static {
        ResourceBundle str;

        try {
            str = ResourceBundle.getBundle("strings");
        } catch (MissingResourceException e) {
            str = ResourceBundle.getBundle("strings", new Locale("en"));
        }

        System.out.println("Test");

        STR = str;
    }
}