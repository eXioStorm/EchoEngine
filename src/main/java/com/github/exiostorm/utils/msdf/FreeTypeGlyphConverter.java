package com.github.exiostorm.utils.msdf;
import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.*;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class FreeTypeGlyphConverter {
    private long ftLibrary;
    private final Map<String, FT_Face> faceCache = new HashMap<>();
    private final Map<String, Long> fontCache = new HashMap<>();

    public FreeTypeGlyphConverter() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pLibrary = stack.mallocPointer(1);
            if (FT_Init_FreeType(pLibrary) != 0) {
                throw new RuntimeException("Failed to initialize FreeType library");
            }
            ftLibrary = pLibrary.get(0);
        }
    }

    /**
     * Load a font face from file path
     */
    public long loadFont(String fontPath) {
        if (fontCache.containsKey(fontPath)) {
            return fontCache.get(fontPath);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            File fontFile = new File(fontPath);
            if (!fontFile.exists()) {
                throw new RuntimeException("Font file not found: " + fontFile.getAbsolutePath());
            }
            if (!fontFile.isFile()) {
                throw new RuntimeException("Path is not a file: " + fontFile.getAbsolutePath());
            }

            System.out.println("Loading font from: " + fontFile.getAbsolutePath());
            System.out.println("File size: " + fontFile.length() + " bytes");

            // Read file into byte array
            byte[] fileBytes = Files.readAllBytes(fontFile.toPath());
            System.out.println("Read " + fileBytes.length + " bytes from file");

            // Allocate native memory and copy font data
            ByteBuffer fontData = memAlloc(fileBytes.length);
            fontData.put(fileBytes);
            fontData.flip();

            System.out.println("FontData buffer: position=" + fontData.position() +
                    ", limit=" + fontData.limit() + ", capacity=" + fontData.capacity());

            PointerBuffer pFace = stack.mallocPointer(1);
            int error = FT_New_Memory_Face(ftLibrary, fontData, 0, pFace);
            if (error != 0) {
                memFree(fontData);
                throw new RuntimeException("Failed to load font (FreeType error " + error + "): " + fontPath);
            }

            long face = pFace.get(0);
            System.out.println("Successfully loaded font face: " + face);
            fontCache.put(fontPath, face);
            return face;
        } catch (Exception e) {
            System.err.println("Exception details: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error loading font: " + fontPath, e);
        }
    }

    /**
     * Main method to create glyph - replaces your createGlyph method
     */
    public String createGlyph(int unicode, String fontPath, File outputFile) {
        long face = loadFont(fontPath);

        // Set font size (64 pixels at 72 DPI)
        if (FT_Set_Pixel_Sizes(FT_Face.create(face), 0, 64) != 0) {
            System.err.println("Failed to set font size");
            return null;
        }

        // Load the glyph
        int glyphIndex = FT_Get_Char_Index(FT_Face.create(face), unicode);
        if (glyphIndex == 0) {
            System.err.println("Glyph not found for unicode: " + unicode);
            return null;
        }

        if (FT_Load_Glyph(FT_Face.create(face), glyphIndex, FT_LOAD_NO_SCALE) != 0) {
            System.err.println("Failed to load glyph");
            return null;
        }

        // Get the glyph slot
        FT_Face ftFace = FT_Face.create(face);
        FT_GlyphSlot slot = ftFace.glyph();

        // Convert FreeType outline to MsdfShape
        MsdfShape msdfShape = fromFreeTypeOutline1(slot.outline());

        if (msdfShape == null) {
            System.err.println("Could not convert glyph outline for unicode: " + unicode);
            return null;
        }

        // IMPORTANT
        msdfShape.normalize();
        return "done";
    }

    /**
     * Convert FreeType outline to MsdfShape - replaces ShapeConverter.fromAwtShape
     */
    public static MsdfShape fromFreeTypeOutline1(FT_Outline outline) {
        MsdfShape msdfShape = new MsdfShape();
        int numContours = outline.n_contours();
        if (numContours == 0) {
            return msdfShape;
        }

        FT_Vector.Buffer points = outline.points();
        ByteBuffer tags = outline.tags();
        ShortBuffer contoursBuffer = outline.contours();
        short[] contourEnds = new short[numContours];
        contoursBuffer.get(contourEnds);

        for (int c = 0; c < numContours; c++) {
            int contourStart = (c == 0) ? 0 : contourEnds[c - 1] + 1;
            int contourEnd = contourEnds[c];
            int numPoints = contourEnd - contourStart + 1;

            if (numPoints < 2) {
                continue;
            }

            Contours.Contour contour = msdfShape.addContour();

            // Determine the actual starting point
            Vector2d startPoint;
            int firstIdx = contourStart;

            byte firstTag = tags.get(contourStart);
            boolean firstIsOnCurve = (firstTag & 0x01) != 0;

            if (firstIsOnCurve) {
                FT_Vector vec = points.get(contourStart);
                startPoint = new Vector2d(vec.x(), -vec.y());
            } else {
                // First point is off-curve
                byte lastTag = tags.get(contourEnd);
                boolean lastIsOnCurve = (lastTag & 0x01) != 0;

                if (lastIsOnCurve) {
                    // Start from last point
                    FT_Vector vec = points.get(contourEnd);
                    startPoint = new Vector2d(vec.x(), -vec.y());
                    // We'll wrap around - don't process last point in main loop
                } else {
                    // Both first and last are off-curve - create implicit midpoint
                    FT_Vector firstVec = points.get(contourStart);
                    FT_Vector lastVec = points.get(contourEnd);
                    startPoint = new Vector2d(
                            (firstVec.x() + lastVec.x()) / 2.0,
                            (-firstVec.y() + (-lastVec.y())) / 2.0
                    );
                }
            }

            Vector2d prevPoint = startPoint;
            Vector2d prevControl = null; // For handling consecutive off-curve points

            // Process all points in the contour
            for (int idx = 0; idx < numPoints; idx++) {
                int i = contourStart + idx;
                FT_Vector vec = points.get(i);
                byte tag = tags.get(i);
                boolean isOnCurve = (tag & 0x01) != 0;
                boolean isCubic = (tag & 0x02) != 0;

                Vector2d current = new Vector2d(vec.x(), -vec.y());

                if (isOnCurve) {
                    if (prevControl != null) {
                        // We had a pending off-curve point - create quadratic bezier
                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(prevPoint, prevControl, current, EdgeColorEnum.WHITE.getValue())
                        ));
                        prevControl = null;
                    } else {
                        // Linear segment
                        if (!prevPoint.equals(current, 1e-9)) {
                            contour.edges.add(new EdgeHolder(
                                    EdgeSegment.create(prevPoint, current, EdgeColorEnum.WHITE.getValue())
                            ));
                        }
                    }
                    prevPoint = current;
                } else if (isCubic) {
                    // Cubic bezier - need 2 control points and 1 endpoint
                    if (idx + 2 < numPoints) {
                        FT_Vector cp1Vec = points.get(i);
                        FT_Vector cp2Vec = points.get(i + 1);
                        FT_Vector endVec = points.get(i + 2);

                        Vector2d cp1 = new Vector2d(cp1Vec.x(), -cp1Vec.y());
                        Vector2d cp2 = new Vector2d(cp2Vec.x(), -cp2Vec.y());
                        Vector2d end = new Vector2d(endVec.x(), -endVec.y());

                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(prevPoint, cp1, cp2, end, EdgeColorEnum.WHITE.getValue())
                        ));

                        prevPoint = end;
                        prevControl = null;
                        // Skip the next 2 points as we've consumed them
                        idx += 2;
                    }
                } else {
                    // Quadratic control point (off-curve)
                    if (prevControl != null) {
                        // Two consecutive off-curve points - create implicit on-curve point between them
                        Vector2d implicitPoint = new Vector2d(
                                (prevControl.x + current.x) / 2.0,
                                (prevControl.y + current.y) / 2.0
                        );

                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(prevPoint, prevControl, implicitPoint, EdgeColorEnum.WHITE.getValue())
                        ));

                        prevPoint = implicitPoint;
                    }
                    prevControl = current;
                }
            }

            // Handle any remaining control point at the end of contour
            if (prevControl != null) {
                // Close with quadratic curve to start
                contour.edges.add(new EdgeHolder(
                        EdgeSegment.create(prevPoint, prevControl, startPoint, EdgeColorEnum.WHITE.getValue())
                ));
            } else {
                // Close with line if needed
                if (!prevPoint.equals(startPoint, 1e-9)) {
                    contour.edges.add(new EdgeHolder(
                            EdgeSegment.create(prevPoint, startPoint, EdgeColorEnum.WHITE.getValue())
                    ));
                }
            }
        }

        msdfShape.orientContours();
        return msdfShape;
    }
    public static MsdfShape fromFreeTypeOutline(FT_Outline outline, FT_Face face) {
        MsdfShape shape = new MsdfShape();

        // Get the scaling factor from units per EM
        double scale = 1.0 / face.units_per_EM();

        // Create user data structure to pass to callbacks
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Allocate native memory for context (pointer to our Java object won't work directly)
            // We'll use a simple approach: store shape reference in a thread-local or static field
            OutlineCallbackContext.currentShape = shape;
            OutlineCallbackContext.currentContour = null;
            OutlineCallbackContext.lastPoint = null;
            OutlineCallbackContext.scale = scale;

            // Create the FT_Outline_Funcs structure
            FT_Outline_Funcs funcs = FT_Outline_Funcs.malloc(stack);

            // Set up callbacks
            funcs.move_to(OutlineCallbackContext::moveToCallback);
            funcs.line_to(OutlineCallbackContext::lineToCallback);
            funcs.conic_to(OutlineCallbackContext::conicToCallback);
            funcs.cubic_to(OutlineCallbackContext::cubicToCallback);
            funcs.shift(0);
            funcs.delta(0);

            // Decompose the outline
            int error = FT_Outline_Decompose(outline, funcs, 0L);

            if (error != 0) {
                System.err.println("FT_Outline_Decompose failed with error: " + error);
                return null;
            }
        }

        // Orient contours for proper inside/outside determination
        if (!shape.contours.isEmpty()) {
            shape.orientContours();
        }

        return shape;
    }
    private static class OutlineCallbackContext {
        static MsdfShape currentShape;
        static Contours.Contour currentContour;
        static Vector2d lastPoint;
        static double scale;

        // Move-to callback: start a new contour
        static int moveToCallback(long toPtr, long user) {
            FT_Vector to = FT_Vector.create(toPtr);
            currentContour = currentShape.addContour();
            lastPoint = new Vector2d(to.x() * scale, -to.y() * scale);
            return 0;
        }

        // Line-to callback: add a linear edge
        static int lineToCallback(long toPtr, long user) {
            FT_Vector to = FT_Vector.create(toPtr);
            Vector2d endpoint = new Vector2d(to.x() * scale, -to.y() * scale);

            if (currentContour != null && lastPoint != null) {
                if (!lastPoint.equals(endpoint, 1e-9)) {
                    currentContour.edges.add(new EdgeHolder(
                            EdgeSegment.create(lastPoint, endpoint, EdgeColorEnum.WHITE.getValue())
                    ));
                }
                lastPoint = endpoint;
            }
            return 0;
        }

        // Conic-to callback: add a quadratic Bezier edge
        static int conicToCallback(long controlPtr, long toPtr, long user) {
            FT_Vector control = FT_Vector.create(controlPtr);
            FT_Vector to = FT_Vector.create(toPtr);

            Vector2d controlPoint = new Vector2d(control.x() * scale, -control.y() * scale);
            Vector2d endpoint = new Vector2d(to.x() * scale, -to.y() * scale);

            if (currentContour != null && lastPoint != null) {
                currentContour.edges.add(new EdgeHolder(
                        EdgeSegment.create(lastPoint, controlPoint, endpoint, EdgeColorEnum.WHITE.getValue())
                ));
                lastPoint = endpoint;
            }
            return 0;
        }

        // Cubic-to callback: add a cubic Bezier edge
        static int cubicToCallback(long control1Ptr, long control2Ptr, long toPtr, long user) {
            FT_Vector control1 = FT_Vector.create(control1Ptr);
            FT_Vector control2 = FT_Vector.create(control2Ptr);
            FT_Vector to = FT_Vector.create(toPtr);

            Vector2d cp1 = new Vector2d(control1.x() * scale, -control1.y() * scale);
            Vector2d cp2 = new Vector2d(control2.x() * scale, -control2.y() * scale);
            Vector2d endpoint = new Vector2d(to.x() * scale, -to.y() * scale);

            if (currentContour != null && lastPoint != null) {
                currentContour.edges.add(new EdgeHolder(
                        EdgeSegment.create(lastPoint, cp1, cp2, endpoint, EdgeColorEnum.WHITE.getValue())
                ));
                lastPoint = endpoint;
            }
            return 0;
        }
    }

    /**
     * Helper method to load font file into ByteBuffer
     */
    private ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws Exception {
        ByteBuffer buffer;

        Path path = Path.of(resource);
        byte[] bytes = Files.readAllBytes(path);

        buffer = memAlloc(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        return buffer;
    }
    public FT_Face createFace(String path, Long face) {
        if (this.faceCache.isEmpty()) {
            this.faceCache.put(path, FT_Face.create(face));
        }
        return this.faceCache.get(path);
    }

    /**
     * Cleanup when done
     */
    public void dispose() {
        for (FT_Face face : faceCache.values()) {
            FT_Done_Face(face);
        }
        fontCache.clear();

        if (ftLibrary != NULL) {
            FT_Done_FreeType(ftLibrary);
            ftLibrary = NULL;
        }
    }
}