package cn.maoyanluo.ui_library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlin.math.sqrt


@Composable
fun CircleTextButton(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    bgColor: Color = Color.Gray,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val bc = if (pressed) textColor else bgColor
    val tc = if (pressed) bgColor else textColor
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bc)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown?.invoke()
                        tryAwaitRelease()
                        pressed = false
                        onUp?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = tc,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    outerColor: Color = Color.Gray,
    knobColor: Color = Color.White,
    activeKnobColor: Color = Color.DarkGray,
    onStickPress: (() -> Unit)? = null,
    onStickRelease: (() -> Unit)? = null,
    onAxisChanged: (x: Int, y: Int) -> Unit = { _, _ -> },
    knobDiameterRatio: Float = 0.24f
) {
    // Compose 的偏移量、尺寸计算都更适合先用像素处理，最后再转成 dp 给 Modifier.offset。
    val density = LocalDensity.current

    // 当前小圆相对大圆中心点的位移。
    // Offset.Zero 表示摇杆处于居中状态。
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    // 是否正在按压中心小圆。
    // 这里的“按压”优先级最高，用来模拟 L3 / R3 这种摇杆按下事件。
    var stickPressed by remember { mutableStateOf(false) }

    // 记录整个摇杆容器的实际像素尺寸。
    // pointerInteropFilter 里需要依赖这个尺寸来计算圆心、半径和触点位置。
    var containerSize by remember { mutableStateOf(Offset.Zero) }

    // 是否正在拖动外圈区域。
    // 只有点在中心小圆之外时，才会进入“外圈拖动 -> 计算 XY”这条逻辑。
    var draggingOuterArea by remember { mutableStateOf(false) }

    // 把任意触点位移限制在可移动半径内。
    // 如果用户手指拖到了大圆外面，小圆仍然只停留在最外侧边界上。
    fun clampToRadius(offset: Offset, maxRadius: Float): Offset {
        val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
        if (distance <= maxRadius || distance == 0f) return offset
        val scale = maxRadius / distance
        return Offset(offset.x * scale, offset.y * scale)
    }

    // 把 [-radius, radius] 范围内的像素位移，映射成 HID 轴值的 [0, 255]。
    // 约定：
    // 1. 中心点 -> 128
    // 2. 最左 / 最上 -> 0
    // 3. 最右 / 最下 -> 255
    // 这样可以直接对接你当前 HID descriptor 里 0..255 的轴定义。
    fun axisValue(value: Float, radius: Float): Int {
        if (radius <= 0f) return 128
        val normalized = (value / radius).coerceIn(-1f, 1f)
        return (((normalized + 1f) * 0.5f) * 255f).toInt().coerceIn(0, 255)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // 记录这个摇杆容器的实际宽高。
            // 后续所有几何计算都基于这个尺寸：
            // 1. 大圆半径
            // 2. 小圆半径
            // 3. 圆心坐标
            .onSizeChanged {
                containerSize = Offset(it.width.toFloat(), it.height.toFloat())
            }
            // 这里改成 Compose 原生 pointerInput，并且按“每个摇杆只跟踪自己的那根手指”来处理。
            // 这样有几个直接好处：
            // 1. 左右两个摇杆可以同时被两根手指独立控制
            // 2. 摇杆和其它按钮 / 扳机可以同时使用
            // 3. 不再依赖 MotionEvent 的单指 x/y，避免第二根手指按下时事件被错误覆盖
            //
            // 这个实现的核心思想是：
            // 1. 每次手势开始时，只绑定当前落在这个摇杆区域内的“第一根手指”
            // 2. 后续 MOVE / UP 只跟踪这根手指，不理会其它手指
            // 3. 等这根手指抬起后，再结束这一次手势
            .pointerInput(Unit) {
                awaitEachGesture {
                    val width = containerSize.x
                    val height = containerSize.y

                    // 尺寸还没测量出来时，这个摇杆还不能正确计算半径和圆心。
                    // 这种情况下直接放弃本轮手势，等下一次手指再按下时重新开始。
                    if (width <= 0f || height <= 0f) {
                        return@awaitEachGesture
                    }

                    // 大圆的半径，始终取宽高中较小的一半，保证外圈永远是正圆。
                    val outerRadius = minOf(width, height) / 2f

                    // 中心小圆的半径。
                    // 小圆直径是整体直径的 24%，所以小圆半径也按同样比例从 outerRadius 推出来。
                    val knobRadius = outerRadius * knobDiameterRatio

                    // 小圆圆心真正可以移动的最大半径。
                    // 小圆不能把自身一半跑出大圆外，所以最大半径要减掉 knobRadius。
                    val movableRadius = outerRadius - knobRadius

                    // 大圆中心点。
                    val center = Offset(width / 2f, height / 2f)

                    // awaitFirstDown(requireUnconsumed = false) 的意思是：
                    // 即使其它组件也在处理自己的手指，我们仍然可以收到本组件范围内的按下事件。
                    // 这对“摇杆 + 按钮 / 两个摇杆同时操作”的场景很重要。
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // 本轮手势的第一根手指位置。
                    // 注意：从这里开始，这个摇杆只跟踪 down.id 对应的那根手指。
                    val touchPoint = down.position
                    val touchOffset = touchPoint - center

                    // 当前小圆圆心位置 = 大圆中心 + 当前小圆偏移。
                    // 如果摇杆当前已经不在中心，这里也能正确判断“是不是按中了当前的小圆”。
                    val currentKnobCenter = center + knobOffset
                    val touchToKnob = touchPoint - currentKnobCenter

                    // 判断这次按下是不是命中了中心小圆。
                    // 如果命中了，优先走“L3 / R3 按压”逻辑，不改摇杆轴值。
                    val touchedKnob = sqrt(
                        touchToKnob.x * touchToKnob.x + touchToKnob.y * touchToKnob.y
                    ) <= knobRadius

                    if (touchedKnob) {
                        // 命中中心小圆：进入“摇杆按下”模式。
                        // 这条路径只发按下 / 抬起回调，不改变 XY。
                        stickPressed = true
                        draggingOuterArea = false
                        onStickPress?.invoke()
                    } else {
                        // 没命中中心小圆：进入“拖动外圈控制摇杆”模式。
                        draggingOuterArea = true
                        stickPressed = false

                        // 按下瞬间就把小圆吸附到当前方向，保证视觉反馈是立即的。
                        val updatedOffset = clampToRadius(touchOffset, movableRadius)
                        knobOffset = updatedOffset
                        onAxisChanged(
                            axisValue(updatedOffset.x, movableRadius),
                            axisValue(updatedOffset.y, movableRadius)
                        )
                    }

                    // 只跟踪本轮手势的这根手指。
                    // 后续即便屏幕上还有其它手指，它们也不会影响当前这个摇杆的状态。
                    val activePointerId = down.id

                    // 循环等待这根手指的后续移动 / 抬起事件。
                    while (true) {
                        val event = awaitPointerEvent()

                        // 从当前所有触点里，找到属于这个摇杆的那根手指。
                        // 如果找不到，说明这根手指已经离开当前事件流了，本轮手势结束。
                        val activeChange = event.changes.firstOrNull { it.id == activePointerId }
                            ?: break

                        // 中心小圆按压模式：
                        // 只等这根手指抬起，然后触发 onStickRelease。
                        if (stickPressed) {
                            if (!activeChange.pressed) {
                                stickPressed = false
                                onStickRelease?.invoke()
                                activeChange.consume()
                                break
                            }
                            activeChange.consume()
                            continue
                        }

                        // 外圈拖动模式：
                        // 只要这根手指还按着，就持续根据它的位置更新摇杆坐标。
                        if (draggingOuterArea) {
                            if (!activeChange.pressed) {
                                // 拖动结束：小圆回中，并回调 HID 的中心值。
                                draggingOuterArea = false
                                knobOffset = Offset.Zero
                                onAxisChanged(128, 128)
                                activeChange.consume()
                                break
                            }

                            val updatedTouchOffset = activeChange.position - center
                            val updatedOffset = clampToRadius(updatedTouchOffset, movableRadius)
                            knobOffset = updatedOffset
                            onAxisChanged(
                                axisValue(updatedOffset.x, movableRadius),
                                axisValue(updatedOffset.y, movableRadius)
                            )
                            activeChange.consume()
                            continue
                        }

                        // 理论上不会走到这里。
                        // 但如果未来逻辑扩展时出现未覆盖状态，这里也安全地结束本轮手势。
                        activeChange.consume()
                        break
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 把像素偏移转换成 dp，给 Modifier.offset 使用。
        val knobOffsetXDp = with(density) { knobOffset.x.toDp() }
        val knobOffsetYDp = with(density) { knobOffset.y.toDp() }
        val knobSize = with(density) { (containerSize.x * knobDiameterRatio).toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 大圆背景。
                .clip(CircleShape)
                .background(outerColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                // 小圆根据当前拖动结果偏移。
                .offset(x = knobOffsetXDp, y = knobOffsetYDp)
                .size(knobSize)
                .clip(CircleShape)
                // 当中心小圆处于“按下”状态时切换成高亮色，
                // 方便视觉上区分“摇杆被按下”和“只是发生了拖动”。
                .background(if (stickPressed) activeKnobColor else knobColor)
                // 小圆尺寸固定为整个摇杆容器直径的 24%，
                // 无论初始状态还是拖动状态，始终保持同样大小。
        )
    }

    LaunchedEffect(Unit) {
        onAxisChanged(128, 128)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameControllerTriggerButton(
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    backgroundColor: Color = Color.Gray,
    activeColor: Color = Color.DarkGray,
    onValueChanged: (value: Int) -> Unit = {}
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var pressed by remember { mutableStateOf(false) }

    var currentValue by remember { mutableIntStateOf(0) }
    fun computeValue(x: Float, width: Float): Int {
        if (width <= 0f) return 0
        val clampedX = x.coerceIn(0f, width)
        val ratio = if (reverseDirection) {
            1f - (clampedX / width)
        } else {
            clampedX / width
        }
        return (ratio * 255f).toInt().coerceIn(0, 255)
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                containerSize = Size(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    currentValue = computeValue(down.position.x, containerSize.width)
                    onValueChanged(currentValue)
                    while (true) {
                        val event = awaitPointerEvent().changes.find { it.id == down.id } ?: break
                        currentValue = computeValue(event.position.x, containerSize.width)
                        onValueChanged(currentValue)
                        event.consume()
                    }
                    pressed = false
                    currentValue = 0
                    onValueChanged(0)
                }
            }
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!pressed || currentValue <= 0) return@Canvas

            val progressWidth = size.width * (currentValue / 255f)
            val left = if (reverseDirection) {
                size.width - progressWidth
            } else {
                0f
            }

            drawRoundRect(
                color = activeColor,
                topLeft = Offset(left, 0f),
                size = Size(progressWidth, size.height)
            )
        }
    }
    LaunchedEffect(Unit) {
        onValueChanged(0)
    }
}

@Composable
fun SquareTextButton(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    bgColor: Color = Color.Gray,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val bc = if (pressed) textColor else bgColor
    val tc = if (pressed) bgColor else textColor
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(bc)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown?.invoke()
                        tryAwaitRelease()
                        pressed = false
                        onUp?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = tc,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun RectangleTextButton(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    bgColor: Color = Color.Gray,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val bc = if (pressed) textColor else bgColor
    val tc = if (pressed) bgColor else textColor
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bc)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown?.invoke()
                        tryAwaitRelease()
                        pressed = false
                        onUp?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = tc,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}
