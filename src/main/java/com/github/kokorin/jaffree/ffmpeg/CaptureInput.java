/*
 *    Copyright  2020 Vicne, Denis Kokorin
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

package com.github.kokorin.jaffree.ffmpeg;

import com.github.kokorin.jaffree.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Input provides a live capture of your computer desktop as source.
 * <p>
 * Most of the information comes from https://trac.ffmpeg.org/wiki/Capture/Desktop
 * <p>
 * TODO list:
 * - Screen selection when multiscreen?
 * - Audio Capture?
 * - Warn if framerate is not set (like FrameInput)
 * - Call ffmpeg to return list of devices? list of screens?
 */
public abstract class CaptureInput<T extends CaptureInput<T>> extends BaseInput<T> implements Input {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureInput.class);

    /**
     * Create a DesktopCaptureInput suitable for your platform.
     *
     * @param screen (unused for now)
     */
    /*public CaptureInput(String screen) {
        if (OS.IS_LINUX) {
            setFormat("x11grab");
            input = ":0.0";
            setInput(input);
        } else if (OS.IS_MAC) {
            // Device list can be obtained with ffmpeg -f avfoundation -list_devices true -i ""
            setFormat("avfoundation");
            setInput("default:none");
            // For audio: setInput("default:default");
        } else if (OS.IS_WINDOWS) {
            if (useDirectShow) {
                // Using DirectShow
                // Device list can be obtained with ffmpeg -f dshow -list_devices true -i ""
                setFormat("dshow");
                setInput("video=\"screen-capture-recorder\"");
                // For audio: setInput("video=\"screen-capture-recorder\":audio=\"virtual-audio-capturer\"");
            } else {
                // Using GDI
                setFormat("gdigrab");
                setInput("desktop");
            }
        }
    }*/


    /**
     * Set capture frame rate.
     * <p>
     * Captures the desktop at the given frame rate
     * <p>
     * Be careful <b>not</b> to specify framerate with the "-r" parameter,
     * like this "ffmpeg -f dshow -r 7.5 -i video=XXX". This actually specifies that the devices
     * incoming PTS timestamps be <b>ignored</b> and replaced as if the device were running
     * at 7.5 fps [so it runs at default fps, but its timestamps are treated as if 7.5 fps].
     * This can cause the recording to appear to have "video slower than audio" or, under high
     * cpu load (if video frames are dropped) it will cause the video to fall "behind" the audio
     * [after playback of the recording is done, audio continues on--and gets highly out of sync,
     * video appears to go into "fast forward" mode during high cpu scenes].
     *
     * @param value Hz value, fraction or abbreviation
     * @return this
     */
    public CaptureInput<T> setCaptureFrameRate(Number value) {
        return addArguments("-framerate", String.valueOf(value));
    }

    public CaptureInput<T> setCaptureVideoSize(int width, int height) {
        return setCaptureVideoSize(width + "x" + height);
    }

    public CaptureInput<T> setCaptureVideoSize(String size) {
        return addArguments("-video_size", size);
    }

    public abstract CaptureInput<T> setCaptureVideoOffset(int xOffset, int yOffset);

    /**
     * Instructs ffmpeg to capture mouse cursor.
     * <p>
     * <b>Note</b>: this feature is not supported on all devices.
     *
     * @param captureCursor true to capture cursor
     * @return this
     */
    public abstract CaptureInput<T> setCaptureCursor(boolean captureCursor);

    // TODO check static method references
    public static <T extends CaptureInput<T>> CaptureInput<T> fromDesktop1() {
        CaptureInput<T> result = null;
        if (OS.IS_LINUX) {
            result = LinuxX11Grab.fromDesktop();
        } else if (OS.IS_MAC) {
            result = MacOsAvFoundation.fromDesktop();
        } else if (OS.IS_WINDOWS) {
            result = WindowsGdiGrab.fromDesktop();
        }

        if (result == null) {
            throw new RuntimeException("Could not detect OS");
        }

        return result;
    }

    /**
     * @see <a href="https://ffmpeg.org/ffmpeg-devices.html#dshow">dshow documentation</a>
     * @see <a href="https://trac.ffmpeg.org/wiki/DirectShow">dshow ffmepg wiki</a>
     */
    public static class WindowsDirectShow extends CaptureInput<WindowsDirectShow> {
        private static final Logger LOGGER = LoggerFactory.getLogger(WindowsDirectShow.class);

        public static WindowsDirectShow captureVideo(String videoDevice) {
            return captureVideoAndAudio(videoDevice, null);
        }

        public static WindowsDirectShow captureAudio(String audioDevice) {
            return captureVideoAndAudio(null, audioDevice);
        }

        public static WindowsDirectShow captureVideoAndAudio(String videoDevice, String audioDevice) {
            String input = "";
            if (videoDevice != null) {
                input = "video=" + videoDevice;
            }
            if (audioDevice != null) {
                if (!input.isEmpty()) {
                    input += ":";
                }
                input += "audio=" + audioDevice;
            }

            return new WindowsDirectShow()
                    .setInput(input)
                    .setFormat("dshow");
        }

        @Override
        public WindowsDirectShow setCaptureCursor(boolean captureCursor) {
            LOGGER.warn("Cursor capture option is not supported");
            return this;
        }

        @Override
        public WindowsDirectShow setCaptureVideoOffset(int xOffset, int yOffset) {
            LOGGER.warn("Capture offset option is not supported");
            return this;
        }
    }

    /**
     * @see <a href="https://ffmpeg.org/ffmpeg-devices.html#gdigrab">gdigrab documentation</a>
     */
    public static class WindowsGdiGrab extends CaptureInput<WindowsGdiGrab> {
        public static WindowsGdiGrab fromDesktop() {
            return new WindowsGdiGrab()
                    .setInput("desktop")
                    .setFormat("gdigrab");
        }

        public static WindowsGdiGrab fromWindow(String windowTitle) {
            return new WindowsGdiGrab()
                    .setInput("title=" + windowTitle)
                    .setFormat("gdigrab");
        }

        @Override
        public WindowsGdiGrab setCaptureCursor(boolean captureCursor) {
            String argValue = captureCursor ? "1" : "0";
            return addArguments("-draw_mouse", argValue);
        }

        @Override
        public WindowsGdiGrab setCaptureVideoOffset(int xOffset, int yOffset) {
            return addArguments("-offset_x", String.valueOf(xOffset))
                    .addArguments("-offset_y", String.valueOf(yOffset));
        }
    }

    /**
     * @see <a href="https://ffmpeg.org/ffmpeg-devices.html#avfoundation">avfoundation
     * documentation</a>
     */
    public static class MacOsAvFoundation extends CaptureInput<MacOsAvFoundation> {
        private static final Logger LOGGER = LoggerFactory.getLogger(MacOsAvFoundation.class);

        public static MacOsAvFoundation fromDesktop() {
            return fromVideoAndAudio("default", null);
        }

        public static MacOsAvFoundation fromVideoAndAudio(String videoDevice, String audioDevice) {
            if (videoDevice == null) {
                videoDevice = "none";
            }
            if (audioDevice == null) {
                audioDevice = "none";
            }
            return new MacOsAvFoundation()
                    .setInput(videoDevice + ":" + audioDevice)
                    .setFormat("avfoundation");
        }

        @Override
        public MacOsAvFoundation setCaptureCursor(boolean captureCursor) {
            String argValue = captureCursor ? "1" : "0";
            return addArguments("-capture_cursor", argValue);
        }

        @Override
        public MacOsAvFoundation setCaptureVideoOffset(int xOffset, int yOffset) {
            LOGGER.warn("Capture offset option is not supported");
            return this;
        }
    }

    /**
     * @see <a href="https://ffmpeg.org/ffmpeg-devices.html#x11grab">x11grab documentation</a>
     */
    public static class LinuxX11Grab extends CaptureInput<LinuxX11Grab> {
        public static LinuxX11Grab fromDesktop() {
            return fromDisplayAndScreen(0, 0);
        }

        public static LinuxX11Grab fromDisplayAndScreen(int display, int screen) {
            return fromHostDisplayAndScreen("", display, screen);
        }

        public static LinuxX11Grab fromHostDisplayAndScreen(String host, int display, int screen) {
            return new LinuxX11Grab()
                    .setInput(host + ":" + display + "." + screen)
                    .setFormat("x11grab");
        }

        @Override
        public LinuxX11Grab setCaptureCursor(boolean captureCursor) {
            String argValue = captureCursor ? "1" : "0";
            return addArguments("-draw_mouse", argValue);
        }

        @Override
        public LinuxX11Grab setCaptureVideoOffset(int xOffset, int yOffset) {
            return addArguments("-grab_x", String.valueOf(xOffset))
                    .addArguments("-grab_y", String.valueOf(yOffset));
        }
    }
}
