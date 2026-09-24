package com.arena.backgammon.ui

import android.os.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import com.arena.backgammon.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.backgammon.ai.Difficulty
import com.arena.backgammon.ai.AiPersona
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
  Box(Modifier.align(Alignment.Center).fillMaxWidth(.56f).fillMaxHeight(.60f)){FlatBoard(TurnState(),s.theme,s.pieces,null,emptyList(),{}, {},{_,_->false},Modifier.fillMaxSize())}
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
@Composable private fun Dialog(kind:String,s:Settings,vm:GameViewModel,close:()->Unit){AlertDialog(close,title={Text(when(kind){"ai"->"Choose your opponent";"themes"->"Board collection";"settings"->"Game settings";"stats"->"Statistics";else->"The Grand Board"})},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){when(kind){"ai"->{Text("PLAYING STYLE",fontSize=11.sp,color=Gold);AiPersona.entries.forEach{persona->Choice("${persona.title} — ${persona.description}",persona==s.aiPersona){vm.update(s.copy(aiPersona=persona))}};Text("DIFFICULTY — SELECT TO START",fontSize=11.sp,color=Gold);Difficulty.entries.forEach{Choice(it.name,it==s.difficulty){vm.update(s.copy(difficulty=it));vm.start(Mode.AI)}}};"themes"->BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};"settings"->{SwitchRow("Sound effects",s.sound){vm.update(s.copy(sound=it))};SwitchRow("Haptic feedback",s.vibration){vm.update(s.copy(vibration=it))};SwitchRow("Premium animations",s.animations){vm.update(s.copy(animations=it))};Text("MATCH CLOCK",fontSize=11.sp,color=Gold);ClockPreset.entries.forEach{Choice(when(it){ClockPreset.OFF->"Off";ClockPreset.RAPID_60->"Rapid — 1:00 bank / 10 sec turn";ClockPreset.STANDARD_180->"Standard — 3:00 bank / 20 sec turn"},it==s.clockPreset){vm.update(s.copy(clockPreset=it))}};Text("MOVE GUIDANCE",fontSize=11.sp,color=Gold);GuidanceMode.entries.forEach{Choice(if(it==GuidanceMode.SIMPLE)"Simple — only the next move" else "Coach — complete compound routes",it==s.guidanceMode){vm.update(s.copy(guidanceMode=it))}};Text("BOARD THEME",fontSize=11.sp,color=Gold);BoardTheme.entries.forEach{Choice(it.name.replace('_',' '),it==s.theme){vm.update(s.copy(theme=it))}};Text("AI DIFFICULTY",fontSize=11.sp,color=Gold);Difficulty.entries.forEach{Choice(it.name,it==s.difficulty){vm.update(s.copy(difficulty=it))}}};"stats"->Text("Match statistics are recorded locally. Play your first match to begin your legacy.",color=Color.White.copy(.7f));else->Text("An original offline 3D Backgammon experience. Built for strategic play, tactile interaction and timeless elegance.\n\nVersion 2.0")}}},confirmButton={TextButton(close){Text("CLOSE")}})}
@Composable private fun Choice(t:String,on:Boolean=false,go:()->Unit){TextButton(go,Modifier.fillMaxWidth()){Text(if(on)"◆  $t" else t,Modifier.fillMaxWidth(),color=if(on)Gold else Color.White)}}
@Composable private fun SwitchRow(t:String,v:Boolean,set:(Boolean)->Unit)=Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(t,Modifier.weight(1f));Switch(v,set)}

@Composable private fun GameScreen(vm:GameViewModel){val game by vm.game.collectAsState();val settings by vm.settings.collectAsState();val rolling by vm.rolling.collectAsState();val match by vm.match.collectAsState();val autoAssist by vm.autoAssist.collectAsState();val clock by vm.clock.collectAsState();var selected by remember(game.position,game.dice){mutableStateOf<Int?>(null)};var panel by remember{mutableStateOf("")};val legal=remember(game,match.thinking){if(match.thinking||(vm.mode==Mode.AI&&game.position.turn==Player.BLACK))emptyList() else GameEngine.legalMoves(game.position,game.dice)};val context=LocalContext.current
 fun haptic(){if(settings.vibration){val v=context.getSystemService(android.os.Vibrator::class.java);if(Build.VERSION.SDK_INT>=26)v?.vibrate(VibrationEffect.createOneShot(22,70))else @Suppress("DEPRECATION")v?.vibrate(22)}}
 val palette=boardPalette(settings.theme);val background=when(settings.tableScene){TableScene.STARRY_SKY->R.drawable.table_starry_sky;TableScene.AUTUMN_SUNSET->R.drawable.table_autumn_sunset;TableScene.PERSIAN_RUG->R.drawable.table_persian_rug;TableScene.DARK_RIVER->R.drawable.table_dark_river}
 fun cycleScene(step:Int){val all=TableScene.entries;val next=all[(all.indexOf(settings.tableScene)+step+all.size)%all.size];vm.update(settings.copy(tableScene=next))}
 BoxWithConstraints(Modifier.fillMaxSize()){
  val layout=boardLayoutProfile(maxWidth.value,maxHeight.value)
  Image(painterResource(background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(.06f)))
  Box(Modifier.align(Alignment.Center).fillMaxWidth(layout.widthFraction).fillMaxHeight(layout.heightFraction).absoluteOffset(y=13.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xff160806)).border(2.dp,Color.Black.copy(.75f),RoundedCornerShape(15.dp)))
  Box(Modifier.align(Alignment.Center).fillMaxWidth(layout.widthFraction).fillMaxHeight(layout.heightFraction).absoluteOffset(y=7.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xff5b2a18),Color(0xff230d07)))).border(1.dp,Color(0xffb57549).copy(.55f),RoundedCornerShape(14.dp)))
  Box(Modifier.align(Alignment.Center).fillMaxWidth(layout.widthFraction).fillMaxHeight(layout.heightFraction).graphicsLayer{shadowElevation=34.dp.toPx();shape=RoundedCornerShape(13.dp);clip=false}.clip(RoundedCornerShape(13.dp)).background(Brush.verticalGradient(listOf(Color(0xffa56d45),Color(0xff542a18)))).border(1.dp,Color(0xffe3a875).copy(.55f),RoundedCornerShape(13.dp))){
   FlatBoard(game,settings.theme,settings.pieces,selected,legal,{selected=it;haptic()},{vm.move(it);selected=null;haptic()},{from,to->vm.moveCombined(from,to).also{if(it){selected=null;haptic()}}},Modifier.fillMaxSize().padding(top=30.dp,start=3.dp,end=3.dp,bottom=3.dp),scene=settings.tableScene,guidance=settings.guidanceMode)
   TopGameBar(settings.difficulty,settings.aiPersona,match,if(match.thinking)"AI THINKING" else if(rolling)"ROLLING" else if(game.position.turn==Player.WHITE)"YOUR TURN" else "AI TURN",{panel="menu"},{panel="settings"},Modifier.align(Alignment.TopCenter))
   BarEngraving("UNDO",Modifier.align(Alignment.BottomCenter).absoluteOffset(x=(-12).dp).padding(bottom=24.dp)){vm.undo()}
   if(!game.rolled&&!rolling&&!match.thinking&&game.position.turn==Player.WHITE&&match.pendingDoubleBy==null&&(match.cubeOwner==null||match.cubeOwner==Player.WHITE))InlayButton("DOUBLE  ×${match.cube*2}",Modifier.align(Alignment.Center).absoluteOffset(x=layout.actionOffsetDp.dp)){vm.offerDouble()}
   if((game.rolled&&!rolling&&game.position.turn==Player.WHITE)||autoAssist)InlayButton(if(autoAssist)"AUTO ON" else "AUTO OFF",Modifier.align(Alignment.Center).absoluteOffset(x=layout.actionOffsetDp.dp)){vm.toggleAutoAssist()}
  }
  if(clock.active)MatchClocks(clock,game.position.turn,Modifier.align(Alignment.TopCenter).fillMaxWidth(.58f).padding(top=52.dp))
  CarvedScoreCounter(match.whiteScore,match.blackScore,Modifier.align(Alignment.TopCenter).padding(top=2.dp))
  if(game.dice.isNotEmpty()||rolling||(!game.rolled&&!(vm.mode==Mode.AI&&game.position.turn==Player.BLACK))) AnimatedDice(game.dice,settings.dice,rolling,{vm.roll();haptic()},Modifier.align(Alignment.Center).absoluteOffset(x=layout.diceOffsetDp.dp))
  SceneArrow("‹",Modifier.align(Alignment.CenterStart).padding(start=8.dp)){cycleScene(-1)}
  SceneArrow("›",Modifier.align(Alignment.CenterEnd).padding(end=8.dp)){cycleScene(1)}
 }
 AnimatedVisibility(panel.isNotEmpty(),enter=slideInHorizontally(tween(420)){it}+fadeIn(),exit=slideOutHorizontally(tween(340)){it}+fadeOut()){GameDrawer(panel,settings,match,vm,{panel=""},{panel=it})}
 match.pendingDoubleBy?.takeIf{it==Player.BLACK}?.let{AlertDialog(onDismissRequest={},title={Text("DOUBLE TO ×${match.cube*2}")},text={Text("Your opponent offers to raise the game value. If you take, you own the cube and only you may offer the next redouble. If you drop, you lose ${match.cube} point${if(match.cube==1)"" else "s"}.")},confirmButton={Button({vm.acceptDouble()}){Text("TAKE")}},dismissButton={OutlinedButton({vm.declineDouble()}){Text("DROP")}})}
 game.winner?.let{w->val loser=w.other();val won=w==Player.WHITE;val result=if(match.timedOutBy!=null)"Clock expired — timed game" else if(match.droppedBy!=null)"Double declined — ${match.cube} point game" else when{game.position.off(loser)>0->"Single game";game.position.bar(loser)>0->"Backgammon";else->"Gammon"};AlertDialog({},icon={Text(if(won)"✦" else "◆",fontSize=42.sp,color=if(won)Color(0xffffd36a)else Color(0xffb87a69))},title={Text(if(won)"VICTORY" else "DEFEAT",color=Color(0xffffdda0),fontWeight=FontWeight.Black,letterSpacing=2.sp)},text={Column(horizontalAlignment=Alignment.CenterHorizontally){Text(if(won)"A masterful finish." else "The board belongs to your opponent this round.",color=Color.White.copy(.78f),textAlign=TextAlign.Center);Spacer(Modifier.height(10.dp));Text("$result\nScore  ${match.whiteScore} — ${match.blackScore}",color=Color(0xffffd59a),textAlign=TextAlign.Center,fontWeight=FontWeight.Bold)}},confirmButton={Button({vm.restart()},colors=ButtonDefaults.buttonColors(Color(0xff8a522f))){Text("REMATCH")}},dismissButton={TextButton({vm.menu()}){Text("EXIT",color=Color(0xffffd59a))}},containerColor=Color(0xff32170f),tonalElevation=18.dp,shape=RoundedCornerShape(16.dp))}
}
@Composable private fun GameDrawer(kind:String,s:Settings,match:MatchInfo,vm:GameViewModel,close:()->Unit,switch:(String)->Unit){
 Box(Modifier.fillMaxSize().background(Color.Black.copy(.24f)).clickable(onClick=close)){
  AnimatedContent(kind,modifier=Modifier.align(Alignment.CenterEnd).widthIn(min=275.dp,max=345.dp).padding(end=22.dp),transitionSpec={slideInHorizontally(tween(330)){it}+fadeIn() togetherWith slideOutHorizontally(tween(250)){it}+fadeOut()},label="woodMenu"){page->
   Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(11.dp)){
    val nested=page in listOf("themes","difficulty","persona","sound","dice","clock","guidance")
    WoodMenuPlank(if(nested)"‹  SETTINGS" else when(page){"menu"->"MATCH MENU";else->"GAME SETTINGS"},"Score ${match.whiteScore} — ${match.blackScore}",large=true){if(nested)switch("settings")else close()}
    when(page){
     "menu"->{WoodMenuPlank("RESUME","Return to the board",go=close);WoodMenuPlank("UNDO LAST MOVE","Restore the previous position"){vm.undo();close()};WoodMenuPlank("MAIN MENU","Leave this match"){vm.menu()}}
     "themes"->BoardTheme.entries.forEach{t->WoodMenuPlank(t.name.replace('_',' '),if(t==s.theme)"◆  SELECTED" else "Board material"){vm.update(s.copy(theme=t))}}
     "difficulty"->Difficulty.entries.forEach{d->WoodMenuPlank(d.name,if(d==s.difficulty)"◆  SELECTED" else "AI strength"){vm.update(s.copy(difficulty=d))}}
     "persona"->AiPersona.entries.forEach{persona->WoodMenuPlank(persona.title,if(persona==s.aiPersona)"◆  SELECTED — ${persona.description}" else persona.description){vm.update(s.copy(aiPersona=persona))}}
     "sound"->{WoodTogglePlank("SOUND EFFECTS",s.sound){vm.update(s.copy(sound=it))};WoodTogglePlank("HAPTIC FEEDBACK",s.vibration){vm.update(s.copy(vibration=it))};WoodTogglePlank("SMOOTH ANIMATIONS",s.animations){vm.update(s.copy(animations=it))}}
     "dice"->{val labels=mapOf(DoublesRate.NATURAL to "Default low • 8.3%",DoublesRate.REDUCED_20 to "20% below default • 6.7%",DoublesRate.REDUCED_50 to "50% below default • 4.2%",DoublesRate.NEVER to "No doubles • 0%");DoublesRate.entries.forEach{rate->WoodMenuPlank(labels[rate]!!,if(rate==s.doublesRate)"◆  SELECTED — applies equally to both" else "Same rule for player and AI"){vm.update(s.copy(doublesRate=rate))}}}
     "clock"->{val labels=mapOf(ClockPreset.OFF to ("CLOCK OFF" to "Relaxed play without a timer"),ClockPreset.RAPID_60 to ("RAPID • 1:00 BANK" to "10 seconds per turn • unused time is saved"),ClockPreset.STANDARD_180 to ("STANDARD • 3:00 BANK" to "20 seconds per turn • unused time is saved"));ClockPreset.entries.forEach{preset->val text=labels[preset]!!;WoodMenuPlank(text.first,if(preset==s.clockPreset)"◆  SELECTED — ${text.second}" else text.second){vm.update(s.copy(clockPreset=preset))}}}
     "guidance"->{WoodMenuPlank("SIMPLE GUIDANCE",if(s.guidanceMode==GuidanceMode.SIMPLE)"◆  SELECTED — only the immediate move" else "Show only one legal step"){vm.update(s.copy(guidanceMode=GuidanceMode.SIMPLE))};WoodMenuPlank("COACH GUIDANCE",if(s.guidanceMode==GuidanceMode.COACH)"◆  SELECTED — complete routes" else "Educational compound paths and dice"){vm.update(s.copy(guidanceMode=GuidanceMode.COACH))}}
     else->{WoodMenuPlank("SOUND & FEEL",if(s.sound)"Sound on" else "Sound off"){switch("sound")};WoodMenuPlank("MATCH CLOCK",when(s.clockPreset){ClockPreset.OFF->"Off";ClockPreset.RAPID_60->"1:00 bank • 10 sec turns";ClockPreset.STANDARD_180->"3:00 bank • 20 sec turns"}){switch("clock")};WoodMenuPlank("MOVE GUIDANCE",if(s.guidanceMode==GuidanceMode.SIMPLE)"Simple • next move only" else "Coach • complete routes"){switch("guidance")};WoodMenuPlank("DICE — DOUBLES",when(s.doublesRate){DoublesRate.NATURAL->"Default low • 8.3%";DoublesRate.REDUCED_20->"20% below default • 6.7%";DoublesRate.REDUCED_50->"50% below default • 4.2%";DoublesRate.NEVER->"Doubles disabled"}){switch("dice")};WoodMenuPlank("BOARD THEME",s.theme.name.replace('_',' ')){switch("themes")};WoodMenuPlank("AI PLAYING STYLE",s.aiPersona.title){switch("persona")};WoodMenuPlank("AI DIFFICULTY",s.difficulty.name){switch("difficulty")}}
    }
   }
  }
 }
}
@Composable private fun WoodMenuPlank(title:String,sub:String="",large:Boolean=false,go:()->Unit){
 val shape=RoundedCornerShape(topStart=12.dp,bottomStart=12.dp,topEnd=4.dp,bottomEnd=4.dp);val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val selected=sub.startsWith("◆")
 val scale by animateFloatAsState(if(pressed)1.045f else if(selected)1.022f else 1f,tween(110),label="plankScale");val shift by animateFloatAsState(if(pressed)-11f else if(selected)-6f else 0f,tween(140),label="plankShift")
 Column(Modifier.width(if(large)310.dp else 282.dp).height(if(large)62.dp else 54.dp).graphicsLayer{scaleX=scale;scaleY=scale;translationX=shift.dp.toPx();shadowElevation=(if(pressed)20 else if(selected)16 else 12).dp.toPx();this.shape=shape;clip=false}.clip(shape).background(Brush.verticalGradient(if(selected)listOf(Color(0xffae7548),Color(0xff714126),Color(0xff432014))else listOf(Color(0xff9a6039),Color(0xff63341f),Color(0xff3b1c11)))).border(if(selected)1.5.dp else 1.dp,if(selected)Color(0xffffd28b)else Color(0xffe1ab70).copy(.72f),shape).clickable(interactionSource=interaction,indication=null,onClick=go).padding(horizontal=18.dp,vertical=9.dp)){
  Text(title,color=Color(0xffffdda0),fontWeight=FontWeight.Black,fontSize=if(large)14.sp else 12.sp,letterSpacing=.8.sp)
  if(sub.isNotEmpty())Text(sub,color=Color(0xffffead0).copy(if(selected).86f else .68f),fontSize=9.sp)
 }
}
@Composable private fun WoodTogglePlank(title:String,value:Boolean,set:(Boolean)->Unit){
 val shape=RoundedCornerShape(topStart=12.dp,bottomStart=12.dp,topEnd=4.dp,bottomEnd=4.dp)
 Row(Modifier.width(282.dp).height(54.dp).graphicsLayer{shadowElevation=12.dp.toPx();this.shape=shape;clip=false}.clip(shape).background(Brush.verticalGradient(listOf(Color(0xff925936),Color(0xff4b2618)))).border(1.dp,Color(0xffd8a069).copy(.68f),shape).clickable{set(!value)}.padding(horizontal=17.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),color=Color(0xffffdda0),fontWeight=FontWeight.Black,fontSize=11.sp);Switch(value,set,colors=SwitchDefaults.colors(checkedThumbColor=Color(0xffffe1a5),checkedTrackColor=Color(0xff3c1d12),uncheckedThumbColor=Color(0xffc49a70),uncheckedTrackColor=Color(0xff62402c)))}
}
@Composable private fun DrawerNav(title:String,sub:String,go:()->Unit){Row(Modifier.fillMaxWidth().padding(vertical=6.dp).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xfffffbef),Color(0xffe2c99e)))).border(1.dp,Color(0xff9b6a3e).copy(.62f),RoundedCornerShape(12.dp)).clickable(onClick=go).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Color(0xff3f2116),fontWeight=FontWeight.Black,fontSize=12.sp);Text(sub,color=Color(0xff836247),fontSize=10.sp)};Text("›",color=Color(0xff7b492a),fontSize=25.sp)}}
@Composable private fun DrawerTab(icon:String,on:Boolean,go:()->Unit){FilledTonalButton(go,Modifier.size(58.dp),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=if(on)Color(0xffffd998)else Color(0xff3b1c11))){Text(icon,color=if(on)Color(0xff3b1c11)else Color(0xffffe2ad),fontSize=22.sp)}}
@Composable private fun DrawerButton(text:String,go:()->Unit){Button(go,Modifier.fillMaxWidth().padding(vertical=5.dp).height(50.dp),colors=ButtonDefaults.buttonColors(Color(0xfffff4de)),border=BorderStroke(1.dp,Color(0xffa87548))){Text(text,Modifier.fillMaxWidth(),color=Color(0xff3b2117),fontWeight=FontWeight.Bold)}}
@Composable private fun DrawerHeader(text:String){Text(text,Modifier.padding(top=18.dp,bottom=5.dp),color=Color(0xff615b50),fontSize=11.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)}
@Composable private fun DrawerSwitch(text:String,value:Boolean,set:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text(text,Modifier.weight(1f),color=Color(0xff3b2117),fontWeight=FontWeight.SemiBold);Switch(value,set,colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Color(0xff8b542f),uncheckedThumbColor=Color(0xff6f7475),uncheckedTrackColor=Color(0xffd7c19e)))}}
@Composable private fun DrawerChoice(text:String,on:Boolean=false,go:()->Unit){TextButton(go,Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(on)Color(0xffead0a4)else Color.Transparent)){Text((if(on)"◆  " else "")+text,Modifier.fillMaxWidth(),color=Color(0xff3b2117),fontWeight=if(on)FontWeight.Bold else FontWeight.Normal)}}
@Composable private fun ThemeChoice(theme:BoardTheme,on:Boolean,go:()->Unit){val p=boardPalette(theme);Row(Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(14.dp)).background(if(on)Color(0xffead0a4)else Color.Transparent).clickable(onClick=go).padding(9.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp,30.dp).clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(listOf(p.frame,p.field,p.pointA,p.pointB))).border(1.dp,Color(0xff9b6a3e),RoundedCornerShape(8.dp)));Spacer(Modifier.width(12.dp));Text(theme.name.replace('_',' '),Modifier.weight(1f),color=Color(0xff3b2117),fontWeight=if(on)FontWeight.Bold else FontWeight.Normal);if(on)Text("◆",color=Color(0xff665b49))}}
@Composable private fun MatchClocks(clock:ClockState,turn:Player,modifier:Modifier=Modifier){
 fun format(ms:Long):String{val safe=ms.coerceAtLeast(0);return if(safe<10_000)"${safe/1000}.${(safe%1000)/100}" else "%d:%02d".format(safe/60_000,(safe/1000)%60)}
 Row(modifier,horizontalArrangement=Arrangement.SpaceBetween){
  ClockFace("BLACK",format(clock.blackMillis),turn==Player.BLACK,clock.turnMillis)
  ClockFace("WHITE",format(clock.whiteMillis),turn==Player.WHITE,clock.turnMillis)
 }
}
@Composable private fun ClockFace(label:String,time:String,active:Boolean,turnMillis:Long){
 Row(Modifier.width(116.dp).height(39.dp).graphicsLayer{shadowElevation=7.dp.toPx();shape=RoundedCornerShape(7.dp);clip=false}.clip(RoundedCornerShape(7.dp)).background(Brush.verticalGradient(if(active)listOf(Color(0xffaa7044),Color(0xff442015))else listOf(Color(0xff4c352b),Color(0xff20130f)))).border(1.dp,if(active)Color(0xffffd47f)else Color.White.copy(.18f),RoundedCornerShape(7.dp)).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){Text(label,color=Color.White.copy(.62f),fontSize=7.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp);Text(time,color=if(active)Color(0xffffe0a0)else Color.White.copy(.62f),fontSize=15.sp,fontWeight=FontWeight.Black)}
  Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Black.copy(.32f)),contentAlignment=Alignment.Center){Text("${(turnMillis/1000).coerceAtLeast(0)}",color=if(turnMillis<3000)Color(0xffff786c)else Color.White.copy(.72f),fontSize=8.sp,fontWeight=FontWeight.Bold)}
 }
}
@Composable private fun CarvedScoreCounter(white:Int,black:Int,modifier:Modifier=Modifier){
 Box(modifier.size(204.dp,49.dp),contentAlignment=Alignment.TopCenter){
  Box(Modifier.align(Alignment.Center).width(174.dp).height(3.dp).background(Brush.verticalGradient(listOf(Color(0xffeee9e2),Color(0xff5a5651)))))
  // Wooden feet visibly join the mechanism to the board's upper rail.
  Box(Modifier.align(Alignment.BottomStart).padding(start=18.dp).size(17.dp,22.dp).clip(RoundedCornerShape(bottomStart=5.dp,bottomEnd=5.dp)).background(Brush.horizontalGradient(listOf(Color(0xff35180d),Color(0xff8d5634),Color(0xff35180d)))))
  Box(Modifier.align(Alignment.BottomEnd).padding(end=18.dp).size(17.dp,22.dp).clip(RoundedCornerShape(bottomStart=5.dp,bottomEnd=5.dp)).background(Brush.horizontalGradient(listOf(Color(0xff35180d),Color(0xff8d5634),Color(0xff35180d)))))
  Row(Modifier.height(43.dp).graphicsLayer{shadowElevation=7.dp.toPx();shape=RoundedCornerShape(7.dp);clip=false}.clip(RoundedCornerShape(7.dp)).background(Brush.verticalGradient(listOf(Color(0xff9b613b),Color(0xff35180d)))).border(1.dp,Color(0xffdda66f).copy(.65f),RoundedCornerShape(7.dp)).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)){
   repeat(3){Box(Modifier.size(14.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Brush.radialGradient(listOf(Color(0xff77706d),Color(0xff090909)))).border(.5.dp,Color.White.copy(.26f),androidx.compose.foundation.shape.CircleShape))}
   Box(Modifier.height(35.dp).clip(RoundedCornerShape(5.dp)).background(Brush.verticalGradient(listOf(Color(0xffc7c2bb),Color(0xff4f4c49)))).border(1.dp,Color(0xffeee8df),RoundedCornerShape(5.dp)).padding(horizontal=5.dp),contentAlignment=Alignment.Center){Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){ScoreDrum(black);ScoreDrum(white)}}
   repeat(3){Box(Modifier.size(14.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Brush.radialGradient(listOf(Color.White,Color(0xffd8d0c8),Color(0xff88827e)))).border(.5.dp,Color.Black.copy(.22f),androidx.compose.foundation.shape.CircleShape))}
  }
 }
}
@Composable private fun ScoreDrum(score:Int){Box(Modifier.size(26.dp,29.dp).clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(Color(0xff080808),Color(0xff242424),Color.Black))).border(.5.dp,Color.White.copy(.28f),RoundedCornerShape(3.dp)),contentAlignment=Alignment.Center){Text((score%10).toString(),color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Medium)}}
@Composable private fun TopGameBar(level:Difficulty,persona:AiPersona,match:MatchInfo,status:String,menu:()->Unit,settings:()->Unit,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth().height(30.dp).background(Brush.verticalGradient(listOf(Color(0xff75442d),Color(0xff4b2618)))).border(BorderStroke(1.dp,Brush.verticalGradient(listOf(Color(0xffd7a06e).copy(.55f),Color(0xff241008).copy(.65f))))).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){Text("${persona.title.removePrefix("THE ")} • L${level.ordinal+1}",color=Color.White.copy(.76f),fontSize=9.sp,fontWeight=FontWeight.Bold);Text(status,Modifier.padding(start=10.dp),color=Color(0xffffd77a),fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=.6.sp);Spacer(Modifier.weight(1f));IconButton(settings,Modifier.size(30.dp)){Text("⚙",color=Color(0xffffdfa0),fontSize=16.sp)}}}
@Composable private fun MiniWoodButton(text:String,modifier:Modifier=Modifier,go:()->Unit){Button(go,modifier.height(38.dp),contentPadding=PaddingValues(horizontal=10.dp),shape=RoundedCornerShape(9.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xff5a2b1a),contentColor=Color(0xffffe0a0)),border=BorderStroke(1.dp,Color(0xffd59a63))){Text(text,fontSize=11.sp,fontWeight=FontWeight.Black)}}
@Composable private fun SceneArrow(text:String,modifier:Modifier=Modifier,go:()->Unit){Box(modifier.size(38.dp,72.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xff17110e).copy(.64f)).border(1.dp,Color.White.copy(.38f),RoundedCornerShape(18.dp)).clickable(onClick=go),contentAlignment=Alignment.Center){Text(text,color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Light)}}
@Composable private fun BarEngraving(text:String,modifier:Modifier=Modifier,go:()->Unit){
 val shape=RoundedCornerShape(4.dp)
 Box(modifier.width(52.dp).height(18.dp).graphicsLayer{shadowElevation=1.dp.toPx();this.shape=shape}.clip(shape).background(Brush.verticalGradient(listOf(Color.Black.copy(.22f),Color(0xff4a2819).copy(.22f),Color.White.copy(.07f)))).border(1.dp,Color(0xff251108).copy(.66f),shape).clickable(onClick=go),contentAlignment=Alignment.Center){
  Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).background(Color.Black.copy(.45f)));Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(Color(0xffd7a16d).copy(.24f)))
  Text(text,Modifier.offset(y=1.dp),color=Color.White.copy(.14f),fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp);Text(text,Modifier.offset(y=(-.5).dp),color=Color(0xff211008),fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)
 }
}
@Composable private fun InlayButton(text:String,modifier:Modifier=Modifier,go:()->Unit){Button(go,modifier.widthIn(min=68.dp).height(30.dp),contentPadding=PaddingValues(horizontal=10.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xff55301f),contentColor=Color(0xffffdfa0)),border=BorderStroke(1.dp,Color(0xffbb8254))){Text(text,fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=.5.sp)}}
@Composable private fun PlayerCard(name:String,score:Int,active:Boolean,light:Boolean){Row(Modifier.clip(RoundedCornerShape(18.dp)).background(if(active)Glass else Glass.copy(.58f)).border(if(active)1.dp else 0.dp,if(active)Gold else Color.Transparent,RoundedCornerShape(18.dp)).padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(light)Color(0xffffe7bd)else Color(0xff1d2028)));Spacer(Modifier.width(9.dp));Column{Text(name,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.White);Row(Modifier.padding(top=5.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){repeat(5){i->Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(i<score.coerceAtMost(5))Gold else Color.White.copy(.20f)))}}}}}
@Composable private fun StatusPill(t:String){Text(t,Modifier.clip(RoundedCornerShape(50)).background(Glass).border(1.dp,Color.White.copy(.12f),RoundedCornerShape(50)).padding(horizontal=20.dp,vertical=9.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Color.White)}
@Composable private fun AnimatedDice(finalDice:List<Int>,style:DiceStyle,rolling:Boolean,onThrow:()->Unit,modifier:Modifier=Modifier){
 var drag by remember{mutableStateOf(Offset.Zero)};var throwVector by remember{mutableStateOf(Offset.Zero)}
 val x1=remember{Animatable(-90f)};val y1=remember{Animatable(-45f)};val r1=remember{Animatable(0f)}
 val x2=remember{Animatable(90f)};val y2=remember{Animatable(-35f)};val r2=remember{Animatable(0f)}
 var face1 by remember{mutableIntStateOf(1)};var face2 by remember{mutableIntStateOf(6)}
 LaunchedEffect(rolling){
  if(rolling){
   val released=throwVector;x1.snapTo(x1.value+released.x);x2.snapTo(x2.value+released.x);y1.snapTo(y1.value+released.y);y2.snapTo(y2.value+released.y);drag=Offset.Zero
   val tx1=(-82..-64).random().toFloat();val tx2=(64..82).random().toFloat();val commonY=(released.y*.12f).coerceIn(-18f,18f);val ty1=commonY+(-7..7).random();val ty2=commonY+(-7..7).random()
   kotlinx.coroutines.coroutineScope{
    launch{x1.animateTo(tx1,tween(1650,easing=FastOutSlowInEasing))};launch{x2.animateTo(tx2,tween(1680,easing=FastOutSlowInEasing))}
    launch{y1.animateTo(ty1,keyframes{durationMillis=1680;y1.value at 0;(ty1-42f) at 420;(ty1+10f) at 930;ty1 at 1680})}
    launch{y2.animateTo(ty2,keyframes{durationMillis=1680;y2.value at 0;(ty2-38f) at 460;(ty2+8f) at 970;ty2 at 1680})}
    launch{r1.animateTo(r1.value+720f,tween(1680,easing=LinearOutSlowInEasing))};launch{r2.animateTo(r2.value-720f,tween(1680,easing=LinearOutSlowInEasing))}
   }
  } else {face1=finalDice.getOrElse(0){face1};face2=finalDice.getOrElse(1){face2};r1.snapTo(0f);r2.snapTo(0f);drag=Offset.Zero}
 }
 Box(modifier.size(230.dp,140.dp).graphicsLayer{translationX=drag.x;translationY=drag.y}.then(if(!rolling&&finalDice.isEmpty())Modifier.pointerInput(Unit){detectDragGestures(onDragEnd={if(drag.getDistance()>24f){throwVector=drag;onThrow()}else drag=Offset.Zero},onDragCancel={drag=Offset.Zero}){change,amount->change.consume();drag+=amount}}else Modifier)){DieFace(finalDice.getOrElse(0){face1},style,rolling,Modifier.align(Alignment.Center).offset{x1.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y1.value.roundToInt())}}.rotate(r1.value));DieFace(finalDice.getOrElse(1){face2},style,rolling,Modifier.align(Alignment.Center).offset{x2.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y2.value.roundToInt())}}.rotate(r2.value));if(!rolling&&finalDice.size>2)DieFace(finalDice[2],style,false,Modifier.align(Alignment.Center).offset(x=(-34).dp,y=48.dp));if(!rolling&&finalDice.size>3)DieFace(finalDice[3],style,false,Modifier.align(Alignment.Center).offset(x=34.dp,y=48.dp))}
}
@Composable private fun DieFace(value:Int,style:DiceStyle,rolling:Boolean,modifier:Modifier=Modifier){
 val base=when(style){DiceStyle.CLASSIC->Color(0xffffedc5);DiceStyle.ONYX->Color(0xff252731);DiceStyle.CRYSTAL->Color(0xff65cce8)};val ink=if(style==DiceStyle.CLASSIC)Color(0xff49382c)else Color.White
 Canvas(modifier.size(46.dp)){
  val u=size.minDimension/62f
  drawRoundRect(Color.Black.copy(.34f),Offset(9*u,11*u),Size(46*u,47*u),CornerRadius(14*u))
  drawRoundRect(Color(0xff6f5136).copy(.44f),Offset(6*u,6*u),Size(46*u,46*u),CornerRadius(13*u),style=Stroke(2.4f*u))
  drawRoundRect(base.copy(.58f),Offset(7*u,10*u),Size(44*u,45*u),CornerRadius(12*u));drawLine(Color.Black.copy(.24f),Offset(12*u,54*u),Offset(46*u,54*u),2.2f*u)
  drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(.58f),base,base.copy(.74f))),Offset(7*u,5*u),Size(44*u,44*u),CornerRadius(12*u));drawRoundRect(Color(0xff765a40).copy(.72f),Offset(7*u,5*u),Size(44*u,44*u),CornerRadius(12*u),style=Stroke(1.5f*u));drawRoundRect(Color.White.copy(.54f),Offset(9*u,7*u),Size(40*u,40*u),CornerRadius(10*u),style=Stroke(1.2f*u))
  val spots=when(value){1->listOf(.5f to .5f);2->listOf(.28f to .28f,.72f to .72f);3->listOf(.27f to .27f,.5f to .5f,.73f to .73f);4->listOf(.28f to .28f,.72f to .28f,.28f to .72f,.72f to .72f);5->listOf(.27f to .27f,.73f to .27f,.5f to .5f,.27f to .73f,.73f to .73f);else->listOf(.28f to .23f,.72f to .23f,.28f to .5f,.72f to .5f,.28f to .77f,.72f to .77f)}
  spots.forEach{drawCircle(Color.Black.copy(.42f),4.2f*u,Offset((7+44*it.first)*u,(7+44*it.second)*u));drawCircle(ink,3.55f*u,Offset((7+44*it.first)*u,(7+44*it.second)*u));drawCircle(Color.White.copy(.22f),.9f*u,Offset((6.2f+44*it.first)*u,(6.2f+44*it.second)*u))};listOf(15f to 20f,42f to 18f,25f to 48f,45f to 40f).forEach{drawCircle(Color(0xff8e7253).copy(.10f),.7f*u,Offset(it.first*u,it.second*u))}
 }
}
@Composable private fun RoundAction(t:String,go:()->Unit){FilledTonalButton(go,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(46.dp),shape=androidx.compose.foundation.shape.CircleShape){Text(t,fontWeight=FontWeight.Bold)}}
