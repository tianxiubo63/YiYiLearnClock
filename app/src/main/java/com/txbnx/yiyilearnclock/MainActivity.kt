package com.txbnx.yiyilearnclock

import android.graphics.Paint.FontMetrics
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.txbnx.yiyilearnclock.ui.theme.YiYiLearnClockTheme
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YiYiLearnClockTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun Main(modifier: Modifier = Modifier) {
    //
    Column(modifier.padding(8.dp)){
        Row(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)){
            Dial(modifier)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(0.dp, 20.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center){
            DigitTime()
        }
        ControlArea()
    }
}

@Composable
fun Dial(modifier: Modifier = Modifier){
    val viewModel: ClockViewModel = viewModel()
    val currentHour = Calendar.getInstance().get(Calendar.HOUR)
    val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
    viewModel.hour = if(currentHour ==0) 12 else currentHour
    viewModel.minute = currentMinute
    val resolver: FontFamily.Resolver = LocalFontFamilyResolver.current
    val fontFamily by remember {
        mutableStateOf(FontFamily(
            Font(R.font.bear_hard_candy, weight = FontWeight.Normal)
        ))
    }
    val typeFace by remember {
        mutableStateOf(resolver.resolve(fontFamily).value as Typeface)
    }
    var fingerX by remember{ mutableFloatStateOf(0.0f) }
    var fingerY by remember{ mutableFloatStateOf(0.0f) }
    //0 表示操作的是时针, 1 表示操作的是分针
    var operateClockHands by remember{ mutableIntStateOf(-1) }
    var lastDraggedMinute by remember{ mutableIntStateOf(viewModel.minute) }

    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures(onPress = {
            operateClockHands = -1
            fingerX = it.x
            fingerY = it.y
            //度数非弧度
            val hourAngle = ((viewModel.hour % 12) * 30 + viewModel.minute * 0.5) % 360
            val minuteAngle = ((viewModel.minute * 6).toDouble()) % 360
            //Log.d("hourAngle|minuteAngle", "$hourAngle|$minuteAngle")
            //计算点击点到表盘中心的连线的角度
            val angle = getPointAngle(fingerX, fingerY, size)
            //Log.d("angle", "$angle")
            val hourAngleDelta = getMinAngleDelta(angle, hourAngle)
            val minuteAngleDelta = getMinAngleDelta(angle, minuteAngle)
            //允许靠近时针或者分针15度以内的范围内操作
            val canOperationDelta = 15
            if(hourAngleDelta < minuteAngleDelta && hourAngleDelta < canOperationDelta){
                operateClockHands = 0
            }else if(minuteAngleDelta <= hourAngleDelta && minuteAngleDelta < canOperationDelta){
                operateClockHands = 1
            }
            //Log.d("operateClockHands", "$operateClockHands")
        })
    }.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            //Log.d("detectDragGestures","detectDragGestures")
            if (change.positionChange() != Offset.Zero) change.consume()
            fingerX += dragAmount.x
            fingerY += dragAmount.y
            fingerX = if(fingerX < 0) 0f else fingerX
            fingerX = if(fingerX > size.width) size.width.toFloat() else fingerX
            fingerY = if(fingerY < 0) 0f else fingerY
            fingerY = if(fingerY > size.height) size.height.toFloat() else fingerY
            //Log.d("fingerX|fingerY", "$fingerX|$fingerY")
            val fingerAngle = getPointAngle(fingerX, fingerY, size)
            if(operateClockHands == 0){
                //操作时针
                val hour = (fingerAngle / 30).toInt()
                viewModel.hour = if(hour == 0) 12 else hour
            }else if(operateClockHands == 1){
                //操作分针
                val minute = (fingerAngle / 6).toInt()
                if(minute == lastDraggedMinute){
                    return@detectDragGestures
                }
                val minDelta = minute - lastDraggedMinute
                if(minDelta > 0 && (minDelta in 50..59)){
                    viewModel.hour = if(viewModel.hour == 1) 12 else viewModel.hour - 1
                }else if(minDelta < 0 && (minDelta in -59..-50)){
                    viewModel.hour = if(viewModel.hour == 12) 1 else viewModel.hour + 1
                }
                Log.d("minuteDelta",""+(minute - lastDraggedMinute))
                lastDraggedMinute = minute
                viewModel.minute = minute
            }
        }
    }.background(Color.Transparent), onDraw = {
        drawCircle(Color.White, center=center, radius = size.width / 2)
        for (i in 0..60){
            val angle = Math.toRadians(i * 6.0)
            if (i % 5 == 0){
                //绘制整点数字
                //小时数字
                if(i > 0 && viewModel.showHourNumber){
                    val text = (i / 5).toString()
                    val textSizeDp = 24f
                    val paint = android.graphics.Paint().apply {
                        color = Color.Black.toArgb()
                        textSize = textSizeDp.dp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = typeFace
                    }
                    val fontMetrics: FontMetrics = paint.fontMetrics
                    //计算文字中线与基线的距离
                    val offset = (fontMetrics.bottom - fontMetrics.top)/2 - fontMetrics.bottom
                    val textX = size.width / 2 + size.width / 2 * 0.86f * sin(angle).toFloat()
                    val textY = size.width / 2 - size.width / 2 * 0.86f * cos(angle).toFloat() + offset
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(text, textX, textY, paint)
                    }
                }
                //分钟和秒钟数字或者刻度
                if(i < 60){
                    if(viewModel.showMinSecNumber){
                        val text = i.toString()
                        val textSizeDp = 12f
                        val paint = android.graphics.Paint().apply {
                            color = Color.Gray.toArgb()
                            textSize = textSizeDp.dp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            //style = android.graphics.Paint.Style.STROKE
                            //strokeWidth = 1.0f.dp.toPx()
                            typeface = typeFace
                        }
                        drawIntoCanvas {
                            val lineStartX = size.width / 2 + size.width / 2 * 0.95f * sin(angle - Math.toRadians(6.0)).toFloat()
                            val lineStartY = size.width / 2 - size.width / 2 * 0.95f * cos(angle - Math.toRadians(6.0)).toFloat()
                            val lineEndX = size.width / 2 + size.width / 2 * 0.95f * sin(angle + Math.toRadians(6.0)).toFloat()
                            val lineEndY = size.width / 2 - size.width / 2 * 0.95f * cos(angle + Math.toRadians(6.0)).toFloat()
                            val path = android.graphics.Path()
                            path.moveTo(lineStartX, lineStartY)
                            path.lineTo(lineEndX, lineEndY)
                            //it.nativeCanvas.drawPath(path, paint)
                            //it.nativeCanvas.drawText(text, textX, textY, paint)
                            it.nativeCanvas.drawTextOnPath(text, path, 0f, 0f, paint)
                        }
                    }else{
                        val startX = size.width / 2 + size.width / 2 * 0.99f * sin(angle).toFloat()
                        val startY = size.width / 2 - size.width / 2 * 0.99f * cos(angle).toFloat()
                        val stopX = size.width / 2 + size.width / 2 * 0.97f * sin(angle).toFloat()
                        val stopY = size.width / 2 - size.width / 2 * 0.97f * cos(angle).toFloat()
                        drawLine(Color.Black, start = Offset(startX, startY), end = Offset(stopX, stopY), 2.8f.dp.toPx(), cap = StrokeCap.Round)
                    }
                }

            }else{
                //绘制非整点分秒针刻度
                val startX = size.width / 2 + size.width / 2 * 0.99f * sin(angle).toFloat()
                val startY = size.width / 2 - size.width / 2 * 0.99f * cos(angle).toFloat()
                val stopX = size.width / 2 + size.width / 2 * 0.98f * sin(angle).toFloat()
                val stopY = size.width / 2 - size.width / 2 * 0.98f * cos(angle).toFloat()
                drawLine(Color.Gray, start = Offset(startX, startY), end = Offset(stopX, stopY), 1.8f.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        //计算时针和分针的角度
        val hourAngle = Math.toRadians((viewModel.hour % 12) * 30 + viewModel.minute * 0.5)
        val minuteAngle = Math.toRadians((viewModel.minute * 6).toDouble())

        //绘制时针
        //时针是否需要设置一定的透明度
        val hourHandsSetAlpha = Math.toDegrees(getMinAngleDelta(hourAngle, minuteAngle)) < 15
        val hourStopX = size.width / 2 + size.width / 2 * 0.6f * sin(hourAngle).toFloat()
        val hourStopY = size.width / 2 - size.width / 2 * 0.6f * cos(hourAngle).toFloat()
        drawLine(Color.Black, start = Offset(size.width / 2, size.width / 2), end = Offset(hourStopX, hourStopY), 6.8f.dp.toPx(), cap = StrokeCap.Round,
            alpha = if(hourHandsSetAlpha) 0.2f else 1.0f)
        val paint = android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textSize = 14.dp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            //style = android.graphics.Paint.Style.STROKE
            //strokeWidth = 1.0f.dp.toPx()
            typeface = typeFace
        }
        if(viewModel.showHansTypeText){
            val needleX = size.width / 2 + size.width / 2 * 0.48f * sin(hourAngle).toFloat()
            val needleY = size.width / 2 - size.width / 2 * 0.48f * cos(hourAngle).toFloat()
            drawCircle(Color.Black, center=Offset(needleX, needleY), radius = 12f.dp.toPx(), alpha = if(hourHandsSetAlpha) 0.2f else 1.0f)
            drawIntoCanvas {
                val lineStartX = size.width / 2 + size.width / 2 * 0.46f * sin(hourAngle - Math.toRadians(6.0)).toFloat()
                val lineStartY = size.width / 2 - size.width / 2 * 0.46f * cos(hourAngle - Math.toRadians(6.0)).toFloat()
                val lineEndX = size.width / 2 + size.width / 2 * 0.46f * sin(hourAngle + Math.toRadians(6.0)).toFloat()
                val lineEndY = size.width / 2 - size.width / 2 * 0.46f * cos(hourAngle + Math.toRadians(6.0)).toFloat()
                val path = android.graphics.Path()
                path.moveTo(lineStartX, lineStartY)
                path.lineTo(lineEndX, lineEndY)
                //it.nativeCanvas.drawPath(path, paint)
                it.nativeCanvas.drawTextOnPath("时", path, 0f, 0f, paint)
            }
        }
        if(viewModel.showHansValue){
            val needleX = size.width / 2 + size.width / 2 * 0.356f * sin(hourAngle).toFloat()
            val needleY = size.width / 2 - size.width / 2 * 0.356f * cos(hourAngle).toFloat()
            drawCircle(Color.Black, center=Offset(needleX, needleY), radius = 12f.dp.toPx(), alpha = if(hourHandsSetAlpha) 0.2f else 1.0f)
            drawIntoCanvas {
                val lineStartX = size.width / 2 + size.width / 2 * 0.336f * sin(hourAngle - Math.toRadians(6.0)).toFloat()
                val lineStartY = size.width / 2 - size.width / 2 * 0.336f * cos(hourAngle - Math.toRadians(6.0)).toFloat()
                val lineEndX = size.width / 2 + size.width / 2 * 0.336f * sin(hourAngle + Math.toRadians(6.0)).toFloat()
                val lineEndY = size.width / 2 - size.width / 2 * 0.336f * cos(hourAngle + Math.toRadians(6.0)).toFloat()
                val path = android.graphics.Path()
                path.moveTo(lineStartX, lineStartY)
                path.lineTo(lineEndX, lineEndY)
                //it.nativeCanvas.drawPath(path, paint)
                it.nativeCanvas.drawTextOnPath("%02d".format(viewModel.hour), path, 0f, 0f, paint)
            }
        }
        //绘制分针
        val minuteStopX = size.width / 2 + size.width / 2 * 0.76f * sin(minuteAngle).toFloat()
        val minuteStopY = size.width / 2 - size.width / 2 * 0.76f * cos(minuteAngle).toFloat()
        drawLine(Color.Black, start = Offset(size.width / 2, size.width / 2), end = Offset(minuteStopX, minuteStopY), 5.0f.dp.toPx(), cap = StrokeCap.Round)
        if(viewModel.showHansTypeText){
            val needleX = size.width / 2 + size.width / 2 * 0.64f * sin(minuteAngle).toFloat()
            val needleY = size.width / 2 - size.width / 2 * 0.64f * cos(minuteAngle).toFloat()
            drawCircle(Color.Black, center=Offset(needleX, needleY), radius = 12f.dp.toPx())
            drawIntoCanvas {
                val lineStartX = size.width / 2 + size.width / 2 * 0.62f * sin(minuteAngle - Math.toRadians(6.0)).toFloat()
                val lineStartY = size.width / 2 - size.width / 2 * 0.62f * cos(minuteAngle - Math.toRadians(6.0)).toFloat()
                val lineEndX = size.width / 2 + size.width / 2 * 0.62f * sin(minuteAngle + Math.toRadians(6.0)).toFloat()
                val lineEndY = size.width / 2 - size.width / 2 * 0.62f * cos(minuteAngle + Math.toRadians(6.0)).toFloat()
                val path = android.graphics.Path()
                path.moveTo(lineStartX, lineStartY)
                path.lineTo(lineEndX, lineEndY)
                //it.nativeCanvas.drawPath(path, paint)
                it.nativeCanvas.drawTextOnPath("分", path, 0f, 0f, paint)
            }
        }
        if(viewModel.showHansValue){
            val needleX = size.width / 2 + size.width / 2 * 0.516f * sin(minuteAngle).toFloat()
            val needleY = size.width / 2 - size.width / 2 * 0.516f * cos(minuteAngle).toFloat()
            drawCircle(Color.Black, center=Offset(needleX, needleY), radius = 12f.dp.toPx())
            drawIntoCanvas {
                val lineStartX = size.width / 2 + size.width / 2 * 0.496f * sin(minuteAngle - Math.toRadians(6.0)).toFloat()
                val lineStartY = size.width / 2 - size.width / 2 * 0.496f * cos(minuteAngle - Math.toRadians(6.0)).toFloat()
                val lineEndX = size.width / 2 + size.width / 2 * 0.496f * sin(minuteAngle + Math.toRadians(6.0)).toFloat()
                val lineEndY = size.width / 2 - size.width / 2 * 0.496f * cos(minuteAngle + Math.toRadians(6.0)).toFloat()
                val path = android.graphics.Path()
                path.moveTo(lineStartX, lineStartY)
                path.lineTo(lineEndX, lineEndY)
                //it.nativeCanvas.drawPath(path, paint)
                it.nativeCanvas.drawTextOnPath("%02d".format(viewModel.minute), path, 0f, 0f, paint)
            }
        }
        //绘制表盘中心圆环
        drawCircle(Color.Black, center=center, radius = 8f.dp.toPx())

    })
}
fun getPointAngle(pointX: Float, pointY: Float, size: IntSize): Double {
    var angle = 0.0
    if(pointX > size.width / 2 && pointY <= size.height/2){
        angle = Math.toDegrees(
            atan((pointX - size.width / 2).toDouble()/(size.width / 2 - pointY).toDouble())
        )
    }else if (pointX > size.width / 2 && pointY > size.height/2) {
        angle = Math.toDegrees(
            atan((pointY - size.height / 2).toDouble() / (pointX - size.width / 2).toDouble())
        ) + 90
    }else if (pointX <= size.width / 2 && pointY > size.height/2) {
        angle = Math.toDegrees(
            atan((size.width / 2 - pointX).toDouble() / (pointY - size.height / 2).toDouble())
        ) + 180
    }else if (pointX <= size.width / 2 && pointY <= size.height/2) {
        angle = Math.toDegrees(
            atan((size.height / 2 - pointY).toDouble() / (size.width / 2 - pointX).toDouble())
        ) + 270
    }
    return angle
}
fun getMinAngleDelta(angle1: Double, angle2: Double): Double {
    var delta = abs(angle1 - angle2)
    delta = min(360-delta, delta)
    return delta
}
@Composable
fun DigitTime(){
    //数字时间
    val viewModel: ClockViewModel = viewModel()
    val hour = viewModel.hour
    val minute = viewModel.minute
    //显示数字时间
    val fontFamily by remember {
        mutableStateOf(FontFamily(
            Font(R.font.ds_digit, weight = FontWeight.Normal)
        ))
    }
    //使用Text显示小时和分钟,保留2位小数
    Text(text = "%02d:%02d".format(hour, minute), color = if(viewModel.showDigitalTime) Color.Black else Color.Transparent, fontSize = 66.sp, fontFamily = fontFamily)
}
@Composable
fun ControlArea(){
    val viewModel: ClockViewModel = viewModel()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp)){
        //组合项必须直接放到另一个组合项内部时才能使用weight属性
        Column(modifier = Modifier
            .fillMaxHeight()){
            Row(modifier = Modifier.fillMaxWidth()){
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally){
                    //显示一个Switch控件用来设置viewModel.showHourNumber的值
                    Switch(checked = viewModel.showHourNumber, onCheckedChange = {
                        viewModel.showHourNumber = it
                    })
                    Text(text = "显示小时数字", color = Color.Black, fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally){
                    //显示一个Switch控件用来设置viewModel.showMinSecNumber的值
                    Switch(checked = viewModel.showMinSecNumber, onCheckedChange = {
                        viewModel.showMinSecNumber = it
                    })
                    Text(text = "显示分/秒数字", color = Color.Black, fontSize = 14.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(0.dp, 16.dp, 0.dp, 0.dp)){
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally){
                    Switch(checked = viewModel.showHansTypeText, onCheckedChange = {
                        viewModel.showHansTypeText = it
                    })
                    Text(text = "显示指针类型", color = Color.Black, fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally){
                    Switch(checked = viewModel.showHansValue, onCheckedChange = {
                        viewModel.showHansValue = it
                    })
                    Text(text = "在指针上显示数字", color = Color.Black, fontSize = 14.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(0.dp, 16.dp, 0.dp, 0.dp)){
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally){
                    Switch(checked = viewModel.showDigitalTime, onCheckedChange = {
                        viewModel.showDigitalTime = it
                    })
                    Text(text = "显示数字时间", color = Color.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    YiYiLearnClockTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Main(modifier = Modifier.fillMaxSize())
        }
    }
}