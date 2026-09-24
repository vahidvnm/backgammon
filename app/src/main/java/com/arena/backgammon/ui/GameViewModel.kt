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
import org.json.JSONObject
import kotlin.random.Random

enum class Screen { MENU, SETUP, GAME }
enum class Mode { AI, LOCAL }
/** Single source of truth for which interactions are safe at any instant. */
enum class InteractionPhase { WAITING_FOR_ROLL, ROLLING, WAITING_FOR_MOVE, ANIMATING_MOVE, AI_THINKING, DOUBLE_OFFER, GAME_OVER }
data class MatchInfo(val whiteScore:Int=0,val blackScore:Int=0,val target:Int=5,val cube:Int=1,val cubeOwner:Player?=null,val thinking:Boolean=false,val pendingDoubleBy:Player?=null,val droppedBy:Player?=null,val timedOutBy:Player?=null)
data class ClockState(val whiteMillis:Long=60_000,val blackMillis:Long=60_000,val turnMillis:Long=10_000,val active:Boolean=true)
class GameViewModel(app:Application):AndroidViewModel(app) {
 private val prefs=Preferences(app);private val session=app.getSharedPreferences("saved_match",android.content.Context.MODE_PRIVATE);private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,48);private val soundPool=SoundPool.Builder().setMaxStreams(3).build();private val diceSound=soundPool.load(app,R.raw.dice_roll,1)
 private val _settings=MutableStateFlow(prefs.load());val settings=_settings.asStateFlow();private val _screen=MutableStateFlow(Screen.MENU);val screen=_screen.asStateFlow();private val _game=MutableStateFlow(TurnState());val game=_game.asStateFlow();private val _rolling=MutableStateFlow(false);val rolling=_rolling.asStateFlow();private val _match=MutableStateFlow(MatchInfo());val match=_match.asStateFlow();private val _autoAssist=MutableStateFlow(false);val autoAssist=_autoAssist.asStateFlow();private val _clock=MutableStateFlow(newClock());val clock=_clock.asStateFlow();private val _hasSaved=MutableStateFlow(false);val hasSaved=_hasSaved.asStateFlow()
 private val history=ArrayDeque<TurnState>();private var turnJob:Job?=null;private var clockTurn=Player.WHITE
 init{loadSession();viewModelScope.launch{var last=android.os.SystemClock.elapsedRealtime();while(isActive){delay(100);val now=android.os.SystemClock.elapsedRealtime();val delta=(now-last).coerceAtMost(250);last=now;tickClock(delta)}};viewModelScope.launch{while(isActive){delay(750);if(_screen.value==Screen.GAME&&!_rolling.value&&!_match.value.thinking&&turnJob?.isActive!=true)saveSession()}}}
 var mode=Mode.AI;private set
 private fun newClock():ClockState{val c=_settings.value.clockPreset;return ClockState(c.bankMillis,c.bankMillis,c.turnMillis,c!=ClockPreset.OFF)}
 private fun resetClock(){_clock.value=newClock();clockTurn=_game.value.position.turn}
 private fun tickClock(delta:Long){
  val preset=_settings.value.clockPreset;if(preset==ClockPreset.OFF||_screen.value!=Screen.GAME||_game.value.winner!=null)return
  val turn=_game.value.position.turn;if(turn!=clockTurn){clockTurn=turn;_clock.value=_clock.value.copy(turnMillis=preset.turnMillis)}
  if(interactionPhase() !in setOf(InteractionPhase.WAITING_FOR_ROLL,InteractionPhase.WAITING_FOR_MOVE))return
  val c=_clock.value;val bank=(if(turn==Player.WHITE)c.whiteMillis else c.blackMillis)-delta;val turnLeft=c.turnMillis-delta
  _clock.value=if(turn==Player.WHITE)c.copy(whiteMillis=bank.coerceAtLeast(0),turnMillis=turnLeft.coerceAtLeast(0))else c.copy(blackMillis=bank.coerceAtLeast(0),turnMillis=turnLeft.coerceAtLeast(0))
  if(bank<=0){val m=_match.value;_match.value=if(turn==Player.WHITE)m.copy(blackScore=m.blackScore+m.cube,timedOutBy=turn)else m.copy(whiteScore=m.whiteScore+m.cube,timedOutBy=turn);_game.value=_game.value.copy(winner=turn.other())}
  else if(turnLeft<=0&&turnJob?.isActive!=true){_clock.value=_clock.value.copy(turnMillis=preset.turnMillis);forceFinishTurn()}
 }
 private fun forceFinishTurn(){turnJob=viewModelScope.launch{
  var s=_game.value;if(!s.rolled)s=s.copy(dice=configuredRoll(),rolled=true)
  val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty,settings.value.aiPersona)}
  for(m in seq){history.addLast(s.copy(position=s.position.copyDeep()));s=GameEngine.afterMove(s,m);_game.value=s;delay(if(settings.value.animations)420 else 20)}
  if(s.winner!=null)scoreGame()else if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty()))_game.value=GameEngine.endTurn(s)
  if(mode==Mode.AI&&_game.value.winner==null&&_game.value.position.turn==Player.BLACK)aiTurn()
 }}
 fun interactionPhase():InteractionPhase=when{
  _game.value.winner!=null->InteractionPhase.GAME_OVER
  _match.value.pendingDoubleBy!=null->InteractionPhase.DOUBLE_OFFER
  _rolling.value->InteractionPhase.ROLLING
  _match.value.thinking||(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)->InteractionPhase.AI_THINKING
  turnJob?.isActive==true->InteractionPhase.ANIMATING_MOVE
  _game.value.rolled->InteractionPhase.WAITING_FOR_MOVE
  else->InteractionPhase.WAITING_FOR_ROLL
 }
 private fun humanCanMove()=interactionPhase()==InteractionPhase.WAITING_FOR_MOVE
 private fun humanCanRoll()=interactionPhase()==InteractionPhase.WAITING_FOR_ROLL
 private fun saveSession(){runCatching{val g=_game.value;val m=_match.value;val p=g.position;val json=JSONObject().apply{put("points",p.points.joinToString(","));put("bw",p.barWhite);put("bb",p.barBlack);put("ow",p.offWhite);put("ob",p.offBlack);put("turn",p.turn.name);put("dice",g.dice.joinToString(","));put("rolled",g.rolled);put("winner",g.winner?.name?:"");put("ws",m.whiteScore);put("bs",m.blackScore);put("target",m.target);put("cube",m.cube);put("owner",m.cubeOwner?.name?:"");put("pending",m.pendingDoubleBy?.name?:"");put("dropped",m.droppedBy?.name?:"");put("timed",m.timedOutBy?.name?:"");put("mode",mode.name);put("auto",_autoAssist.value)};session.edit().putString("state",json.toString()).apply();_hasSaved.value=true}}
 private fun loadSession(){val raw=session.getString("state",null)?:return;runCatching{val j=JSONObject(raw);fun player(key:String)=j.optString(key).takeIf{it.isNotEmpty()}?.let { Player.valueOf(it) };val points=j.getString("points").split(',').map(String::toInt).toIntArray();if(points.size!=24)return@runCatching;val position=Position(points,j.getInt("bw"),j.getInt("bb"),j.getInt("ow"),j.getInt("ob"),Player.valueOf(j.getString("turn")));val dice=j.optString("dice").takeIf{it.isNotEmpty()}?.split(',')?.map(String::toInt)?:emptyList();_game.value=TurnState(position,dice,j.optBoolean("rolled"),player("winner"));_match.value=MatchInfo(whiteScore=j.optInt("ws"),blackScore=j.optInt("bs"),target=j.optInt("target",5),cube=j.optInt("cube",1),cubeOwner=player("owner"),pendingDoubleBy=player("pending"),droppedBy=player("dropped"),timedOutBy=player("timed"));mode=runCatching{Mode.valueOf(j.optString("mode"))}.getOrDefault(Mode.AI);_autoAssist.value=j.optBoolean("auto");_hasSaved.value=true}}
 fun continueGame(){if(_hasSaved.value){resetClock();_screen.value=Screen.GAME;if(mode==Mode.AI&&_game.value.winner==null&&_match.value.pendingDoubleBy==null&&_game.value.position.turn==Player.BLACK)turnJob=viewModelScope.launch{aiTurn()}}}
 fun setup(){_screen.value=Screen.SETUP}
 fun start(m:Mode){mode=m;history.clear();_match.value=MatchInfo();_game.value=TurnState();resetClock();_screen.value=Screen.GAME;saveSession()}
 fun menu(){if(_screen.value==Screen.GAME)saveSession();_screen.value=Screen.MENU};fun restart(){history.clear();turnJob?.cancel();_game.value=TurnState();_rolling.value=false;_match.value=_match.value.copy(cube=1,cubeOwner=null,thinking=false,pendingDoubleBy=null,droppedBy=null,timedOutBy=null);resetClock()};fun update(s:Settings){val clockChanged=s.clockPreset!=_settings.value.clockPreset;_settings.value=s;prefs.save(s);if(clockChanged)resetClock()}
 fun undo(){if(history.isEmpty())return;turnJob?.cancel();turnJob=null;_rolling.value=false;_match.value=_match.value.copy(thinking=false);var restored:TurnState;do{restored=history.removeLast()}while(mode==Mode.AI&&restored.position.turn==Player.BLACK&&history.isNotEmpty());_game.value=restored}
 private fun sound(kind:Int=ToneGenerator.TONE_PROP_BEEP){if(settings.value.sound)tone.startTone(kind,55)}
 private fun configuredRoll():List<Int>{val a=Random.nextInt(1,7);var b=Random.nextInt(1,7);val keep=when(settings.value.doublesRate){DoublesRate.NATURAL->.5f;DoublesRate.REDUCED_20->.4f;DoublesRate.REDUCED_50->.25f;DoublesRate.NEVER->0f};if(a==b&&Random.nextFloat()>keep){b=Random.nextInt(1,6);if(b>=a)b++};return GameEngine.rollValues(a,b)}
 fun roll(){if(!humanCanRoll())return;viewModelScope.launch{val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;if(settings.value.sound)soundPool.play(diceSound,.75f,.75f,1,0,1f);delay(if(settings.value.animations)1700 else 80);_game.value=_game.value.copy(dice=values,rolled=true);_rolling.value=false;if(_autoAssist.value)autoAssistTurn()else autoEnd()}}
 fun move(m:Move){if(!humanCanMove()||m !in GameEngine.legalMoves(_game.value.position,_game.value.dice))return;history.addLast(_game.value.copy(position=_game.value.position.copyDeep()));_game.value=GameEngine.afterMove(_game.value,m);if(_game.value.winner!=null)scoreGame();else autoEnd()}
 fun moveCombined(from:Int,to:Int):Boolean{
  if(!humanCanMove())return false
  val chosen=GameEngine.movePlans(_game.value.position,_game.value.dice,from).filter{it.moves.size>1&&it.to==to}.maxByOrNull{it.moves.size}?:return false
  history.addLast(_game.value.copy(position=_game.value.position.copyDeep()));var s=_game.value;chosen.moves.forEach{s=GameEngine.afterMove(s,it)};_game.value=s
  if(s.winner!=null)scoreGame()else autoEnd();return true
 }
 fun toggleAutoAssist(){_autoAssist.value=!_autoAssist.value;if(!_autoAssist.value){if(_game.value.position.turn==Player.WHITE)turnJob?.cancel()}else if(_game.value.rolled)autoAssistTurn()}
 private fun autoAssistTurn(){if(turnJob?.isActive==true)return;turnJob=viewModelScope.launch{var s=_game.value;val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty,settings.value.aiPersona)};for(m in seq){if(!_autoAssist.value)break;history.addLast(s.copy(position=s.position.copyDeep()));s=GameEngine.afterMove(s,m);_game.value=s;delay(if(settings.value.animations)1180 else 30)};if(_autoAssist.value&&s.winner==null&&s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(300);_game.value=GameEngine.endTurn(s);if(mode==Mode.AI)aiTurn()}}}
 fun offerDouble(){val m=_match.value;val offerer=_game.value.position.turn;if(_game.value.rolled||_rolling.value||m.pendingDoubleBy!=null||m.cube>=64||(m.cubeOwner!=null&&m.cubeOwner!=offerer))return;_match.value=m.copy(pendingDoubleBy=offerer);sound();if(mode==Mode.AI&&offerer==Player.WHITE){turnJob=viewModelScope.launch{delay(650);if(BackgammonAi.shouldTakeDouble(_game.value.position,Player.BLACK))acceptDouble()else declineDouble()}}}
 fun acceptDouble(){val m=_match.value;val offerer=m.pendingDoubleBy?:return;_match.value=m.copy(cube=m.cube*2,cubeOwner=offerer.other(),pendingDoubleBy=null);if(mode==Mode.AI&&offerer==Player.BLACK){turnJob=viewModelScope.launch{aiTurn(skipCubeOffer=true)}}}
 fun declineDouble(){val m=_match.value;val offerer=m.pendingDoubleBy?:return;val loser=offerer.other();_match.value=if(offerer==Player.WHITE)m.copy(whiteScore=m.whiteScore+m.cube,pendingDoubleBy=null,droppedBy=loser)else m.copy(blackScore=m.blackScore+m.cube,pendingDoubleBy=null,droppedBy=loser);_game.value=_game.value.copy(winner=offerer);_rolling.value=false}
 private fun scoreGame(){val s=_game.value;val w=s.winner?:return;val loser=w.other();val multiplier=when{ s.position.off(loser)>0->1;s.position.bar(loser)>0||(0..23).any{it in (if(w==Player.WHITE) 0..5 else 18..23) && s.position.points[it]*loser.sign>0}->3;else->2};val points=_match.value.cube*multiplier;_match.value=if(w==Player.WHITE)_match.value.copy(whiteScore=_match.value.whiteScore+points)else _match.value.copy(blackScore=_match.value.blackScore+points)}
 private fun autoEnd(){turnJob=viewModelScope.launch{val s=_game.value;if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(if(settings.value.animations)860 else 120);_game.value=GameEngine.endTurn(_game.value);if(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)aiTurn()}}}
 private suspend fun aiTurn(skipCubeOffer:Boolean=false){val cube=_match.value;if(!skipCubeOffer&&cube.cube<64&&cube.pendingDoubleBy==null&&(cube.cubeOwner==null||cube.cubeOwner==Player.BLACK)&&BackgammonAi.shouldOfferDouble(_game.value.position,Player.BLACK)){_match.value=cube.copy(pendingDoubleBy=Player.BLACK,thinking=false);return};_match.value=_match.value.copy(thinking=true);delay(450);val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;if(settings.value.sound)soundPool.play(diceSound,.68f,.68f,1,0,1f);delay(if(settings.value.animations)1700 else 80);var s=_game.value.copy(dice=values,rolled=true);_game.value=s;_rolling.value=false;delay(520);val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty,settings.value.aiPersona)};for(m in seq){history.addLast(s.copy(position=s.position.copyDeep()));s=GameEngine.afterMove(s,m);_game.value=s;delay(if(settings.value.animations)1180 else 30)};if(s.winner!=null)scoreGame() else _game.value=GameEngine.endTurn(s);_match.value=_match.value.copy(thinking=false);if(_autoAssist.value&&_game.value.winner==null)roll()}
 override fun onCleared(){if(_screen.value==Screen.GAME)saveSession();tone.release();soundPool.release()}
}
