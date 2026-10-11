package com.dara.backgammon.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.dara.backgammon.BuildConfig
import com.dara.backgammon.R
import com.dara.backgammon.ai.Difficulty
import com.dara.backgammon.core.MatchInfo
import com.dara.backgammon.data.*

@Composable internal fun StudioTab(icon:String,title:String,on:Boolean,go:()->Unit){Row(Modifier.fillMaxWidth().height(40.dp).semantics{selected=on;role=Role.Tab;contentDescription="بخش $title"}.clip(RoundedCornerShape(7.dp)).background(if(on)Brush.horizontalGradient(listOf(Color(0xffa26839),Color(0xff39241a)))else Brush.horizontalGradient(listOf(Color.Transparent,Color.Transparent))).border(if(on)1.dp else 0.dp,if(on)Color(0xffffd18b).copy(.55f)else Color.Transparent,RoundedCornerShape(7.dp)).clickable(onClick=go).padding(horizontal=11.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,color=if(on)Color(0xffffdc9b)else Color.White.copy(.55f));Spacer(Modifier.width(10.dp));Text(title,color=if(on)Color(0xffffe1ad)else Color.White.copy(.70f),fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)}}
@Composable internal fun SettingToggle(title:String,sub:String,value:Boolean,set:(Boolean)->Unit){Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(.035f)).border(1.dp,Color.White.copy(.08f),RoundedCornerShape(8.dp)).clickable{set(!value)}.padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Color.White.copy(.88f),fontSize=10.sp,fontWeight=FontWeight.Bold);Text(sub,color=Color.White.copy(.40f),fontSize=10.sp)};Switch(value,set,colors=SwitchDefaults.colors(checkedThumbColor=Color(0xffffe0a0),checkedTrackColor=Color(0xffa3693d)))}}
@Composable internal fun Choice(t:String,on:Boolean=false,go:()->Unit){val shape=RoundedCornerShape(10.dp);Row(Modifier.fillMaxWidth().padding(vertical=3.dp).heightIn(min=48.dp).semantics{selected=on;role=Role.RadioButton;contentDescription=t}.clip(shape).background(if(on)Brush.horizontalGradient(listOf(Color(0xff555b5d),Color(0xff24282b)))else Brush.horizontalGradient(listOf(Color(0xff241915),Color(0xff1c1513)))).border(if(on)1.4.dp else .8.dp,if(on)Gold.copy(.78f)else Color.White.copy(.13f),shape).clickable(onClick=go).padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Text(if(on)"◆" else "◇",color=if(on)Gold else Color.White.copy(.28f),fontSize=10.sp);Spacer(Modifier.width(10.dp));Text(t,Modifier.weight(1f),color=if(on)Color(0xffffdda0)else Color.White.copy(.84f),fontSize=11.sp,fontWeight=if(on)FontWeight.Black else FontWeight.SemiBold);Text("›",color=if(on)Gold else Color.White.copy(.25f),fontSize=18.sp)}}
@Composable internal fun SwitchRow(t:String,v:Boolean,set:(Boolean)->Unit)=Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(t,Modifier.weight(1f));Switch(v,set)}


@Composable internal fun InGameSettingsStudio(s:Settings,match:MatchInfo,vm:GameViewModel,close:()->Unit){
 var tab by remember{mutableStateOf("GAME")};val interaction=remember{MutableInteractionSource()};val context=LocalContext.current
 Box(Modifier.fillMaxSize().background(Color.Black.copy(.48f)).clickable(onClick=close),contentAlignment=Alignment.Center){
  Row(Modifier.fillMaxWidth(.78f).fillMaxHeight(.78f).graphicsLayer{shadowElevation=32.dp.toPx();shape=RoundedCornerShape(18.dp);clip=false}.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xed22272a),Color(0xf5121619),Color(0xe922272a)))).border(1.2.dp,Color.White.copy(.20f),RoundedCornerShape(18.dp)).clickable(interactionSource=interaction,indication=null,onClick={}),verticalAlignment=Alignment.Top){
   Column(Modifier.width(190.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xe72b3032),Color(0xf0121618)))).border(0.dp,Color.Transparent).padding(9.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Column(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("♛",color=Color(0xffffd38d),fontSize=16.sp);Text("تخته نرد هوشمند",color=Color(0xffffdda2),fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=.6.sp);Text("تنظیمات مسابقه",color=Color.White.copy(.38f),fontSize=10.sp,letterSpacing=1.1.sp)}
    listOf("GAME" to "⚄","AUDIO" to "♪","CONTROLS" to "☝","APPEARANCE" to "◈","ABOUT" to "ⓘ").forEach{(name,icon)->StudioTab(icon,tabFa(name),tab==name){tab=name}}
    Spacer(Modifier.weight(1f));Text("امتیاز  ${match.whiteScore} — ${match.blackScore}",Modifier.fillMaxWidth().padding(5.dp),color=Color.White.copy(.45f),fontSize=10.sp,textAlign=TextAlign.Center,maxLines=1)
   }
   Column(Modifier.weight(1f).fillMaxHeight().padding(22.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(when(tab){"AUDIO"->"صدا و حس بازی";"CONTROLS"->"کنترل‌ها";"APPEARANCE"->"ظاهر بازی";"ABOUT"->"درباره برنامه";else->"تنظیمات بازی"},color=Color(0xffffdda0),fontSize=20.sp,fontWeight=FontWeight.Bold);Text(when(tab){"AUDIO"->"صدای طبیعی و بازخورد لمسی";"CONTROLS"->"کمک حرکتی و بازخورد تخته";"APPEARANCE"->"جنس تخته و مجموعه مهره";"ABOUT"->"ناشر و خدمات فروشگاه";else->"رفتار مسابقه و قدرت هوش مصنوعی"},color=Color.White.copy(.43f),fontSize=10.sp)};Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).border(1.dp,Color(0xffffd18a).copy(.42f),RoundedCornerShape(8.dp)).clickable(onClick=close),contentAlignment=Alignment.Center){Text("×",color=Color(0xffffdda0),fontSize=20.sp)}}
    Spacer(Modifier.height(16.dp));Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.10f)));Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(2.dp)){
     when(tab){
      "ABOUT"->{AboutAndStore(context)}
      "AUDIO"->{StudioToggleRow("افکت‌های صوتی","صدای تاس، مهره و رابط",s.sound){vm.update(s.copy(sound=it))};StudioToggleRow("بازخورد لرزشی","واکنش لمسی هنگام بازی",s.vibration){vm.update(s.copy(vibration=it))}}
      "CONTROLS"->{StudioSelectorRow("راهنمای حرکت",if(s.guidanceMode==GuidanceMode.SIMPLE)"ساده" else "مربی",{val all=GuidanceMode.entries;vm.update(s.copy(guidanceMode=all[(all.indexOf(s.guidanceMode)-1+all.size)%all.size]))},{val all=GuidanceMode.entries;vm.update(s.copy(guidanceMode=all[(all.indexOf(s.guidanceMode)+1)%all.size]))});StudioToggleRow("پویانمایی کامل","حرکت نرم تاس و مهره",s.animations){vm.update(s.copy(animations=it))}}
      "APPEARANCE"->{StudioSelectorRow("پوسته تخته",s.theme.fa(),{val all=BoardTheme.entries;val t=all[(all.indexOf(s.theme)-1+all.size)%all.size];vm.update(s.copy(theme=t,pieces=t.defaultPieces()))},{val all=BoardTheme.entries;val t=all[(all.indexOf(s.theme)+1)%all.size];vm.update(s.copy(theme=t,pieces=t.defaultPieces()))});StudioSelectorRow("مجموعه مهره‌ها",s.pieces.fa(),{val all=PieceStyle.entries;vm.update(s.copy(pieces=all[(all.indexOf(s.pieces)-1+all.size)%all.size]))},{val all=PieceStyle.entries;vm.update(s.copy(pieces=all[(all.indexOf(s.pieces)+1)%all.size]))})}
      else->{StudioSelectorRow("قدرت هوش مصنوعی",s.difficulty.fa(),{val all=Difficulty.entries;vm.update(s.copy(difficulty=all[(all.indexOf(s.difficulty)-1+all.size)%all.size]))},{val all=Difficulty.entries;vm.update(s.copy(difficulty=all[(all.indexOf(s.difficulty)+1)%all.size]))});StudioSelectorRow("ساعت مسابقه",when(s.clockPreset){ClockPreset.OFF->"خاموش";ClockPreset.RAPID_60->"سریع";ClockPreset.STANDARD_180->"استاندارد"},{val all=ClockPreset.entries;vm.update(s.copy(clockPreset=all[(all.indexOf(s.clockPreset)-1+all.size)%all.size]))},{val all=ClockPreset.entries;vm.update(s.copy(clockPreset=all[(all.indexOf(s.clockPreset)+1)%all.size]))});StudioSelectorRow("احتمال جفت",s.doublesRate.fa(),{val all=DoublesRate.entries;vm.update(s.copy(doublesRate=all[(all.indexOf(s.doublesRate)-1+all.size)%all.size]))},{val all=DoublesRate.entries;vm.update(s.copy(doublesRate=all[(all.indexOf(s.doublesRate)+1)%all.size]))})}
     }
    }
   }
  }
 }
}
@Composable internal fun StudioSelectorRow(title:String,value:String,previous:()->Unit,next:()->Unit){Row(Modifier.fillMaxWidth().height(58.dp).border(BorderStroke(1.dp,Color.White.copy(.08f))).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),color=Color.White.copy(.82f),fontSize=11.sp);Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).clickable(onClick=previous),contentAlignment=Alignment.Center){Text("‹",color=Color(0xffffd493),fontSize=20.sp)};Box(Modifier.widthIn(min=150.dp).height(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xff0d1114)).border(1.dp,Color.White.copy(.12f),RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text(value,color=Color(0xffffdfa5),fontSize=10.sp,fontWeight=FontWeight.Bold)};Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).clickable(onClick=next),contentAlignment=Alignment.Center){Text("›",color=Color(0xffffd493),fontSize=20.sp)}}}
@Composable internal fun StudioToggleRow(title:String,sub:String,value:Boolean,set:(Boolean)->Unit){Row(Modifier.fillMaxWidth().height(64.dp).border(BorderStroke(1.dp,Color.White.copy(.08f))).clickable{set(!value)}.padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Color.White.copy(.84f),fontSize=11.sp);Text(sub,color=Color.White.copy(.38f),fontSize=10.sp)};Switch(value,set,colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Color(0xffb8804c),uncheckedThumbColor=Color(0xff777d80),uncheckedTrackColor=Color(0xff272d30)))} }
@Composable internal fun AboutAndStore(context:android.content.Context){
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)){
  Text(stringResource(R.string.app_name),color=Color(0xffffdda0),fontSize=16.sp,fontWeight=FontWeight.Black)
  Text(stringResource(R.string.about_publisher),color=Color.White.copy(.78f),fontSize=10.sp)
  Text(stringResource(R.string.about_version_store,BuildConfig.VERSION_NAME,BuildConfig.STORE_NAME),color=Color.White.copy(.46f),fontSize=10.sp)
  SettingAction(stringResource(R.string.support_title),stringResource(R.string.support_description)){MarketActions.support(context)}
  SettingAction(stringResource(R.string.update_title),stringResource(R.string.update_description,BuildConfig.STORE_NAME)){MarketActions.checkUpdate(context)}
  SettingAction(stringResource(R.string.store_page_title,BuildConfig.STORE_NAME),stringResource(R.string.store_page_description)){MarketActions.openStorePage(context)}
  SettingAction(stringResource(R.string.rate_title),stringResource(R.string.rate_description,BuildConfig.STORE_NAME)){MarketActions.rate(context)}
 }
}
@Composable internal fun SettingAction(title:String,sub:String,go:()->Unit){Row(Modifier.fillMaxWidth().heightIn(min=54.dp).semantics{role=Role.Button;contentDescription="$title، $sub"}.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(.035f)).border(1.dp,Color.White.copy(.09f),RoundedCornerShape(8.dp)).clickable(onClick=go).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Color.White.copy(.88f),fontSize=10.sp,fontWeight=FontWeight.Bold);Text(sub,color=Color.White.copy(.42f),fontSize=10.sp)};Text("‹",color=Color(0xffffd493),fontSize=20.sp)}}
