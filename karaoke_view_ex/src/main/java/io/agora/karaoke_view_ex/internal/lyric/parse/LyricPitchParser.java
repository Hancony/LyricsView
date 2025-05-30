package io.agora.karaoke_view_ex.internal.lyric.parse;

import android.text.TextUtils;

import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import io.agora.karaoke_view_ex.constants.Constants;
import io.agora.karaoke_view_ex.internal.constants.LyricType;
import io.agora.karaoke_view_ex.internal.model.LyricsLineModel;
import io.agora.karaoke_view_ex.internal.model.PitchData;
import io.agora.karaoke_view_ex.internal.model.XmlPitchData;
import io.agora.karaoke_view_ex.internal.utils.LogUtils;
import io.agora.karaoke_view_ex.internal.utils.Utils;
import io.agora.karaoke_view_ex.model.LyricModel;

/**
 * Lyrics and pitch data loader and parser.
 * This class provides methods to load and parse lyrics files along with their
 * corresponding pitch data for karaoke applications.
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public class LyricPitchParser {
    /**
     * Regular expression pattern for matching LRC format lines
     */
    public static final Pattern LRC_PATTERN_LINE = Pattern.compile("((\\[\\d{2}:\\d{2}\\.\\d{2,3}])+)(.+)");

    /**
     * Regular expression pattern for matching KRC format lines
     */
    public static final Pattern KRC_PATTERN_LINE = Pattern.compile("\\[(\\w+):([^]]*)]");

    /**
     * Validates file parameters for parsing
     *
     * @param file The file to validate
     * @throws IllegalArgumentException if the file is invalid
     */
    private static void checkFileParameters(File file) {
        if (file == null || !file.isFile() || !file.exists() || !file.canRead() || file.length() == 0) {
            StringBuilder builder = new StringBuilder("Not a valid file for parser: " + file);
            if (file != null) {
                builder.append("\n");
                builder.append("{isFile: ").append(file.isFile()).append(", ");
                builder.append("exists: ").append(file.exists()).append(", ");
                builder.append("canRead: ").append(file.canRead()).append(", ");
                builder.append("length: ").append(file.length()).append("}");
            }
            throw new IllegalArgumentException(builder.toString());
        }
    }

    /**
     * Determines the lyrics file type based on file extension or content
     *
     * @param lyricFile The lyrics file to analyze
     * @return The detected LyricType
     */
    private static LyricType probeLyricsFileType(File lyricFile) {
        LyricType type = LyricType.LRC;
        String fileName = lyricFile.getName();
        LogUtils.i("probeLyricsFileType fileName: " + fileName);
        if (TextUtils.isEmpty(fileName)) {
            return type;
        }
        if (fileName.endsWith(Constants.FILE_EXTENSION_XML)) {
            type = LyricType.XML;
        } else if (fileName.endsWith(Constants.FILE_EXTENSION_LRC)) {
            type = LyricType.LRC;
        } else if (fileName.endsWith(Constants.FILE_EXTENSION_KRC)) {
            type = LyricType.KRC;
        } else {
            type = probeLyricsFileType(Utils.getFileBytes(lyricFile));
        }
        return type;
    }

    /**
     * Determines the lyrics file type based on file content
     *
     * @param lyricData The byte array containing lyrics file data
     * @return The detected LyricType
     */
    private static LyricType probeLyricsFileType(byte[] lyricData) {
        LyricType type = LyricType.LRC;
        try {
            String fileContent = new String(lyricData, Constants.UTF_8);
            String[] lines = fileContent.split("\\n|\\r\\n");
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                if (!TextUtils.isEmpty(firstLine)) {
                    firstLine = Utils.removeStringBom(firstLine);
                    if (firstLine.contains(Constants.FILE_EXTENSION_XML) || firstLine.contains("<song>")) {
                        type = LyricType.XML;
                    } else if (LRC_PATTERN_LINE.matcher(firstLine).matches()) {
                        return LyricType.LRC;
                    } else if (KRC_PATTERN_LINE.matcher(firstLine).matches()) {
                        type = LyricType.KRC;
                    } else {
                        LogUtils.i("probeLyricsFileType unknown lyric type");
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("probeLyricsFileType error: " + e.getMessage());
        }
        return type;
    }

    /**
     * Parses lyrics and pitch data from files
     *
     * @param lyricFile                The lyrics file
     * @param pitchFile                The pitch data file
     * @param includeCopyrightSentence Whether to include copyright sentences in the output
     * @param lyricOffset              Time offset to apply to lyrics (in milliseconds)
     * @return A LyricModel containing the parsed lyrics and pitch data
     */
    public static LyricModel parseFile(File lyricFile, File pitchFile, boolean includeCopyrightSentence, int lyricOffset) {
        checkFileParameters(lyricFile);
        LyricType type = probeLyricsFileType(lyricFile);
        if (type == LyricType.KRC) {
            return parseKrcLyricData(Utils.getFileBytes(lyricFile), Utils.getFileBytes(pitchFile), includeCopyrightSentence, lyricOffset);
        } else if (type == LyricType.LRC) {
            return parseLrcLyricData(Utils.getFileBytes(lyricFile), Utils.getFileBytes(pitchFile), includeCopyrightSentence, lyricOffset);
        } else if (type == LyricType.XML) {
            return parseXmlLyricData(Utils.getFileBytes(lyricFile), Utils.getFileBytes(pitchFile), includeCopyrightSentence, lyricOffset);
        } else {
            LogUtils.e("Do not support the lyrics file type " + type);
        }

        return null;
    }

    /**
     * Parses lyrics and pitch data from byte arrays
     *
     * @param lyricData                The byte array containing lyrics data
     * @param pitchData                The byte array containing pitch data
     * @param includeCopyrightSentence Whether to include copyright sentences in the output
     * @param lyricOffset              Time offset to apply to lyrics (in milliseconds)
     * @return A LyricModel containing the parsed lyrics and pitch data
     */
    public static LyricModel parseLyricData(byte[] lyricData, byte[] pitchData, boolean includeCopyrightSentence, int lyricOffset) {
        LyricType type = probeLyricsFileType(lyricData);
        if (type == LyricType.KRC) {
            return parseKrcLyricData(lyricData, pitchData, includeCopyrightSentence, lyricOffset);
        } else if (type == LyricType.LRC) {
            return parseLrcLyricData(lyricData, pitchData, includeCopyrightSentence, lyricOffset);
        } else if (type == LyricType.XML) {
            return parseXmlLyricData(lyricData, pitchData, includeCopyrightSentence, lyricOffset);
        } else {
            LogUtils.e("Do not support the lyrics file type " + type);
        }
        return null;
    }

    /**
     * Parses KRC format lyrics and pitch data
     *
     * @param krcData                  The byte array containing KRC lyrics data
     * @param pitchData                The byte array containing pitch data
     * @param includeCopyrightSentence Whether to include copyright sentences in the output
     * @param lyricOffset              Time offset to apply to lyrics (in milliseconds)
     * @return A LyricModel containing the parsed lyrics and pitch data
     */
    public static LyricModel parseKrcLyricData(byte[] krcData, byte[] pitchData, boolean includeCopyrightSentence, int lyricOffset) {
        LyricModel lyricsModel = LyricParser.doParseKrc(krcData, lyricOffset);
        List<PitchData> pitchDataList = PitchParser.doParseKrc(pitchData);
        lyricsModel.pitchDataList = pitchDataList;
        lyricsModel.hasPitch = pitchDataList != null && !pitchDataList.isEmpty();
        if (lyricsModel.hasPitch) {
            lyricsModel.preludeEndPosition = pitchDataList.get(0).startTime;
        }

        // Remove copyright information sentences
        if (!includeCopyrightSentence && lyricsModel.lines != null && !lyricsModel.lines.isEmpty()) {
            ListIterator<LyricsLineModel> iterator = lyricsModel.lines.listIterator();
            while (iterator.hasNext()) {
                LyricsLineModel element = iterator.next();
                if (element.getEndTime() < lyricsModel.preludeEndPosition) {
                    lyricsModel.copyrightSentenceLineCount++;
                    iterator.remove();
                } else {
                    break;
                }
            }
        }

        return lyricsModel;
    }

    /**
     * Parses LRC format lyrics and pitch data
     *
     * @param lyricData                The byte array containing LRC lyrics data
     * @param pitchData                The byte array containing pitch data
     * @param includeCopyrightSentence Whether to include copyright sentences in the output
     * @param lyricOffset              Time offset to apply to lyrics (in milliseconds)
     * @return A LyricModel containing the parsed lyrics and pitch data
     */
    private static LyricModel parseLrcLyricData(byte[] lyricData, byte[] pitchData, boolean includeCopyrightSentence, int lyricOffset) {
        XmlPitchData pitchesModel = null;
        if (pitchData != null) {
            pitchesModel = PitchParser.doParseXml(pitchData);
        }

        LyricModel model = LyricParser.doParseLrc(lyricData);
        if (model == null) {
            return null;
        }
        model.hasPitch = pitchesModel != null && !pitchesModel.pitches.isEmpty();

        // Replace tones and set the pitch value
        // Each tone lasts for 100ms
        for (int i = 0; i < model.lines.size(); i++) {
            LyricsLineModel line = model.lines.get(i);
            List<LyricsLineModel.Tone> tones = line.tones;
            if (null != tones && !tones.isEmpty()) {
                // Figure out how many tones need to be added and do it
                for (int j = 0; j < tones.size(); j++) {
                    LyricsLineModel.Tone tone = tones.get(j);
                    if (j < tones.size() - 1) {
                        tone.end = tones.get(j + 1).begin;
                    } else {
                        if (i < model.lines.size() - 1) {
                            tone.end = model.lines.get(i + 1).tones.get(0).begin;
                        } else {
                            // Last note
                            tone.end = tone.begin + (100 - 1);
                        }
                    }
                    tone.pitch = (int) PitchParser.fetchPitchWithRange(pitchesModel, model.preludeEndPosition, tone.begin, tone.end);
                    if (tone.pitch > 0 && !model.hasPitch) {
                        model.hasPitch = true;
                    }
                }
            }
        }
        return model;
    }

    /**
     * Parses XML format lyrics and pitch data
     *
     * @param lyricData                The byte array containing XML lyrics data
     * @param pitchData                The byte array containing pitch data
     * @param includeCopyrightSentence Whether to include copyright sentences in the output
     * @param lyricOffset              Time offset to apply to lyrics (in milliseconds)
     * @return A LyricModel containing the parsed lyrics and pitch data
     */
    private static LyricModel parseXmlLyricData(byte[] lyricData, byte[] pitchData, boolean includeCopyrightSentence, int lyricOffset) {
        XmlPitchData pitchesModel = null;
        if (pitchData != null) {
            pitchesModel = PitchParser.doParseXml(pitchData);
        }

        LyricModel model = LyricParser.doParseXml(lyricData);
        if (model == null) {
            return null;
        }

        // Replace tones and set the pitch value
        // Each tone lasts for the specified time
        for (int i = 0; i < model.lines.size() - 1; i++) {
            LyricsLineModel cur = model.lines.get(i);
        }
        return model;
    }
}
