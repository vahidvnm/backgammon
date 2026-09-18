package com.arena.backgammon.data

import android.content.Context
import com.arena.backgammon.ai.Difficulty

enum class BoardTheme { CLASSIC_WOOD, DARK_WOOD, LUXURY, MODERN, MINIMAL }
enum class PieceStyle { IVORY, MARBLE, NEON }
enum class DiceStyle { CLASSIC, ONYX, CRYSTAL }
data class Settings(val sound:Boolean=true,val music:Boolean=false,val vibration:Boolean=true,val animations:Boolean=true,val theme:BoardTheme=BoardTheme.CLASSIC_WOOD,val pieces:PieceStyle=PieceStyle.IVORY,val dice:DiceStyle=DiceStyle.CLASSIC,val difficulty:Difficulty=Difficulty.MEDIUM)
class Preferences(context: Context) {
    private val p=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
    fun load()=Settings(p.getBoolean("sound",true),p.getBoolean("music",false),p.getBoolean("vibration",true),p.getBoolean("animations",true),enum("theme",BoardTheme.CLASSIC_WOOD),enum("pieces",PieceStyle.IVORY),enum("dice",DiceStyle.CLASSIC),enum("difficulty",Difficulty.MEDIUM))
    private inline fun <reified T:Enum<T>> enum(key:String, default:T)=runCatching { enumValueOf<T>(p.getString(key,default.name)!!) }.getOrDefault(default)
    fun save(s:Settings)=p.edit().putBoolean("sound",s.sound).putBoolean("music",s.music).putBoolean("vibration",s.vibration).putBoolean("animations",s.animations).putString("theme",s.theme.name).putString("pieces",s.pieces.name).putString("dice",s.dice.name).putString("difficulty",s.difficulty.name).apply()
}
