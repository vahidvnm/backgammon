package com.dara.backgammon.ui

import android.app.Application
import com.dara.backgammon.audio.GameAudio
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dara.backgammon.ai.*
import com.dara.backgammon.core.*
import com.dara.backgammon.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Screen { MENU, SETUP, GAME }
/** Single source of truth for which interactions are safe at any instant. */
enum class InteractionPhase { WAITING_FOR_ROLL, ROLLING, WAITING_FOR_MOVE, ANIMATING_MOVE, AI_THINKING, DOUBLE_OFFER, GAME_OVER }
private data class GameSnapshot(val game:TurnState,val match:MatchInfo,val clock:ClockState,val autoAssist:Boolean)
class GameViewModel(app:Application):AndroidViewModel(app) {
 private val prefs=Preferences(app);private val savedMatches=SavedMatchStore(app);private val audio=GameAudio(app)
 private val _settings=MutableStateFlow(prefs.load());val settings=_settings.asStateFlow();private val _screen=MutableStateFlow(Screen.MENU);val screen=_screen.asStateFlow();private val _game=MutableStateFlow(TurnState());val game=_game.asStateFlow();private val _rolling=MutableStateFlow(false);val rolling=_rolling.asStateFlow();private val _match=MutableStateFlow(MatchInfo());val match=_match.asStateFlow();private val _autoAssist=MutableStateFlow(false);val autoAssist=_autoAssist.asStateFlow();private val _clock=MutableStateFlow(newClock());val clock=_clock.asStateFlow();private val _hasSaved=MutableStateFlow(false);val hasSaved=_hasSaved.asStateFlow();private val _showDiceGestureHint=MutableStateFlow(prefs.shouldShowDiceGestureHint());val showDiceGestureHint=_showDiceGestureHint.asStateFlow()
 private val history=ArrayDeque<GameSnapshot>();private var turnJob:Job?=null;private var clockTurn=Player.WHITE;private var appInForeground=true
 private fun cancelActiveTurn(){turnJob?.cancel();turnJob=null;_rolling.value=false;if(!_game.value.rolled&&_game.value.dice.isNotEmpty())_game.value=_game.value.copy(dice=emptyList());_match.value=_match.value.copy(thinking=false)}
 private fun snapshot()=GameSnapshot(_game.value.copy(position=_game.value.position.copyDeep()),_match.value.copy(thinking=false),_clock.value,_autoAssist.value)
 private fun rememberState(){history.addLast(snapshot())}
 private fun restore(s:GameSnapshot){_game.value=s.game.copy(position=s.game.position.copyDeep());_match.value=s.match.copy(thinking=false);_clock.value=s.clock;_autoAssist.value=s.autoAssist;clockTurn=_game.value.position.turn}
 init{loadSession();viewModelScope.launch{var last=android.os.SystemClock.elapsedRealtime();while(isActive){delay(100);val now=android.os.SystemClock.elapsedRealtime();val delta=(now-last).coerceAtMost(250);last=now;tickClock(delta)}};viewModelScope.launch{while(isActive){delay(750);if(_screen.value==Screen.GAME&&!_rolling.value&&!_match.value.thinking&&turnJob?.isActive!=true)saveSession()}}}
 var mode=Mode.AI;private set
 private fun newClock():ClockState{val c=_settings.value.clockPreset;return ClockState(c.bankMillis,c.bankMillis,c.turnMillis,c!=ClockPreset.OFF)}
 private fun resetClock(){_clock.value=newClock();clockTurn=_game.value.position.turn}
 private fun tickClock(delta:Long){
  val preset=_settings.value.clockPreset;if(!appInForeground||preset==ClockPreset.OFF||_screen.value!=Screen.GAME||_game.value.winner!=null)return
  val turn=_game.value.position.turn;val turnChanged=turn!=clockTurn
  val phase=interactionPhase()
  val shouldRun=ClockRunPolicy.shouldRun(mode,turn,phase in setOf(InteractionPhase.WAITING_FOR_ROLL,InteractionPhase.WAITING_FOR_MOVE),phase==InteractionPhase.GAME_OVER,phase==InteractionPhase.DOUBLE_OFFER)
  val tick=MatchClockEngine.tick(_clock.value,turn,turnChanged,shouldRun,delta,preset.turnMillis)
  if(turnChanged)clockTurn=turn
  _clock.value=tick.clock
  if(tick.timedOut){val m=_match.value;_match.value=if(turn==Player.WHITE)m.copy(blackScore=m.blackScore+m.cube,timedOutBy=turn)else m.copy(whiteScore=m.whiteScore+m.cube,timedOutBy=turn);_game.value=_game.value.copy(winner=turn.other())}
  else if(tick.turnExpired&&turnJob?.isActive!=true){_clock.value=_clock.value.copy(turnMillis=preset.turnMillis);forceFinishTurn()}
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
 private fun saveSession(){
  val saved=SavedSession(_game.value.copy(position=_game.value.position.copyDeep()),_match.value.copy(thinking=false),_clock.value,mode,_autoAssist.value)
  if(savedMatches.save(saved))_hasSaved.value=true
 }
 private fun loadSession(){
  val saved=savedMatches.load(newClock())?:return
  _game.value=saved.game;_match.value=saved.match;_clock.value=saved.clock.copy(active=_settings.value.clockPreset!=ClockPreset.OFF);clockTurn=saved.game.position.turn
  mode=saved.mode;_autoAssist.value=saved.autoAssist;_hasSaved.value=true
 }
 fun onForeground(){
  appInForeground=true;clockTurn=_game.value.position.turn
  val preset=_settings.value.clockPreset
  _clock.value=_clock.value.copy(active=preset!=ClockPreset.OFF)
  ensureAiProgress()
 }
 fun onBackground(){
  appInForeground=false
  if(_screen.value==Screen.GAME){cancelActiveTurn();saveSession()}
 }
 private fun ensureAiProgress(){
  if(_screen.value==Screen.GAME&&mode==Mode.AI&&_game.value.winner==null&&_match.value.pendingDoubleBy==null&&_game.value.position.turn==Player.BLACK&&turnJob?.isActive!=true){
   _rolling.value=false;_match.value=_match.value.copy(thinking=false);turnJob=viewModelScope.launch{aiTurn()}
  }
 }
 fun continueGame(){if(_hasSaved.value){clockTurn=_game.value.position.turn;_screen.value=Screen.GAME;ensureAiProgress()}}
 fun setup(){_screen.value=Screen.SETUP}
 fun start(m:Mode){cancelActiveTurn();mode=m;history.clear();_match.value=MatchInfo(target=_settings.value.matchTarget);_game.value=TurnState();resetClock();_screen.value=Screen.GAME;saveSession()}
 fun menu(){cancelActiveTurn();if(_screen.value==Screen.GAME)saveSession();_screen.value=Screen.MENU}
 fun matchWinner():Player?=MatchRules.winner(_match.value.whiteScore,_match.value.blackScore,_match.value.target)
 fun restart(){
  history.clear();cancelActiveTurn()
  val m=_match.value
  _match.value=if(matchWinner()!=null)MatchInfo(target=m.target)else{
   val crawford=MatchRules.nextIsCrawford(m.whiteScore,m.blackScore,m.target,m.crawfordUsed)
   m.copy(cube=1,cubeOwner=null,thinking=false,pendingDoubleBy=null,droppedBy=null,timedOutBy=null,crawfordUsed=m.crawfordUsed||crawford,crawfordGame=crawford)
  }
  _game.value=TurnState();resetClock();saveSession()
 }
 fun update(s:Settings){
  val clockChanged=s.clockPreset!=_settings.value.clockPreset;_settings.value=s;prefs.save(s)
  if(clockChanged)resetClock() else if(_clock.value.active!=(s.clockPreset!=ClockPreset.OFF))_clock.value=_clock.value.copy(active=s.clockPreset!=ClockPreset.OFF)
  ensureAiProgress()
 }
 fun undo(){
  if(history.isEmpty())return
  cancelActiveTurn()
  var restored=history.removeLast()
  if(mode==Mode.AI){
   // Discard AI snapshots, then rewind all human moves while preserving that turn's exact dice.
   while(history.isNotEmpty()&&!UndoPolicy.isHumanRolledState(restored.game))restored=history.removeLast()
   if(UndoPolicy.isHumanRolledState(restored.game))while(history.lastOrNull()?.game?.let(UndoPolicy::isHumanRolledState)==true)restored=history.removeLast()
  }
  restore(restored);saveSession()
 }
 private fun playDiceSound()=audio.playDice(settings.value.sound)
 private fun playCheckerSound()=audio.playChecker(settings.value.sound,settings.value.theme)
 private fun configuredRoll():List<Int> = DicePolicy.roll(settings.value.doublesRate)
 fun roll(){if(!humanCanRoll())return;if(_showDiceGestureHint.value){_showDiceGestureHint.value=false;prefs.markDiceGestureHintSeen()};rememberState();val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;turnJob=viewModelScope.launch{playDiceSound();delay(if(settings.value.animations)1700 else 80);_game.value=_game.value.copy(dice=values,rolled=true);_rolling.value=false;turnJob=null;saveSession();if(_autoAssist.value)autoAssistTurn()else autoEnd()}}
 private fun autoRollWhite(){if(!_autoAssist.value||_rolling.value||_game.value.winner!=null||_match.value.pendingDoubleBy!=null||_game.value.position.turn!=Player.WHITE||_game.value.rolled)return;rememberState();val values=configuredRoll();_game.value=_game.value.copy(dice=values,rolled=false);_rolling.value=true;turnJob=viewModelScope.launch{playDiceSound();delay(if(settings.value.animations)1700 else 80);_game.value=_game.value.copy(dice=values,rolled=true);_rolling.value=false;turnJob=null;saveSession();autoAssistTurn()}}
 fun move(m:Move){if(!humanCanMove()||m !in GameEngine.legalMoves(_game.value.position,_game.value.dice))return;rememberState();_game.value=GameEngine.afterMove(_game.value,m);playCheckerSound();saveSession();if(_game.value.winner!=null)scoreGame();else autoEnd()}
 fun moveCombined(from:Int,to:Int):Boolean{
  if(!humanCanMove())return false
  val chosen=GameEngine.movePlans(_game.value.position,_game.value.dice,from).filter{it.moves.size>1&&it.to==to}.maxByOrNull{it.moves.size}?:return false
  rememberState();var s=_game.value;chosen.moves.forEach{s=GameEngine.afterMove(s,it)};_game.value=s;playCheckerSound();saveSession()
  if(s.winner!=null)scoreGame()else autoEnd();return true
 }
 fun toggleAutoAssist(){_autoAssist.value=!_autoAssist.value;if(!_autoAssist.value){if(_game.value.position.turn==Player.WHITE)cancelActiveTurn()}else if(!_game.value.rolled&&_game.value.position.turn==Player.WHITE)autoRollWhite() else if(_game.value.rolled)autoAssistTurn()}
 private fun autoAssistTurn(){if(turnJob?.isActive==true)return;turnJob=viewModelScope.launch{var s=_game.value;val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)};for(m in seq){if(!_autoAssist.value)break;rememberState();s=GameEngine.afterMove(s,m);_game.value=s;playCheckerSound();delay(if(settings.value.animations)1000 else 30)};if(_autoAssist.value&&s.winner==null&&s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(300);_game.value=GameEngine.endTurn(s);if(mode==Mode.AI)aiTurn()}}}
 fun offerDouble(){val m=_match.value;val offerer=_game.value.position.turn;if(m.crawfordGame||_game.value.rolled||_rolling.value||m.pendingDoubleBy!=null||m.cube>=64||(m.cubeOwner!=null&&m.cubeOwner!=offerer))return;_match.value=m.copy(pendingDoubleBy=offerer);if(mode==Mode.AI&&offerer==Player.WHITE){turnJob=viewModelScope.launch{delay(650);if(BackgammonAi.shouldTakeDouble(_game.value.position,Player.BLACK,_match.value))acceptDouble()else declineDouble()}}}
 fun acceptDouble(){val m=_match.value;val offerer=m.pendingDoubleBy?:return;_match.value=m.copy(cube=m.cube*2,cubeOwner=offerer.other(),pendingDoubleBy=null);if(mode==Mode.AI&&offerer==Player.BLACK){turnJob=viewModelScope.launch{aiTurn(skipCubeOffer=true)}}}
 fun declineDouble(){val m=_match.value;val offerer=m.pendingDoubleBy?:return;val loser=offerer.other();_match.value=if(offerer==Player.WHITE)m.copy(whiteScore=m.whiteScore+m.cube,pendingDoubleBy=null,droppedBy=loser)else m.copy(blackScore=m.blackScore+m.cube,pendingDoubleBy=null,droppedBy=loser);_game.value=_game.value.copy(winner=offerer);_rolling.value=false}
 private fun scoreGame(){val s=_game.value;val w=s.winner?:return;val loser=w.other();val multiplier=when{ s.position.off(loser)>0->1;s.position.bar(loser)>0||(0..23).any{it in (if(w==Player.WHITE) 0..5 else 18..23) && s.position.points[it]*loser.sign>0}->3;else->2};val points=_match.value.cube*multiplier;_match.value=if(w==Player.WHITE)_match.value.copy(whiteScore=_match.value.whiteScore+points)else _match.value.copy(blackScore=_match.value.blackScore+points)}
 private fun autoEnd(){turnJob=viewModelScope.launch{val s=_game.value;if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty())){delay(if(settings.value.animations)860 else 120);_game.value=GameEngine.endTurn(_game.value);if(mode==Mode.AI&&_game.value.position.turn==Player.BLACK)aiTurn()}}}
 private suspend fun aiTurn(skipCubeOffer:Boolean=false){
  val cube=_match.value
  if(!_game.value.rolled&&!skipCubeOffer&&!cube.crawfordGame&&cube.cube<64&&cube.pendingDoubleBy==null&&(cube.cubeOwner==null||cube.cubeOwner==Player.BLACK)&&BackgammonAi.shouldOfferDouble(_game.value.position,Player.BLACK,cube)){
   _match.value=cube.copy(pendingDoubleBy=Player.BLACK,thinking=false);return
  }
  _match.value=_match.value.copy(thinking=true)
  try{
   var s=_game.value
   if(!s.rolled){
    delay(450);val values=configuredRoll();_game.value=s.copy(dice=values,rolled=false);_rolling.value=true;playDiceSound()
    delay(if(settings.value.animations)1700 else 80);s=_game.value.copy(dice=values,rolled=true);_game.value=s;_rolling.value=false
   }
   delay(520)
   val seq=withContext(Dispatchers.Default){BackgammonAi.chooseSequence(s.position,s.dice,settings.value.difficulty)}
   for(m in seq){rememberState();s=GameEngine.afterMove(s,m);_game.value=s;playCheckerSound();delay(if(settings.value.animations)1000 else 30)}
   if(s.winner!=null)scoreGame() else if(s.rolled&&(s.dice.isEmpty()||GameEngine.legalMoves(s.position,s.dice).isEmpty()))_game.value=GameEngine.endTurn(s)
  }finally{
   _rolling.value=false;_match.value=_match.value.copy(thinking=false)
  }
  if(_autoAssist.value&&_game.value.winner==null)viewModelScope.launch{delay(120);turnJob=null;autoRollWhite()}
 }
 override fun onCleared(){cancelActiveTurn();if(_screen.value==Screen.GAME)saveSession();audio.release()}
}
