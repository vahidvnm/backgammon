package com.arena.backgammon.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arena.backgammon.ai.*
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen { MENU, GAME }
enum class Mode { AI, LOCAL }
class GameViewModel(app:Application):AndroidViewModel(app) {
    private val prefs=Preferences(app); private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,55)
    private val _settings=MutableStateFlow(prefs.load()); val settings=_settings.asStateFlow()
    private val _screen=MutableStateFlow(Screen.MENU); val screen=_screen.asStateFlow()
    private val _game=MutableStateFlow(TurnState()); val game=_game.asStateFlow()
    private val _rolling=MutableStateFlow(false); val rolling=_rolling.asStateFlow()
    var mode=Mode.AI; private set
    fun start(m:Mode){mode=m;_game.value=TurnState();_screen.value=Screen.GAME}
    fun menu(){_screen.value=Screen.MENU}
    fun restart(){_game.value=TurnState()}
    fun update(s:Settings){_settings.value=s;prefs.save(s)}
    fun click(){if(settings.value.sound)tone.startTone(ToneGenerator.TONE_PROP_BEEP,45)}
    fun roll(){if(_rolling.value||_game.value.rolled||_game.value.winner!=null)return; viewModelScope.launch { _rolling.value=true;click();if(settings.value.animations)delay(700);val a=Random.nextInt(1,7);val b=Random.nextInt(1,7);_game.value=_game.value.copy(dice=GameEngine.rollValues(a,b),rolled=true);_rolling.value=false;autoEnd() } }
    fun move(m:Move){ if(m !in GameEngine.legalMoves(_game.value.position,_game.value.dice))return; _game.value=GameEngine.afterMove(_game.value,m);click();autoEnd() }
    private fun autoEnd(){viewModelScope.launch { var s=_game.value;if(s.winner!=null)return@launch;if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(350);_game.value=GameEngine.endTurn(_game.value);if(mode==Mode.AI&&_game.value.position.turn==Player.BLACK) aiTurn()} } }
    private suspend fun aiTurn(){delay(500);val a=Random.nextInt(1,7);val b=Random.nextInt(1,7);var s=_game.value.copy(dice=GameEngine.rollValues(a,b),rolled=true);_game.value=s;delay(600);val seq=BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty);for(m in seq){s=GameEngine.afterMove(s,m);_game.value=s;delay(350)};if(s.winner==null){_game.value=GameEngine.endTurn(s)}}
    override fun onCleared(){tone.release()}
}
