package com.dara.backgammon.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dara.backgammon.R
import com.dara.backgammon.ai.AiPersona
import com.dara.backgammon.ai.Difficulty
import com.dara.backgammon.core.Mode
import com.dara.backgammon.data.*

@Composable internal fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit)=Column(modifier.graphicsLayer{shadowElevation=24.dp.toPx();shape=RoundedCornerShape(18.dp);clip=false}.clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xe8292e31),Color(0xf4111518)))).border(1.dp,Color.White.copy(.18f),RoundedCornerShape(18.dp)).padding(18.dp),content=content)
@Composable internal fun MainMenu(vm:GameViewModel){
 val s by vm.settings.collectAsStateWithLifecycle();val hasSaved by vm.hasSaved.collectAsStateWithLifecycle();var dialog by remember{mutableStateOf("")};val scene=when(s.tableScene){TableScene.DARK_GRASS->R.drawable.table_dark_grass;TableScene.STARRY_SKY->R.drawable.table_starry_sky;TableScene.AUTUMN_SUNSET->R.drawable.table_autumn_sunset;TableScene.PERSIAN_RUG->R.drawable.table_persian_rug;TableScene.DARK_RIVER->R.drawable.table_dark_river}
 Box(Modifier.fillMaxSize()){
  Image(painterResource(scene),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xf40b0e10),Color(0xd51a1e20),Color(0x9c34393a),Color(0xb80b0e10)))))
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.18f),Color.Transparent,Color.Black.copy(.68f)))))
  Column(Modifier.align(Alignment.CenterStart).padding(start=48.dp).width(320.dp),horizontalAlignment=Alignment.Start){
   Text("♛",color=Color(0xffffd58a),fontSize=24.sp)
   Text(stringResource(R.string.app_name),fontSize=30.sp,fontWeight=FontWeight.Black,letterSpacing=2.5.sp,color=Color(0xffffe2a5),style=androidx.compose.ui.text.TextStyle(shadow=androidx.compose.ui.graphics.Shadow(Color.Black,Offset(2f,3f),8f)))
   Text(stringResource(R.string.tagline),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.6.sp,color=Color.White.copy(.62f))
   Spacer(Modifier.height(24.dp))
   HeroMenuButton(if(hasSaved)stringResource(R.string.new_game) else stringResource(R.string.start_game),"▷",true){vm.setup()}
   if(hasSaved){Spacer(Modifier.height(9.dp));HeroMenuButton(stringResource(R.string.continue_game),"↻",false){vm.continueGame()}}
  }
  Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start=30.dp,end=30.dp,bottom=16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   LuxuryNav("♟",stringResource(R.string.play_ai),Modifier.weight(1f)){vm.setup()};LuxuryNav("♟♟",stringResource(R.string.play_local),Modifier.weight(1f)){vm.start(Mode.LOCAL)};LuxuryNav("◈",stringResource(R.string.themes),Modifier.weight(1f)){dialog="themes"};LuxuryNav("⚙",stringResource(R.string.settings),Modifier.weight(1f)){dialog="settings"}
  }
  Row(Modifier.align(Alignment.TopEnd).padding(top=12.dp,end=16.dp)){HomeIcon(if(s.sound)"🔊" else "🔇",if(s.sound)stringResource(R.string.sound_off_action) else stringResource(R.string.sound_on_action)){vm.update(s.copy(sound=!s.sound))};Spacer(Modifier.width(8.dp));HomeIcon("⚙",stringResource(R.string.open_settings_action)){dialog="settings"}}
 }
 if(dialog.isNotEmpty())Dialog(dialog,s,vm){dialog=""}
}
@Composable internal fun HeroMenuButton(text:String,icon:String,primary:Boolean,go:()->Unit){val shape=RoundedCornerShape(6.dp);Row(Modifier.fillMaxWidth().height(52.dp).graphicsLayer{shadowElevation=if(primary)14.dp.toPx()else 6.dp.toPx();this.shape=shape;clip=false}.clip(shape).background(if(primary)Brush.horizontalGradient(listOf(Color(0xee4a4f51),Color(0xf0171a1d)))else Brush.horizontalGradient(listOf(Color(0xcc24282b),Color(0xee101316)))).border(1.dp,if(primary)Color(0xffffcf82).copy(.75f)else Color.White.copy(.24f),shape).clickable(onClick=go).padding(horizontal=17.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,color=Color(0xffffd896),fontSize=22.sp);Spacer(Modifier.width(14.dp));Text(text,Modifier.weight(1f),color=Color(0xffffe6b4),fontWeight=FontWeight.Black,letterSpacing=1.2.sp);Text("›",color=Color(0xffffdc9c),fontSize=22.sp)}}
@Composable internal fun LuxuryNav(icon:String,title:String,modifier:Modifier=Modifier,go:()->Unit){val shape=RoundedCornerShape(7.dp);Row(modifier.height(46.dp).clip(shape).background(Brush.verticalGradient(listOf(Color(0xaa23272b),Color(0xdd101316)))).border(1.dp,Color(0xffffd28a).copy(.28f),shape).clickable(onClick=go).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,color=Color(0xffffd38c),fontSize=16.sp);Spacer(Modifier.width(9.dp));Text(title,color=Color.White.copy(.84f),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=.7.sp)}}
@Composable internal fun HomeIcon(icon:String,label:String,go:()->Unit){FilledTonalButton(go,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(54.dp).semantics{contentDescription=label},shape=androidx.compose.foundation.shape.CircleShape,colors=ButtonDefaults.filledTonalButtonColors(containerColor=Color(0xdd2b332e),contentColor=Color.White)){Text(icon,fontSize=23.sp)}}
@Composable internal fun SetupMenu(vm:GameViewModel){
 val s by vm.settings.collectAsStateWithLifecycle();var dialog by remember{mutableStateOf("")};val scene=when(s.tableScene){TableScene.DARK_GRASS->R.drawable.table_dark_grass;TableScene.STARRY_SKY->R.drawable.table_starry_sky;TableScene.AUTUMN_SUNSET->R.drawable.table_autumn_sunset;TableScene.PERSIAN_RUG->R.drawable.table_persian_rug;TableScene.DARK_RIVER->R.drawable.table_dark_river}
 // The setup page points back at the main menu, matching the on-screen Back control.
 BackHandler{vm.menu()}
 Box(Modifier.fillMaxSize().pointerInput(Unit){var drag=0f;detectHorizontalDragGestures(onDragEnd={if(drag>120f)vm.menu();drag=0f},onHorizontalDrag={_,amount->drag+=amount})}){
  Image(painterResource(scene),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xb2454b4c),Color(0xee111618),Color(0xf4080b0d)))))
  Text(stringResource(R.string.new_game),Modifier.align(Alignment.TopCenter).padding(22.dp),fontSize=34.sp,fontWeight=FontWeight.Black,color=Color(0xffffdc83))
  Panel(Modifier.align(Alignment.Center).widthIn(420.dp,620.dp)){MenuAction(stringResource(R.string.play_ai),stringResource(R.string.play_ai_description)){dialog="ai"};MenuAction(stringResource(R.string.play_local),stringResource(R.string.play_local_description)){vm.start(Mode.LOCAL)};Row{SmallAction(stringResource(R.string.themes_short),Modifier.weight(1f)){dialog="themes"};Spacer(Modifier.width(10.dp));SmallAction(stringResource(R.string.options),Modifier.weight(1f)){dialog="settings"}};Spacer(Modifier.height(8.dp));TextButton({vm.menu()},Modifier.fillMaxWidth()){Text(stringResource(R.string.back)+"  ›",color=Color.White,fontWeight=FontWeight.Bold)}}
 }
 if(dialog.isNotEmpty())Dialog(dialog,s,vm){dialog=""}
}
@Composable internal fun MenuAction(title:String,sub:String,go:()->Unit){val shape=RoundedCornerShape(14.dp);Row(Modifier.fillMaxWidth().height(68.dp).graphicsLayer{shadowElevation=9.dp.toPx();this.shape=shape;clip=false}.clip(shape).background(Brush.horizontalGradient(listOf(Color(0xee353a3d),Color(0xf21d2124),Color(0xee101315)))).border(1.2.dp,Color(0xffffd699).copy(.38f),shape).clickable(onClick=go).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(35.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Black.copy(.25f)).border(1.dp,Gold.copy(.55f),androidx.compose.foundation.shape.CircleShape),contentAlignment=Alignment.Center){Text(if(title.contains("AI"))"✦" else "●",color=Gold,fontSize=17.sp)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,color=Color(0xffffdfa0),fontWeight=FontWeight.Black,letterSpacing=1.1.sp);Text(sub,fontSize=10.sp,color=Color.White.copy(.62f))};Text("›",color=Gold,fontSize=25.sp)};Spacer(Modifier.height(11.dp))}
@Composable internal fun SmallAction(t:String,m:Modifier=Modifier,go:()->Unit){OutlinedButton(go,m.height(44.dp),shape=RoundedCornerShape(11.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=Color(0xff202528).copy(.92f),contentColor=Color(0xffffd58b)),border=BorderStroke(1.dp,Color(0xffffc978).copy(.46f))){Text(t,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=.8.sp)}}
@Composable internal fun Dialog(kind:String,s:Settings,vm:GameViewModel,close:()->Unit){
 var tab by remember(kind){mutableStateOf("GAME")};val context=LocalContext.current
 AlertDialog(onDismissRequest=close,modifier=Modifier.fillMaxWidth(.86f).fillMaxHeight(.88f),properties=DialogProperties(usePlatformDefaultWidth=false),shape=RoundedCornerShape(14.dp),containerColor=Color(0xf015181b),tonalElevation=24.dp,
  title={Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(kind=="settings")"تنظیمات بازی" else when(kind){"ai"->"انتخاب سطح دشواری";"themes"->"مجموعه تخته‌ها";"stats"->"آمار بازی";else->"تخته نرد هوشمند"},Modifier.weight(1f),color=Color(0xffffdda0),fontWeight=FontWeight.Black,letterSpacing=1.2.sp);TextButton(close){Text("×",fontSize=25.sp,color=Color.White.copy(.72f))}}},
  text={if(kind=="settings")Row(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x66101518)).border(1.dp,Color.White.copy(.10f),RoundedCornerShape(10.dp))){
    Column(Modifier.width(165.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xdd25201c),Color(0xee111316)))).padding(7.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){Text("♛  تخته نرد هوشمند",Modifier.padding(horizontal=8.dp,vertical=5.dp),color=Color(0xffffd797),fontSize=10.sp,fontWeight=FontWeight.Black);listOf("GAME" to "⚄","AUDIO" to "♪","APPEARANCE" to "◈","GUIDANCE" to "✦","ABOUT" to "ⓘ").forEach{(name,icon)->StudioTab(icon,tabFa(name),tab==name){tab=name}}}
    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text(when(tab){"AUDIO"->"صدا و بازخورد";"APPEARANCE"->"ظاهر بازی";"GUIDANCE"->"راهنمای حرکت";"ABOUT"->"درباره برنامه";else->"شیوه بازی"},color=Color(0xffffd99a),fontWeight=FontWeight.Black,fontSize=15.sp)
     Text(when(tab){"AUDIO"->"صدا و بازخورد لمسی";"APPEARANCE"->"انتخاب تخته و مجموعه مهره";"GUIDANCE"->"نحوه نمایش حرکت‌های مجاز";"ABOUT"->"اطلاعات ناشر و خدمات فروشگاه";else->"قوانین بازی و قدرت هوش مصنوعی"},color=Color.White.copy(.44f),fontSize=10.sp)
     when(tab){
      "ABOUT"->{AboutAndStore(context)}
      "AUDIO"->{SettingToggle("افکت‌های صوتی","صدای طبیعی تاس و مهره",s.sound){vm.update(s.copy(sound=it))};SettingToggle("بازخورد لرزشی","واکنش لمسی هنگام بازی",s.vibration){vm.update(s.copy(vibration=it))};SettingToggle("پویانمایی کامل","حرکت کامل و نرم مهره و تاس",s.animations){vm.update(s.copy(animations=it))}}
      "APPEARANCE"->{Text("پوسته تخته",fontSize=10.sp,color=Gold);BoardTheme.entries.forEach{Choice(it.fa(),it==s.theme){vm.update(s.copy(theme=it,pieces=it.defaultPieces()))}};Text("مجموعه مهره‌ها",fontSize=10.sp,color=Gold);PieceStyle.entries.forEach{Choice(it.fa(),it==s.pieces){vm.update(s.copy(pieces=it))}}}
      "GUIDANCE"->GuidanceMode.entries.forEach{Choice(if(it==GuidanceMode.SIMPLE)"ساده — مقصدهای مستقیم" else "مربی — مسیرهای ترکیبی کامل",it==s.guidanceMode){vm.update(s.copy(guidanceMode=it))}}
      else->{Text("قدرت هوش مصنوعی",fontSize=10.sp,color=Gold);Difficulty.entries.forEach{Choice(it.fa(),it==s.difficulty){vm.update(s.copy(difficulty=it))}};if(vm.mode==Mode.AI){Text("سبک حریف — ${s.aiPersona.fa()}",fontSize=10.sp,color=Gold);ChoiceChips(AiPersona.entries.map{it.fa()},AiPersona.entries.indexOf(s.aiPersona)){p->vm.update(s.copy(aiPersona=AiPersona.entries[p]))}};Text("هدف مسابقه بعدی",fontSize=10.sp,color=Gold);listOf(3,5,7,9,11).forEach{target->Choice("$target امتیازی",target==s.matchTarget){vm.update(s.copy(matchTarget=target))}};Text("ساعت مسابقه",fontSize=10.sp,color=Gold);ClockPreset.entries.forEach{Choice(when(it){ClockPreset.OFF->"خاموش";ClockPreset.RAPID_60->"سریع — ۱ دقیقه ذخیره / ۱۰ ثانیه هر نوبت";ClockPreset.STANDARD_180->"استاندارد — ۳ دقیقه ذخیره / ۲۰ ثانیه هر نوبت"},it==s.clockPreset){vm.update(s.copy(clockPreset=it))}}}
     }
    }
   }else Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){when(kind){"ai"->{Text("قدرت هوش مصنوعی",fontSize=10.sp,color=Gold);Difficulty.entries.forEach{Choice(it.fa(),it==s.difficulty){vm.update(s.copy(difficulty=it));vm.start(Mode.AI)}};Spacer(Modifier.height(8.dp));Text("سبک حریف — ${s.aiPersona.fa()}",fontSize=10.sp,color=Gold);ChoiceChips(AiPersona.entries.map{it.fa()},AiPersona.entries.indexOf(s.aiPersona)){p->vm.update(s.copy(aiPersona=AiPersona.entries[p]))};Text(s.aiPersona.faHint(),fontSize=9.sp,color=Color.White.copy(.55f),maxLines=2)};"themes"->BoardTheme.entries.forEach{Choice(it.fa(),it==s.theme){vm.update(s.copy(theme=it,pieces=it.defaultPieces()))}};else->Text("یک تجربه کاملاً آفلاین و اختصاصی از تخته‌نرد.")}}},
  confirmButton={if(kind!="settings")TextButton(close){Text("بستن",color=Color(0xffffd99a))}})
}
