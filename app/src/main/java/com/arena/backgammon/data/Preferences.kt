package com.arena.backgammon.data

import android.content.Context
import com.arena.backgammon.ai.Difficulty
import com.arena.backgammon.ai.AiPersona

enum class BoardTheme { PREMIUM_WOOD, MARBLE_STONE, SMOKED_GLASS }
enum class PieceStyle { IVORY, MARBLE, NEON }
enum class DiceStyle { CLASSIC, ONYX, CRYSTAL }
enum class TableScene { STARRY_SKY, AUTUMN_SUNSET, PERSIAN_RUG, DARK_RIVER }
enum class DoublesRate { NATURAL, REDUCED_20, REDUCED_50, NEVER }
enum class ClockPreset(val bankMillis:Long,val turnMillis:Long) { OFF(0,0), RAPID_60(60_000,10_000), STANDARD_180(180_000,20_000) }
data class Settings(val sound:Boolean=true,val music:Boolean=false,val vibration:Boolean=true,val animations:Boolean=true,val theme:BoardTheme=BoardTheme.PREMIUM_WOOD,val pieces:PieceStyle=PieceStyle.IVORY,val dice:DiceStyle=DiceStyle.CLASSIC,val difficulty:Difficulty=Difficulty.MEDIUM,val tableScene:TableScene=TableScene.AUTUMN_SUNSET,val doublesRate:DoublesRate=DoublesRate.NATURAL,val clockPreset:ClockPreset=ClockPreset.RAPID_60,val aiPersona:AiPersona=AiPersona.MASTER)
class Preferences(context: Context) {
    private val p=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
    fun load()=Settings(p.getBoolean("sound",true),p.getBoolean("music",false),p.getBoolean("vibration",true),p.getBoolean("animations",true),enum("theme",BoardTheme.PREMIUM_WOOD),enum("pieces",PieceStyle.IVORY),enum("dice",DiceStyle.CLASSIC),enum("difficulty",Difficulty.MEDIUM),enum("tableScene",TableScene.AUTUMN_SUNSET),enum("doublesRate",DoublesRate.NATURAL),enum("clockPreset",ClockPreset.RAPID_60),enum("aiPersona",AiPersona.MASTER))
    private inline fun <reified T:Enum<T>> enum(key:String, default:T)=runCatching { enumValueOf<T>(p.getString(key,default.name)!!) }.getOrDefault(default)
    fun save(s:Settings)=p.edit().putBoolean("sound",s.sound).putBoolean("music",s.music).putBoolean("vibration",s.vibration).putBoolean("animations",s.animations).putString("theme",s.theme.name).putString("pieces",s.pieces.name).putString("dice",s.dice.name).putString("difficulty",s.difficulty.name).putString("tableScene",s.tableScene.name).putString("doublesRate",s.doublesRate.name).putString("clockPreset",s.clockPreset.name).putString("aiPersona",s.aiPersona.name).apply()
}
