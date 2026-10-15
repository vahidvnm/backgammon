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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable internal fun GameDrawer(kind:String,s:Settings,match:MatchInfo,vm:GameViewModel,close:()->Unit,switch:(String)->Unit){
 Box(Modifier.fillMaxSize().background(Color.Black.copy(.24f)).clickable(onClick=close)){
  AnimatedContent(kind,modifier=Modifier.align(Alignment.CenterEnd).widthIn(min=340.dp,max=400.dp).padding(end=18.dp),transitionSpec={slideInHorizontally(tween(330)){it}+fadeIn() togetherWith slideOutHorizontally(tween(250)){it}+fadeOut()},label="woodMenu"){page->
   Column(Modifier.heightIn(max=530.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xee272c2f),Color(0xf2111518)))).border(1.dp,Color.White.copy(.16f),RoundedCornerShape(16.dp)).verticalScroll(rememberScrollState()).padding(12.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(7.dp)){
    val nested=page in listOf("themes","pieces","difficulty","sound","dice","clock","guidance")
    WoodMenuPlank(if(nested)"‹  SETTINGS" else when(page){"menu"->"منوی مسابقه";else->"تنظیمات بازی"},"امتیاز ${match.whiteScore} — ${match.blackScore}",large=true){if(nested)switch("settings")else close()}
    when(page){
     "menu"->{WoodMenuPlank("ادامه","بازگشت به تخته",go=close);WoodMenuPlank("بازگردانی حرکت","بازگرداندن وضعیت قبلی"){vm.undo();close()};WoodMenuPlank("منوی اصلی","خروج از مسابقه"){vm.menu()}}
     "themes"->BoardTheme.entries.forEach{t->WoodMenuPlank(t.fa(),if(t==s.theme)"◆  انتخاب‌شده — تاس و مهره هماهنگ" else "مجموعه تخته، تاس و مهره پیش‌فرض"){vm.update(s.copy(theme=t,pieces=t.defaultPieces()))}}
     "pieces"->PieceStyle.entries.forEach{piece->WoodMenuPlank(piece.fa(),if(piece==s.pieces)"◆  انتخاب‌شده" else "مجموعه مهره‌ها"){vm.update(s.copy(pieces=piece))}}
     "difficulty"->Difficulty.entries.forEach{d->WoodMenuPlank(d.fa(),if(d==s.difficulty)"◆  انتخاب‌شده" else "قدرت هوش مصنوعی"){vm.update(s.copy(difficulty=d))}}
     "sound"->{WoodTogglePlank("افکت‌های صوتی",s.sound){vm.update(s.copy(sound=it))};WoodTogglePlank("بازخورد لرزشی",s.vibration){vm.update(s.copy(vibration=it))};WoodTogglePlank("پویانمایی نرم",s.animations){vm.update(s.copy(animations=it))}}
     "dice"->{val labels=mapOf(DoublesRate.NATURAL to "طبیعی واقعی • ۱۶٫۷٪",DoublesRate.REDUCED_20 to "۲۰٪ کمتر • ۱۳٫۳٪",DoublesRate.REDUCED_50 to "۵۰٪ کمتر • ۸٫۳٪",DoublesRate.NEVER to "بدون جفت • ۰٪");DoublesRate.entries.forEach{rate->WoodMenuPlank(labels[rate]!!,if(rate==s.doublesRate)"◆  انتخاب‌شده — برای هر دو بازیکن یکسان" else "قانون یکسان برای بازیکن و هوش مصنوعی"){vm.update(s.copy(doublesRate=rate))}}}
     "clock"->{val labels=mapOf(ClockPreset.OFF to ("ساعت خاموش" to "بازی آزاد بدون محدودیت زمان"),ClockPreset.RAPID_60 to ("سریع • ذخیره ۱ دقیقه" to "۱۰ ثانیه در هر نوبت"),ClockPreset.STANDARD_180 to ("استاندارد • ذخیره ۳ دقیقه" to "۲۰ ثانیه در هر نوبت"));ClockPreset.entries.forEach{preset->val text=labels[preset]!!;WoodMenuPlank(text.first,if(preset==s.clockPreset)"◆  انتخاب‌شده — ${text.second}" else text.second){vm.update(s.copy(clockPreset=preset))}}}
     "guidance"->{WoodMenuPlank("راهنمای ساده",if(s.guidanceMode==GuidanceMode.SIMPLE)"◆  انتخاب‌شده — فقط حرکت مستقیم" else "نمایش یک گام مجاز"){vm.update(s.copy(guidanceMode=GuidanceMode.SIMPLE))};WoodMenuPlank("راهنمای مربی",if(s.guidanceMode==GuidanceMode.COACH)"◆  انتخاب‌شده — مسیرهای کامل" else "مسیرهای ترکیبی آموزشی"){vm.update(s.copy(guidanceMode=GuidanceMode.COACH))}}
     else->{WoodMenuPlank("صدا و حس بازی",if(s.sound)"صدا روشن" else "صدا خاموش"){switch("sound")};WoodMenuPlank("ساعت مسابقه",when(s.clockPreset){ClockPreset.OFF->"خاموش";ClockPreset.RAPID_60->"ذخیره ۱ دقیقه • نوبت ۱۰ ثانیه";ClockPreset.STANDARD_180->"ذخیره ۳ دقیقه • نوبت ۲۰ ثانیه"}){switch("clock")};WoodMenuPlank("راهنمای حرکت",if(s.guidanceMode==GuidanceMode.SIMPLE)"ساده • فقط حرکت بعدی" else "مربی • مسیرهای کامل"){switch("guidance")};WoodMenuPlank("تاس — جفت‌ها",when(s.doublesRate){DoublesRate.NATURAL->"طبیعی واقعی • ۱۶٫۷٪";DoublesRate.REDUCED_20->"۲۰٪ کمتر • ۱۳٫۳٪";DoublesRate.REDUCED_50->"۵۰٪ کمتر • ۸٫۳٪";DoublesRate.NEVER->"جفت غیرفعال"}){switch("dice")};WoodMenuPlank("پوسته تخته",s.theme.fa()){switch("themes")};WoodMenuPlank("مجموعه مهره‌ها",s.pieces.fa()){switch("pieces")};WoodMenuPlank("قدرت هوش مصنوعی",s.difficulty.fa()){switch("difficulty")}}
    }
   }
  }
 }
}
@Composable internal fun WoodMenuPlank(title:String,sub:String="",large:Boolean=false,go:()->Unit){
 val shape=RoundedCornerShape(topStart=12.dp,bottomStart=12.dp,topEnd=4.dp,bottomEnd=4.dp);val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val selected=sub.startsWith("◆")
 val scale by animateFloatAsState(if(pressed)1.045f else if(selected)1.022f else 1f,tween(110),label="plankScale");val shift by animateFloatAsState(if(pressed)-11f else if(selected)-6f else 0f,tween(140),label="plankShift")
 Column(Modifier.width(if(large)310.dp else 282.dp).height(if(large)62.dp else 54.dp).graphicsLayer{scaleX=scale;scaleY=scale;translationX=shift.dp.toPx();shadowElevation=(if(pressed)20 else if(selected)16 else 12).dp.toPx();this.shape=shape;clip=false}.clip(shape).background(Brush.horizontalGradient(if(selected)listOf(Color(0xff555b5d),Color(0xff252a2d))else listOf(Color(0xff252a2d),Color(0xff171b1e)))).border(if(selected)1.4.dp else 1.dp,if(selected)Color(0xffffd28b).copy(.72f)else Color.White.copy(.10f),shape).clickable(interactionSource=interaction,indication=null,onClick=go).padding(horizontal=18.dp,vertical=9.dp)){
  Text(title,color=Color(0xffffdda0),fontWeight=FontWeight.Black,fontSize=if(large)14.sp else 12.sp,letterSpacing=.8.sp)
  if(sub.isNotEmpty())Text(sub,color=Color(0xffffead0).copy(if(selected).86f else .68f),fontSize=10.sp)
 }
}
@Composable internal fun WoodTogglePlank(title:String,value:Boolean,set:(Boolean)->Unit){
val shape=RoundedCornerShape(8.dp)
 Row(Modifier.width(282.dp).height(54.dp).graphicsLayer{shadowElevation=8.dp.toPx();this.shape=shape;clip=false}.clip(shape).background(Brush.horizontalGradient(listOf(Color(0xff292e31),Color(0xff171b1e)))).border(1.dp,Color.White.copy(.11f),shape).clickable{set(!value)}.padding(horizontal=17.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),color=Color(0xffffdda0),fontWeight=FontWeight.Black,fontSize=11.sp);Switch(value,set,colors=SwitchDefaults.colors(checkedThumbColor=Color(0xffffe1a5),checkedTrackColor=Color(0xff3c1d12),uncheckedThumbColor=Color(0xffc49a70),uncheckedTrackColor=Color(0xff62402c)))}
}
