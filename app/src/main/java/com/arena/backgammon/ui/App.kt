package com.arena.backgammon.ui

import android.os.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.backgammon.ai.Difficulty
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Gold=Color(0xffffd274);private val Glass=Color(0xcc11151b)
@Composable fun BackgammonApp(vm:GameViewModel=viewModel()){
 val screen by vm.screen.collectAsState()
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,surface=Color(0xff11151b))){
  Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xff253026),Color(0xff07090c))))){
   AnimatedContent(screen,label="navigation"){if(it==Screen.MENU)MainMenu(vm)else GameScreen(vm)}
  }
 }
}
@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit)=Column(modifier.clip(RoundedCornerShape(24.dp)).background(Glass).border(1.dp,Color.White.copy(.14f),RoundedCornerShape(24.dp)).padding(18.dp),content=content)
@Composable private fun MainMenu(vm:GameViewModel){val s by vm.settings.collectAsState();var dialog by remember{mutableStateOf("")};Row(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1.15f)){Text("BACKGAMMON",fontSize=42.sp,fontWeight=FontWeight.Black,letterSpacing=5.sp,color=Gold);Text("THE GRAND BOARD",fontSize=16.sp,letterSpacing=4.sp,color=Color.White.copy(.65f));Spacer(Modifier.height(20.dp));Text("Strategy, beautifully crafted.",fontSize=20.sp,color=Color.White.copy(.8f));Text("A premium three-dimensional board experience.",color=Color.White.copy(.52f))};Panel(Modifier.widthIn(320.dp,440.dp)){MenuAction("PLAY VS AI","Five strategic difficulty levels"){dialog="ai"};MenuAction("LOCAL MATCH","Pass-and-play on one device"){vm.start(Mode.LOCAL)};Row{SmallAction("THEMES",Modifier.weight(1f)){dialog="themes"};Spacer(Modifier.width(10.dp));SmallAction("SETTINGS",Modifier.weight(1f)){dialog="settings"}};Row{SmallAction("STATISTICS",Modifier.weight(1f)){dialog="stats"};Spacer(Modifier.width(10.dp));SmallAction("ABOUT",Modifier.weight(1f)){dialog="about"}}}};if(dialog.isNotEmpty())Dialog(dialog,s,vm,{dialog=""})}
@Composable private fun MenuAction(title:String,sub:String,go:()->Unit){Button(go,Modifier.fillMaxWidth().height(66.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(Color.White.copy(.1f)),border=BorderStroke(1.dp,Color.White.copy(.1f))){Column(Modifier.fillMaxWidth()){Text(title,fontWeight=FontWeight.Bold,letterSpacing=1.sp);Text(sub,fontSize=11.sp,color=Color.White.copy(.55f))}};Spacer(Modifier.height(10.dp))}
@Composable private fun SmallAction(t:String,m:Modifier=Modifier,go:()->Unit){OutlinedButton(go,m.height(44.dp),shape=RoundedCornerShape(14.dp)){Text(t,fontSize=11.sp)}}
@Composable private fun Dialog(kind:String,s:Settings,vm:GameViewModel,close:()->Unit){AlertDialog(close,title={Text(when(kind){"ai"->"Choose your opponent";"themes"->"Board collection";"settings"->"Game settings";"stats"->"Statistics";else->"The Grand Board"})},text={Column(Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState())){when(kind){"ai"->Difficulty.entries.forEach{Choice(it.name){vm.update(s.copy(difficulty=it));vm.start(Mode.AI)}};"themes"->BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};"settings"->{SwitchRow("Sound effects",s.sound){vm.update(s.copy(sound=it))};SwitchRow("Ambient music",s.music){vm.update(s.copy(music=it))};SwitchRow("Haptic feedback",s.vibration){vm.update(s.copy(vibration=it))};SwitchRow("Premium animations",s.animations){vm.update(s.copy(animations=it))};Text("BOARD THEME",fontSize=11.sp,color=Gold);BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};Text("CHECKERS",fontSize=11.sp,color=Gold);PieceStyle.entries.forEach{Choice(it.name,it==s.pieces){vm.update(s.copy(pieces=it))}};Text("DICE",fontSize=11.sp,color=Gold);DiceStyle.entries.forEach{Choice(it.name,it==s.dice){vm.update(s.copy(dice=it))}};Text("AI DIFFICULTY",fontSize=11.sp,color=Gold);Difficulty.entries.forEach{Choice(it.name,it==s.difficulty){vm.update(s.copy(difficulty=it))}}};"stats"->Text("Match statistics are recorded locally. Play your first match to begin your legacy.",color=Color.White.copy(.7f));else->Text("An original offline 3D Backgammon experience. Built for strategic play, tactile interaction and timeless elegance.\n\nVersion 2.0")}}},confirmButton={TextButton(close){Text("CLOSE")}})}
@Composable private fun Choice(t:String,on:Boolean=false,go:()->Unit){TextButton(go,Modifier.fillMaxWidth()){Text(if(on)"◆  $t" else t,Modifier.fillMaxWidth(),color=if(on)Gold else Color.White)}}
@Composable private fun SwitchRow(t:String,v:Boolean,set:(Boolean)->Unit)=Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(t,Modifier.weight(1f));Switch(v,set)}

@Composable private fun GameScreen(vm:GameViewModel){val game by vm.game.collectAsState();val settings by vm.settings.collectAsState();val rolling by vm.rolling.collectAsState();val match by vm.match.collectAsState();var selected by remember(game.position,game.dice){mutableStateOf<Int?>(null)};var pause by remember{mutableStateOf(false)};var gameSettings by remember{mutableStateOf(false)};val legal=remember(game){GameEngine.legalMoves(game.position,game.dice)};val context=LocalContext.current
 fun haptic(){if(settings.vibration){val v=context.getSystemService(android.os.Vibrator::class.java);if(Build.VERSION.SDK_INT>=26)v?.vibrate(VibrationEffect.createOneShot(22,70))else @Suppress("DEPRECATION")v?.vibrate(22)}}
 val palette=boardPalette(settings.theme)
 Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(palette.field.copy(.72f),palette.frameDark,Color(0xff060806))))){
  Box(Modifier.align(Alignment.Center).fillMaxWidth(.88f).fillMaxHeight(.80f).padding(top=8.dp)){
   FlatBoard(game,settings.theme,settings.pieces,selected,legal,{selected=it;haptic()},{vm.move(it);selected=null;haptic()},Modifier.fillMaxSize())
  }
  if(game.dice.isNotEmpty()||rolling) AnimatedDice(game.dice,settings.dice,rolling,Modifier.align(Alignment.Center))
  Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().systemBarsPadding().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){PlayerCard("YOU",match.whiteScore,game.position.turn==Player.WHITE,true);StatusPill(if(match.thinking)"OPPONENT THINKING" else if(rolling)"ROLLING DICE" else if(game.rolled)"${game.dice.size} MOVES REMAIN" else "${if(game.position.turn==Player.WHITE)"YOUR" else "OPPONENT"} TURN");PlayerCard(if(vm.mode==Mode.AI)settings.difficulty.name else "PLAYER TWO",match.blackScore,game.position.turn==Player.BLACK,false)}
  Column(Modifier.align(Alignment.CenterEnd).navigationBarsPadding().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){RoundAction("☰"){pause=true};Spacer(Modifier.height(8.dp));RoundAction("⚙"){gameSettings=true};Spacer(Modifier.height(8.dp));if(vm.mode==Mode.LOCAL){RoundAction("↶"){vm.undo()};Spacer(Modifier.height(8.dp))};RoundAction("${match.cube}×"){vm.offerDouble()}}
  Button({vm.roll();haptic()},enabled=!game.rolled&&!rolling&&game.winner==null&&!(vm.mode==Mode.AI&&game.position.turn==Player.BLACK),modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).width(190.dp).height(52.dp),shape=RoundedCornerShape(18.dp)){Text(if(rolling)"ROLLING…" else "ROLL DICE",fontWeight=FontWeight.Black,letterSpacing=1.sp)}
 }
 if(gameSettings)Dialog("settings",settings,vm){gameSettings=false}
 if(pause)AlertDialog({pause=false},title={Text("MATCH PAUSED")},text={Text("Score ${match.whiteScore} — ${match.blackScore}  •  First to ${match.target}")},confirmButton={Button({pause=false}){Text("RESUME")}},dismissButton={TextButton({vm.menu()}){Text("MAIN MENU")}})
 game.winner?.let{w->val loser=w.other();val result=when{game.position.off(loser)>0->"Single game";game.position.bar(loser)>0->"Backgammon";else->"Gammon"};AlertDialog({},title={Text(if(w==Player.WHITE)"VICTORY" else "MATCH COMPLETE")},text={Text("$result • ${match.cube}× cube\nScore  ${match.whiteScore} — ${match.blackScore}",textAlign=TextAlign.Center)},confirmButton={Button({vm.restart()}){Text("REMATCH")}},dismissButton={TextButton({vm.menu()}){Text("MENU")}})}
}
@Composable private fun PlayerCard(name:String,score:Int,active:Boolean,light:Boolean){Row(Modifier.clip(RoundedCornerShape(18.dp)).background(if(active)Glass else Glass.copy(.58f)).border(if(active)1.dp else 0.dp,if(active)Gold else Color.Transparent,RoundedCornerShape(18.dp)).padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(light)Color(0xffffe7bd)else Color(0xff1d2028)));Spacer(Modifier.width(9.dp));Column{Text(name,fontSize=11.sp,fontWeight=FontWeight.Bold);Text("SCORE  $score",fontSize=10.sp,color=Color.White.copy(.6f))}}}
@Composable private fun StatusPill(t:String){Text(t,Modifier.clip(RoundedCornerShape(50)).background(Glass).border(1.dp,Color.White.copy(.12f),RoundedCornerShape(50)).padding(horizontal=20.dp,vertical=9.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
@Composable private fun AnimatedDice(finalDice:List<Int>,style:DiceStyle,rolling:Boolean,modifier:Modifier=Modifier){
 val x1=remember{Animatable(-90f)};val y1=remember{Animatable(-45f)};val r1=remember{Animatable(0f)}
 val x2=remember{Animatable(90f)};val y2=remember{Animatable(-35f)};val r2=remember{Animatable(0f)}
 var face1 by remember{mutableIntStateOf(1)};var face2 by remember{mutableIntStateOf(6)}
 LaunchedEffect(rolling){
  if(rolling){
   val tx1=(-72..-20).random().toFloat();val tx2=(20..72).random().toFloat();val ty1=(-24..35).random().toFloat();val ty2=(-24..35).random().toFloat()
   kotlinx.coroutines.coroutineScope{
    launch{while(rolling){face1=(1..6).random();face2=(1..6).random();kotlinx.coroutines.delay(65)}}
    launch{x1.animateTo(tx1,tween(820,easing=FastOutSlowInEasing))};launch{x2.animateTo(tx2,tween(850,easing=FastOutSlowInEasing))}
    launch{y1.animateTo(ty1,keyframes{durationMillis=850;-70f at 0;45f at 360;-12f at 610;ty1 at 850})}
    launch{y2.animateTo(ty2,keyframes{durationMillis=850;-90f at 0;38f at 400;-8f at 650;ty2 at 850})}
    launch{r1.animateTo(r1.value+1080f,tween(850,easing=LinearOutSlowInEasing))};launch{r2.animateTo(r2.value-1260f,tween(850,easing=LinearOutSlowInEasing))}
   }
  } else {face1=finalDice.getOrElse(0){face1};face2=finalDice.getOrElse(1){face2}}
 }
 Box(modifier.size(210.dp,120.dp)){DieFace(if(rolling)face1 else finalDice.getOrElse(0){face1},style,rolling,Modifier.align(Alignment.Center).offset{x1.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y1.value.roundToInt())}}.rotate(r1.value));DieFace(if(rolling)face2 else finalDice.getOrElse(1){face2},style,rolling,Modifier.align(Alignment.Center).offset{x2.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y2.value.roundToInt())}}.rotate(r2.value))}
}
@Composable private fun DieFace(value:Int,style:DiceStyle,rolling:Boolean,modifier:Modifier=Modifier){val glyph=listOf("⚀","⚁","⚂","⚃","⚄","⚅")[value-1];val bg=when(style){DiceStyle.CLASSIC->Color(0xfffff5d9);DiceStyle.ONYX->Color(0xff171a20);DiceStyle.CRYSTAL->Color(0xff7bdcff)};Text(glyph,modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp,Color.White.copy(.5f),RoundedCornerShape(10.dp)).padding(bottom=4.dp),color=if(style==DiceStyle.CLASSIC)Color(0xff241b16)else Color.White,fontSize=39.sp,textAlign=TextAlign.Center)
}
@Composable private fun RoundAction(t:String,go:()->Unit){FilledTonalButton(go,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(46.dp),shape=androidx.compose.foundation.shape.CircleShape){Text(t,fontWeight=FontWeight.Bold)}}
