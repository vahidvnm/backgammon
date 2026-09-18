package com.arena.backgammon.render
import com.arena.backgammon.data.BoardTheme

data class ThemeColors(val environment:FloatArray,val wood:FloatArray,val woodDark:FloatArray,val felt:FloatArray,val pointA:FloatArray,val pointB:FloatArray,val metal:FloatArray,val white:FloatArray,val black:FloatArray,val glow:FloatArray,val shadow:FloatArray){companion object{
 private fun c(hex:Long,a:Float=1f)=floatArrayOf(((hex shr 16)and 255)/255f,((hex shr 8)and 255)/255f,(hex and 255)/255f,a)
 fun of(t:BoardTheme)=when(t){
  BoardTheme.CLASSIC_WOOD->ThemeColors(c(0x100b08),c(0x704026),c(0x351b10),c(0x28543c),c(0xd3ad72),c(0x6f251d),c(0xb58a46),c(0xf2dfbb),c(0x241b18),c(0xffc85c),c(0x050403,.55f))
  BoardTheme.ROYAL->ThemeColors(c(0x10080a),c(0x351618),c(0x18090c),c(0x5b2425),c(0xc9a55b),c(0x241014),c(0xd5ae55),c(0xf3dfbd),c(0x310e14),c(0xffd66b),c(0x030202,.6f))
  BoardTheme.EMERALD->ThemeColors(c(0x04110d),c(0x392418),c(0x160c08),c(0x07533d),c(0xd4b763),c(0x113b31),c(0xcbaa51),c(0xf4e6c8),c(0x102a24),c(0xffd863),c(0x020806,.6f))
  BoardTheme.OCEAN->ThemeColors(c(0x040c17),c(0x563b2a),c(0x24170f),c(0x0b4168),c(0x9bc1d9),c(0x17304d),c(0x9cb4c5),c(0xe9f3f7),c(0x102536),c(0x62d4ff),c(0x02050a,.6f))
  BoardTheme.ROYAL_PURPLE->ThemeColors(c(0x0e0717),c(0x2c172d),c(0x130918),c(0x462260),c(0xd1af56),c(0x241135),c(0xd2ad52),c(0xf3dfc0),c(0x25132d),c(0xe5bfff),c(0x030204,.65f))
  BoardTheme.BLACK_LUXURY->ThemeColors(c(0x030405),c(0x17191c),c(0x08090b),c(0x20252a),c(0xc5a55b),c(0x30363c),c(0xb99a55),c(0xddd8cb),c(0x090a0c),c(0xffd56a),c(0x010101,.7f))
 }
}}
