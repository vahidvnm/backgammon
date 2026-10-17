package com.dara.backgammon.ui

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import com.dara.backgammon.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dara.backgammon.ai.AiPersona
import com.dara.backgammon.ai.Difficulty
import com.dara.backgammon.core.*
import com.dara.backgammon.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

internal val Gold=Color(0xffffd274);internal val Glass=Color(0xcc11151b)
internal fun Difficulty.fa()=when(this){Difficulty.BEGINNER->"تازه‌کار";Difficulty.EASY->"آسان";Difficulty.MEDIUM->"متوسط";Difficulty.HARD->"سخت";Difficulty.EXPERT->"حرفه‌ای"}
internal fun BoardTheme.fa()=when(this){BoardTheme.PREMIUM_WOOD->"چوب ممتاز";BoardTheme.BRUSHED_METAL->"فلز برس‌خورده";BoardTheme.OBSIDIAN_IVORY->"ابسیدین و عاج";BoardTheme.CRYSTAL_GLASS->"یخ کریستالی";BoardTheme.ROYAL_SAPPHIRE->"یاقوت سلطنتی"}
internal fun PieceStyle.fa()=when(this){PieceStyle.KHATAM->"خاتم";PieceStyle.METAL->"فلزی";PieceStyle.OBSIDIAN_IVORY->"ابسیدین و عاج";PieceStyle.CRYSTAL->"کریستالی";PieceStyle.SAPPHIRE->"یاقوتی"}
internal fun Player.fa()=if(this==Player.WHITE)"سفید" else "سیاه"
internal fun AiPersona.fa()=when(this){AiPersona.BUILDER->"معمار";AiPersona.RUNNER->"دونده";AiPersona.HUNTER->"شکارچی";AiPersona.MASTER->"استاد"}
internal fun AiPersona.faHint()=when(this){
 AiPersona.BUILDER->"خانه‌سازی و بلوک دفاعی بلند"
 AiPersona.RUNNER->"مسابقه و فرار از درگیری"
 AiPersona.HUNTER->"حمله به مهره‌های تک و نگه‌داشتن حریف روی بار"
 else->"تعادل بین همه سبک‌ها"}
internal fun DoublesRate.fa()=when(this){DoublesRate.NATURAL->"طبیعی";DoublesRate.REDUCED_20->"۲۰٪ کمتر";DoublesRate.REDUCED_50->"۵۰٪ کمتر";DoublesRate.NEVER->"بدون جفت"}
internal fun tabFa(key:String)=when(key){"GAME"->"بازی";"AUDIO"->"صدا";"CONTROLS"->"کنترل‌ها";"APPEARANCE"->"ظاهر";"GUIDANCE"->"راهنما";"ABOUT"->"درباره";else->key}
@Composable fun BackgammonApp(vm:GameViewModel=viewModel()){
 val lifecycleOwner=LocalLifecycleOwner.current
 DisposableEffect(lifecycleOwner,vm){
  val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_START->vm.onForeground();Lifecycle.Event.ON_STOP->vm.onBackground();else->{}}}
  lifecycleOwner.lifecycle.addObserver(observer)
  onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}
 }
 val screen by vm.screen.collectAsStateWithLifecycle()
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,surface=Color(0xff11151b))){CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
  Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xff31483b),Color(0xff101d17))))){
   AnimatedContent(screen,transitionSpec={
    if(targetState.ordinal>initialState.ordinal)(slideInHorizontally(tween(480)){it}+fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(480)){-it/3}+fadeOut(tween(240)))
    else (slideInHorizontally(tween(480)){-it/2}+fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(480)){it}+fadeOut(tween(240)))
   },label="navigation"){destination->when(destination){Screen.MENU->MainMenu(vm);Screen.SETUP->SetupMenu(vm);Screen.GAME->GameScreen(vm)}}
  }
 }
 }
}
@Composable private fun GameScreen(vm:GameViewModel){val game by vm.game.collectAsStateWithLifecycle();val settings by vm.settings.collectAsStateWithLifecycle();val rolling by vm.rolling.collectAsStateWithLifecycle();val match by vm.match.collectAsStateWithLifecycle();val autoAssist by vm.autoAssist.collectAsStateWithLifecycle();val clock by vm.clock.collectAsStateWithLifecycle();var selected by remember(game.position,game.dice){mutableStateOf<Int?>(null)};var panel by remember{mutableStateOf("")};val legal=remember(game,match.thinking){if(match.thinking||(vm.mode==Mode.AI&&game.position.turn==Player.BLACK))emptyList() else GameEngine.legalMoves(game.position,game.dice)};val context=LocalContext.current
 // Back never drops the player out of a live match: it first clears the board
 // selection, then closes an open panel, and only then opens the match menu.
 BackHandler{
  when{
   panel.isNotEmpty()->panel=""
   selected!=null->selected=null
   else->panel="menu"
  }
 }
 fun haptic(){if(settings.vibration){val v=context.getSystemService(android.os.Vibrator::class.java);if(Build.VERSION.SDK_INT>=26)v?.vibrate(VibrationEffect.createOneShot(22,70))else @Suppress("DEPRECATION")v?.vibrate(22)}}
 val palette=boardPalette(settings.theme);val background=when(settings.tableScene){TableScene.DARK_GRASS->R.drawable.table_dark_grass;TableScene.STARRY_SKY->R.drawable.table_starry_sky;TableScene.AUTUMN_SUNSET->R.drawable.table_autumn_sunset;TableScene.DARK_RIVER->R.drawable.table_dark_river;TableScene.PERSIAN_RUG->when(settings.theme){BoardTheme.PREMIUM_WOOD->R.drawable.table_persian_rug;BoardTheme.BRUSHED_METAL->R.drawable.table_theme_metal;BoardTheme.OBSIDIAN_IVORY->R.drawable.table_theme_obsidian;BoardTheme.CRYSTAL_GLASS->R.drawable.table_theme_ice;BoardTheme.ROYAL_SAPPHIRE->R.drawable.table_theme_ruby}}
 fun cycleScene(step:Int){val all=TableScene.entries;val next=all[(all.indexOf(settings.tableScene)+step+all.size)%all.size];vm.update(settings.copy(tableScene=next))}
 BoxWithConstraints(Modifier.fillMaxSize()){
  val layout=boardLayoutProfile(maxWidth.value,maxHeight.value);val shell=boardPalette(settings.theme);val boardAxisOffset=boardAxisOffsetDp(maxWidth.value,layout.widthFraction).dp
  // The table scene is a photograph full of fine pattern, and on the richer boards it competes
  // with the playfield for attention. A gentle blur (Android 12+; a no-op below that), a wash and
  // a vignette throw it out of focus so the board and its checkers keep the eye. The playfield
  // itself is untouched.
  val sceneDim=when(settings.theme){BoardTheme.PREMIUM_WOOD->.14f;BoardTheme.BRUSHED_METAL->.24f;BoardTheme.CRYSTAL_GLASS->.30f;BoardTheme.OBSIDIAN_IVORY->.30f;BoardTheme.ROYAL_SAPPHIRE->.30f}
  Image(painterResource(background),null,Modifier.fillMaxSize().blur(7.dp),contentScale=ContentScale.Crop)
  Box(Modifier.fillMaxSize().background(Color.Black.copy(sceneDim)))
  Box(Modifier.fillMaxSize().background(Brush.radialGradient(0f to Color.Transparent,.52f to Color.Black.copy(sceneDim*.30f),1f to Color.Black.copy(sceneDim*1.35f))))
  Box(Modifier.align(Alignment.Center).fillMaxWidth(layout.widthFraction).fillMaxHeight(layout.heightFraction).absoluteOffset(y=16.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(.78f)).border(2.dp,Color.Black.copy(.82f),RoundedCornerShape(15.dp)))
  Box(Modifier.align(Alignment.Center).fillMaxWidth(layout.widthFraction).fillMaxHeight(layout.heightFraction).absoluteOffset(y=7.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(shell.frame,shell.frameDark))))
  BoxWithConstraints(Modifier.align(Alignment.Center).fillMaxWidth(layout.widthFraction).fillMaxHeight(layout.heightFraction).graphicsLayer{shadowElevation=42.dp.toPx();shape=RoundedCornerShape(13.dp);clip=false}.clip(RoundedCornerShape(13.dp)).background(Brush.verticalGradient(listOf(shell.frame.copy(.92f),shell.frameDark))).border(1.dp,shell.light.copy(.52f),RoundedCornerShape(13.dp))){
   FlatBoard(game,settings.theme,settings.pieces,selected,legal,{selected=it;haptic()},{vm.move(it);selected=null;haptic()},{from,to->vm.moveCombined(from,to).also{if(it){selected=null;haptic()}}},Modifier.fillMaxSize().padding(top=30.dp,start=3.dp,end=3.dp,bottom=3.dp),scene=settings.tableScene,guidance=settings.guidanceMode,animations=settings.animations)
   if(clock.active)ClockSettingsButton({panel="settings"},Modifier.align(Alignment.TopStart).padding(start=8.dp,top=1.dp)) else TopGameBar(settings.difficulty,false,if(match.thinking)"هوش مصنوعی در حال فکر" else if(rolling)"پرتاب تاس" else if(vm.mode==Mode.LOCAL)"نوبت ${game.position.turn.fa()}" else if(game.position.turn==Player.WHITE)"نوبت شما" else "نوبت هوش مصنوعی",{panel="settings"},settings.theme,Modifier.align(Alignment.TopCenter))
   val canDouble=!match.crawfordGame&&match.cube<64&&!game.rolled&&!rolling&&!match.thinking&&game.winner==null&&(vm.mode==Mode.LOCAL||game.position.turn==Player.WHITE)&&match.pendingDoubleBy==null&&(match.cubeOwner==null||match.cubeOwner==game.position.turn)
   val cubeY by animateDpAsState(if(match.cubeOwner==Player.WHITE&&match.pendingDoubleBy==null)maxHeight-45.dp else 6.dp,tween(620,easing=FastOutSlowInEasing),label="cubeDock")
   AxleDoublingCube(match.cube,match.cubeOwner,match.pendingDoubleBy,canDouble,{vm.offerDouble()},settings.theme,Modifier.align(Alignment.TopCenter).absoluteOffset(x=boardAxisOffset,y=cubeY))
   Row(Modifier.align(Alignment.Center).absoluteOffset(x=layout.actionOffsetDp.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){val canUndo by vm.canUndo.collectAsStateWithLifecycle();if(vm.mode==Mode.AI)WoodPlaque(if(autoAssist)"خودکار روشن" else "خودکار خاموش",if(autoAssist)"●" else "○",116.dp,on=autoAssist,enabled=true,theme=settings.theme){vm.toggleAutoAssist()};WoodPlaque("بازگردانی","↶",92.dp,on=true,enabled=canUndo,theme=settings.theme){vm.undo()}}

  }
  if(clock.active)MatchClocks(clock,game.position.turn,settings.theme,Modifier.align(Alignment.TopCenter).absoluteOffset(x=boardAxisOffset).fillMaxWidth(.62f).padding(top=7.dp))
  if(match.crawfordGame)Text("دست کراوفورد • مکعب غیرفعال",Modifier.align(Alignment.BottomCenter).padding(bottom=4.dp).background(Color.Black.copy(.62f),RoundedCornerShape(8.dp)).padding(horizontal=10.dp,vertical=4.dp),color=Color(0xffffd99a),fontSize=10.sp,fontWeight=FontWeight.Bold)
  CarvedScoreCounter(match.whiteScore,match.blackScore,settings.theme,Modifier.align(Alignment.TopCenter).absoluteOffset(x=boardAxisOffset).padding(top=2.dp))
  if(game.dice.isNotEmpty()||rolling||(!game.rolled&&!(vm.mode==Mode.AI&&game.position.turn==Player.BLACK))) AnimatedDice(game.dice,when(settings.theme){BoardTheme.PREMIUM_WOOD->if(game.position.turn==Player.WHITE)DiceStyle.CLASSIC else DiceStyle.ONYX;BoardTheme.BRUSHED_METAL->if(game.position.turn==Player.WHITE)DiceStyle.GOLD else DiceStyle.SILVER;BoardTheme.OBSIDIAN_IVORY->if(game.position.turn==Player.WHITE)DiceStyle.MARBLE_LIGHT else DiceStyle.MARBLE_DARK;BoardTheme.CRYSTAL_GLASS->if(game.position.turn==Player.WHITE)DiceStyle.GLASS_LIGHT else DiceStyle.GLASS_DARK;BoardTheme.ROYAL_SAPPHIRE->if(game.position.turn==Player.WHITE)DiceStyle.GOLD else DiceStyle.SAPPHIRE},rolling,{vm.roll();haptic()},Modifier.align(Alignment.Center).absoluteOffset(x=layout.diceOffsetDp.dp))
  SceneArrow("‹",Modifier.align(Alignment.CenterStart).padding(start=8.dp)){cycleScene(-1)}
  SceneArrow("›",Modifier.align(Alignment.CenterEnd).padding(end=8.dp)){cycleScene(1)}
 }
 AnimatedVisibility(panel.isNotEmpty(),enter=fadeIn(tween(260))+scaleIn(tween(320),initialScale=.96f),exit=fadeOut(tween(220))+scaleOut(tween(220),targetScale=.97f)){if(panel=="settings")InGameSettingsStudio(settings,match,vm){panel=""}else GameDrawer(panel,settings,match,vm,{panel=""},{panel=it})}
 match.pendingDoubleBy?.takeIf{vm.mode==Mode.LOCAL||it==Player.BLACK}?.let{offerer->AlertDialog(onDismissRequest={},title={Text("پیشنهاد دوبرابر از ${offerer.fa()} تا ×${match.cube*2}")},text={Text("بازیکن ${offerer.other().fa()} می‌تواند پیشنهاد را قبول کند و مالک مکعب شود، یا آن را رد کند و بازی را با امتیاز فعلی واگذار کند.")},confirmButton={Button({vm.acceptDouble()}){Text("قبول")}},dismissButton={OutlinedButton({vm.declineDouble()}){Text("رد")}})}
 game.winner?.let{w->VictoryCelebration(w,match.whiteScore,match.blackScore,Modifier.fillMaxSize(),settings.animations)}
 game.winner?.let{w->
  val loser=w.other();val local=vm.mode==Mode.LOCAL;val won=w==Player.WHITE;val champion=vm.matchWinner()
  val result=if(match.timedOutBy!=null)"زمان بازیکن تمام شد" else if(match.droppedBy!=null)"پیشنهاد دوبرابر رد شد — بازی ${match.cube} امتیازی" else when{game.position.off(loser)>0->"برد معمولی";game.position.bar(loser)>0->"بک‌گمون";else->"گمون"}
  AlertDialog({},icon={Text(if(won)"✦" else "◆",fontSize=42.sp,color=if(won)Color(0xffffd36a)else Color(0xffb87a69))},title={Text(if(champion!=null)"${champion.fa()} برنده مسابقه شد" else if(local)"${w.fa()} برنده دست شد" else if(won)"پیروزی" else "شکست",color=Color(0xffffdda0),fontWeight=FontWeight.Black,letterSpacing=2.sp)},text={Column(horizontalAlignment=Alignment.CenterHorizontally){Text(if(champion!=null)"مسابقه ${match.target} امتیازی به پایان رسید." else if(local)"این دست به بازیکن ${w.fa()} رسید." else if(won)"یک پایان استادانه." else "این دست به حریف رسید.",color=Color.White.copy(.78f),textAlign=TextAlign.Center);Spacer(Modifier.height(10.dp));Text("$result\nامتیاز  ${match.whiteScore} — ${match.blackScore}",color=Color(0xffffd59a),textAlign=TextAlign.Center,fontWeight=FontWeight.Bold)}},confirmButton={Button({vm.restart()},colors=ButtonDefaults.buttonColors(Color(0xff8a522f))){Text(if(champion!=null)"مسابقه جدید" else "دست بعدی")}},dismissButton={TextButton({vm.menu()}){Text("خروج",color=Color(0xffffd59a))}},containerColor=Color(0xff32170f),tonalElevation=18.dp,shape=RoundedCornerShape(16.dp))
 }
}
