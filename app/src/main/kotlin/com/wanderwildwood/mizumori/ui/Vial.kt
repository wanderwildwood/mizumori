package com.wanderwildwood.mizumori.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wanderwildwood.mizumori.core.Angles

/**
 * The instrument itself, drawn rather than composed of components.
 *
 * MMD has buttons and rows and tabs, and no spirit level, which is right — a vial is not
 * a control. It is drawn here in the same terms as everything else on the screen: pure
 * black on white, one line weight, no gradient, no shadow and nothing that moves except
 * the bubble.
 *
 * The bubble is a ring, and fills solid only when the reading is level. That is the
 * whole of the feedback: on a panel with sixteen greys and a slow repaint, a shape
 * changing weight is legible across a room and a colour change is not legible at all.
 */

/** How far off level puts the bubble against the end of its travel. */
private const val EDGE_RANGE_DEGREES = 6f
private const val FLAT_RANGE_DEGREES = 6f

/**
 * The one-axis vial, for a phone stood on any of its four edges.
 *
 * Wider than it is tall, with the bubble running left and right, because that is what a
 * spirit level looks like and the shape itself says which way to read it.
 */
@Composable
fun EdgeVial(degrees: Float, isLevel: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(120.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val stroke = 3.dp.toPx()
            val bubbleRadius = 26.dp.toPx()
            val inset = 8.dp.toPx()

            val tubeHeight = bubbleRadius * 2 + stroke * 3
            val tubeTop = (size.height - tubeHeight) / 2f
            val travel = (size.width - inset * 2 - bubbleRadius * 2 - stroke * 2) / 2f

            // the tube
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(inset, tubeTop),
                size = Size(size.width - inset * 2, tubeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(tubeHeight / 2f),
                style = Stroke(width = stroke),
            )

            // the two centre marks a bubble is read against
            val centre = size.width / 2f
            listOf(centre - bubbleRadius, centre + bubbleRadius).forEach { x ->
                drawLine(
                    color = Color.Black,
                    start = Offset(x, tubeTop + stroke),
                    end = Offset(x, tubeTop + tubeHeight - stroke),
                    strokeWidth = stroke,
                )
            }

            // the bubble
            val offset = Angles.bubbleOffset(degrees, EDGE_RANGE_DEGREES)
            drawBubble(
                centre = Offset(centre + offset * travel, tubeTop + tubeHeight / 2f),
                radius = bubbleRadius,
                stroke = stroke,
                filled = isLevel,
            )
        }
    }
}

/**
 * The two-axis bullseye, for a phone lying on its back.
 *
 * A round vial because both axes matter at once and neither is more important; the
 * crosshairs are what you line the bubble up on.
 */
@Composable
fun FlatVial(
    acrossDegrees: Float,
    alongDegrees: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(260.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            val stroke = 3.dp.toPx()
            val bubbleRadius = 26.dp.toPx()
            val centre = Offset(size.width / 2f, size.height / 2f)
            val outer = minOf(size.width, size.height) / 2f - stroke * 2

            drawCircle(
                color = Color.Black,
                radius = outer,
                center = centre,
                style = Stroke(width = stroke),
            )

            // the ring the bubble sits inside when it is level
            drawCircle(
                color = Color.Black,
                radius = bubbleRadius,
                center = centre,
                style = Stroke(width = stroke),
            )

            // crosshairs, stopping short of the middle so they never crowd the bubble
            val gap = bubbleRadius + 6.dp.toPx()
            drawLine(
                Color.Black,
                Offset(centre.x - outer, centre.y),
                Offset(centre.x - gap, centre.y),
                strokeWidth = stroke,
            )
            drawLine(
                Color.Black,
                Offset(centre.x + gap, centre.y),
                Offset(centre.x + outer, centre.y),
                strokeWidth = stroke,
            )
            drawLine(
                Color.Black,
                Offset(centre.x, centre.y - outer),
                Offset(centre.x, centre.y - gap),
                strokeWidth = stroke,
            )
            drawLine(
                Color.Black,
                Offset(centre.x, centre.y + gap),
                Offset(centre.x, centre.y + outer),
                strokeWidth = stroke,
            )

            val travel = outer - bubbleRadius - stroke
            val dx = Angles.bubbleOffset(acrossDegrees, FLAT_RANGE_DEGREES) * travel
            val dy = Angles.bubbleOffset(alongDegrees, FLAT_RANGE_DEGREES) * travel

            drawBubble(
                centre = Offset(centre.x + dx, centre.y + dy),
                radius = bubbleRadius,
                stroke = stroke,
                filled = isLevel,
            )
        }
    }
}

/**
 * A ring, drawn heavier once it is level.
 *
 * Deliberately not a solid disc. A filled bubble reads well on paper and badly here: it
 * is the largest block of black on the screen and it moves, which is precisely the shape
 * E Ink ghosts worst — the trail it leaves behind is still faintly there several
 * refreshes later, so the vial ends up with a smear of old bubbles down it. A ring that
 * doubles its stroke weight says the same thing, is legible at the same distance, and
 * leaves almost nothing behind.
 *
 * White-filled underneath either way, so the tube's centre marks do not show through it.
 */
private fun DrawScope.drawBubble(
    centre: Offset,
    radius: Float,
    stroke: Float,
    filled: Boolean,
) {
    drawCircle(color = Color.White, radius = radius, center = centre)
    drawCircle(
        color = Color.Black,
        radius = radius,
        center = centre,
        style = Stroke(width = if (filled) stroke * 3f else stroke),
    )
}
