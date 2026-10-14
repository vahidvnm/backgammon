package com.dara.backgammon.ui

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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dara.backgammon.R
import com.dara.backgammon.core.*
import com.dara.backgammon.data.*
import kotlin.math.*

private data class CheckerMotion(val from:Int,val to:Int,val white:Boolean,val fromIndex:Int=0,val toIndex:Int=0)
data class BoardPalette(val frame:Color,val frameDark:Color,val field:Color,val pointA:Color,val pointB:Color,val light:Color,val dark:Color,val accent:Color)
fun boardPalette(t:BoardTheme)=when(t){
 BoardTheme.PREMIUM_WOOD->BoardPalette(Color(0xff75402b),Color(0xff2f160d),Color(0xffc4936d),Color(0xffd8bd8d),Color(0xff6d2730),Color(0xffffdfa0),Color(0xff351a15),Color(0xffd7b25b))
 BoardTheme.BRUSHED_METAL->BoardPalette(Color(0xff95603a),Color(0xff181718),Color(0xff8b735f),Color(0xffe0a14c),Color(0xff171616),Color(0xffffdc91),Color(0xff1a1818),Color(0xffd39845))
 BoardTheme.OBSIDIAN_IVORY->BoardPalette(Color(0xff303335),Color(0xff0c0e10),Color(0xff303334),Color(0xffe1d6bd),Color(0xff16191b),Color(0xffffedc8),Color(0xff0a0c0d),Color(0xffb79b6d))
 BoardTheme.CRYSTAL_GLASS->BoardPalette(Color(0x88c8f4fa),Color(0x99131f25),Color(0x558eb4ba),Color(0xfff7f5ee),Color(0xff20272c),Color.White,Color(0xff0f171b),Color(0xffd8b978))
 BoardTheme.ROYAL_SAPPHIRE->BoardPalette(Color(0xff8b1735),Color(0xff310510),Color(0xff5d0b21),Color(0xffffe8bd),Color(0xff5b071c),Color(0xffffe29a),Color(0xff26030d),Color(0xffd8a43d))
}

/** Precise top-down board inspired by classic mobile layouts, drawn entirely with original vector assets. */
@Composable fun FlatBoard(state:TurnState,theme:BoardTheme,pieces:PieceStyle,selected:Int?,legal:List<Move>,select:(Int)->Unit,move:(Move)->Unit,combine:(Int,Int)->Boolean={_,_->false},modifier:Modifier=Modifier,scene:TableScene=TableScene.DARK_GRASS,guidance:GuidanceMode=GuidanceMode.SIMPLE,animations:Boolean=true){
 val p=boardPalette(theme);val boardSurface=ImageBitmap.imageResource(when(theme){BoardTheme.PREMIUM_WOOD->R.drawable.empty_board_final_surface;BoardTheme.BRUSHED_METAL->R.drawable.empty_board_brushed_metal;BoardTheme.OBSIDIAN_IVORY->R.drawable.empty_board_obsidian_ivory;BoardTheme.CRYSTAL_GLASS->R.drawable.empty_board_crystal_glass;BoardTheme.ROYAL_SAPPHIRE->R.drawable.empty_board_royal_sapphire});val animatedPulse by rememberInfiniteTransition(label="legal").animateFloat(.58f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse");val pulse=if(animations)animatedPulse else 1f
 val plans=if(selected==null)emptyList() else GameEngine.movePlans(state.position,state.dice,selected)
 val visiblePlans=plans
 val displayPlans=visiblePlans.groupBy{it.to}.values.mapNotNull{routes->routes.maxByOrNull{it.moves.size}}
 var previous by remember{mutableStateOf(state.position.copyDeep())};var motion by remember{mutableStateOf<CheckerMotion?>(null)};val travel=remember{Animatable(1f)}
 LaunchedEffect(state.position.points.contentHashCode(),state.position.barWhite,state.position.barBlack,state.position.offWhite,state.position.offBlack){
  val now=state.position;val mover=now.turn;val sign=mover.sign
  if(!previous.points.contentEquals(now.points)||previous.bar(mover)!=now.bar(mover)||previous.off(mover)!=now.off(mover)){
   val from=(0..23).firstOrNull{previous.points[it]*sign>now.points[it]*sign}?:if(previous.bar(mover)>now.bar(mover))Move.BAR else null
   val to=(0..23).firstOrNull{now.points[it]*sign>previous.points[it]*sign}?:if(now.off(mover)>previous.off(mover))Move.OFF else null
   if(from!=null&&to!=null){val fromIndex=if(from in 0..23)(abs(previous.points[from])-1).coerceAtLeast(0)else 0;val toIndex=when{to in 0..23->(abs(now.points[to])-1).coerceAtLeast(0);to==Move.OFF->(now.off(mover)-1).coerceAtLeast(0);else->0};motion=CheckerMotion(from,to,mover==Player.WHITE,fromIndex,toIndex);previous=now.copyDeep();travel.snapTo(if(animations)0f else 1f);if(animations)travel.animateTo(1f,tween(920,easing=FastOutSlowInEasing));motion=null}else previous=now.copyDeep()
  }else previous=now.copyDeep()
 }
 val turnName=if(state.position.turn==Player.WHITE)"سفید" else "سیاه"
 val selectedName=when(selected){Move.BAR->"مهره روی بار";null->"هیچ مهره‌ای انتخاب نشده";else->"خانه ${(selected?:0)+1} انتخاب شده"}
 val boardDescription="تخته نرد؛ نوبت $turnName؛ سفید ${state.position.offWhite} مهره خارج و ${state.position.barWhite} مهره روی بار؛ سیاه ${state.position.offBlack} مهره خارج و ${state.position.barBlack} مهره روی بار؛ $selectedName"
 Canvas(modifier.semantics{contentDescription=boardDescription}.pointerInput(state,selected){detectTapGestures{tap->
  val geometry=BoardGeometry.from(size.width.toFloat(),size.height.toFloat());val target=geometry.targetAt(tap);val choices=legal.filter{it.from==selected&&it.to==target};val compound=plans.any{it.to==target&&it.moves.size>1};if(selected!=null&&target!=Move.BAR&&compound&&combine(selected,target))Unit else if(choices.isNotEmpty())move(choices.maxBy{it.die})else if(selected!=null&&target!=Move.BAR&&combine(selected,target))Unit else {val owns=target==Move.BAR&&state.position.bar(state.position.turn)>0||target in 0..23&&state.position.points[target]*state.position.turn.sign>0;if(owns)select(target)}
 }}){
  val geometry=BoardGeometry.from(size.width,size.height);val rail=geometry.rail;val barW=geometry.barWidth;val playLeft=geometry.playLeft;val playRight=geometry.playRight;val half=geometry.half;val barLeft=geometry.barLeft;val cw=geometry.columnWidth
  val trayX=playRight+size.width*.006f;val trayW=size.width-playRight-size.width*.012f;val trayH=size.height*.40f;val topWellY=rail*1.55f;val bottomWellY=size.height-rail*1.55f-trayH
  // Theme-specific masters preserve one calibrated physical geometry while restoring each material identity.
  // The retired procedural chassis is deliberately not rendered underneath: this saves substantial Canvas work.
  drawImage(boardSurface,dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),alpha=if(theme==BoardTheme.CRYSTAL_GLASS).92f else 1f)
  // All material detail is baked into the calibrated master; no synthetic rails or colour washes are
  // drawn here, preventing bright seams and preserving the texture's natural local colour variation.
  fun center(point:Int,index:Int):Offset=geometry.checkerCenter(point,index)
  val checkerTheme=pieces.materialTheme()
  fun DrawScope.piece(c:Offset,r:Float,white:Boolean,on:Boolean=false,available:Boolean=false,variation:Float=0f){
   // Two-part luxury checker: a cast ornamental metal body around a deep fabric inset.
   val gold=when(checkerTheme){BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xffdddcd5)else Color(0xff52606a);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xff8edee5)else Color(0xff174b61);else->if(white)Color(0xffc6a15d)else Color(0xff765238)};val goldLight=when(checkerTheme){BoardTheme.OBSIDIAN_IVORY->if(white)Color.White else Color(0xffa7bac4);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xffd6ffff)else Color(0xff5eb3ca);else->if(white)Color(0xffffe3a0)else Color(0xffc18a5b)};val goldDark=when(checkerTheme){BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xff777a7d)else Color(0xff182127);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xff276c78)else Color(0xff071a25);else->if(white)Color(0xff68431f)else Color(0xff29170f)}
   val inset=when(checkerTheme){BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xffeadfc7)else Color(0xff17191b);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xffd9ffff)else Color(0xff111b27);BoardTheme.ROYAL_SAPPHIRE->if(white)Color(0xfffff2d4)else Color(0xffa80738);else->if(white)Color(0xff741f2d)else Color(0xff21191a)};val insetLight=when(checkerTheme){BoardTheme.OBSIDIAN_IVORY->if(white)Color.White else Color(0xff54575a);BoardTheme.CRYSTAL_GLASS->if(white)Color.White else Color(0xff556477);BoardTheme.ROYAL_SAPPHIRE->if(white)Color.White else Color(0xffff4776);else->if(white)Color(0xffa74450)else Color(0xff574044)};val topC=c-Offset(0f,r*.070f)
   if(checkerTheme==BoardTheme.PREMIUM_WOOD){
    // Persian khatam-style checker for the walnut board: lacquer shell, geometric inlay and deep gloss.
    val shell=if(white)Color(0xffeee7d5)else Color(0xff141315);val shellMid=if(white)Color(0xffc9c0aa)else Color(0xff363034);val shellDark=if(white)Color(0xff756d61)else Color(0xff050506);val ink=if(white)Color(0xff27383a)else Color(0xffeee2ad);val ivory=Color(0xffffedbd);val turquoise=Color(0xff557f7a);val gilt=Color(0xffc9a24e)
    drawOval(Color.Black.copy(.23f),Offset(c.x-r*1.06f,c.y-r*.59f),Size(r*2.12f,r*1.82f));drawOval(Color.Black.copy(.38f),Offset(c.x-r*.95f,c.y-r*.49f),Size(r*1.90f,r*1.58f))
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
    if(available&&!on){drawCircle(Brush.radialGradient(listOf(Color(0xffc8ff9c).copy(.48f+.28f*pulse),Color(0xff55e879).copy(.28f),Color.Transparent),topC,r*1.12f),r*1.02f,topC);drawCircle(Color(0xff9aff91).copy(.76f+.22f*pulse),r*.86f,topC,style=Stroke(r*.075f))};if(on){drawCircle(Color.Black.copy(.30f),r*1.10f,c,style=Stroke(5f));drawCircle(p.accent,r*1.08f,c,style=Stroke(3f))};return
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
    if(available&&!on){drawCircle(Brush.radialGradient(listOf(Color(0xffc8ff9c).copy(.48f+.28f*pulse),Color(0xff55e879).copy(.28f),Color.Transparent),topC,r*1.12f),r*1.02f,topC);drawCircle(Color(0xff9aff91).copy(.76f+.22f*pulse),r*.86f,topC,style=Stroke(r*.075f))};if(on){drawCircle(Color.Black.copy(.30f),r*1.10f,c,style=Stroke(5f));drawCircle(p.accent,r*1.08f,c,style=Stroke(3f))};return
   }
   // Marble and smoked-glass collections retain an ornamental two-part construction.
   // Thick sidewall and soft contact shadow make the ring read as a solid cast piece.
   drawOval(Color.Black.copy(.23f),Offset(c.x-r*1.05f,c.y-r*.59f),Size(r*2.10f,r*1.78f));drawOval(Color.Black.copy(.38f),Offset(c.x-r*.94f,c.y-r*.49f),Size(r*1.88f,r*1.55f))
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
   if(available&&!on){drawCircle(Brush.radialGradient(listOf(Color(0xffc8ff9c).copy(.48f+.28f*pulse),Color(0xff55e879).copy(.28f),Color.Transparent),topC,r*1.12f),r*1.02f,topC);drawCircle(Color(0xff9aff91).copy(.76f+.22f*pulse),r*.86f,topC,style=Stroke(r*.075f));drawArc(Color.White.copy(.36f),205f,125f,false,Offset(topC.x-r*.70f,topC.y-r*.70f),Size(r*1.40f,r*1.40f),style=Stroke(r*.032f))};if(on){drawCircle(Color.Black.copy(.30f),r*1.10f,c,style=Stroke(5f));drawCircle(p.accent,r*1.08f,c,style=Stroke(3f))}
  }
  val active=motion;val drawn=if(active==null)previous else state.position
  for(pt in 0..23){val n=abs(drawn.points[pt]);for(i in 0 until min(n,5)){if(active?.to==pt&&i==min(n,5)-1)continue;piece(center(pt,i),cw*.262f,drawn.points[pt]>0,selected==pt&&i==min(n,5)-1,legal.any{it.from==pt}&&i==min(n,5)-1,(((pt*7+i*3)%7)-3)/3f)};if(n>5)drawCircle(p.accent,cw*.18f,center(pt,4))}
  if(active?.to!=Move.BAR){val br=cw*.238f;for(i in 0 until min(drawn.barWhite,8))piece(Offset(barLeft+barW/2,size.height*.57f+i*br*.48f),br,true,selected==Move.BAR&&i==min(drawn.barWhite,8)-1,legal.any{it.from==Move.BAR}&&i==min(drawn.barWhite,8)-1,((i*5)%7-3)/3f);for(i in 0 until min(drawn.barBlack,8))piece(Offset(barLeft+barW/2,size.height*.43f-i*br*.48f),br,false,selected==Move.BAR&&i==min(drawn.barBlack,8)-1,legal.any{it.from==Move.BAR}&&i==min(drawn.barBlack,8)-1,((i*4+2)%7-3)/3f)}
  // Borne-off checkers keep their material identity and slide into genuine full-width grooves.
  fun DrawScope.offChip(y:Float,white:Boolean){val chipW=trayW*.86f;val chipH=((trayH-16f)/15f)*.84f;val base=when(checkerTheme){BoardTheme.OBSIDIAN_IVORY->if(white)Color(0xffe7e1d3)else Color(0xff1b2024);BoardTheme.CRYSTAL_GLASS->if(white)Color(0xffa9edf1)else Color(0xff16475c);BoardTheme.ROYAL_SAPPHIRE->if(white)Color(0xffffe5a5)else Color(0xff9f0734);else->if(white)Color(0xffeee5cf)else Color(0xff211719)};val edge=if(white)Color(0xff8b7659)else Color(0xff050506);val x=trayX+(trayW-chipW)/2;drawRoundRect(Color.Black.copy(.48f),Offset(x+1.5f,y+2.2f),Size(chipW,chipH),CornerRadius(chipH/2));drawRoundRect(edge,Offset(x,y+chipH*.14f),Size(chipW,chipH*.86f),CornerRadius(chipH/2));drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(if(white).68f else .25f),base,edge)),Offset(x,y),Size(chipW,chipH*.82f),CornerRadius(chipH/2));drawLine(Color.White.copy(if(white).42f else .20f),Offset(x+chipH*.45f,y+chipH*.18f),Offset(x+chipW-chipH*.45f,y+chipH*.18f),1.2f)};val chipStep=(trayH-16f)/15f;for(i in 0 until drawn.offBlack)offChip(topWellY+8f+i*chipStep,false);for(i in 0 until drawn.offWhite)offChip(bottomWellY+trayH-8f-(i+1)*chipStep,true)
  active?.let{m->
   fun endpoint(point:Int,start:Boolean):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,if(m.white)size.height*.62f else size.height*.38f);Move.OFF->{val y=if(m.white)bottomWellY+trayH-8f-(m.toIndex+.5f)*chipStep else topWellY+8f+(m.toIndex+.5f)*chipStep;Offset(trayX+trayW/2,y)};else->center(point,if(start)m.fromIndex else m.toIndex)}
   val a=endpoint(m.from,true);val b=endpoint(m.to,false);val q=travel.value*travel.value*(3f-2f*travel.value);val moving=Offset(a.x+(b.x-a.x)*q,a.y+(b.y-a.y)*q);val lift=sin(Math.PI.toFloat()*q);drawOval(Color.Black.copy(.25f-.11f*lift),Offset(moving.x-cw*(.25f+.04f*lift),moving.y+cw*(.19f+.07f*lift)),Size(cw*(.50f+.08f*lift),cw*(.15f+.03f*lift)));piece(moving,cw*(.272f+.014f*lift),m.white,true)
  }
  // Precision landing reticles reveal direct and compound destinations without fake checkers.
  displayPlans.forEach{plan->
   fun marker(point:Int):Offset=when(point){Move.BAR->Offset(barLeft+barW/2,size.height/2);Move.OFF->Offset(playRight+(size.width-playRight)/2,size.height/2);else->center(point,abs(state.position.points[point]).coerceAtMost(4))}
   val routePoints=mutableListOf(marker(plan.from)).apply{plan.moves.forEach{add(marker(it.to))}}
   if(guidance==GuidanceMode.COACH)for(i in 0 until routePoints.lastIndex)drawLine(Color(0xffffdf68).copy(.34f+.20f*pulse),routePoints[i],routePoints[i+1],3f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(10f,7f)))
   val c=routePoints.last();val direction=if(c.y<size.height/2)1f else -1f;val slide=((pulse-.58f)/.42f).coerceIn(0f,1f)*cw*.11f;val tipY=c.y-direction*(cw*.36f-slide);val tailY=tipY-direction*cw*.42f;val guide=p.accent.copy(.82f+.18f*pulse)
   // A gently sliding inlaid arrow invites the move without resembling another checker.
   drawLine(Color.Black.copy(.28f),Offset(c.x+1.5f,tailY+2f),Offset(c.x+1.5f,tipY-direction*cw*.06f+2f),5.5f)
   drawLine(guide,Offset(c.x,tailY),Offset(c.x,tipY-direction*cw*.06f),3.2f)
   val arrow=Path().apply{moveTo(c.x,tipY);lineTo(c.x-cw*.145f,tipY-direction*cw*.19f);lineTo(c.x+cw*.145f,tipY-direction*cw*.19f);close()};drawPath(arrow,Color.Black.copy(.30f),style=Stroke(4.5f));drawPath(arrow,guide)
   repeat(plan.moves.size.coerceAtMost(4)){i->val x=c.x+(i-(plan.moves.size.coerceAtMost(4)-1)/2f)*cw*.065f;drawCircle(Color(0xffffe29a).copy(.88f),cw*.018f,Offset(x,tailY-direction*cw*.045f))}
  }
 }
}
