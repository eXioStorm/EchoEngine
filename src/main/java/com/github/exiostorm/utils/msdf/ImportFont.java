package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;
import org.lwjgl.CLongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import org.lwjgl.util.freetype.*;


import java.nio.*;
import java.util.*;


import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.*;

import static org.lwjgl.system.MemoryStack.stackPush;

public final class ImportFont {

    // ------------------------------
    // FreeType library wrapper
    // ------------------------------

    public static final class FreetypeHandle implements AutoCloseable {
        private final long library;

        public FreetypeHandle() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pLib = stack.mallocPointer(1);
                int error = FT_Init_FreeType(pLib);
                if (error != 0) {
                    throw new RuntimeException("FT_Init_FreeType failed: " + error);
                }
                library = pLib.get(0);
            }
        }

        public long library() {
            return library;
        }

        @Override
        public void close() {
            FT_Done_FreeType(library);
        }
    }

    // ------------------------------
    // Font metrics
    // ------------------------------

    public static final class FontMetrics {
        public double emSize;
        public double ascenderY;
        public double descenderY;
        public double lineHeight;
        public double underlineY;
        public double underlineThickness;
    }

    // ------------------------------
    // Font handle wrapper
    // ------------------------------

    public static final class FontHandle implements AutoCloseable {
        private final FT_Face face;
        private final boolean ownership;

        public FontHandle(FreetypeHandle library, String filename) {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pFace = stack.mallocPointer(1);
                int error = FT_New_Face(library.library(), filename, 0, pFace);
                if (error != 0) {
                    throw new RuntimeException("FT_New_Face failed: " + error + " (" + filename + ")");
                }
                face = FT_Face.create(pFace.get(0));
                ownership = true;
            }
        }

        public FT_Face face() {
            return face;
        }

        @Override
        public void close() {
            if (ownership) {
                FT_Done_Face(face);
            }
        }
    }

    // ------------------------------
    // Coordinate scaling enum
    // ------------------------------

    public enum FontCoordinateScaling {
        FONT_SCALING_NONE,
        FONT_SCALING_EM_NORMALIZED,
        FONT_SCALING_LEGACY
    }

    // ------------------------------
    // Context for outline decomposition
    // ------------------------------

    private static final class FtContext {
        double scale;
        Vector2d position;
        MsdfShape shape;
        Contours.Contour contour;
    }

    // ------------------------------
    // Utility functions
    // ------------------------------
    private static Vector2d ftPoint2(FT_Vector vector, double scale) {
        return new Vector2d(scale * vector.x(), scale * vector.y());
    }

    private static double getFontCoordinateScale(FT_Face face, FontCoordinateScaling coordinateScaling) {
        switch (coordinateScaling) {
            case FONT_SCALING_NONE:
                return 1.0;
            case FONT_SCALING_EM_NORMALIZED:
                return 1.0 / (face.units_per_EM() != 0 ? face.units_per_EM() : 1);
            case FONT_SCALING_LEGACY:
                return 1.0 / 64.0; // MSDFGEN_LEGACY_FONT_COORDINATE_SCALE
            default:
                return 1.0;
        }
    }

    // Matches: #define DOUBLE_TO_F16DOT16(x) FT_Fixed(65536.*x)
    private static long DOUBLE_TO_F16DOT16(double x) {
        return (long)(65536.0 * x);
    }

    private static double F16DOT16_TO_DOUBLE(long x) {
        return x / 65536.0;
    }
    // FontVariationAxis mirrors the C++ struct of the same name
    public static final class FontVariationAxis {
        public String name;
        public double minValue;
        public double maxValue;
        public double defaultValue;
    }

    public static boolean setFontVariationAxis(
            FreetypeHandle library, FontHandle font,
            String name, double coordinate) {

        boolean success = false;
        FT_Face face = font.face();

        if ((face.face_flags() & FT_FACE_FLAG_MULTIPLE_MASTERS) != 0) {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pMaster = stack.mallocPointer(1);
                if (FT_Get_MM_Var(face, pMaster) != 0)
                    return false;

                FT_MM_Var master = FT_MM_Var.createSafe(pMaster.get(0));
                try {
                    if (master != null && master.num_axis() > 0) {
                        int numAxes = master.num_axis();
                        CLongBuffer coords = stack.mallocCLong(numAxes);

                        if (FT_Get_Var_Design_Coordinates(face, coords) == 0) {
                            FT_Var_Axis.Buffer axes = master.axis();
                            for (int i = 0; i < numAxes; i++) {
                                FT_Var_Axis axis = axes.get(i);
                                if (name.equals(axis.nameString())) {
                                    coords.put(i, DOUBLE_TO_F16DOT16(coordinate));
                                    success = true;
                                    break;
                                }
                            }
                        }

                        if (FT_Set_Var_Design_Coordinates(face, coords) != 0)
                            success = false;
                    }
                } finally {
                    // Mirrors: FT_Done_MM_Var(library->library, master)
                    FT_Done_MM_Var(library.library(), master);
                }
            }
        }
        return success;
    }

    public static boolean listFontVariationAxes(
            List<FontVariationAxis> axes,
            FreetypeHandle library, FontHandle font) {

        FT_Face face = font.face();
        if ((face.face_flags() & FT_FACE_FLAG_MULTIPLE_MASTERS) == 0)
            return false;

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pMaster = stack.mallocPointer(1);
            if (FT_Get_MM_Var(face, pMaster) != 0)
                return false;

            FT_MM_Var master = FT_MM_Var.createSafe(pMaster.get(0));
            try {
                if (master == null) return false;
                int numAxes = master.num_axis();
                axes.clear();
                FT_Var_Axis.Buffer ftAxes = master.axis();
                for (int i = 0; i < numAxes; i++) {
                    FT_Var_Axis src = ftAxes.get(i);
                    FontVariationAxis dst = new FontVariationAxis();
                    dst.name         = src.nameString();
                    dst.minValue     = F16DOT16_TO_DOUBLE(src.minimum());
                    dst.maxValue     = F16DOT16_TO_DOUBLE(src.maximum());
                    dst.defaultValue = F16DOT16_TO_DOUBLE(src.def());
                    axes.add(dst);
                }
            } finally {
                FT_Done_MM_Var(library.library(), master);
            }
            return true;
        }
    }

    // ------------------------------
    // FreeType outline callbacks
    // ------------------------------

    private static int ftMoveTo(long to, long user) {
        FtContext context = contextHolder.get();
        FT_Vector toVec = FT_Vector.create(to);

        if (!(context.contour != null && context.contour.edges.isEmpty())) {
            context.contour = new Contours.Contour();
            context.shape.addContour(context.contour);
        }
        context.position = ftPoint2(toVec, context.scale);
        return 0;
    }

    private static int ftLineTo(long to, long user) {
        FtContext context = contextHolder.get();
        FT_Vector toVec = FT_Vector.create(to);
        Vector2d endpoint = ftPoint2(toVec, context.scale);

        if (!endpoint.equals(context.position)) {
            context.contour.edges.add(new EdgeHolder(
                    EdgeSegment.create(context.position, endpoint,
                            new ColorHolder(EdgeColorEnum.WHITE.getValue().color))
            ));
            context.position = new Vector2d(endpoint);
        }
        return 0;
    }

    private static int ftConicTo(long control, long to, long user) {
        FtContext context = contextHolder.get();
        FT_Vector controlVec = FT_Vector.create(control);
        FT_Vector toVec = FT_Vector.create(to);
        Vector2d endpoint = ftPoint2(toVec, context.scale);

        if (!endpoint.equals(context.position)) {
            context.contour.edges.add(new EdgeHolder(
                    EdgeSegment.create(context.position, ftPoint2(controlVec, context.scale), endpoint,
                            new ColorHolder(EdgeColorEnum.WHITE.getValue().color))
            ));
            context.position = new Vector2d(endpoint);
        }
        return 0;
    }

    private static int ftCubicTo(long control1, long control2, long to, long user) {
        FtContext context = contextHolder.get();
        FT_Vector control1Vec = FT_Vector.create(control1);
        FT_Vector control2Vec = FT_Vector.create(control2);
        FT_Vector toVec = FT_Vector.create(to);
        Vector2d endpoint = ftPoint2(toVec, context.scale);
        Vector2d c1 = ftPoint2(control1Vec, context.scale);
        Vector2d c2 = ftPoint2(control2Vec, context.scale);
        if (!endpoint.equals(context.position) ||
                crossProduct(new Vector2d(c1).sub(endpoint),
                        new Vector2d(c2).sub(endpoint)) != 0.0) {
            context.contour.edges.add(new EdgeHolder(
                    EdgeSegment.create(context.position, c1, c2, endpoint,
                            new ColorHolder(EdgeColorEnum.WHITE.getValue().color))
            ));
            context.position = new Vector2d(endpoint);
        }
        return 0;
    }

    private static double crossProduct(Vector2d a, Vector2d b) {
        return a.x * b.y - a.y * b.x;
    }

    // Helper to store context as user pointer (implement based on your needs)
    private static final ThreadLocal<FtContext> contextHolder = new ThreadLocal<>();

    private static long storeUserObject(FtContext context) {
        contextHolder.set(context);
        return 0; // Return dummy value, actual context retrieved from ThreadLocal
    }

    // ------------------------------
    // Main outline reading function
    // ------------------------------
    public static int readFreetypeOutline(MsdfShape output, FT_Outline outline) {
        return readFreetypeOutline(output, outline, 1.0 / 64.0);
    }
    public static int readFreetypeOutline(MsdfShape output, FT_Outline outline, double scale) {
        output.contours.clear();
        //output.inverseYAxis = false;

        FtContext context = new FtContext();
        context.scale = scale;
        context.shape = output;
        context.contour = null;

        try (MemoryStack stack = stackPush()) {
            FT_Outline_Funcs ftFunctions = FT_Outline_Funcs.malloc(stack);
            ftFunctions.move_to(ImportFont::ftMoveTo);
            ftFunctions.line_to(ImportFont::ftLineTo);
            ftFunctions.conic_to(ImportFont::ftConicTo);
            ftFunctions.cubic_to(ImportFont::ftCubicTo);
            ftFunctions.shift(0);
            ftFunctions.delta(0);

            // not used, but method is necessary for the rest of the process.
            long userPtr = storeUserObject(context);
            int error = FT_Outline_Decompose(outline, ftFunctions, 0L);

            // Remove empty trailing contour if present
            if (!output.contours.isEmpty() &&
                    output.contours.get(output.contours.size() - 1).edges.isEmpty()) {
                output.contours.remove(output.contours.size() - 1);
            }

            return error;
        } finally {
            contextHolder.remove();
        }
    }

    public static boolean getFontMetrics(FontMetrics metrics, FontHandle font,
                                         FontCoordinateScaling coordinateScaling) {
        if (font == null) return false;
        FT_Face face = font.face();
        double scale = getFontCoordinateScale(face, coordinateScaling);
        metrics.emSize             = scale * face.units_per_EM();
        metrics.ascenderY          = scale * face.ascender();
        metrics.descenderY         = scale * face.descender();
        metrics.lineHeight         = scale * face.height();
        metrics.underlineY         = scale * face.underline_position();
        metrics.underlineThickness = scale * face.underline_thickness();
        return true;
    }

    public static boolean
    getFontWhitespaceWidth(
            double[] spaceAdvance,
            double[] tabAdvance,
            FontHandle font,
            FontCoordinateScaling coordinateScaling) {

        if (font == null) return false;
        FT_Face face = font.face();
        double scale =
                getFontCoordinateScale(
                        face,
                        coordinateScaling);

        int error =
                FT_Load_Char(face,
                        ' ', FT_LOAD_NO_SCALE);
        if (error != 0)
            return false;

        if (spaceAdvance != null
                && spaceAdvance.length > 0)
            spaceAdvance[0] =
                    scale * face.glyph()
                            .advance().x();

        error =
                FT_Load_Char(face,
                        '\t', FT_LOAD_NO_SCALE);
        if (error != 0)
            return false;

        if (tabAdvance != null
                && tabAdvance.length > 0)
            tabAdvance[0] =
                    scale * face.glyph()
                            .advance().x();

        return true;
    }

    public static boolean getGlyphCount(int[] output, FontHandle font) {
        if (font == null) return false;
        if (output != null && output.length > 0)
            output[0] = (int) font.face().num_glyphs();
        return true;
    }
    public static boolean getGlyphIndex(int[] glyphIndex, FontHandle font, long unicode) {
        if (font == null) return false;
        int idx = FT_Get_Char_Index(font.face(), unicode);
        if (glyphIndex != null && glyphIndex.length > 0)
            glyphIndex[0] = idx;
        return idx != 0;
    }

    // ------------------------------
    // Glyph loading
    // ------------------------------

    /**
     * Loads a glyph by Unicode codepoint and converts its outline into an msdfgen Shape.
     */
    public static boolean loadGlyph(MsdfShape output, FontHandle font, long unicode,
                                    FontCoordinateScaling coordinateScaling, double[] outAdvance) {
        if (font == null) {
            return false;
        }

        FT_Face face = font.face();
        int glyphIndex = FT_Get_Char_Index(face, unicode);

        /*
        if (glyphIndex == 0) {
            return false;
        }*/

        int error = FT_Load_Glyph(face, glyphIndex, FT_LOAD_NO_SCALE);
        if (error != 0) {
            return false;
        }

        double scale = getFontCoordinateScale(face, coordinateScaling);

        FT_GlyphSlot slot = face.glyph();
        if (outAdvance != null && outAdvance.length > 0) {
            outAdvance[0] = scale * slot.advance().x();
        }

        return readFreetypeOutline(output, slot.outline(), scale) == 0;
    }

    // Convenience overload with legacy scaling
    public static boolean loadGlyph(MsdfShape output, FontHandle font, long unicode, double[] outAdvance) {
        return loadGlyph(output, font, unicode, FontCoordinateScaling.FONT_SCALING_LEGACY, outAdvance);
    }

    // Convenience overload by glyph index
    public static boolean loadGlyph(MsdfShape output, FontHandle font, int glyphIndex,
                                    FontCoordinateScaling coordinateScaling, double[] outAdvance, boolean byIndex) {
        if (!byIndex) {
            return loadGlyph(output, font, glyphIndex, coordinateScaling, outAdvance);
        }

        if (font == null) {
            return false;
        }

        FT_Face face = font.face();
        int error = FT_Load_Glyph(face, glyphIndex, FT_LOAD_NO_SCALE);
        if (error != 0) {
            return false;
        }

        double scale = getFontCoordinateScale(face, coordinateScaling);

        FT_GlyphSlot slot = face.glyph();
        if (outAdvance != null && outAdvance.length > 0) {
            outAdvance[0] = scale * slot.advance().x();
        }

        return readFreetypeOutline(output, slot.outline(), scale) == 0;
    }
    // ------------------------------
// Kerning
// ------------------------------

    /**
     * Outputs the kerning distance between two glyphs, addressed by glyph index.
     * Mirrors: bool getKerning(double &output, FontHandle*, GlyphIndex, GlyphIndex, FontCoordinateScaling)
     */
    public static boolean getKerning(double[] output, FontHandle font, int glyphIndex0, int glyphIndex1,
                                     FontCoordinateScaling coordinateScaling) {
        if (font == null) return false;
        FT_Face face = font.face();
        try (MemoryStack stack = stackPush()) {
            FT_Vector kerning = FT_Vector.malloc(stack);
            int error = FT_Get_Kerning(face, glyphIndex0, glyphIndex1, FT_KERNING_UNSCALED, kerning);
            if (error != 0) {
                if (output != null && output.length > 0) output[0] = 0.0;
                return false;
            }
            if (output != null && output.length > 0) {
                output[0] = getFontCoordinateScale(face, coordinateScaling) * kerning.x();
            }
            return true;
        }
    }

    // Default overload matching C++'s FONT_SCALING_LEGACY default parameter
    public static boolean getKerning(double[] output, FontHandle font, int glyphIndex0, int glyphIndex1) {
        return getKerning(output, font, glyphIndex0, glyphIndex1, FontCoordinateScaling.FONT_SCALING_LEGACY);
    }

    /**
     * Outputs the kerning distance between two glyphs, addressed by Unicode codepoint.
     * Mirrors: bool getKerning(double &output, FontHandle*, unicode_t, unicode_t, FontCoordinateScaling)
     */
    public static boolean getKerning(double[] output, FontHandle font, long unicode0, long unicode1,
                                     FontCoordinateScaling coordinateScaling) {
        if (font == null) return false;
        FT_Face face = font.face();
        int glyphIndex0 = FT_Get_Char_Index(face, unicode0);
        int glyphIndex1 = FT_Get_Char_Index(face, unicode1);
        return getKerning(output, font, glyphIndex0, glyphIndex1, coordinateScaling);
    }

    // Default overload
    public static boolean getKerning(double[] output, FontHandle font, long unicode0, long unicode1) {
        return getKerning(output, font, unicode0, unicode1, FontCoordinateScaling.FONT_SCALING_LEGACY);
    }
}