package com.verity.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-authored outline icons matching the approved Home/nav mockup (Design canvas artifact
 * "Verity Screens", Main.dc.html / HomeDark.dc.html) exactly — coordinates transcribed 1:1 from
 * the mockup's SVG path data (24x24 viewport), not a Material Icons substitute. Approved by the
 * user 2026-09-14 as a deliberate departure from Material Icons for the four bottom-nav
 * destinations (see VerityIcons' doc comment).
 *
 * `stroke = SolidColor(Color.Black)` is a placeholder brush only: Icon()'s tint parameter
 * recolors the rendered glyph via ColorFilter, so the baked-in stroke color here is never seen.
 */

private const val NAV_ICON_STROKE_WIDTH = 2f

val VerityHomeOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NavHome",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 11f)
            lineTo(12f, 3f)
            lineTo(21f, 11f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 10f)
            lineTo(5f, 20f)
            lineTo(19f, 20f)
            lineTo(19f, 10f)
        }
    }.build()
}

val VerityDocumentsOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NavDocuments",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 2f)
            lineTo(15f, 2f)
            lineTo(18f, 5f)
            lineTo(18f, 22f)
            lineTo(16f, 21f)
            lineTo(14f, 22f)
            lineTo(12f, 21f)
            lineTo(10f, 22f)
            lineTo(8f, 21f)
            lineTo(6f, 22f)
            lineTo(6f, 2f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 8f)
            lineTo(15f, 8f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 12f)
            lineTo(15f, 12f)
        }
    }.build()
}

val VerityCustomersOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NavCustomers",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Primary person's head — circle, center (9,8) r=3.2, transcribed as a 4-cubic
        // Bezier approximation (kappa = 0.5522847498).
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12.2f, 8f)
            curveTo(12.2f, 9.7673f, 10.7673f, 11.2f, 9f, 11.2f)
            curveTo(7.2327f, 11.2f, 5.8f, 9.7673f, 5.8f, 8f)
            curveTo(5.8f, 6.2327f, 7.2327f, 4.8f, 9f, 4.8f)
            curveTo(10.7673f, 4.8f, 12.2f, 6.2327f, 12.2f, 8f)
            close()
        }
        // Primary person's shoulders.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(3f, 20f)
            curveTo(3f, 16.7f, 5.7f, 14f, 9f, 14f)
            curveTo(12.3f, 14f, 15f, 16.7f, 15f, 20f)
        }
        // Second person's head — circle, center (17.3,9) r=2.6.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(19.9f, 9f)
            curveTo(19.9f, 10.4359f, 18.7359f, 11.6f, 17.3f, 11.6f)
            curveTo(15.8641f, 11.6f, 14.7f, 10.4359f, 14.7f, 9f)
            curveTo(14.7f, 7.5641f, 15.8641f, 6.4f, 17.3f, 6.4f)
            curveTo(18.7359f, 6.4f, 19.9f, 7.5641f, 19.9f, 9f)
            close()
        }
        // Second person's (partial) shoulder, peeking from behind the first.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = NAV_ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(15.8f, 14.2f)
            curveTo(18.4f, 14.6f, 20.3f, 16.8f, 20.3f, 19.5f)
        }
    }.build()
}

/**
 * Gear outline. The mockup's SVG path (`stroke-width="1.7"`) draws the gear body as a sequence of
 * 7-radius elliptical arcs between eight teeth; those arcs are transcribed here as exact cubic
 * Bezier equivalents (SVG arc endpoint parameterization, one segment per arc since each spans well
 * under 90°), not a freehand approximation — see the script used to derive them if this ever needs
 * re-deriving from a future mockup revision.
 */
val VeritySettingsOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NavSettings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f)
            curveTo(19.0012f, 11.5979f, 18.9677f, 11.1964f, 18.9f, 10.8f)
            lineTo(20.9f, 9.3f)
            lineTo(18.9f, 5.9f)
            lineTo(16.5f, 6.5f)
            curveTo(15.9051f, 5.991f, 15.2291f, 5.5853f, 14.5f, 5.3f)
            lineTo(14f, 3f)
            lineTo(10f, 3f)
            lineTo(9.5f, 5.3f)
            curveTo(8.7709f, 5.5853f, 8.0949f, 5.991f, 7.5f, 6.5f)
            lineTo(5.1f, 5.9f)
            lineTo(3.1f, 9.3f)
            lineTo(5.1f, 10.8f)
            curveTo(5.0323f, 11.1964f, 4.9988f, 11.5979f, 5f, 12f)
            curveTo(5f, 12.4f, 5f, 12.8f, 5.1f, 13.2f)
            lineTo(3.1f, 14.7f)
            lineTo(5.1f, 18.1f)
            lineTo(7.5f, 17.5f)
            curveTo(8.1f, 18f, 8.8f, 18.4f, 9.5f, 18.7f)
            lineTo(10f, 21f)
            lineTo(14f, 21f)
            lineTo(14.5f, 18.7f)
            curveTo(15.2f, 18.4f, 15.9f, 18f, 16.5f, 17.5f)
            lineTo(18.9f, 18.1f)
            lineTo(20.9f, 14.7f)
            lineTo(18.9f, 13.2f)
            curveTo(19f, 12.8f, 19f, 12.4f, 19f, 12f)
            close()
        }
        // Center dot — circle, center (12,12) r=3.2.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15.2f, 12f)
            curveTo(15.2f, 13.7673f, 13.7673f, 15.2f, 12f, 15.2f)
            curveTo(10.2327f, 15.2f, 8.8f, 13.7673f, 8.8f, 12f)
            curveTo(8.8f, 10.2327f, 10.2327f, 8.8f, 12f, 8.8f)
            curveTo(13.7673f, 8.8f, 15.2f, 10.2327f, 15.2f, 12f)
            close()
        }
    }.build()
}

/**
 * FAB "create" glyph — a plain cross, stroke-width 2.4, matching the mockup's FAB icon exactly
 * (Material's `Icons.Filled.Add` is a filled shape with square-cut ends, visibly heavier/sharper
 * than the mockup's thin rounded-cap cross at the FAB's 56dp size).
 */
val VerityAddOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NavAdd",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }.build()
}

/**
 * Back arrow — matches the two-segment stroke arrow used identically across
 * InvoiceWorkspace.dc.html / Preview.dc.html / Finalized.dc.html's top bars (Material's
 * `Icons.AutoMirrored.Filled.ArrowBack` is a solid filled arrowhead, a different silhouette).
 */
val VerityBackOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NavBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 19f)
            lineTo(5f, 12f)
            lineTo(12f, 5f)
        }
    }.build()
}

/**
 * "Preview" eye glyph — matches InvoiceWorkspace.dc.html/Preview.dc.html's top-bar eye icon
 * exactly (transcribed from its SVG path, an "s"-command lens outline plus a pupil circle).
 * `Icons.Outlined.Visibility` would be the obvious Material substitute, but it lives in
 * `material-icons-extended`, a dependency this project doesn't otherwise need — not worth adding
 * for one glyph when hand-authoring it is a few lines using the same technique as the icons above.
 */
val VerityEyeOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PreviewEye",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(1f, 12f)
            curveTo(1f, 12f, 5f, 5f, 12f, 5f)
            curveTo(19f, 5f, 23f, 12f, 23f, 12f)
            curveTo(23f, 12f, 19f, 19f, 12f, 19f)
            curveTo(5f, 19f, 1f, 12f, 1f, 12f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f
        ) {
            moveTo(15f, 12f)
            curveTo(15f, 13.6569f, 13.6569f, 15f, 12f, 15f)
            curveTo(10.3431f, 15f, 9f, 13.6569f, 9f, 12f)
            curveTo(9f, 10.3431f, 10.3431f, 9f, 12f, 9f)
            curveTo(13.6569f, 9f, 15f, 10.3431f, 15f, 12f)
            close()
        }
    }.build()
}

/**
 * "Print" icon — a plain three-rectangle printer pictogram (paper feeding in, printer body,
 * output tray). `Icons.Filled.Print` isn't in material-icons-core either (only `Share` is) —
 * hand-authored here for the same reason the Preview eye was: not worth pulling in
 * material-icons-extended for one glyph. Unlike the icons above, this isn't transcribed from an
 * approved mockup — no Design Blueprint screen covers Share/Print yet (added 2026-09-16 for the
 * new PDF Share/Print feature) — so it's a plain, universally recognizable printer silhouette,
 * not a pixel-exact design deliverable.
 */
val VerityPrintOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Print",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Paper feeding into the printer from above.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 7f)
            lineTo(6f, 2f)
            lineTo(18f, 2f)
            lineTo(18f, 7f)
        }
        // Printer body.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 7f)
            lineTo(22f, 7f)
            lineTo(22f, 17f)
            lineTo(2f, 17f)
            close()
        }
        // Output tray with the printed page.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 13f)
            lineTo(18f, 13f)
            lineTo(18f, 22f)
            lineTo(6f, 22f)
            close()
        }
    }.build()
}

/**
 * Success checkmark — circle, center (12,12) r=9 (4-cubic Bezier approximation, kappa =
 * 0.5522847498), plus the checkmark polyline — matches Finalized.dc.html's `.checkwrap` icon.
 */
val VerityCheckOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "FinalizedCheck",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f
        ) {
            moveTo(21f, 12f)
            curveTo(21f, 16.9706f, 16.9706f, 21f, 12f, 21f)
            curveTo(7.0294f, 21f, 3f, 16.9706f, 3f, 12f)
            curveTo(3f, 7.0294f, 7.0294f, 3f, 12f, 3f)
            curveTo(16.9706f, 3f, 21f, 7.0294f, 21f, 12f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 12.5f)
            lineTo(11f, 15.5f)
            lineTo(16f, 9f)
        }
    }.build()
}
