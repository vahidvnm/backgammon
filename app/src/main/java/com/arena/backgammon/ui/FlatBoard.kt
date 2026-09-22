package com.arena.backgammon.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.pointer.pointerInput
import com.arena.backgammon.R
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlin.math.*

private data class CheckerMotion(val from:Int,val to:Int,val white:Boolean)
data class BoardPalette(val frame:Color,val frameDark:Color,val field:Color,val pointA:Color,val pointB:Color,val light:Color,val dark:Color,val accent:Color)
fun boardPalette(t:BoardTheme)=when(t){
 BoardTheme.PREMIUM_WOOD->BoardPalette(Color(0xff7b4529),Color(0xff2d160d),Color(0xffa8754f),Color(0xffead7ad),Color(0xff6d2730),Color(0xffffdfa0),Color(0xff351a15),Color(0xffd7b25b))
 BoardTheme.MARBLE_STONE->BoardPalette(Color(0xff8c9298),Color(0xff30363c),Color(0xffc8c9c5),Color(0xfff0eee7),Color(0xff596773),Color(0xfff5f3ea),Color(0xff263039),Color(0xffb9c5cc))
 BoardTheme.SMOKED_GLASS->BoardPalette(Color(0xff334650),Color(0xff0b151b),Color(0xff183541),Color(0xff9ed4db),Color(0xff245f70),Color(0xffd5f5f4),Color(0xff0b2029),Color(0xff7de3e8))
}

/** Precise top-down board inspired by classic mobile layouts, drawn entirely with original vector assets. */
@Composable fun FlatBoard(state:TurnState,theme:BoardTheme,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,modifier:Modifier=Modifier){
 val p=boardPalette(theme);val texture=ImageBitmap.imageResource(when(theme){BoardTheme.PREMIUM_WOOD->R.drawable.premium_walnut_texture;BoardTheme.MARBLE_STONE->R.drawable.ivory_marble_texture;BoardTheme.SMOKED_GLASS->R.drawable.smoked_glass_texture});val materialBrush=remember(texture){ShaderBrush(ImageShader(texture,TileMode.Mirror,TileMode.Mirror))};val pulse by rememberInfiniteTransition(label="legal").animateFloat(.58f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse")
 var previous by remember{mutableStateOf(state.position.copyDeep())};var motion by remember{mutableStateOf<CheckerMotion?>(null)};val travel=remember{Animatable(1f)}
 LaunchedEffect(state.position){
  val now=state.position;val sign=now.turn.sign
  if(!previous.points.contentEquals(now.points)||previous.bar(now.turn)!=now.bar(now.turn)||previous.off(now.turn)!=now.off(now.turn)){
   val from=(0..23).firstOrNull{previous.points[it]*sign>now.points[it]*sign}?:if(previous.bar(now.turn)>now.bar(now.turn))Move.BAR else null
   val to=(0..23).firstOrNull{now.points[it]*sign>previous.points[it]*sign}?:if(now.off(now.turn)>previous.off(now.turn))Move.OFF else null
   if(from!=null&&to!=null){motion=CheckerMotion(from,to,now.turn==Player.WHITE);travel.snapTo(0f);travel.animateTo(1f,tween(560,easing=FastOutSlowInEasing));motion=null}
  }
  previous=now.copyDeep()
 }
 Canvas(modifier.aspectRatio(1.72f).pointerInput(state,selected){detectTapGestures{tap->
  val rail=size.width*.055f;val barW=size.width*.075f;val playLeft=rail;val playRight=size.width-rail*1.8f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half
  val bar=tap.x in (barLeft-barW*.28f)..(barLeft+barW*1.28f);val off=tap.x>playRight;val local=if(tap.x<barLeft)tap.x-playLeft else tap.x-(barLeft+barW)+half
  val col=(local/(half/6)).toInt().coerceIn(0,11);val point=if(tap.y<size.height/2)12+col else 11-col;val target=when{off->Move.OFF;bar->Move.BAR;else->point};val choices=legal.filter{it.from==selected&&it.to==target};if(choices.isNotEmpty())move(choices.maxBy{it.die})else {val owns=target==Move.BAR&&state.position.bar(state.position.turn)>0||target in 0..23&&state.position.points[target]*state.position.turn.sign>0;if(owns)select(target)}
 }}){
  val rail=size.width*.055f;val barW=size.width*.075f;val playLeft=rail;val playRight=size.width-rail*1.8f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half;val cw=half/6
  drawRoundRect(materialBrush,cornerRadius=CornerRadius(18f));drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.18f),p.frame.copy(.16f),p.frameDark.copy(.30f))),cornerRadius=CornerRadius(18f));drawRoundRect(Color.Black.copy(.38f),Offset(rail*.42f,rail*.32f),Size(size.width-rail*1.18f,size.height-rail*.64f),CornerRadius(12f));drawRect(materialBrush,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(p.field.copy(if(theme==BoardTheme.SMOKED_GLASS).55f else .34f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.92f),p.frame.copy(.68f),p.frameDark.copy(.92f))),Offset(barLeft,rail),Size(barW,size.height-rail*2));drawRect(Brush.horizontalGradient(listOf(p.frameDark,p.frame.copy(.72f),p.frameDark)),Offset(playRight,rail),Size(size.width-playRight-rail*.25f,size.height-rail*2))
  // Layered bevels and deterministic grain give the frame and playing bed physical depth.
  drawRoundRect(Color.White.copy(.16f),Offset(3f,3f),Size(size.width-6f,size.height-6f),CornerRadius(17f),style=Stroke(3f));drawRoundRect(Color.Black.copy(.38f),Offset(rail*.28f,rail*.28f),Size(size.width-rail*.85f,size.height-rail*.56f),CornerRadius(10f),style=Stroke(5f))
  when(theme){
   BoardTheme.PREMIUM_WOOD->for(i in 0..22){val y=rail+(size.height-rail*2)*i/22f;val grain=Path().apply{moveTo(playLeft,y);cubicTo(size.width*.28f,y+sin(i*1.7f)*6f,size.width*.62f,y+cos(i*1.3f)*5f,playRight,y)};drawPath(grain,Color(0xff4b2415).copy(if(i%4==0).15f else .065f),style=Stroke(if(i%4==0)1.6f else .8f))}
   BoardTheme.MARBLE_STONE->for(i in 0..9){val x=playLeft+(playRight-playLeft)*i/9f;val vein=Path().apply{moveTo(x,rail);cubicTo(x+sin(i*2f)*38f,size.height*.30f,x+cos(i*1.3f)*55f,size.height*.68f,x+sin(i*.7f)*30f,size.height-rail)};drawPath(vein,Color.White.copy(if(i%3==0).24f else .11f),style=Stroke(if(i%3==0)2.8f else 1.2f));drawPath(vein,Color(0xff505860).copy(.08f),style=Stroke(4.5f))}
   BoardTheme.SMOKED_GLASS->{drawRect(Brush.linearGradient(listOf(Color.White.copy(.16f),Color.Transparent,Color(0xff6de7ef).copy(.10f))),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));for(i in 0..7)drawLine(Color.White.copy(.07f),Offset(playLeft+i*90f,rail),Offset(playLeft+i*90f+180f,size.height-rail),1.4f)}
  }
  for(i in 0..7){val y=(i+.5f)*size.height/8f;drawLine(Color.Black.copy(.10f),Offset(3f,y),Offset(rail*.9f,y+sin(i.toFloat())*6f),1.2f);drawLine(Color.White.copy(.07f),Offset(playRight,y),Offset(size.width-4f,y+cos(i.toFloat())*5f),1f)}
  for(c in 0..11){val x=if(c<6)playLeft+c*cw else barLeft+barW+(c-6)*cw;val color=if(c%2==0)p.pointA else p.pointB;val top=Path().apply{moveTo(x+2,rail);lineTo(x+cw-2,rail);lineTo(x+cw/2,size.height*.43f);close()};val bottom=Path().apply{moveTo(x+2,size.height-rail);lineTo(x+cw-2,size.height-rail);lineTo(x+cw/2,size.height*.57f);close()};drawPath(top,color);drawPath(bottom,if(c%2==0)p.pointB else p.pointA)}
  fun center(point:Int,index:Int):Offset{val col=if(point<12)11-point else point-12;val x=if(col<6)playLeft+(col+.5f)*cw else barLeft+barW+(col-5.5f)*cw;val r=cw*.39f;val gap=min(r*1.62f,(size.height*.37f)/5);return Offset(x,if(point>=12)rail+r+index.coerceAtMost(4)*gap else size.height-rail-r-index.coerceAtMost(4)*gap)}
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false){val base=when(theme){BoardTheme.PREMIUM_WOOD->when(pieces){PieceStyle.IVORY->if(white)p.light else p.dark;PieceStyle.MARBLE->if(white)Color(0xffeee5d5)else Color(0xff4a2d29);PieceStyle.NEON->if(white)Color(0xffd7b86a)else Color(0xff51212a)};BoardTheme.MARBLE_STONE->if(white)Color(0xfff3f1e9)else Color(0xff35414b);BoardTheme.SMOKED_GLASS->if(white)Color(0xff9debf0)else Color(0xff123d4c)};drawCircle(Color.Black.copy(.38f),r,Offset(c.x,c.y+r*.14f));drawCircle(Brush.radialGradient(listOf(Color.White.copy(.45f),base,base.copy(.8f)),c-Offset(r*.25f,r*.3f),r*1.4f),r,c);drawCircle(if(on)p.accent else Color.White.copy(.48f),r,c,style=Stroke(if(on)4f else 2.4f));drawCircle(Color.Black.copy(.30f),r*.82f,c,style=Stroke(r*.09f));drawCircle(Color.White.copy(.20f),r*.66f,c,style=Stroke(2f));drawCircle(Brush.radialGradient(listOf(Color.Black.copy(.24f),base.copy(.08f),Color.White.copy(.10f)),c,r*.48f),r*.48f,c);drawCircle(Color.Black.copy(.20f),r*.50f,c,style=Stroke(r*.055f));drawCircle(Color.White.copy(.32f),r*.13f,Offset(c.x-r*.28f,c.y-r*.30f))}
  val active=motion
  for(pt in 0..23){val n=abs(state.position.points[pt]);for(i in 0 until min(n,5)){if(active?.to==pt&&i==min(n,5)-1)continue;piece(center(pt,i),cw*.39f,state.position.points[pt]>0,selected==pt&&i==min(n,5)-1)};if(n>5)drawCircle(p.accent,cw*.18f,center(pt,4))}
  if(state.position.barWhite>0&&active?.to!=Move.BAR)piece(Offset(barLeft+barW/2,size.height*.62f),cw*.39f,true,selected==Move.BAR);if(state.position.barBlack>0&&active?.to!=Move.BAR)piece(Offset(barLeft+barW/2,size.height*.38f),cw*.39f,false,selected==Move.BAR)
  active?.let{m->
   fun endpoint(point:Int,start:Boolean):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,if(m.white)size.height*.62f else size.height*.38f);Move.OFF->Offset(playRight+(size.width-playRight)/2,if(m.white)size.height*.68f else size.height*.32f);else->{val count=abs(state.position.points[point]);center(point,if(start)count else (count-1).coerceAtLeast(0))}}
   val a=endpoint(m.from,true);val b=endpoint(m.to,false);val q=travel.value*travel.value*(3f-2f*travel.value);val ground=Offset(a.x+(b.x-a.x)*q,a.y+(b.y-a.y)*q);val flying=Offset(ground.x,ground.y-sin(q*PI).toFloat()*size.height*.10f);drawOval(Color.Black.copy(.22f*(1f-sin(q*PI).toFloat()*.55f)),Offset(ground.x-cw*.32f,ground.y+cw*.24f),Size(cw*.64f,cw*.22f));piece(flying,cw*.41f,m.white,true)
  }
  legal.filter{it.from==selected}.forEach{m->val c=if(m.to==Move.OFF)Offset(playRight+(size.width-playRight)/2,size.height/2)else center(m.to,abs(state.position.points[m.to]).coerceAtMost(4));val arrow=Path().apply{moveTo(c.x,c.y-cw*.32f);lineTo(c.x+cw*.30f,c.y+cw*.25f);lineTo(c.x+cw*.10f,c.y+cw*.19f);lineTo(c.x,c.y+cw*.38f);lineTo(c.x-cw*.10f,c.y+cw*.19f);lineTo(c.x-cw*.30f,c.y+cw*.25f);close()};drawPath(arrow,Color(0xffffd43b).copy(.68f+.28f*pulse));drawPath(arrow,Color(0xff4a2b00),style=Stroke(2.2f))}
  drawLine(Color.White.copy(.2f),Offset(rail,size.height/2),Offset(playRight,size.height/2),2f)
 }
}
