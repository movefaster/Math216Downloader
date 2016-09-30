/*
 * Copyright [2016] [Morton Mo]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by Morton on 9/30/16.
 * A collection of helper methods used in this project
 */
public class Utils {

    /**
     * Retrieves an arbitrary part after splitting a string using the given regex.
     * If n is negative, it will retrieve the last n-th part of the string
     * (similar to python syntax array[-n]).
     * @param s the string to be split
     * @param regex the regular expression to split the string
     * @param n the n-th part of the string to be retrieved. If it's negative, the
     *          last n-th part
     * @return the n-th part, or last n-th part, of a given string split by regex
     */

    public static String getStringPart(String s, String regex, int n) {
        String[] parts = s.split(regex);
        if (Math.abs(n) >= parts.length) throw new IllegalArgumentException(String.format("N cannot be larger" +
                " than the length (%d) of the split string.", parts.length));
        if (n >= 0) return parts[n];
        else return parts[parts.length + n];
    }
}
