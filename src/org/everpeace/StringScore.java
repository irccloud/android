package org.everpeace;
/**
 * This is a port of Joshaven Potter's string_score.js 0.1.10 to Java.
 * <p/>
 * string_score.js:
 * http://joshaven.com/string_score
 * https://github.com/joshaven/string_score
 * <p/>
 * Ported By: Shingo Omura <everpeace _at_ gmail _dot_ com>
 * Date: 11/03/23
 */
public class StringScore {
    public static double score(String string, String abbreviation) {
        return score(string, abbreviation, 0d);
    }

    public static double score(String string, String abbreviation, double fuzziness) {
        assert string != null && abbreviation != null;
        // If the string is equal to the abbreviation, perfect match.
        if (string.equals(abbreviation)) {
            return 1.0d;
        }
        //if it's not a perfect match and is empty return 0
        if (abbreviation.equals("")) {
            return 0d;
        }

        double stringLength = string.length();
        double abbreviationLength = abbreviation.length();
        double totalCharacterScore = 0d;
        double abbreviationScore = 0d;
        double fuzzies = 1d;
        double finalScore = 0d;
//        boolean shouldAwardCommonPrefixBonus = false;
        boolean startOfWordBonus = false;

        // Walk through abbreviation and add up scores.
        for (int i = 0; i < abbreviation.length(); i++) {
            int indexCLowercase, indexCUpperCase, minIndex, indexInString;
            double characterScore;
            // Find the first case-insensitive match of a character.
            String c = Character.toString(abbreviation.charAt(i));
            indexCLowercase = string.indexOf(c.toLowerCase());
            indexCUpperCase = string.indexOf(c.toUpperCase());
            minIndex = Math.min(indexCLowercase, indexCUpperCase);

            //Finds first valid occurrence
            //In upper or lowercase
            if (minIndex > -1) {
                indexInString = minIndex;
            } else {
                indexInString = Math.max(indexCLowercase, indexCUpperCase);
            }

            //If no value is found
            //Check if fuzzines is allowed
            if (indexInString == -1) {
                if (fuzziness > 0) {
                    fuzzies += 1 - fuzziness;
                    continue;
                } else {
                    return 0d;
                }
            } else {
                characterScore = 0.1d;
            }

            // Same case bonus.
            if (Character.toString(string.charAt(indexInString)).equals(c)) {
                characterScore += 0.1d;
            }

            // Consecutive letter & start-of-string Bonus
            if (indexInString == 0) {
                // Increase the score when matching first character of the
                // remainder of the string
                characterScore += 0.6d;
                if (i == 0) {
                    // If match is the first character of the string
                    // & the first character of abbreviation, add a
                    // start-of-string match bonus.
                    startOfWordBonus = true;
                }
            } else {
                // Acronym Bonus
                // Weighing Logic: Typing the first character of an acronym is as if you
                // preceded it with two perfect character matches.
                if (Character.toString(string.charAt(indexInString - 1)).equals(" ")) {
                    characterScore += 0.8d;
                }
            }
            // Left trim the already matched part of the string
            // (forces sequential matching).
            string = string.substring(indexInString + 1);
            totalCharacterScore += characterScore;
        }// end of for loop

        abbreviationScore = totalCharacterScore / abbreviationLength;

        // Reduce penalty for longer strings.
        finalScore = ((abbreviationScore * (abbreviationLength / stringLength)) + abbreviationScore) / 2;

        //Reduce using fuzzies;
        finalScore = finalScore / fuzzies;

        //Process start of string bonus
        if (startOfWordBonus && finalScore <= 0.85) {
            finalScore += 0.15;
        }
        return finalScore;
    }
}