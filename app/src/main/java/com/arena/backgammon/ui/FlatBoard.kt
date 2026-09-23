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
   if(from!=null&&to!=null){motion=CheckerMotion(from,to,now.turn==Player.WHITE);travel.snapTo(0f);travel.animateTo(1f,tween(780,easing=FastOutSlowInEasing));motion=null}
  }
  previous=now.copyDeep()
 }
 Canvas(modifier.pointerInput(state,selected){detectTapGestures{tap->
  val rail=size.width*.045f;val barW=size.width*.072f;val playLeft=rail;val playRight=size.width-size.width*.095f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half
  val bar=tap.x in (barLeft-barW*.28f)..(barLeft+barW*1.28f);val off=tap.x>playRight;val local=if(tap.x<barLeft)tap.x-playLeft else tap.x-(barLeft+barW)+half
  val col=(local/(half/6)).toInt().coerceIn(0,11);val point=if(tap.y<size.height/2)12+col else 11-col;val target=when{off->Move.OFF;bar->Move.BAR;else->point};val choices=legal.filter{it.from==selected&&it.to==target};if(choices.isNotEmpty())move(choices.maxBy{it.die})else {val owns=target==Move.BAR&&state.position.bar(state.position.turn)>0||target in 0..23&&state.position.points[target]*state.position.turn.sign>0;if(owns)select(target)}
 }}){
  val rail=size.width*.045f;val barW=size.width*.072f;val playLeft=rail;val playRight=size.width-size.width*.095f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half;val cw=half/6
  drawRoundRect(materialBrush,cornerRadius=CornerRadius(18f));drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.18f),p.frame.copy(.16f),p.frameDark.copy(.30f))),cornerRadius=CornerRadius(18f));drawRoundRect(Color.Black.copy(.38f),Offset(rail*.42f,rail*.32f),Size(size.width-rail*1.18f,size.height-rail*.64f),CornerRadius(12f));drawRect(materialBrush,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(p.field.copy(if(theme==BoardTheme.SMOKED_GLASS).55f else .16f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.92f),p.frame.copy(.68f),p.frameDark.copy(.92f))),Offset(barLeft,rail),Size(barW,size.height-rail*2));drawRect(Brush.horizontalGradient(listOf(p.frameDark,p.frame.copy(.72f),p.frameDark)),Offset(playRight,rail),Size(size.width-playRight-rail*.25f,size.height-rail*2))
  // Layered bevels and deterministic grain give the frame and playing bed physical depth.
  drawRoundRect(Color.White.copy(.16f),Offset(3f,3f),Size(size.width-6f,size.height-6f),CornerRadius(17f),style=Stroke(3f));drawRoundRect(Color.Black.copy(.38f),Offset(rail*.28f,rail*.28f),Size(size.width-rail*.85f,size.height-rail*.56f),CornerRadius(10f),style=Stroke(5f))
  when(theme){
   BoardTheme.PREMIUM_WOOD->for(i in 0..22){val y=rail+(size.height-rail*2)*i/22f;val grain=Path().apply{moveTo(playLeft,y);cubicTo(size.width*.28f,y+sin(i*1.7f)*6f,size.width*.62f,y+cos(i*1.3f)*5f,playRight,y)};drawPath(grain,Color(0xff4b2415).copy(if(i%4==0).15f else .065f),style=Stroke(if(i%4==0)1.6f else .8f))}
   BoardTheme.MARBLE_STONE->for(i in 0..9){val x=playLeft+(playRight-playLeft)*i/9f;val vein=Path().apply{moveTo(x,rail);cubicTo(x+sin(i*2f)*38f,size.height*.30f,x+cos(i*1.3f)*55f,size.height*.68f,x+sin(i*.7f)*30f,size.height-rail)};drawPath(vein,Color.White.copy(if(i%3==0).24f else .11f),style=Stroke(if(i%3==0)2.8f else 1.2f));drawPath(vein,Color(0xff505860).copy(.08f),style=Stroke(4.5f))}
   BoardTheme.SMOKED_GLASS->{drawRect(Brush.linearGradient(listOf(Color.White.copy(.16f),Color.Transparent,Color(0xff6de7ef).copy(.10f))),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));for(i in 0..7)drawLine(Color.White.copy(.07f),Offset(playLeft+i*90f,rail),Offset(playLeft+i*90f+180f,size.height-rail),1.4f)}
  }
  for(i in 0..7){val y=(i+.5f)*size.height/8f;drawLine(Color.Black.copy(.10f),Offset(3f,y),Offset(rail*.9f,y+sin(i.toFloat())*6f),1.2f);drawLine(Color.White.copy(.07f),Offset(playRight,y),Offset(size.width-4f,y+cos(i.toFloat())*5f),1f)}
  val trayX=playRight+size.width*.018f;val trayW=size.width-playRight-size.width*.035f;val trayH=size.height*.40f
  drawRoundRect(Brush.horizontalGradient(listOf(Color.Black.copy(.82f),p.frameDark.copy(.76f),Color.Black.copy(.92f))),Offset(trayX,rail*2.2f),Size(trayW,trayH),CornerRadius(10f));drawRoundRect(Color.Black.copy(.65f),Offset(trayX,rail*2.2f),Size(trayW,trayH),CornerRadius(10f),style=Stroke(7f));drawRoundRect(Color.White.copy(.18f),Offset(trayX+4f,rail*2.2f+4f),Size(trayW-8f,trayH-8f),CornerRadius(8f),style=Stroke(2f));drawRoundRect(Brush.horizontalGradient(listOf(Color.Black.copy(.82f),p.frameDark.copy(.76f),Color.Black.copy(.92f))),Offset(trayX,size.height-rail*2.2f-trayH),Size(trayW,trayH),CornerRadius(10f));drawRoundRect(Color.Black.copy(.65f),Offset(trayX,size.height-rail*2.2f-trayH),Size(trayW,trayH),CornerRadius(10f),style=Stroke(7f));drawRoundRect(Color.White.copy(.18f),Offset(trayX+4f,size.height-rail*2.2f-trayH+4f),Size(trayW-8f,trayH-8f),CornerRadius(8f),style=Stroke(2f))
  drawLine(Color.Black.copy(.48f),Offset(playLeft,rail),Offset(playRight,rail),8f);drawLine(Color.White.copy(.18f),Offset(playLeft,rail+5f),Offset(playRight,rail+5f),2f);drawLine(Color.Black.copy(.42f),Offset(playLeft,size.height-rail),Offset(playRight,size.height-rail),8f);drawLine(Color.White.copy(.12f),Offset(playLeft,size.height-rail-5f),Offset(playRight,size.height-rail-5f),2f);drawLine(Color.Black.copy(.40f),Offset(playLeft,rail),Offset(playLeft,size.height-rail),7f);drawLine(Color.White.copy(.14f),Offset(playLeft+5f,rail),Offset(playLeft+5f,size.height-rail),2f)
  drawLine(Color.White.copy(.20f),Offset(barLeft+3f,rail),Offset(barLeft+3f,size.height-rail),3f);drawLine(Color.Black.copy(.52f),Offset(barLeft+barW-3f,rail),Offset(barLeft+barW-3f,size.height-rail),5f)
  for(c in 0..11){val x=if(c<6)playLeft+c*cw else barLeft+barW+(c-6)*cw;val color=if(c%2==0)p.pointA else p.pointB;val other=if(c%2==0)p.pointB else p.pointA;val top=Path().apply{moveTo(x+2,rail);lineTo(x+cw-2,rail);lineTo(x+cw/2,size.height*.43f);close()};val bottom=Path().apply{moveTo(x+2,size.height-rail);lineTo(x+cw-2,size.height-rail);lineTo(x+cw/2,size.height*.57f);close()};drawPath(top,Brush.verticalGradient(listOf(color.copy(.72f),color,color.copy(.76f)),rail,size.height*.43f));drawPath(bottom,Brush.verticalGradient(listOf(other.copy(.76f),other,other.copy(.72f)),size.height*.57f,size.height-rail));drawPath(top,Color.Black.copy(.20f),style=Stroke(1.5f));drawPath(bottom,Color.Black.copy(.20f),style=Stroke(1.5f))}
  fun center(point:Int,index:Int):Offset{val col=if(point<12)11-point else point-12;val x=if(col<6)playLeft+(col+.5f)*cw else barLeft+barW+(col-5.5f)*cw;val r=cw*.33f;val gap=min(r*1.72f,(size.height*.37f)/5);return Offset(x,if(point>=12)rail+r+index.coerceAtMost(4)*gap else size.height-rail-r-index.coerceAtMost(4)*gap)}
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false,available:Boolean=false){
   val base=when(theme){BoardTheme.PREMIUM_WOOD->if(white)Color(0xffefd598)else Color(0xff4a211d);BoardTheme.MARBLE_STONE->if(white)Color(0xffe8e8e2)else Color(0xff34434d);BoardTheme.SMOKED_GLASS->if(white)Color(0xffb5edf0)else Color(0xff123b52)}
   val edge=if(white)base.copy(.78f)else Color(0xff170d0c)
   drawOval(Color.Black.copy(.42f),Offset(c.x-r*.90f,c.y-r*.72f),Size(r*1.80f,r*1.72f))
   drawCircle(edge,r,c)
   if(theme==BoardTheme.PREMIUM_WOOD){drawCircle(materialBrush,r*.94f,c);drawCircle(base.copy(.38f),r*.94f,c)}
   drawCircle(Brush.radialGradient(listOf(Color.White.copy(if(white).62f else .38f),base,edge),c-Offset(r*.30f,r*.34f),r*1.30f),r*.91f,c)
   drawCircle(Color.White.copy(.40f),r*.88f,c,style=Stroke(r*.055f));drawCircle(Color.Black.copy(.38f),r*.73f,c,style=Stroke(r*.075f));drawCircle(Color.White.copy(.24f),r*.62f,c,style=Stroke(r*.045f));drawCircle(Color.Black.copy(.30f),r*.49f,c,style=Stroke(r*.060f));drawCircle(Color.White.copy(.18f),r*.38f,c,style=Stroke(r*.040f));drawCircle(Brush.radialGradient(listOf(Color.Black.copy(.22f),base.copy(.22f),Color.White.copy(.16f)),c,r*.31f),r*.29f,c)
   drawOval(Color.White.copy(.42f),Offset(c.x-r*.44f,c.y-r*.49f),Size(r*.44f,r*.18f))
   if(available&&!on){drawCircle(Color(0xffb7f45c).copy(.30f+.22f*pulse),r*1.15f,c);drawCircle(Color(0xff9ee84e),r*1.12f,c,style=Stroke(4f))};if(on){drawCircle(Color.Black.copy(.42f),r*1.13f,c,style=Stroke(7f));drawCircle(p.accent,r*1.13f,c,style=Stroke(4f))}
  }
  val active=motion
  for(pt in 0..23){val n=abs(state.position.points[pt]);for(i in 0 until min(n,5)){if(active?.to==pt&&i==min(n,5)-1)continue;piece(center(pt,i),cw*.33f,state.position.points[pt]>0,selected==pt&&i==min(n,5)-1,legal.any{it.from==pt}&&i==min(n,5)-1)};if(n>5)drawCircle(p.accent,cw*.18f,center(pt,4))}
  if(active?.to!=Move.BAR){val br=cw*.29f;for(i in 0 until min(state.position.barWhite,8))piece(Offset(barLeft+barW/2,size.height*.57f+i*br*.48f),br,true,selected==Move.BAR&&i==min(state.position.barWhite,8)-1,legal.any{it.from==Move.BAR}&&i==min(state.position.barWhite,8)-1);for(i in 0 until min(state.position.barBlack,8))piece(Offset(barLeft+barW/2,size.height*.43f-i*br*.48f),br,false,selected==Move.BAR&&i==min(state.position.barBlack,8)-1,legal.any{it.from==Move.BAR}&&i==min(state.position.barBlack,8)-1)}
  val offR=min(cw*.27f,trayW*.34f);for(i in 0 until min(state.position.offBlack,7))piece(Offset(trayX+trayW/2,rail*2.2f+offR*1.35f+i*offR*.82f),offR,false);for(i in 0 until min(state.position.offWhite,7))piece(Offset(trayX+trayW/2,size.height-rail*2.2f-offR*1.35f-i*offR*.82f),offR,true)
  active?.let{m->
   fun endpoint(point:Int,start:Boolean):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,if(m.white)size.height*.62f else size.height*.38f);Move.OFF->Offset(playRight+(size.width-playRight)/2,if(m.white)size.height*.68f else size.height*.32f);else->{val count=abs(state.position.points[point]);center(point,if(start)count else (count-1).coerceAtLeast(0))}}
   val a=endpoint(m.from,true);val b=endpoint(m.to,false);val q=travel.value*travel.value*(3f-2f*travel.value);val moving=Offset(a.x+(b.x-a.x)*q,a.y+(b.y-a.y)*q);drawOval(Color.Black.copy(.24f),Offset(moving.x-cw*.25f,moving.y+cw*.19f),Size(cw*.50f,cw*.15f));piece(moving,cw*.34f,m.white,true)
  }
  legal.filter{it.from==selected}.forEach{m->val c=if(m.to==Move.OFF)Offset(playRight+(size.width-playRight)/2,size.height/2)else center(m.to,abs(state.position.points[m.to]).coerceAtMost(4));val arrow=Path().apply{moveTo(c.x,c.y-cw*.32f);lineTo(c.x+cw*.30f,c.y+cw*.25f);lineTo(c.x+cw*.10f,c.y+cw*.19f);lineTo(c.x,c.y+cw*.38f);lineTo(c.x-cw*.10f,c.y+cw*.19f);lineTo(c.x-cw*.30f,c.y+cw*.25f);close()};drawPath(arrow,Color(0xffffd43b).copy(.68f+.28f*pulse));drawPath(arrow,Color(0xff4a2b00),style=Stroke(2.2f))}
  drawLine(Color.White.copy(.2f),Offset(rail,size.height/2),Offset(playRight,size.height/2),2f)
 }
}
