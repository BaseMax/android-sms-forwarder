package com.basemax.smsforwarder.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The twelve Material Symbols (rounded) this app actually draws.
 *
 * `androidx.compose.material:material-icons-extended` carries every icon
 * Google has ever published - thousands of classes that all land in the APK,
 * because a debug build is not shrunk. These are the same glyphs: the path
 * data below is copied verbatim from the rounded 24dp sources, on the same
 * 24x24 viewport, so they render identically for a few hundred bytes.
 */
object AppIcons {

    val BatteryChargingFull by icon(
        "M15.67 4H14V3c0-.55-.45-1-1-1h-2c-.55 0-1 .45-1 1v1H8.33C7.6 4 7 4.6 7 5.33v15.33C7 " +
            "21.4 7.6 22 8.34 22h7.32c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4zm-1.06 " +
            "9.24l-2.67 5c-.24.45-.94.28-.94-.24v-3.5H9.83c-.38 0-.62-.4-.44-.74l2.67-5c.24-.45" +
            ".94-.28.94.24v3.5h1.17c.37 0 .62.4.44.74z"
    )

    val CheckCircle by icon(
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM9.29 16.29 5.7 " +
            "12.7c-.39-.39-.39-1.02 0-1.41.39-.39 1.02-.39 1.41 0L10 14.17l6.88-6.88c.39-.39 " +
            "1.02-.39 1.41 0 .39.39.39 1.02 0 1.41l-7.59 7.59c-.38.39-1.02.39-1.41 0z"
    )

    val CloudUpload by icon(
        "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 " +
            "14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 " +
            "13v4h-4v-4H7l4.65-4.65c.2-.2.51-.2.71 0L17 13h-3z"
    )

    val Dns by icon(
        "M19 13H5c-1.1 0-2 .9-2 2v4c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-4c0-1.1-.9-2-2-2zM7 " +
            "19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM19 3H5c-1.1 0-2 .9-2 2v4c0 1.1.9 2 " +
            "2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 " +
            "2-2 2z"
    )

    val Key by icon(
        "M20.59,10h-7.94C11.7,7.31,8.89,5.5,5.77,6.12c-2.29,0.46-4.15,2.3-4.63,4.58C0.32,14.58," +
            "3.26,18,7,18 c2.61,0,4.83-1.67,5.65-4H13l1.29,1.29c0.39,0.39,1.02,0.39,1.41,0L17,14l" +
            "1.29,1.29c0.39,0.39,1.03,0.39,1.42,0l2.59-2.61 c0.39-0.39,0.39-1.03-0.01-1.42l-0.99-" +
            "0.97C21.1,10.1,20.85,10,20.59,10z M7,15c-1.65,0-3-1.35-3-3c0-1.65,1.35-3,3-3s3,1.35," +
            "3,3 C10,13.65,8.65,15,7,15z"
    )

    val Login by icon(
        "M10.3,7.7L10.3,7.7c-0.39,0.39-0.39,1.01,0,1.4l1.9,1.9H3c-0.55,0-1,0.45-1,1v0c0,0.55,0." +
            "45,1,1,1h9.2l-1.9,1.9 c-0.39,0.39-0.39,1.01,0,1.4l0,0c0.39,0.39,1.01,0.39,1.4,0l3.59" +
            "-3.59c0.39-0.39,0.39-1.02,0-1.41L11.7,7.7 C11.31,7.31,10.69,7.31,10.3,7.7z M20,19h-7" +
            "c-0.55,0-1,0.45-1,1v0c0,0.55,0.45,1,1,1h7c1.1,0,2-0.9,2-2V5c0-1.1-0.9-2-2-2h-7 c-0." +
            "55,0-1,0.45-1,1v0c0,0.55,0.45,1,1,1h7V19z"
    )

    val Schedule by icon(
        "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 " +
            "20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm-.22-13h-.06c-.4 0-.72.32-." +
            "72.72v4.72c0 .35.18.68.49.86l4.15 2.49c.34.2.78.1.98-.24.21-.34.1-.79-.25-.99l-3.87-" +
            "2.3V7.72c0-.4-.32-.72-.72-.72z"
    )

    val Shield by icon(
        "M11.3,2.26l-6,2.25C4.52,4.81,4,5.55,4,6.39v4.7c0,4.83,3.13,9.37,7.43,10.75c0.37,0.12," +
            "0.77,0.12,1.14,0 c4.3-1.38,7.43-5.91,7.43-10.75v-4.7c0-0.83-0.52-1.58-1.3-1.87l-6-2." +
            "25C12.25,2.09,11.75,2.09,11.3,2.26z"
    )

    val Smartphone by icon(
        "M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99z" +
            "M17 19H7V5h10v14z"
    )

    val Sms by icon(
        "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM9 11H7V9h2v2zm4 " +
            "0h-2V9h2v2zm4 0h-2V9h2v2z"
    )

    val Sync by icon(
        "M12 4V2.21c0-.45-.54-.67-.85-.35l-2.8 2.79c-.2.2-.2.51 0 .71l2.79 2.79c.32.31.86.09.86" +
            "-.36V6c3.31 0 6 2.69 6 6 0 .79-.15 1.56-.44 2.25-.15.36-.04.77.23 1.04.51.51 1.37.33 " +
            "1.64-.34.37-.91.57-1.91.57-2.95 0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-.79.15-" +
            "1.56.44-2.25.15-.36.04-.77-.23-1.04-.51-.51-1.37-.33-1.64.34C4.2 9.96 4 10.96 4 12c0 " +
            "4.42 3.58 8 8 8v1.79c0 .45.54.67.85.35l2.79-2.79c.2-.2.2-.51 0-.71l-2.79-2.79c-.31-." +
            "31-.85-.09-.85.36V18z"
    )

    val WarningAmber by icon(
        "M12 5.99L19.53 19H4.47L12 5.99M2.74 18c-.77 1.33.19 3 1.73 3h15.06c1.54 0 2.5-1.67 " +
            "1.73-3L13.73 4.99c-.77-1.33-2.69-1.33-3.46 0L2.74 18zM11 11v2c0 .55.45 1 1 1s1-.45 " +
            "1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zm0 5h2v2h-2z"
    )
}

/**
 * Builds the vector on first use and caches it, the way the generated Material
 * icons do. Filled black; `Icon` recolours it with the tint it is given.
 */
private fun icon(pathData: String): Lazy<ImageVector> = lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
    ).build()
}
