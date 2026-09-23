package it.scvnsc.whoknows.ui.screens.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Testo che riduce automaticamente la dimensione del font finche' non entra nello spazio
 * disponibile (larghezza e altezza) e nel numero massimo di righe.
 * Permette layout fissi, senza scroll, che si adattano a schermi piccoli e a font di sistema ingranditi.
 */
@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = style.fontSize,
    minFontSize: TextUnit = 10.sp,
    maxLines: Int = 1,
    textAlign: TextAlign? = null
) {
    val startSize = if (fontSize.isSpecified) fontSize else 16.sp
    var currentSize by remember(text, startSize) { mutableStateOf(startSize) }
    var readyToDraw by remember(text, startSize) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        style = style,
        fontSize = currentSize,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Clip,
        textAlign = textAlign,
        //non disegno finche' non ho trovato la dimensione giusta, per evitare sfarfallii
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && currentSize.value > minFontSize.value) {
                currentSize = (currentSize.value * 0.9f).coerceAtLeast(minFontSize.value).sp
            } else {
                readyToDraw = true
            }
        }
    )
}
