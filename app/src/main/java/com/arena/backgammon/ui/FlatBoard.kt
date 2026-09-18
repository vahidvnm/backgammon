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
import androidx.compose.ui.input.pointer.pointerInput
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import kotlin.math.*

data class BoardPalette(val frame:Color,val frameDark:Color,val field:Color,val pointA:Color,val pointB:Color,val light:Color,val dark:Color,val accent:Color)
fun boardPalette(t:BoardTheme)=when(t){
 BoardTheme.CLASSIC_WOOD->BoardPalette(Color(0xff875332),Color(0xff3d2115),Color(0xff426f61),Color(0xffe4dfc5),Color(0xff253b50),Color(0xffffd27f),Color(0xff51291d),Color(0xffffd35c))
 BoardTheme.ROYAL->BoardPalette(Color(0xff5b2826),Color(0xff241010),Color(0xff7a3d3d),Color(0xffd6b66a),Color(0xff30151d),Color(0xffffe2a3),Color(0xff431018),Color(0xffffcf59))
 BoardTheme.EMERALD->BoardPalette(Color(0xff4e3323),Color(0xff1c100b),Color(0xff17604b),Color(0xffd5bd78),Color(0xff0a352d),Color(0xffffe7ae),Color(0xff164337),Color(0xffffd866))
 BoardTheme.OCEAN->BoardPalette(Color(0xff684832),Color(0xff2d1d15),Color(0xff236786),Color(0xffb9e0e8),Color(0xff17364f),Color(0xffedfaff),Color(0xff173148),Color(0xff60dbff))
 BoardTheme.ROYAL_PURPLE->BoardPalette(Color(0xff3d243f),Color(0xff180d1d),Color(0xff5c3576),Color(0xffdfbd70),Color(0xff281338),Color(0xffffe8bd),Color(0xff39194c),Color(0xffffd05e))
 BoardTheme.BLACK_LUXURY->BoardPalette(Color(0xff25282c),Color(0xff08090a),Color(0xff33383d),Color(0xffc7a95d),Color(0xff101317),Color(0xffe8dfca),Color(0xff090b0e),Color(0xffffcf56))
 BoardTheme.MAHOGANY->BoardPalette(Color(0xff793926),Color(0xff32140d),Color(0xffb87c63),Color(0xfff1d5a4),Color(0xff692e28),Color(0xffffe0a3),Color(0xff5a211b),Color(0xffffc56a))
 BoardTheme.ARCTIC->BoardPalette(Color(0xff607d91),Color(0xff293b49),Color(0xffd8e7eb),Color(0xffffffff),Color(0xff4a7695),Color(0xfff7fcff),Color(0xff24485e),Color(0xff67dfff))
 BoardTheme.DESERT->BoardPalette(Color(0xffa36d3e),Color(0xff52351e),Color(0xffd6ad73),Color(0xffffe2a8),Color(0xff8b4c35),Color(0xffffe0a0),Color(0xff633524),Color(0xffffcf67))
 BoardTheme.JADE->BoardPalette(Color(0xff315f54),Color(0xff122b25),Color(0xff8db8a5),Color(0xffe6d9ae),Color(0xff246758),Color(0xffffecc0),Color(0xff17483f),Color(0xffffd466)) }

/** Precise top-down board inspired by classic mobile layouts, drawn entirely with original vector assets. */
@Composable fun FlatBoard(state:TurnState,theme:BoardTheme,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,modifier:Modifier=Modifier){
 val p=boardPalette(theme);val pulse by rememberInfiniteTransition(label="legal").animateFloat(.58f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse")
 Canvas(modifier.aspectRatio(1.72f).pointerInput(state,selected){detectTapGestures{tap->
  val rail=size.width*.055f;val barW=size.width*.075f;val playLeft=rail;val playRight=size.width-rail*1.8f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half
  val bar=tap.x in barLeft..barLeft+barW;val off=tap.x>playRight;val local=if(tap.x<barLeft)tap.x-playLeft else tap.x-(barLeft+barW)+half
  val col=(local/(half/6)).toInt().coerceIn(0,11);val point=if(tap.y<size.height/2)12+col else 11-col;val target=when{off->Move.OFF;bar->Move.BAR;else->point};val choices=legal.filter{it.from==selected&&it.to==target};if(choices.isNotEmpty())move(choices.maxBy{it.die})else {val owns=target==Move.BAR&&state.position.bar(state.position.turn)>0||target in 0..23&&state.position.points[target]*state.position.turn.sign>0;if(owns)select(target)}
 }}){
  val rail=size.width*.055f;val barW=size.width*.075f;val playLeft=rail;val playRight=size.width-rail*1.8f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half;val cw=half/6
  drawRoundRect(Brush.verticalGradient(listOf(p.frame,p.frameDark)),cornerRadius=CornerRadius(18f));drawRoundRect(Color.Black.copy(.3f),Offset(rail*.45f,rail*.35f),Size(size.width-rail*1.25f,size.height-rail*.7f),CornerRadius(12f));drawRect(p.field,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(Brush.horizontalGradient(listOf(p.frameDark,p.frame,p.frameDark)),Offset(barLeft,rail),Size(barW,size.height-rail*2));drawRect(p.frameDark,Offset(playRight,rail),Size(size.width-playRight-rail*.25f,size.height-rail*2))
  for(c in 0..11){val x=if(c<6)playLeft+c*cw else barLeft+barW+(c-6)*cw;val color=if(c%2==0)p.pointA else p.pointB;val top=Path().apply{moveTo(x+2,rail);lineTo(x+cw-2,rail);lineTo(x+cw/2,size.height*.43f);close()};val bottom=Path().apply{moveTo(x+2,size.height-rail);lineTo(x+cw-2,size.height-rail);lineTo(x+cw/2,size.height*.57f);close()};drawPath(top,color);drawPath(bottom,if(c%2==0)p.pointB else p.pointA)}
  fun center(point:Int,index:Int):Offset{val col=if(point<12)11-point else point-12;val x=if(col<6)playLeft+(col+.5f)*cw else barLeft+barW+(col-5.5f)*cw;val r=cw*.39f;val gap=min(r*1.62f,(size.height*.37f)/5);return Offset(x,if(point>=12)rail+r+index.coerceAtMost(4)*gap else size.height-rail-r-index.coerceAtMost(4)*gap)}
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false){val base=when(pieces){PieceStyle.IVORY->if(white)p.light else p.dark;PieceStyle.MARBLE->if(white)Color(0xffe5edf1)else Color(0xff26394c);PieceStyle.NEON->if(white)Color(0xff66e5ff)else Color(0xffff496a)};drawCircle(Color.Black.copy(.38f),r,Offset(c.x,c.y+r*.14f));drawCircle(Brush.radialGradient(listOf(Color.White.copy(.45f),base,base.copy(.8f)),c-Offset(r*.25f,r*.3f),r*1.4f),r,c);drawCircle(if(on)p.accent else Color.White.copy(.35f),r,c,style=Stroke(if(on)4f else 2f));drawCircle(Color.Black.copy(.18f),r*.72f,c,style=Stroke(2f))}
  for(pt in 0..23){val n=abs(state.position.points[pt]);for(i in 0 until min(n,5))piece(center(pt,i),cw*.39f,state.position.points[pt]>0,selected==pt&&i==min(n,5)-1);if(n>5)drawCircle(p.accent,cw*.18f,center(pt,4))}
  if(state.position.barWhite>0)piece(Offset(barLeft+barW/2,size.height*.62f),cw*.39f,true,selected==Move.BAR);if(state.position.barBlack>0)piece(Offset(barLeft+barW/2,size.height*.38f),cw*.39f,false,selected==Move.BAR)
  legal.filter{it.from==selected}.forEach{m->val c=if(m.to==Move.OFF)Offset(playRight+(size.width-playRight)/2,size.height/2)else center(m.to,abs(state.position.points[m.to]).coerceAtMost(4));drawCircle(p.accent.copy(pulse*.35f),cw*.46f,c);drawCircle(p.accent,cw*.12f,c)}
  drawLine(Color.White.copy(.2f),Offset(rail,size.height/2),Offset(playRight,size.height/2),2f)
 }
}
