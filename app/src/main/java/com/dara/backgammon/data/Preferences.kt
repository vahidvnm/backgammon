package com.dara.backgammon.data

import android.content.Context
import com.dara.backgammon.ai.Difficulty
import com.dara.backgammon.ai.AiPersona
enum class BoardTheme { PREMIUM_WOOD, BRUSHED_METAL, OBSIDIAN_IVORY, CRYSTAL_GLASS, ROYAL_SAPPHIRE }
enum class PieceStyle { KHATAM, METAL, OBSIDIAN_IVORY, CRYSTAL, SAPPHIRE }
enum class DiceStyle { CLASSIC, ONYX, CRYSTAL, GOLD, SILVER, MARBLE_LIGHT, MARBLE_DARK, GLASS_LIGHT, GLASS_DARK, BURGUNDY, JADE, SAPPHIRE }
enum class TableScene { DARK_GRASS, STARRY_SKY, AUTUMN_SUNSET, PERSIAN_RUG, DARK_RIVER }
enum class DoublesRate { NATURAL, REDUCED_20, REDUCED_50, NEVER }
enum class ClockPreset(val bankMillis:Long,val turnMillis:Long){OFF(0,0),RAPID_60(60_000,10_000),STANDARD_180(180_000,20_000)}
enum class GuidanceMode { SIMPLE, COACH }
fun BoardTheme.defaultPieces()=when(this){BoardTheme.PREMIUM_WOOD->PieceStyle.KHATAM;BoardTheme.BRUSHED_METAL->PieceStyle.METAL;BoardTheme.OBSIDIAN_IVORY->PieceStyle.OBSIDIAN_IVORY;BoardTheme.CRYSTAL_GLASS->PieceStyle.CRYSTAL;BoardTheme.ROYAL_SAPPHIRE->PieceStyle.SAPPHIRE}
fun PieceStyle.materialTheme()=when(this){PieceStyle.KHATAM->BoardTheme.PREMIUM_WOOD;PieceStyle.METAL->BoardTheme.BRUSHED_METAL;PieceStyle.OBSIDIAN_IVORY->BoardTheme.OBSIDIAN_IVORY;PieceStyle.CRYSTAL->BoardTheme.CRYSTAL_GLASS;PieceStyle.SAPPHIRE->BoardTheme.ROYAL_SAPPHIRE}
data class Settings(val sound:Boolean=true,val music:Boolean=false,val vibration:Boolean=true,val animations:Boolean=true,val theme:BoardTheme=BoardTheme.PREMIUM_WOOD,val pieces:PieceStyle=PieceStyle.KHATAM,val dice:DiceStyle=DiceStyle.CLASSIC,val difficulty:Difficulty=Difficulty.MEDIUM,val tableScene:TableScene=TableScene.DARK_GRASS,val doublesRate:DoublesRate=DoublesRate.NATURAL,val clockPreset:ClockPreset=ClockPreset.OFF,val aiPersona:AiPersona=AiPersona.MASTER,val guidanceMode:GuidanceMode=GuidanceMode.SIMPLE)
class Preferences(ctx:Context){
 private val p=ctx.getSharedPreferences("settings",Context.MODE_PRIVATE)
 private inline fun <reified T:Enum<T>> enum(k:String,d:T)=runCatching{enumValueOf<T>(p.getString(k,d.name)!!)}.getOrDefault(d)
 fun load():Settings{
  val clock=if(!p.getBoolean("clockOptInDefaultV2",false)){p.edit().putBoolean("clockOptInDefaultV2",true).apply();ClockPreset.OFF}else enum("clockPreset",ClockPreset.OFF)
  return Settings(p.getBoolean("sound",true),p.getBoolean("music",false),p.getBoolean("vibration",true),p.getBoolean("animations",true),enum("theme",BoardTheme.PREMIUM_WOOD),enum("pieces",PieceStyle.KHATAM),enum("dice",DiceStyle.CLASSIC),enum("difficulty",Difficulty.MEDIUM),enum("tableScene",TableScene.DARK_GRASS),enum("doublesRate",DoublesRate.NATURAL),clock,enum("aiPersona",AiPersona.MASTER),enum("guidanceMode",GuidanceMode.SIMPLE))
 }
 fun save(s:Settings){p.edit().putBoolean("sound",s.sound).putBoolean("music",s.music).putBoolean("vibration",s.vibration).putBoolean("animations",s.animations).putString("theme",s.theme.name).putString("pieces",s.pieces.name).putString("dice",s.dice.name).putString("difficulty",s.difficulty.name).putString("tableScene",s.tableScene.name).putString("doublesRate",s.doublesRate.name).putString("clockPreset",s.clockPreset.name).putString("aiPersona",s.aiPersona.name).putString("guidanceMode",s.guidanceMode.name).apply()}
}
