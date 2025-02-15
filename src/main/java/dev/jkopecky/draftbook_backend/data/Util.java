package dev.jkopecky.draftbook_backend.data;

import java.io.File;
import java.io.IOException;

public class Util {


    public static String toInternalResource(String target) {
        String output = "";
        for (char c : target.toCharArray()) {
            if (("" + c).matches("[a-zA-Z]")) {
                output += ("" + c).toLowerCase();
            } else if (("" + c).matches("\\s")) {
                output += "_";
            }
        }
        return output;
    }




    public static void recursiveDeleteFiles(String path) throws IOException {
        File target = new File(path);
        if (target.isDirectory()) {
            //call recursively on all children in directory
            for (File file : target.listFiles()) {
                recursiveDeleteFiles(file.getPath());
            }
        }

        target.delete();
    }
}
