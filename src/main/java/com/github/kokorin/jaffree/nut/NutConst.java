/*
 *    Copyright  2017 Denis Kokorin
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.github.kokorin.jaffree.nut;

/**
 * Nut format constants.
 */
@SuppressWarnings("checkstyle:magicnumber")
public final class NutConst {
    public static final String FILE_ID = "nut/multimedia container";
    public static final byte[] FILE_ID_BYTES = toCStr(FILE_ID);

    public static final long MAIN_STARTCODE = toStartCode(0x7A561F5F04ADL, 'M');
    public static final long STREAM_STARTCODE = toStartCode(0x11405BF2F9DBL, 'S');
    public static final long SYNCPOINT_STARTCODE = toStartCode(0xE4ADEECA4569L, 'K');
    public static final long INDEX_STARTCODE = toStartCode(0xDD672F23E64EL, 'X');
    public static final long INFO_STARTCODE = toStartCode(0xAB68B596BA78L, 'I');

    private NutConst() {
    }

    private static byte[] toCStr(final String value) {
        byte[] result = new byte[value.length() + 1];
        for (int i = 0; i < value.length(); i++) {
            result[i] = (byte) value.charAt(i);
        }
        return result;
    }

    private static long toStartCode(final long code, final char c) {
        return code + (((((long) 'N') << 8) + (long) c) << 48);
    }
}
