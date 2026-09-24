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
 BoardTheme.PREMIUM_WOOD->BoardPalette(Color(0xff75402b),Color(0xff2f160d),Color(0xffbd8a61),Color(0xffd8bd8d),Color(0xff6d2730),Color(0xffffdfa0),Color(0xff351a15),Color(0xffd7b25b))
 BoardTheme.MARBLE_STONE->BoardPalette(Color(0xff8c9298),Color(0xff30363c),Color(0xffc8c9c5),Color(0xfff0eee7),Color(0xff596773),Color(0xfff5f3ea),Color(0xff263039),Color(0xffb9c5cc))
 BoardTheme.SMOKED_GLASS->BoardPalette(Color(0xff334650),Color(0xff0b151b),Color(0xff183541),Color(0xff9ed4db),Color(0xff245f70),Color(0xffd5f5f4),Color(0xff0b2029),Color(0xff7de3e8))
}

/** Precise top-down board inspired by classic mobile layouts, drawn entirely with original vector assets. */
@Composable fun FlatBoard(state:TurnState,theme:BoardTheme,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,combine:(Int,Int)->Boolean={_,_->false},modifier:Modifier=Modifier){
 val p=boardPalette(theme);val texture=ImageBitmap.imageResource(when(theme){BoardTheme.PREMIUM_WOOD->R.drawable.premium_walnut_texture;BoardTheme.MARBLE_STONE->R.drawable.ivory_marble_texture;BoardTheme.SMOKED_GLASS->R.drawable.smoked_glass_texture});val materialBrush=remember(texture){ShaderBrush(ImageShader(texture,TileMode.Mirror,TileMode.Mirror))};val pulse by rememberInfiniteTransition(label="legal").animateFloat(.58f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse")
 var previous by remember{mutableStateOf(state.position.copyDeep())};var motion by remember{mutableStateOf<CheckerMotion?>(null)};val travel=remember{Animatable(1f)}
 LaunchedEffect(state.position){
  val now=state.position;val mover=previous.turn;val sign=mover.sign
  if(!previous.points.contentEquals(now.points)||previous.bar(mover)!=now.bar(mover)||previous.off(mover)!=now.off(mover)){
   val from=(0..23).firstOrNull{previous.points[it]*sign>now.points[it]*sign}?:if(previous.bar(mover)>now.bar(mover))Move.BAR else null
   val to=(0..23).firstOrNull{now.points[it]*sign>previous.points[it]*sign}?:if(now.off(mover)>previous.off(mover))Move.OFF else null
   if(from!=null&&to!=null){motion=CheckerMotion(from,to,mover==Player.WHITE);travel.snapTo(0f);travel.animateTo(1f,tween(1080,easing=FastOutSlowInEasing));motion=null}
  }
  previous=now.copyDeep()
 }
 Canvas(modifier.pointerInput(state,selected){detectTapGestures{tap->
  val rail=size.width*.032f;val barW=size.width*.083f;val playLeft=rail;val playRight=size.width-size.width*.060f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half
  val bar=tap.x in (barLeft-barW*.28f)..(barLeft+barW*1.28f);val off=tap.x>playRight;val local=if(tap.x<barLeft)tap.x-playLeft else tap.x-(barLeft+barW)+half
  val col=(local/(half/6)).toInt().coerceIn(0,11);val point=if(tap.y<size.height/2)12+col else 11-col;val target=when{off->Move.OFF;bar->Move.BAR;else->point};val choices=legal.filter{it.from==selected&&it.to==target};if(choices.isNotEmpty())move(choices.maxBy{it.die})else if(selected!=null&&target!=Move.BAR&&combine(selected,target))Unit else {val owns=target==Move.BAR&&state.position.bar(state.position.turn)>0||target in 0..23&&state.position.points[target]*state.position.turn.sign>0;if(owns)select(target)}
 }}){
  val rail=size.width*.032f;val barW=size.width*.083f;val playLeft=rail;val playRight=size.width-size.width*.060f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half;val cw=half/6
  // The board is constructed as wood surfaces and vertical walls, rather than dark outline bands.
  drawRoundRect(materialBrush,cornerRadius=CornerRadius(13f));drawRoundRect(p.frame.copy(.42f),cornerRadius=CornerRadius(13f))
  drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.24f),Color.Transparent,p.frameDark.copy(.22f))),cornerRadius=CornerRadius(13f),style=Stroke(4f))
  val wall=rail*.42f
  // A continuous dark vertical wall surrounds the carved playing bed.
  drawRect(Brush.verticalGradient(listOf(p.frameDark.copy(.82f),p.frameDark.copy(.48f))),Offset(playLeft-wall,rail-wall),Size(playRight-playLeft+wall*2,size.height-rail*2+wall*2))
  drawRect(materialBrush,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(p.field.copy(if(theme==BoardTheme.SMOKED_GLASS).55f else .14f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2))
  // Short directional contact shadows reveal the vertical wall without painting a broad brown band.
  val lip=9f
  drawRect(Brush.verticalGradient(listOf(Color.Black.copy(.34f),Color.Transparent)),Offset(playLeft,rail),Size(playRight-playLeft,lip))
  drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(.30f))),Offset(playLeft,size.height-rail-lip),Size(playRight-playLeft,lip))
  drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(.30f),Color.Transparent)),Offset(playLeft,rail),Size(lip,size.height-rail*2))
  drawRect(Brush.horizontalGradient(listOf(Color.Transparent,Color.Black.copy(.25f))),Offset(playRight-lip,rail),Size(lip,size.height-rail*2))
  drawLine(Color.White.copy(.22f),Offset(playLeft+2f,rail+2f),Offset(playRight-2f,rail+2f),2f)
  drawLine(Color.White.copy(.13f),Offset(playLeft+2f,rail+2f),Offset(playLeft+2f,size.height-rail-2f),1.5f)
  // A full-height timber beam physically joins the upper and lower rails.
  drawRect(Color.Black.copy(.34f),Offset(barLeft+barW+2f,0f),Size(7f,size.height))
  drawRect(materialBrush,Offset(barLeft,0f),Size(barW,size.height))
  drawRect(Brush.horizontalGradient(listOf(p.frame.copy(.86f),p.frame.copy(.50f),p.frameDark.copy(.62f))),Offset(barLeft,0f),Size(barW,size.height))
  if(theme==BoardTheme.PREMIUM_WOOD)for(g in 1..5){val gx=barLeft+barW*g/6f;val grain=Path().apply{moveTo(gx,0f);cubicTo(gx+sin(g*1.3f)*3f,size.height*.30f,gx+cos(g*1.7f)*3f,size.height*.70f,gx,size.height)};drawPath(grain,p.frameDark.copy(.15f),style=Stroke(1f))}
  drawRect(Brush.horizontalGradient(listOf(Color.White.copy(.26f),Color.Transparent)),Offset(barLeft,0f),Size(5f,size.height))
  drawRect(Brush.horizontalGradient(listOf(Color.Transparent,p.frameDark.copy(.64f))),Offset(barLeft+barW-8f,0f),Size(8f,size.height))
  drawLine(Color.White.copy(.20f),Offset(barLeft+3f,2f),Offset(barLeft+barW-5f,2f),2f)
  drawLine(p.frameDark.copy(.62f),Offset(barLeft+3f,size.height-3f),Offset(barLeft+barW-4f,size.height-3f),3f)
  // Bear-off rail is another wood member, not a translucent overlay.
  drawRect(materialBrush,Offset(playRight,0f),Size(size.width-playRight,size.height));drawRect(p.frame.copy(.46f),Offset(playRight,0f),Size(size.width-playRight,size.height))
  drawRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.52f),Color.Transparent)),Offset(playRight,0f),Size(7f,size.height))
  // Layered bevels and deterministic grain give the frame and playing bed physical depth.
  drawLine(Color.White.copy(.18f),Offset(10f,5f),Offset(size.width-10f,5f),2f);drawLine(p.frameDark.copy(.38f),Offset(10f,size.height-5f),Offset(size.width-10f,size.height-5f),3f)
  when(theme){
   BoardTheme.PREMIUM_WOOD->for(i in 0..22){val y=rail+(size.height-rail*2)*i/22f;val grain=Path().apply{moveTo(playLeft,y);cubicTo(size.width*.28f,y+sin(i*1.7f)*6f,size.width*.62f,y+cos(i*1.3f)*5f,playRight,y)};drawPath(grain,Color(0xff4b2415).copy(if(i%4==0).15f else .065f),style=Stroke(if(i%4==0)1.6f else .8f))}
   BoardTheme.MARBLE_STONE->for(i in 0..9){val x=playLeft+(playRight-playLeft)*i/9f;val vein=Path().apply{moveTo(x,rail);cubicTo(x+sin(i*2f)*38f,size.height*.30f,x+cos(i*1.3f)*55f,size.height*.68f,x+sin(i*.7f)*30f,size.height-rail)};drawPath(vein,Color.White.copy(if(i%3==0).24f else .11f),style=Stroke(if(i%3==0)2.8f else 1.2f));drawPath(vein,Color(0xff505860).copy(.08f),style=Stroke(4.5f))}
   BoardTheme.SMOKED_GLASS->{drawRect(Brush.linearGradient(listOf(Color.White.copy(.16f),Color.Transparent,Color(0xff6de7ef).copy(.10f))),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));for(i in 0..7)drawLine(Color.White.copy(.07f),Offset(playLeft+i*90f,rail),Offset(playLeft+i*90f+180f,size.height-rail),1.4f)}
  }
  for(i in 0..7){val y=(i+.5f)*size.height/8f;drawLine(Color.Black.copy(.10f),Offset(3f,y),Offset(rail*.9f,y+sin(i.toFloat())*6f),1.2f);drawLine(Color.White.copy(.07f),Offset(playRight,y),Offset(size.width-4f,y+cos(i.toFloat())*5f),1f)}
  val trayX=playRight+size.width*.006f;val trayW=size.width-playRight-size.width*.012f;val trayH=size.height*.40f
  fun DrawScope.bearOffWell(y:Float){
   val wx=trayX+trayW*.08f;val ww=trayW*.84f
   // Raised wooden rim, sloped inner wall, and a recessed timber floor.
   drawRoundRect(Color.Black.copy(.30f),Offset(wx+2f,y+3f),Size(ww,trayH),CornerRadius(8f))
   drawRoundRect(materialBrush,Offset(wx,y),Size(ww,trayH),CornerRadius(8f));drawRoundRect(p.frame.copy(.48f),Offset(wx,y),Size(ww,trayH),CornerRadius(8f))
   drawRoundRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.88f),p.frame.copy(.38f),p.frameDark.copy(.88f))),Offset(wx+4f,y+4f),Size(ww-8f,trayH-8f),CornerRadius(6f))
   drawRoundRect(materialBrush,Offset(wx+8f,y+8f),Size(ww-16f,trayH-16f),CornerRadius(4f));drawRoundRect(p.frameDark.copy(.42f),Offset(wx+8f,y+8f),Size(ww-16f,trayH-16f),CornerRadius(4f))
   drawLine(Color.White.copy(.24f),Offset(wx+5f,y+5f),Offset(wx+ww-5f,y+5f),1.5f);drawLine(Color.Black.copy(.30f),Offset(wx+5f,y+trayH-5f),Offset(wx+ww-5f,y+trayH-5f),2f)
   val step=(trayH-16f)/15f;for(i in 1..14){val sy=y+8f+i*step;drawLine(Color.Black.copy(.11f),Offset(wx+11f,sy),Offset(wx+ww-11f,sy),1f)}
  }
  val topWellY=rail*1.55f;val bottomWellY=size.height-rail*1.55f-trayH;bearOffWell(topWellY);bearOffWell(bottomWellY)
  // A solid timber bridge separates black and white storage wells.
  val bridgeTop=topWellY+trayH+5f;val bridgeBottom=bottomWellY-5f;drawRect(materialBrush,Offset(trayX,bridgeTop),Size(trayW,bridgeBottom-bridgeTop));drawRect(p.frame.copy(.58f),Offset(trayX,bridgeTop),Size(trayW,bridgeBottom-bridgeTop));drawLine(Color.White.copy(.20f),Offset(trayX+3f,bridgeTop+2f),Offset(trayX+trayW-3f,bridgeTop+2f),1.5f);drawLine(p.frameDark.copy(.45f),Offset(trayX+3f,bridgeBottom-2f),Offset(trayX+trayW-3f,bridgeBottom-2f),2f)
  drawLine(Color.White.copy(.12f),Offset(playLeft+2f,rail+2f),Offset(playRight-2f,rail+2f),1.5f)
  drawLine(Color.White.copy(.12f),Offset(barLeft+4f,rail),Offset(barLeft+4f,size.height-rail),1.5f)
  for(c in 0..11){val x=if(c<6)playLeft+c*cw else barLeft+barW+(c-6)*cw;val color=if(c%2==0)p.pointA else p.pointB;val other=if(c%2==0)p.pointB else p.pointA;val top=Path().apply{moveTo(x+cw*.10f,rail);lineTo(x+cw*.90f,rail);lineTo(x+cw/2,size.height*.405f);close()};val bottom=Path().apply{moveTo(x+cw*.10f,size.height-rail);lineTo(x+cw*.90f,size.height-rail);lineTo(x+cw/2,size.height*.595f);close()};drawPath(top,Brush.verticalGradient(listOf(color.copy(.72f),color,color.copy(.76f)),rail,size.height*.43f));drawPath(bottom,Brush.verticalGradient(listOf(other.copy(.76f),other,other.copy(.72f)),size.height*.57f,size.height-rail));drawPath(top,Color.Black.copy(.20f),style=Stroke(1.5f));drawPath(bottom,Color.Black.copy(.20f),style=Stroke(1.5f))}
  fun center(point:Int,index:Int):Offset{val col=if(point<12)11-point else point-12;val x=if(col<6)playLeft+(col+.5f)*cw else barLeft+barW+(col-5.5f)*cw;val r=cw*.262f;val gap=min(r*1.72f,(size.height*.37f)/5);return Offset(x,if(point>=12)rail+r+index.coerceAtMost(4)*gap else size.height-rail-r-index.coerceAtMost(4)*gap)}
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false,available:Boolean=false){
   val base=when(theme){BoardTheme.PREMIUM_WOOD->if(white)Color(0xffe2c58d)else Color(0xff603326);BoardTheme.MARBLE_STONE->if(white)Color(0xffe8e8e2)else Color(0xff34434d);BoardTheme.SMOKED_GLASS->if(white)Color(0xffb5edf0)else Color(0xff123b52)}
   val edge=if(white)base.copy(.78f)else Color(0xff170d0c)
   val topC=c-Offset(0f,r*.07f)
   drawOval(Color.Black.copy(.40f),Offset(c.x-r*.93f,c.y-r*.63f),Size(r*1.86f,r*1.78f))
   drawCircle(edge,r,c+Offset(0f,r*.05f));drawCircle(Brush.verticalGradient(listOf(base.copy(.95f),edge.copy(.82f))),r*.96f,c)
   drawCircle(edge,r*.93f,topC)
   if(theme==BoardTheme.PREMIUM_WOOD){drawCircle(materialBrush,r*.92f,topC);drawCircle(base.copy(.44f),r*.92f,topC)}
   drawCircle(Brush.radialGradient(listOf(Color.White.copy(if(white).58f else .34f),base,edge),topC-Offset(r*.27f,r*.28f),r*1.22f),r*.89f,topC)
   drawCircle(Color.White.copy(.40f),r*.87f,topC,style=Stroke(r*.050f));drawCircle(Color.Black.copy(.28f),r*.74f,topC,style=Stroke(r*.055f));drawCircle(Color.White.copy(.16f),r*.65f,topC,style=Stroke(r*.028f));val hollow=topC;drawCircle(Brush.radialGradient(listOf(base.copy(.94f),base.copy(.82f),Color.Black.copy(.40f)),hollow-Offset(r*.10f,r*.10f),r*.54f),r*.47f,hollow);drawArc(Color.Black.copy(.38f),190f,160f,false,Offset(hollow.x-r*.48f,hollow.y-r*.48f),Size(r*.96f,r*.96f),style=Stroke(r*.055f));drawArc(Color.White.copy(.18f),10f,160f,false,Offset(hollow.x-r*.45f,hollow.y-r*.45f),Size(r*.90f,r*.90f),style=Stroke(r*.035f));for(g in -2..2){val yy=c.y-r*.10f+g*r*.16f;val span=sqrt(max(0f,r*r*.48f-(yy-(c.y-r*.10f)).pow(2)));drawLine((if(white)Color(0xff80582f)else Color(0xffc08255)).copy(.12f),Offset(c.x-span,yy),Offset(c.x+span,yy+sin(g*1.8f)*r*.05f),r*.025f)}
   drawOval(Color.White.copy(.42f),Offset(topC.x-r*.44f,topC.y-r*.39f),Size(r*.44f,r*.18f))
   if(available&&!on){drawCircle(Color(0xffb7f45c).copy(.30f+.22f*pulse),r*1.15f,c);drawCircle(Color(0xff9ee84e),r*1.12f,c,style=Stroke(4f))};if(on){drawCircle(Color.Black.copy(.42f),r*1.13f,c,style=Stroke(7f));drawCircle(p.accent,r*1.13f,c,style=Stroke(4f))}
  }
  val active=motion
  for(pt in 0..23){val n=abs(state.position.points[pt]);for(i in 0 until min(n,5)){if(active?.to==pt&&i==min(n,5)-1)continue;piece(center(pt,i),cw*.262f,state.position.points[pt]>0,selected==pt&&i==min(n,5)-1,legal.any{it.from==pt}&&i==min(n,5)-1)};if(n>5)drawCircle(p.accent,cw*.18f,center(pt,4))}
  if(active?.to!=Move.BAR){val br=cw*.238f;for(i in 0 until min(state.position.barWhite,8))piece(Offset(barLeft+barW/2,size.height*.57f+i*br*.48f),br,true,selected==Move.BAR&&i==min(state.position.barWhite,8)-1,legal.any{it.from==Move.BAR}&&i==min(state.position.barWhite,8)-1);for(i in 0 until min(state.position.barBlack,8))piece(Offset(barLeft+barW/2,size.height*.43f-i*br*.48f),br,false,selected==Move.BAR&&i==min(state.position.barBlack,8)-1,legal.any{it.from==Move.BAR}&&i==min(state.position.barBlack,8)-1)}
  fun DrawScope.offChip(y:Float,white:Boolean){val chipW=trayW*.68f;val chipH=((trayH-16f)/15f)*.66f;val base=if(white)Color(0xffe2c58d)else Color(0xff603326);drawRoundRect(Color.Black.copy(.38f),Offset(trayX+(trayW-chipW)/2+1f,y+2f),Size(chipW,chipH),CornerRadius(chipH/2));drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.42f),base,base.copy(.72f))),Offset(trayX+(trayW-chipW)/2,y),Size(chipW,chipH),CornerRadius(chipH/2));drawLine(Color.White.copy(.30f),Offset(trayX+(trayW-chipW)/2+3f,y+2f),Offset(trayX+(trayW+chipW)/2-3f,y+2f),1f)};val chipStep=(trayH-16f)/15f;for(i in 0 until state.position.offBlack)offChip(topWellY+8f+i*chipStep,false);for(i in 0 until state.position.offWhite)offChip(bottomWellY+trayH-8f-(i+1)*chipStep,true)
  active?.let{m->
   fun endpoint(point:Int,start:Boolean):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,if(m.white)size.height*.62f else size.height*.38f);Move.OFF->Offset(playRight+(size.width-playRight)/2,if(m.white)size.height*.68f else size.height*.32f);else->{val count=if(start)abs(previous.points[point])else abs(state.position.points[point]);center(point,(count-1).coerceAtLeast(0))}}
   val a=endpoint(m.from,true);val b=endpoint(m.to,false);val q=travel.value*travel.value*(3f-2f*travel.value);val moving=Offset(a.x+(b.x-a.x)*q,a.y+(b.y-a.y)*q);val lift=sin(Math.PI.toFloat()*q);drawOval(Color.Black.copy(.25f-.11f*lift),Offset(moving.x-cw*(.25f+.04f*lift),moving.y+cw*(.19f+.07f*lift)),Size(cw*(.50f+.08f*lift),cw*(.15f+.03f*lift)));piece(moving,cw*(.272f+.014f*lift),m.white,true)
  }
  legal.filter{it.from==selected}.forEach{m->val c=if(m.to==Move.OFF)Offset(playRight+(size.width-playRight)/2,size.height/2)else center(m.to,abs(state.position.points[m.to]).coerceAtMost(4));val arrow=Path().apply{moveTo(c.x,c.y-cw*.32f);lineTo(c.x+cw*.30f,c.y+cw*.25f);lineTo(c.x+cw*.10f,c.y+cw*.19f);lineTo(c.x,c.y+cw*.38f);lineTo(c.x-cw*.10f,c.y+cw*.19f);lineTo(c.x-cw*.30f,c.y+cw*.25f);close()};drawPath(arrow,Color(0xffffd43b).copy(.68f+.28f*pulse));drawPath(arrow,Color(0xff4a2b00),style=Stroke(2.2f))}
  drawLine(Color.White.copy(.2f),Offset(rail,size.height/2),Offset(playRight,size.height/2),2f)
 }
}
