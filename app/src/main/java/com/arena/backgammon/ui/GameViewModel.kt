package com.arena.backgammon.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arena.backgammon.ai.*
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class Screen { MENU, GAME }
enum class Mode { AI, LOCAL }
data class MatchInfo(val whiteScore:Int=0,val blackScore:Int=0,val target:Int=5,val cube:Int=1,val cubeOwner:Player?=null,val thinking:Boolean=false)
class GameViewModel(app:Application):AndroidViewModel(app) {
 private val prefs=Preferences(app);private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,48)
 private val _settings=MutableStateFlow(prefs.load());val settings=_settings.asStateFlow();private val _screen=MutableStateFlow(Screen.MENU);val screen=_screen.asStateFlow();private val _game=MutableStateFlow(TurnState());val game=_game.asStateFlow();private val _rolling=MutableStateFlow(false);val rolling=_rolling.asStateFlow();private val _match=MutableStateFlow(MatchInfo());val match=_match.asStateFlow()
 private val history=ArrayDeque<TurnState>()
 var mode=Mode.AI;private set
 fun start(m:Mode){mode=m;history.clear();_match.value=MatchInfo();_game.value=TurnState();_screen.value=Screen.GAME}
 fun menu(){_screen.value=Screen.MENU};fun restart(){history.clear();_game.value=TurnState()};fun update(s:Settings){_settings.value=s;prefs.save(s)}
 fun undo(){if(mode==Mode.LOCAL&&!_rolling.value&&history.isNotEmpty())_game.value=history.removeLast()}
 private fun sound(kind:Int=ToneGenerator.TONE_PROP_BEEP){if(settings.value.sound)tone.startTone(kind,55)}
 fun roll(){if(_rolling.value||_game.value.rolled||_game.value.winner!=null)return;viewModelScope.launch{_rolling.value=true;sound(ToneGenerator.TONE_PROP_BEEP2);delay(if(settings.value.animations)850 else 80);val a=Random.nextInt(1,7);val b=Random.nextInt(1,7);_game.value=_game.value.copy(dice=GameEngine.rollValues(a,b),rolled=true);_rolling.value=false;autoEnd()}}
 fun move(m:Move){if(m !in GameEngine.legalMoves(_game.value.position,_game.value.dice))return;if(mode==Mode.LOCAL)history.addLast(_game.value.copy(position=_game.value.position.copyDeep()));_game.value=GameEngine.afterMove(_game.value,m);sound();if(_game.value.winner!=null)scoreGame();else autoEnd()}
 fun offerDouble(){val m=_match.value;if(_game.value.rolled||m.cube>=64)return;_match.value=m.copy(cube=m.cube*2,cubeOwner=_game.value.position.turn);sound()}
 private fun scoreGame(){val s=_game.value;val w=s.winner?:return;val loser=w.other();val multiplier=when{ s.position.off(loser)>0->1;s.position.bar(loser)>0||(0..23).any{it in (if(w==Player.WHITE) 0..5 else 18..23) && s.position.points[it]*loser.sign>0}->3;else->2};val points=_match.value.cube*multiplier;_match.value=if(w==Player.WHITE)_match.value.copy(whiteScore=_match.value.whiteScore+points)else _match.value.copy(blackScore=_match.value.blackScore+points)}
 private fun autoEnd(){viewModelScope.launch{val s=_game.value;if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(280);_game.value=GameEngine.endTurn(_game.value);if(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)aiTurn()}}}
 private suspend fun aiTurn(){_match.value=_match.value.copy(thinking=true);delay(450);val a=Random.nextInt(1,7);val b=Random.nextInt(1,7);var s=_game.value.copy(dice=GameEngine.rollValues(a,b),rolled=true);_game.value=s;delay(650);val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)};for(m in seq){s=GameEngine.afterMove(s,m);_game.value=s;delay(if(settings.value.animations)420 else 30)};if(s.winner!=null)scoreGame() else _game.value=GameEngine.endTurn(s);_match.value=_match.value.copy(thinking=false)}
 override fun onCleared(){tone.release()}
}
