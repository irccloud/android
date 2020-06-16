/*
 * Copyright (c) 2020 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Comparator for ordering by Collator while treating digits numerically.
 * This provides a "natural" order that humans usually perceive as 'logical'.
 *
 * It should work reasonably well for western languages (provided you
 * use the proper collator when constructing). For free control over the
 * Collator, use the constructor that takes a Collator as parameter.
 * Configure the Collator using Collator.setDecomposition()/setStrength()
 * to suit your requirements.
 *
 * Source: http://stackoverflow.com/a/12642970/1406639
 */

package com.irccloud.android;

import java.text.Collator;
import java.util.Comparator;

public class AlphanumComparator implements Comparator<CharSequence> {

    /**
     * The collator used for comparison of the alpha part
     */
    private final Collator collator;

    /**
     * Create comparator using platform default collator.
     * (equivalent to using Collator.getInstance())
     */
    public AlphanumComparator() {
        this(Collator.getInstance());
    }

    /**
     * Create comparator using specified collator
     */
    public AlphanumComparator(final Collator collator) {
        if (collator == null)
            throw new IllegalArgumentException("collator must not be null");
        this.collator = collator;
    }

    /**
     * Ideally this would be generalized to Character.isDigit(), but I have
     * no knowledge about arabic language and other digits, so I treat
     * them as characters...
     */
    private static boolean isDigit(final int character) {
        // code between ASCII '0' and '9'?
        return character >= 48 && character <= 57;
    }

    /**
     * Get subsequence of only characters or only digits, but not mixed
     */
    private static CharSequence getChunk(final CharSequence charSeq, final int start) {
        int index = start;
        final int length = charSeq.length();
        final boolean mode = isDigit(charSeq.charAt(index++));
        while (index < length) {
            if (isDigit(charSeq.charAt(index)) != mode)
                break;
            ++index;
        }
        return charSeq.subSequence(start, index);
    }

    /**
     * Implements Comparator<CharSequence>.compare
     */
    public int compare(final CharSequence charSeq1, final CharSequence charSeq2) {
        final int length1 = charSeq1.length();
        final int length2 = charSeq2.length();
        int index1 = 0;
        int index2 = 0;
        int result = 0;
        while (result == 0 && index1 < length1 && index2 < length2) {
            final CharSequence chunk1 = getChunk(charSeq1, index1);
            index1 += chunk1.length();

            final CharSequence chunk2 = getChunk(charSeq2, index2);
            index2 += chunk2.length();

            if (isDigit(chunk1.charAt(0)) && isDigit(chunk2.charAt(0))) {
                final int clen1 = chunk1.length();
                final int clen2 = chunk2.length();
                // count and skip leading zeros
                int zeros1 = 0;
                while (zeros1 < clen1 && chunk1.charAt(zeros1) == '0')
                    ++zeros1;
                // count and skip leading zeros
                int zeros2 = 0;
                while (zeros2 < clen2 && chunk2.charAt(zeros2) == '0')
                    ++zeros2;
                // the longer run of non-zero digits is greater
                result = (clen1 - zeros1) - (clen2 - zeros2);
                // if the length is the same, the first differing digit decides
                // which one is deemed greater.
                int subi1 = zeros1;
                int subi2 = zeros2;
                while (result == 0 && subi1 < clen1 && subi2 < clen2) {
                    result = chunk1.charAt(subi1++) - chunk2.charAt(subi2++);
                }
                // if still no difference, the longer zeros-prefix is greater
                if (result == 0)
                    result = subi1 - subi2;
            } else {
                // in case we are working with Strings, toString() doesn't create
                // any objects (String.toString() returns the same string itself).
                result = collator.compare(chunk1.toString(), chunk2.toString());
            }
        }
        // if there was no difference at all, let the longer one be the greater one
        if (result == 0)
            result = length1 - length2;
        // limit result to (-1, 0, or 1)
        return Integer.signum(result);
    }
}