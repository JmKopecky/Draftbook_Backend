package dev.jkopecky.draftbook_backend.data;

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
}
