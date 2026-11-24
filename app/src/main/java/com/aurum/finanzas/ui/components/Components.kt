package com.aurum.finanzas.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.data.Categories
import com.aurum.finanzas.data.Category
import com.aurum.finanzas.data.Transaction
import com.aurum.finanzas.data.TxSource
import com.aurum.finanzas.data.TxType
import com.aurum.finanzas.data.YM
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum
import com.aurum.finanzas.util.Format
import kotlinx.coroutines.delay

/* ---------- Interacción ---------- */

/** Efecto de "rebote" suave al tocar (se encoge un poco y vuelve) */
@Composable
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "bounce"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Aparición escalonada: sube y se desvanece hacia adentro */
@Composable
fun Modifier.fadeUpIn(index: Int = 0): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        a.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 28.dp.toPx()
    }
}

/* ---------- Números animados ---------- */

@Composable
fun AnimatedMoney(
    value: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "",
) {
    val progress = remember { Animatable(0f) }
    var fromV by remember { mutableDoubleStateOf(0.0) }
    var toV by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(value) {
        fromV += (toV - fromV) * progress.value
        toV = value
        progress.snapTo(0f)
        progress.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    val shown = fromV + (toV - fromV) * progress.value
    Text(prefix + Format.money(shown), style = style, color = color, modifier = modifier, maxLines = 1)
}

/* ---------- Contenedores ---------- */

@Composable
fun AurumCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    background: Color = Aurum.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Aurum.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, c.line.copy(alpha = 0.7f), shape)
            .padding(padding),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Aurum.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = AType.heading, color = c.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = AType.bodyStrong.copy(fontSize = 13.5.sp),
                color = c.gold,
                modifier = Modifier.bounceClick { onAction() }.padding(4.dp)
            )
        }
    }
}

@Composable
fun IconCircle(
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp = 42.dp,
    tint: Color = Aurum.colors.ink,
    background: Color = Aurum.colors.surface,
) {
    val c = Aurum.colors
    Box(
        Modifier
            .size(size)
            .bounceClick { onClick() }
            .clip(CircleShape)
            .background(background)
            .border(1.dp, c.line, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Aurum.colors
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, label = "btnAlpha")
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .bounceClick(enabled = enabled) { onClick() }
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(18.dp))
            .background(c.ink),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = c.bg, style = AType.bodyStrong.copy(fontSize = 16.sp))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = Aurum.colors.ink) {
    val c = Aurum.colors
    Row(
        modifier
            .height(44.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceAlt)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = AType.bodyStrong.copy(fontSize = 14.sp))
    }
}

/* ---------- Chips y selectores ---------- */

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, leading: ImageVector? = null) {
    val c = Aurum.colors
    val bg by animateColorAsState(if (selected) c.ink else c.surface, tween(260), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.bg else c.ink, tween(260), label = "chipFg")
    val border by animateColorAsState(if (selected) c.ink else c.line, tween(260), label = "chipBorder")
    Row(
        Modifier
            .bounceClick { onClick() }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, style = AType.bodyStrong.copy(fontSize = 13.5.sp))
    }
}

/** Selector tipo "píldora" con un indicador que se desliza */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Aurum.colors.ink,
    activeText: Color = Aurum.colors.bg,
) {
    val c = Aurum.colors
    val indicatorColor by animateColorAsState(activeColor, tween(300), label = "segColor")
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(c.surfaceAlt)
            .padding(4.dp)
    ) {
        val segW = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = segW * selected,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            label = "segOffset"
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segW)
                .height(42.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Row(Modifier.fillMaxWidth().height(42.dp)) {
            options.forEachIndexed { i, label ->
                val col by animateColorAsState(if (i == selected) activeText else c.muted, label = "segText")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = col, style = AType.bodyStrong.copy(fontSize = 14.sp))
                }
            }
        }
    }
}

@Composable
fun MonthSelector(ym: YM, onChange: (YM) -> Unit) {
    val c = Aurum.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(Icons.Rounded.ChevronLeft, { onChange(ym.prev()) }, size = 38.dp, background = c.surfaceAlt)
        AnimatedContent(
            targetState = ym,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.index() > initialState.index()
                (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(240)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
            },
            label = "month"
        ) { m ->
            Text(
                Format.monthYear(m),
                style = AType.heading,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconCircle(Icons.Rounded.ChevronRight, { onChange(ym.next()) }, size = 38.dp, background = c.surfaceAlt)
    }
}

/* ---------- Campos de texto ---------- */

@Composable
fun AurumTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    leading: ImageVector? = null,
    onFocus: (Boolean) -> Unit = {},
) {
    val c = Aurum.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (focused) c.gold else c.line, tween(250), label = "tfBorder")
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.2.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        if (leading != null) {
            Icon(leading, null, tint = if (focused) c.gold else c.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = AType.body, color = c.faint)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else minLines,
                textStyle = AType.body.copy(color = c.ink),
                cursorBrush = SolidColor(c.gold),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                    keyboardType = keyboardType
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocus(it.isFocused)
                    }
            )
        }
        if (value.isNotEmpty() && singleLine) {
            Icon(
                Icons.Rounded.Close, null, tint = c.faint,
                modifier = Modifier.size(18.dp).clickable { onChange("") }
            )
        }
    }
}

/* ---------- Categorías y movimientos ---------- */

@Composable
fun CategoryBadge(cat: Category, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(cat.color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun CategoryPick(cat: Category, selected: Boolean, onClick: () -> Unit) {
    val c = Aurum.colors
    val border by animateColorAsState(if (selected) cat.color else c.line, tween(250), label = "catBorder")
    val bg by animateColorAsState(if (selected) cat.color.copy(alpha = 0.13f) else c.surface, tween(250), label = "catBg")
    Column(
        Modifier
            .width(84.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryBadge(cat, 38.dp)
        Spacer(Modifier.height(7.dp))
        Text(cat.name, style = AType.caption, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TxRow(tx: Transaction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aurum.colors
    val cat = Categories.byId(tx.categoryId)
    Row(
        modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(cat, 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.merchant.ifBlank { cat.name },
                style = AType.bodyStrong, color = c.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tx.source == TxSource.AUTO) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = c.gold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                val sub = buildString {
                    append(cat.name)
                    append(" · ")
                    append(Format.time(tx.dateMillis))
                    if (tx.cardLast4.isNotBlank()) append(" · •${tx.cardLast4}")
                }
                Text(sub, style = AType.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            (if (tx.type == TxType.EXPENSE) "−" else "+") + Format.money(tx.amount),
            style = AType.amount,
            color = if (tx.type == TxType.EXPENSE) c.ink else c.income
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Aurum.colors
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(c.goldSoft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = c.gold, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, style = AType.heading, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = AType.caption, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            SecondaryButton(action, onAction, color = c.gold)
        }
    }
}

/* ---------- Gráficas ---------- */

@Composable
fun ProgressLine(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        a.animateTo(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(a.value).clip(CircleShape).background(color))
    }
}

/** Gráfica de dona que "se dibuja" al aparecer */
@Composable
fun DonutChart(values: List<Float>, colors: List<Color>, track: Color, modifier: Modifier = Modifier, stroke: Dp = 20.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val sw = stroke.toPx()
        val d = size.minDimension - sw
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val arc = Size(d, d)
        drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(sw))
        val total = values.sum()
        if (total <= 0f) return@Canvas
        val gap = if (values.count { it > 0f } > 1) 2.5f else 0f
        var start = -90f
        values.forEachIndexed { i, v ->
            val full = 360f * v / total
            val sweep = (full * progress.value - gap).coerceAtLeast(0f)
            if (sweep > 0f) {
                drawArc(colors[i], start + gap / 2f, sweep, false, topLeft, arc, style = Stroke(sw, cap = StrokeCap.Butt))
            }
            start += full * progress.value
        }
    }
}

/** Barras redondeadas que crecen al aparecer */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    highlight: Int,
    barColor: Color,
    highlightColor: Color,
    labelColor: Color,
    highlightLabelColor: Color,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = values.size
            if (n == 0) return@Canvas
            val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            val slot = size.width / n
            val bw = slot * 0.46f
            values.forEachIndexed { i, v ->
                val h = (size.height * (v / max).toFloat() * progress.value).coerceAtLeast(4.dp.toPx())
                val x = slot * i + (slot - bw) / 2f
                drawRoundRect(
                    color = if (i == highlight) highlightColor else barColor,
                    topLeft = Offset(x, size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(bw / 2.6f, bw / 2.6f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(
                    l,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = AType.caption,
                    color = if (i == highlight) highlightLabelColor else labelColor
                )
            }
        }
    }
}

/** Logo de la app: anillo dorado con la "A" */
@Composable
fun AurumMark(size: Dp = 88.dp) {
    val c = Aurum.colors
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(c.gold, radius = this.size.minDimension / 2f - 2.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            drawCircle(c.gold.copy(alpha = 0.10f), radius = this.size.minDimension / 2f - 8.dp.toPx())
        }
        Text("A", style = AType.display.copy(fontSize = (size.value * 0.46f).sp), color = c.gold)
    }
}
