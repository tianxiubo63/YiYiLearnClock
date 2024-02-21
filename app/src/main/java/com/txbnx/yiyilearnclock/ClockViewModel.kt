package com.txbnx.yiyilearnclock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ClockViewModel : ViewModel(){
    var hour by mutableIntStateOf(12)
    var minute by mutableIntStateOf(15)
    //是否显示小时的数字
    var showHourNumber by mutableStateOf(true)
    //是否显示分钟和秒钟的数字
    var showMinSecNumber by mutableStateOf(true)
    //是否显示指针类型文字
    var showHansTypeText by mutableStateOf(true)
    //是否在指针上显示当前数值
    var showHansValue by mutableStateOf(true)
    //是否显示数字时间
    var showDigitalTime by mutableStateOf(true)
}