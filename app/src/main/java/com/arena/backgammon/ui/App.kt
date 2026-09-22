package com.arena.backgammon.ui

import android.os.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.*
import androidx.compose.ui.input.pointer.pointerInput
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
  Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xff31483b),Color(0xff101d17))))){
   AnimatedContent(screen,transitionSpec={
    if(targetState.ordinal>initialState.ordinal)(slideInHorizontally(tween(480)){it}+fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(480)){-it/3}+fadeOut(tween(240)))
    else (slideInHorizontally(tween(480)){-it/2}+fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(480)){it}+fadeOut(tween(240)))
   },label="navigation"){destination->when(destination){Screen.MENU->MainMenu(vm);Screen.SETUP->SetupMenu(vm);Screen.GAME->GameScreen(vm)}}
  }
 }
}
@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit)=Column(modifier.clip(RoundedCornerShape(24.dp)).background(Glass).border(1.dp,Color.White.copy(.14f),RoundedCornerShape(24.dp)).padding(18.dp),content=content)
@Composable private fun MainMenu(vm:GameViewModel){
 val s by vm.settings.collectAsState();var dialog by remember{mutableStateOf("")};val p=boardPalette(s.theme)
 Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(p.field.copy(.9f),p.frameDark,Color(0xff14271e))))){
  Text("BACKGAMMON",Modifier.align(Alignment.TopCenter).padding(top=12.dp),fontSize=42.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp,color=Color(0xffffdf8b),style=androidx.compose.ui.text.TextStyle(shadow=androidx.compose.ui.graphics.Shadow(Color.Black,Offset(3f,4f),6f)))
  Box(Modifier.align(Alignment.Center).fillMaxWidth(.56f).fillMaxHeight(.60f)){FlatBoard(TurnState(),s.theme,s.pieces,null,emptyList(),{}, {},Modifier.fillMaxSize())}
  Button({vm.start(Mode.AI)},Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=18.dp).width(230.dp).height(62.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xffffd77b),contentColor=Color(0xff351c16)),border=BorderStroke(2.dp,Color(0xff6d321f))){Text("PLAY",fontSize=26.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp)}
  Row(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),verticalAlignment=Alignment.CenterVertically){HomeIcon(if(s.sound)"🔊" else "🔇"){vm.update(s.copy(sound=!s.sound))};Spacer(Modifier.width(12.dp));HomeIcon("⚙"){dialog="settings"}}
 }
 if(dialog.isNotEmpty())Dialog(dialog,s,vm){dialog=""}
}
@Composable private fun HomeIcon(icon:String,go:()->Unit){FilledTonalButton(go,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(54.dp),shape=androidx.compose.foundation.shape.CircleShape,colors=ButtonDefaults.filledTonalButtonColors(containerColor=Color(0xdd2b332e),contentColor=Color.White)){Text(icon,fontSize=23.sp)}}
@Composable private fun SetupMenu(vm:GameViewModel){
 val s by vm.settings.collectAsState();var dialog by remember{mutableStateOf("")};val p=boardPalette(s.theme)
 Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(p.field,p.frameDark,Color(0xff13231c)))).pointerInput(Unit){var drag=0f;detectHorizontalDragGestures(onDragEnd={if(drag>120f)vm.menu();drag=0f},onHorizontalDrag={_,amount->drag+=amount})}){
  Text("NEW GAME",Modifier.align(Alignment.TopCenter).padding(22.dp),fontSize=34.sp,fontWeight=FontWeight.Black,color=Color(0xffffdc83))
  Panel(Modifier.align(Alignment.Center).widthIn(420.dp,620.dp)){MenuAction("PLAY VS AI","Choose from five difficulty levels"){dialog="ai"};MenuAction("LOCAL TWO PLAYER","Play together on this device"){vm.start(Mode.LOCAL)};Row{SmallAction("THEMES",Modifier.weight(1f)){dialog="themes"};Spacer(Modifier.width(10.dp));SmallAction("OPTIONS",Modifier.weight(1f)){dialog="settings"}};Spacer(Modifier.height(8.dp));TextButton({vm.menu()},Modifier.fillMaxWidth()){Text("‹  BACK",color=Color.White,fontWeight=FontWeight.Bold)}}
 }
 if(dialog.isNotEmpty())Dialog(dialog,s,vm){dialog=""}
}
@Composable private fun MenuAction(title:String,sub:String,go:()->Unit){Button(go,Modifier.fillMaxWidth().height(66.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xff35251f),contentColor=Color(0xffffe3a0)),border=BorderStroke(1.dp,Color(0xffffd77b).copy(.45f))){Column(Modifier.fillMaxWidth()){Text(title,fontWeight=FontWeight.Bold,letterSpacing=1.sp);Text(sub,fontSize=11.sp,color=Color.White.copy(.55f))}};Spacer(Modifier.height(10.dp))}
@Composable private fun SmallAction(t:String,m:Modifier=Modifier,go:()->Unit){OutlinedButton(go,m.height(44.dp),shape=RoundedCornerShape(14.dp)){Text(t,fontSize=11.sp)}}
@Composable private fun Dialog(kind:String,s:Settings,vm:GameViewModel,close:()->Unit){AlertDialog(close,title={Text(when(kind){"ai"->"Choose your opponent";"themes"->"Board collection";"settings"->"Game settings";"stats"->"Statistics";else->"The Grand Board"})},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){when(kind){"ai"->Difficulty.entries.forEach{Choice(it.name){vm.update(s.copy(difficulty=it));vm.start(Mode.AI)}};"themes"->BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};"settings"->{SwitchRow("Sound effects",s.sound){vm.update(s.copy(sound=it))};SwitchRow("Ambient music",s.music){vm.update(s.copy(music=it))};SwitchRow("Haptic feedback",s.vibration){vm.update(s.copy(vibration=it))};SwitchRow("Premium animations",s.animations){vm.update(s.copy(animations=it))};Text("BOARD THEME",fontSize=11.sp,color=Gold);BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};Text("CHECKERS",fontSize=11.sp,color=Gold);PieceStyle.entries.forEach{Choice(it.name,it==s.pieces){vm.update(s.copy(pieces=it))}};Text("DICE",fontSize=11.sp,color=Gold);DiceStyle.entries.forEach{Choice(it.name,it==s.dice){vm.update(s.copy(dice=it))}};Text("AI DIFFICULTY",fontSize=11.sp,color=Gold);Difficulty.entries.forEach{Choice(it.name,it==s.difficulty){vm.update(s.copy(difficulty=it))}}};"stats"->Text("Match statistics are recorded locally. Play your first match to begin your legacy.",color=Color.White.copy(.7f));else->Text("An original offline 3D Backgammon experience. Built for strategic play, tactile interaction and timeless elegance.\n\nVersion 2.0")}}},confirmButton={TextButton(close){Text("CLOSE")}})}
@Composable private fun Choice(t:String,on:Boolean=false,go:()->Unit){TextButton(go,Modifier.fillMaxWidth()){Text(if(on)"◆  $t" else t,Modifier.fillMaxWidth(),color=if(on)Gold else Color.White)}}
@Composable private fun SwitchRow(t:String,v:Boolean,set:(Boolean)->Unit)=Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(t,Modifier.weight(1f));Switch(v,set)}

@Composable private fun GameScreen(vm:GameViewModel){val game by vm.game.collectAsState();val settings by vm.settings.collectAsState();val rolling by vm.rolling.collectAsState();val match by vm.match.collectAsState();var selected by remember(game.position,game.dice){mutableStateOf<Int?>(null)};var panel by remember{mutableStateOf("")};val legal=remember(game){GameEngine.legalMoves(game.position,game.dice)};val context=LocalContext.current
 fun haptic(){if(settings.vibration){val v=context.getSystemService(android.os.Vibrator::class.java);if(Build.VERSION.SDK_INT>=26)v?.vibrate(VibrationEffect.createOneShot(22,70))else @Suppress("DEPRECATION")v?.vibrate(22)}}
 val palette=boardPalette(settings.theme)
 Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(palette.field.copy(.72f),palette.frameDark,Color(0xff172820))))){
  Box(Modifier.align(Alignment.Center).fillMaxWidth(.78f).fillMaxHeight(.72f).graphicsLayer{shadowElevation=24.dp.toPx();shape=RoundedCornerShape(18.dp);clip=false}.padding(top=8.dp)){
   FlatBoard(game,settings.theme,settings.pieces,selected,legal,{selected=it;haptic()},{vm.move(it);selected=null;haptic()},Modifier.fillMaxSize())
  }
  if(game.dice.isNotEmpty()||rolling||(!game.rolled&&!(vm.mode==Mode.AI&&game.position.turn==Player.BLACK))) AnimatedDice(game.dice,settings.dice,rolling,{vm.roll();haptic()},Modifier.align(Alignment.Center))
  Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().systemBarsPadding().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){PlayerCard("YOU",match.whiteScore,game.position.turn==Player.WHITE,true);StatusPill(if(match.thinking)"OPPONENT THINKING" else if(rolling)"ROLLING DICE" else if(game.rolled)"${game.dice.size} MOVES REMAIN" else "${if(game.position.turn==Player.WHITE)"YOUR" else "OPPONENT"} TURN");PlayerCard(if(vm.mode==Mode.AI)settings.difficulty.name else "PLAYER TWO",match.blackScore,game.position.turn==Player.BLACK,false)}
  Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(34.dp).pointerInput(Unit){var swipe=0f;detectHorizontalDragGestures(onDragEnd={if(swipe>65f)panel="menu";swipe=0f},onHorizontalDrag={_,amount->swipe+=amount})})
  Column(Modifier.align(Alignment.CenterEnd).navigationBarsPadding().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){RoundAction("☰"){panel="menu"};Spacer(Modifier.height(8.dp));RoundAction("⚙"){panel="settings"};Spacer(Modifier.height(8.dp));RoundAction("↶"){vm.undo()};Spacer(Modifier.height(8.dp));RoundAction("${match.cube}×"){vm.offerDouble()}}
 }
 AnimatedVisibility(panel.isNotEmpty(),enter=slideInHorizontally(tween(420)){-it}+fadeIn(),exit=slideOutHorizontally(tween(340)){-it}+fadeOut()){GameDrawer(panel,settings,match,vm,{panel=""},{panel=it})}
 game.winner?.let{w->val loser=w.other();val result=when{game.position.off(loser)>0->"Single game";game.position.bar(loser)>0->"Backgammon";else->"Gammon"};AlertDialog({},title={Text(if(w==Player.WHITE)"VICTORY" else "MATCH COMPLETE")},text={Text("$result • ${match.cube}× cube\nScore  ${match.whiteScore} — ${match.blackScore}",textAlign=TextAlign.Center)},confirmButton={Button({vm.restart()}){Text("REMATCH")}},dismissButton={TextButton({vm.menu()}){Text("MENU")}})}
}
@Composable private fun GameDrawer(kind:String,s:Settings,match:MatchInfo,vm:GameViewModel,close:()->Unit,switch:(String)->Unit){
 Box(Modifier.fillMaxSize().background(Color.Black.copy(.46f)).clickable(onClick=close)){
  Row(Modifier.fillMaxHeight().widthIn(360.dp,500.dp).clip(RoundedCornerShape(topEnd=30.dp,bottomEnd=30.dp)).background(Brush.verticalGradient(listOf(Color(0xee29352f),Color(0xf21a211e)))).border(1.dp,Color.White.copy(.18f),RoundedCornerShape(topEnd=30.dp,bottomEnd=30.dp)).clickable(enabled=false){}){
   Column(Modifier.width(92.dp).fillMaxHeight().background(Color.Black.copy(.20f)).padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("BG",color=Gold,fontWeight=FontWeight.Black,fontSize=22.sp);Spacer(Modifier.height(28.dp));DrawerTab("☰",kind=="menu"){switch("menu")};Spacer(Modifier.height(12.dp));DrawerTab("⚙",kind=="settings"){switch("settings")};Spacer(Modifier.weight(1f));DrawerTab("‹",false,close)}
   Column(Modifier.weight(1f).fillMaxHeight().padding(22.dp).verticalScroll(rememberScrollState())){
    Text(if(kind=="settings")"GAME SETTINGS" else "MATCH MENU",color=Color.White,fontWeight=FontWeight.Black,fontSize=22.sp);Text("Score ${match.whiteScore} — ${match.blackScore}",color=Gold,fontSize=12.sp);Spacer(Modifier.height(20.dp))
    if(kind=="menu"){DrawerButton("RESUME",close);DrawerButton("UNDO LAST MOVE"){vm.undo();close()};DrawerButton("NEW GAME"){vm.restart();close()};DrawerButton("MAIN MENU"){vm.menu()}}
    else {SwitchRow("Sound effects",s.sound){vm.update(s.copy(sound=it))};SwitchRow("Haptic feedback",s.vibration){vm.update(s.copy(vibration=it))};SwitchRow("Smooth animations",s.animations){vm.update(s.copy(animations=it))};DrawerHeader("BOARD");BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};DrawerHeader("CHECKERS");PieceStyle.entries.forEach{Choice(it.name,it==s.pieces){vm.update(s.copy(pieces=it))}};DrawerHeader("DICE");DiceStyle.entries.forEach{Choice(it.name,it==s.dice){vm.update(s.copy(dice=it))}};DrawerHeader("DIFFICULTY");Difficulty.entries.forEach{Choice(it.name,it==s.difficulty){vm.update(s.copy(difficulty=it))}}}
   }
  }
 }
}
@Composable private fun DrawerTab(icon:String,on:Boolean,go:()->Unit){FilledTonalButton(go,Modifier.size(58.dp),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=if(on)Gold.copy(.24f)else Color.White.copy(.07f))){Text(icon,color=if(on)Gold else Color.White,fontSize=22.sp)}}
@Composable private fun DrawerButton(text:String,go:()->Unit){Button(go,Modifier.fillMaxWidth().padding(vertical=5.dp).height(50.dp),colors=ButtonDefaults.buttonColors(Color.White.copy(.09f)),border=BorderStroke(1.dp,Color.White.copy(.14f))){Text(text,Modifier.fillMaxWidth(),color=Color.White,fontWeight=FontWeight.Bold)}}
@Composable private fun DrawerHeader(text:String){Text(text,Modifier.padding(top=18.dp,bottom=5.dp),color=Gold,fontSize=11.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)}
@Composable private fun PlayerCard(name:String,score:Int,active:Boolean,light:Boolean){Row(Modifier.clip(RoundedCornerShape(18.dp)).background(if(active)Glass else Glass.copy(.58f)).border(if(active)1.dp else 0.dp,if(active)Gold else Color.Transparent,RoundedCornerShape(18.dp)).padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(light)Color(0xffffe7bd)else Color(0xff1d2028)));Spacer(Modifier.width(9.dp));Column{Text(name,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("SCORE  $score",fontSize=10.sp,color=Color.White.copy(.6f))}}}
@Composable private fun StatusPill(t:String){Text(t,Modifier.clip(RoundedCornerShape(50)).background(Glass).border(1.dp,Color.White.copy(.12f),RoundedCornerShape(50)).padding(horizontal=20.dp,vertical=9.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Color.White)}
@Composable private fun AnimatedDice(finalDice:List<Int>,style:DiceStyle,rolling:Boolean,onThrow:()->Unit,modifier:Modifier=Modifier){
 var drag by remember{mutableStateOf(Offset.Zero)};var throwVector by remember{mutableStateOf(Offset.Zero)}
 val x1=remember{Animatable(-90f)};val y1=remember{Animatable(-45f)};val r1=remember{Animatable(0f)}
 val x2=remember{Animatable(90f)};val y2=remember{Animatable(-35f)};val r2=remember{Animatable(0f)}
 var face1 by remember{mutableIntStateOf(1)};var face2 by remember{mutableIntStateOf(6)}
 LaunchedEffect(rolling){
  if(rolling){
   val released=throwVector;x1.snapTo(x1.value+released.x);x2.snapTo(x2.value+released.x);y1.snapTo(y1.value+released.y);y2.snapTo(y2.value+released.y);drag=Offset.Zero
   val tx1=(x1.value+(-14..14).random()).coerceIn(-150f,-34f);val tx2=(x2.value+(-14..14).random()).coerceIn(34f,150f);val ty1=(y1.value+(-10..10).random()).coerceIn(-64f,58f);val ty2=(y2.value+(-10..10).random()).coerceIn(-58f,64f)
   kotlinx.coroutines.coroutineScope{
    launch{while(rolling){face1=(1..6).random();face2=(1..6).random();kotlinx.coroutines.delay(110)}}
    launch{x1.animateTo(tx1,tween(1250,easing=FastOutSlowInEasing))};launch{x2.animateTo(tx2,tween(1280,easing=FastOutSlowInEasing))}
    launch{y1.animateTo(ty1,keyframes{durationMillis=1280;y1.value at 0;(ty1-42f) at 430;(ty1+10f) at 890;ty1 at 1280})}
    launch{y2.animateTo(ty2,keyframes{durationMillis=1280;y2.value at 0;(ty2-38f) at 470;(ty2+8f) at 920;ty2 at 1280})}
    launch{r1.animateTo(r1.value+540f,tween(1280,easing=LinearOutSlowInEasing))};launch{r2.animateTo(r2.value-630f,tween(1280,easing=LinearOutSlowInEasing))}
   }
  } else {face1=finalDice.getOrElse(0){face1};face2=finalDice.getOrElse(1){face2}}
 }
 Box(modifier.size(230.dp,140.dp).graphicsLayer{translationX=drag.x;translationY=drag.y}.pointerInput(rolling,finalDice){if(!rolling&&finalDice.isEmpty())detectDragGestures(onDragEnd={if(drag.getDistance()>24f){throwVector=drag;onThrow()}else drag=Offset.Zero},onDragCancel={drag=Offset.Zero}){change,amount->change.consume();drag+=amount}}){DieFace(if(rolling)face1 else finalDice.getOrElse(0){face1},style,rolling,Modifier.align(Alignment.Center).offset{x1.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y1.value.roundToInt())}}.rotate(r1.value));DieFace(if(rolling)face2 else finalDice.getOrElse(1){face2},style,rolling,Modifier.align(Alignment.Center).offset{x2.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y2.value.roundToInt())}}.rotate(r2.value))}
}
@Composable private fun DieFace(value:Int,style:DiceStyle,rolling:Boolean,modifier:Modifier=Modifier){
 val base=when(style){DiceStyle.CLASSIC->Color(0xffffedc5);DiceStyle.ONYX->Color(0xff252731);DiceStyle.CRYSTAL->Color(0xff65cce8)};val ink=if(style==DiceStyle.CLASSIC)Color(0xff49382c)else Color.White
 Canvas(modifier.size(62.dp)){
  val u=size.minDimension/62f
  drawRoundRect(Color.Black.copy(.35f),Offset(9*u,13*u),Size(48*u,47*u),CornerRadius(8*u))
  val top=Path().apply{moveTo(7*u,12*u);lineTo(17*u,3*u);lineTo(58*u,3*u);lineTo(50*u,12*u);close()};drawPath(top,base.copy(.9f))
  val side=Path().apply{moveTo(50*u,12*u);lineTo(58*u,3*u);lineTo(58*u,45*u);lineTo(50*u,55*u);close()};drawPath(side,base.copy(.58f))
  drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(.48f),base,base.copy(.72f))),Offset(7*u,12*u),Size(43*u,43*u),CornerRadius(7*u));drawRoundRect(Color.White.copy(.5f),Offset(7*u,12*u),Size(43*u,43*u),CornerRadius(7*u),style=Stroke(1.5f*u))
  val spots=when(value){1->listOf(.5f to .5f);2->listOf(.28f to .28f,.72f to .72f);3->listOf(.27f to .27f,.5f to .5f,.73f to .73f);4->listOf(.28f to .28f,.72f to .28f,.28f to .72f,.72f to .72f);5->listOf(.27f to .27f,.73f to .27f,.5f to .5f,.27f to .73f,.73f to .73f);else->listOf(.28f to .23f,.72f to .23f,.28f to .5f,.72f to .5f,.28f to .77f,.72f to .77f)}
  spots.forEach{drawCircle(Color.Black.copy(.22f),3.8f*u,Offset((7+43*it.first)*u,(12+43*it.second)*u));drawCircle(ink,3.1f*u,Offset((7+43*it.first)*u,(12+43*it.second)*u));drawCircle(Color.White.copy(.18f),.9f*u,Offset((6.2f+43*it.first)*u,(11.2f+43*it.second)*u))};listOf(15f to 20f,42f to 18f,25f to 48f,45f to 40f).forEach{drawCircle(Color(0xff8e7253).copy(.10f),.7f*u,Offset(it.first*u,it.second*u))}
 }
}
@Composable private fun RoundAction(t:String,go:()->Unit){FilledTonalButton(go,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(46.dp),shape=androidx.compose.foundation.shape.CircleShape){Text(t,fontWeight=FontWeight.Bold)}}
