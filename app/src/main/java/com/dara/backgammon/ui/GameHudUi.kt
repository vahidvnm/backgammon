package com.dara.backgammon.ui

import android.os.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.dara.backgammon.ai.Difficulty
import com.dara.backgammon.core.*
import com.dara.backgammon.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

@Composable internal fun MatchClocks(clock:ClockState,turn:Player,modifier:Modifier=Modifier){
 fun format(ms:Long):String{val safe=ms.coerceAtLeast(0);return if(safe<10_000)"${safe/1000}.${(safe%1000)/100}" else "%d:%02d".format(safe/60_000,(safe/1000)%60)}
 androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){Row(modifier,horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top){
  ClockFace("سفید",format(clock.whiteMillis),turn==Player.WHITE,clock.turnMillis)
  ClockFace("سیاه",format(clock.blackMillis),turn==Player.BLACK,clock.turnMillis)
 }}
}
@Composable internal fun ClockFace(label:String,time:String,active:Boolean,turnMillis:Long){
 val shape=RoundedCornerShape(13.dp);val edge=if(active)Color(0xffffd58a)else Color.White.copy(.26f)
 Box(Modifier.width(126.dp).height(46.dp).semantics{contentDescription="زمان بازیکن $label، $time، زمان نوبت ${(turnMillis/1000).coerceAtLeast(0)} ثانیه";stateDescription=if(active)"نوبت فعال" else "غیرفعال"}.graphicsLayer{shadowElevation=12.dp.toPx();this.shape=shape;clip=false}){
  Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.90f).height(12.dp).clip(RoundedCornerShape(bottomStart=8.dp,bottomEnd=8.dp)).background(Brush.verticalGradient(listOf(Color(0xff9b633d),Color(0xff3a190f)))).border(.7.dp,Color(0xffd99b64).copy(.55f),RoundedCornerShape(bottomStart=8.dp,bottomEnd=8.dp)))
  Box(Modifier.fillMaxWidth().height(42.dp).clip(shape).background(Brush.linearGradient(if(active)listOf(Color(0xdde8e1d5),Color(0xb8a6b0ac),Color(0xd84d413a))else listOf(Color(0xcac7cbc8),Color(0x9d7c8583),Color(0xc637302e)))).border(if(active)1.5.dp else 1.dp,edge,shape).padding(horizontal=7.dp)){
   Box(Modifier.matchParentSize().padding(2.dp).clip(RoundedCornerShape(11.dp)).background(Brush.verticalGradient(listOf(Color.White.copy(.20f),Color.Transparent,Color.Black.copy(.14f)))))
   Text(label,Modifier.align(Alignment.TopEnd).padding(top=3.dp),color=if(active)Color(0xff422418)else Color.White.copy(.74f),fontSize=6.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)
   Text(time,Modifier.align(Alignment.Center).offset(y=2.dp),color=if(active)Color(0xff2a1711)else Color.White.copy(.88f),fontSize=16.sp,fontWeight=FontWeight.Black,letterSpacing=.7.sp)
   Box(Modifier.align(Alignment.CenterStart).size(25.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xff241711).copy(.72f)).border(1.dp,edge.copy(.75f),androidx.compose.foundation.shape.CircleShape),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("نوبت",fontSize=5.sp,color=Color.White.copy(.56f),lineHeight=5.sp);Text("${(turnMillis/1000).coerceAtLeast(0)}",color=if(turnMillis<3000)Color(0xffff8075)else Color.White,fontSize=10.sp,fontWeight=FontWeight.Black,lineHeight=9.sp)}}
  }
 }
}
@Composable internal fun CarvedScoreCounter(white:Int,black:Int,modifier:Modifier=Modifier){
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
@Composable internal fun ScoreDrum(score:Int){Box(Modifier.size(26.dp,29.dp).clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(Color(0xff080808),Color(0xff242424),Color.Black))).border(.5.dp,Color.White.copy(.28f),RoundedCornerShape(3.dp)),contentAlignment=Alignment.Center){Text((score%10).toString(),color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Medium)}}
@Composable internal fun ClockSettingsButton(go:()->Unit,modifier:Modifier=Modifier){val shape=RoundedCornerShape(9.dp);Box(modifier.size(48.dp).semantics{role=Role.Button;contentDescription="تنظیمات مسابقه"}.clickable(onClick=go),contentAlignment=Alignment.Center){Box(Modifier.size(32.dp,28.dp).clip(shape).background(Brush.verticalGradient(listOf(Color(0xff94603e),Color(0xff3b1b11)))).border(1.dp,Color(0xffffce88).copy(.58f),shape),contentAlignment=Alignment.Center){Text("⚙",color=Color(0xffffe0aa),fontSize=15.sp)}}}
@Composable internal fun TopGameBar(level:Difficulty,clockOn:Boolean,status:String,settings:()->Unit,modifier:Modifier=Modifier){CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){Row(modifier.fillMaxWidth().height(30.dp).background(Brush.verticalGradient(listOf(Color(0xff75442d),Color(0xff4b2618)))).border(BorderStroke(1.dp,Brush.verticalGradient(listOf(Color(0xffd7a06e).copy(.55f),Color(0xff241008).copy(.65f))))).padding(horizontal=if(clockOn)18.dp else 10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(settings,Modifier.size(28.dp)){Text("⚙",color=Color(0xffffdfa0),fontSize=16.sp)};if(!clockOn)Text("هوش مصنوعی • ${level.fa()} • سطح ${level.ordinal+1}",Modifier.padding(start=8.dp),color=Color.White.copy(.76f),fontSize=10.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(if(clockOn)status.replace("YOUR ","") else status,color=Color(0xffffd77a),fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=.6.sp)}}}

@Composable internal fun SceneArrow(text:String,modifier:Modifier=Modifier,go:()->Unit){Box(modifier.size(38.dp,72.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xff17110e).copy(.64f)).border(1.dp,Color.White.copy(.38f),RoundedCornerShape(18.dp)).clickable(onClick=go),contentAlignment=Alignment.Center){Text(text,color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Light)}}
@Composable internal fun BarEngraving(text:String,modifier:Modifier=Modifier,go:()->Unit){
 val shape=RoundedCornerShape(6.dp)
 Box(modifier.size(90.dp,34.dp).semantics{role=Role.Button;contentDescription=text}.clip(shape).background(Color.Black.copy(.10f)).clickable(onClick=go),contentAlignment=Alignment.Center){
  // Opposing one-pixel highlights create a recess cut into the existing board wood, not a plaque.
  Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Color.Black.copy(.28f)));Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(Color.White.copy(.14f)))
  Row(Modifier.offset(y=1.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("↶",color=Color.White.copy(.18f),fontSize=15.sp,fontWeight=FontWeight.Bold);Text(text,color=Color.White.copy(.16f),fontSize=11.sp,fontWeight=FontWeight.Black)}
  Row(Modifier.offset(y=(-.5).dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("↶",color=Color(0xff29140d),fontSize=15.sp,fontWeight=FontWeight.Bold);Text(text,color=Color(0xff29140d),fontSize=11.sp,fontWeight=FontWeight.Black)}
 }
}
@Composable internal fun AxleDoublingCube(value:Int,owner:Player?,pending:Player?,enabled:Boolean,go:()->Unit,modifier:Modifier=Modifier){
 val pulse by rememberInfiniteTransition(label="cubeOffer").animateFloat(.52f,1f,infiniteRepeatable(tween(850),RepeatMode.Reverse),label="cubePulse");val rotation=remember{Animatable(0f)};var lastValue by remember{mutableIntStateOf(value)};var accepted by remember{mutableStateOf(false)}
 LaunchedEffect(value,pending){val changed=value!=lastValue;if(changed){accepted=true;lastValue=value};rotation.snapTo(0f);rotation.animateTo(360f,tween(720,easing=FastOutSlowInEasing));if(changed){delay(1350);accepted=false}}
 val shown=if(value<=1)2 else if(owner!=null)(value*2).coerceAtMost(64)else value;val octagon=androidx.compose.foundation.shape.GenericShape{size,_->val cut=size.minDimension*.22f;moveTo(cut,0f);lineTo(size.width-cut,0f);lineTo(size.width,cut);lineTo(size.width,size.height-cut);lineTo(size.width-cut,size.height);lineTo(cut,size.height);lineTo(0f,size.height-cut);lineTo(0f,cut);close()};val glow=when{accepted->Color(0xff63df78);pending!=null->Color(0xffffc85e);enabled->Color(0xffffd36f);else->Color(0xff735039)};val glowAlpha=if(enabled||pending!=null||accepted){.72f+.25f*pulse}else .55f
 Box(modifier.size(112.dp,38.dp).clickable(enabled=enabled,onClick=go),contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxWidth().height(16.dp)){val y=size.height/2;drawLine(Color.Black.copy(.42f),Offset(7f,y+2f),Offset(size.width-7f,y+2f),6f,cap=StrokeCap.Round);drawLine(Brush.horizontalGradient(listOf(Color(0xff4b2517),Color(0xffd7a05f),Color(0xff4b2517))),Offset(7f,y),Offset(size.width-7f,y),4.2f,cap=StrokeCap.Round);drawCircle(Color(0xffefb86f),3.5f,Offset(8f,y));drawCircle(Color(0xffefb86f),3.5f,Offset(size.width-8f,y))}
  Box(Modifier.size(36.dp).graphicsLayer{rotationY=rotation.value;cameraDistance=10f*density;shadowElevation=8.dp.toPx();shape=octagon;clip=false}.clip(octagon).background(Brush.linearGradient(listOf(Color(0xfffff0c7),Color(0xffd1a963),Color(0xff76502f)))).border(if(enabled||pending!=null||accepted)2.dp else 1.dp,glow.copy(glowAlpha),octagon),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){val c=Offset(size.width/2,size.height/2);for(i in 0 until 8){val a=Math.toRadians((i*45.0-90));val b=Math.toRadians((i*45.0-67.5));val p1=Offset(c.x+cos(a).toFloat()*size.width*.46f,c.y+sin(a).toFloat()*size.height*.46f);val p2=Offset(c.x+cos(b).toFloat()*size.width*.35f,c.y+sin(b).toFloat()*size.height*.35f);drawLine(Color(0xff5d3822).copy(.28f),p1,p2,1f)}};Text("${shown}x",color=if(accepted)Color(0xff176b2d)else Color(0xff342015),fontSize=12.sp,fontWeight=FontWeight.Black,style=androidx.compose.ui.text.TextStyle(shadow=androidx.compose.ui.graphics.Shadow(Color.White.copy(.42f),Offset(-1f,-1f),1f)))}
 }
}
@Composable internal fun EngravedWoodAction(text:String,modifier:Modifier=Modifier,go:()->Unit){
 val shape=RoundedCornerShape(6.dp);val enabled=text.contains("روشن");val mark=if(enabled)"●" else "○"
 Box(modifier.size(104.dp,34.dp).semantics{role=Role.Button;contentDescription=text}.clip(shape).background(Color.Black.copy(.10f)).clickable(onClick=go),contentAlignment=Alignment.Center){
  Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Color.Black.copy(.28f)));Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(Color.White.copy(.14f)))
  Row(Modifier.offset(y=1.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Text(mark,color=Color.White.copy(.17f),fontSize=10.sp);Text(text,color=Color.White.copy(.16f),fontSize=11.sp,fontWeight=FontWeight.Black)}
  Row(Modifier.offset(y=(-.5).dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Text(mark,color=Color(0xff29140d),fontSize=10.sp);Text(text,color=Color(0xff29140d),fontSize=11.sp,fontWeight=FontWeight.Black)}
 }
}



@Composable internal fun AnimatedDice(finalDice:List<Int>,style:DiceStyle,rolling:Boolean,onThrow:()->Unit,modifier:Modifier=Modifier){
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
 val diceDescription=if(rolling)"تاس‌ها در حال چرخش" else if(finalDice.isEmpty())"تاس‌ها؛ برای پرتاب دوبار ضربه بزنید یا بکشید" else "نتیجه تاس: ${finalDice.joinToString(" و ")}"
 Box(modifier.size(230.dp,140.dp).semantics{contentDescription=diceDescription;if(!rolling&&finalDice.isEmpty())onClick("پرتاب تاس"){onThrow();true}}.graphicsLayer{translationX=drag.x;translationY=drag.y}.then(if(!rolling&&finalDice.isEmpty())Modifier.pointerInput(Unit){detectDragGestures(onDragEnd={if(drag.getDistance()>24f){throwVector=drag;onThrow()}else drag=Offset.Zero},onDragCancel={drag=Offset.Zero}){change,amount->change.consume();drag+=amount}}else Modifier)){

  DieFace(finalDice.getOrElse(0){face1},style,rolling,Modifier.align(Alignment.Center).offset{x1.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y1.value.roundToInt())}}.rotate(r1.value));DieFace(finalDice.getOrElse(1){face2},style,rolling,Modifier.align(Alignment.Center).offset{x2.value.roundToInt().let{androidx.compose.ui.unit.IntOffset(it,y2.value.roundToInt())}}.rotate(r2.value));if(!rolling&&finalDice.size>2)DieFace(finalDice[2],style,false,Modifier.align(Alignment.Center).offset(x=(-34).dp,y=48.dp));if(!rolling&&finalDice.size>3)DieFace(finalDice[3],style,false,Modifier.align(Alignment.Center).offset(x=34.dp,y=48.dp))}
}
@Composable internal fun DieFace(value:Int,style:DiceStyle,rolling:Boolean,modifier:Modifier=Modifier){
 val base=when(style){DiceStyle.CLASSIC->Color(0xffffedc5);DiceStyle.ONYX->Color(0xff252731);DiceStyle.CRYSTAL, DiceStyle.GLASS_LIGHT->Color(0xff77dce6);DiceStyle.GLASS_DARK->Color(0xff123b52);DiceStyle.GOLD->Color(0xffd99a3d);DiceStyle.SILVER->Color(0xffc8cbcd);DiceStyle.MARBLE_LIGHT->Color(0xffe9e7df);DiceStyle.MARBLE_DARK->Color(0xff303538);DiceStyle.BURGUNDY->Color(0xff70232d);DiceStyle.JADE->Color(0xff83ae91);DiceStyle.SAPPHIRE->Color(0xff123c91)};val lightInk=style in listOf(DiceStyle.CLASSIC,DiceStyle.GOLD,DiceStyle.SILVER,DiceStyle.MARBLE_LIGHT,DiceStyle.GLASS_LIGHT);val ink=if(lightInk)Color(0xff28231f)else Color.White;val rim=when(style){DiceStyle.GOLD->Color(0xffffd27c);DiceStyle.SILVER->Color(0xfff5f6f5);DiceStyle.MARBLE_LIGHT,DiceStyle.MARBLE_DARK->Color(0xff89959b);DiceStyle.CRYSTAL,DiceStyle.GLASS_LIGHT,DiceStyle.GLASS_DARK->Color(0xff9beef3);else->Color(0xff765a40)}
 Canvas(modifier.size(52.dp)){
  val u=size.minDimension/62f
  // Three visible planes make the die read as a solid cube rather than a flat tile.
  drawRoundRect(Color.Black.copy(.34f),Offset(10*u,12*u),Size(48*u,47*u),CornerRadius(12*u))
  val rightFace=Path().apply{moveTo(50*u,7*u);lineTo(57*u,12*u);lineTo(57*u,48*u);lineTo(50*u,52*u);close()};drawPath(rightFace,Brush.horizontalGradient(listOf(base.copy(.72f),Color.Black.copy(.34f)),50*u,57*u));drawPath(rightFace,rim.copy(.62f),style=Stroke(1.4f*u))
  val bottomFace=Path().apply{moveTo(8*u,48*u);lineTo(50*u,48*u);lineTo(57*u,52*u);lineTo(14*u,57*u);close()};drawPath(bottomFace,Brush.verticalGradient(listOf(base.copy(.68f),Color.Black.copy(.42f)),48*u,57*u));drawPath(bottomFace,rim.copy(.58f),style=Stroke(1.4f*u))
  drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(.66f),base,base.copy(.72f))),Offset(6*u,4*u),Size(44*u,44*u),CornerRadius(10*u));drawRoundRect(rim.copy(.88f),Offset(6*u,4*u),Size(44*u,44*u),CornerRadius(10*u),style=Stroke(1.8f*u));drawRoundRect(Color.White.copy(.50f),Offset(8*u,6*u),Size(40*u,40*u),CornerRadius(8*u),style=Stroke(1.1f*u))
  when(style){DiceStyle.GOLD,DiceStyle.SILVER->{repeat(7){i->drawLine((if(i%2==0)Color.White else Color.Black).copy(.10f),Offset(11*u,(11+i*5)*u),Offset(47*u,(11+i*5)*u),.6f*u)}};DiceStyle.MARBLE_LIGHT,DiceStyle.MARBLE_DARK->{val vein=Path().apply{moveTo(10*u,18*u);cubicTo(21*u,12*u,29*u,34*u,48*u,27*u);cubicTo(39*u,38*u,31*u,39*u,18*u,47*u)};drawPath(vein,rim.copy(.22f),style=Stroke(1.1f*u))};DiceStyle.CRYSTAL,DiceStyle.GLASS_LIGHT,DiceStyle.GLASS_DARK->drawRoundRect(Brush.radialGradient(listOf(Color.White.copy(.28f),Color.Transparent),Offset(17*u,14*u),28*u),Offset(9*u,7*u),Size(40*u,40*u),CornerRadius(10*u));else->Unit}
  val spots=when(value){1->listOf(.5f to .5f);2->listOf(.28f to .28f,.72f to .72f);3->listOf(.27f to .27f,.5f to .5f,.73f to .73f);4->listOf(.28f to .28f,.72f to .28f,.28f to .72f,.72f to .72f);5->listOf(.27f to .27f,.73f to .27f,.5f to .5f,.27f to .73f,.73f to .73f);else->listOf(.28f to .23f,.72f to .23f,.28f to .5f,.72f to .5f,.28f to .77f,.72f to .77f)}
  spots.forEach{drawCircle(Color.Black.copy(.42f),4.2f*u,Offset((6+44*it.first)*u,(4+44*it.second)*u));drawCircle(ink,3.55f*u,Offset((6+44*it.first)*u,(4+44*it.second)*u));drawCircle(Color.White.copy(.22f),.9f*u,Offset((5.2f+44*it.first)*u,(3.2f+44*it.second)*u))};listOf(15f to 20f,42f to 18f,25f to 48f,45f to 40f).forEach{drawCircle(Color(0xff8e7253).copy(.10f),.7f*u,Offset(it.first*u,it.second*u))}
 }
}

@Composable internal fun VictoryCelebration(winner:Player,whiteScore:Int,blackScore:Int,modifier:Modifier=Modifier){
 val progress by rememberInfiniteTransition(label="victory").animateFloat(0f,1f,infiniteRepeatable(tween(2200,easing=LinearEasing)),label="victoryBurst")
 Box(modifier.semantics{contentDescription="تابلوی پایان بازی؛ ${winner.fa()} برنده؛ امتیاز $whiteScore به $blackScore"},contentAlignment=Alignment.TopCenter){
  Canvas(Modifier.fillMaxSize()){
   drawRect(Brush.radialGradient(listOf(Color.Transparent,Color.Black.copy(.70f)),Offset(size.width/2,size.height/2),size.maxDimension*.72f))
   repeat(54){i->val seed=(i*47%101)/101f;val lane=(i*83%97)/97f;val y=((seed+progress)%1f)*size.height;val x=lane*size.width+sin((progress*6.28f+i))*18f;val color=listOf(Color(0xffffd15c),Color(0xfff6eee0),Color(0xff63d59a),Color(0xffd97872))[i%4];rotate((i*29f+progress*260f),Offset(x,y)){drawRoundRect(color.copy(.88f),Offset(x-3f,y-6f),Size(6f,12f),CornerRadius(2f))}}
  }
  Column(Modifier.padding(top=42.dp).width(300.dp).graphicsLayer{shadowElevation=28.dp.toPx();shape=RoundedCornerShape(20.dp);clip=false}.clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xf05f351f),Color(0xf0180d09)))).border(2.dp,Color(0xffffd47d),RoundedCornerShape(20.dp)).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text(if(winner==Player.WHITE)"پیروزی" else "پایان دست",color=Color(0xffffdf91),fontSize=25.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)
   Text("♛",color=Color(0xffffca58),fontSize=35.sp)
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(.38f)).border(1.dp,Color.White.copy(.18f),RoundedCornerShape(12.dp)).padding(10.dp),horizontalArrangement=Arrangement.SpaceEvenly){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("سفید",color=Color.White.copy(.72f),fontSize=9.sp);Text(whiteScore.toString(),color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black)};Text("—",color=Color(0xffffd47d),fontSize=24.sp);Column(horizontalAlignment=Alignment.CenterHorizontally){Text("سیاه",color=Color.White.copy(.72f),fontSize=9.sp);Text(blackScore.toString(),color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black)}}
  }
 }
}
