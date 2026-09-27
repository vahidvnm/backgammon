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
 BoardTheme.BRUSHED_METAL->BoardPalette(Color(0xff95603a),Color(0xff181718),Color(0xff8b735f),Color(0xffe0a14c),Color(0xff171616),Color(0xffffdc91),Color(0xff1a1818),Color(0xffd39845))
 BoardTheme.BURGUNDY_MARBLE->BoardPalette(Color(0xff722b35),Color(0xff1d1013),Color(0xff76232c),Color(0xffead8b2),Color(0xff211e20),Color(0xffffdda0),Color(0xff201114),Color(0xffbd9557))
 BoardTheme.JADE_MARBLE->BoardPalette(Color(0xff224d40),Color(0xff091311),Color(0xff296b56),Color(0xffeee9d5),Color(0xff12191a),Color(0xffe3f4e7),Color(0xff091312),Color(0xff9bc0aa))
 BoardTheme.OBSIDIAN_IVORY->BoardPalette(Color(0xff303335),Color(0xff0c0e10),Color(0xff303334),Color(0xffe1d6bd),Color(0xff16191b),Color(0xffffedc8),Color(0xff0a0c0d),Color(0xffb79b6d))
 BoardTheme.CRYSTAL_GLASS->BoardPalette(Color(0xffabcbd0),Color(0xff19272b),Color(0xff9aaeb2),Color(0xfff4f3ed),Color(0xff20272c),Color.White,Color(0xff0f171b),Color(0xffd8b978))
 BoardTheme.ROYAL_SAPPHIRE->BoardPalette(Color(0xff233b65),Color(0xff080f22),Color(0xff112b59),Color(0xffffe9c2),Color(0xff071b48),Color(0xffffdf92),Color(0xff071126),Color(0xffd5a84d))
}

/** Precise top-down board inspired by classic mobile layouts, drawn entirely with original vector assets. */
@Composable fun FlatBoard(state:TurnState,theme:BoardTheme,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,combine:(Int,Int)->Boolean={_,_->false},modifier:Modifier=Modifier,scene:TableScene=TableScene.DARK_GRASS,guidance:GuidanceMode=GuidanceMode.SIMPLE){
 val p=boardPalette(theme);val texture=ImageBitmap.imageResource(when(theme){BoardTheme.PREMIUM_WOOD->R.drawable.premium_walnut_texture;BoardTheme.BRUSHED_METAL->R.drawable.smoked_glass_texture;BoardTheme.BURGUNDY_MARBLE,BoardTheme.JADE_MARBLE,BoardTheme.OBSIDIAN_IVORY->R.drawable.ivory_marble_texture;BoardTheme.CRYSTAL_GLASS->R.drawable.smoked_glass_texture;BoardTheme.ROYAL_SAPPHIRE->R.drawable.premium_walnut_texture});val materialBrush=remember(texture){ShaderBrush(ImageShader(texture,TileMode.Mirror,TileMode.Mirror))};val pulse by rememberInfiniteTransition(label="legal").animateFloat(.58f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse")
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
  drawRect(materialBrush,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(p.field.copy(if(theme==BoardTheme.CRYSTAL_GLASS).62f else .32f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));drawRect(Color(0xffffead0).copy(if(theme==BoardTheme.PREMIUM_WOOD).055f else .025f),Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2))
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
   BoardTheme.BRUSHED_METAL->{
    // Fine directional machining, panel seams and fasteners create a fabricated metal chassis.
    for(i in 0..34){val y=rail+(size.height-rail*2)*i/34f;val a=if(i%6==0).16f else .055f;drawLine(if(i%2==0)Color.White.copy(a)else Color.Black.copy(a),Offset(playLeft+3f,y),Offset(playRight-3f,y+sin(i*.73f)*1.2f),if(i%6==0)1.2f else .65f)}
    for(x in listOf(playLeft,barLeft,barLeft+barW,playRight)){drawLine(Color.Black.copy(.38f),Offset(x+2f,rail),Offset(x+2f,size.height-rail),2.6f);drawLine(Color.White.copy(.22f),Offset(x+4f,rail+2f),Offset(x+4f,size.height-rail-2f),1f)}
    for(x in listOf(playLeft+rail*.45f,playRight-rail*.45f))for(y in listOf(rail*.45f,size.height-rail*.45f)){drawCircle(Color.Black.copy(.48f),5.2f,Offset(x+1.5f,y+1.8f));drawCircle(Brush.radialGradient(listOf(Color.White.copy(.70f),p.frame,p.frameDark)),4.3f,Offset(x,y));drawLine(Color.Black.copy(.48f),Offset(x-2.2f,y),Offset(x+2.2f,y),.9f)}
   }
   BoardTheme.BURGUNDY_MARBLE->{for(i in 0..18){val y=rail+(size.height-rail*2)*i/18f;drawLine(if(i%3==0)Color(0xffffd4bd).copy(.075f)else Color.Black.copy(.045f),Offset(playLeft,y),Offset(playRight,y+sin(i*1.8f)*3f),if(i%3==0)1.2f else .7f)};for(i in 0..7){val x=i*size.width/7f;val vein=Path().apply{moveTo(x,0f);cubicTo(x+24f,size.height*.3f,x-18f,size.height*.7f,x+12f,size.height)};drawPath(vein,Color(0xffffc7bc).copy(.10f),style=Stroke(1.4f))}}
   BoardTheme.JADE_MARBLE->{for(i in 0..12){val x=playLeft+(playRight-playLeft)*i/12f;val vein=Path().apply{moveTo(x,rail);cubicTo(x+sin(i*1.7f)*38f,size.height*.30f,x+cos(i*1.2f)*34f,size.height*.70f,x-12f,size.height-rail)};drawPath(vein,Color(0xffb9e4c7).copy(if(i%3==0).22f else .09f),style=Stroke(if(i%3==0)2.1f else .8f));drawPath(vein,Color.Black.copy(.06f),style=Stroke(4f))}}
   BoardTheme.OBSIDIAN_IVORY->{for(i in 0..8){val x=playLeft+(playRight-playLeft)*i/8f;if(x !in barLeft..(barLeft+barW)){val vein=Path().apply{moveTo(x,rail);cubicTo(x+sin(i*2f)*32f,size.height*.30f,x+cos(i*1.3f)*46f,size.height*.68f,x+sin(i*.7f)*25f,size.height-rail)};drawPath(vein,Color.White.copy(if(i%3==0).22f else .09f),style=Stroke(if(i%3==0)2.3f else 1f));drawPath(vein,Color(0xff505860).copy(.07f),style=Stroke(4f))}}}
   BoardTheme.ROYAL_SAPPHIRE->{val sheen=Brush.linearGradient(listOf(Color(0xffffd77c).copy(.10f),Color.Transparent,Color(0xff31589b).copy(.16f)));drawRect(sheen,Offset(playLeft,rail),Size(playRight-playLeft,size.height-rail*2));for(i in 0..16){val x=playLeft+(playRight-playLeft)*i/16f;drawLine(if(i%2==0)p.accent.copy(.18f)else Color.White.copy(.05f),Offset(x,rail),Offset(x+sin(i.toFloat())*8f,size.height-rail),.8f)}}
   BoardTheme.CRYSTAL_GLASS->{val sheen=Brush.linearGradient(listOf(Color.White.copy(.24f),Color(0xff9beaf0).copy(.06f),Color.Transparent,Color(0xffffd9a2).copy(.10f)));drawRect(sheen,Offset(playLeft,rail),Size(half,size.height-rail*2));drawRect(sheen,Offset(barLeft+barW,rail),Size(half,size.height-rail*2));for(i in 0..9){val x=playLeft+i*(playRight-playLeft)/9f;drawLine(Color.White.copy(if(i%3==0).18f else .07f),Offset(x,rail),Offset(x+110f,size.height-rail),if(i%3==0)1.8f else .8f);drawLine(Color(0xff7de8f2).copy(.08f),Offset(x+45f,rail),Offset(x-30f,size.height-rail),1f)};drawRoundRect(Color.White.copy(.20f),Offset(3f,3f),Size(size.width-6f,size.height-6f),CornerRadius(12f),style=Stroke(2f))}
  }
  // Fine routed ornament inspired by traditional luxury boards, kept inside the timber rails.
  if(theme==BoardTheme.PREMIUM_WOOD||theme==BoardTheme.ROYAL_SAPPHIRE){
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
  val checkerTheme=pieces.materialTheme()
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false,available:Boolean=false,variation:Float=0f){
   // Two-part luxury checker: a cast ornamental metal body around a deep fabric inset.
   val gold=when(checkerTheme){BoardTheme.BURGUNDY_MARBLE,BoardTheme.JADE_MARBLE,BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xffdddcd5)else Color(0xff52606a);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xff8edee5)else Color(0xff174b61);else->if(white)Color(0xffc6a15d)else Color(0xff765238)};val goldLight=when(checkerTheme){BoardTheme.BURGUNDY_MARBLE,BoardTheme.JADE_MARBLE,BoardTheme.OBSIDIAN_IVORY->if(white)Color.White else Color(0xffa7bac4);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xffd6ffff)else Color(0xff5eb3ca);else->if(white)Color(0xffffe3a0)else Color(0xffc18a5b)};val goldDark=when(checkerTheme){BoardTheme.BURGUNDY_MARBLE,BoardTheme.JADE_MARBLE,BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xff777a7d)else Color(0xff182127);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xff276c78)else Color(0xff071a25);else->if(white)Color(0xff68431f)else Color(0xff29170f)}
   val inset=when(checkerTheme){BoardTheme.BURGUNDY_MARBLE->if(white)Color(0xffeee0c4)else Color(0xff6c202b);BoardTheme.JADE_MARBLE->if(white)Color(0xffb9d7bd)else Color(0xff101718);BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xffeadfc7)else Color(0xff17191b);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xffd9ffff)else Color(0xff111b27);BoardTheme.ROYAL_SAPPHIRE->if(white)Color(0xfffff2d4)else Color(0xff09266e);else->if(white)Color(0xff741f2d)else Color(0xff21191a)};val insetLight=when(checkerTheme){BoardTheme.BURGUNDY_MARBLE->if(white)Color.White else Color(0xffad5260);BoardTheme.JADE_MARBLE->if(white)Color(0xffedfff0)else Color(0xff4d5c59);BoardTheme.OBSIDIAN_IVORY->if(white)Color.White else Color(0xff54575a);BoardTheme.CRYSTAL_GLASS->if(white)Color.White else Color(0xff556477);BoardTheme.ROYAL_SAPPHIRE->if(white)Color.White else Color(0xff365bc0);else->if(white)Color(0xffa74450)else Color(0xff574044)};val topC=c-Offset(0f,r*.070f)
   if(checkerTheme==BoardTheme.PREMIUM_WOOD){
    // Persian khatam-style checker for the walnut board: lacquer shell, geometric inlay and deep gloss.
    val shell=if(white)Color(0xffeee7d5)else Color(0xff141315);val shellMid=if(white)Color(0xffc9c0aa)else Color(0xff363034);val shellDark=if(white)Color(0xff756d61)else Color(0xff050506);val ink=if(white)Color(0xff27383a)else Color(0xffeee2ad);val ivory=Color(0xffffedbd);val turquoise=Color(0xff557f7a);val gilt=Color(0xffc9a24e)
    drawOval(Color.Black.copy(.15f),Offset(c.x-r*1.06f,c.y-r*.59f),Size(r*2.12f,r*1.82f));drawOval(Color.Black.copy(.30f),Offset(c.x-r*.95f,c.y-r*.49f),Size(r*1.90f,r*1.58f))
    drawCircle(shellDark,r,c+Offset(0f,r*.09f));drawCircle(Brush.verticalGradient(listOf(Color.White.copy(if(white).75f else .22f),shellMid,shellDark)),r*.97f,c)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(if(white).82f else .30f),shell,shellMid,shellDark),topC-Offset(r*.30f,r*.34f),r*1.30f),r*.91f,topC)
    drawCircle(Color.Black.copy(.48f),r*.77f,topC);drawCircle(Brush.radialGradient(listOf(shell.copy(.98f),shellMid.copy(.92f))),r*.73f,topC);drawCircle(gilt.copy(.85f),r*.70f,topC,style=Stroke(r*.028f))
    // Three concentric rings of tiny triangular inlay remain legible at game scale.
    repeat(12){i->val a=Math.toRadians(i*30.0-90.0);val ca=cos(a).toFloat();val sa=sin(a).toFloat();val q=Offset(topC.x+ca*r*.58f,topC.y+sa*r*.58f);val tang=Offset(-sa,ca);val radial=Offset(ca,sa);val tri=Path().apply{moveTo(q.x+radial.x*r*.085f,q.y+radial.y*r*.085f);lineTo(q.x-tang.x*r*.075f-radial.x*r*.050f,q.y-tang.y*r*.075f-radial.y*r*.050f);lineTo(q.x+tang.x*r*.075f-radial.x*r*.050f,q.y+tang.y*r*.075f-radial.y*r*.050f);close()};drawPath(tri,when(i%3){0->gilt;1->turquoise;else->ivory});drawPath(tri,Color.Black.copy(.42f),style=Stroke(r*.012f));drawCircle(if(i%2==0)ink else gilt,r*.018f,q)}
    repeat(8){i->val a=Math.toRadians(i*45.0-90.0);val q=Offset(topC.x+cos(a).toFloat()*r*.38f,topC.y+sin(a).toFloat()*r*.38f);drawCircle(if(i%2==0)turquoise else gilt,r*.070f,q);drawCircle(ink.copy(.85f),r*.070f,q,style=Stroke(r*.014f));drawCircle(ivory,r*.020f,q)}
    // Interlocked central eight-point star, the focal motif of the reference set.
    val star=Path().apply{for(i in 0 until 16){val a=Math.toRadians(i*22.5-90.0);val rr=if(i%2==0)r*.31f else r*.145f;val x=topC.x+cos(a).toFloat()*rr;val y=topC.y+sin(a).toFloat()*rr;if(i==0)moveTo(x,y)else lineTo(x,y)};close()};drawPath(star,if(white)gilt else ivory);drawPath(star,ink,style=Stroke(r*.027f));drawCircle(turquoise,r*.090f,topC);drawCircle(ivory,r*.040f,topC)
    // Lacquer reflection follows the curved cap without washing out the mosaic.
    drawArc(Color.White.copy(if(white).48f else .28f),202f,113f,false,Offset(topC.x-r*.86f,topC.y-r*.86f),Size(r*1.72f,r*1.72f),style=Stroke(r*.045f));drawOval(Color.White.copy(if(white).25f else .16f),Offset(topC.x-r*.48f,topC.y-r*.49f),Size(r*.45f,r*.13f));drawArc(Color.Black.copy(.40f),20f,145f,false,Offset(topC.x-r*.88f,topC.y-r*.88f),Size(r*1.76f,r*1.76f),style=Stroke(r*.050f))
    if(available&&!on){drawCircle(Brush.radialGradient(listOf(Color(0xffbaff88).copy(.30f+.18f*pulse),Color(0xff55d66b).copy(.18f),Color.Transparent),topC,r*.92f),r*.82f,topC);drawCircle(Color(0xff7ee779).copy(.62f+.28f*pulse),r*.78f,topC,style=Stroke(r*.055f))};if(on){drawCircle(Color.Black.copy(.30f),r*1.10f,c,style=Stroke(5f));drawCircle(p.accent,r*1.08f,c,style=Stroke(3f))};return
   }
   if(checkerTheme==BoardTheme.BRUSHED_METAL){
    // Precision-machined gold/silver counters designed specifically for the metal chassis.
    val metal=if(white)Color(0xffd79a42)else Color(0xffc7c9ca);val hi=if(white)Color(0xffffd68b)else Color(0xfff7f8f6);val low=if(white)Color(0xff6e3513)else Color(0xff46494c)
    drawOval(Color.Black.copy(.18f),Offset(c.x-r*1.06f,c.y-r*.58f),Size(r*2.12f,r*1.82f));drawOval(Color.Black.copy(.38f),Offset(c.x-r*.94f,c.y-r*.47f),Size(r*1.88f,r*1.58f));drawCircle(low,r,c+Offset(0f,r*.10f));drawCircle(Brush.verticalGradient(listOf(hi,metal,low)),r*.97f,c)
    drawCircle(Brush.sweepGradient(listOf(hi,metal,low,metal,hi,metal,low,metal,hi),topC),r*.90f,topC);drawCircle(low.copy(.80f),r*.78f,topC,style=Stroke(r*.055f));drawCircle(hi.copy(.72f),r*.71f,topC,style=Stroke(r*.025f))
    // Radial machining marks and a knurled side band catch the board's warm light.
    repeat(24){i->val a=Math.toRadians(i*15.0);val v=Offset(cos(a).toFloat(),sin(a).toFloat());drawLine((if(i%2==0)hi else low).copy(.20f),topC+v*r*.10f,topC+v*r*.68f,r*.010f)}
    repeat(18){i->val a=Math.toRadians(i*20.0);val q=Offset(topC.x+cos(a).toFloat()*r*.82f,topC.y+sin(a).toFloat()*r*.82f);drawCircle(if(i%2==0)hi.copy(.42f)else low.copy(.48f),r*.018f,q)}
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(.38f),Color.Transparent),topC-Offset(r*.28f,r*.30f),r*.65f),r*.62f,topC);drawArc(Color.White.copy(.52f),205f,105f,false,Offset(topC.x-r*.87f,topC.y-r*.87f),Size(r*1.74f,r*1.74f),style=Stroke(r*.035f));drawArc(Color.Black.copy(.40f),18f,142f,false,Offset(topC.x-r*.89f,topC.y-r*.89f),Size(r*1.78f,r*1.78f),style=Stroke(r*.045f))
    if(available&&!on){drawCircle(Brush.radialGradient(listOf(Color(0xffbaff88).copy(.30f+.18f*pulse),Color(0xff55d66b).copy(.18f),Color.Transparent),topC,r*.92f),r*.82f,topC);drawCircle(Color(0xff7ee779).copy(.62f+.28f*pulse),r*.78f,topC,style=Stroke(r*.055f))};if(on){drawCircle(Color.Black.copy(.30f),r*1.10f,c,style=Stroke(5f));drawCircle(p.accent,r*1.08f,c,style=Stroke(3f))};return
   }
   // Marble and smoked-glass collections retain an ornamental two-part construction.
   // Thick sidewall and soft contact shadow make the ring read as a solid cast piece.
   drawOval(Color.Black.copy(.15f),Offset(c.x-r*1.05f,c.y-r*.59f),Size(r*2.10f,r*1.78f));drawOval(Color.Black.copy(.30f),Offset(c.x-r*.94f,c.y-r*.49f),Size(r*1.88f,r*1.55f))
   drawCircle(goldDark,r,c+Offset(0f,r*.085f));drawCircle(Brush.verticalGradient(listOf(goldLight,gold,goldDark)),r*.97f,c)
   drawCircle(Brush.radialGradient(listOf(goldLight,gold,goldDark),topC-Offset(r*.30f,r*.32f),r*1.28f),r*.91f,topC)
   drawCircle(Color.Black.copy(.40f),r*.72f,topC,style=Stroke(r*.095f));drawCircle(goldLight.copy(.72f),r*.76f,topC,style=Stroke(r*.025f))
   // Engraved floral/beaded band inspired by the supplied metal texture.
   repeat(12){i->val a=Math.toRadians(i*30.0-90.0);val q=Offset(topC.x+cos(a).toFloat()*r*.71f,topC.y+sin(a).toFloat()*r*.71f);drawCircle(Color.Black.copy(.34f),r*.045f,q+Offset(r*.012f,r*.018f));drawCircle(goldLight.copy(.75f),r*.035f,q)}
   repeat(8){i->val start=i*45f+8f;drawArc(Color.Black.copy(.32f),start,27f,false,Offset(topC.x-r*.66f,topC.y-r*.66f),Size(r*1.32f,r*1.32f),style=Stroke(r*.028f));drawArc(goldLight.copy(.48f),start+1f,23f,false,Offset(topC.x-r*.64f,topC.y-r*.64f),Size(r*1.28f,r*1.28f),style=Stroke(r*.018f))}
   // The coloured centre is a separate recessed velvet/leather insert, not a painted flat circle.
   drawCircle(Color.Black.copy(.55f),r*.55f,topC+Offset(0f,r*.025f));drawCircle(Brush.radialGradient(listOf(insetLight,inset,inset.copy(.82f),Color.Black.copy(.76f)),topC-Offset(r*.17f,r*.20f),r*.72f),r*.51f,topC)
   drawCircle(insetLight.copy(.34f),r*.47f,topC,style=Stroke(r*.022f));drawOval(Color.White.copy(if(white).16f else .09f),Offset(topC.x-r*.34f,topC.y-r*.31f),Size(r*.29f,r*.10f))
   // Fine crossed fibres provide a restrained textile texture in the inset.
   repeat(3){i->val y=topC.y-r*.24f+i*r*.23f;drawLine(goldLight.copy(.055f),Offset(topC.x-r*.39f,y),Offset(topC.x+r*.39f,y+r*.05f),r*.014f)}
   drawArc(Color.White.copy(.44f),202f,112f,false,Offset(topC.x-r*.86f,topC.y-r*.86f),Size(r*1.72f,r*1.72f),style=Stroke(r*.032f));drawArc(Color.Black.copy(.42f),18f,145f,false,Offset(topC.x-r*.88f,topC.y-r*.88f),Size(r*1.76f,r*1.76f),style=Stroke(r*.045f))
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
