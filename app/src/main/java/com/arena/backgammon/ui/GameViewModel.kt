package com.arena.backgammon.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.SoundPool
import com.arena.backgammon.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arena.backgammon.ai.*
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class Screen { MENU, SETUP, GAME }
enum class Mode { AI, LOCAL }
data class MatchInfo(val whiteScore:Int=0,val blackScore:Int=0,val target:Int=5,val cube:Int=1,val cubeOwner:Player?=null,val thinking:Boolean=false)
class GameViewModel(app:Application):AndroidViewModel(app) {
 private val prefs=Preferences(app);private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,48);private val soundPool=SoundPool.Builder().setMaxStreams(3).build();private val diceSound=soundPool.load(app,R.raw.dice_roll,1)
 private val _settings=MutableStateFlow(prefs.load());val settings=_settings.asStateFlow();private val _screen=MutableStateFlow(Screen.MENU);val screen=_screen.asStateFlow();private val _game=MutableStateFlow(TurnState());val game=_game.asStateFlow();private val _rolling=MutableStateFlow(false);val rolling=_rolling.asStateFlow();private val _match=MutableStateFlow(MatchInfo());val match=_match.asStateFlow();private val _autoAssist=MutableStateFlow(false);val autoAssist=_autoAssist.asStateFlow()
 private val history=ArrayDeque<TurnState>();private var turnJob:Job?=null
 var mode=Mode.AI;private set
 fun setup(){_screen.value=Screen.SETUP}
 fun start(m:Mode){mode=m;history.clear();_match.value=MatchInfo();_game.value=TurnState();_screen.value=Screen.GAME}
 fun menu(){_screen.value=Screen.MENU};fun restart(){history.clear();_game.value=TurnState()};fun update(s:Settings){_settings.value=s;prefs.save(s)}
 fun undo(){if(history.isEmpty())return;turnJob?.cancel();turnJob=null;_rolling.value=false;_match.value=_match.value.copy(thinking=false);var restored:TurnState;do{restored=history.removeLast()}while(mode==Mode.AI&&restored.position.turn==Player.BLACK&&history.isNotEmpty());_game.value=restored}
 private fun sound(kind:Int=ToneGenerator.TONE_PROP_BEEP){if(settings.value.sound)tone.startTone(kind,55)}
 private fun configuredRoll():List<Int>{val a=Random.nextInt(1,7);var b=Random.nextInt(1,7);val keep=when(settings.value.doublesRate){DoublesRate.NATURAL->1f;DoublesRate.REDUCED_20->.8f;DoublesRate.REDUCED_50->.5f;DoublesRate.NEVER->0f};if(a==b&&Random.nextFloat()>keep){b=Random.nextInt(1,6);if(b>=a)b++};return GameEngine.rollValues(a,b)}
 fun roll(){if(_rolling.value||_match.value.thinking||_game.value.rolled||_game.value.winner!=null||(mode==Mode.AI&&_game.value.position.turn==Player.BLACK))return;viewModelScope.launch{val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;if(settings.value.sound)soundPool.play(diceSound,.75f,.75f,1,0,1f);delay(if(settings.value.animations)1900 else 80);_game.value=_game.value.copy(dice=values,rolled=true);_rolling.value=false;if(_autoAssist.value)autoAssistTurn()else autoEnd()}}
 fun move(m:Move){if(_rolling.value||_match.value.thinking||(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)||m !in GameEngine.legalMoves(_game.value.position,_game.value.dice))return;history.addLast(_game.value.copy(position=_game.value.position.copyDeep()));_game.value=GameEngine.afterMove(_game.value,m);if(_game.value.winner!=null)scoreGame();else autoEnd()}
 fun moveCombined(from:Int,to:Int):Boolean{
  if(_rolling.value||_match.value.thinking||(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)||!_game.value.rolled)return false
  fun search(s:TurnState,current:Int,path:List<Move>):List<List<Move>>{if(path.size>=4||s.dice.isEmpty())return listOf(path);val next=GameEngine.legalMoves(s.position,s.dice).filter{it.from==current};return listOf(path)+next.flatMap{search(GameEngine.afterMove(s,it),it.to,path+it)}}
  val chosen=search(_game.value,from,emptyList()).filter{it.size>1&&it.last().to==to}.maxByOrNull{it.size}?:return false
  history.addLast(_game.value.copy(position=_game.value.position.copyDeep()));var s=_game.value;chosen.forEach{s=GameEngine.afterMove(s,it)};_game.value=s
  if(s.winner!=null)scoreGame()else autoEnd();return true
 }
 fun toggleAutoAssist(){_autoAssist.value=!_autoAssist.value;if(!_autoAssist.value){if(_game.value.position.turn==Player.WHITE)turnJob?.cancel()}else if(_game.value.rolled)autoAssistTurn()}
 private fun autoAssistTurn(){if(turnJob?.isActive==true)return;turnJob=viewModelScope.launch{var s=_game.value;val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)};for(m in seq){if(!_autoAssist.value)break;history.addLast(s.copy(position=s.position.copyDeep()));s=GameEngine.afterMove(s,m);_game.value=s;delay(if(settings.value.animations)1180 else 30)};if(_autoAssist.value&&s.winner==null&&s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(300);_game.value=GameEngine.endTurn(s);if(mode==Mode.AI)aiTurn()}}}
 fun offerDouble(){val m=_match.value;if(_game.value.rolled||m.cube>=64)return;_match.value=m.copy(cube=m.cube*2,cubeOwner=_game.value.position.turn);sound()}
 private fun scoreGame(){val s=_game.value;val w=s.winner?:return;val loser=w.other();val multiplier=when{ s.position.off(loser)>0->1;s.position.bar(loser)>0||(0..23).any{it in (if(w==Player.WHITE) 0..5 else 18..23) && s.position.points[it]*loser.sign>0}->3;else->2};val points=_match.value.cube*multiplier;_match.value=if(w==Player.WHITE)_match.value.copy(whiteScore=_match.value.whiteScore+points)else _match.value.copy(blackScore=_match.value.blackScore+points)}
 private fun autoEnd(){turnJob=viewModelScope.launch{val s=_game.value;if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(if(settings.value.animations)860 else 120);_game.value=GameEngine.endTurn(_game.value);if(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)aiTurn()}}}
 private suspend fun aiTurn(){_match.value=_match.value.copy(thinking=true);delay(450);val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;if(settings.value.sound)soundPool.play(diceSound,.68f,.68f,1,0,1f);delay(if(settings.value.animations)1900 else 80);var s=_game.value.copy(dice=values,rolled=true);_game.value=s;_rolling.value=false;delay(520);val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)};for(m in seq){history.addLast(s.copy(position=s.position.copyDeep()));s=GameEngine.afterMove(s,m);_game.value=s;delay(if(settings.value.animations)1180 else 30)};if(s.winner!=null)scoreGame() else _game.value=GameEngine.endTurn(s);_match.value=_match.value.copy(thinking=false);if(_autoAssist.value&&_game.value.winner==null)roll()}
 override fun onCleared(){tone.release();soundPool.release()}
}
