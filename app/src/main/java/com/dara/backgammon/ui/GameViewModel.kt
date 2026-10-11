package com.dara.backgammon.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.SoundPool
import com.dara.backgammon.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dara.backgammon.ai.*
import com.dara.backgammon.core.*
import com.dara.backgammon.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class Screen { MENU, SETUP, GAME }
enum class Mode { AI, LOCAL }
/** Single source of truth for which interactions are safe at any instant. */
enum class InteractionPhase { WAITING_FOR_ROLL, ROLLING, WAITING_FOR_MOVE, ANIMATING_MOVE, AI_THINKING, DOUBLE_OFFER, GAME_OVER }
data class MatchInfo(val whiteScore:Int=0,val blackScore:Int=0,val target:Int=5,val cube:Int=1,val cubeOwner:Player?=null,val thinking:Boolean=false,val pendingDoubleBy:Player?=null,val droppedBy:Player?=null,val timedOutBy:Player?=null,val crawfordUsed:Boolean=false,val crawfordGame:Boolean=false)
data class ClockState(val whiteMillis:Long=60_000,val blackMillis:Long=60_000,val turnMillis:Long=10_000,val active:Boolean=true)
private data class GameSnapshot(val game:TurnState,val match:MatchInfo,val clock:ClockState,val autoAssist:Boolean)
class GameViewModel(app:Application):AndroidViewModel(app) {
 private val prefs=Preferences(app);private val session=app.getSharedPreferences("saved_match",android.content.Context.MODE_PRIVATE);private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,48);private val soundPool=SoundPool.Builder().setMaxStreams(4).build();private val diceSound=soundPool.load(app,R.raw.dice_roll,1);private val checkerSound=soundPool.load(app,R.raw.checker_place,1)
 private val _settings=MutableStateFlow(prefs.load());val settings=_settings.asStateFlow();private val _screen=MutableStateFlow(Screen.MENU);val screen=_screen.asStateFlow();private val _game=MutableStateFlow(TurnState());val game=_game.asStateFlow();private val _rolling=MutableStateFlow(false);val rolling=_rolling.asStateFlow();private val _match=MutableStateFlow(MatchInfo());val match=_match.asStateFlow();private val _autoAssist=MutableStateFlow(false);val autoAssist=_autoAssist.asStateFlow();private val _clock=MutableStateFlow(newClock());val clock=_clock.asStateFlow();private val _hasSaved=MutableStateFlow(false);val hasSaved=_hasSaved.asStateFlow()
 private val history=ArrayDeque<GameSnapshot>();private var turnJob:Job?=null;private var clockTurn=Player.WHITE
 private fun snapshot()=GameSnapshot(_game.value.copy(position=_game.value.position.copyDeep()),_match.value.copy(thinking=false),_clock.value,_autoAssist.value)
 private fun rememberState(){history.addLast(snapshot())}
 private fun restore(s:GameSnapshot){_game.value=s.game.copy(position=s.game.position.copyDeep());_match.value=s.match.copy(thinking=false);_clock.value=s.clock;_autoAssist.value=s.autoAssist;clockTurn=_game.value.position.turn}
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
  var s=_game.value;if(!s.rolled){s=s.copy(dice=configuredRoll(),rolled=true);_game.value=s}
  val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)}
  for(m in seq){rememberState();s=GameEngine.afterMove(s,m);_game.value=s;playCheckerSound();delay(if(settings.value.animations)420 else 20)}
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
 private fun saveSession(){runCatching{
  val g=_game.value;val m=_match.value;val p=g.position;val c=_clock.value
  val json=JSONObject().apply{
   put("schema",2);put("points",p.points.joinToString(","));put("bw",p.barWhite);put("bb",p.barBlack);put("ow",p.offWhite);put("ob",p.offBlack);put("turn",p.turn.name)
   put("dice",g.dice.joinToString(","));put("rolled",g.rolled);put("winner",g.winner?.name?:"");put("ws",m.whiteScore);put("bs",m.blackScore);put("target",m.target)
   put("cube",m.cube);put("owner",m.cubeOwner?.name?:"");put("pending",m.pendingDoubleBy?.name?:"");put("dropped",m.droppedBy?.name?:"");put("timed",m.timedOutBy?.name?:"");put("crawfordUsed",m.crawfordUsed);put("crawfordGame",m.crawfordGame)
   put("mode",mode.name);put("auto",_autoAssist.value);put("clockWhite",c.whiteMillis);put("clockBlack",c.blackMillis);put("clockTurn",c.turnMillis);put("clockActive",c.active)
  }
  session.edit().putString("state",json.toString()).apply();_hasSaved.value=true
 }}
 private fun validPosition(p:Position):Boolean{
  if(p.points.any{it !in -15..15}||listOf(p.barWhite,p.barBlack,p.offWhite,p.offBlack).any{it !in 0..15})return false
  val white=p.points.sumOf{it.coerceAtLeast(0)}+p.barWhite+p.offWhite
  val black=p.points.sumOf{(-it).coerceAtLeast(0)}+p.barBlack+p.offBlack
  return white==15&&black==15
 }
 private fun loadSession(){
  val raw=session.getString("state",null)?:return
  val loaded=runCatching{
   val j=JSONObject(raw)
   fun player(key:String):Player?=j.optString(key).takeIf{it.isNotEmpty()}?.let{Player.valueOf(it)}
   val points=j.getString("points").split(',').map{it.toInt()}.toIntArray();require(points.size==24)
   val position=Position(points,j.getInt("bw"),j.getInt("bb"),j.getInt("ow"),j.getInt("ob"),Player.valueOf(j.getString("turn")));require(validPosition(position))
   val dice=j.optString("dice").takeIf{it.isNotEmpty()}?.split(',')?.map{it.toInt()}?:emptyList();require(dice.size<=4&&dice.all{it in 1..6})
   val match=MatchInfo(whiteScore=j.optInt("ws"),blackScore=j.optInt("bs"),target=j.optInt("target",5),cube=j.optInt("cube",1),cubeOwner=player("owner"),pendingDoubleBy=player("pending"),droppedBy=player("dropped"),timedOutBy=player("timed"),crawfordUsed=j.optBoolean("crawfordUsed"),crawfordGame=j.optBoolean("crawfordGame"))
   require(match.whiteScore>=0&&match.blackScore>=0&&match.target in 1..25&&match.cube in setOf(1,2,4,8,16,32,64))
   val game=TurnState(position,dice,j.optBoolean("rolled"),player("winner"));require(game.rolled||dice.isEmpty())
   val preset=_settings.value.clockPreset
   val clock=if(j.optInt("schema",1)>=2)ClockState(j.optLong("clockWhite",preset.bankMillis),j.optLong("clockBlack",preset.bankMillis),j.optLong("clockTurn",preset.turnMillis),j.optBoolean("clockActive",preset!=ClockPreset.OFF))else newClock()
   require(clock.whiteMillis>=0&&clock.blackMillis>=0&&clock.turnMillis>=0)
   _game.value=game;_match.value=match;_clock.value=clock;clockTurn=position.turn
   mode=runCatching{Mode.valueOf(j.optString("mode"))}.getOrDefault(Mode.AI);_autoAssist.value=j.optBoolean("auto");_hasSaved.value=true
  }.isSuccess
  if(!loaded){session.edit().remove("state").apply();_hasSaved.value=false}
 }
 fun continueGame(){if(_hasSaved.value){clockTurn=_game.value.position.turn;_screen.value=Screen.GAME;if(mode==Mode.AI&&_game.value.winner==null&&_match.value.pendingDoubleBy==null&&_game.value.position.turn==Player.BLACK)turnJob=viewModelScope.launch{aiTurn()}}}
 fun setup(){_screen.value=Screen.SETUP}
 fun start(m:Mode){mode=m;history.clear();_match.value=MatchInfo();_game.value=TurnState();resetClock();_screen.value=Screen.GAME;saveSession()}
 fun menu(){if(_screen.value==Screen.GAME)saveSession();_screen.value=Screen.MENU}
 fun matchWinner():Player?=MatchRules.winner(_match.value.whiteScore,_match.value.blackScore,_match.value.target)
 fun restart(){
  history.clear();turnJob?.cancel();_rolling.value=false
  val m=_match.value
  _match.value=if(matchWinner()!=null)MatchInfo(target=m.target)else{
   val crawford=MatchRules.nextIsCrawford(m.whiteScore,m.blackScore,m.target,m.crawfordUsed)
   m.copy(cube=1,cubeOwner=null,thinking=false,pendingDoubleBy=null,droppedBy=null,timedOutBy=null,crawfordUsed=m.crawfordUsed||crawford,crawfordGame=crawford)
  }
  _game.value=TurnState();resetClock();saveSession()
 }
 fun update(s:Settings){val clockChanged=s.clockPreset!=_settings.value.clockPreset;_settings.value=s;prefs.save(s);if(clockChanged)resetClock()}
 fun undo(){if(history.isEmpty())return;turnJob?.cancel();turnJob=null;_rolling.value=false;var restored:GameSnapshot;do{restored=history.removeLast()}while(mode==Mode.AI&&restored.game.position.turn==Player.BLACK&&history.isNotEmpty());restore(restored);saveSession()}
 private fun sound(kind:Int=ToneGenerator.TONE_PROP_BEEP){if(settings.value.sound)tone.startTone(kind,55)}
 private fun playDiceSound(){if(settings.value.sound)soundPool.play(diceSound,.76f,.76f,1,0,1f)}
 private fun playCheckerSound(){if(!settings.value.sound)return;val rate=when(settings.value.theme){BoardTheme.PREMIUM_WOOD->.92f;BoardTheme.BRUSHED_METAL->1.08f;BoardTheme.OBSIDIAN_IVORY->.82f;BoardTheme.CRYSTAL_GLASS->1.16f;BoardTheme.ROYAL_SAPPHIRE->1.0f};soundPool.play(checkerSound,.48f,.48f,1,0,rate)}
 private fun configuredRoll():List<Int> = DicePolicy.roll(settings.value.doublesRate)
 fun roll(){if(!humanCanRoll())return;val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;turnJob=viewModelScope.launch{playDiceSound();delay(if(settings.value.animations)1700 else 80);_game.value=_game.value.copy(dice=values,rolled=true);_rolling.value=false;turnJob=null;if(_autoAssist.value)autoAssistTurn()else autoEnd()}}
 private fun autoRollWhite(){if(!_autoAssist.value||_rolling.value||_game.value.winner!=null||_match.value.pendingDoubleBy!=null||_game.value.position.turn!=Player.WHITE||_game.value.rolled)return;val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;turnJob=viewModelScope.launch{playDiceSound();delay(if(settings.value.animations)1700 else 80);_game.value=_game.value.copy(dice=values,rolled=true);_rolling.value=false;turnJob=null;autoAssistTurn()}}
 fun move(m:Move){if(!humanCanMove()||m !in GameEngine.legalMoves(_game.value.position,_game.value.dice))return;rememberState();_game.value=GameEngine.afterMove(_game.value,m);playCheckerSound();if(_game.value.winner!=null)scoreGame();else autoEnd()}
 fun moveCombined(from:Int,to:Int):Boolean{
  if(!humanCanMove())return false
  val chosen=GameEngine.movePlans(_game.value.position,_game.value.dice,from).filter{it.moves.size>1&&it.to==to}.maxByOrNull{it.moves.size}?:return false
  rememberState();var s=_game.value;chosen.moves.forEach{s=GameEngine.afterMove(s,it)};_game.value=s;playCheckerSound()
  if(s.winner!=null)scoreGame()else autoEnd();return true
 }
 fun toggleAutoAssist(){_autoAssist.value=!_autoAssist.value;if(!_autoAssist.value){if(_game.value.position.turn==Player.WHITE)turnJob?.cancel()}else if(!_game.value.rolled&&_game.value.position.turn==Player.WHITE)autoRollWhite() else if(_game.value.rolled)autoAssistTurn()}
 private fun autoAssistTurn(){if(turnJob?.isActive==true)return;turnJob=viewModelScope.launch{var s=_game.value;val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)};for(m in seq){if(!_autoAssist.value)break;rememberState();s=GameEngine.afterMove(s,m);_game.value=s;playCheckerSound();delay(if(settings.value.animations)1180 else 30)};if(_autoAssist.value&&s.winner==null&&s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(300);_game.value=GameEngine.endTurn(s);if(mode==Mode.AI)aiTurn()}}}
 fun offerDouble(){val m=_match.value;val offerer=_game.value.position.turn;if(m.crawfordGame||_game.value.rolled||_rolling.value||m.pendingDoubleBy!=null||m.cube>=64||(m.cubeOwner!=null&&m.cubeOwner!=offerer))return;_match.value=m.copy(pendingDoubleBy=offerer);sound();if(mode==Mode.AI&&offerer==Player.WHITE){turnJob=viewModelScope.launch{delay(650);if(BackgammonAi.shouldTakeDouble(_game.value.position,Player.BLACK))acceptDouble()else declineDouble()}}}
 fun acceptDouble(){val m=_match.value;val offerer=m.pendingDoubleBy?:return;_match.value=m.copy(cube=m.cube*2,cubeOwner=offerer.other(),pendingDoubleBy=null);if(mode==Mode.AI&&offerer==Player.BLACK){turnJob=viewModelScope.launch{aiTurn(skipCubeOffer=true)}}}
 fun declineDouble(){val m=_match.value;val offerer=m.pendingDoubleBy?:return;val loser=offerer.other();_match.value=if(offerer==Player.WHITE)m.copy(whiteScore=m.whiteScore+m.cube,pendingDoubleBy=null,droppedBy=loser)else m.copy(blackScore=m.blackScore+m.cube,pendingDoubleBy=null,droppedBy=loser);_game.value=_game.value.copy(winner=offerer);_rolling.value=false}
 private fun scoreGame(){val s=_game.value;val w=s.winner?:return;val loser=w.other();val multiplier=when{ s.position.off(loser)>0->1;s.position.bar(loser)>0||(0..23).any{it in (if(w==Player.WHITE) 0..5 else 18..23) && s.position.points[it]*loser.sign>0}->3;else->2};val points=_match.value.cube*multiplier;_match.value=if(w==Player.WHITE)_match.value.copy(whiteScore=_match.value.whiteScore+points)else _match.value.copy(blackScore=_match.value.blackScore+points)}
 private fun autoEnd(){turnJob=viewModelScope.launch{val s=_game.value;if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(if(settings.value.animations)860 else 120);_game.value=GameEngine.endTurn(_game.value);if(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)aiTurn()}}}
 private suspend fun aiTurn(skipCubeOffer:Boolean=false){val cube=_match.value;if(!skipCubeOffer&&!cube.crawfordGame&&cube.cube<64&&cube.pendingDoubleBy==null&&(cube.cubeOwner==null||cube.cubeOwner==Player.BLACK)&&BackgammonAi.shouldOfferDouble(_game.value.position,Player.BLACK)){_match.value=cube.copy(pendingDoubleBy=Player.BLACK,thinking=false);return};_match.value=_match.value.copy(thinking=true);delay(450);val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;playDiceSound();delay(if(settings.value.animations)1700 else 80);var s=_game.value.copy(dice=values,rolled=true);_game.value=s;_rolling.value=false;delay(520);val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)};for(m in seq){rememberState();s=GameEngine.afterMove(s,m);_game.value=s;playCheckerSound();delay(if(settings.value.animations)1180 else 30)};if(s.winner!=null)scoreGame() else _game.value=GameEngine.endTurn(s);_match.value=_match.value.copy(thinking=false);if(_autoAssist.value&&_game.value.winner==null)viewModelScope.launch{delay(120);turnJob=null;autoRollWhite()}}
 override fun onCleared(){if(_screen.value==Screen.GAME)saveSession();tone.release();soundPool.release()}
}
