package com.arena.backgammon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arena.backgammon.ui.BackgammonApp

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();immersive();setContent{BackgammonApp()}}
 override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus)immersive()}
 private fun immersive(){WindowCompat.setDecorFitsSystemWindows(window,false);WindowInsetsControllerCompat(window,window.decorView).apply{hide(WindowInsetsCompat.Type.systemBars());systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}}
}
