package com.arena.backgammon.ui

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.backgammon.ai.Difficulty
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlinx.coroutines.delay

private data class Palette(val bg:Color,val wood:Color,val dark:Color,val light:Color,val accent:Color)
private fun palette(t:BoardTheme)=when(t){
 BoardTheme.CLASSIC_WOOD->Palette(Color(0xff17100d),Color(0xff8b5334),Color(0xff512d20),Color(0xffd7a56f),Color(0xffffc86b))
 BoardTheme.DARK_WOOD->Palette(Color(0xff090b0e),Color(0xff322822),Color(0xff12171d),Color(0xff8c6b52),Color(0xffd7a45d))
 BoardTheme.LUXURY->Palette(Color(0xff071814),Color(0xff174b3c),Color(0xff092c25),Color(0xffd2b36c),Color(0xffffdf84))
 BoardTheme.MODERN->Palette(Color(0xff101421),Color(0xff33415c),Color(0xff141b2d),Color(0xff8da9c4),Color(0xff64d8ff))
 BoardTheme.MINIMAL->Palette(Color(0xffdedbd2),Color(0xfff4f3ee),Color(0xff77736b),Color(0xffc9c3b8),Color(0xffde6b48)) }

@Composable fun BackgammonApp(vm:GameViewModel=viewModel()){
 val settings by vm.settings.collectAsState();val screen by vm.screen.collectAsState();val p=palette(settings.theme)
 MaterialTheme(colorScheme=darkColorScheme(primary=p.accent,surface=Color(0xdd201d1c),onSurface=Color.White)){Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bg,p.wood.copy(.65f),p.bg)))){AnimatedContent(screen,label="screen"){if(it==Screen.MENU)Menu(vm,settings,p) else Game(vm,settings,p)}}}
}

@Composable private fun Glass(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit)=Column(modifier.clip(RoundedCornerShape(28.dp)).background(Color.White.copy(.10f)).border(1.dp,Color.White.copy(.22f),RoundedCornerShape(28.dp)).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,content=content)
@Composable private fun MenuButton(text:String,onClick:()->Unit)=Button(onClick=onClick,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White.copy(.13f)),border=BorderStroke(1.dp,Color.White.copy(.2f))){Text(text,fontWeight=FontWeight.SemiBold,fontSize=17.sp)}

@Composable private fun Menu(vm:GameViewModel,s:Settings,p:Palette){var dialog by remember{mutableStateOf("")};Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)){Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Text("BACKGAMMON",fontSize=34.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp,color=p.accent);Text("A timeless game of strategy",color=Color.White.copy(.65f));Spacer(Modifier.height(34.dp));Glass(Modifier.fillMaxWidth()){MenuButton("Play vs AI"){dialog="difficulty"};Spacer(Modifier.height(10.dp));MenuButton("Two Player"){vm.start(Mode.LOCAL)};Spacer(Modifier.height(10.dp));MenuButton("Themes"){dialog="themes"};Spacer(Modifier.height(10.dp));MenuButton("Settings"){dialog="settings"};Spacer(Modifier.height(10.dp));MenuButton("About"){dialog="about"}}}};if(dialog.isNotEmpty())MenuDialog(dialog,s,{dialog=""},{vm.update(it)},onAi={vm.update(s.copy(difficulty=it));vm.start(Mode.AI)})}

@Composable private fun MenuDialog(kind:String,s:Settings,close:()->Unit,update:(Settings)->Unit,onAi:(Difficulty)->Unit){AlertDialog(onDismissRequest=close,confirmButton={TextButton(onClick=close){Text("Done")}},title={Text(when(kind){"themes"->"Board Themes";"difficulty"->"Choose Difficulty";"about"->"About";else->"Settings"})},text={Column(Modifier.verticalScroll(rememberScrollState())){when(kind){
 "difficulty"->Difficulty.entries.forEach{Option(it.name.lowercase().replaceFirstChar(Char::uppercase)){onAi(it)}}
 "themes"->BoardTheme.entries.forEach{Option(it.name.replace('_',' ').lowercase().split(' ').joinToString(" "){x->x.replaceFirstChar(Char::uppercase)},it==s.theme){update(s.copy(theme=it))}}
 "about"->Text("Premium offline Backgammon\n\nStandard rules • Five AI levels • Local two-player\n\nDesigned for comfortable one-finger play. No internet connection is required.")
 else->{Toggle("Sound effects",s.sound){update(s.copy(sound=it))};Toggle("Music ready",s.music){update(s.copy(music=it))};Toggle("Vibration",s.vibration){update(s.copy(vibration=it))};Toggle("Animations",s.animations){update(s.copy(animations=it))};Text("Checker style",fontWeight=FontWeight.Bold);PieceStyle.entries.forEach{Option(it.name,it==s.pieces){update(s.copy(pieces=it))}};Text("Dice style",fontWeight=FontWeight.Bold);DiceStyle.entries.forEach{Option(it.name,it==s.dice){update(s.copy(dice=it))}}}
 }}})}
@Composable private fun Toggle(name:String,value:Boolean,set:(Boolean)->Unit)=Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically){Text(name,Modifier.weight(1f));Switch(value,set)}
@Composable private fun Option(name:String,selected:Boolean=false,go:()->Unit)=TextButton(go,Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=if(selected)MaterialTheme.colorScheme.primary else Color.White)){Text((if(selected)"✓  " else "")+name,Modifier.fillMaxWidth())}

@Composable private fun Game(vm:GameViewModel,s:Settings,p:Palette){val state by vm.game.collectAsState();val rolling by vm.rolling.collectAsState();var selected by remember(state.position,state.dice){mutableStateOf<Int?>(null)};var paused by remember{mutableStateOf(false)};val legal=remember(state){GameEngine.legalMoves(state.position,state.dice)};val context=LocalContext.current
 fun feedback(){if(s.vibration){val v=context.getSystemService(Vibrator::class.java);if(Build.VERSION.SDK_INT>=26)v?.vibrate(VibrationEffect.createOneShot(25,80))else @Suppress("DEPRECATION") v?.vibrate(25)}}
 Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=12.dp,vertical=8.dp)){
  Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton({paused=true}){Text("Ⅱ",fontSize=22.sp)};Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text(if(state.position.turn==Player.WHITE)"Ivory's turn" else if(vm.mode==Mode.AI)"AI is thinking" else "Onyx's turn",fontWeight=FontWeight.Bold,fontSize=18.sp);Text(if(!state.rolled)"Roll to begin" else "${state.dice.size} move${if(state.dice.size==1)"" else "s"} remaining",fontSize=12.sp,color=Color.White.copy(.7f))};Text("${state.position.offWhite}  —  ${state.position.offBlack}",fontWeight=FontWeight.Bold);Spacer(Modifier.width(12.dp))}
  Spacer(Modifier.height(8.dp));Board(state,p,s.pieces,selected,legal,{src->selected=src;feedback()},{move->vm.move(move);selected=null;feedback()},Modifier.weight(1f).fillMaxWidth());Spacer(Modifier.height(10.dp));DiceRow(state.dice,rolling,s.dice,p.accent);Spacer(Modifier.height(8.dp));Button({vm.roll();feedback()},enabled=!state.rolled&&!rolling&&state.winner==null&&!(vm.mode==Mode.AI&&state.position.turn==Player.BLACK),modifier=Modifier.fillMaxWidth(.72f).height(54.dp),shape=RoundedCornerShape(18.dp)){Text(if(rolling)"Rolling…" else "ROLL DICE",fontWeight=FontWeight.Black)}
 }}
 if(paused)AlertDialog({paused=false},title={Text("Game paused")},text={Text("Take your time. Your board is waiting.")},confirmButton={Button({paused=false}){Text("Resume")}},dismissButton={TextButton({vm.menu()}){Text("Main menu")}})
 state.winner?.let{winner->AlertDialog({},title={Text(if(winner==Player.WHITE)"Ivory wins!" else "Onyx wins!")},text={Text("A beautifully played game.\n${state.position.off(winner)} checkers borne off.")},confirmButton={Button({vm.restart()}){Text("Rematch")}},dismissButton={TextButton({vm.menu()}){Text("Main menu")}})}
}

@Composable private fun DiceRow(dice:List<Int>,rolling:Boolean,style:DiceStyle,accent:Color){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){val shown=if(rolling)listOf(1+(System.currentTimeMillis()/90%6).toInt(),1+(System.currentTimeMillis()/70%6).toInt()) else dice.take(2);shown.forEach{Die(it,style,accent,rolling)}}}
@Composable private fun Die(value:Int,style:DiceStyle,accent:Color,rolling:Boolean){Box(Modifier.size(52.dp).graphicsLayer{rotationZ=if(rolling)18f else 0f;shadowElevation=10f}.clip(RoundedCornerShape(12.dp)).background(if(style==DiceStyle.ONYX)Color(0xff17191f) else if(style==DiceStyle.CRYSTAL)accent.copy(.7f) else Color(0xfffff6df)).border(1.dp,Color.White.copy(.4f),RoundedCornerShape(12.dp))){Canvas(Modifier.fillMaxSize()){val dark=if(style==DiceStyle.CLASSIC)Color(0xff2a211c) else Color.White;val positions=mapOf(1 to listOf(.5f to .5f),2 to listOf(.28f to .28f,.72f to .72f),3 to listOf(.28f to .28f,.5f to .5f,.72f to .72f),4 to listOf(.28f to .28f,.72f to .28f,.28f to .72f,.72f to .72f),5 to listOf(.28f to .28f,.72f to .28f,.5f to .5f,.28f to .72f,.72f to .72f),6 to listOf(.28f to .25f,.72f to .25f,.28f to .5f,.72f to .5f,.28f to .75f,.72f to .75f));positions[value]?.forEach{drawCircle(dark,size.minDimension*.065f,Offset(size.width*it.first,size.height*it.second))}}}}

@Composable private fun Board(state:TurnState,p:Palette,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,modifier:Modifier){
 val destinations=legal.filter{it.from==selected};Canvas(modifier.aspectRatio(.66f).clip(RoundedCornerShape(20.dp)).pointerInput(state,selected){detectTapGestures{tap->
  val pad=size.width*.055f;val boardW=size.width-pad*2;val boardH=size.height-pad*2;val col=((tap.x-pad)/(boardW/12)).toInt().coerceIn(0,11);val isTop=tap.y<size.height/2;val point=if(isTop)12+col else 11-col
  val offTap=tap.x>size.width*.91f;val barTap=tap.x in size.width*.455f..size.width*.545f
  val target=when{offTap->Move.OFF;barTap->Move.BAR;else->point}
  destinations.filter{it.to==target}.maxByOrNull{it.die}?.let(move) ?: run {if(target!=Move.OFF&&((target==Move.BAR&&state.position.bar(state.position.turn)>0)||(target in 0..23&&state.position.points[target]*state.position.turn.sign>0)))select(target)}
 }}.background(p.wood)){
  val pad=size.width*.055f;val left=pad;val top=pad;val w=size.width-pad*2;val h=size.height-pad*2;drawRoundRect(Brush.verticalGradient(listOf(p.wood.copy(.92f),p.wood,p.dark.copy(.75f))),Offset(left,top),Size(w,h),CornerRadius(18f));
  // central hinge/bar and bear-off rails
  drawRect(p.dark.copy(.8f),Offset(size.width*.47f,top),Size(size.width*.06f,h));drawRect(Color.Black.copy(.28f),Offset(size.width*.91f,top),Size(size.width*.035f,h));drawLine(Color.White.copy(.15f),Offset(left,size.height/2),Offset(left+w,size.height/2),2f)
  val cw=w/12f;for(col in 0..11){val x=left+col*cw;val c=if(col%2==0)p.light else p.dark;val pathTop=Path().apply{moveTo(x+2,top);lineTo(x+cw-2,top);lineTo(x+cw/2,top+h*.39f);close()};val pathBottom=Path().apply{moveTo(x+2,top+h);lineTo(x+cw-2,top+h);lineTo(x+cw/2,top+h*.61f);close()};drawPath(pathTop,c.copy(.92f));drawPath(pathBottom,if(col%2==0)p.dark else p.light)}
  fun center(point:Int,stack:Int):Offset{val col=if(point<12)11-point else point-12;val x=left+(col+.5f)*cw;val fromTop=point>=12;val r=cw*.39f;val spacing=minOf(r*1.58f,h*.32f/5);val y=if(fromTop)top+r+stack*spacing else top+h-r-stack*spacing;return Offset(x,y)}
  for(point in 0..23){val count=kotlin.math.abs(state.position.points[point]);for(i in 0 until minOf(count,5)){val c=center(point,i);checker(c,cw*.38f,state.position.points[point]>0,pieces,selected==point&&i==minOf(count,5)-1)}}
  // Bar pieces
  fun barPiece(white:Boolean,count:Int,y:Float){if(count>0){val c=Offset(size.width*.5f,y);checker(c,cw*.37f,white,pieces,selected==Move.BAR);}}
  barPiece(true,state.position.barWhite,size.height*.58f);barPiece(false,state.position.barBlack,size.height*.42f)
  destinations.forEach{m->val c=if(m.to==Move.OFF)Offset(size.width*.925f,size.height/2) else center(m.to, kotlin.math.abs(state.position.points[m.to]).coerceAtMost(4));drawCircle(p.accent.copy(.25f),cw*.34f,c);drawCircle(p.accent,cw*.13f,c)}
 }
}
private fun DrawScope.checker(c:Offset,r:Float,white:Boolean,style:PieceStyle,selected:Boolean){val base=when(style){PieceStyle.IVORY->if(white)Color(0xfffff1ce) else Color(0xff25242a);PieceStyle.MARBLE->if(white)Color(0xffe8edf2) else Color(0xff354052);PieceStyle.NEON->if(white)Color(0xff77e8ff) else Color(0xffff5577)};drawCircle(Color.Black.copy(.32f),r,Offset(c.x,c.y+r*.16f));drawCircle(Brush.radialGradient(listOf(base.copy(.95f),base.copy(.65f),Color.Black.copy(.25f)),c-Offset(r*.25f,r*.3f),r*1.45f),r,c);drawCircle(if(selected)Color(0xffffd469) else Color.White.copy(.25f),r,c,style=Stroke(width=if(selected)4f else 2f));drawCircle(Color.White.copy(.18f),r*.7f,c,style=Stroke(width=2f))}
