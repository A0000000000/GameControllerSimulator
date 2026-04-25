package cn.maoyanluo.ui_library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import cn.maoyanluo.hid_library.GameControllerHIDReportGenerator


@Composable
fun ActionButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit
    ) {
    Box(modifier = modifier) {
        CircleTextButton(
            text = "Y",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            textColor = Color(0xFFFFC107),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.Y, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.Y, false)
            }
        )

        CircleTextButton(
            text = "X",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            textColor = Color(0xFF2196F3),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.X, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.X, false)
            }
        )

        CircleTextButton(
            text = "B",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            textColor = Color(0xFFF44336),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.B, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.B, false)
            }
        )

        CircleTextButton(
            text = "A",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            textColor = Color(0xFF4CAF50),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.A, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.A, false)
            }
        )
    }
}

@Composable
fun ActionButtons2(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        CircleTextButton(
            text = "△",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.Y, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.Y, false)
            }
        )

        CircleTextButton(
            text = "○",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.X, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.X, false)
            }
        )

        CircleTextButton(
            text = "×",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.B, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.B, false)
            }
        )

        CircleTextButton(
            text = "□",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.A, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.A, false)
            }
        )
    }
}

@Composable
fun DPadButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.DPad, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        SquareTextButton(
            text = "↑",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.TOP, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.TOP, false)
            }
        )
        SquareTextButton(
            text = "↓",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.BOTTOM, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.BOTTOM, false)
            }
        )
        SquareTextButton(
            text = "←",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.LEFT, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.LEFT, false)
            }
        )
        SquareTextButton(
            text = "→",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.RIGHT, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.DPad.RIGHT, false)
            }
        )
    }
}

@Composable
fun DPadButtons2(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        SquareTextButton(
            text = "↑",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.TOP, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.TOP, false)
            }
        )
        SquareTextButton(
            text = "↓",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.BOTTOM, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.BOTTOM, false)
            }
        )
        SquareTextButton(
            text = "←",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.LEFT, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.LEFT, false)
            }
        )
        SquareTextButton(
            text = "→",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.RIGHT, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.RIGHT, false)
            }
        )
    }
}

@Composable
fun LTLBButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onTriggerChanged: (value: Int) -> Unit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit
) {
    Row(modifier = modifier) {
        GameControllerTriggerButton(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            reverseDirection = true,
            onValueChanged = onTriggerChanged
        )

        SquareTextButton(
            text = "LB",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.LB, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.LB, false)
            }
        )
    }
}

@Composable
fun RBRTButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onTriggerChanged: (value: Int) -> Unit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit
) {
    Row(modifier = modifier) {
        SquareTextButton(
            text = "RB",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.RB, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.RB, false)
            }
        )

        GameControllerTriggerButton(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            reverseDirection = false,
            onValueChanged = onTriggerChanged
        )
    }
}

@Composable
fun LeftButtonGroup(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit) {
    Row(modifier = modifier) {
        RectangleTextButton(
            text = "L2",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.L2, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.L2, false)
            }
        )
        RectangleTextButton(
            text = "LB",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.LB, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.LB, false)
            }
        )
    }
}

@Composable
fun RightButtonGroup(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GameControllerHIDReportGenerator.Button, on: Boolean) -> Unit) {
    Row(modifier = modifier) {
        RectangleTextButton(
            text = "RB",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.RB, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.RB, false)
            }
        )
        RectangleTextButton(
            text = "R2",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.R2, true)
            },
            onUp = {
                onKeyEvent(GameControllerHIDReportGenerator.Button.R2, false)
            }
        )
    }
}