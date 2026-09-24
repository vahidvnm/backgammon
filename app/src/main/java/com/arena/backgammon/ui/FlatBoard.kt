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

private data class CheckerMotion(val from:Int,val to:Int,val white:Boolean,val fromIndex:Int=0,val toIndex:Int=0)
data class BoardPalette(val frame:Color,val frameDark:Color,val field:Color,val pointA:Color,val pointB:Color,val light:Color,val dark:Color,val accent:Color)
fun boardPalette(t:BoardTheme)=when(t){
 BoardTheme.PREMIUM_WOOD->BoardPalette(Color(0xff75402b),Color(0xff2f160d),Color(0xffc4936d),Color(0xffd8bd8d),Color(0xff6d2730),Color(0xffffdfa0),Color(0xff351a15),Color(0xffd7b25b))
 BoardTheme.MARBLE_STONE->BoardPalette(Color(0xff8c9298),Color(0xff30363c),Color(0xffc8c9c5),Color(0xfff0eee7),Color(0xff596773),Color(0xfff5f3ea),Color(0xff263039),Color(0xffb9c5cc))
 BoardTheme.SMOKED_GLASS->BoardPalette(Color(0xff334650),Color(0xff0b151b),Color(0xff183541),Color(0xff9ed4db),Color(0xff245f70),Color(0xffd5f5f4),Color(0xff0b2029),Color(0xff7de3e8))
}

/** Precise top-down board inspired by classic mobile layouts, drawn entirely with original vector assets. */
@Composable fun FlatBoard(state:TurnState,theme:BoardTheme,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,combine:(Int,Int)->Boolean={_,_->false},modifier:Modifier=Modifier,scene:TableScene=TableScene.DARK_GRASS,guidance:GuidanceMode=GuidanceMode.SIMPLE){
 val p=boardPalette(theme);val texture=ImageBitmap.imageResource(when(theme){BoardTheme.PREMIUM_WOOD->R.drawable.premium_walnut_texture;BoardTheme.MARBLE_STONE->R.drawable.ivory_marble_texture;BoardTheme.SMOKED_GLASS->R.drawable.smoked_glass_texture});val materialBrush=remember(texture){ShaderBrush(ImageShader(texture,TileMode.Mirror,TileMode.Mirror))};val pulse by rememberInfiniteTransition(label="legal").animateFloat(.58f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse")
 val plans=if(selected==null)emptyList() else GameEngine.movePlans(state.position,state.dice,selected)
 val visiblePlans=plans
 val displayPlans=visiblePlans.groupBy{it.to}.values.mapNotNull{routes->routes.maxByOrNull{it.moves.size}}
 var previous by remember{mutableStateOf(state.position.copyDeep())};var motion by remember{mutableStateOf<CheckerMotion?>(null)};val travel=remember{Animatable(1f)}
 LaunchedEffect(state.position.points.contentHashCode(),state.position.barWhite,state.position.barBlack,state.position.offWhite,state.position.offBlack){
  val now=state.position;val mover=now.turn;val sign=mover.sign
  if(!previous.points.contentEquals(now.points)||previous.bar(mover)!=now.bar(mover)||previous.off(mover)!=now.off(mover)){
   val from=(0..23).firstOrNull{previous.points[it]*sign>now.points[it]*sign}?:if(previous.bar(mover)>now.bar(mover))Move.BAR else null
   val to=(0..23).firstOrNull{now.points[it]*sign>previous.points[it]*sign}?:if(now.off(mover)>previous.off(mover))Move.OFF else null
   if(from!=null&&to!=null){val fromIndex=if(from in 0..23)(abs(previous.points[from])-1).coerceAtLeast(0)else 0;val toIndex=if(to in 0..23)(abs(now.points[to])-1).coerceAtLeast(0)else 0;motion=CheckerMotion(from,to,mover==Player.WHITE,fromIndex,toIndex);previous=now.copyDeep();travel.snapTo(0f);travel.animateTo(1f,tween(1080,easing=FastOutSlowInEasing));motion=null}else previous=now.copyDeep()
  }else previous=now.copyDeep()
 }
 Canvas(modifier.pointerInput(state,selected){detectTapGestures{tap->
  val rail=size.width*.032f;val barW=size.width*.062f;val playLeft=rail;val playRight=size.width-size.width*.060f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half
  val bar=tap.x in (barLeft-barW*.28f)..(barLeft+barW*1.28f);val off=tap.x>playRight;val local=if(tap.x<barLeft)tap.x-playLeft else tap.x-(barLeft+barW)+half
  val col=(local/(half/6)).toInt().coerceIn(0,11);val point=if(tap.y<size.height/2)12+col else 11-col;val target=when{off->Move.OFF;bar->Move.BAR;else->point};val choices=legal.filter{it.from==selected&&it.to==target};val compound=plans.any{it.to==target&&it.moves.size>1};if(selected!=null&&target!=Move.BAR&&compound&&combine(selected,target))Unit else if(choices.isNotEmpty())move(choices.maxBy{it.die})else if(selected!=null&&target!=Move.BAR&&combine(selected,target))Unit else {val owns=target==Move.BAR&&state.position.bar(state.position.turn)>0||target in 0..23&&state.position.points[target]*state.position.turn.sign>0;if(owns)select(target)}
 }}){
  val rail=size.width*.032f;val barW=size.width*.062f;val playLeft=rail;val playRight=size.width-size.width*.060f;val half=(playRight-playLeft-barW)/2;val barLeft=playLeft+half;val cw=half/6
  // The board is constructed as wood surfaces and vertical walls, rather than dark outline bands.
  drawRoundRect(materialBrush,cornerRadius=CornerRadius(13f));drawRoundRect(p.frame.copy(.50f),cornerRadius=CornerRadius(13f))
  drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.24f),Color.Transparent,p.frameDark.copy(.22f))),cornerRadius=CornerRadius(13f),style=Stroke(4f))
  val wall=rail*.42f
  // A continuous dark vertical wall surrounds the carved playing bed.
  drawRect(Brush.verticalGradient(listOf(p.frameDark.copy(.82f),p.frameDark.copy(.48f))),Offset(playLeft-wall,rail-wall),Size(playRight-playLeft+wall*2,size.height-rail*2+wall*2))
  drawRect(materialBrush,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(p.field.copy(if(theme==BoardTheme.SMOKED_GLASS).62f else .32f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(Color(0xffffead0).copy(if(theme==BoardTheme.PREMIUM_WOOD).055f else .025f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2))
  // Short directional contact shadows reveal the vertical wall without painting a broad brown band.
  val lip=16f
  // Visible sloped walls make the bed read as wood removed from a solid slab.
  drawRect(Brush.verticalGradient(listOf(p.frameDark.copy(.78f),p.frame.copy(.34f),Color.Transparent)),Offset(playLeft,rail),Size(playRight-playLeft,lip))
  drawRect(Brush.verticalGradient(listOf(Color.Transparent,p.frame.copy(.24f),p.frameDark.copy(.72f))),Offset(playLeft,size.height-rail-lip),Size(playRight-playLeft,lip))
  drawRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.72f),p.frame.copy(.26f),Color.Transparent)),Offset(playLeft,rail),Size(lip,size.height-rail*2))
  drawRect(Brush.horizontalGradient(listOf(Color.Transparent,p.frame.copy(.20f),p.frameDark.copy(.68f))),Offset(playRight-lip,rail),Size(lip,size.height-rail*2))
  drawLine(Color.White.copy(.30f),Offset(playLeft+3f,rail+3f),Offset(playRight-3f,rail+3f),2f)
  drawLine(Color.White.copy(.20f),Offset(playLeft+3f,rail+3f),Offset(playLeft+3f,size.height-rail-3f),2f)
  drawLine(Color.Black.copy(.30f),Offset(playLeft+4f,size.height-rail-3f),Offset(playRight-4f,size.height-rail-3f),2f)
  // A full-height timber beam physically joins the upper and lower rails.
  drawRect(Color.Black.copy(.34f),Offset(barLeft+barW+2f,0f),Size(7f,size.height))
  drawRect(materialBrush,Offset(barLeft,0f),Size(barW,size.height))
  drawRect(Brush.horizontalGradient(listOf(p.frame.copy(.86f),p.frame.copy(.50f),p.frameDark.copy(.62f))),Offset(barLeft,0f),Size(barW,size.height))
  if(theme==BoardTheme.PREMIUM_WOOD)for(g in 1..5){val gx=barLeft+barW*g/6f;val grain=Path().apply{moveTo(gx,0f);cubicTo(gx+sin(g*1.3f)*3f,size.height*.30f,gx+cos(g*1.7f)*3f,size.height*.70f,gx,size.height)};drawPath(grain,p.frameDark.copy(.15f),style=Stroke(1f))}
  drawRect(Brush.horizontalGradient(listOf(Color.White.copy(.26f),Color.Transparent)),Offset(barLeft,0f),Size(5f,size.height))
  drawRect(Brush.horizontalGradient(listOf(Color.Transparent,p.frameDark.copy(.64f))),Offset(barLeft+barW-8f,0f),Size(8f,size.height))
  drawLine(Color.White.copy(.20f),Offset(barLeft+3f,2f),Offset(barLeft+barW-5f,2f),2f)
  drawLine(p.frameDark.copy(.62f),Offset(barLeft+3f,size.height-3f),Offset(barLeft+barW-4f,size.height-3f),3f)
  // Routed inner panel, side walls and ornaments turn the centre bar into carved joinery.
  drawRoundRect(Color.Black.copy(.28f),Offset(barLeft+barW*.18f,rail*.72f),Size(barW*.67f,size.height-rail*1.44f),CornerRadius(barW*.19f),style=Stroke(4f))
  drawRoundRect(Color.White.copy(.12f),Offset(barLeft+barW*.22f,rail*.82f),Size(barW*.56f,size.height-rail*1.64f),CornerRadius(barW*.16f),style=Stroke(1.4f))
  fun DrawScope.barDiamond(y:Float){val cx=barLeft+barW/2;val r=barW*.13f;val d=Path().apply{moveTo(cx,y-r);lineTo(cx+r,y);lineTo(cx,y+r);lineTo(cx-r,y);close()};drawPath(d,Color.Black.copy(.30f));drawPath(d,p.accent.copy(.32f),style=Stroke(1.5f))}
  barDiamond(size.height*.14f);barDiamond(size.height*.86f)
  // Two small recessed dice-rest pockets decorate the important central bridge.
  fun DrawScope.diceBay(y:Float,alternate:Boolean){val side=barW*.48f;val x=barLeft+(barW-side)/2;drawRoundRect(Color.Black.copy(.35f),Offset(x+2f,y-side/2+3f),Size(side,side),CornerRadius(side*.20f));drawRoundRect(Brush.radialGradient(listOf(p.frame.copy(.20f),p.frameDark.copy(.42f))),Offset(x,y-side/2),Size(side,side),CornerRadius(side*.20f));drawRoundRect(Color.White.copy(.13f),Offset(x+2f,y-side/2+2f),Size(side-4f,side-4f),CornerRadius(side*.17f),style=Stroke(1.2f));val pip=side*.055f;val cx=x+side/2;if(alternate){drawCircle(p.frameDark.copy(.62f),pip,Offset(cx-side*.19f,y-side*.19f));drawCircle(p.frameDark.copy(.62f),pip,Offset(cx+side*.19f,y+side*.19f))}else{drawCircle(p.frameDark.copy(.62f),pip,Offset(cx,y))}}
  diceBay(size.height*.455f,false);diceBay(size.height*.545f,true)
  // Bear-off rail is another wood member, not a translucent overlay.
  drawRect(materialBrush,Offset(playRight,0f),Size(size.width-playRight,size.height));drawRect(p.frame.copy(.46f),Offset(playRight,0f),Size(size.width-playRight,size.height))
  drawRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.52f),Color.Transparent)),Offset(playRight,0f),Size(7f,size.height))
  // Layered bevels and deterministic grain give the frame and playing bed physical depth.
  drawLine(Color.White.copy(.18f),Offset(10f,5f),Offset(size.width-10f,5f),2f);drawLine(p.frameDark.copy(.38f),Offset(10f,size.height-5f),Offset(size.width-10f,size.height-5f),3f)
  // Material grain follows the construction direction of each physical timber member.
  when(theme){
   BoardTheme.PREMIUM_WOOD->{
    for(i in 0..20){val y=rail+(size.height-rail*2)*i/20f;val alpha=if(i%4==0).14f else .055f;val leftGrain=Path().apply{moveTo(playLeft,y);cubicTo(playLeft+half*.30f,y+sin(i*1.7f)*4f,playLeft+half*.72f,y+cos(i*1.3f)*4f,barLeft,y)};val rightGrain=Path().apply{moveTo(barLeft+barW,y);cubicTo(barLeft+barW+half*.30f,y+cos(i*1.5f)*4f,barLeft+barW+half*.70f,y+sin(i*1.2f)*4f,playRight,y)};drawPath(leftGrain,p.frameDark.copy(alpha),style=Stroke(if(i%4==0)1.4f else .7f));drawPath(rightGrain,p.frameDark.copy(alpha),style=Stroke(if(i%4==0)1.4f else .7f))}
    for(i in 1..5){val y=rail*i/6f;drawLine(p.frameDark.copy(.13f),Offset(10f,y),Offset(size.width-10f,y+sin(i.toFloat())*2f),1f);drawLine(Color.White.copy(.055f),Offset(10f,size.height-y),Offset(size.width-10f,size.height-y+cos(i.toFloat())*2f),1f)}
    for(i in 1..4){val x=playLeft*i/5f;val side=Path().apply{moveTo(x,8f);cubicTo(x+2f,size.height*.3f,x-2f,size.height*.7f,x,size.height-8f)};drawPath(side,p.frameDark.copy(.11f),style=Stroke(1f))}
    for(i in 1..4){val x=playRight+(size.width-playRight)*i/5f;drawLine(p.frameDark.copy(.10f),Offset(x,7f),Offset(x+sin(i.toFloat())*2f,size.height-7f),1f)}
   }
   BoardTheme.MARBLE_STONE->{for(i in 0..8){val x=playLeft+(playRight-playLeft)*i/8f;if(x !in barLeft..(barLeft+barW)){val vein=Path().apply{moveTo(x,rail);cubicTo(x+sin(i*2f)*32f,size.height*.30f,x+cos(i*1.3f)*46f,size.height*.68f,x+sin(i*.7f)*25f,size.height-rail)};drawPath(vein,Color.White.copy(if(i%3==0).22f else .09f),style=Stroke(if(i%3==0)2.3f else 1f));drawPath(vein,Color(0xff505860).copy(.07f),style=Stroke(4f))}}}
   BoardTheme.SMOKED_GLASS->{val sheen=Brush.linearGradient(listOf(Color.White.copy(.14f),Color.Transparent,Color(0xff6de7ef).copy(.09f)));drawRect(sheen,Offset(playLeft,rail),Size(half,size.height-rail*2));drawRect(sheen,Offset(barLeft+barW,rail),Size(half,size.height-rail*2));for(i in 0..5){drawLine(Color.White.copy(.055f),Offset(playLeft+i*70f,rail),Offset(playLeft+i*70f+140f,size.height-rail),1.2f);drawLine(Color.White.copy(.055f),Offset(barLeft+barW+i*70f,rail),Offset(barLeft+barW+i*70f+140f,size.height-rail),1.2f)}}
  }
  // Fine routed ornament inspired by traditional luxury boards, kept inside the timber rails.
  if(theme==BoardTheme.PREMIUM_WOOD){
   val ornamentDark=p.frameDark.copy(.38f);val ornamentLight=p.accent.copy(.34f);val oy=rail*.43f;val step=(playRight-playLeft)/18f
   drawLine(ornamentDark,Offset(playLeft+8f,oy+1.5f),Offset(playRight-8f,oy+1.5f),2.2f);drawLine(ornamentLight,Offset(playLeft+8f,oy),Offset(playRight-8f,oy),1f)
   drawLine(ornamentDark,Offset(playLeft+8f,size.height-oy+1.5f),Offset(playRight-8f,size.height-oy+1.5f),2.2f);drawLine(ornamentLight,Offset(playLeft+8f,size.height-oy),Offset(playRight-8f,size.height-oy),1f)
   fun railDiamond(x:Float,y:Float):Path{val r=min(rail*.12f,6f);return Path().apply{moveTo(x,y-r);lineTo(x+r,y);lineTo(x,y+r);lineTo(x-r,y);close()}}
   for(i in 1..17){val x=playLeft+i*step;drawPath(railDiamond(x,oy),ornamentDark);drawPath(railDiamond(x,oy),ornamentLight,style=Stroke(1f));drawPath(railDiamond(x,size.height-oy),ornamentDark);drawPath(railDiamond(x,size.height-oy),ornamentLight,style=Stroke(1f))}
   fun cornerFlourish(cx:Float,cy:Float,sx:Float,sy:Float){val a=rail*.42f;val curl=Path().apply{moveTo(cx,cy+sy*a);cubicTo(cx+sx*a*.15f,cy+sy*a*.25f,cx+sx*a*.70f,cy+sy*a*.78f,cx+sx*a,cy);cubicTo(cx+sx*a*.70f,cy-sy*a*.20f,cx+sx*a*.42f,cy+sy*a*.04f,cx+sx*a*.30f,cy+sy*a*.22f)};drawPath(curl,ornamentDark,style=Stroke(3f));drawPath(curl,ornamentLight,style=Stroke(1.2f))}
   cornerFlourish(playLeft+5f,oy,1f,1f);cornerFlourish(playRight-5f,oy,-1f,1f);cornerFlourish(playLeft+5f,size.height-oy,1f,-1f);cornerFlourish(playRight-5f,size.height-oy,-1f,-1f)
  }
  drawLine(Color.White.copy(.15f),Offset(5f,8f),Offset(5f,size.height-10f),2f);drawLine(p.frameDark.copy(.42f),Offset(size.width-6f,10f),Offset(size.width-6f,size.height-10f),3f)
  for(i in 0..7){val y=(i+.5f)*size.height/8f;drawLine(Color.Black.copy(.10f),Offset(3f,y),Offset(rail*.9f,y+sin(i.toFloat())*6f),1.2f);drawLine(Color.White.copy(.07f),Offset(playRight,y),Offset(size.width-4f,y+cos(i.toFloat())*5f),1f)}
  val trayX=playRight+size.width*.006f;val trayW=size.width-playRight-size.width*.012f;val trayH=size.height*.40f
  fun DrawScope.bearOffWell(y:Float){
   val wx=trayX+trayW*.08f;val ww=trayW*.84f
   // Raised wooden rim, sloped inner wall, and a recessed timber floor.
   drawRoundRect(Color.Black.copy(.55f),Offset(wx+4f,y+7f),Size(ww,trayH),CornerRadius(9f))
   drawRoundRect(materialBrush,Offset(wx,y),Size(ww,trayH),CornerRadius(9f));drawRoundRect(p.frame.copy(.52f),Offset(wx,y),Size(ww,trayH),CornerRadius(9f));drawRoundRect(p.frameDark.copy(.48f),Offset(wx+1f,y+1f),Size(ww-2f,trayH-2f),CornerRadius(8f),style=Stroke(2f))
   drawRoundRect(Brush.horizontalGradient(listOf(p.frameDark.copy(.96f),p.frame.copy(.28f),p.frameDark.copy(.96f))),Offset(wx+3f,y+3f),Size(ww-6f,trayH-6f),CornerRadius(7f))
   drawRoundRect(materialBrush,Offset(wx+13f,y+13f),Size(ww-26f,trayH-26f),CornerRadius(4f));drawRoundRect(p.frameDark.copy(.31f),Offset(wx+13f,y+13f),Size(ww-26f,trayH-26f),CornerRadius(4f))
   drawLine(Color.White.copy(.30f),Offset(wx+4f,y+4f),Offset(wx+ww-4f,y+4f),1.7f);drawLine(Color.Black.copy(.42f),Offset(wx+4f,y+trayH-4f),Offset(wx+ww-4f,y+trayH-4f),2.4f)
   drawLine(Color.Black.copy(.42f),Offset(wx+12f,y+14f),Offset(wx+12f,y+trayH-14f),2f);drawLine(Color.White.copy(.14f),Offset(wx+14f,y+14f),Offset(wx+14f,y+trayH-14f),1f)
   val step=(trayH-20f)/15f;for(i in 1..14){val sy=y+10f+i*step;drawLine(Color.Black.copy(.20f),Offset(wx+14f,sy),Offset(wx+ww-14f,sy),1.3f);drawLine(Color.White.copy(.07f),Offset(wx+15f,sy+1.2f),Offset(wx+ww-15f,sy+1.2f),.8f)}
  }
  val topWellY=rail*1.55f;val bottomWellY=size.height-rail*1.55f-trayH;bearOffWell(topWellY);bearOffWell(bottomWellY)
  // A solid timber bridge separates black and white storage wells.
  val bridgeTop=topWellY+trayH+5f;val bridgeBottom=bottomWellY-5f;drawRect(materialBrush,Offset(trayX,bridgeTop),Size(trayW,bridgeBottom-bridgeTop));drawRect(p.frame.copy(.58f),Offset(trayX,bridgeTop),Size(trayW,bridgeBottom-bridgeTop));drawLine(Color.White.copy(.20f),Offset(trayX+3f,bridgeTop+2f),Offset(trayX+trayW-3f,bridgeTop+2f),1.5f);drawLine(p.frameDark.copy(.45f),Offset(trayX+3f,bridgeBottom-2f),Offset(trayX+trayW-3f,bridgeBottom-2f),2f)
  drawLine(Color.White.copy(.12f),Offset(playLeft+2f,rail+2f),Offset(playRight-2f,rail+2f),1.5f)
  drawLine(Color.White.copy(.12f),Offset(barLeft+4f,rail),Offset(barLeft+4f,size.height-rail),1.5f)
  // The board picks up a restrained cast from the selected room instead of looking pasted over it.
  val ambient=when(scene){TableScene.DARK_GRASS->Color(0xff6e9270);TableScene.STARRY_SKY->Color(0xff6f9ed2);TableScene.AUTUMN_SUNSET->Color(0xffffb268);TableScene.PERSIAN_RUG->Color(0xffd69a78);TableScene.DARK_RIVER->Color(0xff60a59c)}
  drawRect(Brush.linearGradient(listOf(ambient.copy(.075f),Color.Transparent,ambient.copy(.035f)),Offset(playLeft,rail),Offset(playRight,size.height-rail)),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2),blendMode=BlendMode.Softlight)
  for(c in 0..11){val x=if(c<6)playLeft+c*cw else barLeft+barW+(c-6)*cw;val color=if(c%2==0)p.pointA else p.pointB;val other=if(c%2==0)p.pointB else p.pointA;val top=Path().apply{moveTo(x+cw*.10f,rail);lineTo(x+cw*.90f,rail);lineTo(x+cw/2,size.height*.405f);close()};val bottom=Path().apply{moveTo(x+cw*.10f,size.height-rail);lineTo(x+cw*.90f,size.height-rail);lineTo(x+cw/2,size.height*.595f);close()};drawPath(top,Brush.verticalGradient(listOf(color.copy(.72f),color,color.copy(.76f)),rail,size.height*.43f));drawPath(bottom,Brush.verticalGradient(listOf(other.copy(.76f),other,other.copy(.72f)),size.height*.57f,size.height-rail));drawPath(top,Color.Black.copy(.18f),style=Stroke(1.3f));drawPath(bottom,Color.Black.copy(.18f),style=Stroke(1.3f));drawLine(Color.White.copy(.13f),Offset(x+cw*.10f,rail+1f),Offset(x+cw/2,size.height*.405f),1f);drawLine(Color.Black.copy(.13f),Offset(x+cw*.90f,rail+1f),Offset(x+cw/2,size.height*.405f),1f);drawLine(Color.White.copy(.10f),Offset(x+cw*.10f,size.height-rail-1f),Offset(x+cw/2,size.height*.595f),1f);drawLine(Color.Black.copy(.14f),Offset(x+cw*.90f,size.height-rail-1f),Offset(x+cw/2,size.height*.595f),1f)}
  fun center(point:Int,index:Int):Offset{val col=if(point<12)11-point else point-12;val x=if(col<6)playLeft+(col+.5f)*cw else barLeft+barW+(col-5.5f)*cw;val r=cw*.262f;val gap=min(r*1.72f,(size.height*.37f)/5);return Offset(x,if(point>=12)rail+r+index.coerceAtMost(4)*gap else size.height-rail-r-index.coerceAtMost(4)*gap)}
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false,available:Boolean=false,variation:Float=0f){
   val materialBase=when(theme){BoardTheme.PREMIUM_WOOD->if(white)Color(0xffe2c58d)else Color(0xff603326);BoardTheme.MARBLE_STONE->if(white)Color(0xffe8e8e2)else Color(0xff34434d);BoardTheme.SMOKED_GLASS->if(white)Color(0xffb5edf0)else Color(0xff123b52)}
   val styleTint=when(pieces){PieceStyle.IVORY->materialBase;PieceStyle.MARBLE->if(white)Color(0xffe3e2dc)else Color(0xff3c4650);PieceStyle.NEON->if(white)Color(0xffbceff0)else Color(0xff174760)}
   // Deterministic ±3% material variation prevents cloned checkers without changing team colour.
   val base=if(variation>=0)lerp(styleTint,Color.White,variation*.03f)else lerp(styleTint,Color.Black,-variation*.03f)
   val edge=if(white)lerp(base,Color(0xff80572f),.43f)else lerp(base,Color.Black,.58f);val topC=c-Offset(0f,r*.055f)
   // Layered ambient/contact shadows are softer than a single black outline.
   drawOval(Color.Black.copy(.13f),Offset(c.x-r*1.02f,c.y-r*.63f),Size(r*2.04f,r*1.80f));drawOval(Color.Black.copy(.24f),Offset(c.x-r*.91f,c.y-r*.55f),Size(r*1.82f,r*1.63f))
   drawCircle(edge,r,c+Offset(0f,r*.055f));drawCircle(Brush.verticalGradient(listOf(base,edge.copy(.88f))),r*.96f,c)
   drawCircle(Brush.radialGradient(listOf(Color.White.copy(if(white).48f else .25f),base,edge),topC-Offset(r*.25f,r*.28f),r*1.18f),r*.90f,topC)
   // One rim and one concave centre: no stacked-disc or attached-bubble appearance.
   drawCircle(Color.White.copy(.30f),r*.86f,topC,style=Stroke(r*.038f));drawCircle(Color.Black.copy(.24f),r*.69f,topC,style=Stroke(r*.042f))
   drawCircle(Brush.radialGradient(listOf(base.copy(.96f),base.copy(.83f),edge.copy(.74f)),topC-Offset(r*.10f,r*.10f),r*.55f),r*.47f,topC)
   drawArc(Color.Black.copy(.30f),188f,164f,false,Offset(topC.x-r*.47f,topC.y-r*.47f),Size(r*.94f,r*.94f),style=Stroke(r*.045f));drawArc(Color.White.copy(.15f),8f,164f,false,Offset(topC.x-r*.44f,topC.y-r*.44f),Size(r*.88f,r*.88f),style=Stroke(r*.028f))
   // A single subtle grain/vein per checker keeps the material natural and uncluttered.
   val grainY=topC.y+r*variation*.12f;drawLine((if(white)Color(0xff76502d)else Color(0xffc08358)).copy(.10f),Offset(c.x-r*.48f,grainY),Offset(c.x+r*.46f,grainY+r*.035f*variation),r*.022f)
   drawOval(Color.White.copy(.31f),Offset(topC.x-r*.39f,topC.y-r*.36f),Size(r*.35f,r*.13f))
   if(available&&!on){drawCircle(Brush.radialGradient(listOf(Color(0xffbaff88).copy(.30f+.18f*pulse),Color(0xff55d66b).copy(.18f),Color.Transparent),topC,r*.92f),r*.82f,topC);drawCircle(Color(0xff7ee779).copy(.62f+.28f*pulse),r*.78f,topC,style=Stroke(r*.055f));drawArc(Color.White.copy(.36f),205f,125f,false,Offset(topC.x-r*.70f,topC.y-r*.70f),Size(r*1.40f,r*1.40f),style=Stroke(r*.032f))};if(on){drawCircle(Color.Black.copy(.30f),r*1.10f,c,style=Stroke(5f));drawCircle(p.accent,r*1.08f,c,style=Stroke(3f))}
  }
  val active=motion;val drawn=if(active==null)previous else state.position
  for(pt in 0..23){val n=abs(drawn.points[pt]);for(i in 0 until min(n,5)){if(active?.to==pt&&i==min(n,5)-1)continue;piece(center(pt,i),cw*.262f,drawn.points[pt]>0,selected==pt&&i==min(n,5)-1,legal.any{it.from==pt}&&i==min(n,5)-1,(((pt*7+i*3)%7)-3)/3f)};if(n>5)drawCircle(p.accent,cw*.18f,center(pt,4))}
  if(active?.to!=Move.BAR){val br=cw*.238f;for(i in 0 until min(drawn.barWhite,8))piece(Offset(barLeft+barW/2,size.height*.57f+i*br*.48f),br,true,selected==Move.BAR&&i==min(drawn.barWhite,8)-1,legal.any{it.from==Move.BAR}&&i==min(drawn.barWhite,8)-1,((i*5)%7-3)/3f);for(i in 0 until min(drawn.barBlack,8))piece(Offset(barLeft+barW/2,size.height*.43f-i*br*.48f),br,false,selected==Move.BAR&&i==min(drawn.barBlack,8)-1,legal.any{it.from==Move.BAR}&&i==min(drawn.barBlack,8)-1,((i*4+2)%7-3)/3f)}
  fun DrawScope.offChip(y:Float,white:Boolean){val chipW=trayW*.68f;val chipH=((trayH-16f)/15f)*.66f;val base=if(white)Color(0xffe2c58d)else Color(0xff603326);drawRoundRect(Color.Black.copy(.38f),Offset(trayX+(trayW-chipW)/2+1f,y+2f),Size(chipW,chipH),CornerRadius(chipH/2));drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.42f),base,base.copy(.72f))),Offset(trayX+(trayW-chipW)/2,y),Size(chipW,chipH),CornerRadius(chipH/2));drawLine(Color.White.copy(.30f),Offset(trayX+(trayW-chipW)/2+3f,y+2f),Offset(trayX+(trayW+chipW)/2-3f,y+2f),1f)};val chipStep=(trayH-16f)/15f;for(i in 0 until drawn.offBlack)offChip(topWellY+8f+i*chipStep,false);for(i in 0 until drawn.offWhite)offChip(bottomWellY+trayH-8f-(i+1)*chipStep,true)
  active?.let{m->
   fun endpoint(point:Int,start:Boolean):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,if(m.white)size.height*.62f else size.height*.38f);Move.OFF->Offset(playRight+(size.width-playRight)/2,if(m.white)size.height*.68f else size.height*.32f);else->center(point,if(start)m.fromIndex else m.toIndex)}
   val a=endpoint(m.from,true);val b=endpoint(m.to,false);val q=travel.value*travel.value*(3f-2f*travel.value);val moving=Offset(a.x+(b.x-a.x)*q,a.y+(b.y-a.y)*q);val lift=sin(Math.PI.toFloat()*q);drawOval(Color.Black.copy(.25f-.11f*lift),Offset(moving.x-cw*(.25f+.04f*lift),moving.y+cw*(.19f+.07f*lift)),Size(cw*(.50f+.08f*lift),cw*(.15f+.03f*lift)));piece(moving,cw*(.272f+.014f*lift),m.white,true)
  }
  // Precision landing reticles reveal direct and compound destinations without fake checkers.
  displayPlans.forEach{plan->
   fun marker(point:Int):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,size.height/2);Move.OFF->Offset(playRight+(size.width-playRight)/2,size.height/2);else->center(point,abs(state.position.points[point]).coerceAtMost(4))}
   val routePoints=mutableListOf(marker(plan.from)).apply{plan.moves.forEach{add(marker(it.to))}}
   if(guidance==GuidanceMode.COACH)for(i in 0 until routePoints.lastIndex)drawLine(Color(0xffffdf68).copy(.34f+.20f*pulse),routePoints[i],routePoints[i+1],3f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(10f,7f)))
   val c=routePoints.last();val direction=if(c.y<size.height/2)1f else -1f;val slide=((pulse-.58f)/.42f).coerceIn(0f,1f)*cw*.11f;val tipY=c.y-direction*(cw*.30f-slide);val tailY=tipY-direction*cw*.31f;val guide=p.accent.copy(.70f+.25f*pulse)
   // A gently sliding inlaid arrow invites the move without resembling another checker.
   drawLine(Color.Black.copy(.28f),Offset(c.x+1.5f,tailY+2f),Offset(c.x+1.5f,tipY-direction*cw*.06f+2f),5.5f)
   drawLine(guide,Offset(c.x,tailY),Offset(c.x,tipY-direction*cw*.06f),3.2f)
   val arrow=Path().apply{moveTo(c.x,tipY);lineTo(c.x-cw*.105f,tipY-direction*cw*.14f);lineTo(c.x+cw*.105f,tipY-direction*cw*.14f);close()};drawPath(arrow,Color.Black.copy(.30f),style=Stroke(4.5f));drawPath(arrow,guide)
   repeat(plan.moves.size.coerceAtMost(4)){i->val x=c.x+(i-(plan.moves.size.coerceAtMost(4)-1)/2f)*cw*.065f;drawCircle(Color(0xffffe29a).copy(.88f),cw*.018f,Offset(x,tailY-direction*cw*.045f))}
  }
 }
}
