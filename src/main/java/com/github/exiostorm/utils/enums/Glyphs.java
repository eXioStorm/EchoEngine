package com.github.exiostorm.utils.enums;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public enum Glyphs {
    ENGLISH(createFullEnglishUnicodeList());

    public final List<Integer> unicode;
    Glyphs(List<Integer> unicode) {
        this.unicode = unicode;
    }
    private static List<Integer> createFullEnglishUnicodeList() {
        List<Integer> unicodeList = new ArrayList<>();

        // Standard keyboard symbols
        unicodeList.addAll(Arrays.asList(
                0x00A7, //TODO § Section sign (move this to primary logic as it's going to be used universally)
                0x0060, // Backtick `
                0x0031, // One 1
                0x0032, // Two 2
                0x0033, // Three 3
                0x0034, // Four 4
                0x0035, // Five 5
                0x0036, // Six 6
                0x0037, // Seven 7
                0x0038, // Eight 8
                0x0039, // Nine 9
                0x0030, // Zero 0
                0x002D, // Minus -
                0x003D, // Equals =
                0x005B, // Left Bracket [
                0x005D, // Right Bracket ]
                0x005C, // Backslash \
                0x003B, // Semicolon ;
                0x0027, // Quote '
                0x002C, // Comma ,
                0x002E, // Period .
                0x002F, // Slash /
                0x0020, // Space
                0x007E, // Tilde ~
                0x0021, // Exclamation !
                0x0040, // At @
                0x0023, // Pound #
                0x0024, // Dollar $
                0x0025, // Percent %
                0x005E, // Caret ^
                0x0026, // Ampersand &
                0x002A, // Asterisk *
                0x0028, // Left Paren (
                0x0029, // Right Paren )
                0x005F, // Underscore _
                0x002B, // Plus +
                0x007B, // Left Brace {
                0x007D, // Right Brace }
                0x007C, // Pipe |
                0x003A, // Colon :
                0x0022, // Double Quote "
                0x003C, // Less Than <
                0x003E, // Greater Than >
                0x003F  // Question ?
        ));

        // Lowercase letters
        for (int i = 'a'; i <= 'z'; i++) { unicodeList.add(i); }
        // Uppercase letters
        for (int i = 'A'; i <= 'Z'; i++) { unicodeList.add(i); }

        return unicodeList;
    }
}
